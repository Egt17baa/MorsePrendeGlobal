package io.github.egt17baa.morseprende.data.progress

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM mission_progress")
    fun observeAll(): Flow<List<MissionProgressEntity>>

    @Query("SELECT * FROM mission_progress WHERE missionId = :missionId")
    suspend fun find(missionId: String): MissionProgressEntity?

    @Upsert
    suspend fun upsert(entity: MissionProgressEntity)

    /** Guarda el resultado conservando la mejor marca. */
    @Transaction
    suspend fun record(missionId: String, stars: Int, nowMs: Long) {
        val old = find(missionId)
        upsert(
            MissionProgressEntity(
                missionId = missionId,
                bestStars = maxOf(stars, old?.bestStars ?: 0),
                attempts = (old?.attempts ?: 0) + 1,
                lastPlayedAt = nowMs,
            ),
        )
    }
}
