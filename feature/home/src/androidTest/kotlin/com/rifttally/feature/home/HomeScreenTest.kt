package com.rifttally.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        uiState: HomeUiState,
        onNewGameClick: () -> Unit = {},
        onResumeClick: (GameId) -> Unit = {},
        onSettingsClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            RiftTheme {
                HomeScreen(
                    uiState = uiState,
                    onNewGameClick = onNewGameClick,
                    onResumeClick = onResumeClick,
                    onSettingsClick = onSettingsClick,
                )
            }
        }
    }

    @Test
    fun clickingNewGame_invokesCallback() {
        var newGameClicks = 0
        setContent(HomeUiState.Success(activeGame = null), onNewGameClick = { newGameClicks++ })

        composeTestRule.onNodeWithText(context.getString(R.string.feature_home_new_game))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, newGameClicks)
    }

    @Test
    fun clickingSettings_invokesCallback() {
        var settingsClicks = 0
        setContent(HomeUiState.Success(activeGame = null), onSettingsClick = { settingsClicks++ })

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_home_settings))
            .performClick()

        assertEquals(1, settingsClicks)
    }

    @Test
    fun activeGame_showsResumeCard_andResumesThatGame() {
        var resumed: GameId? = null
        setContent(HomeUiState.Success(activeGame = previewActiveGame), onResumeClick = { resumed = it })

        composeTestRule.onNodeWithText("Ana").assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_home_resume)).performClick()

        assertEquals(previewActiveGame.gameId, resumed)
    }
}
