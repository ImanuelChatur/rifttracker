package com.rifttally.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.PlayerColorPicker

/**
 * Edits a saved player's name, color and Legend (a utilitarian dialog: standard scheme, DESIGN.md §7).
 * Nothing is written until "Save"; the Legend row opens the shared Legend sheet.
 */
@Composable
internal fun PresetEditorDialog(
    editor: PresetEditorUiState,
    onNameChange: (String) -> Unit,
    onColorSelect: (colorId: String) -> Unit,
    onLegendClick: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_settings_preset_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.feature_settings_preset_name)) },
                    singleLine = true,
                    isError = !editor.canSave || editor.duplicateName != null,
                    supportingText = editor.duplicateName?.let { existing ->
                        { Text(stringResource(R.string.feature_settings_preset_duplicate, existing)) }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                PlayerColorPicker(selectedColorId = editor.colorId, onColorSelect = onColorSelect)
                LegendRow(editor = editor, onClick = onLegendClick)
            }
        },
        confirmButton = {
            RiftTextButton(
                text = stringResource(R.string.feature_settings_preset_save),
                onClick = onSave,
                enabled = editor.canSave,
            )
        },
        dismissButton = {
            RiftTextButton(text = stringResource(R.string.feature_settings_preset_cancel), onClick = onDismiss)
        },
    )
}

/** The chosen Legend (portrait + name); tapping it opens the Legend sheet. */
@Composable
private fun LegendRow(editor: PresetEditorUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val legendName = editor.legend?.name ?: stringResource(R.string.feature_settings_preset_no_legend)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
            .clickable(
                onClickLabel = stringResource(R.string.feature_settings_preset_choose_legend),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = RiftTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        LegendAvatar(
            legendName = editor.legend?.name,
            imageUri = editor.legend?.imageUri,
            playerName = editor.name,
            playerColor = PlayerColors.fromId(editor.colorId) ?: PlayerColors.Grey,
            size = RiftTheme.sizes.avatarSmall,
            // The row's text says the same.
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.feature_settings_preset_legend),
                style = RiftTheme.typography.labelMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = legendName,
                style = RiftTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.feature_settings_preset_change),
            style = RiftTheme.typography.labelLarge,
            color = RiftTheme.colorScheme.primary,
        )
    }
}

internal val previewPresetEditor = PresetEditorUiState(
    id = "1",
    name = "Ana",
    colorId = "orange",
    legend = Legend("jinx", "Jinx", null),
)

@PreviewRiftThemes
@Composable
private fun PresetEditorDialogPreview() {
    RiftTheme {
        Surface {
            PresetEditorDialog(
                editor = previewPresetEditor,
                onNameChange = {},
                onColorSelect = {},
                onLegendClick = {},
                onSave = {},
                onDismiss = {},
            )
        }
    }
}

@PreviewRiftThemes
@Composable
private fun PresetEditorDialogDuplicatePreview() {
    RiftTheme {
        Surface {
            PresetEditorDialog(
                editor = previewPresetEditor.copy(name = "Ben", duplicateName = "Ben"),
                onNameChange = {},
                onColorSelect = {},
                onLegendClick = {},
                onSave = {},
                onDismiss = {},
            )
        }
    }
}
