package io.github.egt17baa.morseprende.ui.mission

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
)
