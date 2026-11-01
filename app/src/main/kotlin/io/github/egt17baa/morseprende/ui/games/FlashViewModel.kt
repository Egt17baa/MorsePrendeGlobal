package io.github.egt17baa.morseprende.ui.games

import io.github.egt17baa.morseprende.core.MorseAlphabet
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.game.games.FlashGame
import io.github.egt17baa.morseprende.game.games.FlashPhase
import io.github.egt17baa.morseprende.playback.SteppedOutput
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.translator.TranslatorViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class FlashUiState(
    val phase: GamePhase = GamePhase.READY,
    val round: Int = 0,
    /** Letra de la ronda (solo para la lógica y las pruebas; la UI no la muestra). */
    val answer: Char? = null,
    val options: List<Char> = emptyList(),
    val score: Int = 0,
    val lampOn: Boolean = false,
    val flashWpm: Int = 0,
    val lastReactionMs: Int? = null,
    val lastCorrect: Boolean? = null,
    val averageReactionMs: Int = 0,
    val isNewRecord: Boolean = false,
    val lampColor: String = "lamp_cream",
    val reward: io.github.egt17baa.morseprende.rewards.RewardSummary? = null,
)

class FlashViewModel(deps: GameDeps) : GameViewModelBase(deps, GameId.FLASH) {

    private val ui = MutableStateFlow(FlashUiState(phase = idlePhase()))
    val state: StateFlow<FlashUiState> = combine(ui, settings) { s, prefs -> s.copy(flashWpm = flashWpm(prefs), lampColor = prefs.lampColor) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ui.value.copy(flashWpm = flashWpm(settings.value)))

    private var game: FlashGame? = null
    private var flashJob: Job? = null
    private val lamp = SteppedOutput(setOn = { on -> ui.update { it.copy(lampOn = on) } })

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
        game = FlashGame(learned, boxes.value, deps.random)
        ui.update { FlashUiState(phase = GamePhase.PLAYING) }
        showRound()
    }

    fun answer(c: Char) {
        val g = game ?: return
        if (ui.value.phase != GamePhase.PLAYING || g.phase != FlashPhase.ANSWERING) return
        val before = g.averageReactionMs
        val correct = g.answer(c, now())
        val reaction = if (correct) g.averageReactionMs.let { avg -> if (before == 0) avg else null } else null
        ui.update { it.copy(score = g.score, lastCorrect = correct, lastReactionMs = reaction, averageReactionMs = g.averageReactionMs, options = emptyList()) }
        if (g.isOver) {
            ui.update { it.copy(phase = GamePhase.OVER, answer = null) }
            saveOnce(g.score, g.averageReactionMs, g.outcomes) { isRecord, reward -> ui.update { it.copy(isNewRecord = isRecord, reward = reward) } }
        } else {
            showRound()
        }
    }

    fun abandon() {
        if (ui.value.phase == GamePhase.PLAYING) {
            flashJob?.cancel()
            lamp.stop()
            game = null
            ui.update { it.copy(phase = GamePhase.READY, options = emptyList(), lampOn = false) }
        }
    }

    override fun onCleared() {
        flashJob?.cancel()
        super.onCleared()
    }

    private fun showRound() {
        val g = game ?: return
        val c = g.current ?: return
        ui.update { it.copy(round = g.round, answer = c, options = emptyList()) }
        val wpm = flashWpm(settings.value)
        val signals = MorseTiming(wpm).signalsFor(MorseAlphabet.codeFor(c).orEmpty())
        flashJob?.cancel()
        flashJob = gameScope.launch {
            delay(PAUSE_BEFORE_FLASH_MS)
            lamp.play(signals)
            g.flashFinished(now())
            ui.update { it.copy(options = g.options) }
        }
    }

    /** Velocidad del destello: limitada a 7 PPM si «limitar destellos» está activado. */
    private fun flashWpm(prefs: AppSettings): Int =
        if (prefs.limitFlashes) minOf(prefs.charWpm, TranslatorViewModel.FLASH_SAFE_WPM) else prefs.charWpm

    private companion object {
        const val PAUSE_BEFORE_FLASH_MS = 700L
    }
}
