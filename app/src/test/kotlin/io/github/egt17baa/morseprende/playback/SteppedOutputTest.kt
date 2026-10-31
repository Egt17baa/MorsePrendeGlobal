package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SteppedOutputTest {

    @Test fun stepped_toggles_at_expected_times() = runTest {
        val rec = RecordingOutput(this)
        rec.output.play(listOf(Signal(true, 60), Signal(false, 60), Signal(true, 180)))
        assertEquals(listOf(0L to true, 60L to false, 120L to true, 300L to false), rec.events)
    }

    @Test fun stepped_cancel_turns_off() = runTest {
        val rec = RecordingOutput(this)
        val job = launch { rec.output.play(listOf(Signal(true, 1000))) }
        advanceTimeBy(100)
        job.cancel()
        job.join()
        assertEquals(false, rec.lastState)
    }

    @Test fun stepped_stop_turns_off() = runTest {
        val rec = RecordingOutput(this)
        rec.output.stop()
        assertEquals(false, rec.lastState)
    }

    @Test fun stepped_does_not_drift_when_setOn_is_slow() = runTest {
        // Cada setOn "tarda" 5 ms de reloj real (llamada al servicio de cámara).
        var skew = 0L
        val now = { testScheduler.currentTime + skew }
        val seen = mutableListOf<Long>()
        val output = SteppedOutput(setOn = { seen += now(); skew += 5 }, now = now)
        output.play(listOf(Signal(true, 60), Signal(false, 60), Signal(true, 180)))
        assertEquals(listOf(0L, 60L, 120L, 300L), seen)
    }
}
