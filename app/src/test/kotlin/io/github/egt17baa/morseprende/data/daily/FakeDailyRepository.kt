package io.github.egt17baa.morseprende.data.daily

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeDailyRepository(initial: Set<LocalDate> = emptySet()) : DailyRepository {
    override val completed = MutableStateFlow(initial)
    override suspend fun markCompleted(day: LocalDate): Boolean {
        if (day in completed.value) return false
        completed.value = completed.value + day
        return true
    }
    override suspend fun clear() { completed.value = emptySet() }
}
