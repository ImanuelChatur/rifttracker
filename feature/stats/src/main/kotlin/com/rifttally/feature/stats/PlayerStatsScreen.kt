package com.rifttally.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (uiState) {
            PlayerStatsUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            PlayerStatsUiState.NotFound -> StatsMessage(
                stringResource(R.string.feature_stats_not_found),
                contentModifier,
            )

            is PlayerStatsUiState.Success -> PlayerStatsContent(uiState, onPlayerClick, contentModifier)
        }
    }
}

@Composable
private fun PlayerStatsContent(
    state: PlayerStatsUiState.Success,
    onPlayerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Column(
                modifier = Modifier.padding(RiftTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
                    StatTile(R.string.feature_stats_games_label, state.games.toString(), Modifier.weight(1f))
                    StatTile(R.string.feature_stats_wins_label, state.wins.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
                    StatTile(
                        label = R.string.feature_stats_win_rate_label,
                        value = stringResource(R.string.feature_stats_percent, state.winRatePercent),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = R.string.feature_stats_streak_label,
                        value = state.currentWinStreak.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item { SectionHeading(R.string.feature_stats_favorite_legend) }
        item {
            val favorite = state.favoriteLegend
            if (favorite == null) {
                BodyText(stringResource(R.string.feature_stats_no_legend))
            } else {
                LegendStatRow(legend = favorite)
            }
        }
        item { SectionHeading(R.string.feature_stats_head_to_head) }
        if (state.headToHead.isEmpty()) {
            item { BodyText(stringResource(R.string.feature_stats_head_to_head_empty)) }
        } else {
            items(state.headToHead, key = { it.opponentKey }) { record ->
                HeadToHeadRow(
                    playerName = state.name,
                    record = record,
                    onClick = { onPlayerClick(record.opponentKey) },
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: Int, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.lg),
        ) {
            Text(
                text = stringResource(label),
                style = RiftTheme.typography.labelMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = RiftTheme.typography.headlineMedium)
        }
    }
}

/** "Ben · 4 games · 3 – 1". Tapping opens the opponent's stats. */
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
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
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
            Text(text = games, style = RiftTheme.typography.bodySmall, color = RiftTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.feature_stats_head_to_head_score, record.wins, record.opponentWins),
            style = RiftTheme.typography.titleLarge,
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
