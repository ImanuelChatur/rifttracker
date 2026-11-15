package com.rifttally.core.model

import kotlinx.serialization.Serializable

/**
 * A player at the table.
 *
 * @property colorId stable key of the player's color (matches `PlayerColor.id` in `:core:designsystem`,
 *   e.g. `"orange"`). Kept as a string so this module stays pure Kotlin.
 * @property legendId the chosen [Legend.id], or `null` for none. This is the only link to Legend data.
 * @property presetId the [PlayerPreset.id] this player was loaded from, or `null`. Links the same person
 *   across games for stats. Stored inside the game's config JSON; older games simply don't have it.
 * @property hasDefaultName `true` when the name was left blank and [name] is a placeholder ("Player 1").
 *   Such players aren't people for stats (they'd all merge into one "Player 1"); their Legend still
 *   counts. Older games decode as `false`.
 */
@Serializable
public data class Player(
    val id: PlayerId,
    val name: String,
    val colorId: String,
    val legendId: String? = null,
    val presetId: String? = null,
    val hasDefaultName: Boolean = false,
)
