package com.rifttally.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.feature.stats.LegendStatsRoute as LegendStatsRouteContent
import kotlinx.serialization.Serializable

/**
 * One Legend's stats and matchups. [period] is the date range's name (`StatsPeriod.name`), so the detail
 * shows the same range as the Stats screen it was opened from; anything unknown means all time.
 */
@Serializable
data class LegendStatsRoute(val legendId: String, val period: String = ALL_TIME)

fun NavController.navigateToLegendStats(route: LegendStatsRoute, navOptions: NavOptions? = null) {
    navigate(route = route, navOptions = navOptions)
}

fun NavGraphBuilder.legendStatsScreen(onBackClick: () -> Unit) {
    composable<LegendStatsRoute> {
        LegendStatsRouteContent(onBackClick = onBackClick)
    }
}

/** `StatsPeriod.ALL_TIME.name`, without exposing `:core:stats` to `:app`. */
internal const val ALL_TIME = "ALL_TIME"
