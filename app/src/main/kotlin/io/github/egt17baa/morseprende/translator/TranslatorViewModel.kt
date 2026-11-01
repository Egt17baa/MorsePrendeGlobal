package io.github.egt17baa.morseprende.translator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.playback.SteppedOutput
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TranslatorViewModel(
    private val settingsRepo: SettingsRepository,
    private val outputs: OutputFactory,
    torchAvailable: Boolean,
    playerScope: CoroutineScope? = null,
) : ViewModel() {

    private val settings: StateFlow<AppSettings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val player = SignalPlayer(playerScope ?: viewModelScope)
    private val ui = MutableStateFlow(TranslatorState(torchAvailable = torchAvailable))
    private var pendingLightOutput: OutputKind? = null

    val state: StateFlow<TranslatorState> = combine(ui, player.isPlaying) { s, playing -> s.copy(isPlaying = playing) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ui.value)

    private val screenOutput = SteppedOutput(setOn = { lit -> ui.update { it.copy(screenLit = lit) } })

    init {
        viewModelScope.launch { settings.collect { prefs -> ui.update { it.copy(charWpm = prefs.charWpm) } } }
    }

    fun onInputChange(text: String) {
        ui.update { s ->
            val input = text.take(if (s.direction == Direction.MORSE_TO_TEXT) MAX_MORSE_INPUT else MAX_INPUT)
            when (s.direction) {
                Direction.TEXT_TO_MORSE -> {
                    val result = MorseCodec.encode(input)
                    s.copy(input = input, output = result.morse, unsupported = result.unsupported)
                }
                Direction.MORSE_TO_TEXT -> s.copy(input = input, output = MorseCodec.decode(input), unsupported = emptySet())
            }
        }
    }

    fun swapDirection() {
        val current = ui.value
        val next = if (current.direction == Direction.TEXT_TO_MORSE) Direction.MORSE_TO_TEXT else Direction.TEXT_TO_MORSE
        ui.update { it.copy(direction = next) }
        onInputChange(current.output)
    }

    fun toggleOutput(kind: OutputKind) {
        val s = ui.value
        when {
            kind == OutputKind.TORCH && !s.torchAvailable -> Unit
            kind in s.outputs -> setOutputs(s.outputs - kind)
            kind.isLight && !settings.value.flashWarningAccepted -> {
                pendingLightOutput = kind
                ui.update { it.copy(showFlashWarning = true) }
            }
            else -> setOutputs(s.outputs + kind)
        }
    }

    fun acceptFlashWarning() {
        val kind = pendingLightOutput
        pendingLightOutput = null
        ui.update { it.copy(showFlashWarning = false) }
        if (kind != null) setOutputs(ui.value.outputs + kind)
        viewModelScope.launch { settingsRepo.update { it.copy(flashWarningAccepted = true) } }
    }

    fun dismissFlashWarning() {
        pendingLightOutput = null
        ui.update { it.copy(showFlashWarning = false) }
    }

    fun play() {
        val s = ui.value
        if (!s.canPlay) return
        val prefs = settings.value
        val limited = prefs.limitFlashes && s.outputs.any { it.isLight }
        val timing = if (limited) {
            MorseTiming(minOf(prefs.charWpm, FLASH_SAFE_WPM), minOf(prefs.effectiveWpm, FLASH_SAFE_WPM))
        } else {
            MorseTiming(prefs.charWpm, prefs.effectiveWpm)
        }
        ui.update { it.copy(speedLimitedForFlashes = limited, failedOutputs = emptySet()) }
        val byOutput = s.outputs.associateBy { kind ->
            if (kind == OutputKind.SCREEN) screenOutput else outputs.create(kind, prefs, onFailure = { markFailed(kind) })
        }
        player.play(timing.signalsFor(s.morseToPlay), byOutput.keys.toList()) { failed ->
            byOutput[failed]?.let(::markFailed)
        }
    }

    /** Cambia la velocidad de las letras (la efectiva nunca la supera). */
    fun setSpeed(wpm: Int) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(charWpm = wpm, effectiveWpm = minOf(it.effectiveWpm, wpm)) }
        }
    }

    /** Cambiar las salidas mientras suena detiene la reproducción: nada sigue emitiendo a escondidas. */
    private fun setOutputs(newOutputs: Set<OutputKind>) {
        if (player.isPlaying.value) stop()
        ui.update { it.copy(outputs = newOutputs) }
    }

    private fun markFailed(kind: OutputKind) {
        ui.update { it.copy(failedOutputs = it.failedOutputs + kind) }
    }

    fun stop() = player.stop()

    fun clear() {
        stop()
        ui.update { it.copy(input = "", output = "", unsupported = emptySet(), speedLimitedForFlashes = false) }
    }

    override fun onCleared() = player.stop()

    private val OutputKind.isLight: Boolean get() = this == OutputKind.TORCH || this == OutputKind.SCREEN

    companion object {
        const val MAX_INPUT = 1000
        /** El Morse ocupa varias veces más que el texto: al invertir no se debe recortar. */
        const val MAX_MORSE_INPUT = 8000

        /** Unos 3 destellos por segundo como máximo (pauta WCAG 2.3.1). */
        const val FLASH_SAFE_WPM = 7
    }
}
