package com.rifttally.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun clickingNewGame_invokesCallback() {
        var newGameClicks = 0
        composeTestRule.setContent {
            RiftTheme {
                HomeScreen(onNewGameClick = { newGameClicks++ }, onSettingsClick = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.feature_home_new_game))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, newGameClicks)
    }

    @Test
    fun clickingSettings_invokesCallback() {
        var settingsClicks = 0
        composeTestRule.setContent {
            RiftTheme {
                HomeScreen(onNewGameClick = {}, onSettingsClick = { settingsClicks++ })
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_home_settings))
            .performClick()

        assertEquals(1, settingsClicks)
    }
}
