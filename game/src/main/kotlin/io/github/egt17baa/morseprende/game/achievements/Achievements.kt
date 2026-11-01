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
    /** «Mensajes del día» descifrados en total. */
    val dailyDone: Int = 0,
    /** Récord de la Guardia nocturna y hora más tardía alcanzada (0 = 22:00). */
    val nightBest: Int = 0,
    val nightHour: Int = 0,
    /** Todas las misiones de la campaña (los tres capítulos). */
    val allMissionIds: List<String> = emptyList(),
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
        "mole_hunter" to { i -> "c2f" in i.missionStars },
        "stargazer" to { i -> "c3f" in i.missionStars },
        "daily_first" to { i -> i.dailyDone >= 1 },
        "daily_week" to { i -> i.dailyDone >= 7 },
        "night_owl" to { i -> i.nightBest >= 15 },
        "dawn" to { i -> i.nightHour >= 8 },
        "enye" to { i -> "c2x" in i.missionStars },
        "completionist" to { i -> i.allMissionIds.isNotEmpty() && i.allMissionIds.all { (i.missionStars[it] ?: 0) >= 3 } },
    )

    val ids: List<String> = rules.map { it.first }

    fun earned(input: AchievementInput): Set<String> = rules.filter { (_, rule) -> rule(input) }.map { it.first }.toSet()
}
