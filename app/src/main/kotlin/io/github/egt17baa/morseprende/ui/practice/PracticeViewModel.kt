package io.github.egt17baa.morseprende.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecord
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PracticeGame(val id: GameId, val unlocked: Boolean, val unlockMissionTitle: String?, val record: GameRecord?)

data class PracticeUiState(val games: List<PracticeGame> = emptyList())

class PracticeViewModel(
    content: ContentSource,
    progress: ProgressRepository,
    records: GameRecordsRepository,
) : ViewModel() {

    private val missionTitles: Map<String, String> =
        runCatching { content.chapters().flatMap { it.missions }.associate { it.id to it.title } }.getOrDefault(emptyMap())

    val state: StateFlow<PracticeUiState> = combine(progress.progress, records.records) { progressMap, recordMap ->
        val completed = progressMap.keys
        PracticeUiState(
            GameId.entries.map { id ->
                val unlockId = ProgressRules.gameUnlockMission(id.name)
                val unlocked = ProgressRules.isGameUnlocked(id.name, completed)
                PracticeGame(
                    id = id,
                    unlocked = unlocked,
                    unlockMissionTitle = if (unlocked) null else unlockId?.let { missionTitles[it] ?: it },
                    record = recordMap[id],
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PracticeUiState())
}
