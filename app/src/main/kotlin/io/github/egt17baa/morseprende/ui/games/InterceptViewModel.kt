package io.github.egt17baa.morseprende.ui.games

import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.game.games.InterceptGame
import io.github.egt17baa.morseprende.ui.mission.Feedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InterceptUiState(
    val phase: GamePhase = GamePhase.READY,
    /** Palabra actual (solo para la lógica y las pruebas; la UI no la muestra). */
    val word: String? = null,
    val wpm: Int = 0,
    val maxWpm: Int = 0,
    val score: Int = 0,
    val strikes: Int = 0,
    val feedback: Feedback? = null,
    val canRepeat: Boolean = false,
    val isNewRecord: Boolean = false,
    val reward: io.github.egt17baa.morseprende.rewards.RewardSummary? = null,
)

class InterceptViewModel(deps: GameDeps) : GameViewModelBase(deps, GameId.INTERCEPT) {

    private val ui = MutableStateFlow(InterceptUiState(phase = idlePhase()))
    val state: StateFlow<InterceptUiState> = ui.asStateFlow()
    private var game: InterceptGame? = null

    init {
        observeProgress()
    }

    override fun onProgressChanged() {
        ui.update { if (it.phase == GamePhase.PLAYING || it.phase == GamePhase.OVER) it else it.copy(phase = idlePhase()) }
    }

    fun start() {
        if (!unlocked) {
            ui.update { it.copy(phase = GamePhase.LOCKED) }
            return
        }
        beginGame()
        // Empieza a la velocidad efectiva (con las pausas largas entre letras), como las misiones.
        val g = InterceptGame(chapterWords, learned, settings.value.effectiveWpm, deps.random)
        game = g
        ui.value = InterceptUiState(phase = GamePhase.PLAYING, word = g.currentWord, wpm = g.wpm, maxWpm = g.maxWpm, canRepeat = true)
        playCurrent()
    }

    fun repeat() {
        if (ui.value.phase != GamePhase.PLAYING || !ui.value.canRepeat || ui.value.feedback != null) return
        ui.update { it.copy(canRepeat = false) }
        playCurrent()
    }

    fun answer(text: String) {
        val g = game ?: return
        if (ui.value.phase != GamePhase.PLAYING || ui.value.feedback != null || g.isOver) return
        val result = g.answer(text)
        player.stop()
        ui.update {
            it.copy(
                feedback = Feedback(result.correct, result.expected), score = g.score, strikes = g.strikes,
                wpm = g.wpm, maxWpm = g.maxWpm, word = g.currentWord,
            )
        }
        if (g.isOver) {
            ui.update { it.copy(phase = GamePhase.OVER) }
            // La velocidad máxima solo cuenta si se copió alguna palabra (si no, «20 PPM» saldría gratis).
            saveOnce(g.score, if (g.score > 0) g.maxWpm else 0, g.outcomes) { isRecord, reward -> ui.update { it.copy(isNewRecord = isRecord, reward = reward) } }
        }
    }

    fun next() {
        if (ui.value.phase != GamePhase.PLAYING || ui.value.feedback == null) return
        ui.update { it.copy(feedback = null, canRepeat = true) }
        playCurrent()
    }

    fun abandon() {
        if (ui.value.phase == GamePhase.PLAYING) {
            player.stop()
            game = null
            ui.update { it.copy(phase = GamePhase.READY, feedback = null) }
        }
    }

    private fun playCurrent() {
        val g = game ?: return
        val prefs = settings.value
        playSound(g.currentWord, wpm = maxOf(prefs.charWpm, g.wpm), effectiveWpm = g.wpm)
    }
}
