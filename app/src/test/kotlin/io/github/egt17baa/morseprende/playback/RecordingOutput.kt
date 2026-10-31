package io.github.egt17baa.morseprende.playback

import kotlinx.coroutines.test.TestScope

/** Salida de prueba que graba cada cambio de estado con el instante virtual en que ocurre. */
class RecordingOutput(private val scope: TestScope) {
    val events = mutableListOf<Pair<Long, Boolean>>()
    val output = SteppedOutput { on -> events += scope.testScheduler.currentTime to on }
    val lastState: Boolean? get() = events.lastOrNull()?.second
}
