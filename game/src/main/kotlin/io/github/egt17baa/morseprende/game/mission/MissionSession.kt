package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.core.MorseCodec

data class AnswerResult(val correct: Boolean, val expected: String, val advanced: Boolean, val attemptsLeft: Int)

data class MissionResult(val correct: Int, val total: Int, val stars: Int) {
    val accuracy: Double get() = if (total == 0) 1.0 else correct.toDouble() / total
}

/**
 * Una partida de una misión.
 *
 * - Recibir: un solo intento; siempre avanza y cuenta si se acierta.
 * - Enviar: hasta [SEND_ATTEMPTS] intentos; puntúa solo el acierto al primer intento.
 * - Mensaje final: no puntúa; solo avanza al acertar y da pistas cada 2 fallos.
 */
class MissionSession(val exercises: List<Exercise>) {

    var index: Int = 0
        private set
    var hintLevel: Int = 0
        private set

    private var correct = 0
    private var attemptsUsed = 0
    private var finalFailures = 0

    val current: Exercise? get() = exercises.getOrNull(index)
    val isFinished: Boolean get() = index >= exercises.size

    private val scoredTotal = exercises.count { it is Exercise.ReceiveChoice || it is Exercise.ReceiveTyped || it is Exercise.Send }

    fun continueListen() {
        check(current is Exercise.Listen) { "El ejercicio actual no es de escuchar" }
        advance()
    }

    fun answer(response: String): AnswerResult = when (val e = current) {
        is Exercise.ReceiveChoice -> receive(response, e.answer)
        is Exercise.ReceiveTyped -> receive(response, e.answer)
        is Exercise.FinalMessage -> {
            val ok = matches(response, e.message.text)
            if (ok) {
                advance()
            } else {
                finalFailures++
                hintLevel = minOf(MAX_HINT_LEVEL, finalFailures / 2)
            }
            AnswerResult(ok, e.message.text, advanced = ok, attemptsLeft = Int.MAX_VALUE)
        }
        else -> throw IllegalStateException("El ejercicio actual no admite respuesta escrita: $e")
    }

    fun send(decoded: String): AnswerResult {
        val e = current as? Exercise.Send ?: throw IllegalStateException("El ejercicio actual no es de enviar")
        val ok = matches(decoded, e.target)
        attemptsUsed++
        if (ok && attemptsUsed == 1) correct++
        val attemptsLeft = SEND_ATTEMPTS - attemptsUsed
        val advance = ok || attemptsLeft <= 0
        if (advance) advance()
        return AnswerResult(ok, e.target, advanced = advance, attemptsLeft = attemptsLeft)
    }

    fun result(): MissionResult {
        check(isFinished) { "La misión no ha terminado" }
        val partial = MissionResult(correct, scoredTotal, 0)
        return partial.copy(stars = stars(partial.accuracy))
    }

    private fun receive(response: String, expected: String): AnswerResult {
        val ok = matches(response, expected)
        if (ok) correct++
        advance()
        return AnswerResult(ok, expected, advanced = true, attemptsLeft = 0)
    }

    private fun advance() {
        index++
        attemptsUsed = 0
        finalFailures = 0
        hintLevel = 0
    }

    companion object {
        const val SEND_ATTEMPTS = 3
        private const val MAX_HINT_LEVEL = 2

        fun stars(accuracy: Double): Int = when {
            accuracy >= 0.9 -> 3
            accuracy >= 0.7 -> 2
            else -> 1
        }

        /** Compara sin distinguir mayúsculas, tildes ni espacios sobrantes. */
        fun matches(response: String, expected: String): Boolean {
            val normalizedResponse = MorseCodec.normalizeText(response)
            return normalizedResponse.isNotEmpty() && normalizedResponse == MorseCodec.normalizeText(expected)
        }
    }
}
