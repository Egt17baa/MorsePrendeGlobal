package io.github.egt17baa.morseprende.translator

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseDisplayTest {

    @Test fun elements_are_separated_by_narrow_no_break_spaces() =
        assertEquals("▬\u202F▬\u202F▬", MorseDisplay.pretty("---"))

    @Test fun letters_and_words_have_wide_gaps() =
        assertEquals("•\u202F▬   ▬     /     •", MorseDisplay.pretty(".- - / ."))

    @Test fun empty_stays_empty() = assertEquals("", MorseDisplay.pretty(""))
}
