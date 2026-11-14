package com.rifttally.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rifttally.core.database.model.PlayerPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
public interface PlayerPresetDao {
    @Query("SELECT * FROM player_preset ORDER BY name COLLATE NOCASE ASC")
    public fun observeAll(): Flow<List<PlayerPresetEntity>>

    @Upsert
    public suspend fun upsert(preset: PlayerPresetEntity)

    @Query("DELETE FROM player_preset WHERE id = :id")
    public suspend fun delete(id: String)
}
