package io.github.egt17baa.morseprende.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Repositorio en memoria para pruebas. */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial.clamped())
    override val settings: StateFlow<AppSettings> = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value).clamped()
    }
}
