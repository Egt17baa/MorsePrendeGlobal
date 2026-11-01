package io.github.egt17baa.morseprende.playback

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /**
     * Como [play], y además llama a [onStarted] en el instante en que la primera señal empieza a
     * emitirse de verdad. Por defecto se supone que es tras [startLatencyMs]; el audio lo mide con el
     * reloj del altavoz, porque preparar el audio tarda distinto en cada móvil y cada vez.
     */
    suspend fun play(signals: List<Signal>, onStarted: () -> Unit) {
        coroutineScope {
            launch {
                delay(startLatencyMs)
                onStarted()
            }
            play(signals)
        }
    }

    /** Apaga la salida. Se puede llamar varias veces. */
    fun stop()
}
