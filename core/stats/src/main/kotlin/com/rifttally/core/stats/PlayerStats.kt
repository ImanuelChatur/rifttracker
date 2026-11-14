package com.rifttally.core.stats

/**
 * One person's record over all games, see [playerStats].
 *
 * @property name their name in the most recent game.
 * @property games games played, including games that ended without a winner.
 * @property wins games their side was named winner (in 2v2 both teammates get the win).
 * @property currentWinStreak wins in a row, counting back from their most recent game.
 * @property favoriteLegendId the Legend they picked most often (ties: the more recently picked), or
 *   `null` if they never picked one.
 */
public data class PlayerStats(
    val key: PlayerKey,
    val name: String,
    val games: Int,
    val wins: Int,
    val currentWinStreak: Int,
    val favoriteLegendId: String?,
) {
    /** [wins] / [games], from 0 to 1 (0 without games). */
    public val winRate: Double get() = if (games == 0) 0.0 else wins.toDouble() / games
}
