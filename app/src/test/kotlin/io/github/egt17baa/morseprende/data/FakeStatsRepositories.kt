package io.github.egt17baa.morseprende.data

import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecord
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeCharStatsRepository(initial: Map<Char, Int> = emptyMap()) : CharStatsRepository {
    private val state = MutableStateFlow(initial)
    val recorded = mutableListOf<List<CharOutcome>>()
    val gentleFlags = mutableListOf<Boolean>()
    override val boxes: StateFlow<Map<Char, Int>> = state
    override suspend fun record(outcomes: List<CharOutcome>, gentle: Boolean) {
        recorded += outcomes
        gentleFlags += gentle
        state.value = Leitner.apply(state.value, outcomes, gentle)
    }
}

class FakeGameRecordsRepository(initial: Map<GameId, GameRecord> = emptyMap()) : GameRecordsRepository {
    private val state = MutableStateFlow(initial)
    val games = mutableListOf<Triple<GameId, Int, Int>>()
    override val records: StateFlow<Map<GameId, GameRecord>> = state
    override suspend fun recordGame(game: GameId, score: Int, extra: Int): Boolean {
        games += Triple(game, score, extra)
        val old = state.value[game]
        val isRecord = old == null || score > old.bestScore
        state.value = state.value + (game to GameRecord(game, maxOf(score, old?.bestScore ?: 0), extra, (old?.plays ?: 0) + 1))
        return isRecord
    }
}
