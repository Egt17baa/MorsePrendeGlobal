package io.github.egt17baa.morseprende.game.activity

import java.time.LocalDate
import java.time.temporal.IsoFields

/**
 * Racha suave: días seguidos con alguna misión o partida completada. Se perdona un día sin jugar
 * por semana ISO (descanso automático) y no jugar hoy todavía no la rompe. Nunca castiga.
 */
object Streak {

    fun current(playedDays: Set<LocalDate>, today: LocalDate): Int {
        val first = playedDays.minOrNull() ?: return 0
        var day = if (today in playedDays) today else today.minusDays(1)
        val restUsed = mutableSetOf<Pair<Int, Int>>()
        var count = 0
        while (!day.isBefore(first)) {
            if (day in playedDays) {
                count++
            } else {
                val week = day.get(IsoFields.WEEK_BASED_YEAR) to day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
                if (!restUsed.add(week)) break
            }
            day = day.minusDays(1)
        }
        return count
    }
}
