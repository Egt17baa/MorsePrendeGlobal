package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MorseCodecTest {

    @Test fun encode_sos() = assertEquals("... --- ...", MorseCodec.encode("sos").morse)

    @Test fun encode_words() =
        assertEquals(".... --- .-.. .- / -- ..- -. -.. ---", MorseCodec.encode("Hola mundo").morse)

    @Test fun encode_strips_accents_keeps_enye() =
        assertEquals("-.-. .- -. -.-. .. --- -. / -. .. --.-- ---", MorseCodec.encode("Canción  niño").morse)

    @Test fun encode_dieresis() = assertEquals("--. ..- . .-. .-", MorseCodec.encode("güera").morse)

    @Test fun encode_reports_unsupported() {
        val result = MorseCodec.encode("Hi 😀 #")
        assertEquals(".... ..", result.morse)
        assertEquals(setOf("😀", "#"), result.unsupported)
    }

    @Test fun encode_newlines_are_word_breaks() = assertEquals(".- / -...", MorseCodec.encode("a\nb").morse)

    @Test fun encode_empty() = assertEquals("", MorseCodec.encode("   ").morse)

    @Test fun normalize_text() = assertEquals("ÑANDU CAFE", MorseCodec.normalizeText("  ñandú\t café "))

    @Test fun normalize_morse() = assertEquals(".- -... / -", MorseCodec.normalizeMorse(" ·−  −··· |  − "))

    @Test fun decode_basic() = assertEquals("SOS A", MorseCodec.decode("... --- ... / .- "))

    @Test fun decode_unicode_dashes() {
        assertEquals("#", MorseCodec.decode("·−·−"))
        assertEquals("AB", MorseCodec.decode("·− −···"))
    }

    @Test fun decode_unknown_and_garbage() = assertEquals("##I", MorseCodec.decode(".-.-.-.-. x .."))

    @Test fun decode_extra_spaces() = assertEquals("A B", MorseCodec.decode("  .-   -...  "))

    @Test fun decode_empty() = assertEquals("", MorseCodec.decode("  / "))

    @Test fun round_trip_all_characters() {
        for (c in MorseAlphabet.codes.keys) {
            assertEquals(c.toString(), MorseCodec.decode(MorseCodec.encode(c.toString()).morse))
        }
    }

    @Test fun decode_non_breaking_spaces() {
        assertEquals("AB", MorseCodec.decode(".-\u00A0-..."))
        assertEquals("AB", MorseCodec.decode(".-\u202F-..."))
        assertEquals("A B", MorseCodec.decode(".-\u00A0\u00A0\u00A0-..."))
    }

    @Test fun encode_non_breaking_space_separates_words() {
        val result = MorseCodec.encode("hola\u00A0mundo")
        assertEquals(".... --- .-.. .- / -- ..- -. -.. ---", result.morse)
        assertEquals(emptySet<String>(), result.unsupported)
    }
}
