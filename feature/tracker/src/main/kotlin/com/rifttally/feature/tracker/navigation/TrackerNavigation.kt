package com.rifttally.feature.tracker.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rifttally.feature.tracker.TrackerScreen
import kotlinx.serialization.Serializable

@Serializable
data class TrackerRoute(val gameId: String)

fun NavController.navigateToTracker(gameId: String, navOptions: NavOptions? = null) {
    navigate(route = TrackerRoute(gameId), navOptions = navOptions)
}

fun NavGraphBuilder.trackerScreen(onBackClick: () -> Unit) {
    composable<TrackerRoute> { entry ->
        TrackerScreen(
            gameId = entry.toRoute<TrackerRoute>().gameId,
            onBackClick = onBackClick,
        )
    }
}
