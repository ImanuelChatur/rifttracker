package com.rifttally.core.stats

/**
 * How [legendId] did against [opponentLegendId], see [legendMatchups]. Counted per pair of picks on
 * different sides, so a game where both opponents picked the same Legend counts twice for it.
 *
 * @property games pairs of picks that met.
 * @property wins of those, pairs where [legendId]'s side won.
 * @property losses of those, pairs where [opponentLegendId]'s side won. In 3–4 player games someone else
 *   may have won (or nobody did), so `wins + losses` can be less than [games].
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
