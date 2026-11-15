package com.rifttally.feature.stats

import com.rifttally.core.stats.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface PlayerStatsUiState {
    data object Loading : PlayerStatsUiState

    /**
     * Nobody with this key played an ended game in [period] (e.g. their games were deleted, or they
     * didn't play in that range).
     */
    data class NotFound(val period: StatsPeriod = StatsPeriod.ALL_TIME) : PlayerStatsUiState

    /**
     * @property period the date range everything covers (from the Stats screen).
     * @property favoriteLegend the Legend they picked most, or `null` if they never picked one.
     * @property firstPlayer their win rate going first vs not, or `null` when none of their games had a
     *   first-player pick and a winner.
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
        val period: StatsPeriod = StatsPeriod.ALL_TIME,
        val firstPlayer: FirstPlayerUiState? = null,
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

/** Win rate when going first and when not; each `null` when there's no such game. */
internal data class FirstPlayerUiState(val goingFirst: WinRateUiState?, val notFirst: WinRateUiState?)
