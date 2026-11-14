package com.rifttally.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * A compact extra counter shown under a player's points: `XP  − 3 +`. The −/+ buttons repeat while
 * held. Long-pressing the label offers "Remove" (also exposed as an accessibility action).
 *
 * @param ownerName the player's name, used in accessibility labels ("Ana, XP, increase").
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
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val removeLabel = stringResource(R.string.core_ui_counter_remove)
    val description = stringResource(R.string.core_ui_counter_description, ownerName, label, value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .sizeIn(minHeight = RiftTheme.sizes.minTouchTarget)
                    // Tap or long-press opens the "Remove" menu (a tap alone would otherwise do nothing).
                    .combinedClickable(
                        onClickLabel = removeLabel,
                        onClick = { menuOpen = true },
                        onLongClick = { menuOpen = true },
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
                    .padding(horizontal = RiftTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = RiftTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(removeLabel) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
        RepeatingClickBox(
            contentDescription = stringResource(R.string.core_ui_counter_decrease, ownerName, label),
            onClick = { onValueChange(-1) },
            modifier = Modifier.sizeIn(
                minWidth = RiftTheme.sizes.minTouchTarget,
                minHeight = RiftTheme.sizes.minTouchTarget,
            ),
        ) {
            Icon(imageVector = RiftIcons.Remove, contentDescription = null)
        }
        // The label's description already reads the value.
        Text(
            text = value.toString(),
            style = RiftTheme.scoreTypography.counter,
            modifier = Modifier.clearAndSetSemantics {},
        )
        RepeatingClickBox(
            contentDescription = stringResource(R.string.core_ui_counter_increase, ownerName, label),
            onClick = { onValueChange(1) },
            modifier = Modifier.sizeIn(
                minWidth = RiftTheme.sizes.minTouchTarget,
                minHeight = RiftTheme.sizes.minTouchTarget,
            ),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        }
    }
}

@PreviewRiftThemes
@Composable
private fun CounterRowPreview() {
    RiftTheme {
        Surface {
            Column {
                CounterRow(label = "XP", value = 3, ownerName = "Ana", onValueChange = {}, onRemove = {})
                CounterRow(label = "Energy", value = 12, ownerName = "Ana", onValueChange = {}, onRemove = {})
            }
        }
    }
}
