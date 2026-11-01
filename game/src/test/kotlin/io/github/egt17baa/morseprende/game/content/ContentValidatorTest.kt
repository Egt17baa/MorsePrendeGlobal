package io.github.egt17baa.morseprende.game.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {

    private val ch = TestContent.chapter
    private val cards = TestContent.cards

    private fun errors(chapter: Chapter = ch, libraryCards: List<LibraryCard> = cards) =
        ContentValidator.validate(listOf(chapter), libraryCards)

    private fun List<String>.hasErrorContaining(text: String) =
        assertTrue("Se esperaba un error con '$text' en $this", any { it.contains(text) })

    private fun withMission(index: Int, transform: (Mission) -> Mission) =
        ch.copy(missions = ch.missions.mapIndexed { i, m -> if (i == index) transform(m) else m })

    @Test fun valid_content_has_no_errors() = assertEquals(emptyList<String>(), errors())

    @Test fun duplicate_mission_ids() =
        errors(withMission(2) { it.copy(id = "m1") }).hasErrorContaining("m1")

    @Test fun lesson_needs_1_to_3_new_chars() {
        errors(withMission(0) { it.copy(newChars = emptyList()) }).hasErrorContaining("m1")
        errors(withMission(0) { it.copy(newChars = listOf('S', 'O', 'E', 'T')) }).hasErrorContaining("m1")
    }

    @Test fun new_char_must_exist_in_alphabet() =
        errors(withMission(0) { it.copy(newChars = listOf('S', '#')) }).hasErrorContaining("#")

    @Test fun final_message_uses_only_learned_chars() =
        errors(withMission(0) { it.copy(finalMessage = StoryMessage("X", "SOL")) }).hasErrorContaining("L")

    @Test fun review_cannot_add_new_chars() =
        errors(withMission(1) { it.copy(newChars = listOf('E')) }).hasErrorContaining("r1")

    @Test fun words_must_be_morse_characters() =
        errors(ch.copy(words = ch.words + "CAÑÓN")).hasErrorContaining("CAÑÓN")

    @Test fun card_unlock_must_reference_mission() =
        errors(libraryCards = cards + LibraryCard("c3", "T", TestContent.body(160), "d", "nope", "book"))
            .hasErrorContaining("nope")

    @Test fun card_body_150_to_300_words() {
        errors(libraryCards = listOf(cards[0].copy(body = TestContent.body(149)))).hasErrorContaining("c1")
        errors(libraryCards = listOf(cards[0].copy(body = TestContent.body(301)))).hasErrorContaining("c1")
    }

    @Test fun duplicate_card_ids() =
        errors(libraryCards = cards + cards[0]).hasErrorContaining("c1")

    @Test fun learned_chars_accumulate() {
        assertEquals(setOf('S', 'O'), ContentValidator.learnedCharsUpTo(listOf(ch), "r1"))
        assertEquals(setOf('S', 'O', 'E', 'T'), ContentValidator.learnedCharsUpTo(listOf(ch), "m2"))
    }

    @Test fun optional_chars_not_learned_by_others() {
        val ch = ch.copy(
            missions = listOf(
                ch.missions[0],
                Mission("x", MissionType.LESSON, "Ñ", "b", listOf('Ñ'), StoryMessage("x", "Ñ"), "d", optional = true),
                Mission("m2", MissionType.LESSON, "Dos", "b", listOf('E', 'T'), StoryMessage("Gaviota", "ESTO ES SOS Ñ"), "d"),
            ),
        )
        errors(ch).hasErrorContaining("Ñ")
        assertEquals(setOf('S', 'O', 'E', 'T'), ContentValidator.learnedCharsUpTo(listOf(ch), "m2"))
        assertEquals(setOf('S', 'O', 'Ñ'), ContentValidator.learnedCharsUpTo(listOf(ch), "x"))
    }

    @Test fun noise_and_time_limit_validated() {
        errors(withMission(0) { it.copy(noise = 0.6f) }).hasErrorContaining("m1")
        errors(withMission(0) { it.copy(timeLimitSeconds = 4) }).hasErrorContaining("m1")
        errors(withMission(1) { it.copy(optional = true) }).hasErrorContaining("r1")
        assertEquals(emptyList<String>(), errors(withMission(0) { it.copy(noise = 0.5f, timeLimitSeconds = 5) }))
    }
}
