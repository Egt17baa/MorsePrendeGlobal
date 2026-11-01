package io.github.egt17baa.morseprende.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val outputs: OutputFactory? = null,
    private val reset: io.github.egt17baa.morseprende.data.ProgressReset? = null,
) : ViewModel() {

    val state: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val player = SignalPlayer(viewModelScope)

    fun setCharWpm(v: Int) = update { it.copy(charWpm = v, effectiveWpm = minOf(it.effectiveWpm, v)) }
    fun setEffectiveWpm(v: Int) = update { it.copy(effectiveWpm = v) }
    fun setToneHz(v: Int) = update { it.copy(toneHz = v) }
    fun setVolume(v: Float) = update { it.copy(volume = v) }
    fun setLimitFlashes(v: Boolean) = update { it.copy(limitFlashes = v) }
    fun setKeyerMode(mode: KeyerMode) = update { it.copy(keyerMode = mode) }
    fun setVibration(v: Boolean) = update { it.copy(vibration = v) }
    fun setTextScale(v: Float) = update { it.copy(textScale = v) }
    fun setHighContrast(v: Boolean) = update { it.copy(highContrast = v) }

    enum class ResetState { IDLE, RUNNING, DONE }

    private val _resetState = kotlinx.coroutines.flow.MutableStateFlow(ResetState.IDLE)
    val resetState: StateFlow<ResetState> = _resetState

    /** Borra todo lo jugado (ver [io.github.egt17baa.morseprende.data.AppProgressReset]). */
    fun resetProgress() {
        val r = reset ?: return
        if (_resetState.value == ResetState.RUNNING) return
        _resetState.value = ResetState.RUNNING
        viewModelScope.launch {
            r.resetAll()
            _resetState.value = ResetState.DONE
        }
    }

    /** Reproduce "PARIS" con los ajustes actuales. */
    fun testSound() {
        val factory = outputs ?: return
        val s = state.value
        val signals = MorseTiming(s.charWpm, s.effectiveWpm).signalsFor(MorseCodec.encode("PARIS").morse)
        player.play(signals, listOf(factory.create(OutputKind.SOUND, s)))
    }

    override fun onCleared() = player.stop()

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }
}
