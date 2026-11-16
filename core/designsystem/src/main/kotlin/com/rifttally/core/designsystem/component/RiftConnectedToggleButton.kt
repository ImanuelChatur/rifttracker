package com.rifttally.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled

/**
 * One option of a single-choice *connected button group* (M3 Expressive `ButtonGroup`, connected
 * variant; it is in material3 1.5.0 only, so this mirrors its look, DESIGN.md §1, §4, §9).
 *
 * Put the options in a `Row(Modifier.selectableGroup(), spacedBy(RiftTheme.spacing.xxs))`, each with
 * `Modifier.weight(1f)`. The group's outer ends are pills; inner corners are small (8dp). The selected
 * option is filled in the secondary color, shows a check and morphs into a full pill on the expressive
 * fast spatial spring (static with reduce motion), so selection reads by color, shape and icon, not by
 * color alone. At least 48dp tall; the label wraps at large font scales.
 *
 * @param contentDescription replaces the spoken label when [text] is terse (e.g. "4" → "4 players").
 * @param index this option's position in the group, and [count] the number of options (for the corners).
 */
@Composable
public fun RiftConnectedToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val reducedMotion = reduceMotionEnabled()
    val motion = RiftTheme.motion.expressive
    val innerPercent by animateFloatAsState(
        targetValue = if (selected) FULL_PERCENT else INNER_PERCENT,
        animationSpec = if (reducedMotion) RiftTheme.motion.none() else motion.fastSpatial(),
        label = "connectedCorners",
    )
    val container by animateColorAsState(
        targetValue = if (selected) RiftTheme.colorScheme.secondary else RiftTheme.colorScheme.surfaceContainerHighest,
        animationSpec = if (reducedMotion) RiftTheme.motion.none() else motion.fastEffects(),
        label = "connectedContainer",
    )
    val content = if (selected) RiftTheme.colorScheme.onSecondary else RiftTheme.colorScheme.onSurface
    val inner = CornerSize(innerPercent.coerceIn(0f, FULL_PERCENT))
    val outer = CornerSize(FULL_PERCENT)
    val start = if (index == 0) outer else inner
    val end = if (index == count - 1) outer else inner
    val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
    Surface(
        shape = shape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        // Selectable inside the Surface, so the ripple is clipped to the (morphing) shape.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RiftTheme.sizes.minTouchTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = RiftTheme.spacing.sm, vertical = RiftTheme.spacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = RiftIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(RiftTheme.spacing.xs))
            }
            Text(text = text, style = RiftTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}

/** The pill end of a connected group, as a percentage of the button's height. */
private const val FULL_PERCENT = 50f

/** ≈ 8dp inner corners on a 48dp button. */
private const val INNER_PERCENT = 17f

@PreviewRiftThemes
@Composable
private fun RiftConnectedToggleButtonPreview() {
    RiftTheme {
        Surface {
            Column(modifier = Modifier.padding(RiftTheme.spacing.lg)) {
                var selected by remember { mutableIntStateOf(1) }
                val labels = listOf("2", "3", "4", "2v2")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
                ) {
                    labels.forEachIndexed { index, label ->
                        RiftConnectedToggleButton(
                            text = label,
                            selected = index == selected,
                            onClick = { selected = index },
                            index = index,
                            count = labels.size,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
