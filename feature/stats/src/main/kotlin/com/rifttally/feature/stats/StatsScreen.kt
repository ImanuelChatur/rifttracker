package com.rifttally.feature.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun StatsRoute(
    onBackClick: () -> Unit,
    onPlayerClick: (playerKey: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(
        uiState = uiState,
        actions = StatsActions(
            onBackClick = onBackClick,
            onPlayerClick = onPlayerClick,
            onTabSelect = viewModel::onTabSelect,
            onLegendSortSelect = viewModel::onLegendSortSelect,
        ),
        modifier = modifier,
    )
}

/** Every user action on the stats screen. */
internal data class StatsActions(
    val onBackClick: () -> Unit = {},
    val onPlayerClick: (playerKey: String) -> Unit = {},
    val onTabSelect: (StatsTab) -> Unit = {},
    val onLegendSortSelect: (LegendSort) -> Unit = {},
)

@Composable
internal fun StatsScreen(uiState: StatsUiState, actions: StatsActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_stats_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_stats_back),
                        onClick = actions.onBackClick,
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
            StatsUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            StatsUiState.Empty -> StatsMessage(stringResource(R.string.feature_stats_empty), contentModifier)

            is StatsUiState.Success -> Column(contentModifier) {
                PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                    StatsTab.entries.forEach { tab ->
                        Tab(
                            selected = tab == uiState.selectedTab,
                            onClick = { actions.onTabSelect(tab) },
                            text = { Text(stringResource(tab.label)) },
                        )
                    }
                }
                when (uiState.selectedTab) {
                    StatsTab.OVERVIEW -> OverviewTab(uiState, onPlayerClick = actions.onPlayerClick)
                    StatsTab.LEGENDS -> LegendsTab(uiState, onSortSelect = actions.onLegendSortSelect)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(state: StatsUiState.Success, onPlayerClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.padding(RiftTheme.spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                SummaryCard(
                    label = stringResource(R.string.feature_stats_total_games_label),
                    value = state.totalGames.toString(),
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    label = stringResource(R.string.feature_stats_most_played_legend),
                    value = state.mostPlayedLegend?.name ?: stringResource(R.string.feature_stats_no_legend),
                    detail = state.mostPlayedLegend?.let {
                        pluralStringResource(R.plurals.feature_stats_games, it.games, it.games)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { SectionHeading(R.string.feature_stats_top_players) }
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.feature_stats_top_players_hint,
                    state.minGamesForRanking,
                    state.minGamesForRanking,
                ),
                style = RiftTheme.typography.bodySmall,
                color = RiftTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = RiftTheme.spacing.lg),
            )
        }
        if (state.topPlayers.isEmpty()) {
            item { BodyText(stringResource(R.string.feature_stats_top_players_empty)) }
        } else {
            itemsIndexed(state.topPlayers, key = { _, player -> "top-${player.key}" }) { index, player ->
                PlayerStatRow(player = player, onClick = { onPlayerClick(player.key) }, rank = index + 1)
            }
        }
        item { SectionHeading(R.string.feature_stats_all_players) }
        itemsIndexed(state.players, key = { _, player -> "all-${player.key}" }) { _, player ->
            PlayerStatRow(player = player, onClick = { onPlayerClick(player.key) })
        }
    }
}

@Composable
private fun LegendsTab(state: StatsUiState.Success, onSortSelect: (LegendSort) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                Text(text = stringResource(R.string.feature_stats_sort_label), style = RiftTheme.typography.labelLarge)
                LegendSort.entries.forEach { sort ->
                    FilterChip(
                        selected = sort == state.legendSort,
                        onClick = { onSortSelect(sort) },
                        label = { Text(stringResource(sort.label)) },
                    )
                }
            }
        }
        if (state.legends.isEmpty()) {
            item { BodyText(stringResource(R.string.feature_stats_no_legend)) }
        } else {
            itemsIndexed(state.legends, key = { _, legend -> legend.legendId }) { _, legend ->
                LegendStatRow(legend = legend)
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier, detail: String? = null) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
        ) {
            Text(text = label, style = RiftTheme.typography.labelMedium, color = RiftTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = RiftTheme.typography.headlineSmall)
            if (detail != null) {
                Text(text = detail, style = RiftTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SectionHeading(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = RiftTheme.typography.titleSmall,
        color = RiftTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = RiftTheme.spacing.lg, end = RiftTheme.spacing.lg, top = RiftTheme.spacing.xl)
            .semantics { heading() },
    )
}

@Composable
internal fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.bodyMedium,
        color = RiftTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
    )
}

@Composable
internal fun StatsMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(RiftTheme.spacing.xl), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = RiftTheme.typography.bodyLarge,
            color = RiftTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val StatsTab.label: Int
    get() = when (this) {
        StatsTab.OVERVIEW -> R.string.feature_stats_tab_overview
        StatsTab.LEGENDS -> R.string.feature_stats_tab_legends
    }

private val LegendSort.label: Int
    get() = when (this) {
        LegendSort.GAMES -> R.string.feature_stats_sort_games
        LegendSort.WIN_RATE -> R.string.feature_stats_sort_win_rate
        LegendSort.NAME -> R.string.feature_stats_sort_name
    }

internal val previewPlayers = persistentListOf(
    PlayerRowUiState("name:ana", "Ana", games = 7, wins = 5, winRatePercent = 71),
    PlayerRowUiState("name:ben", "Ben", games = 6, wins = 2, winRatePercent = 33),
    PlayerRowUiState("name:cleo", "Cleo", games = 2, wins = 1, winRatePercent = 50),
)

internal val previewLegends = persistentListOf(
    LegendRowUiState("jinx", "Jinx", null, games = 6, winRatePercent = 67),
    LegendRowUiState("viktor", "Viktor", null, games = 4, winRatePercent = 25),
)

internal val previewStatsState = StatsUiState.Success(
    totalGames = 8,
    mostPlayedLegend = previewLegends[0],
    topPlayers = persistentListOf(previewPlayers[0], previewPlayers[1]),
    players = previewPlayers,
    legends = previewLegends,
    minGamesForRanking = 3,
)

@PreviewRiftThemes
@Composable
private fun StatsScreenOverviewPreview() {
    RiftTheme {
        StatsScreen(uiState = previewStatsState, actions = StatsActions())
    }
}

@PreviewRiftThemes
@Composable
private fun StatsScreenLegendsPreview() {
    RiftTheme {
        StatsScreen(uiState = previewStatsState.copy(selectedTab = StatsTab.LEGENDS), actions = StatsActions())
    }
}

@PreviewRiftThemes
@Composable
private fun StatsScreenEmptyPreview() {
    RiftTheme {
        StatsScreen(uiState = StatsUiState.Empty, actions = StatsActions())
    }
}
