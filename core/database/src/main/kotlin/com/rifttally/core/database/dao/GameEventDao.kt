package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rifttally.core.database.model.GameEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface GameEventDao {
    @Insert
    public suspend fun insert(event: GameEventEntity)

    @Query("SELECT * FROM game_event WHERE game_id = :gameId ORDER BY seq ASC")
    public fun observeEvents(gameId: String): Flow<List<GameEventEntity>>

    @Query("SELECT MAX(seq) FROM game_event WHERE game_id = :gameId")
    public suspend fun lastSeq(gameId: String): Int?

    /** Undo: removes the newest event of a game. */
    @Query(
        """
        DELETE FROM game_event
        WHERE game_id = :gameId AND seq = (SELECT MAX(seq) FROM game_event WHERE game_id = :gameId)
        """,
    )
    public suspend fun deleteLast(gameId: String)
}
