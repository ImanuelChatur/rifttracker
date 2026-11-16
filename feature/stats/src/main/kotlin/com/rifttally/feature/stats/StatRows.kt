package com.rifttally.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
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
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.list.SegmentedTile

/** "5 games · 60% wins". */
@Composable
internal fun gamesAndRateText(games: Int, winRatePercent: Int): String = stringResource(
    R.string.feature_stats_games_and_rate,
    pluralStringResource(R.plurals.feature_stats_games, games, games),
    stringResource(R.string.feature_stats_win_rate, winRatePercent),
)

/**
 * A player's line inside a list tile: optional [rank] badge, name, games and win rate, and a chevron.
 * Tapping opens their stats.
 */
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
            .heightIn(min = RiftTheme.sizes.listRowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { onClick(label = openLabel, action = null) }
            .padding(start = RiftTheme.spacing.lg, end = RiftTheme.spacing.sm)
            .padding(vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        if (rank != null) RankBadge(rank)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = RiftTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = gamesAndRateText(player.games, player.winRatePercent),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = RiftIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = RiftTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ranking position on a small tonal circle. */
@Composable
private fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RiftTheme.shapes.full,
        color = RiftTheme.colorScheme.secondaryContainer,
        contentColor = RiftTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(
            modifier = Modifier.sizeIn(minWidth = RiftTheme.sizes.avatarSmall, minHeight = RiftTheme.sizes.avatarSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = rank.toString(), style = RiftTheme.typography.titleLarge)
        }
    }
}

/**
 * A Legend's line inside a list tile: portrait (or fallback), name, games and win rate. With [onClick]
 * the row opens the Legend's stats and shows a chevron; [trailing] (e.g. a matchup score) sits at the end.
 */
@Composable
internal fun LegendStatRow(
    legend: LegendRowUiState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val openLabel = stringResource(R.string.feature_stats_open_legend, legend.name)
    val clickModifier = if (onClick != null) {
        Modifier
            .heightIn(min = RiftTheme.sizes.listRowMinHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { onClick(label = openLabel, action = null) }
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(
                start = RiftTheme.spacing.lg,
                end = if (onClick !=
                    null
                ) {
                    RiftTheme.spacing.sm
                } else {
                    RiftTheme.spacing.lg
                },
            )
            .padding(vertical = RiftTheme.spacing.md),
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
            Text(
                text = legend.name,
                style = RiftTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = gamesAndRateText(legend.games, legend.winRatePercent),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
        if (onClick != null) {
            Icon(
                imageVector = RiftIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewRiftThemes
@Composable
private fun StatRowsPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
        ) {
            SegmentedTile(index = 0, count = 3) { PlayerStatRow(player = previewPlayers[0], onClick = {}, rank = 1) }
            SegmentedTile(index = 1, count = 3) { PlayerStatRow(player = previewPlayers[1], onClick = {}) }
            SegmentedTile(index = 2, count = 3) { LegendStatRow(legend = previewLegends[0], onClick = {}) }
        }
    }
}
