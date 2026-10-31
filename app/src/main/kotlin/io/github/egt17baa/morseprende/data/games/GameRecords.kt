package io.github.egt17baa.morseprende.data.games

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class GameId { STORM, FLASH, INTERCEPT }

/**
 * @property bestExtra Interceptación: máxima velocidad (PPM, mayor es mejor). Destello rápido:
 * mejor tiempo medio de reacción en ms (menor es mejor; 0 = sin dato).
 */
data class GameRecord(val game: GameId, val bestScore: Int, val bestExtra: Int, val plays: Int)

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey val game: String,
    val bestScore: Int,
    val bestExtra: Int,
    val plays: Int,
)

@Dao
interface GameRecordDao {
    @Query("SELECT * FROM game_records")
    fun observeAll(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records WHERE game = :game")
    suspend fun find(game: String): GameRecordEntity?

    @Upsert
    suspend fun upsert(row: GameRecordEntity)

    /** @return true si la puntuación es un récord nuevo. */
    @Transaction
    suspend fun record(game: GameId, score: Int, extra: Int): Boolean {
        val old = find(game.name)
        val isRecord = old == null || score > old.bestScore
        val bestExtra = when {
            old == null -> extra
            game == GameId.FLASH -> listOf(old.bestExtra, extra).filter { it > 0 }.minOrNull() ?: 0
            else -> maxOf(old.bestExtra, extra)
        }
        upsert(GameRecordEntity(game.name, maxOf(score, old?.bestScore ?: 0), bestExtra, (old?.plays ?: 0) + 1))
        return isRecord
    }
}

interface GameRecordsRepository {
    val records: Flow<Map<GameId, GameRecord>>

    /** Guarda la partida; devuelve true si es nuevo récord de puntuación. */
    suspend fun recordGame(game: GameId, score: Int, extra: Int): Boolean
}

class RoomGameRecordsRepository(db: AppDatabase) : GameRecordsRepository {
    private val dao = db.gameRecordDao()
    override val records: Flow<Map<GameId, GameRecord>> = dao.observeAll().map { rows ->
        rows.mapNotNull { row ->
            GameId.entries.firstOrNull { it.name == row.game }?.let { it to GameRecord(it, row.bestScore, row.bestExtra, row.plays) }
        }.toMap()
    }
    override suspend fun recordGame(game: GameId, score: Int, extra: Int): Boolean = dao.record(game, score, extra)
}
