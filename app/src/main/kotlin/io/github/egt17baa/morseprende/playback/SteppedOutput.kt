package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.delay

/**
 * Salida que se enciende y apaga paso a paso (linterna, destello de pantalla).
 *
 * Cada cambio se programa contra un instante absoluto desde el inicio, no "duración tras el
 * anterior": así el tiempo que tarda [setOn] (p. ej. la llamada a la cámara) no se acumula y la
 * luz no se desfasa del sonido en mensajes largos.
 */
open class SteppedOutput(
    private val setOn: (Boolean) -> Unit,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) : SignalOutput {

    override suspend fun play(signals: List<Signal>) {
        try {
            var deadline = now()
            for (signal in signals) {
                setOn(signal.on)
                deadline += signal.durationMs
                val wait = deadline - now()
                if (wait > 0) delay(wait)
            }
        } finally {
            setOn(false)
        }
    }

    override fun stop() = setOn(false)
}
