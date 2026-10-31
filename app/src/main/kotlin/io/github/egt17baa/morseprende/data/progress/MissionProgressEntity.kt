package io.github.egt17baa.morseprende.data.progress

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_progress")
data class MissionProgressEntity(
    @PrimaryKey val missionId: String,
    val bestStars: Int,
    val attempts: Int,
    val lastPlayedAt: Long,
)
