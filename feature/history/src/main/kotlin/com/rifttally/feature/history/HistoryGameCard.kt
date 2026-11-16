package com.rifttally.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.list.SegmentedTile

/**
 * One ended game as a tile of the segmented history list ([index] of [count]): date and format, each
 * side's Legend, names and final score (winner emphasized), and the result. Tapping opens the detail;
 * the overflow menu deletes it.
 */
@Composable
internal fun HistoryGameCard(
    game: HistoryGameUiState,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedTile(
        index = index,
        count = count,
        onClick = onClick,
        onClickLabel = stringResource(R.string.feature_history_open_game),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(
                start = RiftTheme.spacing.lg,
                end = RiftTheme.spacing.xs,
                bottom = RiftTheme.spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.feature_history_row_header,
                        dateTimeText(game.endedAt),
                        formatLabel(game.format),
                    ),
                    style = RiftTheme.typography.labelLarge,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = RiftTheme.spacing.xs),
                )
                GameMenu(
                    names = game.sides.joinToString(stringResource(R.string.feature_history_names_separator)) {
                        it.name
                    },
                    onDelete = onDelete,
                )
            }
            Column(
                modifier = Modifier.padding(end = RiftTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                game.sides.forEach { side -> SideResultRow(side = side) }
                Text(
                    text = game.winnerName?.let { stringResource(R.string.feature_history_winner, it) }
                        ?: stringResource(R.string.feature_history_no_winner),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GameMenu(names: String, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        RiftIconButton(
            imageVector = RiftIcons.MoreVert,
            contentDescription = stringResource(R.string.feature_history_row_menu, names),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RiftTheme.shapes.medium,
            containerColor = RiftTheme.colorScheme.surfaceContainerHigh,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_history_delete)) },
                leadingIcon = { Icon(imageVector = RiftIcons.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

internal val previewGame = HistoryGameUiState(
    gameId = GameId("g1"),
    endedAt = 1_763_150_400_000,
    format = GameFormat.TWO_PLAYERS,
    sides = previewSides,
    winnerName = "Ana",
)

@PreviewRiftThemes
@Composable
private fun HistoryGameCardPreview() {
    RiftTheme {
        Box(modifier = Modifier.padding(RiftTheme.spacing.lg)) {
            HistoryGameCard(game = previewGame, index = 0, count = 1, onClick = {}, onDelete = {})
        }
    }
}
