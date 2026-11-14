package com.rifttally.feature.setup

import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test
    fun clickingStart_invokesCallback() {
        var clicks = 0
        composeTestRule.setContent {
            RiftTheme {
                SetupScreen(onBackClick = {}, onStartClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.feature_setup_start)).performClick()

        assertEquals(1, clicks)
    }
}
