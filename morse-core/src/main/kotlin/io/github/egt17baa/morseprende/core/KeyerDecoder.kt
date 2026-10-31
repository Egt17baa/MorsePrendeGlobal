package io.github.egt17baa.morseprende.core

import kotlin.math.roundToLong

sealed interface KeyerEvent {
    /** Se cerró una letra. [char] es null si el código no existe. */
    data class LetterEnded(val code: String, val char: Char?) : KeyerEvent

    data object WordEnded : KeyerEvent
}

/**
 * Convierte pulsaciones del manipulador en letras.
 *
 * - Pulsación < 2 unidades = punto; si no, raya.
 * - Silencio ≥ 2,5 unidades cierra la letra; ≥ 6 unidades, la palabra.
 * - La unidad se adapta al ritmo del jugador (media móvil, α = 0,25) entre 40 y 600 ms, para no
 *   castigar a quien empieza despacio.
 * - Las pulsaciones de menos de 15 ms se ignoran (rebotes).
 *
 * @param initialUnitMs unidad de partida; la UI usa `1200 / effectiveWpm` de los ajustes.
 */
class KeyerDecoder(initialUnitMs: Long) {

    private var unit: Double = initialUnitMs.toDouble().coerceIn(MIN_UNIT_MS.toDouble(), MAX_UNIT_MS.toDouble())
    private val elements = StringBuilder()
    private var pressedAt: Long? = null
    private var lastReleaseAt: Long? = null
    private var wordPending = false

    val unitMs: Long get() = unit.roundToLong()
    val pending: String get() = elements.toString()

    fun press(atMs: Long) {
        pressedAt = atMs
    }

    fun release(atMs: Long): Char? {
        val start = pressedAt ?: return null
        pressedAt = null
        val duration = atMs - start
        if (duration < DEBOUNCE_MS) return null

        val element = if (duration < 2 * unit) '.' else '-'
        val estimate = if (element == '.') duration.toDouble() else duration / 3.0
        unit = (unit * (1 - ALPHA) + estimate * ALPHA).coerceIn(MIN_UNIT_MS.toDouble(), MAX_UNIT_MS.toDouble())
        elements.append(element)
        lastReleaseAt = atMs
        wordPending = false
        return element
    }

    fun poll(nowMs: Long): KeyerEvent? {
        if (pressedAt != null) return null
        val since = nowMs - (lastReleaseAt ?: return null)
        if (elements.isNotEmpty()) {
            return if (since >= LETTER_GAP_UNITS * unit) closeLetter().also { wordPending = true } else null
        }
        if (wordPending && since >= WORD_GAP_UNITS * unit) {
            wordPending = false
            return KeyerEvent.WordEnded
        }
        return null
    }

    fun appendElement(element: Char) {
        require(element == '.' || element == '-') { "Elemento no válido: $element" }
        elements.append(element)
    }

    fun commitLetter(): KeyerEvent.LetterEnded? = if (elements.isEmpty()) null else closeLetter()

    fun reset() {
        elements.clear()
        pressedAt = null
        lastReleaseAt = null
        wordPending = false
    }

    private fun closeLetter(): KeyerEvent.LetterEnded {
        val code = elements.toString()
        elements.clear()
        return KeyerEvent.LetterEnded(code, MorseAlphabet.charFor(code))
    }

    companion object {
        const val MIN_UNIT_MS = 40L
        const val MAX_UNIT_MS = 600L
        const val DEBOUNCE_MS = 15L
        private const val ALPHA = 0.25
        private const val LETTER_GAP_UNITS = 2.5
        private const val WORD_GAP_UNITS = 6.0
    }
}
