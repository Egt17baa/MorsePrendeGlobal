package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import io.github.egt17baa.morseprende.core.Signal
import io.github.egt17baa.morseprende.core.Timbre
import io.github.egt17baa.morseprende.core.ToneSynth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Pitidos Morse por el altavoz. El PCM se genera señal a señal y se escribe en streaming, de
 * modo que la memoria no crece con la longitud del mensaje.
 */
class AudioOutput(
    context: Context,
    private val freqHz: Int,
    private val volume: Float,
    private val timbre: Timbre = Timbre.SINE,
) : SignalOutput {

    private val sampleRate: Int = context.getSystemService(AudioManager::class.java)
        ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        ?.toIntOrNull()
        ?: DEFAULT_SAMPLE_RATE

    private val lock = Any()
    private val silence = ShortArray(CHUNK_SAMPLES)
    private var track: AudioTrack? = null

    override suspend fun play(signals: List<Signal>) = withContext(Dispatchers.IO) {
        val audioTrack = createTrack()
        synchronized(lock) { track = audioTrack }
        var finishedNormally = false
        try {
            audioTrack.play()
            for (signal in withTail(signals)) {
                // Los silencios reutilizan un bloque de ceros: no se reserva memoria por pausa.
                val pcm = if (signal.on) ToneSynth.render(signal, sampleRate, freqHz, volume, timbre = timbre) else null
                val total = ToneSynth.samplesFor(signal.durationMs, sampleRate)
                var offset = 0
                while (offset < total) {
                    ensureActive()
                    val count = min(CHUNK_SAMPLES, total - offset)
                    val written = if (pcm != null) {
                        audioTrack.write(pcm, offset, count)
                    } else {
                        audioTrack.write(silence, 0, count)
                    }
                    if (written <= 0) return@withContext
                    offset += written
                }
            }
            finishedNormally = true
        } finally {
            synchronized(lock) {
                if (track === audioTrack) track = null
                runCatching {
                    if (finishedNormally) {
                        // En streaming, stop() deja sonar lo que queda en el búfer.
                        audioTrack.stop()
                    } else {
                        audioTrack.pause()
                        audioTrack.flush()
                    }
                }
                audioTrack.release()
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            track?.let { runCatching { it.pause(); it.flush() } }
        }
    }

    private fun createTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(CHUNK_SAMPLES * 2) * BUFFER_MULTIPLIER
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuffer)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    companion object {
        const val TAIL_SILENCE_MS = 150L

        /**
         * Añade un silencio final: así lo último que queda en el búfer al llamar a stop() es
         * silencio y no se recorta el último punto o raya.
         */
        fun withTail(signals: List<Signal>): List<Signal> = signals + Signal(false, TAIL_SILENCE_MS)

        private const val DEFAULT_SAMPLE_RATE = 48_000
        private const val CHUNK_SAMPLES = 1_024

        /** Margen de búfer contra cortes: en el traductor la latencia no es crítica. */
        private const val BUFFER_MULTIPLIER = 4
    }
}
