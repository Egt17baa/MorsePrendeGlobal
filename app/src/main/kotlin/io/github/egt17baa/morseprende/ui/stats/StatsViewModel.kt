package io.github.egt17baa.morseprende.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.activity.ActivityRepository
import io.github.egt17baa.morseprende.data.daily.DailyRepository
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecord
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.game.activity.Streak
import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.progress.ProgressRules
import io.github.egt17baa.morseprende.game.review.Mastery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class LetterStat(val char: Char, val level: Mastery.Level)

data class StatsUiState(
    val loaded: Boolean = false,
    /** No se pudo cargar la campaña: no se enseñan totales engañosos (0 de 0). */
    val error: Boolean = false,
    val missionsDone: Int = 0,
    val missionsTotal: Int = 0,
    val stars: Int = 0,
    val starsMax: Int = 0,
    val dailyDone: Int = 0,
    val daysPlayed: Int = 0,
    val streak: Int = 0,
    /** Letras aprendidas, en el orden de la campaña, con su nivel. */
    val letters: List<LetterStat> = emptyList(),
    /** Las más flojas para practicar (vacío si no hay al menos 2). */
    val weakest: List<Char> = emptyList(),
    /** Solo los minijuegos ya jugados, en el orden de la lista de juegos. */
    val records: List<GameRecord> = emptyList(),
)

/** «Mi progreso»: un resumen de todo lo hecho. Solo informa; nada resta ni castiga. */
class StatsViewModel(
    content: ContentSource,
    progress: ProgressRepository,
    charStats: CharStatsRepository,
    records: GameRecordsRepository,
    daily: DailyRepository?,
    activity: ActivityRepository,
    today: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val chapters: List<Chapter>? = runCatching { content.chapters() }.getOrNull()

    private val days = combine(daily?.completed ?: flowOf(emptySet()), activity.playedDays) { done, played -> done to played }

    val state: StateFlow<StatsUiState> = combine(progress.progress, charStats.boxes, records.records, days) { progressMap, boxes, recs, (done, played) ->
        val chapters = chapters ?: return@combine StatsUiState(loaded = true, error = true)
        val missions = chapters.flatMap { it.missions }
        // Las misiones extra también cuentan, como en el logro «Farero legendario».
        val learned = ProgressRules.learnedChars(chapters, progressMap.keys).toList()
        StatsUiState(
            loaded = true,
            missionsDone = missions.count { it.id in progressMap },
            missionsTotal = missions.size,
            stars = missions.sumOf { progressMap[it.id]?.bestStars ?: 0 },
            starsMax = missions.size * 3,
            dailyDone = done.size,
            daysPlayed = played.size,
            streak = Streak.current(played, today()),
            letters = learned.map { LetterStat(it, Mastery.level(boxes[it])) },
            weakest = Mastery.weakest(learned, boxes),
            records = GameId.entries.mapNotNull { recs[it] },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())
}
