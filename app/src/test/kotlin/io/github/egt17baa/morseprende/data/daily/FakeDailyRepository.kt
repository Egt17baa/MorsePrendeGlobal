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
    var phrase: Pair<LocalDate, String>? = null
    override suspend fun phraseFor(day: LocalDate, choose: () -> String?): String? {
        phrase?.takeIf { it.first == day }?.let { return it.second }
        return choose()?.also { phrase = day to it }
    }
    override suspend fun clear() {
        completed.value = emptySet()
        phrase = null
    }
}
