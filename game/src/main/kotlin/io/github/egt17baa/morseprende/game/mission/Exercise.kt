package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.content.StoryMessage

/** Un paso de una misión. */
sealed interface Exercise {
    /** Presentación de letras nuevas: se oyen y se ve su patrón. */
    data class Listen(val chars: List<Char>) : Exercise

    /** Suena [answer] y se elige entre [options]. */
    data class ReceiveChoice(val answer: String, val options: List<String>) : Exercise

    /** Suena [answer] y se escribe. */
    data class ReceiveTyped(val answer: String) : Exercise

    /** Hay que transmitir [target] con el manipulador. */
    data class Send(val target: String) : Exercise

    /** El mensaje de la historia que cierra la misión. */
    data class FinalMessage(val message: StoryMessage) : Exercise
}
