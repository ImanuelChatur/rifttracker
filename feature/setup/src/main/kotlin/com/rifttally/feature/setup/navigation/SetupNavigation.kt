package com.rifttally.feature.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.core.model.GameId
import com.rifttally.feature.setup.SetupRoute as SetupRouteContent
import kotlinx.serialization.Serializable

@Serializable
data object SetupRoute

fun NavController.navigateToSetup(navOptions: NavOptions? = null) {
    navigate(route = SetupRoute, navOptions = navOptions)
}

/** [onGameStarted] receives the id of the newly created game. */
fun NavGraphBuilder.setupScreen(onBackClick: () -> Unit, onGameStarted: (gameId: GameId) -> Unit) {
    composable<SetupRoute> {
        SetupRouteContent(onBackClick = onBackClick, onGameStart = onGameStarted)
    }
}
