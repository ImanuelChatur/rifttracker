package com.rifttally.core.model

import kotlinx.serialization.Serializable

/**
 * Everything fixed at game start. The live state is derived from this plus the event log.
 *
 * @property sides point owners, in display order. Each player should belong to exactly one side.
 * @property players all players, in seat order.
 * @property targetScore starting target score (see [GameFormat.defaultTargetScore]).
 * @property initialCounters counters each player starts with (e.g. XP), in display order.
 */
@Serializable
public data class GameConfig(
    val format: GameFormat,
    val sides: List<Side>,
    val players: List<Player>,
    val targetScore: Int = format.defaultTargetScore,
    val initialCounters: Map<PlayerId, List<Counter>> = emptyMap(),
)
