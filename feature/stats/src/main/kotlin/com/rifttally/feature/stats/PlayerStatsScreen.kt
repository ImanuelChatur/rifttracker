package com.rifttally.feature.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.displayLargeEmphasized
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PlayerStatsRoute(
    onBackClick: () -> Unit,
    onPlayerClick: (playerKey: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PlayerStatsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPlayerClick = onPlayerClick,
        modifier = modifier,
    )
}

/**
 * One player's stats. The hero (DESIGN.md §2.2) is their win rate; games, wins and streak sit on quiet
 * tiles below, then the favorite Legend and head-to-head records as segmented lists (DESIGN.md §8).
 */
@Composable
internal fun PlayerStatsScreen(
    uiState: PlayerStatsUiState,
    onBackClick: () -> Unit,
    onPlayerClick: (playerKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = (uiState as? PlayerStatsUiState.Success)?.name
                    ?: stringResource(R.string.feature_stats_player_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_stats_back),
                        onClick = onBackClick,
                    )
                },
            )
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        when (uiState) {
            PlayerStatsUiState.Loading -> CenteredList(scaffoldPadding = padding) { skeletonTiles() }

            PlayerStatsUiState.NotFound -> EmptyState(
                icon = Icons.Filled.Search,
                text = stringResource(R.string.feature_stats_not_found),
                actionLabel = stringResource(R.string.feature_stats_empty_action),
                onAction = onBackClick,
                modifier = Modifier.padding(padding),
            )

            is PlayerStatsUiState.Success -> CenteredList(scaffoldPadding = padding) {
                item(key = "hero") {
                    WinRateHero(
                        winRatePercent = uiState.winRatePercent,
                        modifier = Modifier.padding(bottom = RiftTheme.spacing.sm),
                    )
                }
                item(key = "tiles") { StatTiles(state = uiState) }

                item(key = "legend_header") {
                    ListSectionHeader(stringResource(R.string.feature_stats_favorite_legend))
                }
                item(key = "legend") {
                    SegmentedTile(index = 0, count = 1) {
                        val favorite = uiState.favoriteLegend
                        if (favorite == null) {
                            TileMessage(stringResource(R.string.feature_stats_no_legend))
                        } else {
                            LegendStatRow(legend = favorite)
                        }
                    }
                }

                item(key = "h2h_header") { ListSectionHeader(stringResource(R.string.feature_stats_head_to_head)) }
                if (uiState.headToHead.isEmpty()) {
                    item(key = "h2h_empty") {
                        SegmentedTile(index = 0, count = 1) {
                            TileMessage(stringResource(R.string.feature_stats_head_to_head_empty))
                        }
                    }
                } else {
                    itemsIndexed(uiState.headToHead, key = { _, record -> record.opponentKey }) { index, record ->
                        SegmentedTile(index = index, count = uiState.headToHead.size) {
                            HeadToHeadRow(
                                playerName = uiState.name,
                                record = record,
                                onClick = { onPlayerClick(record.opponentKey) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The hero: the player's win rate in emphasized display type on an emphasized container. */
@Composable
private fun WinRateHero(winRatePercent: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.extraLarge,
        color = RiftTheme.colorScheme.primaryContainer,
        contentColor = RiftTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.xl),
        ) {
            Text(text = stringResource(R.string.feature_stats_win_rate_label), style = RiftTheme.typography.labelLarge)
            Text(
                text = stringResource(R.string.feature_stats_percent, winRatePercent),
                style = RiftTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** Games, wins and current streak on three equal tonal tiles. */
@Composable
private fun StatTiles(state: PlayerStatsUiState.Success, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
    ) {
        StatTile(R.string.feature_stats_games_label, state.games.toString(), Modifier.weight(1f))
        StatTile(R.string.feature_stats_wins_label, state.wins.toString(), Modifier.weight(1f))
        StatTile(R.string.feature_stats_streak_label, state.currentWinStreak.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(@StringRes label: Int, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RiftTheme.shapes.large,
        color = RiftTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(label),
                style = RiftTheme.typography.labelLarge,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = RiftTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** "Ben · 4 games · 3 – 1" inside a list tile. Tapping opens the opponent's stats. */
@Composable
private fun HeadToHeadRow(
    playerName: String,
    record: HeadToHeadUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val games = pluralStringResource(R.plurals.feature_stats_games, record.games, record.games)
    val description = stringResource(
        R.string.feature_stats_head_to_head_description,
        playerName,
        record.wins,
        record.opponentName,
        record.opponentWins,
        games,
    )
    val openLabel = stringResource(R.string.feature_stats_open_player, record.opponentName)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.heroButtonHeight)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = openLabel) {
                    onClick()
                    true
                }
            }
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.opponentName,
                style = RiftTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = games, style = RiftTheme.typography.bodyMedium, color = RiftTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.feature_stats_head_to_head_score, record.wins, record.opponentWins),
            style = RiftTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

internal val previewPlayerState = PlayerStatsUiState.Success(
    name = "Ana",
    games = 7,
    wins = 5,
    winRatePercent = 71,
    currentWinStreak = 3,
    favoriteLegend = previewLegends[0],
    headToHead = persistentListOf(
        HeadToHeadUiState("name:ben", "Ben", games = 5, wins = 4, opponentWins = 1),
        HeadToHeadUiState("name:cleo", "Cleo", games = 2, wins = 1, opponentWins = 0),
    ),
)

@PreviewRiftThemes
@Composable
private fun PlayerStatsScreenPreview() {
    RiftTheme {
        PlayerStatsScreen(uiState = previewPlayerState, onBackClick = {}, onPlayerClick = {})
    }
}

@PreviewRiftThemes
@Composable
private fun PlayerStatsScreenNoLegendPreview() {
    RiftTheme {
        PlayerStatsScreen(
            uiState = previewPlayerState.copy(favoriteLegend = null, headToHead = persistentListOf()),
            onBackClick = {},
            onPlayerClick = {},
        )
    }
}
