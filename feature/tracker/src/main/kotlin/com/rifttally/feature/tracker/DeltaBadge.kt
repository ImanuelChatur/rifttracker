package com.rifttally.feature.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.coroutines.delay

/**
 * Briefly shows how much [value] just changed ("+1", "−2"), summing rapid taps, then fades out.
 * Purely visual: the new score itself is announced by the portrait.
 */
@Composable
internal fun DeltaBadge(value: Int, modifier: Modifier = Modifier) {
    var previous by remember { mutableIntStateOf(value) }
    var delta by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    // What the badge shows; kept while it fades out so the exit never flashes "+0".
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) {
        val change = value - previous
        previous = value
        if (change != 0) {
            delta += change
            visible = delta != 0
            if (visible) shown = delta
            // A new change restarts this effect, so the badge stays up while tapping continues.
            delay(BADGE_VISIBLE_MILLIS)
            delta = 0
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.clearAndSetSemantics {},
    ) {
        Surface(shape = CircleShape, color = RiftTheme.colorScheme.secondaryContainer) {
            Text(
                text = stringResource(R.string.feature_tracker_delta_badge, shown),
                style = RiftTheme.typography.labelLarge,
                color = RiftTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = RiftTheme.spacing.sm, vertical = RiftTheme.spacing.xxs),
            )
        }
    }
}

private const val BADGE_VISIBLE_MILLIS = 900L

@PreviewRiftThemes
@Composable
private fun DeltaBadgePreview() {
    RiftTheme {
        Surface(shape = CircleShape, color = RiftTheme.colorScheme.secondaryContainer) {
            Text(
                text = stringResource(R.string.feature_tracker_delta_badge, 2),
                style = RiftTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = RiftTheme.spacing.sm, vertical = RiftTheme.spacing.xxs),
            )
        }
    }
}
