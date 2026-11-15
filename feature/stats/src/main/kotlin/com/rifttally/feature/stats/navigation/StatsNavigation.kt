package com.rifttally.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.feature.stats.StatsRoute as StatsRouteContent
import kotlinx.serialization.Serializable

/** Stats overview and Legend tab. */
@Serializable
data object StatsRoute

fun NavController.navigateToStats(navOptions: NavOptions? = null) {
    navigate(route = StatsRoute, navOptions = navOptions)
}

/**
 * [onPlayerClick] opens a player's stats (wire it to [navigateToPlayerStats]) and [onLegendClick] a
 * Legend's (wire it to [navigateToLegendStats]); both routes carry the selected date range.
 */
fun NavGraphBuilder.statsScreen(
    onBackClick: () -> Unit,
    onPlayerClick: (PlayerStatsRoute) -> Unit,
    onLegendClick: (LegendStatsRoute) -> Unit,
) {
    composable<StatsRoute> {
        StatsRouteContent(onBackClick = onBackClick, onPlayerClick = onPlayerClick, onLegendClick = onLegendClick)
    }
}
