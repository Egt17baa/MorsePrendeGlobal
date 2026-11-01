package io.github.egt17baa.morseprende.game.daily

import io.github.egt17baa.morseprende.core.MorseAlphabet
import io.github.egt17baa.morseprende.game.content.ContentParser
import io.github.egt17baa.morseprende.game.content.RealContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DailyChallengeTest {

    private val phrases = listOf("SOS", "NOTA", "ESTO NO", "MAR", "FARO 1", "HOLA, FARO.")
    private val early = setOf('S', 'O', 'E', 'T', 'A', 'N')
    private val all = MorseAlphabet.codes.keys

    @Test fun daily_message_stable_same_day() {
        val day = LocalDate.of(2026, 11, 1)
        assertEquals(DailyChallenge.messageFor(day, phrases, emptyList(), all), DailyChallenge.messageFor(day, phrases, emptyList(), all))
    }

    @Test fun daily_message_changes_with_date() {
        val start = LocalDate.of(2026, 11, 1)
        val week = (0L until 7).map { DailyChallenge.messageFor(start.plusDays(it), phrases, emptyList(), all) }
        assertTrue("varía en una semana: $week", week.toSet().size >= 4)
        // Dos días seguidos nunca repiten (si hay más de una frase posible).
        week.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }

    @Test fun daily_uses_only_learned_chars() {
        for (d in 0L until 60) {
            val m = DailyChallenge.messageFor(LocalDate.of(2026, 1, 1).plusDays(d), phrases, emptyList(), early)!!
            assertTrue(m, m.all { it == ' ' || it in early })
        }
    }

    @Test fun daily_falls_back_to_words() {
        val m = DailyChallenge.messageFor(LocalDate.of(2026, 11, 1), listOf("FARO"), listOf("SOS", "OSO", "FARO"), setOf('S', 'O'))
        assertTrue(m in setOf("SOS", "OSO"))
    }

    @Test fun daily_null_without_letters() {
        assertNull(DailyChallenge.messageFor(LocalDate.of(2026, 11, 1), phrases, listOf("SOS"), emptySet()))
    }

    @Test fun daily_hint_levels() {
        assertNull(DailyChallenge.hint("SOS", 0))
        assertEquals("... ---", DailyChallenge.hint("SOS", 1))
        assertEquals("... --- ...", DailyChallenge.hint("SOS", 2))
        assertEquals(".- / -...", DailyChallenge.hint("A B", 2))
    }

    @Test fun real_daily_phrases_valid() {
        val real = ContentParser.dailyPhrases(File(RealContent.dir, "daily.json").readText())
        assertTrue("al menos 60: ${real.size}", real.size >= 60)
        assertEquals(real.size, real.toSet().size)
        real.forEach { p ->
            assertTrue(p, p.length in 1..24 && p == p.trim() && p == p.uppercase())
            assertTrue(p, p.all { it == ' ' || MorseAlphabet.codeFor(it) != null })
        }
        // Desde el primer capítulo ya hay varias frases posibles.
        assertTrue(real.count { p -> p.all { it == ' ' || it in early } } >= 5)
    }
}
