package io.github.egt17baa.morseprende.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.daily.DailyRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.game.daily.DailyChallenge
import io.github.egt17baa.morseprende.game.mission.MissionSession
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.rewards.RewardSummary
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class DailyPhase { LOADING, LOCKED, READY, DONE }

data class DailyUiState(
    val phase: DailyPhase = DailyPhase.LOADING,
    /** Frase del día: la UI solo la enseña al descifrarla. */
    val message: String? = null,
    val failures: Int = 0,
    val hint: String? = null,
    val lastWrong: Boolean = false,
    /** Destellos ganados ahora (null si ya estaba hecho de antes). */
    val reward: RewardSummary? = null,
    val totalDone: Int = 0,
    val isPlaying: Boolean = false,
)

/**
 * «Mensaje del día»: la frase del día (solo con letras aprendidas) se escucha y se escribe, con
 * pistas si se falla. Se recompensa una sola vez por día; no hacerlo no tiene castigo.
 */
class DailyViewModel(
    private val content: ContentSource,
    progress: ProgressRepository,
    private val daily: DailyRepository,
    settings: SettingsRepository,
    private val outputs: OutputFactory,
    private val rewards: RewardsService? = null,
    today: () -> LocalDate = { LocalDate.now() },
    playerScope: CoroutineScope? = null,
) : ViewModel() {

    /** El día se fija al abrir la pantalla: si pasa la medianoche con ella abierta, cuenta el día de la frase. */
    private val day: LocalDate = today()
    private val settings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val player = SignalPlayer(playerScope ?: viewModelScope)
    private val ui = MutableStateFlow(DailyUiState())

    val state: StateFlow<DailyUiState> = combine(ui, player.isPlaying) { s, playing -> s.copy(isPlaying = playing) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DailyUiState())

    init {
        viewModelScope.launch {
            val chapters = runCatching { content.chapters() }.getOrDefault(emptyList())
            val completed = progress.progress.first().keys
            val learned = ProgressRules.learnedChars(chapters, completed)
            val message = DailyChallenge.messageFor(day, content.dailyPhrases(), chapters.flatMap { it.words }, learned)
            val done = daily.completed.first()
            ui.update {
                it.copy(
                    message = message,
                    totalDone = done.size,
                    phase = when {
                        message == null -> DailyPhase.LOCKED
                        day in done -> DailyPhase.DONE
                        else -> DailyPhase.READY
                    },
                )
            }
        }
    }

    fun play() {
        val message = ui.value.message ?: return
        if (ui.value.phase == DailyPhase.LOCKED || ui.value.phase == DailyPhase.LOADING) return
        val prefs = settings.value
        val signals = MorseTiming(prefs.charWpm, prefs.effectiveWpm).signalsFor(MorseCodec.encode(message).morse)
        player.play(signals, listOf(outputs.create(OutputKind.SOUND, prefs)))
    }

    fun answer(text: String) {
        val message = ui.value.message ?: return
        if (ui.value.phase != DailyPhase.READY) return
        if (!MissionSession.matches(text, message)) {
            val failures = ui.value.failures + 1
            ui.update { it.copy(failures = failures, lastWrong = true, hint = DailyChallenge.hint(message, failures / 2)) }
            return
        }
        player.stop()
        ui.update { it.copy(phase = DailyPhase.DONE, lastWrong = false) }
        // Aunque se salga de la pantalla justo ahora, el reto queda guardado y pagado una sola vez.
        viewModelScope.launch(NonCancellable) {
            val isNew = daily.markCompleted(day)
            val reward = if (isNew) rewards?.onDailyChallenge() else null
            val total = daily.completed.first().size
            ui.update { it.copy(reward = reward, totalDone = total) }
        }
    }

    fun abandon() = player.stop()

    override fun onCleared() = player.stop()
}
