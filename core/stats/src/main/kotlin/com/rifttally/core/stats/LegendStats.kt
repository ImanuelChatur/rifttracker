package com.rifttally.core.stats

/**
 * How a Legend did, see [legendStats]. Counted per pick: a game where two players picked the same
 * Legend counts twice (once won, once lost in a 1v1 mirror match).
 *
 * @property games times the Legend was picked in a game.
 * @property wins picks whose side was named winner.
 */
public data class LegendStats(val legendId: String, val games: Int, val wins: Int) {
    /** [wins] / [games], from 0 to 1 (0 without games). */
    public val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}
