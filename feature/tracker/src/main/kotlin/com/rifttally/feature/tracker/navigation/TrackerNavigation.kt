package com.rifttally.feature.tracker.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.core.model.GameId
import com.rifttally.feature.tracker.TrackerRoute as TrackerRouteContent
import kotlinx.serialization.Serializable

/** The live table for game [gameId]. The id survives process death via the back stack. */
@Serializable
data class TrackerRoute(val gameId: String)

fun NavController.navigateToTracker(gameId: GameId, navOptions: NavOptions? = null) {
    navigate(route = TrackerRoute(gameId.value), navOptions = navOptions)
}

/**
 * [onExit] is called after the game ended; [onOpenGame] after a restart created a new game.
 */
fun NavGraphBuilder.trackerScreen(onBackClick: () -> Unit, onExit: () -> Unit, onOpenGame: (GameId) -> Unit) {
    composable<TrackerRoute> {
        TrackerRouteContent(onBackClick = onBackClick, onExit = onExit, onOpenGame = onOpenGame)
    }
}
