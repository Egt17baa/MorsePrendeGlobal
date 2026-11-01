package io.github.egt17baa.morseprende.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeSeenCardsRepository(initial: Set<String> = emptySet()) : SeenCardsRepository {
    private val state = MutableStateFlow(initial)
    override val seen: StateFlow<Set<String>> = state
    override suspend fun markSeen(cardId: String) { state.value = state.value + cardId }
    override suspend fun clear() { state.value = emptySet() }
}
