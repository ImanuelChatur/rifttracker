package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rifttally.core.database.model.GameTimerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Round timers. Each change is a single statement (or one transaction), so concurrent taps can't lose
 * elapsed time. `now` is passed in (from the injected clock), never read here.
 */
@Dao
public interface GameTimerDao {
    @Query("SELECT * FROM game_timer WHERE game_id = :gameId")
    public fun observe(gameId: String): Flow<GameTimerEntity?>

    @Query("SELECT * FROM game_timer WHERE game_id = :gameId")
    public suspend fun get(gameId: String): GameTimerEntity?

    @Upsert
    public suspend fun upsert(timer: GameTimerEntity)

    /** Starts a paused timer at [now]. Does nothing if it's running or doesn't exist. */
    @Query("UPDATE game_timer SET running_since = :now WHERE game_id = :gameId AND running_since IS NULL")
    public suspend fun start(gameId: String, now: Long)

    /**
     * Pauses a running timer at [now], banking the time since it started. A clock that moved backwards
     * banks nothing rather than a negative amount.
     */
    @Query(
        """
        UPDATE game_timer
        SET elapsed_before_ms = elapsed_before_ms + MAX(0, :now - running_since), running_since = NULL
        WHERE game_id = :gameId AND running_since IS NOT NULL
        """,
    )
    public suspend fun pause(gameId: String, now: Long)

    /** Start if paused, pause if running, in one transaction. */
    @Transaction
    public suspend fun toggle(gameId: String, now: Long) {
        val timer = get(gameId) ?: return
        if (timer.runningSince == null) start(gameId, now) else pause(gameId, now)
    }

    /** Back to the full duration, paused. */
    @Query("UPDATE game_timer SET elapsed_before_ms = 0, running_since = NULL WHERE game_id = :gameId")
    public suspend fun reset(gameId: String)

    /**
     * Copies [fromGameId]'s timer, exactly as it is (still running if it was), to [toGameId], replacing
     * any timer that game has. Does nothing if [fromGameId] has no timer.
     */
    @Query(
        """
        INSERT OR REPLACE INTO game_timer (game_id, duration_ms, elapsed_before_ms, running_since)
        SELECT :toGameId, duration_ms, elapsed_before_ms, running_since FROM game_timer WHERE game_id = :fromGameId
        """,
    )
    public suspend fun copy(fromGameId: String, toGameId: String)
}
