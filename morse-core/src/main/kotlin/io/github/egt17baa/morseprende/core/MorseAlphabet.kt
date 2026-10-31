package io.github.egt17baa.morseprende.core

/**
 * Código Morse internacional (Recomendación UIT-R M.1677-1) más dos extensiones habituales
 * que no forman parte del estándar: la Ñ española y el signo de exclamación.
 */
object MorseAlphabet {

    val codes: Map<Char, String> = linkedMapOf(
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

    /** Caracteres incluidos que no pertenecen al estándar internacional. */
    val nonItu: Set<Char> = setOf('Ñ', '!')

    private val byCode: Map<String, Char> = codes.entries.associate { (char, code) -> code to char }

    fun codeFor(c: Char): String? = codes[c.uppercaseChar()]

    fun charFor(code: String): Char? = byCode[code]
}
