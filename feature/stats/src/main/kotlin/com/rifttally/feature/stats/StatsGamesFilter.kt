package com.rifttally.feature.stats

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.toStatsGame
import com.rifttally.core.stats.StatsGame
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.stats.inPeriod
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Turns stored games into stats input, keeping only those that ended in a [StatsPeriod] (now, local zone). */
@OptIn(ExperimentalTime::class)
internal class StatsGamesFilter @Inject constructor(private val clock: Clock, private val timeZone: StatsTimeZone) {
    fun gamesIn(records: List<GameRecord>, period: StatsPeriod): List<StatsGame> =
        records.map { it.toStatsGame() }.inPeriod(period, clock.now().toEpochMilliseconds(), timeZone.current())
}

/** The [StatsPeriod] named [name] (a route argument), or all time for anything unknown. */
internal fun statsPeriodOf(name: String?): StatsPeriod =
    StatsPeriod.entries.firstOrNull { it.name == name } ?: StatsPeriod.ALL_TIME
