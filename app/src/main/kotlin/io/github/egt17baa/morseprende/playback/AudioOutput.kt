package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import io.github.egt17baa.morseprende.core.Signal
import io.github.egt17baa.morseprende.core.Timbre
import io.github.egt17baa.morseprende.core.ToneSynth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
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
    /** Ruido de radio (0 = limpio); también suena en los silencios. */
    val noise: Float = 0f,
) : SignalOutput {

    private val sampleRate: Int = context.getSystemService(AudioManager::class.java)
        ?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        ?.toIntOrNull()
        ?: DEFAULT_SAMPLE_RATE

    override val startLatencyMs: Long = LEAD_IN_MS

    private val lock = Any()
    /** Silencio «despierto» (ver [ToneSynth.keepAlive]); con volumen 0, ceros de verdad. */
    private val silence = if (volume > 0f) ToneSynth.keepAlive(CHUNK_SAMPLES) else ShortArray(CHUNK_SAMPLES)
    private var track: AudioTrack? = null

    override suspend fun play(signals: List<Signal>) = play(signals) {}

    override suspend fun play(signals: List<Signal>, onStarted: () -> Unit) = withContext(Dispatchers.IO) {
        val audioTrack = createTrack()
        // Un AudioTrack que no se pudo crear no suena: mejor avisar que quedarse callado.
        if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
            audioTrack.release()
            error("AudioTrack sin inicializar")
        }
        synchronized(lock) { track = audioTrack }
        var finishedNormally = false
        var framesWritten = 0L
        var watcher: kotlinx.coroutines.Job? = null
        try {
            audioTrack.play()
            check(audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) { "AudioTrack no arrancó" }
            watcher = launch { awaitAudibleStart(audioTrack); onStarted() }
            val random = kotlin.random.Random(System.nanoTime())
            for (signal in withPadding(signals)) {
                // Los silencios reutilizan un bloque de relleno casi inaudible (sin ruido): no se
                // reserva memoria por pausa y el amplificador del altavoz no se apaga entre letras.
                val pcm = if (signal.on || noise > 0f) {
                    ToneSynth.render(signal, sampleRate, freqHz, volume, timbre = timbre, noise = noise, random = random)
                } else {
                    null
                }
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
                    // 0: la pista se paró (stop()). Negativo: error de verdad (p. ej. el servicio de
                    // audio perdió la pista); antes se cortaba en silencio, ahora se avisa.
                    if (written == 0) return@withContext
                    if (written < 0) {
                        ensureActive()
                        error("AudioTrack.write devolvió $written")
                    }
                    offset += written
                    framesWritten += written
                }
            }
            // release() corta lo que queda en el búfer: antes de parar se espera, todavía
            // reproduciendo, a que suene entero. Si no, se perdía el final de la señal.
            awaitDrain(audioTrack, framesWritten)
            finishedNormally = true
        } finally {
            watcher?.cancel()
            synchronized(lock) {
                if (track === audioTrack) track = null
                runCatching {
                    if (finishedNormally) {
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

    /**
     * Espera hasta el instante en que el final del silencio inicial sale por el altavoz, es decir,
     * cuando empieza a oírse la primera señal. Lo calcula con la marca de tiempo del altavoz
     * (getTimestamp: qué muestra sonó y cuándo). Si el móvil no la da, usa la posición de
     * reproducción y, como último recurso, un tiempo máximo.
     */
    private suspend fun awaitAudibleStart(audioTrack: AudioTrack) {
        val leadFrames = ToneSynth.samplesFor(LEAD_IN_MS, sampleRate).toLong()
        val giveUp = System.nanoTime() + (LEAD_IN_MS + START_TIMEOUT_MS) * 1_000_000
        val stamp = AudioTimestamp()
        while (System.nanoTime() < giveUp) {
            if (runCatching { audioTrack.getTimestamp(stamp) }.getOrDefault(false) && stamp.framePosition > 0) {
                val at = stamp.nanoTime + (leadFrames - stamp.framePosition) * 1_000_000_000L / sampleRate
                val waitMs = (at - System.nanoTime()) / 1_000_000
                if (waitMs > 0) delay(waitMs)
                return
            }
            val head = runCatching { audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL }.getOrDefault(0L)
            if (head >= leadFrames) return
            delay(START_POLL_MS)
        }
    }

    /**
     * Espera (cancelable) a que el altavoz reproduzca todo lo escrito, como mucho el tiempo del
     * búfer más un margen, por si el móvil no avanza la posición.
     */
    private suspend fun awaitDrain(audioTrack: AudioTrack, framesWritten: Long) {
        val deadline = System.nanoTime() + (bufferMs(audioTrack) + DRAIN_MARGIN_MS) * 1_000_000
        while (System.nanoTime() < deadline) {
            val head = audioTrack.playbackHeadPosition.toLong() and 0xFFFFFFFFL
            if (head >= framesWritten) return
            delay(DRAIN_POLL_MS)
        }
    }

    private fun bufferMs(audioTrack: AudioTrack): Long =
        audioTrack.bufferSizeInFrames.toLong() * 1000 / sampleRate

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
            // Sin modo de baja latencia: al reproducir no hace falta (hay silencio inicial) y su búfer
            // más justo puede cortar el arranque en algunos móviles.
            .build()
    }

    companion object {
        const val TAIL_SILENCE_MS = 150L

        /**
         * Silencio inicial (con el relleno de [ToneSynth.keepAlive]): muchos móviles tardan
         * 100–400 ms en «despertar» el amplificador del altavoz y lo que suena en ese tiempo no se
         * oye (una S, ···, se oía como un solo punto). Con ceros exactos algunos ni despiertan.
         */
        const val LEAD_IN_MS = 400L

        /**
         * Silencio al principio (ver [LEAD_IN_MS]) y al final: así lo último que queda en el búfer
         * al llamar a stop() es silencio y no se recorta el último punto o raya.
         */
        fun withPadding(signals: List<Signal>): List<Signal> =
            listOf(Signal(false, LEAD_IN_MS)) + signals + Signal(false, TAIL_SILENCE_MS)

        private const val DEFAULT_SAMPLE_RATE = 48_000
        private const val DRAIN_MARGIN_MS = 200L
        private const val DRAIN_POLL_MS = 10L
        private const val START_POLL_MS = 5L
        /** Si en este tiempo (tras el silencio inicial) no se sabe cuándo arrancó el audio, se da por arrancado. */
        private const val START_TIMEOUT_MS = 600L
        private const val CHUNK_SAMPLES = 1_024

        /** Margen de búfer contra cortes: en el traductor la latencia no es crítica. */
        private const val BUFFER_MULTIPLIER = 4
    }
}
