package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalPlayerTest {

    private val long = listOf(Signal(true, 1000))
    private val short = listOf(Signal(true, 100))

    /** advanceUntilIdle() no espera a backgroundScope: se avanza el reloj virtual a mano. */
    private fun kotlinx.coroutines.test.TestScope.finish() {
        advanceTimeBy(5_000)
        runCurrent()
    }

    @Test fun player_second_play_cancels_first() = runTest {
        val player = SignalPlayer(backgroundScope)
        val a = RecordingOutput(this)
        val b = RecordingOutput(this)
        player.play(long, listOf(a.output))
        advanceTimeBy(50)
        player.play(short, listOf(b.output))
        runCurrent()
        assertEquals(false, a.lastState)
        assertEquals(true, player.isPlaying.value)
        finish()
        assertEquals(false, b.lastState)
        assertEquals(false, player.isPlaying.value)
    }

    @Test fun player_stop_turns_everything_off() = runTest {
        val player = SignalPlayer(backgroundScope)
        val a = RecordingOutput(this)
        val b = RecordingOutput(this)
        player.play(long, listOf(a.output, b.output))
        advanceTimeBy(10)
        player.stop()
        runCurrent()
        assertEquals(false, a.lastState)
        assertEquals(false, b.lastState)
        assertEquals(false, player.isPlaying.value)
    }

    @Test fun player_is_playing_lifecycle() = runTest {
        val player = SignalPlayer(backgroundScope)
        val a = RecordingOutput(this)
        assertEquals(false, player.isPlaying.value)
        player.play(short, listOf(a.output))
        runCurrent()
        assertEquals(true, player.isPlaying.value)
        advanceTimeBy(50)
        assertEquals(true, player.isPlaying.value)
        finish()
        assertEquals(false, player.isPlaying.value)
    }

    @Test fun player_runs_outputs_in_parallel() = runTest {
        val player = SignalPlayer(backgroundScope)
        val a = RecordingOutput(this)
        val b = RecordingOutput(this)
        player.play(short, listOf(a.output, b.output))
        finish()
        assertEquals(listOf(0L to true, 100L to false), a.events.take(2))
        assertEquals(listOf(0L to true, 100L to false), b.events.take(2))
    }

    /** Salida cuyo cierre tarda (como el audio, que se libera en otro hilo). */
    private class SlowTeardownOutput : SignalOutput {
        override suspend fun play(signals: List<Signal>) {
            try {
                delay(signals.sumOf { it.durationMs })
            } finally {
                withContext(NonCancellable) { delay(30) }
            }
        }
        override fun stop() = Unit
    }

    @Test fun late_teardown_does_not_stop_next_playback() = runTest {
        // Dos salidas que comparten un mismo dispositivo físico (vibrador, linterna).
        var deviceOn = false
        val sharedA = SteppedOutput(setOn = { deviceOn = it }, now = { testScheduler.currentTime })
        val sharedB = SteppedOutput(setOn = { deviceOn = it }, now = { testScheduler.currentTime })
        val player = SignalPlayer(backgroundScope)
        player.play(long, listOf(SlowTeardownOutput(), sharedA))
        advanceTimeBy(50)
        player.play(long, listOf(SlowTeardownOutput(), sharedB))
        advanceTimeBy(100)
        runCurrent()
        assertEquals(true, deviceOn)
        assertEquals(true, player.isPlaying.value)
    }

    @Test fun failing_output_does_not_crash_or_stop_the_others() = runTest {
        val player = SignalPlayer(backgroundScope)
        val failures = mutableListOf<SignalOutput>()
        val broken = object : SignalOutput {
            override suspend fun play(signals: List<Signal>) = throw UnsupportedOperationException("sin audio")
            override fun stop() = Unit
        }
        val b = RecordingOutput(this)
        player.play(short, listOf(broken, b.output), onOutputFailure = { failures += it })
        advanceTimeBy(50)
        runCurrent()
        assertEquals(listOf(broken), failures)
        assertEquals(true, player.isPlaying.value)
        assertEquals(true, b.lastState)
        finish()
        assertEquals(false, b.lastState)
        assertEquals(false, player.isPlaying.value)
    }

    @Test fun other_outputs_wait_for_audio_lead_in_to_stay_in_sync() = runTest {
        val player = SignalPlayer(backgroundScope)
        val light = RecordingOutput(this)
        val audioLike = object : SignalOutput {
            override val startLatencyMs: Long = 300
            override suspend fun play(signals: List<Signal>) = delay(300 + signals.sumOf { it.durationMs })
            override fun stop() = Unit
        }
        player.play(short, listOf(audioLike, light.output))
        advanceTimeBy(299)
        runCurrent()
        assertEquals(emptyList<Pair<Long, Boolean>>(), light.events)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(300L to true), light.events)
    }

    @Test fun no_delay_without_audio() = runTest {
        val player = SignalPlayer(backgroundScope)
        val light = RecordingOutput(this)
        player.play(short, listOf(light.output))
        runCurrent()
        assertEquals(listOf(0L to true), light.events)
    }
}
