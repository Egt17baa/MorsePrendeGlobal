package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.content.StoryMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Un paso de una misión (serializable para guardar una partida a medias). */
@Serializable
sealed interface Exercise {
    /** Presentación de letras nuevas: se oyen y se ve su patrón. */
    @Serializable @SerialName("listen")
    data class Listen(val chars: List<Char>) : Exercise

    /** Suena [answer] y se elige entre [options]. */
    @Serializable @SerialName("choice")
    data class ReceiveChoice(val answer: String, val options: List<String>) : Exercise

    /** Suena [answer] y se escribe. */
    @Serializable @SerialName("typed")
    data class ReceiveTyped(val answer: String) : Exercise

    /** Hay que transmitir [target] con el manipulador. */
    @Serializable @SerialName("send")
    data class Send(val target: String) : Exercise

    /** El mensaje de la historia que cierra la misión. */
    @Serializable @SerialName("final")
    data class FinalMessage(val message: StoryMessage) : Exercise
}
