package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow

/** Stored games and their event logs. Every event is persisted the moment it's appended. */
public interface GameRepository {
    /** The most recently played game that hasn't ended, or `null`. */
    public val activeGame: Flow<GameRecord?>

    /** The game with [id], or `null` if it doesn't exist (or its data can't be read). */
    public fun observeGame(id: GameId): Flow<GameRecord?>

    /** Stores a new game with an empty event log and returns its id. */
    public suspend fun createGame(config: GameConfig): GameId

    /** Appends [event] to the game's log. A [GameEvent.GameEnded] also marks the game ended. */
    public suspend fun appendEvent(gameId: GameId, event: GameEvent)

    /** Removes the newest event (undo). Undoing a [GameEvent.GameEnded] makes the game active again. */
    public suspend fun undoLastEvent(gameId: GameId)
}
