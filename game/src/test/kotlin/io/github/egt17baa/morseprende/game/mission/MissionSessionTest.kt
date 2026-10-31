package io.github.egt17baa.morseprende.game.mission

import io.github.egt17baa.morseprende.game.content.StoryMessage
import io.github.egt17baa.morseprende.game.review.CharOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertFailsWith

class MissionSessionTest {

    private val final = Exercise.FinalMessage(StoryMessage("Gaviota", "ESTO ES SOS"))

    @Test fun matches_is_lenient() {
        assertTrue(MissionSession.matches("  hola  mundo ", "HOLA MUNDO"))
        assertTrue(MissionSession.matches("canción", "CANCION"))
        assertTrue(MissionSession.matches("ñ", "Ñ"))
        assertFalse(MissionSession.matches("hola", "HOLAS"))
        assertFalse(MissionSession.matches("", "E"))
    }

    @Test fun listen_then_continue() {
        val s = MissionSession(listOf(Exercise.Listen(listOf('S', 'O')), final))
        assertEquals(Exercise.Listen(listOf('S', 'O')), s.current)
        s.continueListen()
        assertEquals(final, s.current)
    }

    @Test fun receive_one_attempt_always_advances() {
        val s = MissionSession(listOf(Exercise.ReceiveTyped("SOS"), Exercise.ReceiveChoice("S", listOf("S", "O")), final))
        val wrong = s.answer("oso")
        assertFalse(wrong.correct)
        assertTrue(wrong.advanced)
        assertEquals("SOS", wrong.expected)
        val right = s.answer("s")
        assertTrue(right.correct)
        assertTrue(right.advanced)
        assertEquals(final, s.current)
    }

    @Test fun send_three_attempts() {
        val s = MissionSession(listOf(Exercise.Send("S"), Exercise.Send("O"), final))
        val r1 = s.send("E")
        assertFalse(r1.advanced)
        assertEquals(2, r1.attemptsLeft)
        s.send("I")
        val r3 = s.send("S")
        assertTrue(r3.correct)
        assertTrue(r3.advanced)
        assertTrue(s.send("O").correct)
        s.answer("esto es sos")
        assertEquals(MissionResult(correct = 1, total = 2, stars = 1), s.result())
    }

    @Test fun send_advances_after_third_failure() {
        val s = MissionSession(listOf(Exercise.Send("S"), final))
        assertEquals(2, s.send("E").attemptsLeft)
        assertEquals(1, s.send("E").attemptsLeft)
        val last = s.send("E")
        assertEquals(0, last.attemptsLeft)
        assertTrue(last.advanced)
        assertFalse(last.correct)
        assertEquals(final, s.current)
    }

    @Test fun final_message_needs_correct_answer() {
        val s = MissionSession(listOf(final))
        assertFalse(s.answer("ESTO").advanced)
        assertEquals(0, s.hintLevel)
        s.answer("X")
        assertEquals(1, s.hintLevel)
        s.answer("X"); s.answer("X")
        assertEquals(2, s.hintLevel)
        s.answer("X"); s.answer("X")
        assertEquals(2, s.hintLevel)
        assertFalse(s.isFinished)
        assertTrue(s.answer("esto  es sos").advanced)
        assertTrue(s.isFinished)
    }

    @Test fun stars_thresholds() {
        assertEquals(3, MissionSession.stars(1.0))
        assertEquals(3, MissionSession.stars(0.9))
        assertEquals(2, MissionSession.stars(0.89))
        assertEquals(2, MissionSession.stars(0.7))
        assertEquals(1, MissionSession.stars(0.69))
        assertEquals(1, MissionSession.stars(0.0))
    }

    @Test fun result_counts() {
        val s = MissionSession(
            listOf(Exercise.ReceiveTyped("S"), Exercise.ReceiveTyped("O"), Exercise.Send("S"), Exercise.Send("O"), final),
        )
        s.answer("S"); s.answer("E"); s.send("S"); s.send("O"); s.answer("ESTO ES SOS")
        val r = s.result()
        assertEquals(3, r.correct)
        assertEquals(4, r.total)
        assertEquals(0.75, r.accuracy, 1e-9)
        assertEquals(2, r.stars)
    }

    @Test fun no_scored_exercises_gives_three_stars() {
        val s = MissionSession(listOf(final))
        s.answer("ESTO ES SOS")
        assertEquals(3, s.result().stars)
    }

    @Test fun wrong_method_throws() {
        val s = MissionSession(listOf(Exercise.ReceiveTyped("S"), final))
        assertFailsWith<IllegalStateException> { s.send("S") }
        assertFailsWith<IllegalStateException> { s.continueListen() }
    }

    @Test fun result_before_finish_throws() {
        assertFailsWith<IllegalStateException> { MissionSession(listOf(final)).result() }
    }

    @Test fun outcomes_record_receive_and_first_send() {
        val s = MissionSession(listOf(Exercise.ReceiveTyped("SO"), Exercise.Send("S"), final))
        s.answer("SE")
        s.send("E")
        s.send("S")
        s.answer("ESTO ES SOS")
        assertEquals(listOf(CharOutcome('S', true), CharOutcome('O', false), CharOutcome('S', false)), s.outcomes)
    }
}
