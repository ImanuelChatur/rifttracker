package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.reduce
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId

/** A stored game: its fixed [config] plus the ordered event log. Reduce it with `reduce(config, events)`. */
public data class GameRecord(
    val id: GameId,
    val config: GameConfig,
    val events: List<GameEvent>,
    val createdAt: Long,
    val endedAt: Long? = null,
)

/**
 * The config for a rematch (or "Play again") of this game: same players, seats, teams and initial
 * counters as the original config, with the target score the game ended on.
 */
public fun GameRecord.rematchConfig(): GameConfig = config.copy(targetScore = reduce(config, events).targetScore)
