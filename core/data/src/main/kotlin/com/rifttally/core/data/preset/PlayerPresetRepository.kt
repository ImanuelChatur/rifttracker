package com.rifttally.core.data.preset

import com.rifttally.core.model.PlayerPreset
import kotlinx.coroutines.flow.Flow

/** Saved players, sorted by name. */
public interface PlayerPresetRepository {
    public val presets: Flow<List<PlayerPreset>>

    /** Saves a new preset. A blank [name] is ignored. */
    public suspend fun save(name: String, colorId: String, legendId: String?)

    /**
     * Replaces the name, color and Legend of the saved preset with [preset]'s id (the id stays, so the
     * player's stats stay linked). A blank name or an unknown id is ignored; the name is trimmed.
     */
    public suspend fun update(preset: PlayerPreset)

    public suspend fun delete(id: String)
}
