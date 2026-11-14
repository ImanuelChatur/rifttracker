package com.rifttally.core.stats

/**
 * A player's record against one [opponent], see [headToHead].
 *
 * @property opponentName the opponent's name in their most recent game.
 * @property games games both played on different sides (2v2 teammates aren't opponents).
 * @property wins of those, games the player's side won.
 * @property opponentWins of those, games the opponent's side won. In 3–4 player games someone else may
 *   have won, so `wins + opponentWins` can be less than [games].
 */
public data class HeadToHead(
    val player: PlayerKey,
    val opponent: PlayerKey,
    val opponentName: String,
    val games: Int,
    val wins: Int,
    val opponentWins: Int,
)
