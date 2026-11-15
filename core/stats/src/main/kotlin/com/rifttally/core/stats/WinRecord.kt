package com.rifttally.core.stats

/** [wins] out of [games]. */
public data class WinRecord(val games: Int, val wins: Int) {
    /** [wins] / [games], from 0 to 1 (0 without games). */
    public val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}
