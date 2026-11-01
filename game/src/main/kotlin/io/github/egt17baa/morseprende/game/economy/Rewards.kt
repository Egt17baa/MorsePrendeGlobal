package io.github.egt17baa.morseprende.game.economy

import io.github.egt17baa.morseprende.game.content.MissionType

/** Destellos que se ganan (spec §7.7). Solo sirven para cosas estéticas. */
object Rewards {
    const val MISSION_BASE = 5
    const val PER_NEW_STAR = 10
    const val REVIEW_BONUS = 5
    const val RECORD_BONUS = 15
    const val DAILY_BONUS = 5
    const val GAME_MAX = 10
    private const val POINTS_PER_DESTELLO = 5

    /** Base + 10 por cada estrella por encima de la mejor marca anterior (+5 en repasos y final). */
    fun forMission(type: MissionType, previousBest: Int, stars: Int): Int {
        val newStars = (stars - previousBest).coerceAtLeast(0)
        val bonus = if (type == MissionType.LESSON) 0 else REVIEW_BONUS
        return MISSION_BASE + PER_NEW_STAR * newStars + bonus
    }

    /** 1 por cada 5 puntos (máx. 10) + 15 por récord nuevo con puntos. */
    fun forGame(score: Int, isRecord: Boolean): Int {
        val points = minOf(GAME_MAX, score.coerceAtLeast(0) / POINTS_PER_DESTELLO)
        return points + if (isRecord && score > 0) RECORD_BONUS else 0
    }
}
