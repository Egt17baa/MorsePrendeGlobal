package io.github.egt17baa.morseprende.translator

import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.playback.OutputKind

enum class Direction { TEXT_TO_MORSE, MORSE_TO_TEXT }

data class TranslatorState(
    val input: String = "",
    val direction: Direction = Direction.TEXT_TO_MORSE,
    val output: String = "",
    val unsupported: Set<String> = emptySet(),
    val outputs: Set<OutputKind> = setOf(OutputKind.SOUND),
    val torchAvailable: Boolean = false,
    val isPlaying: Boolean = false,
    val screenLit: Boolean = false,
    val speedLimitedForFlashes: Boolean = false,
    val showFlashWarning: Boolean = false,
    val failedOutputs: Set<OutputKind> = emptySet(),
    val charWpm: Int = 18,
) {
    /** Morse que se reproduciría al pulsar Reproducir. */
    val morseToPlay: String
        get() = when (direction) {
            Direction.TEXT_TO_MORSE -> output
            Direction.MORSE_TO_TEXT -> MorseCodec.normalizeMorse(input)
        }

    val canPlay: Boolean get() = outputs.isNotEmpty() && morseToPlay.any { it == '.' || it == '-' }
}
