package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
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
}
