package io.github.egt17baa.morseprende.ui.mission

import io.github.egt17baa.morseprende.game.content.Channel
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.game.mission.MissionSession
import io.github.egt17baa.morseprende.rewards.RewardSummary
import io.github.egt17baa.morseprende.settings.KeyerMode

enum class MissionPhase { BRIEFING, EXERCISE, DEBRIEFING, ERROR }

data class Feedback(val correct: Boolean, val expected: String)

data class MissionUiState(
    val phase: MissionPhase = MissionPhase.BRIEFING,
    val title: String = "",
    val briefing: String = "",
    val debriefing: String = "",
    val exercise: Exercise? = null,
    val exerciseIndex: Int = 0,
    val exerciseCount: Int = 0,
    val feedback: Feedback? = null,
    val keyerPending: String = "",
    val sentText: String = "",
    val keyerMode: KeyerMode = KeyerMode.ONE_BUTTON,
    val attemptsLeft: Int = MissionSession.SEND_ATTEMPTS,
    val hintLevel: Int = 0,
    val isPlaying: Boolean = false,
    val stars: Int = 0,
    val previousBest: Int = 0,
    /** Ayuda visual: en el capítulo 1 se enseña el patrón de las letras nuevas. */
    val showPattern: Boolean = true,
    val newChars: Set<Char> = emptySet(),
    /** Destellos, bono diario y logros ganados al terminar (null hasta entonces). */
    val reward: RewardSummary? = null,
    /** Aro del manipulador equipado en la tienda. */
    val keyerSkin: String = "key_coral",
    /** Contrarreloj del ejercicio de recibir (null = sin límite o aún sonando). */
    val secondsLeft: Int? = null,
    /** Partida a medias guardada: índice del ejercicio por el que seguir (null = no hay). */
    val resumeAt: Int? = null,
    /** La misión tiene ruido de radio en los ejercicios. */
    val noisy: Boolean = false,
    /** Por dónde llegan los ejercicios de recibir y el mensaje final. */
    val channel: Channel = Channel.SOUND,
    /** El faro pequeño de la misión (canal de luz). */
    val lampOn: Boolean = false,
    /** El móvil no vibra: la misión de vibración se hace por sonido. */
    val channelFallback: Boolean = false,
    /** Color de la luz del faro equipado en la tienda. */
    val lampColor: String = "lamp_cream",
)
