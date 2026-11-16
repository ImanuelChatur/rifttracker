package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.common.dispatchers.DefaultDispatcher
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.stats.LegendStats
import com.rifttally.core.stats.MIN_GAMES_FOR_RANKING
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.stats.firstPlayerRecord
import com.rifttally.core.stats.legendStats
import com.rifttally.core.stats.statsOverview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * Stats overview and the Legend tab, derived from the ended games in the selected date range. The tab,
 * sort and period are kept in [savedStateHandle], so they survive process death and recreation.
 */
@HiltViewModel
internal class StatsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
    gamesFilter: StatsGamesFilter,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val selectedTab = savedStateHandle.getStateFlow(KEY_TAB, StatsTab.OVERVIEW)
    private val legendSort = savedStateHandle.getStateFlow(KEY_SORT, LegendSort.GAMES)
    private val period = savedStateHandle.getStateFlow(KEY_PERIOD, StatsPeriod.ALL_TIME)

    val uiState: StateFlow<StatsUiState> = combine(
        gameRepository.endedGames(),
        legendRepository.legends,
        selectedTab,
        legendSort,
        period,
    ) { records, legends, tab, sort, period ->
        if (records.isEmpty()) return@combine StatsUiState.Empty
        val games = gamesFilter.gamesIn(records, period)
        if (games.isEmpty()) return@combine StatsUiState.EmptyPeriod(period)
        val legendsById = legends.associateBy { it.id }
        val overview = statsOverview(games)
        StatsUiState.Success(
            totalGames = overview.totalGames,
            mostPlayedLegend = overview.mostPlayedLegend?.toRow(legendsById),
            topPlayers = overview.topPlayers.map { it.toRow() }.toImmutableList(),
            players = overview.players.map { it.toRow() }.toImmutableList(),
            legends = legendStats(games).map { it.toRow(legendsById) to it }
                .sortedWith(sort.comparator())
                .map { it.first }
                .toImmutableList(),
            legendSort = sort,
            selectedTab = tab,
            minGamesForRanking = MIN_GAMES_FOR_RANKING,
            period = period,
            firstPlayer = firstPlayerRecord(games).toUiStateOrNull(),
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    fun onTabSelect(tab: StatsTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    fun onLegendSortSelect(sort: LegendSort) {
        savedStateHandle[KEY_SORT] = sort
    }

    fun onPeriodSelect(period: StatsPeriod) {
        savedStateHandle[KEY_PERIOD] = period
    }

    private fun LegendSort.comparator(): Comparator<Pair<LegendRowUiState, LegendStats>> = when (this) {
        // legendStats is already sorted by games, then win rate, then id.
        LegendSort.GAMES -> compareByDescending { it.second.games }

        LegendSort.WIN_RATE -> compareByDescending<Pair<LegendRowUiState, LegendStats>> { it.second.winRate }
            .thenByDescending { it.second.games }

        LegendSort.NAME -> compareBy { it.first.name.lowercase() }
    }

    private companion object {
        const val KEY_TAB = "stats_tab"
        const val KEY_SORT = "stats_legend_sort"
        const val KEY_PERIOD = "stats_period"
    }
}
