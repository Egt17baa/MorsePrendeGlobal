package io.github.egt17baa.morseprende.core

import java.text.Normalizer
import java.util.Locale

/**
 * Conversión texto ⇄ Morse.
 *
 * Formato del Morse: puntos y rayas ASCII, letras separadas por un espacio y palabras por " / ".
 */
object MorseCodec {

    /** Carácter que se muestra al decodificar un código que no existe. */
    const val UNKNOWN = '#'

    /** @property unsupported code points de la entrada que no tienen código Morse (p. ej. "😀"). */
    data class EncodeResult(val morse: String, val unsupported: Set<String>)

    private const val WORD_SEPARATOR = " / "
    private const val ENYE_MARKER = '\u0001'
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")
    private val wordBreakSpaces = Regex("\\s{3,}")

    /** Mayúsculas, sin tildes ni diéresis (excepto la Ñ) y con los espacios agrupados. */
    fun normalizeText(text: String): String {
        val protected = text.replace('ñ', ENYE_MARKER).replace('Ñ', ENYE_MARKER)
        val withoutMarks = Normalizer.normalize(protected, Normalizer.Form.NFD).replace(combiningMarks, "")
        return withoutMarks
            .replace(ENYE_MARKER, 'Ñ')
            .uppercase(Locale.ROOT)
            .replace(whitespace, " ")
            .trim()
    }

    /**
     * Acepta los puntos y rayas tipográficos más comunes y separadores de palabra variados
     * ("/", "|" o tres o más espacios) y devuelve el formato canónico.
     */
    fun normalizeMorse(morse: String): String {
        val ascii = buildString(morse.length) {
            for (c in morse) {
                append(
                    when (c) {
                        '·', '•', '∙', '⋅' -> '.'
                        '−', '–', '—', '_', '‒' -> '-'
                        '|' -> '/'
                        else -> c
                    },
                )
            }
        }
        return ascii.replace(wordBreakSpaces, "/")
            .split('/')
            .map { word -> word.trim().split(whitespace).filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .joinToString(WORD_SEPARATOR) { it.joinToString(" ") }
    }

    fun encode(text: String): EncodeResult {
        val unsupported = linkedSetOf<String>()
        val words = normalizeText(text).split(' ').mapNotNull { word ->
            val codes = mutableListOf<String>()
            var i = 0
            while (i < word.length) {
                val codePoint = word.codePointAt(i)
                val symbol = String(Character.toChars(codePoint))
                val code = if (symbol.length == 1) MorseAlphabet.codeFor(symbol[0]) else null
                if (code != null) codes += code else unsupported += symbol
                i += Character.charCount(codePoint)
            }
            codes.takeIf { it.isNotEmpty() }?.joinToString(" ")
        }
        return EncodeResult(words.joinToString(WORD_SEPARATOR), unsupported)
    }

    fun decode(morse: String): String {
        val normalized = normalizeMorse(morse)
        if (normalized.isEmpty()) return ""
        return normalized.split(WORD_SEPARATOR).joinToString(" ") { word ->
            word.split(' ').map { MorseAlphabet.charFor(it) ?: UNKNOWN }.joinToString("")
        }
    }
}
