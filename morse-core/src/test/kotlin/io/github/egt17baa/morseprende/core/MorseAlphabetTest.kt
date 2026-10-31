package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MorseAlphabetTest {

    private val expected: Map<Char, String> = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
        'Ñ' to "--.--",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", ':' to "---...", '=' to "-...-",
        '+' to ".-.-.", '-' to "-....-", '"' to ".-..-.", '@' to ".--.-.",
    )

    @Test fun table_is_exact() = assertEquals(expected, MorseAlphabet.codes)

    @Test fun table_has_51_entries() = assertEquals(51, MorseAlphabet.codes.size)

    @Test fun codes_have_no_duplicate_codes() =
        assertEquals(MorseAlphabet.codes.size, MorseAlphabet.codes.values.toSet().size)

    @Test fun non_itu_characters() = assertEquals(setOf('Ñ', '!'), MorseAlphabet.nonItu)

    @Test fun code_for_is_case_insensitive() {
        assertEquals(".-", MorseAlphabet.codeFor('a'))
        assertEquals("--.--", MorseAlphabet.codeFor('ñ'))
        assertNull(MorseAlphabet.codeFor('#'))
    }

    @Test fun char_for_code() {
        assertEquals('S', MorseAlphabet.charFor("..."))
        assertNull(MorseAlphabet.charFor(".-.-"))
    }
}
