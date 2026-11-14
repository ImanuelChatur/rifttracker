package com.rifttally.core.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Smoke test for the screenshot setup: records `src/test/screenshots/rift_button_*.png`. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h120dp-xhdpi")
class RiftButtonScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun riftButton_light() = capture(darkTheme = false, name = "rift_button_light.png")

    @Test
    fun riftButton_dark() = capture(darkTheme = true, name = "rift_button_dark.png")

    private fun capture(darkTheme: Boolean, name: String) {
        composeRule.setContent {
            RiftTheme(darkTheme = darkTheme) {
                Surface {
                    RiftButton(text = "New game", onClick = {}, modifier = Modifier.padding(16.dp))
                }
            }
        }
        composeRule.onRoot().captureRoboImage(name)
    }
}
