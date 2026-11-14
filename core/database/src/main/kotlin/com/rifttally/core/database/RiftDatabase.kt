package com.rifttally.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.dao.PlayerPresetDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.database.model.PlayerPresetEntity

/**
 * Every schema change bumps [version], adds a migration and a migration test.
 * Exception: until the first public release, v1 may still change in place (re-export 1.json, no migration).
 * Exported schemas live in `core/database/schemas` and are committed.
 */
@Database(
    entities = [GameEntity::class, GameEventEntity::class, PlayerPresetEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class RiftDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun gameEventDao(): GameEventDao

    abstract fun playerPresetDao(): PlayerPresetDao

    companion object {
        const val NAME: String = "rift-tally.db"
    }
}
