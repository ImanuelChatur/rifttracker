package com.rifttally.feature.stats.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.feature.stats.PlayerStatsRoute as PlayerStatsRouteContent
import kotlinx.serialization.Serializable

/** One player's stats. [playerKey] is the stable key from the stats screens (saved player or name). */
@Serializable
data class PlayerStatsRoute(val playerKey: String)

fun NavController.navigateToPlayerStats(playerKey: String, navOptions: NavOptions? = null) {
    navigate(route = PlayerStatsRoute(playerKey), navOptions = navOptions)
}

/** [onPlayerClick] opens an opponent's stats from the head-to-head list. */
fun NavGraphBuilder.playerStatsScreen(onBackClick: () -> Unit, onPlayerClick: (playerKey: String) -> Unit) {
    composable<PlayerStatsRoute> {
        PlayerStatsRouteContent(onBackClick = onBackClick, onPlayerClick = onPlayerClick)
    }
}
