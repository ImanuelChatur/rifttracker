package com.rifttally.feature.settings

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var backs = 0
    private val themes = mutableListOf<ThemeMode>()
    private val haptics = mutableListOf<Boolean>()
    private val deleted = mutableListOf<String>()

    private fun setContent() {
        composeTestRule.setContent {
            RiftTheme {
                SettingsScreen(
                    uiState = previewSettingsState,
                    versionName = "1.2.3",
                    actions = SettingsActions(
                        onThemeModeSelect = { themes += it },
                        onHapticsToggle = { haptics += it },
                        onPresetDelete = { deleted += it },
                    ),
                    onBackClick = { backs++ },
                )
            }
        }
    }

    @Test
    fun clickingBack_invokesCallback() {
        setContent()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_settings_back)).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun themeAndToggles_invokeCallbacks() {
        setContent()

        composeTestRule.onNodeWithText(context.getString(R.string.feature_settings_theme_light)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_settings_haptics))
            .assertIsOn()
            .performClick()

        assertEquals(listOf(ThemeMode.LIGHT), themes)
        assertEquals(listOf(false), haptics)
    }

    @Test
    fun presets_canBeDeleted_andAboutShowsVersionAndDisclaimer() {
        setContent()
        val list = composeTestRule.onNode(hasScrollAction())

        val delete = context.getString(R.string.feature_settings_preset_delete, "Ana")
        list.performScrollToNode(hasContentDescription(delete))
        composeTestRule.onNodeWithContentDescription(delete).performClick()
        assertEquals(listOf("1"), deleted)

        list.performScrollToNode(hasText(context.getString(R.string.feature_settings_disclaimer)))
        composeTestRule.onNodeWithText(context.getString(R.string.feature_settings_version, "1.2.3")).assertExists()
    }
}
