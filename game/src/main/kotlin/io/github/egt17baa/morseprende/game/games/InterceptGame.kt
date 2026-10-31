package io.github.egt17baa.morseprende.game.games

import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.game.mission.AnswerResult
import io.github.egt17baa.morseprende.game.mission.MissionSession
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlin.random.Random

/**
 * Interceptación: se oyen palabras cada vez más rápidas y hay que copiarlas. Cada 2 aciertos
 * seguidos se sube 1 PPM (hasta 30); 3 fallos terminan la partida.
 */
class InterceptGame(
    chapterWords: List<String>,
    chars: Collection<Char>,
    startWpm: Int,
    private val random: Random,
) {
    private val letters = chars.distinct()
    private val pool: List<String> = chapterWords.filter { w -> w.length > 1 && w.all { it in letters } }.distinct()
    private val charOutcomes = mutableListOf<CharOutcome>()
    private var streak = 0

    var currentWord: String = ""
        private set
    var wpm: Int = startWpm.coerceIn(MorseTiming.MIN_WPM, MorseTiming.MAX_WPM)
        private set
    var maxWpm: Int = wpm
        private set
    var score: Int = 0
        private set
    var strikes: Int = 0
        private set

    val isOver: Boolean get() = strikes >= STRIKES
    val outcomes: List<CharOutcome> get() = charOutcomes.toList()

    init {
        require(letters.isNotEmpty()) { "Interceptación necesita al menos una letra" }
        currentWord = nextWord(previous = null)
    }

    fun answer(text: String): AnswerResult {
        check(!isOver) { "La partida ha terminado" }
        val expected = currentWord
        val ok = MissionSession.matches(text, expected)
        charOutcomes += Leitner.outcomesFor(expected, text)
        if (ok) {
            score++
            streak++
            if (streak % 2 == 0) {
                wpm = minOf(MorseTiming.MAX_WPM, wpm + 1)
                maxWpm = maxOf(maxWpm, wpm)
            }
        } else {
            streak = 0
            strikes++
        }
        if (!isOver) currentWord = nextWord(previous = expected)
        return AnswerResult(ok, expected, advanced = true, attemptsLeft = STRIKES - strikes)
    }

    private fun nextWord(previous: String?): String {
        val candidates = pool.filter { it != previous }
        if (candidates.isNotEmpty()) return candidates.random(random)
        if (pool.isNotEmpty()) return pool.first()
        var combo: String
        var tries = 0
        do {
            combo = String(CharArray(random.nextInt(2, 4)) { letters.random(random) })
        } while (combo == previous && tries++ < 20)
        return combo
    }

    companion object {
        const val STRIKES = 3
    }
}
