package io.github.egt17baa.morseprende.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MissionProgress(val missionId: String, val bestStars: Int, val attempts: Int, val lastPlayedAt: Long)

interface ProgressRepository {
    val progress: Flow<Map<String, MissionProgress>>

    /** Guarda max(estrellas) y suma un intento. */
    suspend fun recordResult(missionId: String, stars: Int, nowMs: Long)
}

class RoomProgressRepository(db: AppDatabase) : ProgressRepository {
    private val dao = db.progressDao()

    override val progress: Flow<Map<String, MissionProgress>> = dao.observeAll().map { rows ->
        rows.associate { it.missionId to MissionProgress(it.missionId, it.bestStars, it.attempts, it.lastPlayedAt) }
    }

    override suspend fun recordResult(missionId: String, stars: Int, nowMs: Long) = dao.record(missionId, stars, nowMs)
}
