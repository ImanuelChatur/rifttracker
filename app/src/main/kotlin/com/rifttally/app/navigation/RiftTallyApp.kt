package com.rifttally.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/** Root composable: app-wide surface plus the navigation graph. */
@Composable
fun RiftTallyApp(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        RiftNavHost(navController = navController)
    }
}
