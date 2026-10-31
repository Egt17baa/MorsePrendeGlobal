package io.github.egt17baa.morseprende.core

/** Un tramo de la transmisión: encendido (tono, luz, vibración) o apagado, con su duración. */
data class Signal(val on: Boolean, val durationMs: Long)
