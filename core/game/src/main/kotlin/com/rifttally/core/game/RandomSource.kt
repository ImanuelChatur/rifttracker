package com.rifttally.core.game

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.PlayerId

/** Random numbers for the first-player pick. The app uses `SecureRandom`; tests inject a fixed one. */
public fun interface RandomSource {
    /** A uniformly random int in `0 until until` ([until] > 0). */
    public fun nextInt(until: Int): Int
}

/** Picks who goes first among all of [config]'s players, or `null` if it has none. */
public fun pickFirstPlayer(config: GameConfig, random: RandomSource): PlayerId? {
    val players = config.players
    if (players.isEmpty()) return null
    return players[random.nextInt(players.size).coerceIn(players.indices)].id
}
