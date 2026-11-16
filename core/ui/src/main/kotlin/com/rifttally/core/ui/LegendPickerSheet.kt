package com.rifttally.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.model.Legend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Searchable 3-column grid of Legend portrait cards plus "None", in a bottom sheet. The selected card
 * gets a ring in the player's [playerColor]. [legends] is already filtered by [query] (by the caller's
 * ViewModel); [selectedLegendId] `null` selects "None".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun LegendPickerSheet(
    query: String,
    legends: ImmutableList<Legend>,
    selectedLegendId: String?,
    playerName: String,
    playerColor: PlayerColor,
    onQueryChange: (String) -> Unit,
    onLegendSelect: (legendId: String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        LegendPickerContent(
            query = query,
            legends = legends,
            selectedLegendId = selectedLegendId,
            playerName = playerName,
            playerColor = playerColor,
            onQueryChange = onQueryChange,
            onLegendSelect = onLegendSelect,
        )
    }
}

@Composable
public fun LegendPickerContent(
    query: String,
    legends: ImmutableList<Legend>,
    selectedLegendId: String?,
    playerName: String,
    playerColor: PlayerColor,
    onQueryChange: (String) -> Unit,
    onLegendSelect: (legendId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = RiftTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Text(
            text = stringResource(R.string.core_ui_choose_legend, playerName),
            style = RiftTheme.typography.titleLarge,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.core_ui_legend_search)) },
            leadingIcon = { Icon(RiftIcons.Search, contentDescription = null) },
            shape = RiftTheme.shapes.medium,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            item(key = NONE_KEY) {
                LegendCard(
                    legend = null,
                    selected = selectedLegendId == null,
                    playerColor = playerColor,
                    onClick = { onLegendSelect(null) },
                )
            }
            items(legends, key = { "legend:${it.id}" }) { legend ->
                LegendCard(
                    legend = legend,
                    selected = legend.id == selectedLegendId,
                    playerColor = playerColor,
                    onClick = { onLegendSelect(legend.id) },
                )
            }
            if (legends.isEmpty() && query.isNotBlank()) {
                item(key = EMPTY_KEY, span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.core_ui_legend_empty),
                        style = RiftTheme.typography.bodyMedium,
                        color = RiftTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(RiftTheme.spacing.md),
                    )
                }
            }
        }
    }
}

/** A portrait card (art or color fallback) with the name underneath; ring in the player's color when selected. */
@Composable
private fun LegendCard(
    legend: Legend?,
    selected: Boolean,
    playerColor: PlayerColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = legend?.name ?: stringResource(R.string.core_ui_legend_none)
    val shape = RiftTheme.shapes.largeIncreased
    val ring = RiftTheme.sizes.borderThick
    val ringColor by animateColorAsState(
        targetValue = if (selected) playerColor.color else Color.Transparent,
        animationSpec = if (reduceMotionEnabled()) {
            RiftTheme.motion.none()
        } else {
            RiftTheme.motion.expressive.fastEffects()
        },
        label = "selectionRing",
    )
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PORTRAIT_ASPECT)
                .border(ring, ringColor, shape)
                .padding(ring + RiftTheme.spacing.xxs)
                .clip(shape),
        ) {
            if (legend != null) {
                LegendBackdrop(
                    legendName = legend.name,
                    imageUri = legend.imageUri,
                    fallbackName = legend.name,
                    playerColor = if (selected) playerColor else PlayerColors.Grey,
                    scrim = false,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(RiftTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RiftIcons.Close,
                        contentDescription = null,
                        tint = RiftTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(RiftTheme.sizes.minTouchTarget),
                    )
                }
            }
        }
        Text(
            text = name,
            style = RiftTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = RiftTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val GRID_COLUMNS = 3

/** Portrait cards are a little taller than wide (the square art is cropped to fit). */
private const val PORTRAIT_ASPECT = 0.8f

// Prefixed so a Legend id can never collide with these fixed keys.
private const val NONE_KEY = "__none__"
private const val EMPTY_KEY = "__empty__"

@PreviewRiftThemes
@Composable
private fun LegendPickerContentPreview() {
    RiftTheme {
        Surface {
            LegendPickerContent(
                query = "",
                legends = persistentListOf(
                    Legend("jinx", "Jinx", null),
                    Legend("viktor", "Viktor", null),
                    Legend("miss_fortune", "Miss Fortune", null),
                ),
                selectedLegendId = "jinx",
                playerName = "Ana",
                playerColor = PlayerColors.Orange,
                onQueryChange = {},
                onLegendSelect = {},
            )
        }
    }
}
