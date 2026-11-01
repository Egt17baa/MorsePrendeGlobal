package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.ContentValidator
import io.github.egt17baa.morseprende.game.content.Mission
import io.github.egt17baa.morseprende.game.content.MissionType
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlin.random.Random

/**
 * Genera los ejercicios de una misión usando solo caracteres ya aprendidos.
 *
 * - LESSON: escuchar las letras nuevas, 4 de elegir, 4 de escribir, enviar cada letra nueva y
 *   2 palabras, y el mensaje final.
 * - REVIEW: 5 de elegir, 5 de escribir, 4 envíos (2 letras y 2 palabras) y el mensaje final.
 * - FINALE: 4 palabras para escribir, 2 envíos y el mensaje final.
 */
object MissionPlanner {

    private const val MIN_SEND_WORD = 2
    private const val MAX_SEND_WORD = 4
    private const val MAX_DISTRACTOR_TRIES = 50

    /** @param boxes cajas de Leitner por carácter: en los repasos se insiste en las letras débiles. */
    /**
     * @param chapters todos los capítulos de la campaña: las letras aprendidas se acumulan entre
     * capítulos (en el capítulo 2 se siguen usando las del 1).
     */
    fun plan(
        mission: Mission,
        chapter: Chapter,
        random: Random,
        boxes: Map<Char, Int> = emptyMap(),
        chapters: List<Chapter> = listOf(chapter),
    ): List<Exercise> {
        val pool = Pool(mission, chapter, chapters, random, if (mission.type == MissionType.REVIEW) boxes else emptyMap())
        return when (mission.type) {
            MissionType.LESSON -> lesson(mission, pool)
            MissionType.REVIEW -> review(pool)
            MissionType.FINALE -> finale(pool)
        } + Exercise.FinalMessage(mission.finalMessage)
    }

    private fun lesson(mission: Mission, pool: Pool): List<Exercise> {
        val newLetters = mission.newChars.map { it.toString() }
        val choiceAnswers = (newLetters + pool.words(4 - newLetters.size, preferNew = true)).shuffled(pool.random)
        val typedLetters = (newLetters.shuffled(pool.random) + pool.letters(2)).distinct().take(2)
        val typed = (typedLetters + pool.words(2, preferNew = true)).shuffled(pool.random)
        val sends = newLetters + pool.words(2, preferNew = true, short = true)
        return listOf(Exercise.Listen(mission.newChars)) +
            choiceAnswers.map(pool::choice) +
            typed.map { Exercise.ReceiveTyped(it) } +
            sends.map { Exercise.Send(it) }
    }

    private fun review(pool: Pool): List<Exercise> {
        fun mixed(n: Int) = List(n) { i -> if (i % 2 == 0) pool.letters(1).single() else pool.words(1).single() }
        return mixed(5).map(pool::choice) +
            mixed(5).map { Exercise.ReceiveTyped(it) } +
            (pool.letters(2) + pool.words(2, short = true)).map { Exercise.Send(it) }
    }

    private fun finale(pool: Pool): List<Exercise> =
        pool.words(4).map { Exercise.ReceiveTyped(it) } +
            pool.words(2, short = true).map { Exercise.Send(it) }

    /** Letras y palabras disponibles para una misión concreta. */
    private class Pool(mission: Mission, chapter: Chapter, chapters: List<Chapter>, val random: Random, private val boxes: Map<Char, Int>) {
        private val learned: List<Char> = ContentValidator.learnedCharsUpTo(chapters, mission.id).toList()
        private val newChars = mission.newChars.toSet()
        private val validWords = chapter.words.filter { w -> w.length > 1 && w.all { it in learned } }.distinct()

        fun letters(n: Int): List<String> = List(n) {
            if (boxes.isEmpty()) learned.random(random).toString() else Leitner.pick(learned, boxes, random).toString()
        }

        /** La letra aprendida con la caja más baja (la más débil), o null si no hay cajas. */
        private val weakest: Char? = if (boxes.isEmpty()) null else learned.minByOrNull { boxes[it] ?: Leitner.MIN_BOX }

        fun words(n: Int, preferNew: Boolean = false, short: Boolean = false): List<String> {
            if (n <= 0) return emptyList()
            var candidates = validWords
            if (short) candidates = candidates.filter { it.length in MIN_SEND_WORD..MAX_SEND_WORD }
            if (preferNew) {
                val withNew = candidates.filter { w -> w.any { it in newChars } }
                if (withNew.isNotEmpty()) candidates = withNew
            }
            weakest?.let { weak ->
                val withWeak = candidates.filter { weak in it }
                if (withWeak.isNotEmpty()) candidates = withWeak
            }
            if (candidates.isEmpty()) return List(n) { combo(random.nextInt(2, 4)) }
            val shuffled = candidates.shuffled(random)
            return List(n) { shuffled[it % shuffled.size] }
        }

        fun choice(answer: String): Exercise.ReceiveChoice {
            val candidates = if (answer.length == 1) {
                learned.map { it.toString() }
            } else {
                validWords
            }
            val distractors = candidates.filter { it != answer }.shuffled(random).take(3).toMutableList()
            // Con muy pocas letras aprendidas puede no haber distractor posible: se limita el intento.
            var tries = 0
            while (distractors.isEmpty() && tries++ < MAX_DISTRACTOR_TRIES) {
                val extra = if (answer.length == 1) learned.random(random).toString() else combo(answer.length.coerceIn(2, 3))
                if (extra != answer) distractors += extra
            }
            return Exercise.ReceiveChoice(answer, (distractors + answer).shuffled(random))
        }

        /** Combinación de letras aprendidas, por si el banco no tiene palabras adecuadas. */
        private fun combo(length: Int): String = String(CharArray(length) { learned.random(random) })
    }
}
