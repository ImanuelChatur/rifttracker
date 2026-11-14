package com.rifttally.feature.setup

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
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var starts = 0
    private var legendClicks = mutableListOf<Int>()
    private var targetIncreases = 0

    private fun setContent(uiState: SetupUiState = previewSetupState) {
        composeTestRule.setContent {
            RiftTheme {
                SetupScreen(
                    uiState = uiState,
                    onBackClick = {},
                    onNameChange = { _, _ -> },
                    onColorSelect = { _, _ -> },
                    onLegendClick = { legendClicks += it },
                    onTargetIncrease = { targetIncreases++ },
                    onTargetDecrease = {},
                    onStartClick = { starts++ },
                )
            }
        }
    }

    @Test
    fun clickingStart_invokesCallback() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.feature_setup_start)).performClick()

        assertEquals(1, starts)
    }

    @Test
    fun blankName_showsDefaultName_andLegendButtonOpensPickerForThatPlayer() {
        setContent()

        val label = context.getString(
            R.string.feature_setup_choose_legend,
            context.getString(R.string.feature_setup_default_player_name, 2),
        )
        composeTestRule.onNodeWithContentDescription(label).performClick()

        assertEquals(listOf(1), legendClicks)
    }

    @Test
    fun targetScore_isShown_andIncreaseInvokesCallback() {
        setContent()

        composeTestRule.onNodeWithText("8").assertExists()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_setup_target_increase))
            .performClick()

        assertEquals(1, targetIncreases)
    }
}
