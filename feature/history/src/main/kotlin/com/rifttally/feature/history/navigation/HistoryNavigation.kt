package com.rifttally.feature.history.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.rifttally.core.model.GameId
import com.rifttally.feature.history.HistoryRoute as HistoryRouteContent
import kotlinx.serialization.Serializable

/** The list of ended games. */
@Serializable
data object HistoryRoute

fun NavController.navigateToHistory(navOptions: NavOptions? = null) {
    navigate(route = HistoryRoute, navOptions = navOptions)
}

/** [onGameClick] opens a game's detail (wire it to [navigateToGameDetail]). */
fun NavGraphBuilder.historyScreen(onBackClick: () -> Unit, onGameClick: (GameId) -> Unit) {
    composable<HistoryRoute> {
        HistoryRouteContent(onBackClick = onBackClick, onGameClick = onGameClick)
    }
}
