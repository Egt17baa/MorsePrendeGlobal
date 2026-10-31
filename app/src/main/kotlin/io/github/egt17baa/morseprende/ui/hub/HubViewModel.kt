package io.github.egt17baa.morseprende.ui.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.SeenCardsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.LibraryCard
import io.github.egt17baa.morseprende.game.content.MissionType
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MissionNode(
    val id: String,
    val title: String,
    val type: MissionType,
    val stars: Int,
    val unlocked: Boolean,
    val isNext: Boolean,
)

data class HubUiState(
    val chapterTitle: String = "",
    val nodes: List<MissionNode> = emptyList(),
    /** Progreso del capítulo (0..1): da brillo y alcance al haz del faro. */
    val glow: Float = 0f,
    val error: Boolean = false,
    val newCards: Int = 0,
)

class HubViewModel(
    content: ContentSource,
    progress: ProgressRepository,
    seenCards: SeenCardsRepository,
) : ViewModel() {

    private val chapters: List<Chapter>? = runCatching { content.chapters() }.getOrNull()
    private val cards: List<LibraryCard> = runCatching { content.libraryCards() }.getOrDefault(emptyList())

    val state: StateFlow<HubUiState> = combine(progress.progress, seenCards.seen) { progressMap, seen ->
        val chs = chapters ?: return@combine HubUiState(error = true)
        val chapter = chs.first()
        val completed = progressMap.keys
        val next = ProgressRules.nextMission(chs, completed)
        HubUiState(
            chapterTitle = chapter.title,
            nodes = chapter.missions.map { m ->
                MissionNode(
                    id = m.id,
                    title = m.title,
                    type = m.type,
                    stars = progressMap[m.id]?.bestStars ?: 0,
                    unlocked = ProgressRules.isUnlocked(m.id, chs, completed),
                    isNext = m.id == next?.id,
                )
            },
            glow = ProgressRules.chapterProgress(chapter, completed),
            newCards = ProgressRules.unlockedCards(cards, completed).count { it.id !in seen },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, if (chapters == null) HubUiState(error = true) else HubUiState())
}
