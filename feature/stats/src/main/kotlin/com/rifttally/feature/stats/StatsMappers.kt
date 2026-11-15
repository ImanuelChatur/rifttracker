package com.rifttally.feature.stats

import com.rifttally.core.model.Legend
import com.rifttally.core.stats.LegendMatchup
import com.rifttally.core.stats.LegendStats
import com.rifttally.core.stats.PlayerStats
import com.rifttally.core.stats.WinRecord
import kotlin.math.roundToInt

/** A 0–1 rate as a whole percentage. */
internal fun Double.toPercent(): Int = (this * PERCENT).roundToInt()

internal fun PlayerStats.toRow(): PlayerRowUiState = PlayerRowUiState(
    key = key.value,
    name = name,
    games = games,
    wins = wins,
    winRatePercent = winRate.toPercent(),
)

internal fun LegendStats.toRow(legendsById: Map<String, Legend>): LegendRowUiState =
    legendRow(legendId, legendsById, games = games, winRate = winRate)

/** The opponent's line of a matchup, with the matchup's games and win rate. */
internal fun LegendMatchup.toOpponentRow(legendsById: Map<String, Legend>): LegendRowUiState =
    legendRow(opponentLegendId, legendsById, games = games, winRate = winRate)

private fun legendRow(
    legendId: String,
    legendsById: Map<String, Legend>,
    games: Int,
    winRate: Double,
): LegendRowUiState {
    val legend = legendsById[legendId]
    return LegendRowUiState(
        legendId = legendId,
        name = legend?.name ?: legendId,
        imageUri = legend?.imageUri,
        games = games,
        winRatePercent = winRate.toPercent(),
    )
}

/** The record as UI state, or `null` without games (nothing to show). */
internal fun WinRecord.toUiStateOrNull(): WinRateUiState? =
    if (games == 0) null else WinRateUiState(games = games, winRatePercent = winRate.toPercent())

private const val PERCENT = 100
