package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.database.model.GameTimerEntity
import kotlinx.coroutines.flow.Flow

/**
 * The event log. Writes go through [append], [undoLast] and [restart], which also keep the owning
 * game's `updated_at` / `ended_at` in sync, atomically.
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

    @Insert
    protected abstract suspend fun insertGame(game: GameEntity)

    @Insert
    protected abstract suspend fun insertAll(events: List<GameEventEntity>)

    /** 1 if the game exists and hasn't ended, else 0. */
    @Query("SELECT COUNT(*) FROM game WHERE id = :gameId AND ended_at IS NULL")
    protected abstract suspend fun countActive(gameId: String): Int

    /** Same SQL as [GameDao.copyTimer]: keep them in sync. */
    @Query(
        """
        INSERT OR REPLACE INTO game_timer (game_id, duration_ms, elapsed_before_ms, running_since)
        SELECT :toGameId, duration_ms, elapsed_before_ms, running_since FROM game_timer WHERE game_id = :fromGameId
        """,
    )
    protected abstract suspend fun copyTimer(fromGameId: String, toGameId: String)

    @Upsert
    protected abstract suspend fun upsertTimer(timer: GameTimerEntity)

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

    /**
     * Restart, in one transaction: ends [oldGameId] with the given `GameEnded` event (unless it already
     * ended or doesn't exist) and inserts [newGame] with its opening [newEvents] (e.g. the first-player
     * pick) and its round timer: [timer], or a copy of [copyTimerFrom]'s, or none. A crash can't leave both
     * games open, or the new one without its timer. As in [append], [endTimestamp] becomes the old game's
     * `ended_at` and `updated_at`.
     */
    @Transaction
    public open suspend fun restart(
        oldGameId: String,
        endEventId: String,
        endTimestamp: Long,
        endPayload: String,
        newGame: GameEntity,
        newEvents: List<GameEventEntity> = emptyList(),
        timer: GameTimerEntity? = null,
        copyTimerFrom: String? = null,
    ) {
        require(timer == null || copyTimerFrom == null) { "A new game gets one timer: fresh or copied" }
        if (countActive(oldGameId) > 0) {
            append(oldGameId, endEventId, endTimestamp, endPayload, endsGame = true)
        }
        insertGame(newGame)
        insertAll(newEvents)
        timer?.let { upsertTimer(it) }
        copyTimerFrom?.let { copyTimer(fromGameId = it, toGameId = newGame.id) }
    }
}
