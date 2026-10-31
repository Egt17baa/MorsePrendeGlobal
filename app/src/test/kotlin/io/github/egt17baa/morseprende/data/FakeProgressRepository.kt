package io.github.egt17baa.morseprende.data

import io.github.egt17baa.morseprende.data.progress.MissionProgress
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeProgressRepository(initial: Map<String, MissionProgress> = emptyMap()) : ProgressRepository {
    private val state = MutableStateFlow(initial)
    val recorded = mutableListOf<Pair<String, Int>>()
    override val progress: StateFlow<Map<String, MissionProgress>> = state

    override suspend fun recordResult(missionId: String, stars: Int, nowMs: Long) {
        recorded += missionId to stars
        val old = state.value[missionId]
        state.value = state.value + (
            missionId to MissionProgress(missionId, maxOf(stars, old?.bestStars ?: 0), (old?.attempts ?: 0) + 1, nowMs)
            )
    }

    companion object {
        fun completed(vararg stars: Pair<String, Int>) = FakeProgressRepository(
            stars.associate { (id, s) -> id to MissionProgress(id, s, 1, 0) },
        )
    }
}
