package io.github.egt17baa.morseprende.data.activity

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Días con alguna misión o partida completada (formato ISO, p. ej. "2026-11-01"). */
@Entity(tableName = "played_days")
data class PlayedDayEntity(@PrimaryKey val day: String)

@Dao
interface ActivityDao {
    @Query("SELECT day FROM played_days")
    fun observeDays(): Flow<List<String>>

    /** @return -1 si el día ya estaba. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(day: PlayedDayEntity): Long
}

interface ActivityRepository {
    val playedDays: Flow<Set<LocalDate>>

    /** @return true si es la primera actividad de ese día. */
    suspend fun markPlayed(day: LocalDate): Boolean
}

class RoomActivityRepository(db: AppDatabase) : ActivityRepository {
    private val dao = db.activityDao()
    override val playedDays: Flow<Set<LocalDate>> = dao.observeDays().map { days -> days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() }
    override suspend fun markPlayed(day: LocalDate): Boolean = dao.insert(PlayedDayEntity(day.toString())) != -1L
}
