package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.content.ContentValidator
import io.github.egt17baa.morseprende.game.content.MissionType
import io.github.egt17baa.morseprende.game.content.RealContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MissionPlannerTest {

    private val chapter = RealContent.chapter1
    private fun mission(id: String) = chapter.missions.single { it.id == id }
    private fun plan(id: String, seed: Int = 42) = MissionPlanner.plan(mission(id), chapter, Random(seed))

    private fun List<Exercise>.kinds() = map { it::class.simpleName }

    @Test fun lesson_structure() {
        val exercises = plan("c1m3")
        assertEquals(
            listOf("Listen") + List(4) { "ReceiveChoice" } + List(4) { "ReceiveTyped" } +
                List(4) { "Send" } + listOf("FinalMessage"),
            exercises.kinds(),
        )
        assertEquals(Exercise.Listen(listOf('A', 'N')), exercises.first())
        assertEquals(Exercise.FinalMessage(mission("c1m3").finalMessage), exercises.last())
    }

    @Test fun only_learned_chars() {
        for (m in chapter.missions) {
            val learned = ContentValidator.learnedCharsUpTo(listOf(chapter), m.id)
            for (seed in 0 until 20) {
                val texts = MissionPlanner.plan(m, chapter, Random(seed)).flatMap { e ->
                    when (e) {
                        is Exercise.Listen -> e.chars.map { it.toString() }
                        is Exercise.ReceiveChoice -> e.options + e.answer
                        is Exercise.ReceiveTyped -> listOf(e.answer)
                        is Exercise.Send -> listOf(e.target)
                        is Exercise.FinalMessage -> emptyList()
                    }
                }
                texts.forEach { t ->
                    assertTrue("${m.id} seed=$seed '$t' usa letras no aprendidas", t.all { it in learned })
                }
            }
        }
    }

    @Test fun every_new_char_is_asked() {
        for (m in chapter.missions.filter { it.type == MissionType.LESSON }) {
            val exercises = plan(m.id)
            val choiceAnswers = exercises.filterIsInstance<Exercise.ReceiveChoice>().map { it.answer }
            val sendTargets = exercises.filterIsInstance<Exercise.Send>().map { it.target }
            m.newChars.forEach { c ->
                assertTrue("${m.id}: $c como respuesta", c.toString() in choiceAnswers)
                assertTrue("${m.id}: $c como envío", c.toString() in sendTargets)
            }
        }
    }

    @Test fun options_contain_answer_and_are_distinct() {
        for (m in chapter.missions) for (seed in 0 until 20) {
            MissionPlanner.plan(m, chapter, Random(seed)).filterIsInstance<Exercise.ReceiveChoice>().forEach { e ->
                assertTrue(e.answer in e.options)
                assertEquals(e.options.size, e.options.toSet().size)
                assertTrue("${m.id}: ${e.options}", e.options.size in 2..4)
                assertTrue("mismo tipo: ${e.options}", e.options.all { (it.length == 1) == (e.answer.length == 1) })
            }
        }
    }

    @Test fun first_mission_letter_options() {
        plan("c1m1").filterIsInstance<Exercise.ReceiveChoice>().filter { it.answer.length == 1 }.forEach {
            assertEquals(setOf("S", "O"), it.options.toSet())
        }
    }

    @Test fun send_words_are_short() {
        for (m in chapter.missions) {
            MissionPlanner.plan(m, chapter, Random(3)).filterIsInstance<Exercise.Send>()
                .filter { it.target.length > 1 }
                .forEach { assertTrue("${m.id}: ${it.target}", it.target.length in 2..4) }
        }
    }

    @Test fun deterministic_with_seed() = assertEquals(plan("c1m4", 7), plan("c1m4", 7))

    @Test fun review_and_finale_structure() {
        assertEquals(
            List(5) { "ReceiveChoice" } + List(5) { "ReceiveTyped" } + List(4) { "Send" } + listOf("FinalMessage"),
            plan("c1r1").kinds(),
        )
        val finale = plan("c1f")
        assertEquals(List(4) { "ReceiveTyped" } + List(2) { "Send" } + listOf("FinalMessage"), finale.kinds())
        assertTrue(finale.filterIsInstance<Exercise.ReceiveTyped>().all { it.answer.length > 1 })
    }

    @Test(timeout = 2_000)
    fun single_letter_first_lesson_terminates() {
        val one = chapter.copy(
            words = emptyList(),
            missions = listOf(chapter.missions.first().copy(newChars = listOf('E'), finalMessage = chapter.missions.first().finalMessage.copy(text = "E"))),
        )
        val exercises = MissionPlanner.plan(one.missions.first(), one, Random(1))
        assertTrue(exercises.filterIsInstance<Exercise.ReceiveChoice>().all { it.answer in it.options })
    }

    private fun singleLetters(boxes: Map<Char, Int>): List<String> = (0 until 200).flatMap { seed ->
        MissionPlanner.plan(mission("c1r1"), chapter, Random(seed), boxes).flatMap { e ->
            when (e) {
                is Exercise.ReceiveChoice -> listOf(e.answer)
                is Exercise.ReceiveTyped -> listOf(e.answer)
                is Exercise.Send -> listOf(e.target)
                else -> emptyList()
            }
        }.filter { it.length == 1 }
    }

    @Test fun review_prefers_weak_letters() {
        val weakA = singleLetters(mapOf('A' to 1, 'S' to 5, 'O' to 5, 'E' to 5, 'T' to 5, 'N' to 5))
        assertTrue("A=${weakA.count { it == "A" }} de ${weakA.size}", weakA.count { it == "A" } >= weakA.size * 0.4)
        val uniform = singleLetters(emptyMap())
        uniform.groupingBy { it }.eachCount().forEach { (c, n) ->
            assertTrue("$c=$n de ${uniform.size}", n <= uniform.size * 0.35)
        }
    }
}
