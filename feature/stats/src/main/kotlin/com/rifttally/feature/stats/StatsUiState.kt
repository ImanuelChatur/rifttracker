package com.rifttally.feature.stats

import com.rifttally.core.stats.StatsPeriod
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface StatsUiState {
    data object Loading : StatsUiState

    /** No game has ended yet. */
    data object Empty : StatsUiState

    /** Games have ended, but none in [period]: the range chips stay so it can be changed back. */
    data class EmptyPeriod(val period: StatsPeriod) : StatsUiState

    /**
     * @property period the date range everything below covers.
     * @property mostPlayedLegend `null` when nobody ever picked a Legend.
     * @property firstPlayer how often the first player won, or `null` when no game had a pick and a winner.
     * @property topPlayers players with at least [minGamesForRanking] games, best win rate first.
     * @property players everyone, most games first (each opens their stats).
     * @property legends every picked Legend, ordered by [legendSort] (each opens its stats).
     */
    data class Success(
        val totalGames: Int,
        val mostPlayedLegend: LegendRowUiState?,
        val topPlayers: ImmutableList<PlayerRowUiState> = persistentListOf(),
        val players: ImmutableList<PlayerRowUiState> = persistentListOf(),
        val legends: ImmutableList<LegendRowUiState> = persistentListOf(),
        val legendSort: LegendSort = LegendSort.GAMES,
        val selectedTab: StatsTab = StatsTab.OVERVIEW,
        val minGamesForRanking: Int = 0,
        val period: StatsPeriod = StatsPeriod.ALL_TIME,
        val firstPlayer: WinRateUiState? = null,
    ) : StatsUiState
}

/** A player's line in a list. [key] identifies them across games (see `PlayerKey`). */
internal data class PlayerRowUiState(
    val key: String,
    val name: String,
    val games: Int,
    val wins: Int,
    val winRatePercent: Int,
)

/** A Legend's line. [name] falls back to the id when the Legend isn't in the catalog any more. */
internal data class LegendRowUiState(
    val legendId: String,
    val name: String,
    val imageUri: String?,
    val games: Int,
    val winRatePercent: Int,
)

/** [winRatePercent] of [games] (e.g. going first). */
internal data class WinRateUiState(val games: Int, val winRatePercent: Int)

internal enum class StatsTab {
    OVERVIEW,
    LEGENDS,
}

/** Orders of the Legend tab. */
internal enum class LegendSort {
    GAMES,
    WIN_RATE,
    NAME,
}
