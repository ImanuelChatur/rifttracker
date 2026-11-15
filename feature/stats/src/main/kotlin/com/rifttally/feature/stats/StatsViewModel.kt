package com.rifttally.feature.stats

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Stats overview and the Legend tab, derived from the ended games in the selected date range. */
@HiltViewModel
internal class StatsViewModel @Inject constructor(
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
    gamesFilter: StatsGamesFilter,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(StatsTab.OVERVIEW)
    private val legendSort = MutableStateFlow(LegendSort.GAMES)
    private val period = MutableStateFlow(StatsPeriod.ALL_TIME)

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
        selectedTab.value = tab
    }

    fun onLegendSortSelect(sort: LegendSort) {
        legendSort.value = sort
    }

    fun onPeriodSelect(period: StatsPeriod) {
        this.period.value = period
    }

    private fun LegendSort.comparator(): Comparator<Pair<LegendRowUiState, LegendStats>> = when (this) {
        // legendStats is already sorted by games, then win rate, then id.
        LegendSort.GAMES -> compareByDescending { it.second.games }

        LegendSort.WIN_RATE -> compareByDescending<Pair<LegendRowUiState, LegendStats>> { it.second.winRate }
            .thenByDescending { it.second.games }

        LegendSort.NAME -> compareBy { it.first.name.lowercase() }
    }
}
