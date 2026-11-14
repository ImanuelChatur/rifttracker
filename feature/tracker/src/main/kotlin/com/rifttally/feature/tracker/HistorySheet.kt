package com.rifttally.feature.tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.game.GameLogLine
import com.rifttally.core.ui.text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Bottom sheet with the event log, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistorySheet(items: ImmutableList<GameLogLine>, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        HistoryContent(items = items)
    }
}

@Composable
private fun HistoryContent(items: ImmutableList<GameLogLine>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = RiftTheme.spacing.lg)) {
        Text(
            text = stringResource(R.string.feature_tracker_history_title),
            style = RiftTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = RiftTheme.spacing.sm),
        )
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.feature_tracker_history_empty),
                style = RiftTheme.typography.bodyLarge,
                color = RiftTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = RiftTheme.spacing.lg),
            )
        } else {
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    Text(
                        text = item.text(),
                        style = RiftTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RiftTheme.spacing.md),
                    )
                    HorizontalDivider(color = RiftTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

internal val previewHistory = persistentListOf(
    GameLogLine.Points("e4", "Ana", 1, 6),
    GameLogLine.TargetChanged("e3", 10),
    GameLogLine.Points("e2", "Ben", -1, 2),
    GameLogLine.CounterChanged("e1", "Ana", "XP", 2, 4),
)

@PreviewRiftThemes
@Composable
private fun HistoryContentPreview() {
    RiftTheme {
        Surface {
            HistoryContent(items = previewHistory)
        }
    }
}

@PreviewRiftThemes
@Composable
private fun HistoryContentEmptyPreview() {
    RiftTheme {
        Surface {
            HistoryContent(items = persistentListOf())
        }
    }
}
