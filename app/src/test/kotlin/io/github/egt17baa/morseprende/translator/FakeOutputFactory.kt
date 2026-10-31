package io.github.egt17baa.morseprende.translator

import io.github.egt17baa.morseprende.core.Signal
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalOutput
import io.github.egt17baa.morseprende.playback.SteppedOutput
import io.github.egt17baa.morseprende.settings.AppSettings
import kotlinx.coroutines.test.TestScope

/** Salida de prueba: guarda las señales recibidas y cada cambio de estado. */
class RecordingSignalOutput(private val scope: TestScope) : SignalOutput {
    val played = mutableListOf<List<Signal>>()
    val events = mutableListOf<Pair<Long, Boolean>>()
    private val stepped = SteppedOutput { on -> events += scope.testScheduler.currentTime to on }
    val lastState: Boolean? get() = events.lastOrNull()?.second

    override suspend fun play(signals: List<Signal>) {
        played += signals
        stepped.play(signals)
    }

    override fun stop() = stepped.stop()
}

class FakeOutputFactory(private val scope: TestScope) : OutputFactory {
    val created = mutableListOf<Pair<OutputKind, RecordingSignalOutput>>()

    override fun create(kind: OutputKind, settings: AppSettings): SignalOutput {
        require(kind != OutputKind.SCREEN) { "SCREEN la crea el ViewModel" }
        return RecordingSignalOutput(scope).also { created += kind to it }
    }
}
