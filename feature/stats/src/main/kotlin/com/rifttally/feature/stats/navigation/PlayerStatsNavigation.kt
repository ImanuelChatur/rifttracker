package com.rifttally.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.feature.stats.PlayerStatsRoute as PlayerStatsRouteContent
import kotlinx.serialization.Serializable

/**
 * One player's stats. [playerKey] is the stable key from the stats screens (saved player or name);
 * [period] is the date range's name (`StatsPeriod.name`), anything unknown means all time.
 */
@Serializable
data class PlayerStatsRoute(val playerKey: String, val period: String = ALL_TIME)

fun NavController.navigateToPlayerStats(route: PlayerStatsRoute, navOptions: NavOptions? = null) {
    navigate(route = route, navOptions = navOptions)
}

/** [onPlayerClick] opens an opponent's stats (same date range) from the head-to-head list. */
fun NavGraphBuilder.playerStatsScreen(onBackClick: () -> Unit, onPlayerClick: (PlayerStatsRoute) -> Unit) {
    composable<PlayerStatsRoute> {
        PlayerStatsRouteContent(onBackClick = onBackClick, onPlayerClick = onPlayerClick)
    }
}
