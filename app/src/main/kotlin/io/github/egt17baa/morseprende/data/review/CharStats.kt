package io.github.egt17baa.morseprende.data.review

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "char_stats")
data class CharStatsEntity(
    @PrimaryKey val char: String,
    val box: Int,
    val correct: Int,
    val wrong: Int,
)

@Dao
interface CharStatsDao {
    @Query("SELECT * FROM char_stats")
    fun observeAll(): Flow<List<CharStatsEntity>>

    @Query("SELECT * FROM char_stats")
    suspend fun all(): List<CharStatsEntity>

    @Upsert
    suspend fun upsert(rows: List<CharStatsEntity>)

    /** Aplica las reglas de Leitner a los resultados y suma aciertos y fallos. */
    @Transaction
    suspend fun record(outcomes: List<CharOutcome>) {
        val rows = all().associateBy { it.char }.toMutableMap()
        for ((c, ok) in outcomes) {
            val key = c.toString()
            val old = rows[key] ?: CharStatsEntity(key, Leitner.MIN_BOX, 0, 0)
            rows[key] = old.copy(
                box = Leitner.next(old.box, ok),
                correct = old.correct + if (ok) 1 else 0,
                wrong = old.wrong + if (ok) 0 else 1,
            )
        }
        upsert(rows.values.toList())
    }
}

/** Cajas de Leitner por carácter (repaso inteligente). */
interface CharStatsRepository {
    val boxes: Flow<Map<Char, Int>>
    suspend fun record(outcomes: List<CharOutcome>)
}

class RoomCharStatsRepository(db: AppDatabase) : CharStatsRepository {
    private val dao = db.charStatsDao()
    override val boxes: Flow<Map<Char, Int>> = dao.observeAll().map { rows -> rows.associate { it.char.first() to it.box } }
    override suspend fun record(outcomes: List<CharOutcome>) {
        if (outcomes.isNotEmpty()) dao.record(outcomes)
    }
}
