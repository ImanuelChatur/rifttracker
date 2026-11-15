package com.rifttally.core.game

import com.rifttally.core.model.Counter
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId

/**
 * Immutable snapshot of a game, produced by [reduce].
 *
 * @property config the config this state was derived from (players, sides, Legends…).
 * @property points points per side; contains every side of [config], in config order.
 * @property counters counters per player in insertion order; contains every player of [config].
 * @property targetScore current target score (always ≥ 1).
 * @property isEnded whether a [GameEvent.GameEnded] was applied.
 * @property winner the winner named when the game ended, or `null` (no winner, or not ended).
 * @property firstPlayer who goes first (the latest valid [GameEvent.FirstPlayerChosen]), or `null`.
 */
public data class GameState(
    val config: GameConfig,
    val points: Map<SideId, Int>,
    val counters: Map<PlayerId, List<Counter>>,
    val targetScore: Int,
    val isEnded: Boolean = false,
    val winner: SideId? = null,
    val firstPlayer: PlayerId? = null,
) {
    /** Points of [sideId], or 0 if the side is unknown. */
    public fun pointsOf(sideId: SideId): Int = points[sideId] ?: 0

    /** Counters of [playerId] in display order, or empty if the player is unknown. */
    public fun countersOf(playerId: PlayerId): List<Counter> = counters[playerId].orEmpty()
}
