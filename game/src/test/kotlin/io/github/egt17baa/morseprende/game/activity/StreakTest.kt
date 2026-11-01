package io.github.egt17baa.morseprende.game.activity

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    // Semana ISO: lunes 2026-11-02 … domingo 2026-11-08; la anterior: lunes 2026-10-26.
    private val mon = LocalDate.of(2026, 11, 2)
    private fun d(offset: Long) = mon.plusDays(offset)

    @Test fun empty_is_zero() = assertEquals(0, Streak.current(emptySet(), mon))
    @Test fun today_only_is_one() = assertEquals(1, Streak.current(setOf(mon), mon))
    @Test fun not_played_today_keeps_yesterday_streak() =
        assertEquals(3, Streak.current(setOf(d(0), d(1), d(2)), d(3)))
    @Test fun one_rest_day_per_week_forgiven() =
        assertEquals(4, Streak.current(setOf(d(0), d(1), d(3), d(4)), d(4)))
    @Test fun two_missing_days_same_week_break() =
        assertEquals(2, Streak.current(setOf(d(0), d(3), d(4)), d(4)))
    @Test fun rest_days_in_different_weeks() {
        // Semana anterior: jueves 29 sin jugar; esta semana: miércoles 4 sin jugar.
        val played = setOf(d(-7), d(-6), d(-5), d(-3), d(-2), d(-1), d(0), d(1), d(3))
        assertEquals(9, Streak.current(played, d(3)))
    }
    @Test fun does_not_count_before_first_day() = assertEquals(2, Streak.current(setOf(d(3), d(4)), d(4)))
    @Test fun long_gap_is_zero() = assertEquals(0, Streak.current(setOf(d(-10)), d(4)))
}
