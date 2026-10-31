package io.github.egt17baa.morseprende.game.games

import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlin.random.Random

enum class FlashPhase { SHOWING, ANSWERING, OVER }

/**
 * Destello rápido: el faro emite una letra en Morse y hay que reconocerla cuanto antes.
 * 10 rondas; un acierto vale `max(0, 2000 − ms) / 20` puntos (0–100).
 * La UI reproduce el destello y llama a [flashFinished]; desde ahí se mide la reacción.
 */
class FlashGame(
    private val chars: Collection<Char>,
    private val boxes: Map<Char, Int>,
    private val random: Random,
) {
    private val letters = chars.distinct()
    private val charOutcomes = mutableListOf<CharOutcome>()
    private val reactions = mutableListOf<Long>()
    private var shownAt = 0L

    var round: Int = 1
        private set
    var phase: FlashPhase = FlashPhase.SHOWING
        private set
    var current: Char? = null
        private set
    var options: List<Char> = emptyList()
        private set
    var score: Int = 0
        private set

    val isOver: Boolean get() = phase == FlashPhase.OVER
    val outcomes: List<CharOutcome> get() = charOutcomes.toList()
    val averageReactionMs: Int get() = if (reactions.isEmpty()) 0 else reactions.average().toInt()

    init {
        require(letters.isNotEmpty()) { "Destello rápido necesita al menos una letra" }
        current = Leitner.pick(letters, boxes, random)
    }

    fun flashFinished(now: Long) {
        if (phase != FlashPhase.SHOWING) return
        val answer = current ?: return
        val distractors = letters.filter { it != answer }.shuffled(random).take(OPTIONS - 1)
        options = (distractors + answer).shuffled(random)
        shownAt = now
        phase = FlashPhase.ANSWERING
    }

    /** @return true si acierta. Fuera de la fase de respuesta se ignora (devuelve false). */
    fun answer(c: Char, now: Long): Boolean {
        if (phase != FlashPhase.ANSWERING) return false
        val answer = current ?: return false
        val ok = c == answer
        charOutcomes += CharOutcome(answer, ok)
        if (ok) {
            val ms = (now - shownAt).coerceAtLeast(0)
            reactions += ms
            score += (maxOf(0L, MAX_MS - ms) / 20).toInt()
        }
        if (round >= ROUNDS) {
            phase = FlashPhase.OVER
            current = null
            options = emptyList()
        } else {
            round++
            current = Leitner.pick(letters, boxes, random)
            options = emptyList()
            phase = FlashPhase.SHOWING
        }
        return ok
    }

    companion object {
        const val ROUNDS = 10
        private const val OPTIONS = 4
        private const val MAX_MS = 2000L
    }
}
