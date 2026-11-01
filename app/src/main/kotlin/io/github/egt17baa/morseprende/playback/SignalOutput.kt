package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal

/** Un medio por el que se emite una línea de tiempo de señales. */
interface SignalOutput {
    /**
     * Retraso con el que esta salida empieza a emitir de verdad (p. ej. el silencio inicial del
     * audio). Las demás salidas esperan lo mismo para que todo vaya sincronizado.
     */
    val startLatencyMs: Long get() = 0L

    /** Emite [signals] y vuelve al terminar. Si se cancela, deja la salida apagada. */
    suspend fun play(signals: List<Signal>)

    /** Apaga la salida. Se puede llamar varias veces. */
    fun stop()
}
