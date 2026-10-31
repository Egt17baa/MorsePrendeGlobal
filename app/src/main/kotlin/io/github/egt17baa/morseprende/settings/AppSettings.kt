package io.github.egt17baa.morseprende.settings

/** Preferencias del usuario. Se guardan en el dispositivo. */
data class AppSettings(
    val charWpm: Int = 18,
    val effectiveWpm: Int = 10,
    val toneHz: Int = 600,
    val volume: Float = 0.8f,
    val limitFlashes: Boolean = true,
    val flashWarningAccepted: Boolean = false,
)
