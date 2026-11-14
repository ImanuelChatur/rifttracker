package com.rifttally.app.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.feature.home.navigation.HomeRoute
import com.rifttally.feature.settings.navigation.SettingsRoute
import com.rifttally.feature.tracker.navigation.TrackerRoute
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Phase 0 "done when": the app navigates to each placeholder screen and back. */
@RunWith(AndroidJUnit4::class)
class RiftNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            RiftTheme {
                RiftNavHost(navController = navController)
            }
        }
    }

    @Test
    fun startsAtHome() {
        assertDestination<HomeRoute>()
    }

    @Test
    fun newGame_start_opensTracker_andBackReturnsHome() {
        composeTestRule.onNodeWithText("New game").performClick()
        composeTestRule.onNodeWithText("Start").performClick()
        assertDestination<TrackerRoute>()
        composeTestRule.onNodeWithText("Game: placeholder").assertIsDisplayed()

        // Setup is popped when the game starts, so Back goes straight home.
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertDestination<HomeRoute>()
    }

    @Test
    fun settings_opensAndReturns() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertDestination<SettingsRoute>()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertDestination<HomeRoute>()
    }

    private inline fun <reified T : Any> assertDestination() {
        composeTestRule.waitForIdle()
        val destination = navController.currentBackStackEntry?.destination
        assertTrue(
            "Expected ${T::class.simpleName}, was ${destination?.route}",
            destination?.hasRoute<T>() == true,
        )
    }
}
