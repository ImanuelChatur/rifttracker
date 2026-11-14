package com.rifttally.core.stats

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.SideId

/**
 * One ended game as input for the stats functions. The caller builds it from a stored game, so this
 * module needs neither the database nor the event types.
 *
 * @property id the game's id.
 * @property config players, sides and format, exactly as stored.
 * @property createdAt epoch millis when the game was created.
 * @property endedAt epoch millis when the game ended.
 * @property targetScore the target score when the game ended (it can change during a game).
 * @property winner the side named as winner, or `null` when the game ended without one. Such a game
 *   counts as played, not won, for everyone.
 * @property pointChanges every point change before the game ended, in log order. Other events
 *   (counters, target changes) don't affect stats and are left out.
 */
public data class StatsGame(
    val id: String,
    val config: GameConfig,
    val createdAt: Long,
    val endedAt: Long,
    val targetScore: Int,
    val winner: SideId?,
    val pointChanges: List<PointChange>,
)
