package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rifttally.core.database.model.GameEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * The event log. Writes go through [append] and [undoLast], which also keep the owning game's
 * `updated_at` / `ended_at` in sync, atomically.
 */
@Dao
public abstract class GameEventDao {
    @Insert
    public abstract suspend fun insert(event: GameEventEntity)

    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY seq ASC")
    public abstract fun observeEvents(gameId: String): Flow<List<GameEventEntity>>

    @Query("SELECT MAX(seq) FROM game_event WHERE game_id = :gameId")
    public abstract suspend fun lastSeq(gameId: String): Int?

    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY seq DESC LIMIT 1")
    public abstract suspend fun lastEvent(gameId: String): GameEventEntity?

    /** Undo: removes the newest event of a game. */
    @Query(
        """
        DELETE FROM game_event
        WHERE game_id = :gameId AND seq = (SELECT MAX(seq) FROM game_event WHERE game_id = :gameId)
        """,
    )
    public abstract suspend fun deleteLast(gameId: String)

    @Query("UPDATE game SET updated_at = :updatedAt WHERE id = :gameId")
    protected abstract suspend fun touchGame(gameId: String, updatedAt: Long)

    @Query("UPDATE game SET updated_at = :updatedAt, ended_at = :endedAt WHERE id = :gameId")
    protected abstract suspend fun setGameEnded(gameId: String, updatedAt: Long, endedAt: Long?)

    /**
     * Appends an event after the game's newest one and bumps the game's `updated_at`.
     * When [endsGame] is true the game is also marked ended at [timestamp].
     */
    @Transaction
    public open suspend fun append(
        gameId: String,
        eventId: String,
        timestamp: Long,
        payload: String,
        endsGame: Boolean,
    ) {
        val seq = (lastSeq(gameId) ?: 0) + 1
        insert(GameEventEntity(id = eventId, gameId = gameId, seq = seq, timestamp = timestamp, payload = payload))
        if (endsGame) setGameEnded(gameId, updatedAt = timestamp, endedAt = timestamp) else touchGame(gameId, timestamp)
    }

    /**
     * Removes the game's newest event and bumps `updated_at`. When [reopensGame] is true (the removed
     * event ended the game) `ended_at` is cleared so the game is active again.
     */
    @Transaction
    public open suspend fun undoLast(gameId: String, updatedAt: Long, reopensGame: Boolean) {
        deleteLast(gameId)
        if (reopensGame) setGameEnded(gameId, updatedAt = updatedAt, endedAt = null) else touchGame(gameId, updatedAt)
    }
}
