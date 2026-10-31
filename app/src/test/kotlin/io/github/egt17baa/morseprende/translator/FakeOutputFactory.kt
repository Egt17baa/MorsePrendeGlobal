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
    private val stepped = SteppedOutput(
        setOn = { on -> events += scope.testScheduler.currentTime to on },
        now = { scope.testScheduler.currentTime },
    )
    val lastState: Boolean? get() = events.lastOrNull()?.second

    override suspend fun play(signals: List<Signal>) {
        played += signals
        stepped.play(signals)
    }

    override fun stop() = stepped.stop()
}

class FakeOutputFactory(
    private val scope: TestScope,
    private val failing: Set<OutputKind> = emptySet(),
) : OutputFactory {
    val created = mutableListOf<Pair<OutputKind, RecordingSignalOutput>>()

    override fun create(kind: OutputKind, settings: AppSettings, onFailure: () -> Unit): SignalOutput {
        require(kind != OutputKind.SCREEN) { "SCREEN la crea el ViewModel" }
        if (kind in failing) {
            return object : SignalOutput {
                override suspend fun play(signals: List<Signal>) = onFailure()
                override fun stop() = Unit
            }
        }
        return RecordingSignalOutput(scope).also { created += kind to it }
    }
}
