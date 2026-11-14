package com.rifttally.feature.tracker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme

/** Center strip between the panels: Undo · target · Menu. */
@Composable
internal fun ControlStrip(
    targetScore: Int,
    canUndo: Boolean,
    isMenuOpen: Boolean,
    onUndo: () -> Unit,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onChangeTargetClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRestartClick: () -> Unit,
    onEndGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftIconButton(
            imageVector = RiftIcons.Undo,
            contentDescription = stringResource(R.string.feature_tracker_undo),
            onClick = onUndo,
            enabled = canUndo,
        )
        Text(
            text = stringResource(R.string.feature_tracker_target_label, targetScore),
            style = RiftTheme.typography.titleMedium,
            color = RiftTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box {
            RiftIconButton(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.feature_tracker_menu),
                onClick = onMenuOpen,
            )
            DropdownMenu(expanded = isMenuOpen, onDismissRequest = onMenuDismiss) {
                MenuItem(R.string.feature_tracker_menu_target, onChangeTargetClick)
                MenuItem(R.string.feature_tracker_menu_history, onHistoryClick)
                MenuItem(R.string.feature_tracker_menu_restart, onRestartClick)
                MenuItem(R.string.feature_tracker_menu_end, onEndGameClick)
            }
        }
    }
}

@Composable
private fun MenuItem(@StringRes label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

@PreviewRiftThemes
@Composable
private fun ControlStripPreview() {
    RiftTheme {
        ControlStrip(
            targetScore = 8,
            canUndo = true,
            isMenuOpen = false,
            onUndo = {},
            onMenuOpen = {},
            onMenuDismiss = {},
            onChangeTargetClick = {},
            onHistoryClick = {},
            onRestartClick = {},
            onEndGameClick = {},
        )
    }
}
