package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import io.github.egt17baa.morseprende.settings.AppSettings
import kotlin.math.PI
import kotlin.math.sin

/** Tono que suena mientras se mantiene pulsado el manipulador. */
interface Sidetone {
    fun keyDown()
    fun keyUp()

    /** Libera el audio. Se puede llamar varias veces; después, keyDown/keyUp no hacen nada. */
    fun close()
}

fun interface SidetoneFactory {
    fun create(settings: AppSettings): Sidetone
}

/**
 * Tono en vivo de baja latencia: un hilo escribe sin parar bloques muy cortos (96 muestras, unos
 * 2 ms) de tono o silencio en un AudioTrack con el búfer mínimo. Al pulsar o soltar, el volumen
 * sube o baja en 3 ms para que no haya clics. Si el audio no está disponible, no hace nada.
 */
class AudioSidetone(context: Context, private val freqHz: Int, private val volume: Float) : Sidetone {

    private val sampleRate: Int = context.getSystemService(AudioManager::class.java)
        ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48_000

    @Volatile var isKeyDown: Boolean = false
        private set

    @Volatile private var closed = false
    private var thread: Thread? = null
    private val lock = Any()

    override fun keyDown() {
        if (closed) return
        isKeyDown = true
        ensureRunning()
    }

    override fun keyUp() {
        isKeyDown = false
    }

    override fun close() {
        val running: Thread?
        synchronized(lock) {
            if (closed) return
            closed = true
            isKeyDown = false
            running = thread
            thread = null
        }
        running?.interrupt()
        running?.join(JOIN_TIMEOUT_MS)
    }

    private fun ensureRunning() {
        synchronized(lock) {
            if (closed || thread != null) return
            thread = Thread(::run, "sidetone").apply {
                priority = Thread.MAX_PRIORITY
                isDaemon = true
                start()
            }
        }
    }

    private fun run() {
        val track = runCatching { createTrack() }.getOrNull() ?: return
        try {
            track.play()
            val block = ShortArray(BLOCK_SAMPLES)
            val amplitude = volume.coerceIn(0f, 1f) * HEADROOM * Short.MAX_VALUE
            val rampStep = 1.0 / (RAMP_MS * sampleRate / 1000.0)
            val phaseStep = 2 * PI * freqHz / sampleRate
            var gain = 0.0
            var phase = 0.0
            while (!closed && !Thread.currentThread().isInterrupted) {
                val target = if (isKeyDown) 1.0 else 0.0
                for (i in block.indices) {
                    gain = if (gain < target) minOf(target, gain + rampStep) else maxOf(target, gain - rampStep)
                    block[i] = (amplitude * gain * sin(phase)).toInt().toShort()
                    phase += phaseStep
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                if (track.write(block, 0, block.size) <= 0) Thread.sleep(IDLE_SLEEP_MS)
            }
        } catch (_: InterruptedException) {
            // Cierre normal.
        } catch (_: RuntimeException) {
            // El audio dejó de estar disponible: el manipulador sigue funcionando sin tono.
        } finally {
            runCatching { track.pause(); track.flush() }
            track.release()
        }
    }

    private fun createTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
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
            .setBufferSizeInBytes(maxOf(minBuffer, BLOCK_SAMPLES * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private companion object {
        const val BLOCK_SAMPLES = 96
        const val RAMP_MS = 3
        const val HEADROOM = 0.8
        const val IDLE_SLEEP_MS = 2L
        const val JOIN_TIMEOUT_MS = 500L
    }
}

class AndroidSidetoneFactory(private val context: Context) : SidetoneFactory {
    override fun create(settings: AppSettings): Sidetone = AudioSidetone(context, settings.toneHz, settings.volume)
}
