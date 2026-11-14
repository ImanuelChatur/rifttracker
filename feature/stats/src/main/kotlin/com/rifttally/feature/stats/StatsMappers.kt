package com.rifttally.feature.stats

import com.rifttally.core.model.Legend
import com.rifttally.core.stats.LegendStats
import com.rifttally.core.stats.PlayerStats
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

internal fun LegendStats.toRow(legendsById: Map<String, Legend>): LegendRowUiState {
    val legend = legendsById[legendId]
    return LegendRowUiState(
        legendId = legendId,
        name = legend?.name ?: legendId,
        imageUri = legend?.imageUri,
        games = games,
        winRatePercent = winRate.toPercent(),
    )
}

private const val PERCENT = 100
