package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.common.dispatchers.DefaultDispatcher
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.game.toStatsGame
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.stats.PlayerKey
import com.rifttally.core.stats.headToHeads
import com.rifttally.core.stats.legendStats
import com.rifttally.core.stats.playerStats
import com.rifttally.feature.stats.navigation.PlayerStatsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** One player's stats plus their head-to-head record against each opponent. */
@HiltViewModel
internal class PlayerStatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
    // Stats replay every game's log: keep that work off the main thread.
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Navigation stores route arguments under their property names (same approach as the tracker).
    private val key = PlayerKey(checkNotNull(savedStateHandle.get<String>(PlayerStatsRoute::playerKey.name)))

    val uiState: StateFlow<PlayerStatsUiState> = combine(
        gameRepository.endedGames(),
        legendRepository.legends,
    ) { records, legends ->
        val games = records.map { it.toStatsGame() }
        val stats = playerStats(games, key) ?: return@combine PlayerStatsUiState.NotFound
        val legendsById = legends.associateBy { it.id }
        val favorite = stats.favoriteLegendId?.let { id ->
            // Show the Legend's overall numbers next to it, like the Legend tab.
            legendStats(games).firstOrNull { it.legendId == id }?.toRow(legendsById)
        }
        PlayerStatsUiState.Success(
            name = stats.name,
            games = stats.games,
            wins = stats.wins,
            winRatePercent = stats.winRate.toPercent(),
            currentWinStreak = stats.currentWinStreak,
            favoriteLegend = favorite,
            headToHead = headToHeads(games, key).map {
                HeadToHeadUiState(
                    opponentKey = it.opponent.value,
                    opponentName = it.opponentName,
                    games = it.games,
                    wins = it.wins,
                    opponentWins = it.opponentWins,
                )
            }.toImmutableList(),
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerStatsUiState.Loading)
}
