package com.rifttally.feature.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LegendAvatar
import kotlinx.collections.immutable.persistentListOf

/** Searchable grid of Legends plus "None". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LegendPickerSheet(
    picker: LegendPickerUiState,
    playerName: String,
    onQueryChange: (String) -> Unit,
    onLegendSelect: (legendId: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        LegendPickerContent(
            picker = picker,
            playerName = playerName,
            onQueryChange = onQueryChange,
            onLegendSelect = onLegendSelect,
        )
    }
}

@Composable
private fun LegendPickerContent(
    picker: LegendPickerUiState,
    playerName: String,
    onQueryChange: (String) -> Unit,
    onLegendSelect: (legendId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = RiftTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.feature_setup_choose_legend, playerName),
            style = RiftTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = picker.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.feature_setup_legend_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(RiftTheme.sizes.avatarMedium + RiftTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            item(key = NONE_KEY) {
                LegendCell(
                    legend = null,
                    selected = picker.selectedLegendId == null,
                    onClick = { onLegendSelect(null) },
                )
            }
            items(picker.legends, key = { "legend:${it.id}" }) { legend ->
                LegendCell(
                    legend = legend,
                    selected = legend.id == picker.selectedLegendId,
                    onClick = { onLegendSelect(legend.id) },
                )
            }
            if (picker.legends.isEmpty() && picker.query.isNotBlank()) {
                item(key = EMPTY_KEY) {
                    Text(
                        text = stringResource(R.string.feature_setup_legend_empty),
                        style = RiftTheme.typography.bodyMedium,
                        color = RiftTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(RiftTheme.spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendCell(legend: Legend?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = legend?.name ?: stringResource(R.string.feature_setup_legend_none)
    Surface(
        modifier = modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(RiftTheme.spacing.md),
        color = if (selected) RiftTheme.colorScheme.secondaryContainer else RiftTheme.colorScheme.surfaceContainer,
        contentColor = if (selected) RiftTheme.colorScheme.onSecondaryContainer else RiftTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
        ) {
            LegendAvatar(
                legendName = legend?.name,
                imageUri = legend?.imageUri,
                playerName = name,
                playerColor = PlayerColors.Grey,
                contentDescription = null,
            )
            Text(
                text = name,
                style = RiftTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Prefixed so a Legend id can never collide with these fixed keys.
private const val NONE_KEY = "__none__"
private const val EMPTY_KEY = "__empty__"

@PreviewRiftThemes
@Composable
private fun LegendPickerContentPreview() {
    RiftTheme {
        Surface {
            LegendPickerContent(
                picker = LegendPickerUiState(
                    playerIndex = 0,
                    query = "",
                    legends = persistentListOf(
                        Legend("jinx", "Jinx", null),
                        Legend("viktor", "Viktor", null),
                        Legend("miss_fortune", "Miss Fortune", null),
                    ),
                    selectedLegendId = "jinx",
                ),
                playerName = "Ana",
                onQueryChange = {},
                onLegendSelect = {},
            )
        }
    }
}
