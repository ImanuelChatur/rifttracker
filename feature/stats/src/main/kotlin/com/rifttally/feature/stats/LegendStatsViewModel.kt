package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.common.dispatchers.DefaultDispatcher
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.stats.legendMatchups
import com.rifttally.core.stats.legendStats
import com.rifttally.feature.stats.navigation.LegendStatsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** One Legend's games and win rate, and its record against each opposing Legend, in the route's date range. */
@HiltViewModel
internal class LegendStatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
    gamesFilter: StatsGamesFilter,
    // Stats replay every game's log: keep that work off the main thread.
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Navigation stores route arguments under their property names (same approach as the tracker).
    private val legendId = checkNotNull(savedStateHandle.get<String>(LegendStatsRoute::legendId.name))
    private val period = statsPeriodOf(savedStateHandle.get<String>(LegendStatsRoute::period.name))

    val uiState: StateFlow<LegendStatsUiState> = combine(
        gameRepository.endedGames(),
        legendRepository.legends,
    ) { records, legends ->
        val games = gamesFilter.gamesIn(records, period)
        val stats = legendStats(games).firstOrNull { it.legendId == legendId }
            ?: return@combine LegendStatsUiState.NotFound(period)
        val legendsById = legends.associateBy { it.id }
        LegendStatsUiState.Success(
            legend = stats.toRow(legendsById),
            wins = stats.wins,
            matchups = legendMatchups(games, legendId).map { matchup ->
                MatchupUiState(
                    opponent = matchup.toOpponentRow(legendsById),
                    wins = matchup.wins,
                    losses = matchup.losses,
                )
            }.toImmutableList(),
            period = period,
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LegendStatsUiState.Loading)
}
