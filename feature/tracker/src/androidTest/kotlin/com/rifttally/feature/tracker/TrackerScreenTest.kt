package com.rifttally.feature.tracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun clickingBack_invokesCallback() {
        var clicks = 0
        composeTestRule.setContent {
            RiftTheme {
                TrackerScreen(gameId = "g1", onBackClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_back)).performClick()

        assertEquals(1, clicks)
    }
}
