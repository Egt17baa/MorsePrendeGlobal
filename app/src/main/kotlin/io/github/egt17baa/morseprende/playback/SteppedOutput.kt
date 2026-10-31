package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.delay

/** Salida que se enciende y apaga paso a paso (linterna, destello de pantalla). */
open class SteppedOutput(private val setOn: (Boolean) -> Unit) : SignalOutput {

    override suspend fun play(signals: List<Signal>) {
        try {
            for (signal in signals) {
                setOn(signal.on)
                delay(signal.durationMs)
            }
        } finally {
            setOn(false)
        }
    }

    override fun stop() = setOn(false)
}
