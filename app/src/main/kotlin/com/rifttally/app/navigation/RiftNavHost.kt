package com.rifttally.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.navOptions
import com.rifttally.feature.home.navigation.HomeRoute
import com.rifttally.feature.home.navigation.homeScreen
import com.rifttally.feature.settings.navigation.navigateToSettings
import com.rifttally.feature.settings.navigation.settingsScreen
import com.rifttally.feature.setup.navigation.SetupRoute
import com.rifttally.feature.setup.navigation.navigateToSetup
import com.rifttally.feature.setup.navigation.setupScreen
import com.rifttally.feature.tracker.navigation.navigateToTracker
import com.rifttally.feature.tracker.navigation.trackerScreen

/** Wires the feature graphs together. Features never reference each other; only `:app` does. */
@Composable
fun RiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            onNewGameClick = { navController.navigateToSetup() },
            onSettingsClick = { navController.navigateToSettings() },
        )
        setupScreen(
            onBackClick = { navController.navigateUp() },
            onGameStarted = { gameId ->
                navController.navigateToTracker(
                    gameId = gameId,
                    navOptions = navOptions { popUpTo<SetupRoute> { inclusive = true } },
                )
            },
        )
        trackerScreen(onBackClick = { navController.navigateUp() })
        settingsScreen(onBackClick = { navController.navigateUp() })
    }
}
