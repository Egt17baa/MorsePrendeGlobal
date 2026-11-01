package io.github.egt17baa.morseprende.translator

import io.github.egt17baa.morseprende.core.Signal
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalOutput
import io.github.egt17baa.morseprende.settings.AppSettings

object NoOpOutputFactory : OutputFactory {
    override fun create(kind: OutputKind, settings: AppSettings, onFailure: () -> Unit, noise: Float): SignalOutput = object : SignalOutput {
        override suspend fun play(signals: List<Signal>) = Unit
        override fun stop() = Unit
    }
}

/** Fábrica cuyas salidas suenan hasta que se cancelan (para probar estados de reproducción en la UI). */
object EndlessOutputFactory : OutputFactory {
    override fun create(kind: OutputKind, settings: AppSettings, onFailure: () -> Unit, noise: Float): SignalOutput = object : SignalOutput {
        override suspend fun play(signals: List<Signal>) = kotlinx.coroutines.awaitCancellation()
        override fun stop() = Unit
    }
}
