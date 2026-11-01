package io.github.egt17baa.morseprende.playback

import android.content.Context
import io.github.egt17baa.morseprende.settings.AppSettings

/** Crea las salidas de hardware. La salida SCREEN la crea quien dibuja la pantalla. */
interface OutputFactory {
    fun create(kind: OutputKind, settings: AppSettings, onFailure: () -> Unit = {}, noise: Float = 0f): SignalOutput
}

class AndroidOutputFactory(private val context: Context) : OutputFactory {
    override fun create(kind: OutputKind, settings: AppSettings, onFailure: () -> Unit, noise: Float): SignalOutput = when (kind) {
        OutputKind.SOUND -> AudioOutput(context, settings.toneHz, settings.volume, settings.timbre, noise)
        OutputKind.VIBRATION -> VibrationOutput(context)
        OutputKind.TORCH -> TorchOutput(context, onFailure)
        OutputKind.SCREEN -> throw IllegalArgumentException("La salida SCREEN la crea el ViewModel")
    }
}
