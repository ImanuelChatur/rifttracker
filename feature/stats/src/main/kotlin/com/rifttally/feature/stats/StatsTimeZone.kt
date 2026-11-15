package com.rifttally.feature.stats

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId

/**
 * The local time zone for date ranges ("This year"). Read on every use, so a time zone change shows up
 * the next time the stats are computed. Tests pass a fixed zone.
 */
internal fun interface StatsTimeZone {
    fun current(): ZoneId
}

@Module
@InstallIn(SingletonComponent::class)
internal object StatsTimeZoneModule {
    @Provides
    fun providesStatsTimeZone(): StatsTimeZone = StatsTimeZone { ZoneId.systemDefault() }
}
