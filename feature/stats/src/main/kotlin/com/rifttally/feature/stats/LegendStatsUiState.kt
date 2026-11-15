package com.rifttally.feature.stats

import com.rifttally.core.stats.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface LegendStatsUiState {
    data object Loading : LegendStatsUiState

    /** Nobody picked this Legend in an ended game in [period]. */
    data class NotFound(val period: StatsPeriod = StatsPeriod.ALL_TIME) : LegendStatsUiState

    /**
     * @property legend the Legend's games and win rate (counted per pick, like the Legend tab).
     * @property wins picks whose side won.
     * @property matchups opposing Legends, most games first.
     */
    data class Success(
        val legend: LegendRowUiState,
        val wins: Int,
        val matchups: ImmutableList<MatchupUiState> = persistentListOf(),
        val period: StatsPeriod = StatsPeriod.ALL_TIME,
    ) : LegendStatsUiState
}

/**
 * The Legend's record against [opponent]. [losses] are games the opponent's side won; someone else may
 * have won the rest (3–4 players) or nobody did.
 */
internal data class MatchupUiState(val opponent: LegendRowUiState, val wins: Int, val losses: Int)
