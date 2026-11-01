package io.github.egt17baa.morseprende.game.achievements

data class AchievementInput(
    val missionStars: Map<String, Int>,
    val chapterMissionIds: List<String>,
    val stormBest: Int,
    val flashBestReactionMs: Int,
    val interceptBest: Int,
    val interceptMaxWpm: Int,
    val streak: Int,
    val boughtItems: Int,
    val seenCards: Int,
    val totalCards: Int,
)

/** Logros del faro y sus condiciones (spec §7.7). */
object Achievements {

    private val rules: List<Pair<String, (AchievementInput) -> Boolean>> = listOf(
        "first_sos" to { i -> "c1m1" in i.missionStars },
        "three_flashes" to { i -> i.missionStars.values.any { it >= 3 } },
        "keeper" to { i -> "c1f" in i.missionStars },
        "perfectionist" to { i -> i.chapterMissionIds.isNotEmpty() && i.chapterMissionIds.all { (i.missionStars[it] ?: 0) >= 3 } },
        "storm_rider" to { i -> i.stormBest >= 20 },
        "quick_eye" to { i -> i.flashBestReactionMs in 1..800 },
        "sharp_ear" to { i -> i.interceptBest >= 10 },
        "speed_20" to { i -> i.interceptMaxWpm >= 20 },
        "steady" to { i -> i.streak >= 7 },
        "collector" to { i -> i.boughtItems >= 3 },
        "reader" to { i -> i.totalCards > 0 && i.seenCards >= i.totalCards },
    )

    val ids: List<String> = rules.map { it.first }

    fun earned(input: AchievementInput): Set<String> = rules.filter { (_, rule) -> rule(input) }.map { it.first }.toSet()
}
