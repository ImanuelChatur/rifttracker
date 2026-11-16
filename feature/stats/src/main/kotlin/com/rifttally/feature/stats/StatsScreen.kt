package com.rifttally.feature.stats

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.displayLargeEmphasized
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.list.CenteredList
import com.rifttally.core.ui.list.EmptyState
import com.rifttally.core.ui.list.ListSectionHeader
import com.rifttally.core.ui.list.SegmentedTile
import com.rifttally.core.ui.list.skeletonTiles
import com.rifttally.feature.stats.navigation.LegendStatsRoute
import com.rifttally.feature.stats.navigation.PlayerStatsRoute
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun StatsRoute(
    onBackClick: () -> Unit,
    onPlayerClick: (PlayerStatsRoute) -> Unit,
    onLegendClick: (LegendStatsRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Detail screens open with the range shown here (rows only exist in Success).
    val period = (uiState as? StatsUiState.Success)?.period ?: StatsPeriod.ALL_TIME
    StatsScreen(
        uiState = uiState,
        actions = StatsActions(
            onBackClick = onBackClick,
            onPlayerClick = { key -> onPlayerClick(PlayerStatsRoute(playerKey = key, period = period.name)) },
            onLegendClick = { id -> onLegendClick(LegendStatsRoute(legendId = id, period = period.name)) },
            onTabSelect = viewModel::onTabSelect,
            onLegendSortSelect = viewModel::onLegendSortSelect,
            onPeriodSelect = viewModel::onPeriodSelect,
        ),
        modifier = modifier,
    )
}

/** Every user action on the stats screen. */
internal data class StatsActions(
    val onBackClick: () -> Unit = {},
    val onPlayerClick: (playerKey: String) -> Unit = {},
    val onLegendClick: (legendId: String) -> Unit = {},
    val onTabSelect: (StatsTab) -> Unit = {},
    val onLegendSortSelect: (LegendSort) -> Unit = {},
    val onPeriodSelect: (StatsPeriod) -> Unit = {},
)

/**
 * Stats: Overview and Legends tabs. The Overview's hero (DESIGN.md §2.2) is the total number of games;
 * everything else is a calm segmented list (DESIGN.md §2.3, §8).
 */
@Composable
internal fun StatsScreen(uiState: StatsUiState, actions: StatsActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_stats_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = RiftIcons.ArrowBack,
                        contentDescription = stringResource(R.string.feature_stats_back),
                        onClick = actions.onBackClick,
                    )
                },
            )
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        when (uiState) {
            StatsUiState.Loading -> CenteredList(scaffoldPadding = padding) { skeletonTiles() }

            StatsUiState.Empty -> EmptyState(
                icon = RiftIcons.Star,
                text = stringResource(R.string.feature_stats_empty),
                actionLabel = stringResource(R.string.feature_stats_empty_action),
                onAction = actions.onBackClick,
                modifier = Modifier.padding(padding),
            )

            is StatsUiState.EmptyPeriod -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                StatsPeriodChips(
                    selected = uiState.period,
                    onSelect = actions.onPeriodSelect,
                    modifier = Modifier.padding(vertical = RiftTheme.spacing.sm),
                )
                // The chips stay, so the range can be changed back without leaving.
                EmptyState(
                    icon = RiftIcons.DateRange,
                    text = stringResource(R.string.feature_stats_period_empty),
                    actionLabel = stringResource(R.string.feature_stats_period_empty_action),
                    onAction = { actions.onPeriodSelect(StatsPeriod.ALL_TIME) },
                    modifier = Modifier.weight(1f),
                )
            }

            is StatsUiState.Success -> {
                val layoutDirection = LocalLayoutDirection.current
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                ) {
                    StatsPeriodChips(
                        selected = uiState.period,
                        onSelect = actions.onPeriodSelect,
                        modifier = Modifier
                            .padding(
                                start = padding.calculateStartPadding(layoutDirection),
                                end = padding.calculateEndPadding(layoutDirection),
                            )
                            .padding(vertical = RiftTheme.spacing.sm),
                    )
                    StatsTabs(selected = uiState.selectedTab, onSelect = actions.onTabSelect)
                    // The chips and tab row consume the top inset; the list keeps the side and bottom ones.
                    val listPadding = PaddingValues(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    )
                    when (uiState.selectedTab) {
                        StatsTab.OVERVIEW -> OverviewTab(uiState, listPadding, onPlayerClick = actions.onPlayerClick)

                        StatsTab.LEGENDS -> LegendsTab(
                            state = uiState,
                            listPadding = listPadding,
                            onSortSelect = actions.onLegendSortSelect,
                            onLegendClick = actions.onLegendClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTabs(selected: StatsTab, onSelect: (StatsTab) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        PrimaryTabRow(
            selectedTabIndex = selected.ordinal,
            modifier = Modifier.widthIn(max = RiftTheme.sizes.listMaxContentWidth),
            containerColor = RiftTheme.colorScheme.background,
            divider = {},
        ) {
            StatsTab.entries.forEach { tab ->
                Tab(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    text = { Text(stringResource(tab.label), style = RiftTheme.typography.titleSmall) },
                    unselectedContentColor = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    state: StatsUiState.Success,
    listPadding: PaddingValues,
    onPlayerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredList(scaffoldPadding = listPadding, modifier = modifier) {
        item(key = "hero") { TotalGamesHero(totalGames = state.totalGames, period = state.period) }
        item(key = "legend") {
            MostPlayedLegendTile(
                legend = state.mostPlayedLegend,
                modifier = Modifier.padding(top = RiftTheme.spacing.sm),
            )
        }
        state.firstPlayer?.let { firstPlayer ->
            item(key = "first_player") {
                FirstPlayerTile(firstPlayer = firstPlayer, modifier = Modifier.padding(top = RiftTheme.spacing.xxs))
            }
        }

        item(key = "top_header") {
            Column {
                ListSectionHeader(stringResource(R.string.feature_stats_top_players))
                Text(
                    text = pluralStringResource(
                        R.plurals.feature_stats_top_players_hint,
                        state.minGamesForRanking,
                        state.minGamesForRanking,
                    ),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = RiftTheme.spacing.lg,
                        end = RiftTheme.spacing.lg,
                        bottom = RiftTheme.spacing.sm,
                    ),
                )
            }
        }
        if (state.topPlayers.isEmpty()) {
            item(key = "top_empty") {
                SegmentedTile(index = 0, count = 1) {
                    TileMessage(stringResource(R.string.feature_stats_top_players_empty))
                }
            }
        } else {
            itemsIndexed(state.topPlayers, key = { _, player -> "top-${player.key}" }) { index, player ->
                SegmentedTile(index = index, count = state.topPlayers.size) {
                    PlayerStatRow(player = player, onClick = { onPlayerClick(player.key) }, rank = index + 1)
                }
            }
        }

        item(key = "all_header") { ListSectionHeader(stringResource(R.string.feature_stats_all_players)) }
        itemsIndexed(state.players, key = { _, player -> "all-${player.key}" }) { index, player ->
            SegmentedTile(index = index, count = state.players.size) {
                PlayerStatRow(player = player, onClick = { onPlayerClick(player.key) })
            }
        }
    }
}

@Composable
private fun LegendsTab(
    state: StatsUiState.Success,
    listPadding: PaddingValues,
    onSortSelect: (LegendSort) -> Unit,
    onLegendClick: (legendId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredList(scaffoldPadding = listPadding, modifier = modifier) {
        item(key = "sort") {
            // Sort chips (DESIGN.md §9): small radius, one row that scrolls horizontally.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = RiftTheme.spacing.sm, bottom = RiftTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.feature_stats_sort_label),
                    style = RiftTheme.typography.labelLarge,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
                LegendSort.entries.forEach { sort ->
                    FilterChip(
                        selected = sort == state.legendSort,
                        onClick = { onSortSelect(sort) },
                        label = { Text(stringResource(sort.label)) },
                        shape = RiftTheme.shapes.small,
                    )
                }
            }
        }
        if (state.legends.isEmpty()) {
            item(key = "legends_empty") {
                SegmentedTile(index = 0, count = 1) { TileMessage(stringResource(R.string.feature_stats_no_legend)) }
            }
        } else {
            itemsIndexed(state.legends, key = { _, legend -> legend.legendId }) { index, legend ->
                SegmentedTile(index = index, count = state.legends.size) {
                    LegendStatRow(legend = legend, onClick = { onLegendClick(legend.legendId) })
                }
            }
        }
    }
}

/** The Overview's hero: total games in emphasized display type on an emphasized container. */
@Composable
private fun TotalGamesHero(totalGames: Int, period: StatsPeriod, modifier: Modifier = Modifier) {
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
            Text(
                text = labelWithPeriod(stringResource(R.string.feature_stats_total_games_label), period),
                style = RiftTheme.typography.labelLarge,
            )
            Text(
                text = totalGames.toString(),
                style = RiftTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** The most played Legend: portrait (or fallback), name and games. Quiet tonal tile. */
@Composable
private fun MostPlayedLegendTile(legend: LegendRowUiState?, modifier: Modifier = Modifier) {
    SegmentedTile(index = 0, count = 1, modifier = modifier) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            if (legend != null) {
                LegendAvatar(
                    legendName = legend.name,
                    imageUri = legend.imageUri,
                    playerName = legend.name,
                    playerColor = PlayerColors.Grey,
                    size = RiftTheme.sizes.avatarSmall,
                    contentDescription = null,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.feature_stats_most_played_legend),
                    style = RiftTheme.typography.labelLarge,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = legend?.name ?: stringResource(R.string.feature_stats_no_legend),
                    style = RiftTheme.typography.titleLarge,
                )
            }
            if (legend != null) {
                Text(
                    text = pluralStringResource(R.plurals.feature_stats_games, legend.games, legend.games),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Going first wins 60% (5 games)" on a quiet tile. */
@Composable
private fun FirstPlayerTile(firstPlayer: WinRateUiState, modifier: Modifier = Modifier) {
    SegmentedTile(index = 0, count = 1, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.feature_stats_going_first_label),
                style = RiftTheme.typography.labelLarge,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.feature_stats_going_first_wins,
                    firstPlayer.games,
                    firstPlayer.winRatePercent,
                    firstPlayer.games,
                ),
                style = RiftTheme.typography.bodyLarge,
            )
        }
    }
}

/** One sentence inside a tile (inline empty state of a section). */
@Composable
internal fun TileMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.bodyMedium,
        color = RiftTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
    )
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
    firstPlayer = WinRateUiState(games = 5, winRatePercent = 60),
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

@PreviewRiftThemes
@Composable
private fun StatsScreenEmptyPeriodPreview() {
    RiftTheme {
        StatsScreen(uiState = StatsUiState.EmptyPeriod(StatsPeriod.LAST_30_DAYS), actions = StatsActions())
    }
}
