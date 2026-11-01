package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.review.CharOutcome
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Una partida a medias, para seguir donde se dejó al salir de la misión (o si el sistema cierra
 * la app). Guarda los ejercicios tal cual se planificaron, por dónde iba y lo acertado hasta ahí.
 */
@Serializable
data class MissionSnapshot(
    val version: Int = VERSION,
    val exercises: List<Exercise>,
    val index: Int,
    val correct: Int,
    val outcomes: List<CharOutcome>,
    val attemptsUsed: Int = 0,
    val finalFailures: Int = 0,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val VERSION = 1
        private val json = Json { ignoreUnknownKeys = true }

        /** null si el texto está roto o es de una versión que no se entiende. */
        fun fromJson(text: String): MissionSnapshot? =
            runCatching { json.decodeFromString(serializer(), text) }.getOrNull()?.takeIf { it.version == VERSION }
    }
}
