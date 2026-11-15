package com.rifttally.core.game

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.SideId

/**
 * Turns the replayed log ([entries], from [history]) into display lines, oldest first. [sideNames] maps
 * each side to its display name; [initial] is the state before the first event. Events that changed
 * nothing (unknown side, after the end, …) are dropped, except a valid first-player pick: re-rolling
 * the same player is still a roll worth showing.
 */
public fun gameLog(
    config: GameConfig,
    entries: List<HistoryEntry>,
    sideNames: Map<SideId, String>,
    initial: GameState = reduce(config, emptyList()),
): List<GameLogLine> {
    val playerNames = config.players.associate { it.id to it.name }
    return entries.mapIndexedNotNull { index, entry ->
        val before = if (index == 0) initial else entries[index - 1].stateAfter
        val after = entry.stateAfter
        val event = entry.event
        val isPick = event is GameEvent.FirstPlayerChosen && !before.isEnded && event.playerId in playerNames
        if (after == before && !isPick) return@mapIndexedNotNull null
        when (event) {
            is GameEvent.PointsAdjusted -> sideNames[event.sideId]?.let { name ->
                GameLogLine.Points(event.id, name, event.delta, after.pointsOf(event.sideId))
            }

            is GameEvent.CounterAdded -> playerNames[event.playerId]?.let { name ->
                GameLogLine.CounterAdded(event.id, name, event.label)
            }

            is GameEvent.CounterAdjusted -> {
                val name = playerNames[event.playerId]
                val counter = after.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) {
                    GameLogLine.CounterChanged(event.id, name, counter.label, event.delta, counter.value)
                } else {
                    null
                }
            }

            is GameEvent.CounterRemoved -> {
                val name = playerNames[event.playerId]
                val counter = before.countersOf(event.playerId).firstOrNull { it.id == event.counterId }
                if (name != null && counter != null) GameLogLine.CounterRemoved(event.id, name, counter.label) else null
            }

            is GameEvent.TargetScoreChanged -> GameLogLine.TargetChanged(event.id, after.targetScore)

            is GameEvent.FirstPlayerChosen -> playerNames[event.playerId]?.let { name ->
                GameLogLine.FirstPlayer(event.id, name)
            }

            is GameEvent.GameEnded -> GameLogLine.GameEnded(event.id, event.winner?.let(sideNames::get))
        }
    }
}
