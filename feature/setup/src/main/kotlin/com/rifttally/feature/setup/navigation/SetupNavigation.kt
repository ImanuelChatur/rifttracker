package com.rifttally.feature.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.feature.setup.SetupScreen
import kotlinx.serialization.Serializable

@Serializable
data object SetupRoute

fun NavController.navigateToSetup(navOptions: NavOptions? = null) {
    navigate(route = SetupRoute, navOptions = navOptions)
}

/** [onGameStarted] receives the id of the newly created game. */
fun NavGraphBuilder.setupScreen(onBackClick: () -> Unit, onGameStarted: (gameId: String) -> Unit) {
    composable<SetupRoute> {
        SetupScreen(
            onBackClick = onBackClick,
            onStartClick = { onGameStarted(PLACEHOLDER_GAME_ID) },
        )
    }
}

// Phase 0 only: real game ids come from the game repository in Phase 2.
private const val PLACEHOLDER_GAME_ID = "placeholder"
