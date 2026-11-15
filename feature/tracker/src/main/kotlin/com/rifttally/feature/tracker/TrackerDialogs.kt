package com.rifttally.feature.tracker

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.MIN_TARGET_SCORE
import com.rifttally.core.ui.rotateLayout

/** Stepper dialog for the target score. The value is a local draft until "Save". */
@Composable
internal fun TargetScoreDialog(
    currentTarget: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableIntStateOf(currentTarget) }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_tracker_target_dialog_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                RiftIconButton(
                    imageVector = RiftIcons.Remove,
                    contentDescription = stringResource(R.string.feature_tracker_target_decrease),
                    onClick = { draft = (draft - 1).coerceAtLeast(MIN_TARGET_SCORE) },
                    enabled = draft > MIN_TARGET_SCORE,
                )
                Text(text = draft.toString(), style = RiftTheme.scoreTypography.scoreSmall)
                RiftIconButton(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.feature_tracker_target_increase),
                    onClick = { draft += 1 },
                )
            }
        },
        confirmButton = {
            RiftTextButton(text = stringResource(R.string.feature_tracker_dialog_save), onClick = { onConfirm(draft) })
        },
        dismissButton = {
            RiftTextButton(text = stringResource(R.string.feature_tracker_dialog_cancel), onClick = onDismiss)
        },
    )
}

/** Yes/cancel confirmation for ending or restarting the game from the menu. */
@Composable
internal fun ConfirmDialog(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes confirmLabel: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = { RiftTextButton(text = stringResource(confirmLabel), onClick = onConfirm) },
        dismissButton = {
            RiftTextButton(text = stringResource(R.string.feature_tracker_dialog_cancel), onClick = onDismiss)
        },
    )
}

@PreviewRiftThemes
@Composable
private fun TargetScoreDialogPreview() {
    RiftTheme {
        TargetScoreDialog(currentTarget = 8, onConfirm = {}, onDismiss = {})
    }
}

@PreviewRiftThemes
@Composable
private fun ConfirmDialogPreview() {
    RiftTheme {
        ConfirmDialog(
            title = R.string.feature_tracker_end_dialog_title,
            body = R.string.feature_tracker_end_dialog_body,
            confirmLabel = R.string.feature_tracker_menu_end,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

/**
 * Adds a counter for [playerName]: one tap on the "XP" preset, or a custom label (trimmed, at most
 * [maxLabelLength] characters).
 *
 * The dialog is its own window, so it doesn't inherit the panel's rotation; [quarterTurns] (the asking
 * player's seat) turns its content to face them instead.
 */
@Composable
internal fun AddCounterDialog(
    playerName: String,
    maxLabelLength: Int,
    onAdd: (label: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    quarterTurns: Int = 0,
) {
    var label by rememberSaveable { mutableStateOf("") }
    val xp = stringResource(R.string.feature_tracker_counter_preset_xp)
    AlertDialog(
        modifier = modifier.rotateLayout(quarterTurns),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_tracker_counter_dialog_title, playerName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
                RiftButton(text = xp, onClick = { onAdd(xp) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(maxLabelLength) },
                    label = { Text(stringResource(R.string.feature_tracker_counter_custom_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            RiftTextButton(
                text = stringResource(R.string.feature_tracker_counter_add),
                onClick = { onAdd(label) },
                enabled = label.isNotBlank(),
            )
        },
        dismissButton = {
            RiftTextButton(text = stringResource(R.string.feature_tracker_dialog_cancel), onClick = onDismiss)
        },
    )
}

@PreviewRiftThemes
@Composable
private fun AddCounterDialogPreview() {
    RiftTheme {
        AddCounterDialog(playerName = "Ana", maxLabelLength = 12, onAdd = {}, onDismiss = {})
    }
}

@PreviewRiftThemes
@Composable
private fun AddCounterDialogFarSeatPreview() {
    RiftTheme {
        AddCounterDialog(playerName = "Ben", maxLabelLength = 12, onAdd = {}, onDismiss = {}, quarterTurns = 2)
    }
}
