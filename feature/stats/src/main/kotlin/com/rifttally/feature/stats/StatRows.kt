package com.rifttally.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.LegendAvatar

/** "5 games · 60% wins". */
@Composable
internal fun gamesAndRateText(games: Int, winRatePercent: Int): String = stringResource(
    R.string.feature_stats_games_and_rate,
    pluralStringResource(R.plurals.feature_stats_games, games, games),
    stringResource(R.string.feature_stats_win_rate, winRatePercent),
)

/** A player's line: optional [rank], name, games and win rate. Tapping opens their stats. */
@Composable
internal fun PlayerStatRow(
    player: PlayerRowUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
) {
    val openLabel = stringResource(R.string.feature_stats_open_player, player.name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { onClick(label = openLabel, action = null) }
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = RiftTheme.typography.titleMedium,
                color = RiftTheme.colorScheme.primary,
            )
        }
        Text(
            text = player.name,
            style = RiftTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = gamesAndRateText(player.games, player.winRatePercent),
            style = RiftTheme.typography.bodyMedium,
            color = RiftTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A Legend's line: portrait (or fallback), name, games and win rate. */
@Composable
internal fun LegendStatRow(legend: LegendRowUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        LegendAvatar(
            legendName = legend.name,
            imageUri = legend.imageUri,
            playerName = legend.name,
            playerColor = PlayerColors.Grey,
            size = RiftTheme.sizes.avatarSmall,
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = legend.name, style = RiftTheme.typography.bodyLarge)
            Text(
                text = gamesAndRateText(legend.games, legend.winRatePercent),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewRiftThemes
@Composable
private fun StatRowsPreview() {
    RiftTheme {
        Surface {
            Column {
                PlayerStatRow(player = previewPlayers[0], onClick = {}, rank = 1)
                PlayerStatRow(player = previewPlayers[1], onClick = {})
                LegendStatRow(legend = previewLegends[0])
            }
        }
    }
}
