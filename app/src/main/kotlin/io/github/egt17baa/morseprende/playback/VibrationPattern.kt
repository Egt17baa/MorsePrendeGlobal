package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal

/**
 * Convierte la línea de tiempo al formato de `VibrationEffect.createWaveform(timings, -1)`:
 * espera inicial, vibración, pausa, vibración…
 */
fun vibrationPattern(signals: List<Signal>): LongArray {
    if (signals.isEmpty()) return LongArray(0)
    val timings = mutableListOf<Long>()
    if (signals.first().on) timings += 0L
    signals.forEach { timings += it.durationMs }
    return timings.toLongArray()
}
