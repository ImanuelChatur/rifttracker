package com.rifttally.core.stats

import com.rifttally.core.model.Player

/**
 * Identifies the same person across games: their saved player ([Player.presetId]) when they were
 * loaded from one, otherwise their trimmed, case-insensitive name. [value] is stable and can be put
 * in a navigation route.
 */
@JvmInline
public value class PlayerKey(public val value: String) {
    public companion object {
        private const val PRESET_PREFIX = "preset:"
        private const val NAME_PREFIX = "name:"

        /** The key of [player]. */
        public fun of(player: Player): PlayerKey = player.presetId?.let { PlayerKey(PRESET_PREFIX + it) }
            ?: PlayerKey(NAME_PREFIX + player.name.trim().lowercase())
    }
}
