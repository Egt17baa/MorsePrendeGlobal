package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import io.github.egt17baa.morseprende.core.Signal
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
) : SignalOutput {

    private val sampleRate: Int = context.getSystemService(AudioManager::class.java)
        ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        ?.toIntOrNull()
        ?: DEFAULT_SAMPLE_RATE

    private val lock = Any()
    private var track: AudioTrack? = null

    override suspend fun play(signals: List<Signal>) = withContext(Dispatchers.IO) {
        val audioTrack = createTrack()
        synchronized(lock) { track = audioTrack }
        var finishedNormally = false
        try {
            audioTrack.play()
            for (signal in signals) {
                val pcm = ToneSynth.render(signal, sampleRate, freqHz, volume)
                var offset = 0
                while (offset < pcm.size) {
                    ensureActive()
                    val written = audioTrack.write(pcm, offset, min(CHUNK_SAMPLES, pcm.size - offset))
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
        ).coerceAtLeast(CHUNK_SAMPLES * 2)
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

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 48_000
        const val CHUNK_SAMPLES = 1_024
    }
}
