package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface GameDao {
    @Insert
    public suspend fun insert(game: GameEntity)

    @Query("SELECT * FROM game WHERE id = :id")
    public fun observeGame(id: String): Flow<GameEntity?>

    /** The most recently updated game that hasn't ended, if any. */
    @Query("SELECT * FROM game WHERE ended_at IS NULL ORDER BY updated_at DESC LIMIT 1")
    public fun observeActiveGame(): Flow<GameEntity?>

    /** Ended games, most recently ended first. */
    @Query("SELECT * FROM game WHERE ended_at IS NOT NULL ORDER BY ended_at DESC LIMIT :limit")
    public fun observeEndedGames(limit: Int): Flow<List<GameEntity>>

    /** Every ended game, most recently ended first (match history). */
    @Query("SELECT * FROM game WHERE ended_at IS NOT NULL ORDER BY ended_at DESC")
    public fun observeAllEndedGames(): Flow<List<GameEntity>>

    /** The event logs of every ended game in one query, ordered by game and then by `seq`. */
    @Query(
        """
        SELECT * FROM game_event
        WHERE game_id IN (SELECT id FROM game WHERE ended_at IS NOT NULL)
        ORDER BY game_id ASC, seq ASC
        """,
    )
    public fun observeEndedGameEvents(): Flow<List<GameEventEntity>>

    @Query("UPDATE game SET updated_at = :updatedAt, ended_at = :endedAt WHERE id = :id")
    public suspend fun updateTimestamps(id: String, updatedAt: Long, endedAt: Long?)

    @Query("DELETE FROM game_event WHERE game_id = :gameId")
    public suspend fun deleteEvents(gameId: String)

    @Query("DELETE FROM game WHERE id = :id")
    public suspend fun deleteGameRow(id: String)

    @Insert
    public suspend fun insertEvents(events: List<GameEventEntity>)

    /** Deletes a game and its event log, atomically. Deleting an unknown id does nothing. */
    @Transaction
    public suspend fun delete(id: String) {
        deleteEvents(id)
        deleteGameRow(id)
    }

    /** Re-inserts a game together with its event log, atomically (undo of [delete]). */
    @Transaction
    public suspend fun insertWithEvents(game: GameEntity, events: List<GameEventEntity>) {
        insert(game)
        insertEvents(events)
    }
}
