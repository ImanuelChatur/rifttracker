package com.rifttally.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.core.model.GameId
import com.rifttally.feature.history.GameDetailRoute as GameDetailRouteContent
import kotlinx.serialization.Serializable

/** One finished game, [gameId]. */
@Serializable
data class GameDetailRoute(val gameId: String)

fun NavController.navigateToGameDetail(gameId: GameId, navOptions: NavOptions? = null) {
    navigate(route = GameDetailRoute(gameId.value), navOptions = navOptions)
}

fun NavGraphBuilder.gameDetailScreen(onBackClick: () -> Unit) {
    composable<GameDetailRoute> {
        GameDetailRouteContent(onBackClick = onBackClick)
    }
}
