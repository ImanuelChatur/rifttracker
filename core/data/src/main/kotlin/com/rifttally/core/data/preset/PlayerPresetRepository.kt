package com.rifttally.core.data.preset

import com.rifttally.core.model.PlayerPreset
import kotlinx.coroutines.flow.Flow

/** Saved players, sorted by name. */
public interface PlayerPresetRepository {
    public val presets: Flow<List<PlayerPreset>>

    /** Saves a new preset. A blank [name] is ignored. */
    public suspend fun save(name: String, colorId: String, legendId: String?)

    public suspend fun delete(id: String)
}
