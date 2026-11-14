package com.rifttally.feature.stats

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface PlayerStatsUiState {
    data object Loading : PlayerStatsUiState

    /** Nobody with this key played an ended game (e.g. their games were deleted). */
    data object NotFound : PlayerStatsUiState

    /**
     * @property favoriteLegend the Legend they picked most, or `null` if they never picked one.
     * @property headToHead their record against each opponent, most games first.
     */
    data class Success(
        val name: String,
        val games: Int,
        val wins: Int,
        val winRatePercent: Int,
        val currentWinStreak: Int,
        val favoriteLegend: LegendRowUiState?,
        val headToHead: ImmutableList<HeadToHeadUiState> = persistentListOf(),
    ) : PlayerStatsUiState
}

/** Record against one opponent. Someone else may have won some of [games] (3–4 players). */
internal data class HeadToHeadUiState(
    val opponentKey: String,
    val opponentName: String,
    val games: Int,
    val wins: Int,
    val opponentWins: Int,
)
