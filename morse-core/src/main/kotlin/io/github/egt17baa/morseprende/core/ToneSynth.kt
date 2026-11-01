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

    /**
     * Nivel del relleno de los silencios (unos −63 dBFS): inaudible, pero distinto de cero.
     */
    const val KEEP_ALIVE_LEVEL = 24

    /**
     * Relleno para los silencios entre pitidos. Muchos móviles apagan el amplificador del altavoz
     * cuando reciben ceros exactos y tardan en volver a encenderlo con el siguiente sonido: se
     * perdía el principio de cada letra (una S sonaba como un solo «pi») y se oían chasquidos.
     * Un susurro aleatorio de amplitud mínima, nunca cero, lo mantiene despierto.
     */
    fun keepAlive(samples: Int, random: Random = Random(7)): ShortArray = ShortArray(samples) {
        val magnitude = random.nextInt(1, KEEP_ALIVE_LEVEL + 1)
        (if (random.nextBoolean()) magnitude else -magnitude).toShort()
    }

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

        val amplitude = toneAmplitude(volume, noise)
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
     * Amplitud del tono: con ruido se reduce a `0,8 × volumen / (1 + ruido)` para que tono y ruido
     * juntos nunca pasen de 0,8 del máximo (sin saturar ni recortar).
     */
    private fun toneAmplitude(volume: Float, noise: Float): Double =
        volume.coerceIn(0f, 1f) * HEADROOM / (1 + noise.coerceIn(0f, 1f)) * Short.MAX_VALUE

    /**
     * Ruido de radio uniforme sumado a todas las muestras (también a los silencios). Su amplitud
     * es `ruido ×` la del tono, así que la relación señal/ruido no depende del volumen.
     */
    fun addNoise(pcm: ShortArray, noise: Float, volume: Float, random: Random) {
        if (noise <= 0f || volume <= 0f) return
        val amplitude = toneAmplitude(volume, noise) * noise.coerceIn(0f, 1f)
        for (i in pcm.indices) {
            val v = pcm[i] + (random.nextDouble(-1.0, 1.0) * amplitude).toInt()
            pcm[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
