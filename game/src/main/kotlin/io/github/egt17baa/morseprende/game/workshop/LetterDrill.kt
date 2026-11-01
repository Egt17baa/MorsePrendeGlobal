package io.github.egt17baa.morseprende.game.workshop

import kotlin.random.Random

/**
 * «Letras a la carta» del Taller: suena una letra al azar de las elegidas y se marca cuál era. Sin
 * puntos ni vidas; solo cuenta aciertos por letra. Nunca suena la misma letra dos veces seguidas.
 */
class LetterDrill(letters: Collection<Char>, private val random: Random) {

    data class Score(val right: Int, val total: Int)

    private val letters: List<Char> = letters.distinct()

    init {
        require(canStart(this.letters)) { "Hacen falta al menos $MIN_LETTERS letras" }
    }

    var current: Char = this.letters.random(random)
        private set

    private val scores = linkedMapOf<Char, Score>()

    /** Aciertos y intentos de cada letra en esta práctica. */
    val stats: Map<Char, Score> get() = scores

    /** true si [c] era la letra que sonó. */
    fun answer(c: Char): Boolean {
        val correct = c.uppercaseChar() == current
        val old = scores[current] ?: Score(0, 0)
        scores[current] = Score(old.right + if (correct) 1 else 0, old.total + 1)
        return correct
    }

    fun next() {
        current = letters.filter { it != current }.random(random)
    }

    companion object {
        const val MIN_LETTERS = 2
        private const val DEFAULT_COUNT = 4

        fun canStart(letters: Collection<Char>): Boolean = letters.distinct().size >= MIN_LETTERS

        /** Por defecto, las últimas letras aprendidas: las que más se suelen atascar. */
        fun defaultSelection(learnedInOrder: List<Char>): Set<Char> = learnedInOrder.takeLast(DEFAULT_COUNT).toSet()
    }
}
