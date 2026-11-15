package com.rifttally.feature.stats

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.list.CenteredList
import com.rifttally.core.ui.list.EmptyState
import com.rifttally.core.ui.list.ListSectionHeader
import com.rifttally.core.ui.list.SegmentedTile
import com.rifttally.core.ui.list.skeletonTiles
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun LegendStatsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LegendStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LegendStatsScreen(uiState = uiState, onBackClick = onBackClick, modifier = modifier)
}

/**
 * One Legend's stats. The hero (DESIGN.md §2.2) is its win rate next to its portrait; games and wins sit
 * on quiet tiles, then the opposing Legends as a segmented list (DESIGN.md §8).
 */
@Composable
internal fun LegendStatsScreen(uiState: LegendStatsUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = (uiState as? LegendStatsUiState.Success)?.legend?.name
                    ?: stringResource(R.string.feature_stats_legend_title),
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
            LegendStatsUiState.Loading -> CenteredList(scaffoldPadding = padding) { skeletonTiles() }

            is LegendStatsUiState.NotFound -> EmptyState(
                icon = Icons.Filled.Search,
                text = if (uiState.period == StatsPeriod.ALL_TIME) {
                    stringResource(R.string.feature_stats_legend_not_found)
                } else {
                    stringResource(R.string.feature_stats_period_empty)
                },
                actionLabel = stringResource(R.string.feature_stats_empty_action),
                onAction = onBackClick,
                modifier = Modifier.padding(padding),
            )

            is LegendStatsUiState.Success -> CenteredList(scaffoldPadding = padding) {
                item(key = "hero") {
                    WinRateHero(
                        label = labelWithPeriod(stringResource(R.string.feature_stats_win_rate_label), uiState.period),
                        winRatePercent = uiState.legend.winRatePercent,
                        modifier = Modifier.padding(bottom = RiftTheme.spacing.sm),
                        leading = {
                            LegendAvatar(
                                legendName = uiState.legend.name,
                                imageUri = uiState.legend.imageUri,
                                playerName = uiState.legend.name,
                                playerColor = PlayerColors.Grey,
                                size = RiftTheme.sizes.avatarMedium,
                                // The name is the screen title.
                                contentDescription = null,
                            )
                        },
                    )
                }
                item(key = "tiles") {
                    StatTileRow {
                        StatTile(
                            label = stringResource(R.string.feature_stats_games_label),
                            value = uiState.legend.games.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            label = stringResource(R.string.feature_stats_wins_label),
                            value = uiState.wins.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item(key = "matchups_header") { ListSectionHeader(stringResource(R.string.feature_stats_matchups)) }
                if (uiState.matchups.isEmpty()) {
                    item(key = "matchups_empty") {
                        SegmentedTile(index = 0, count = 1) {
                            TileMessage(stringResource(R.string.feature_stats_matchups_empty))
                        }
                    }
                } else {
                    itemsIndexed(uiState.matchups, key = { _, matchup ->
                        matchup.opponent.legendId
                    }) { index, matchup ->
                        SegmentedTile(index = index, count = uiState.matchups.size) { MatchupRow(matchup) }
                    }
                }
            }
        }
    }
}

/** "Viktor · 4 games · 50% wins" with "2 – 1" (wins – losses) at the end. */
@Composable
private fun MatchupRow(matchup: MatchupUiState, modifier: Modifier = Modifier) {
    val opponent = matchup.opponent
    val description = stringResource(
        R.string.feature_stats_matchup_description,
        opponent.name,
        pluralStringResource(R.plurals.feature_stats_games, opponent.games, opponent.games),
        matchup.wins,
        matchup.losses,
        stringResource(R.string.feature_stats_win_rate, opponent.winRatePercent),
    )
    LegendStatRow(
        legend = opponent,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        trailing = {
            Text(
                text = stringResource(R.string.feature_stats_matchup_score, matchup.wins, matchup.losses),
                style = RiftTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            )
        },
    )
}

internal val previewLegendState = LegendStatsUiState.Success(
    legend = LegendRowUiState("jinx", "Jinx", null, games = 6, winRatePercent = 67),
    wins = 4,
    matchups = persistentListOf(
        MatchupUiState(
            LegendRowUiState("viktor", "Viktor", null, games = 4, winRatePercent = 75),
            wins = 3,
            losses = 1,
        ),
        MatchupUiState(LegendRowUiState("ahri", "Ahri", null, games = 2, winRatePercent = 0), wins = 0, losses = 1),
    ),
    period = StatsPeriod.LAST_30_DAYS,
)

@PreviewRiftThemes
@Composable
private fun LegendStatsScreenPreview() {
    RiftTheme {
        LegendStatsScreen(uiState = previewLegendState, onBackClick = {})
    }
}

@PreviewRiftThemes
@Composable
private fun LegendStatsScreenNoMatchupsPreview() {
    RiftTheme {
        LegendStatsScreen(uiState = previewLegendState.copy(matchups = persistentListOf()), onBackClick = {})
    }
}

@PreviewRiftThemes
@Composable
private fun LegendStatsScreenNotFoundPreview() {
    RiftTheme {
        LegendStatsScreen(uiState = LegendStatsUiState.NotFound(StatsPeriod.THIS_YEAR), onBackClick = {})
    }
}
