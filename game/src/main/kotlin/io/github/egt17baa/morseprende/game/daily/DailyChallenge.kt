package io.github.egt17baa.morseprende.game.daily

import io.github.egt17baa.morseprende.core.MorseCodec
import java.time.LocalDate

/**
 * «Mensaje del día»: una frase por día, la misma todo el día, elegida solo entre las que usan letras
 * ya aprendidas (nunca un reto imposible). Sin castigos: no hacerlo un día no quita nada.
 */
object DailyChallenge {

    /**
     * Frase del día [date]. Primero las frases del reto que encajan con [learned]; si no hay, las
     * palabras de los capítulos que encajan. null si aún no se puede formar ninguna.
     */
    fun messageFor(date: LocalDate, phrases: List<String>, fallbackWords: List<String>, learned: Set<Char>): String? {
        fun fits(text: String) = text.isNotBlank() && text.all { it == ' ' || it.uppercaseChar() in learned }
        val pool = phrases.filter(::fits).ifEmpty { fallbackWords.filter(::fits) }.map { it.uppercase() }.distinct().sorted()
        if (pool.isEmpty()) return null
        return pool[indexFor(date, pool.size)]
    }

    /** Índice determinista por día; dos días seguidos nunca dan el mismo (si hay más de uno). */
    private fun indexFor(date: LocalDate, size: Int): Int {
        if (size == 1) return 0
        val day = date.toEpochDay()
        // Paso coprimo con el tamaño: recorre todas las frases antes de repetir y nunca repite seguido.
        var step = 7919L % size
        while (step == 0L || gcd(step, size.toLong()) != 1L) step = (step + 1) % size
        return Math.floorMod(day * step + 13, size.toLong()).toInt()
    }

    private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

    /** Pista tras fallar: nivel 1, el Morse de la primera mitad de los caracteres; nivel 2, todo. */
    fun hint(message: String, level: Int): String? {
        if (level <= 0) return null
        val morse = MorseCodec.encode(message).morse
        if (level >= 2) return morse
        val tokens = morse.split(' ')
        val letters = tokens.count { it != "/" }
        var shown = 0
        val half = (letters + 1) / 2
        return tokens.takeWhile { t -> if (t == "/") true else (shown++ < half) }.joinToString(" ").trimEnd('/', ' ')
    }
}
