package io.github.egt17baa.morseprende.ui.games

import io.github.egt17baa.morseprende.core.MorseAlphabet
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.game.games.StormEvent
import io.github.egt17baa.morseprende.game.games.StormGame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Una señal en pantalla: se dibuja su patrón Morse (no la letra). */
data class SignalView(val id: Int, val char: Char, val pattern: String, val progress: Float)

data class StormUiState(
    val phase: GamePhase = GamePhase.READY,
    val letters: List<Char> = emptyList(),
    val signals: List<SignalView> = emptyList(),
    val score: Int = 0,
    val lives: Int = StormGame.LIVES,
    val isNewRecord: Boolean = false,
)

class StormViewModel(deps: GameDeps) : GameViewModelBase(deps, GameId.STORM) {

    private val ui = MutableStateFlow(StormUiState())
    val state: StateFlow<StormUiState> = ui.asStateFlow()
    private var game: StormGame? = null

    init {
        ui.update { it.copy(phase = idlePhase(), letters = learned) }
        observeProgress()
    }

    override fun onProgressChanged() {
        ui.update { if (it.phase == GamePhase.PLAYING || it.phase == GamePhase.OVER) it else it.copy(phase = idlePhase(), letters = learned) }
    }

    fun start() {
        if (!unlocked) return
        beginGame()
        game = StormGame(learned, boxes.value, deps.random, now())
        ui.update { it.copy(phase = GamePhase.PLAYING, score = 0, lives = StormGame.LIVES, signals = emptyList(), isNewRecord = false) }
        frame()
    }

    /** La UI lo llama en cada fotograma mientras se juega. */
    fun frame() {
        val g = game ?: return
        if (ui.value.phase != GamePhase.PLAYING) return
        val t = now()
        val events = g.tick(t)
        events.filterIsInstance<StormEvent.Spawned>().lastOrNull()?.let { playSound(it.char.toString(), settings.value.charWpm) }
        sync(g, t)
        if (g.isOver) finish(g)
    }

    fun input(c: Char) {
        val g = game ?: return
        if (ui.value.phase != GamePhase.PLAYING) return
        g.input(c, now())
        sync(g, now())
    }

    fun abandon() {
        if (ui.value.phase == GamePhase.PLAYING) {
            player.stop()
            game = null
            ui.update { it.copy(phase = GamePhase.READY, signals = emptyList()) }
        }
    }

    private fun finish(g: StormGame) {
        player.stop()
        ui.update { it.copy(phase = GamePhase.OVER) }
        saveOnce(g.score, 0, g.outcomes) { isRecord -> ui.update { it.copy(isNewRecord = isRecord) } }
    }

    private fun sync(g: StormGame, t: Long) {
        ui.update {
            it.copy(
                score = g.score,
                lives = g.lives,
                signals = g.signals.map { s -> SignalView(s.id, s.char, MorseAlphabet.codeFor(s.char).orEmpty(), s.progress(t).coerceAtMost(1f)) },
            )
        }
    }

}
