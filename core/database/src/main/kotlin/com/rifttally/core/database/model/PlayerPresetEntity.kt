package com.rifttally.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A saved player. [colorId] is a stable player-palette id (e.g. "orange"), not an ARGB value. */
@Entity(tableName = "player_preset")
public data class PlayerPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "color_id") val colorId: String,
    @ColumnInfo(name = "legend_id") val legendId: String? = null,
)
