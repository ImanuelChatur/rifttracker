package com.rifttally.core.stats

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

class StatsPeriodTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun millis(zone: ZoneId, year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0) =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun game(endedAt: Long) = ffa(ANA, BEN, ended = endedAt, id = "g$endedAt")

    @Test
    fun allTime_keepsEverything_asTheSameList() {
        val games = listOf(game(1), game(Long.MAX_VALUE))

        assertSame(games, games.inPeriod(StatsPeriod.ALL_TIME, nowMillis = 0, zone = berlin))
    }

    @Test
    fun last30Days_isARolling30x24Hours_inclusiveOfTheStart() {
        val now = millis(berlin, 2026, 11, 15, 12)
        val start = now - 30L * 24 * 60 * 60 * 1000
        val games = listOf(game(start - 1), game(start), game(now), game(now + 1))

        assertEquals(
            listOf(start, now, now + 1),
            games.inPeriod(StatsPeriod.LAST_30_DAYS, now, berlin).map { it.endedAt },
        )
    }

    @Test
    fun thisYear_startsAtLocalMidnightOnJanuaryFirst() {
        val now = millis(berlin, 2026, 11, 15, 12)
        val newYear = millis(berlin, 2026, 1, 1)
        val games = listOf(game(newYear - 1), game(newYear), game(now))

        assertEquals(listOf(newYear, now), games.inPeriod(StatsPeriod.THIS_YEAR, now, berlin).map { it.endedAt })
    }

    @Test
    fun thisYear_dependsOnTheTimeZone_aroundNewYear() {
        // 2026-12-31 20:00 in Berlin is already 2027-01-01 04:00 in Tokyo.
        val now = millis(berlin, 2026, 12, 31, 20)
        val lateDecember = millis(berlin, 2026, 12, 31, 10)

        assertEquals(1, listOf(game(lateDecember)).inPeriod(StatsPeriod.THIS_YEAR, now, berlin).size)
        assertEquals(0, listOf(game(lateDecember)).inPeriod(StatsPeriod.THIS_YEAR, now, tokyo).size)
    }

    @Test
    fun emptyRange_isEmpty_andOrderIsKept() {
        val now = millis(berlin, 2026, 11, 15)
        val games = listOf(game(now), game(now - 2), game(now - 1))

        assertEquals(emptyList(), listOf(game(0)).inPeriod(StatsPeriod.LAST_30_DAYS, now, berlin))
        assertEquals(
            listOf(now, now - 2, now - 1),
            games.inPeriod(StatsPeriod.THIS_YEAR, now, berlin).map { it.endedAt },
        )
    }
}
