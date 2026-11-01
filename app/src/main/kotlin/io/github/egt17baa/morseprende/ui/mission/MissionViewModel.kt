package io.github.egt17baa.morseprende.ui.mission

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.KeyerDecoder
import io.github.egt17baa.morseprende.core.KeyerEvent
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.game.review.CharOutcome
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.Mission
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.game.mission.MissionPlanner
import io.github.egt17baa.morseprende.game.mission.MissionSession
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.Sidetone
import io.github.egt17baa.morseprende.playback.SidetoneFactory
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Una partida de una misión: briefing, ejercicios (escuchar, recibir, enviar con el manipulador,
 * mensaje final) y debriefing. El progreso solo se guarda al terminar; abandonar no guarda nada.
 */
class MissionViewModel(
    private val missionId: String,
    content: ContentSource,
    private val progress: ProgressRepository,
    settings: SettingsRepository,
    private val outputs: OutputFactory,
    private val sidetoneFactory: SidetoneFactory,
    private val charStats: CharStatsRepository = NoCharStats,
    private val rewards: RewardsService? = null,
    private val random: Random = Random.Default,
    playerScope: CoroutineScope? = null,
    private val clock: () -> Long = { SystemClock.uptimeMillis() },
) : ViewModel() {

    private val settings: StateFlow<AppSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val boxes: StateFlow<Map<Char, Int>> = charStats.boxes.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    private val player = SignalPlayer(playerScope ?: viewModelScope)
    private val ui = MutableStateFlow(MissionUiState())

    val state: StateFlow<MissionUiState> = combine(ui, player.isPlaying, this.settings) { s, playing, prefs ->
        s.copy(isPlaying = playing, keyerMode = prefs.keyerMode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ui.value)

    private val mission: Mission?
    private val chapter: Chapter?
    private var session: MissionSession?
    private var keyer = KeyerDecoder(DEFAULT_UNIT_MS)
    private var sidetone: Sidetone? = null
    private var pendingAdvance = false
    private var recorded = false

    init {
        val loaded = runCatching {
            val chapters = content.chapters()
            val chapter = chapters.first { ch -> ch.missions.any { it.id == missionId } }
            val m = chapter.missions.first { it.id == missionId }
            Triple(m, chapter, MissionSession(MissionPlanner.plan(m, chapter, random)))
        }
        mission = loaded.getOrNull()?.first
        chapter = loaded.getOrNull()?.second
        val initialSession = loaded.getOrNull()?.third
        session = initialSession
        if (mission == null || chapter == null || initialSession == null) {
            // Contenido roto (ContentLoadException), misión inexistente o cualquier otro fallo de
            // carga: se muestra un error en vez de cerrar la app.
            ui.value = MissionUiState(phase = MissionPhase.ERROR)
        } else {
            ui.value = briefingState(mission, initialSession)
        }
    }

    private fun briefingState(m: Mission, s: MissionSession) = MissionUiState(
        title = m.title,
        briefing = m.briefing,
        debriefing = m.debriefing,
        exerciseCount = s.exercises.size,
        newChars = m.newChars.toSet(),
    )

    fun start() {
        if (session == null || ui.value.phase != MissionPhase.BRIEFING) return
        val m = mission
        val ch = chapter
        // Se planifica al empezar con las cajas de Leitner actuales (los repasos insisten en lo fallado).
        if (m != null && ch != null) session = MissionSession(MissionPlanner.plan(m, ch, random, boxes.value))
        val prefs = settings.value
        keyer = KeyerDecoder(1200L / prefs.effectiveWpm)
        sidetone = sidetoneFactory.create(prefs)
        ui.update { it.copy(phase = MissionPhase.EXERCISE) }
        enterExercise()
    }

    fun replay() {
        val text = when (val e = session?.current) {
            is Exercise.Listen -> e.chars.joinToString(" ")
            is Exercise.ReceiveChoice -> e.answer
            is Exercise.ReceiveTyped -> e.answer
            is Exercise.Send -> e.target
            is Exercise.FinalMessage -> e.message.text
            null -> return
        }
        val prefs = settings.value
        val signals = MorseTiming(prefs.charWpm, prefs.effectiveWpm).signalsFor(MorseCodec.encode(text).morse)
        player.play(signals, listOf(outputs.create(OutputKind.SOUND, prefs)))
    }

    fun continueListen() {
        val s = session ?: return
        if (s.current !is Exercise.Listen) return
        s.continueListen()
        enterExercise()
    }

    fun answer(text: String) {
        val s = session ?: return
        if (ui.value.feedback != null) return
        when (s.current) {
            is Exercise.ReceiveChoice, is Exercise.ReceiveTyped, is Exercise.FinalMessage -> Unit
            else -> return
        }
        val result = s.answer(text)
        pendingAdvance = result.advanced
        ui.update { it.copy(feedback = Feedback(result.correct, result.expected), hintLevel = s.hintLevel) }
    }

    fun keyDown(atMs: Long = clock()) {
        if (!acceptsKeying()) return
        keyer.press(atMs)
        val tone = sidetone ?: sidetoneFactory.create(settings.value).also { sidetone = it }
        keyIsDown = true
        tone.keyDown()
    }

    fun keyUp(atMs: Long = clock()) {
        // El tono se apaga siempre, aunque el ejercicio ya no acepte pulsaciones: nunca queda sonando.
        releaseKey()
        if (!acceptsKeying()) return
        keyer.release(atMs)
        syncKeyer()
    }

    /** La UI lo llama cada ~20 ms durante un ejercicio de enviar: cierra letras y palabras. */
    fun tick() {
        if (!acceptsKeying()) return
        when (val event = keyer.poll(clock())) {
            is KeyerEvent.LetterEnded -> appendLetter(event)
            KeyerEvent.WordEnded -> appendSpace()
            null -> Unit
        }
        syncKeyer()
    }

    fun dot() = appendElement('.')

    fun dash() = appendElement('-')

    fun nextLetter() {
        if (!acceptsKeying()) return
        val event = keyer.commitLetter()
        if (event != null) appendLetter(event) else appendSpace()
        syncKeyer()
    }

    fun deleteLast() {
        if (!acceptsKeying()) return
        if (keyer.pending.isNotEmpty()) {
            keyer.reset()
        } else {
            ui.update { it.copy(sentText = it.sentText.dropLast(1)) }
        }
        syncKeyer()
    }

    fun submitSend() {
        val s = session ?: return
        if (!acceptsKeying()) return
        releaseKey()
        keyer.commitLetter()?.let(::appendLetter)
        val result = s.send(ui.value.sentText.trim())
        pendingAdvance = result.advanced
        keyer.reset()
        ui.update {
            it.copy(feedback = Feedback(result.correct, result.expected), attemptsLeft = result.attemptsLeft, keyerPending = "")
        }
    }

    /** Quita el feedback y pasa al siguiente ejercicio (o repite el actual si toca). */
    fun next() {
        if (ui.value.phase != MissionPhase.EXERCISE || ui.value.feedback == null) return
        releaseKey()
        ui.update { it.copy(feedback = null, sentText = "", keyerPending = "") }
        if (pendingAdvance) {
            pendingAdvance = false
            enterExercise()
        }
    }

    /**
     * Salir a mitad (atrás, botón de inicio, bloqueo, llamada): se apaga todo, no se guarda nada y
     * la misión vuelve al briefing para empezar de nuevo con ejercicios nuevos. Un debriefing se
     * conserva tal cual.
     */
    fun abandon() {
        releaseAudio()
        val m = mission ?: return
        val ch = chapter ?: return
        if (ui.value.phase == MissionPhase.EXERCISE) {
            val fresh = MissionSession(MissionPlanner.plan(m, ch, random))
            session = fresh
            pendingAdvance = false
            ui.value = briefingState(m, fresh)
        }
    }

    override fun onCleared() = releaseAudio()

    private var keyIsDown = false

    /** Apaga el tono si la tecla seguía pulsada. */
    private fun releaseKey() {
        if (keyIsDown) {
            keyIsDown = false
            sidetone?.keyUp()
        }
    }

    private fun releaseAudio() {
        player.stop()
        releaseKey()
        sidetone?.close()
        sidetone = null
        keyer.reset()
    }

    private fun enterExercise() {
        val s = session ?: return
        player.stop()
        releaseKey()
        keyer.reset()
        if (s.isFinished) {
            finish()
            return
        }
        val exercise = s.current
        ui.update {
            it.copy(
                exercise = exercise, exerciseIndex = s.index, feedback = null, sentText = "", keyerPending = "",
                attemptsLeft = MissionSession.SEND_ATTEMPTS, hintLevel = s.hintLevel,
            )
        }
        if (exercise is Exercise.ReceiveChoice || exercise is Exercise.ReceiveTyped || exercise is Exercise.FinalMessage) {
            replay()
        }
    }

    private fun finish() {
        val s = session ?: return
        val result = s.result()
        sidetone?.close()
        ui.update { it.copy(phase = MissionPhase.DEBRIEFING, exercise = null, stars = result.stars) }
        if (recorded) return
        recorded = true
        viewModelScope.launch {
            val previous = progress.progress.first()[missionId]?.bestStars ?: 0
            ui.update { it.copy(previousBest = previous) }
            progress.recordResult(missionId, result.stars, System.currentTimeMillis())
            charStats.record(s.outcomes)
            val m = mission
            if (rewards != null && m != null) {
                val summary = rewards.onMissionCompleted(m, previous, result.stars)
                ui.update { it.copy(reward = summary) }
            }
        }
    }

    private fun acceptsKeying(): Boolean =
        ui.value.phase == MissionPhase.EXERCISE && session?.current is Exercise.Send && ui.value.feedback == null

    private fun appendElement(element: Char) {
        if (!acceptsKeying()) return
        keyer.appendElement(element)
        syncKeyer()
    }

    private fun appendLetter(event: KeyerEvent.LetterEnded) {
        ui.update { it.copy(sentText = it.sentText + (event.char ?: UNKNOWN_CHAR)) }
    }

    /**
     * Solo hay separación de palabras si el objetivo tiene varias: así la pausa larga de un
     * principiante entre letras no estropea una palabra correcta.
     */
    private fun appendSpace() {
        val target = (session?.current as? Exercise.Send)?.target ?: return
        if (' ' !in target) return
        ui.update { if (it.sentText.isEmpty() || it.sentText.endsWith(' ')) it else it.copy(sentText = it.sentText + ' ') }
    }

    private fun syncKeyer() {
        ui.update { it.copy(keyerPending = keyer.pending) }
    }

    private companion object {
        const val DEFAULT_UNIT_MS = 120L
        const val UNKNOWN_CHAR = '?'
    }
}

/** Sin repaso inteligente (p. ej. en pruebas que no lo necesitan). */
private object NoCharStats : CharStatsRepository {
    override val boxes: Flow<Map<Char, Int>> = emptyFlow()
    override suspend fun record(outcomes: List<CharOutcome>, gentle: Boolean) = Unit
}
