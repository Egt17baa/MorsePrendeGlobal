package io.github.egt17baa.morseprende.ui.games

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.rewards.RewardSummary
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Dependencias comunes de los minijuegos. */
class GameDeps(
    val content: ContentSource,
    val progress: ProgressRepository,
    val charStats: CharStatsRepository,
    val records: GameRecordsRepository,
    val settings: SettingsRepository,
    val outputs: OutputFactory,
    val random: Random = Random.Default,
    val playerScope: CoroutineScope? = null,
    val clock: () -> Long = { SystemClock.uptimeMillis() },
    val rewards: RewardsService? = null,
)

enum class GamePhase { LOADING, LOCKED, READY, PLAYING, OVER }

/**
 * Base de los minijuegos: letras aprendidas, bloqueo, cajas de Leitner, sonido y guardado único del
 * resultado al terminar. Abandonar a mitad no guarda nada.
 */
abstract class GameViewModelBase(protected val deps: GameDeps, private val gameId: GameId) : ViewModel() {

    protected val chapters: List<Chapter> = runCatching { deps.content.chapters() }.getOrDefault(emptyList())
    protected val chapterWords: List<String> = chapters.flatMap { it.words }
    protected val settings: StateFlow<AppSettings> = deps.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    protected val boxes: StateFlow<Map<Char, Int>> = deps.charStats.boxes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    /** null hasta que llega el progreso (Room lo entrega un instante después de crear el ViewModel). */
    private val completed: StateFlow<Set<String>?> =
        deps.progress.progress.map<Map<String, *>, Set<String>?> { it.keys }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    protected val player = SignalPlayer(deps.playerScope ?: viewModelScope)
    protected val gameScope: CoroutineScope get() = deps.playerScope ?: viewModelScope
    private var saved = false

    /** Letras aprendidas en misiones completadas, en orden alfabético. */
    protected val learned: List<Char> get() = ProgressRules.learnedChars(chapters, completed.value.orEmpty()).sorted()

    protected val unlocked: Boolean
        get() = ProgressRules.isGameUnlocked(gameId.name, completed.value.orEmpty()) && learned.isNotEmpty()

    protected val progressLoaded: Boolean get() = completed.value != null

    /** Fase inicial: cargando hasta tener el progreso; después, listo o bloqueado. */
    protected fun idlePhase(): GamePhase = when {
        !progressLoaded -> GamePhase.LOADING
        unlocked -> GamePhase.READY
        else -> GamePhase.LOCKED
    }

    /**
     * Las subclases lo llaman al final de su init: cada vez que cambia el progreso se reevalúa el
     * bloqueo (sin tocar una partida en curso ni su final).
     */
    protected fun observeProgress() {
        viewModelScope.launch { completed.filterNotNull().collect { onProgressChanged() } }
    }

    protected abstract fun onProgressChanged()

    protected fun now(): Long = deps.clock()

    protected fun playSound(text: String, wpm: Int, effectiveWpm: Int = wpm) {
        val signals = MorseTiming(wpm, minOf(effectiveWpm, wpm)).signalsFor(MorseCodec.encode(text).morse)
        player.play(signals, listOf(deps.outputs.create(OutputKind.SOUND, settings.value)))
    }

    /** Partida actual: el resultado de una partida anterior que llegue tarde no toca la nueva. */
    private var generation = 0

    protected fun beginGame() {
        saved = false
        generation++
    }

    /** Guarda récord y resultados por carácter una sola vez por partida. */
    protected fun saveOnce(
        score: Int,
        extra: Int,
        outcomes: List<CharOutcome>,
        onSaved: (isRecord: Boolean, reward: RewardSummary?) -> Unit,
    ) {
        if (saved) return
        saved = true
        val mine = generation
        // Aunque se salga de la pantalla justo al terminar, el récord y la recompensa se guardan.
        viewModelScope.launch(NonCancellable) {
            val isRecord = deps.records.recordGame(gameId, score, extra)
            deps.charStats.record(outcomes, gentle = true)
            val reward = deps.rewards?.onGameFinished(score, isRecord)
            if (mine == generation) onSaved(isRecord, reward)
        }
    }

    override fun onCleared() = player.stop()
}
