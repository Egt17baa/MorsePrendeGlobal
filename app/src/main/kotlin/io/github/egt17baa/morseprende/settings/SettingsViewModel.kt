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
) : ViewModel() {

    val state: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val player = SignalPlayer(viewModelScope)

    fun setCharWpm(v: Int) = update { it.copy(charWpm = v, effectiveWpm = minOf(it.effectiveWpm, v)) }
    fun setEffectiveWpm(v: Int) = update { it.copy(effectiveWpm = v) }
    fun setToneHz(v: Int) = update { it.copy(toneHz = v) }
    fun setVolume(v: Float) = update { it.copy(volume = v) }
    fun setLimitFlashes(v: Boolean) = update { it.copy(limitFlashes = v) }

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
