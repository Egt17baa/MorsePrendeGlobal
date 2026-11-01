package io.github.egt17baa.morseprende.ui.games

import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.game.games.NightChannel
import io.github.egt17baa.morseprende.game.games.NightWatchGame
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SteppedOutput
import io.github.egt17baa.morseprende.translator.TranslatorViewModel
import io.github.egt17baa.morseprende.ui.mission.Feedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NightWatchUiState(
    val phase: GamePhase = GamePhase.READY,
    /** Mensaje actual (solo para la lógica y las pruebas; la UI no lo muestra). */
    val text: String? = null,
    val channel: NightChannel = NightChannel.SOUND,
    val hour: Int = 0,
    val maxHour: Int = 0,
    val wpm: Int = 0,
    val score: Int = 0,
    val strikes: Int = 0,
    val lampOn: Boolean = false,
    /** El móvil no pudo vibrar: los mensajes de vibración van por sonido. */
    val vibrationFallback: Boolean = false,
    val feedback: Feedback? = null,
    val canRepeat: Boolean = false,
    val isNewRecord: Boolean = false,
    val reward: io.github.egt17baa.morseprende.rewards.RewardSummary? = null,
)

/** «Guardia nocturna»: ver [NightWatchGame]. Récord = mensajes copiados; extra = hora alcanzada. */
class NightWatchViewModel(deps: GameDeps) : GameViewModelBase(deps, GameId.NIGHT_WATCH) {

    private val ui = MutableStateFlow(NightWatchUiState(phase = idlePhase()))
    val state: StateFlow<NightWatchUiState> = ui.asStateFlow()
    private var game: NightWatchGame? = null
    private val lamp = SteppedOutput(setOn = { on -> ui.update { it.copy(lampOn = on) } }, now = deps.clock)

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
        val prefs = settings.value
        val g = NightWatchGame(chapterWords, learned, prefs.effectiveWpm, deps.random, vibrationAllowed = prefs.vibration)
        game = g
        ui.value = NightWatchUiState(phase = GamePhase.PLAYING, canRepeat = true).synced(g)
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
        ui.update { it.synced(g).copy(feedback = Feedback(result.correct, result.expected)) }
        if (g.isOver) {
            ui.update { it.copy(phase = GamePhase.OVER) }
            saveOnce(g.score, if (g.score > 0) g.hour else 0, g.outcomes) { isRecord, reward -> ui.update { it.copy(isNewRecord = isRecord, reward = reward) } }
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
            ui.update { it.copy(phase = GamePhase.READY, feedback = null, lampOn = false) }
        }
    }

    private fun NightWatchUiState.synced(g: NightWatchGame) = copy(
        text = g.current.text, channel = g.current.channel, hour = g.hour, maxHour = maxOf(maxHour, g.hour),
        wpm = g.wpm, score = g.score, strikes = g.strikes,
    )

    private fun playCurrent() {
        val g = game ?: return
        val prefs = settings.value
        var charWpm = maxOf(prefs.charWpm, g.wpm)
        var effective = g.wpm
        val channel = g.current.channel
        if (channel == NightChannel.LIGHT && prefs.limitFlashes) {
            // Personas fotosensibles: la luz nunca pasa de 7 PPM, por rápida que vaya la guardia.
            charWpm = minOf(charWpm, TranslatorViewModel.FLASH_SAFE_WPM)
            effective = minOf(effective, charWpm)
        }
        val signals = MorseTiming(charWpm, minOf(effective, charWpm)).signalsFor(MorseCodec.encode(g.current.text).morse)
        val output = when {
            channel == NightChannel.LIGHT -> lamp
            channel == NightChannel.VIBRATION && prefs.vibration && !ui.value.vibrationFallback ->
                deps.outputs.create(OutputKind.VIBRATION, prefs, onFailure = ::vibrationFailed)
            else -> deps.outputs.create(OutputKind.SOUND, prefs, noise = g.noise)
        }
        player.play(signals, listOf(output))
    }

    /** Sin vibrador (o con ahorro de batería): se avisa y se repite por sonido. */
    private fun vibrationFailed() {
        if (ui.value.vibrationFallback) return
        ui.update { it.copy(vibrationFallback = true) }
        gameScope.launch { playCurrent() }
    }
}
