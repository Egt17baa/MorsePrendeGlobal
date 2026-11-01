package io.github.egt17baa.morseprende.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.random.Random

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

    fun render(
        signal: Signal,
        sampleRate: Int,
        freqHz: Int,
        volume: Float,
        rampMs: Int = 5,
        timbre: Timbre = Timbre.SINE,
        noise: Float = 0f,
        random: Random = Random(0),
    ): ShortArray {
        val count = samplesFor(signal.durationMs, sampleRate)
        val pcm = ShortArray(count)
        if (!signal.on || volume <= 0f) {
            addNoise(pcm, noise, volume, random)
            return pcm
        }

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
            pcm[i] = (amplitude * envelope * timbre.sample(step * i)).toInt().toShort()
        }
        addNoise(pcm, noise, volume, random)
        return pcm
    }

    /**
     * Ruido de radio uniforme de amplitud `noise × 0,8 × volumen` sumado a todas las muestras
     * (también a los silencios), limitado para que nunca sature ni dé la vuelta.
     */
    fun addNoise(pcm: ShortArray, noise: Float, volume: Float, random: Random) {
        if (noise <= 0f || volume <= 0f) return
        val amplitude = noise.coerceIn(0f, 1f) * HEADROOM * volume.coerceIn(0f, 1f) * Short.MAX_VALUE
        for (i in pcm.indices) {
            val v = pcm[i] + (random.nextDouble(-1.0, 1.0) * amplitude).toInt()
            pcm[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
