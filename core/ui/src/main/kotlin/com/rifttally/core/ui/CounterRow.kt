package com.rifttally.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.coroutines.delay

/**
 * A compact extra counter pill shown under a player's points: `−  ★ XP 3  +`. The −/+ buttons are
 * 48dp touch targets and repeat while held. Tapping or long-pressing the label turns it into a "Remove"
 * button in place (also exposed as an accessibility action); it reverts after a few seconds or on −/+.
 * It's drawn inside the pill, not in a popup window, so it turns with a rotated panel and faces its
 * player.
 *
 * @param ownerName the player's name, used in accessibility labels ("Ana, XP, increase").
 * @param dense for small panels: drops the visible label (★ and value stay; the label is still in the
 *   accessibility description) and tightens the padding, so the pill never outweighs the score.
 *   The −/+ targets stay 48dp.
 * @param containerColor the pill's (usually translucent) background; [contentColor] is drawn on it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun CounterRow(
    label: String,
    value: Int,
    ownerName: String,
    onValueChange: (delta: Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = RiftTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = RiftTheme.colorScheme.onSurface,
    dense: Boolean = false,
) {
    var armed by rememberSaveable { mutableStateOf(false) }
    if (armed) {
        LaunchedEffect(Unit) {
            delay(REMOVE_ARMED_MILLIS)
            armed = false
        }
    }
    val removeLabel = stringResource(R.string.core_ui_counter_remove)
    val description = stringResource(R.string.core_ui_counter_description, ownerName, label, value)
    val removeDescription = stringResource(R.string.core_ui_counter_remove_description, ownerName, label)
    val touch = RiftTheme.sizes.minTouchTarget
    Surface(modifier = modifier, shape = RiftTheme.shapes.full, color = containerColor, contentColor = contentColor) {
        Row(modifier = Modifier.height(touch), verticalAlignment = Alignment.CenterVertically) {
            RepeatingClickBox(
                contentDescription = stringResource(R.string.core_ui_counter_decrease, ownerName, label),
                onClick = {
                    armed = false
                    onValueChange(-1)
                },
                modifier = Modifier
                    .size(touch)
                    .clip(RiftTheme.shapes.full),
            ) {
                Icon(imageVector = RiftIcons.Remove, contentDescription = null)
            }
            if (armed) {
                Box(
                    modifier = Modifier
                        .sizeIn(minHeight = touch)
                        .clip(RiftTheme.shapes.full)
                        .combinedClickable(onClick = {
                            armed = false
                            onRemove()
                        })
                        .semantics { contentDescription = removeDescription }
                        .padding(horizontal = RiftTheme.spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    // The pill's own content color keeps the contrast the label had (DESIGN.md §3); the
                    // icon and verb mark the state.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
                    ) {
                        Icon(
                            imageVector = RiftIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                        )
                        Text(
                            text = removeLabel,
                            style = RiftTheme.typography.labelLarge,
                            maxLines = 1,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .sizeIn(minHeight = touch)
                        .clip(RiftTheme.shapes.full)
                        // Tap or long-press arms "Remove" (a tap alone would otherwise do nothing).
                        .combinedClickable(
                            onClickLabel = removeLabel,
                            onClick = { armed = true },
                            onLongClick = { armed = true },
                        )
                        .semantics {
                            contentDescription = description
                            customActions = listOf(
                                CustomAccessibilityAction(removeLabel) {
                                    onRemove()
                                    true
                                },
                            )
                        }
                        .padding(horizontal = if (dense) RiftTheme.spacing.xxs else RiftTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        if (dense) RiftTheme.spacing.xxs else RiftTheme.spacing.xs,
                    ),
                ) {
                    Icon(
                        imageVector = RiftIcons.Star,
                        contentDescription = null,
                        modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                    )
                    // The row's description already reads the label and value.
                    if (!dense) {
                        Text(
                            text = label,
                            style = RiftTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                    Text(
                        text = value.toString(),
                        style = RiftTheme.scoreTypography.counter,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
            RepeatingClickBox(
                contentDescription = stringResource(R.string.core_ui_counter_increase, ownerName, label),
                onClick = {
                    armed = false
                    onValueChange(1)
                },
                modifier = Modifier
                    .size(touch)
                    .clip(RiftTheme.shapes.full),
            ) {
                Icon(imageVector = RiftIcons.Add, contentDescription = null)
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun CounterRowPreview() {
    RiftTheme {
        Surface {
            Column(
                modifier = Modifier.padding(RiftTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                CounterRow(label = "XP", value = 3, ownerName = "Ana", onValueChange = {}, onRemove = {})
                CounterRow(label = "Energy", value = 12, ownerName = "Ana", onValueChange = {}, onRemove = {})
                CounterRow(label = "XP", value = 3, ownerName = "Ana", onValueChange = {}, onRemove = {}, dense = true)
            }
        }
    }
}

/** How long the in-place "Remove" button stays armed before the pill reverts. */
private const val REMOVE_ARMED_MILLIS = 3_000L
