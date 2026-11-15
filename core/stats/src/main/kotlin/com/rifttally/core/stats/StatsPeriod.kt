package com.rifttally.core.stats

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The date range the stats screens show, see [inPeriod]. */
public enum class StatsPeriod {
    ALL_TIME,

    /** The 30 × 24 hours before now. */
    LAST_30_DAYS,

    /** Since January 1st, 00:00, of the current year in the local time zone. */
    THIS_YEAR,
}

/**
 * The games of [this] that ended in [period], judged by [StatsGame.endedAt] against [nowMillis] (epoch
 * millis) in [zone]. There's no upper bound: a game that ended "after now" (the clock was turned back)
 * still counts. Order is kept.
 */
public fun List<StatsGame>.inPeriod(period: StatsPeriod, nowMillis: Long, zone: ZoneId): List<StatsGame> {
    val start = period.startMillis(nowMillis, zone) ?: return this
    return filter { it.endedAt >= start }
}

/** The first epoch milli of [this] period at [nowMillis] in [zone], or `null` for [StatsPeriod.ALL_TIME]. */
internal fun StatsPeriod.startMillis(nowMillis: Long, zone: ZoneId): Long? = when (this) {
    StatsPeriod.ALL_TIME -> null

    StatsPeriod.LAST_30_DAYS -> nowMillis - THIRTY_DAYS_MILLIS

    StatsPeriod.THIS_YEAR -> {
        val year = Instant.ofEpochMilli(nowMillis).atZone(zone).year
        LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000
