package com.rifttally.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.dao.GameTimerDao
import com.rifttally.core.database.dao.PlayerPresetDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.database.model.GameTimerEntity
import com.rifttally.core.database.model.PlayerPresetEntity

/**
 * Every schema change bumps [version], adds a migration and a migration test (`MigrationTest`).
 * Exported schemas live in `core/database/schemas` and are committed; never change an exported one.
 *
 * Versions:
 * - 1: `game`, `game_event`, `player_preset`.
 * - 2: adds `game_timer` (the optional round timer of a game); auto-migrated from 1.
 */
@Database(
    entities = [GameEntity::class, GameEventEntity::class, PlayerPresetEntity::class, GameTimerEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
internal abstract class RiftDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun gameEventDao(): GameEventDao

    abstract fun playerPresetDao(): PlayerPresetDao

    abstract fun gameTimerDao(): GameTimerDao

    companion object {
        const val NAME: String = "rift-tally.db"
    }
}
