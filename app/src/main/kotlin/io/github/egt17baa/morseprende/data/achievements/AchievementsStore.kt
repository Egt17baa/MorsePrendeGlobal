package io.github.egt17baa.morseprende.data.achievements

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "achievements")
data class AchievementEntity(@PrimaryKey val id: String, val unlockedAt: Long)

@Dao
interface AchievementsDao {
    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementEntity): Long

    /** Inserta los logros y devuelve solo los que no estaban. */
    @Transaction
    suspend fun unlock(ids: Set<String>, now: Long): Set<String> =
        ids.filter { insert(AchievementEntity(it, now)) != -1L }.toSet()
}

interface AchievementsRepository {
    val unlocked: Flow<Map<String, Long>>

    /** @return solo los logros nuevos (los ya conseguidos no se vuelven a anunciar). */
    suspend fun unlock(ids: Set<String>, now: Long): Set<String>
}

class RoomAchievementsRepository(db: AppDatabase) : AchievementsRepository {
    private val dao = db.achievementsDao()
    override val unlocked: Flow<Map<String, Long>> = dao.observeAll().map { rows -> rows.associate { it.id to it.unlockedAt } }
    override suspend fun unlock(ids: Set<String>, now: Long): Set<String> = if (ids.isEmpty()) emptySet() else dao.unlock(ids, now)
}
