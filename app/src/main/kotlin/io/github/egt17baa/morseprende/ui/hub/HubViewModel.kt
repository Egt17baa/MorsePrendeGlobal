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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import io.github.egt17baa.morseprende.data.activity.ActivityRepository
import io.github.egt17baa.morseprende.data.economy.EconomyRepository
import io.github.egt17baa.morseprende.game.activity.Streak
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import java.time.LocalDate

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
    val balance: Int = 0,
    val streak: Int = 0,
    val lampColor: String = "lamp_cream",
    val decorations: Set<String> = emptySet(),
)

class HubViewModel(
    content: ContentSource,
    progress: ProgressRepository,
    seenCards: SeenCardsRepository,
    economy: EconomyRepository? = null,
    activity: ActivityRepository? = null,
    settings: SettingsRepository? = null,
    private val today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val chapters: List<Chapter>? = runCatching { content.chapters() }.getOrNull()
    private val cards: List<LibraryCard> = runCatching { content.libraryCards() }.getOrDefault(emptyList())

    private val extras = combine(
        economy?.balance ?: flowOf(0),
        activity?.playedDays ?: flowOf(emptySet()),
        settings?.settings ?: flowOf(AppSettings()),
    ) { balance, days, prefs -> Triple(balance, Streak.current(days, today()), prefs) }

    val state: StateFlow<HubUiState> = combine(progress.progress, seenCards.seen, extras) { progressMap, seen, (balance, streak, prefs) ->
        val chs = chapters ?: return@combine HubUiState(error = true, balance = balance, streak = streak)
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
            balance = balance,
            streak = streak,
            lampColor = prefs.lampColor,
            decorations = prefs.decorations,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, if (chapters == null) HubUiState(error = true) else HubUiState())
}
