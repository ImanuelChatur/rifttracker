package com.rifttally.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.core.model.GameId
import com.rifttally.feature.home.HomeRoute as HomeRouteContent
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(route = HomeRoute, navOptions = navOptions)
}

/**
 * Home destination.
 *
 * @param onHistoryClick opens match history (wire to `navigateToHistory()`); `null` hides the entry.
 * @param onStatsClick opens stats (wire to `navigateToStats()`); `null` hides the entry.
 * @param onRecentGameClick opens an ended game (wire to `navigateToGameDetail(gameId)`); `null` keeps
 *   recent games read-only.
 */
fun NavGraphBuilder.homeScreen(
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onRecentGameClick: ((GameId) -> Unit)? = null,
) {
    composable<HomeRoute> {
        HomeRouteContent(
            onNewGameClick = onNewGameClick,
            onResumeClick = onResumeClick,
            onSettingsClick = onSettingsClick,
            onHistoryClick = onHistoryClick,
            onStatsClick = onStatsClick,
            onRecentGameClick = onRecentGameClick,
        )
    }
}
