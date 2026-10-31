package io.github.egt17baa.morseprende.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Genera PCM de 16 bits, mono, para un [Signal].
 *
 * Cada tono sube y baja con una envolvente de coseno alzado para que no se oigan clics. Se
 * sintetiza señal a señal para poder escribir el audio en streaming con memoria constante.
 */
object ToneSynth {

    /** Margen respecto al máximo para no saturar el altavoz. */
    private const val HEADROOM = 0.8

    fun samplesFor(durationMs: Long, sampleRate: Int): Int = (durationMs * sampleRate / 1000).toInt()

    fun render(signal: Signal, sampleRate: Int, freqHz: Int, volume: Float, rampMs: Int = 5): ShortArray {
        val count = samplesFor(signal.durationMs, sampleRate)
        val pcm = ShortArray(count)
        if (!signal.on || volume <= 0f) return pcm

        val amplitude = volume.coerceIn(0f, 1f) * HEADROOM * Short.MAX_VALUE
        val rampSamples = samplesFor(min(rampMs.toLong(), signal.durationMs / 2), sampleRate)
        val step = 2 * PI * freqHz / sampleRate
        for (i in 0 until count) {
            val distanceToEdge = min(i, count - 1 - i)
            val envelope = if (distanceToEdge < rampSamples) {
                0.5 * (1 - cos(PI * distanceToEdge / rampSamples))
            } else {
                1.0
            }
            pcm[i] = (amplitude * envelope * sin(step * i)).toInt().toShort()
        }
        return pcm
    }
}
