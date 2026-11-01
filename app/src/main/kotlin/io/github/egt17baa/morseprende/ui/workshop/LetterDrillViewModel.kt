package io.github.egt17baa.morseprende.ui.workshop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.workshop.LetterDrill
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class DrillPhase { LOADING, LOCKED, SETUP, PLAYING }

data class DrillFeedback(val correct: Boolean, val expected: Char)

data class LetterDrillUiState(
    val phase: DrillPhase = DrillPhase.LOADING,
    /** Letras aprendidas, en el orden de la campaña. */
    val available: List<Char> = emptyList(),
    val selected: Set<Char> = emptySet(),
    /** Letra que acaba de sonar (la UI no la enseña hasta responder). */
    val current: Char? = null,
    val feedback: DrillFeedback? = null,
    val stats: Map<Char, LetterDrill.Score> = emptyMap(),
    val isPlaying: Boolean = false,
)

/** «Letras a la carta» del Taller: ver [LetterDrill]. Cada respuesta alimenta el repaso (modo suave). */
class LetterDrillViewModel(
    private val content: ContentSource,
    progress: ProgressRepository,
    private val charStats: CharStatsRepository,
    settings: SettingsRepository,
    private val outputs: OutputFactory,
    private val random: Random = Random.Default,
    playerScope: CoroutineScope? = null,
) : ViewModel() {

    private val settings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val scope = playerScope ?: viewModelScope
    private val player = SignalPlayer(scope)
    private val ui = MutableStateFlow(LetterDrillUiState())
    private var drill: LetterDrill? = null
    private var advance: Job? = null

    val state: StateFlow<LetterDrillUiState> = combine(ui, player.isPlaying) { s, playing -> s.copy(isPlaying = playing) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LetterDrillUiState())

    init {
        viewModelScope.launch {
            val chapters = runCatching { content.chapters() }.getOrDefault(emptyList())
            val learned = ProgressRules.learnedChars(chapters, progress.progress.first().keys).toList()
            ui.update {
                it.copy(
                    available = learned,
                    selected = LetterDrill.defaultSelection(learned),
                    phase = if (LetterDrill.canStart(learned)) DrillPhase.SETUP else DrillPhase.LOCKED,
                )
            }
        }
    }

    fun toggle(c: Char) {
        if (ui.value.phase != DrillPhase.SETUP || c !in ui.value.available) return
        ui.update { it.copy(selected = if (c in it.selected) it.selected - c else it.selected + c) }
    }

    fun start() {
        val s = ui.value
        if (s.phase != DrillPhase.SETUP || !LetterDrill.canStart(s.selected)) return
        val d = LetterDrill(s.available.filter { it in s.selected }, random)
        drill = d
        ui.update { it.copy(phase = DrillPhase.PLAYING, current = d.current, feedback = null, stats = emptyMap()) }
        play()
    }

    fun replay() {
        if (ui.value.phase == DrillPhase.PLAYING && ui.value.feedback == null) play()
    }

    fun answer(c: Char) {
        val d = drill ?: return
        if (ui.value.phase != DrillPhase.PLAYING || ui.value.feedback != null) return
        val heard = d.current
        val correct = d.answer(c)
        player.stop()
        ui.update { it.copy(feedback = DrillFeedback(correct, heard), stats = d.stats.toMap()) }
        // Aunque se salga justo ahora, la respuesta llega al repaso.
        viewModelScope.launch(NonCancellable) { charStats.record(listOf(CharOutcome(heard, correct)), gentle = true) }
        advance = scope.launch {
            delay(FEEDBACK_MS)
            d.next()
            ui.update { it.copy(feedback = null, current = d.current) }
            play()
        }
    }

    /** Vuelve a elegir letras. */
    fun stop() {
        abandon()
        drill = null
        ui.update { if (it.phase == DrillPhase.PLAYING) it.copy(phase = DrillPhase.SETUP, current = null, feedback = null) else it }
    }

    /**
     * Al pasar a segundo plano (o girar el móvil): se calla sin perder la práctica. Si se estaba
     * viendo un resultado, se prepara la siguiente letra y suena al pulsar «Escuchar».
     */
    fun pause() {
        val d = drill ?: return abandon()
        val showingResult = advance?.isActive == true
        abandon()
        if (showingResult) {
            d.next()
            ui.update { it.copy(feedback = null, current = d.current) }
        }
    }

    /** Al salir de la pantalla: se calla. */
    fun abandon() {
        advance?.cancel()
        advance = null
        player.stop()
    }

    private fun play() {
        val c = drill?.current ?: return
        val prefs = settings.value
        val signals = MorseTiming(prefs.charWpm, prefs.effectiveWpm).signalsFor(MorseCodec.encode(c.toString()).morse)
        player.play(signals, listOf(outputs.create(OutputKind.SOUND, prefs)))
    }

    override fun onCleared() = abandon()

    companion object {
        /** Tiempo que se ve el resultado antes de que suene la siguiente letra. */
        const val FEEDBACK_MS = 900L
    }
}
