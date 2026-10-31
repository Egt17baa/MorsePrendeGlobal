package io.github.egt17baa.morseprende.core

import kotlin.math.roundToLong

/**
 * Tiempos de transmisión.
 *
 * La unidad es `1200 / charWpm` ms (palabra de referencia "PARIS"). Si [effectiveWpm] es menor que
 * [charWpm] se aplica el espaciado Farnsworth (fórmula de ARRL): los caracteres suenan a velocidad
 * normal y se alargan solo los silencios entre caracteres y entre palabras.
 */
class MorseTiming(val charWpm: Int, val effectiveWpm: Int = charWpm) {

    init {
        require(charWpm in MIN_WPM..MAX_WPM) { "charWpm fuera de rango: $charWpm" }
        require(effectiveWpm in MIN_WPM..charWpm) { "effectiveWpm fuera de rango: $effectiveWpm" }
    }

    val unitMs: Long = (1200.0 / charWpm).roundToLong()
    val charGapMs: Long
    val wordGapMs: Long

    init {
        if (effectiveWpm == charWpm) {
            charGapMs = 3 * unitMs
            wordGapMs = 7 * unitMs
        } else {
            val c = charWpm.toDouble()
            val e = effectiveWpm.toDouble()
            val totalDelaySeconds = (60 * c - 37.2 * e) / (c * e)
            charGapMs = (3 * totalDelaySeconds / 19 * 1000).roundToLong()
            wordGapMs = (7 * totalDelaySeconds / 19 * 1000).roundToLong()
        }
    }

    /** Línea de tiempo del Morse dado. Los tokens que no son solo puntos y rayas se ignoran. */
    fun signalsFor(morse: String): List<Signal> {
        val words = MorseCodec.normalizeMorse(morse)
            .split(" / ")
            .map { word -> word.split(' ').filter { token -> token.isNotEmpty() && token.all { it == '.' || it == '-' } } }
            .filter { it.isNotEmpty() }

        val signals = mutableListOf<Signal>()
        words.forEachIndexed { wordIndex, tokens ->
            if (wordIndex > 0) signals += Signal(false, wordGapMs)
            tokens.forEachIndexed { tokenIndex, token ->
                if (tokenIndex > 0) signals += Signal(false, charGapMs)
                token.forEachIndexed { elementIndex, element ->
                    if (elementIndex > 0) signals += Signal(false, unitMs)
                    signals += Signal(true, if (element == '.') unitMs else 3 * unitMs)
                }
            }
        }
        return signals
    }

    companion object {
        const val MIN_WPM = 5
        const val MAX_WPM = 30

        fun totalMs(signals: List<Signal>): Long = signals.sumOf { it.durationMs }
    }
}
