package com.rifttally.core.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A tap zone that fires [onClick] once on press and keeps repeating while held (after
 * [initialDelayMillis], every [repeatIntervalMillis]). Exposed to accessibility services as a button
 * labelled [contentDescription].
 */
@Composable
public fun RepeatingClickBox(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialDelayMillis: Long = REPEAT_INITIAL_DELAY_MILLIS,
    repeatIntervalMillis: Long = REPEAT_INTERVAL_MILLIS,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                onClick {
                    currentOnClick()
                    true
                }
            }
            .indication(interactionSource, LocalIndication.current)
            .pointerInput(initialDelayMillis, repeatIntervalMillis) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val press = PressInteraction.Press(down.position)
                        launch { interactionSource.emit(press) }
                        currentOnClick()
                        val repeater = launch {
                            delay(initialDelayMillis)
                            while (true) {
                                currentOnClick()
                                delay(repeatIntervalMillis)
                            }
                        }
                        val up = waitForUpOrCancellation()
                        repeater.cancel()
                        up?.consume()
                        val end = if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                        launch { interactionSource.emit(end) }
                    }
                }
            },
        contentAlignment = contentAlignment,
        content = content,
    )
}

private const val REPEAT_INITIAL_DELAY_MILLIS = 400L
private const val REPEAT_INTERVAL_MILLIS = 120L

@PreviewRiftThemes
@Composable
private fun RepeatingClickBoxPreview() {
    RiftTheme {
        Surface {
            RepeatingClickBox(
                contentDescription = "Increase",
                onClick = {},
                modifier = Modifier.size(RiftTheme.sizes.minTouchTarget * 2),
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}
