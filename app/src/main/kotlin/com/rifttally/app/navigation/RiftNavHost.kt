package com.rifttally.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.rifttally.feature.history.navigation.gameDetailScreen
import com.rifttally.feature.history.navigation.historyScreen
import com.rifttally.feature.history.navigation.navigateToGameDetail
import com.rifttally.feature.history.navigation.navigateToHistory
import com.rifttally.feature.home.navigation.HomeRoute
import com.rifttally.feature.home.navigation.homeScreen
import com.rifttally.feature.home.navigation.navigateToHome
import com.rifttally.feature.settings.navigation.navigateToSettings
import com.rifttally.feature.settings.navigation.settingsScreen
import com.rifttally.feature.setup.navigation.SetupRoute
import com.rifttally.feature.setup.navigation.navigateToSetup
import com.rifttally.feature.setup.navigation.setupScreen
import com.rifttally.feature.stats.navigation.PlayerStatsRoute
import com.rifttally.feature.stats.navigation.navigateToPlayerStats
import com.rifttally.feature.stats.navigation.navigateToStats
import com.rifttally.feature.stats.navigation.playerStatsScreen
import com.rifttally.feature.stats.navigation.statsScreen
import com.rifttally.feature.tracker.navigation.TrackerRoute
import com.rifttally.feature.tracker.navigation.navigateToTracker
import com.rifttally.feature.tracker.navigation.trackerScreen

/** Wires the feature graphs together. Features never reference each other; only `:app` does. */
@Composable
fun RiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    // Single top: a double tap must not stack two copies of a screen.
    val singleTop = navOptions { launchSingleTop = true }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            // Single top: a double tap must not stack two setups (or two trackers below).
            onNewGameClick = { navController.navigateToSetup(navOptions { launchSingleTop = true }) },
            onResumeClick = { gameId ->
                navController.navigateToTracker(gameId, navOptions { launchSingleTop = true })
            },
            onSettingsClick = { navController.navigateToSettings() },
            onHistoryClick = { navController.navigateToHistory(singleTop) },
            onStatsClick = { navController.navigateToStats(singleTop) },
            onRecentGameClick = { gameId -> navController.navigateToGameDetail(gameId, singleTop) },
        )
        setupScreen(
            onBackClick = { navController.navigateUp() },
            onGameStarted = { gameId ->
                // Back from the tracker goes Home, not to a stale setup form.
                navController.navigateToTracker(
                    gameId = gameId,
                    navOptions = navOptions { popUpTo<SetupRoute> { inclusive = true } },
                )
            },
        )
        trackerScreen(
            onBackClick = { navController.navigateUp() },
            onExit = {
                if (!navController.popBackStack<HomeRoute>(inclusive = false)) {
                    navController.navigateToHome(navOptions { popUpTo(0) })
                }
            },
            onOpenGame = { gameId ->
                navController.navigateToTracker(
                    gameId = gameId,
                    navOptions = navOptions { popUpTo<TrackerRoute> { inclusive = true } },
                )
            },
        )
        settingsScreen(onBackClick = { navController.navigateUp() })
        historyScreen(
            onBackClick = { navController.navigateUp() },
            onGameClick = { gameId -> navController.navigateToGameDetail(gameId, singleTop) },
        )
        gameDetailScreen(
            onBackClick = { navController.navigateUp() },
            // Back from the new game goes Home, not to the old game's detail.
            onPlayAgain = { gameId ->
                navController.navigateToTracker(
                    gameId = gameId,
                    navOptions = navOptions { popUpTo<HomeRoute> { inclusive = false } },
                )
            },
        )
        statsScreen(
            onBackClick = { navController.navigateUp() },
            onPlayerClick = { playerKey -> navController.navigateToPlayerStats(playerKey, singleTop) },
        )
        playerStatsScreen(
            onBackClick = { navController.navigateUp() },
            // Opponent → their stats replaces the current player, so A → B → A → … can't grow the back stack.
            onPlayerClick = { playerKey ->
                navController.navigateToPlayerStats(
                    playerKey,
                    navOptions {
                        launchSingleTop = true
                        popUpTo<PlayerStatsRoute> { inclusive = true }
                    },
                )
            },
        )
    }
}
