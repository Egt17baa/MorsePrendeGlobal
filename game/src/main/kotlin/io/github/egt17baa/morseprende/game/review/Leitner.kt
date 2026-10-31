package io.github.egt17baa.morseprende.game.review

import io.github.egt17baa.morseprende.core.MorseCodec
import kotlin.random.Random

/** Resultado de un carácter en un ejercicio. */
data class CharOutcome(val char: Char, val correct: Boolean)

/**
 * Repetición espaciada con 5 cajas de Leitner por carácter. Acertar sube una caja; fallar devuelve
 * a la 1. Al elegir, cada carácter pesa `6 − caja`: los que más se fallan aparecen más.
 */
object Leitner {
    const val MIN_BOX = 1
    const val MAX_BOX = 5

    fun next(box: Int, correct: Boolean): Int = if (correct) minOf(MAX_BOX, box + 1) else MIN_BOX

    /**
     * Aplica una sesión (una misión o una partida): cada carácter se mueve **un solo paso** aunque
     * aparezca varias veces. Estricto (misiones): algún fallo → caja 1; si no, sube una.
     * Suave (minijuegos, que siempre acaban en fallo a velocidad máxima): aciertos y fallos → se
     * queda; solo fallos → baja una; solo aciertos → sube una.
     */
    fun apply(boxes: Map<Char, Int>, outcomes: List<CharOutcome>, gentle: Boolean = false): Map<Char, Int> {
        val result = boxes.toMutableMap()
        outcomes.groupBy { it.char }.forEach { (c, list) ->
            val box = result[c] ?: MIN_BOX
            val anyWrong = list.any { !it.correct }
            val anyRight = list.any { it.correct }
            result[c] = when {
                !gentle -> next(box, correct = !anyWrong)
                anyWrong && anyRight -> box
                anyWrong -> maxOf(MIN_BOX, box - 1)
                else -> minOf(MAX_BOX, box + 1)
            }
        }
        return result
    }

    fun weight(box: Int): Int = MAX_BOX + 1 - box.coerceIn(MIN_BOX, MAX_BOX)

    fun pick(chars: Collection<Char>, boxes: Map<Char, Int>, random: Random): Char {
        require(chars.isNotEmpty()) { "No hay caracteres para elegir" }
        val list = chars.distinct()
        val weights = list.map { weight(boxes[it] ?: MIN_BOX) }
        var roll = random.nextInt(weights.sum())
        list.forEachIndexed { i, c ->
            roll -= weights[i]
            if (roll < 0) return c
        }
        return list.last()
    }

    /** Cada carácter del esperado (sin espacios) acierta si la respuesta tiene el mismo en su posición. */
    fun outcomesFor(expected: String, response: String): List<CharOutcome> {
        val e = MorseCodec.normalizeText(expected).filter { it != ' ' }
        val r = MorseCodec.normalizeText(response).filter { it != ' ' }
        return e.mapIndexed { i, c -> CharOutcome(c, r.getOrNull(i) == c) }
    }
}
