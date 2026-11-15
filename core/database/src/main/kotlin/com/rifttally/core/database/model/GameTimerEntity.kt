package com.rifttally.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * A game's optional round timer. Not part of the event log (Undo never touches it). Elapsed time is
 * [elapsedBeforeMs] plus, while running, `now - runningSince` (epoch millis). Deleted with its game.
 */
@Entity(
    tableName = "game_timer",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
public data class GameTimerEntity(
    @PrimaryKey @ColumnInfo(name = "game_id") val gameId: String,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "elapsed_before_ms") val elapsedBeforeMs: Long = 0,
    /** When the timer was last started, or `null` while paused. */
    @ColumnInfo(name = "running_since") val runningSince: Long? = null,
)
