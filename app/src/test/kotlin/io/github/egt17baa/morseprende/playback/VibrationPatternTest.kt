package io.github.egt17baa.morseprende.playback

import io.github.egt17baa.morseprende.core.Signal
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class VibrationPatternTest {

    @Test fun vibration_pattern() = assertArrayEquals(
        longArrayOf(0, 60, 60, 180),
        vibrationPattern(listOf(Signal(true, 60), Signal(false, 60), Signal(true, 180))),
    )

    @Test fun vibration_pattern_empty() = assertArrayEquals(longArrayOf(), vibrationPattern(emptyList()))

    @Test fun vibration_pattern_leading_silence() = assertArrayEquals(
        longArrayOf(100, 60),
        vibrationPattern(listOf(Signal(false, 100), Signal(true, 60))),
    )
}
