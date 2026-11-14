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

fun NavGraphBuilder.homeScreen(
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
) {
    composable<HomeRoute> {
        HomeRouteContent(
            onNewGameClick = onNewGameClick,
            onResumeClick = onResumeClick,
            onSettingsClick = onSettingsClick,
        )
    }
}
