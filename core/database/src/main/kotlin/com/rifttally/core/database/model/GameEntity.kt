package com.rifttally.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One game. [configJson] is the serialized game config; [endedAt] is set once a `GameEnded` event is stored. */
@Entity(tableName = "game")
public data class GameEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "config_json") val configJson: String,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
)
