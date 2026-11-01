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

    // Sin expresiones regulares a propósito: el motor de Android (ICU) no admite `(?U)` y
    // `\p{...}` falla en versiones antiguas. Char.isWhitespace() ya reconoce los espacios Unicode
    // (no separable &nbsp;, estrecho U+202F…).

    /** Mayúsculas, sin tildes ni diéresis (excepto la Ñ) y con los espacios agrupados. */
    fun normalizeText(text: String): String {
        // Teclados del móvil: apóstrofos tipográficos y los signos de apertura del español (¿ ¡),
        // que no existen en Morse.
        val typed = buildString(text.length) {
            for (c in text) {
                when (c) {
                    '\u2018', '\u2019', '\u00B4', '`', '\u02BC' -> append('\'')
                    '\u00BF', '\u00A1' -> Unit
                    else -> append(c)
                }
            }
        }
        val protected = typed.replace('ñ', ENYE_MARKER).replace('Ñ', ENYE_MARKER)
        val withoutMarks = Normalizer.normalize(protected, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
        return collapseSpaces(withoutMarks.replace(ENYE_MARKER, 'Ñ').uppercase(Locale.ROOT))
    }

    /**
     * Acepta los puntos y rayas tipográficos más comunes y separadores de palabra variados
     * ("/", "|" o tres o más espacios) y devuelve el formato canónico.
     */
    fun normalizeMorse(morse: String): String {
        val ascii = buildString(morse.length) {
            var spaces = 0
            fun flushSpaces() {
                if (spaces >= 3) append('/') else if (spaces > 0) append(' ')
                spaces = 0
            }
            for (c in morse) {
                if (c.isWhitespace()) {
                    spaces++
                    continue
                }
                flushSpaces()
                append(
                    when (c) {
                        '·', '•', '∙', '⋅' -> '.'
                        '−', '–', '—', '_', '‒' -> '-'
                        '|' -> '/'
                        else -> c
                    },
                )
            }
            flushSpaces()
        }
        return ascii.split('/')
            .map { word -> word.split(' ').filter { it.isNotEmpty() } }
            .filter { it.isNotEmpty() }
            .joinToString(WORD_SEPARATOR) { it.joinToString(" ") }
    }

    private fun collapseSpaces(text: String): String = buildString(text.length) {
        var pendingSpace = false
        for (c in text) {
            if (c.isWhitespace()) {
                pendingSpace = isNotEmpty()
            } else {
                if (pendingSpace) append(' ')
                pendingSpace = false
                append(c)
            }
        }
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
