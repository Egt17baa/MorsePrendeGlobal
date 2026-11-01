package io.github.egt17baa.morseprende.rewards

import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.SeenCardsRepository
import io.github.egt17baa.morseprende.data.achievements.AchievementsRepository
import io.github.egt17baa.morseprende.data.activity.ActivityRepository
import io.github.egt17baa.morseprende.data.economy.EconomyRepository
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.game.achievements.AchievementInput
import io.github.egt17baa.morseprende.game.achievements.Achievements
import io.github.egt17baa.morseprende.game.activity.Streak
import io.github.egt17baa.morseprende.game.content.Mission
import io.github.egt17baa.morseprende.game.economy.Rewards
import io.github.egt17baa.morseprende.game.economy.Shop
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Lo que se ganó al terminar una misión o partida, para mostrarlo en la pantalla final. */
data class RewardSummary(val destellos: Int, val dailyBonus: Boolean, val newAchievements: List<String>)

/**
 * Al terminar una misión o partida: marca el día jugado (bono diario), suma Destellos y
 * desbloquea los logros nuevos con datos frescos.
 */
class RewardsService(
    private val content: ContentSource,
    private val progress: ProgressRepository,
    private val records: GameRecordsRepository,
    private val seenCards: SeenCardsRepository,
    private val economy: EconomyRepository,
    private val activity: ActivityRepository,
    private val achievements: AchievementsRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = System::currentTimeMillis,
) {

    suspend fun onMissionCompleted(mission: Mission, previousBest: Int, stars: Int): RewardSummary =
        reward(Rewards.forMission(mission.type, previousBest, stars))

    suspend fun onGameFinished(score: Int, isRecord: Boolean): RewardSummary =
        reward(Rewards.forGame(score, isRecord))

    /** Reevalúa logros que no dependen de jugar (comprar, leer fichas). */
    suspend fun refreshAchievements(): List<String> = unlockEarned()

    private suspend fun reward(base: Int): RewardSummary {
        val daily = activity.markPlayed(today())
        val total = base + if (daily) Rewards.DAILY_BONUS else 0
        economy.earn(total)
        return RewardSummary(total, daily, unlockEarned())
    }

    private suspend fun unlockEarned(): List<String> {
        val chapters = runCatching { content.chapters() }.getOrDefault(emptyList())
        val cards = runCatching { content.libraryCards() }.getOrDefault(emptyList())
        val stars = progress.progress.first().mapValues { it.value.bestStars }
        val recs = records.records.first()
        val owned = economy.owned.first()
        val input = AchievementInput(
            missionStars = stars,
            chapterMissionIds = chapters.firstOrNull()?.missions?.map { it.id }.orEmpty(),
            stormBest = recs[GameId.STORM]?.bestScore ?: 0,
            flashBestReactionMs = recs[GameId.FLASH]?.bestExtra ?: 0,
            interceptBest = recs[GameId.INTERCEPT]?.bestScore ?: 0,
            interceptMaxWpm = recs[GameId.INTERCEPT]?.bestExtra ?: 0,
            streak = Streak.current(activity.playedDays.first(), today()),
            boughtItems = (owned - Shop.defaults()).size,
            seenCards = seenCards.seen.first().count { id -> cards.any { it.id == id } },
            totalCards = cards.size,
        )
        val earned = Achievements.earned(input)
        val new = achievements.unlock(earned, now())
        return Achievements.ids.filter { it in new }
    }
}
