package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow

/** Stored games and their event logs. Every event is persisted the moment it's appended. */
public interface GameRepository {
    /** The most recently played game that hasn't ended, or `null`. */
    public val activeGame: Flow<GameRecord?>

    /** Up to [limit] ended games, most recently ended first (read-only history). */
    public fun recentGames(limit: Int): Flow<List<GameRecord>>

    /** Every ended game with its event log, most recently ended first (match history and stats). */
    public fun endedGames(): Flow<List<GameRecord>>

    /** The game with [id], or `null` if it doesn't exist (or its data can't be read). */
    public fun observeGame(id: GameId): Flow<GameRecord?>

    /** Stores a new game with an empty event log and returns its id. */
    public suspend fun createGame(config: GameConfig): GameId

    /** Appends [event] to the game's log. A [GameEvent.GameEnded] also marks the game ended. */
    public suspend fun appendEvent(gameId: GameId, event: GameEvent)

    /**
     * Ends [oldGameId] with [endEvent] (unless it already ended) and creates a game with [newConfig], in
     * one transaction, so a crash can't leave two open games. Returns the new game's id.
     */
    public suspend fun restartGame(oldGameId: GameId, endEvent: GameEvent.GameEnded, newConfig: GameConfig): GameId

    /** Removes the newest event (undo). Undoing a [GameEvent.GameEnded] makes the game active again. */
    public suspend fun undoLastEvent(gameId: GameId)

    /** Deletes the game and its event log. Deleting an unknown id does nothing. */
    public suspend fun deleteGame(id: GameId)

    /**
     * Stores [record] again exactly as it was (id, timestamps, events), e.g. to undo [deleteGame].
     * Does nothing if a game with that id still exists.
     */
    public suspend fun restoreGame(record: GameRecord)
}
