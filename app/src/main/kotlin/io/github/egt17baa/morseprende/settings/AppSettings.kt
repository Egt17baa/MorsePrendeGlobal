package io.github.egt17baa.morseprende.settings

import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.core.Timbre

/** Cómo se transmite en las misiones: un manipulador (duración) o dos botones (punto | raya). */
enum class KeyerMode { ONE_BUTTON, TWO_BUTTONS }

/** Preferencias del usuario. Se guardan en el dispositivo. */
data class AppSettings(
    val charWpm: Int = 18,
    val effectiveWpm: Int = 10,
    val toneHz: Int = 600,
    val volume: Float = 0.8f,
    val limitFlashes: Boolean = true,
    val flashWarningAccepted: Boolean = false,
    val keyerMode: KeyerMode = KeyerMode.ONE_BUTTON,
    /** Cosméticos equipados (ids del catálogo de la tienda). */
    val lampColor: String = "lamp_cream",
    val toneTimbre: String = "tone_sine",
    val keyerSkin: String = "key_coral",
    val decorations: Set<String> = emptySet(),
) {
    /** Timbre de sonido correspondiente al objeto equipado. */
    val timbre: Timbre
        get() = when (toneTimbre) {
            "tone_soft" -> Timbre.SOFT
            "tone_classic" -> Timbre.CLASSIC
            else -> Timbre.SINE
        }

    /** Devuelve una copia con todos los valores dentro de sus rangos válidos. */
    fun clamped(): AppSettings {
        val char = charWpm.coerceIn(MorseTiming.MIN_WPM, MorseTiming.MAX_WPM)
        return copy(
            charWpm = char,
            effectiveWpm = effectiveWpm.coerceIn(MorseTiming.MIN_WPM, char),
            toneHz = toneHz.coerceIn(MIN_TONE_HZ, MAX_TONE_HZ),
            volume = volume.coerceIn(0f, 1f),
        )
    }

    companion object {
        const val MIN_TONE_HZ = 400
        const val MAX_TONE_HZ = 1000
    }
}
