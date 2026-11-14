package com.rifttally.core.game

/** Returns the event log without its last event (unlimited undo). Undoing an empty log returns it unchanged. */
public fun List<GameEvent>.undo(): List<GameEvent> = dropLast(1)
