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

/** [onPlayAgain] is called with the new game "Play again" created; `:app` opens it in the tracker. */
fun NavGraphBuilder.gameDetailScreen(onBackClick: () -> Unit, onPlayAgain: (GameId) -> Unit) {
    composable<GameDetailRoute> {
        GameDetailRouteContent(onBackClick = onBackClick, onPlayAgain = onPlayAgain)
    }
}
