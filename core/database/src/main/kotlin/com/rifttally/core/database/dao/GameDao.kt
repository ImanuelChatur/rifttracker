package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rifttally.core.database.model.GameEntity
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

    @Query("UPDATE game SET updated_at = :updatedAt, ended_at = :endedAt WHERE id = :id")
    public suspend fun updateTimestamps(id: String, updatedAt: Long, endedAt: Long?)
}
