package com.rifttally.feature.history

import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.history
import com.rifttally.core.game.reduce
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.SideId

/**
 * The replayed log as [GameLogItem]s, oldest first. [sideNames] maps each side to its display name.
 * Events that changed nothing (unknown side, after the end, …) are dropped.
 *
 * Copied from the tracker's `historyItems` (which lists newest first); dedupe at the merge.
 */
internal fun gameLogItems(
    config: GameConfig,
    events: List<GameEvent>,
    sideNames: Map<SideId, String>,
): List<GameLogItem> {
    val playerNames = config.players.associate { it.id to it.name }
    val entries = history(config, events)
    val initial = reduce(config, emptyList())
    return entries.mapIndexedNotNull { index, entry ->
        val before = if (index == 0) initial else entries[index - 1].stateAfter
        val after = entry.stateAfter
        if (after == before) return@mapIndexedNotNull null
        when (val event = entry.event) {
            is GameEvent.PointsAdjusted -> sideNames[event.sideId]?.let { name ->
                GameLogItem.Points(event.id, name, event.delta, after.pointsOf(event.sideId))
            }

            is GameEvent.CounterAdded -> playerNames[event.playerId]?.let { name ->
                GameLogItem.CounterAdded(event.id, name, event.label)
            }

            is GameEvent.CounterAdjusted -> {
                val name = playerNames[event.playerId]
                val counter = after.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) {
                    GameLogItem.CounterChanged(event.id, name, counter.label, event.delta, counter.value)
                } else {
                    null
                }
            }

            is GameEvent.CounterRemoved -> {
                val name = playerNames[event.playerId]
                val counter = before.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) GameLogItem.CounterRemoved(event.id, name, counter.label) else null
            }

            is GameEvent.TargetScoreChanged -> GameLogItem.TargetChanged(event.id, after.targetScore)

            is GameEvent.GameEnded -> GameLogItem.GameEnded(event.id, event.winner?.let(sideNames::get))
        }
    }
}
