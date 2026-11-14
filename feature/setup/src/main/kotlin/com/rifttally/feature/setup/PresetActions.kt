package com.rifttally.feature.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** "Load saved player" (menu of presets, only when there are some) and "Save player" for one card. */
@Composable
internal fun PresetActions(
    playerName: String,
    presets: ImmutableList<SavedPlayerUiState>,
    canSave: Boolean,
    onSave: () -> Unit,
    onPresetApply: (presetId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val loadDescription = stringResource(R.string.feature_setup_preset_load_for, playerName)
    val saveDescription = stringResource(R.string.feature_setup_preset_save_for, playerName)
    Row(modifier = modifier) {
        if (presets.isNotEmpty()) {
            Box {
                RiftTextButton(
                    text = stringResource(R.string.feature_setup_preset_load),
                    onClick = { menuOpen = true },
                    modifier = Modifier.semantics { contentDescription = loadDescription },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.name) },
                            onClick = {
                                menuOpen = false
                                onPresetApply(preset.id)
                            },
                        )
                    }
                }
            }
        }
        RiftTextButton(
            text = stringResource(R.string.feature_setup_preset_save),
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.semantics { contentDescription = saveDescription },
        )
    }
}

@PreviewRiftThemes
@Composable
private fun PresetActionsPreview() {
    RiftTheme {
        Surface {
            PresetActions(
                playerName = "Ana",
                presets = persistentListOf(SavedPlayerUiState("1", "Ana")),
                canSave = true,
                onSave = {},
                onPresetApply = {},
            )
        }
    }
}
