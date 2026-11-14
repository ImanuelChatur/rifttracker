package com.rifttally.core.model

import kotlinx.serialization.Serializable

/**
 * A player at the table.
 *
 * @property colorId stable key of the player's color (matches `PlayerColor.id` in `:core:designsystem`,
 *   e.g. `"orange"`). Kept as a string so this module stays pure Kotlin.
 * @property legendId the chosen [Legend.id], or `null` for none. This is the only link to Legend data.
 */
@Serializable
public data class Player(val id: PlayerId, val name: String, val colorId: String, val legendId: String? = null)
