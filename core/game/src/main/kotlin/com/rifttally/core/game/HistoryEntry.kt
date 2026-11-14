package com.rifttally.core.game

/** One line of the history log: [event] and the [stateAfter] applying it. See [history]. */
public data class HistoryEntry(val event: GameEvent, val stateAfter: GameState)
