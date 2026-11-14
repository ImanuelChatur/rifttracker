package com.rifttally.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.titleLargeEmphasized
import com.rifttally.core.ui.LegendAvatar
import kotlinx.collections.immutable.persistentListOf

/** One side's final result: Legend avatar, names, Legends, score, and a star for the winner. */
@Composable
internal fun SideResultRow(
    side: HistorySideUiState,
    modifier: Modifier = Modifier,
    avatarSize: Dp = RiftTheme.sizes.avatarSmall,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        LegendAvatar(
            legendName = side.legendName,
            imageUri = side.legendImageUri,
            playerName = side.name,
            playerColor = PlayerColors.fromId(side.colorId) ?: PlayerColors.Grey,
            size = avatarSize,
            // The names are visible next to it.
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = side.name,
                style = RiftTheme.typography.titleSmall,
                fontWeight = if (side.isWinner) FontWeight.Bold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (side.legendNames != null) {
                Text(
                    text = side.legendNames,
                    style = RiftTheme.typography.bodySmall,
                    // Follows the container (tile or the detail's hero), slightly muted.
                    color = LocalContentColor.current.copy(alpha = SECONDARY_TEXT_ALPHA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (side.isWinner) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(R.string.feature_history_winner_icon),
                tint = RiftTheme.colorScheme.primary,
                modifier = Modifier.size(RiftTheme.sizes.iconSmall),
            )
        }
        Text(
            text = side.score.toString(),
            // Display font with tabular figures; the winner's score is the row's emphasis.
            style = if (side.isWinner) {
                RiftTheme.typography.titleLargeEmphasized
            } else {
                RiftTheme.typography.titleLarge
            }.copy(fontFeatureSettings = "tnum"),
            color = if (side.isWinner) RiftTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier.padding(start = RiftTheme.spacing.xs),
        )
    }
}

private const val SECONDARY_TEXT_ALPHA = 0.8f

internal val previewSides = persistentListOf(
    HistorySideUiState("Ana", "Jinx", "orange", "Jinx", null, 8, isWinner = true),
    HistorySideUiState("Ben", null, "sky", null, null, 5, isWinner = false),
)

@PreviewRiftThemes
@Composable
private fun SideResultRowPreview() {
    RiftTheme {
        Surface {
            Column(
                modifier = Modifier.padding(RiftTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                previewSides.forEach { SideResultRow(side = it) }
                SideResultRow(side = HistorySideUiState("Ana & Cleo", "Jinx & Viktor", "green", "Jinx", null, 11, true))
            }
        }
    }
}
