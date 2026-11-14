package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
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
