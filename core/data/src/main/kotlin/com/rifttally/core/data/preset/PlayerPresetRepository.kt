package com.rifttally.core.data.preset

import com.rifttally.core.model.PlayerPreset
import kotlinx.coroutines.flow.Flow

/** Saved players, sorted by name. Names are unique (trimmed, ignoring case). */
public interface PlayerPresetRepository {
    public val presets: Flow<List<PlayerPreset>>

    /**
     * Saves a player and returns its preset id, or `null` for a blank [name] (nothing saved). If a preset
     * with the same name (trimmed, ignoring case) exists, it's updated with this color and Legend and
     * keeps its id and its stored name, so the person's stats stay linked; nothing is written when
     * nothing changed.
     */
    public suspend fun save(name: String, colorId: String, legendId: String?): String?

    /**
     * Replaces the name, color and Legend of the saved preset with [preset]'s id (the id stays, so the
     * player's stats stay linked). A blank name, an unknown id, or a name another preset already has
     * (trimmed, ignoring case) is ignored; the name is trimmed.
     */
    public suspend fun update(preset: PlayerPreset)

    public suspend fun delete(id: String)
}
