package com.rifttally.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One event in a game's log. [seq] orders events within a game; [payload] is the event as JSON. */
@Entity(
    tableName = "game_event",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["game_id", "seq"], unique = true)],
)
public data class GameEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "game_id") val gameId: String,
    val seq: Int,
    val timestamp: Long,
    val payload: String,
)
