package com.rifttally.feature.tracker

import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameState
import com.rifttally.core.game.HistoryEntry
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.SideId

/**
 * Turns the replayed log into history lines, newest first. [sideNames] maps each side to its display
 * name. Events that changed nothing (unknown side, after the end, …) are dropped.
 */
internal fun historyItems(
    config: GameConfig,
    initial: GameState,
    entries: List<HistoryEntry>,
    sideNames: Map<SideId, String>,
): List<HistoryItemUiState> {
    val playerNames = config.players.associate { it.id to it.name }
    return entries.mapIndexedNotNull { index, entry ->
        val before = if (index == 0) initial else entries[index - 1].stateAfter
        val after = entry.stateAfter
        // Events that changed nothing (e.g. after the game ended) aren't worth a line.
        if (after == before) return@mapIndexedNotNull null
        when (val event = entry.event) {
            is GameEvent.PointsAdjusted -> sideNames[event.sideId]?.let { name ->
                HistoryItemUiState.Points(event.id, name, event.delta, after.pointsOf(event.sideId))
            }

            is GameEvent.CounterAdded -> playerNames[event.playerId]?.let { name ->
                HistoryItemUiState.CounterAdded(event.id, name, event.label)
            }

            is GameEvent.CounterAdjusted -> {
                val name = playerNames[event.playerId]
                val counter = after.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) {
                    HistoryItemUiState.CounterChanged(event.id, name, counter.label, event.delta, counter.value)
                } else {
                    null
                }
            }

            is GameEvent.CounterRemoved -> {
                val name = playerNames[event.playerId]
                val counter = before.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) {
                    HistoryItemUiState.CounterRemoved(event.id, name, counter.label)
                } else {
                    null
                }
            }

            is GameEvent.TargetScoreChanged -> HistoryItemUiState.TargetChanged(event.id, after.targetScore)

            is GameEvent.GameEnded -> HistoryItemUiState.GameEnded(event.id, event.winner?.let(sideNames::get))
        }
    }.asReversed()
}
