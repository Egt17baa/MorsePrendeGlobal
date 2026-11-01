package io.github.egt17baa.morseprende.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.sin

/** Timbre del pitido Morse. Todos tienen el mismo pico (±1) para que el volumen no cambie. */
enum class Timbre {
    /** Senoidal pura (de serie). */
    SINE,

    /** Triangular: más suave y redondeada. */
    SOFT,

    /** «Clásico»: armónicos 1, 3 y 5 (1, 1/3, 1/5), como un zumbador de radio antiguo pero sin aspereza. */
    CLASSIC;

    /** Valor de la onda en la fase dada (radianes), entre −1 y 1. */
    fun sample(phase: Double): Double = when (this) {
        SINE -> sin(phase)
        SOFT -> (2 / PI) * asin(sin(phase))
        CLASSIC -> (sin(phase) + sin(3 * phase) / 3 + sin(5 * phase) / 5) / CLASSIC_PEAK
    }

    private companion object {
        /** Pico de la suma de armónicos, calculado una vez para normalizar a ±1. */
        val CLASSIC_PEAK: Double = (0 until 20_000).maxOf { i ->
            val p = 2 * PI * i / 20_000
            kotlin.math.abs(sin(p) + sin(3 * p) / 3 + sin(5 * p) / 5)
        }
    }
}
