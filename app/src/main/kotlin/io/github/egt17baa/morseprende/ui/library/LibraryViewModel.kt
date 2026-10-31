package io.github.egt17baa.morseprende.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.SeenCardsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.game.content.LibraryCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryItem(
    val id: String,
    val title: String,
    val icon: String,
    val unlocked: Boolean,
    val isNew: Boolean,
    /** Título de la misión que la desbloquea (si está bloqueada). */
    val unlockMissionTitle: String?,
)

data class LibraryUiState(
    val items: List<LibraryItem> = emptyList(),
    val opened: LibraryCard? = null,
    val error: Boolean = false,
)

class LibraryViewModel(
    content: ContentSource,
    progress: ProgressRepository,
    private val seenCards: SeenCardsRepository,
) : ViewModel() {

    private val cards: List<LibraryCard>? = runCatching { content.libraryCards() }.getOrNull()
    private val missionTitles: Map<String, String> = runCatching {
        content.chapters().flatMap { it.missions }.associate { it.id to it.title }
    }.getOrDefault(emptyMap())
    private val opened = MutableStateFlow<LibraryCard?>(null)
    private var unlockedIds: Set<String> = emptySet()

    val state: StateFlow<LibraryUiState> = combine(progress.progress, seenCards.seen, opened) { progressMap, seen, open ->
        val all = cards ?: return@combine LibraryUiState(error = true)
        val completed = progressMap.keys
        val items = all.map { card ->
            val unlocked = card.unlockedBy == null || card.unlockedBy in completed
            LibraryItem(
                id = card.id,
                title = card.title,
                icon = card.icon,
                unlocked = unlocked,
                isNew = unlocked && card.id !in seen,
                unlockMissionTitle = if (unlocked) null else card.unlockedBy?.let { missionTitles[it] ?: it },
            )
        }
        unlockedIds = items.filter { it.unlocked }.map { it.id }.toSet()
        LibraryUiState(items = items, opened = open)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    fun open(cardId: String) {
        if (cardId !in unlockedIds) return
        opened.value = cards?.firstOrNull { it.id == cardId } ?: return
        viewModelScope.launch { seenCards.markSeen(cardId) }
    }

    fun close() {
        opened.value = null
    }
}
