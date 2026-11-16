package com.rifttally.core.stats

/**
 * How [legendId] did against [opponentLegendId], see [legendMatchups]. Counted per game, however many
 * times either Legend was picked in it.
 *
 * @property games games where the two Legends were on different sides.
 * @property wins of those, games a side with [legendId] won.
 * @property losses of those, games a side with [opponentLegendId] (and without [legendId]) won. In 3–4
 *   player games someone else may have won (or nobody did), so `wins + losses` can be less than [games].
 */
public data class LegendMatchup(
    val legendId: String,
    val opponentLegendId: String,
    val games: Int,
    val wins: Int,
    val losses: Int,
) {
    /** [wins] / [games], from 0 to 1 (0 without games). */
    public val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}
