package com.rifttally.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings in both themes, landscape and at 200% font, plus the saved-player editor (a dialog window, so
 * the whole screen is captured). Records `src/test/screenshots/settings_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_TALL)
class SettingsScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings_dark() = capture(darkTheme = true, name = "settings_dark.png")

    @Test
    fun settings_light() = capture(darkTheme = false, name = "settings_light.png")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun settings_landscape_dark() = capture(darkTheme = true, name = "settings_landscape_dark.png")

    @Test
    @Config(qualifiers = PHONE, fontScale = 2f)
    fun settings_fontScale200_dark() = capture(darkTheme = true, name = "settings_font200_dark.png")

    @Test
    @Config(qualifiers = PHONE)
    fun settings_loading_dark() =
        capture(darkTheme = true, name = "settings_loading_dark.png", state = SettingsUiState.Loading)

    @Test
    @Config(qualifiers = PHONE)
    fun presetEditor_dark() = captureEditor(darkTheme = true, name = "settings_preset_editor_dark.png")

    @Test
    @Config(qualifiers = PHONE)
    fun presetEditor_light() = captureEditor(darkTheme = false, name = "settings_preset_editor_light.png")

    @Test
    @Config(qualifiers = PHONE, fontScale = 2f)
    fun presetEditor_fontScale200_dark() =
        captureEditor(darkTheme = true, name = "settings_preset_editor_font200_dark.png")

    private fun captureEditor(darkTheme: Boolean, name: String) {
        composeRule.setContent {
            RiftTheme(darkTheme = darkTheme) {
                SettingsScreen(
                    uiState = previewSettingsState.copy(presetEditor = previewPresetEditor),
                    versionName = "2.0.0",
                    actions = SettingsActions(),
                    onBackClick = {},
                )
            }
        }
        composeRule.waitForIdle()
        captureScreenRoboImage(name)
    }

    private fun capture(darkTheme: Boolean, name: String, state: SettingsUiState = previewSettingsState) {
        composeRule.setContent {
            RiftTheme(darkTheme = darkTheme) {
                SettingsScreen(uiState = state, versionName = "2.0.0", actions = SettingsActions(), onBackClick = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(name)
    }
}

private const val PHONE = "w411dp-h891dp-xhdpi"

/** Tall enough to show the whole list in one image. */
private const val PHONE_TALL = "w411dp-h1500dp-xhdpi"
private const val LANDSCAPE = "w891dp-h411dp-xhdpi"
