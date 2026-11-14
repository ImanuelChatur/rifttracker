package com.rifttally.core.testing.repository

import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.model.PlayerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [PlayerPresetRepository] for ViewModel tests. Ids are `preset1`, `preset2`, … */
public class FakePlayerPresetRepository(initial: List<PlayerPreset> = emptyList()) : PlayerPresetRepository {
    private val state = MutableStateFlow(initial)
    private var created = 0

    override val presets: Flow<List<PlayerPreset>> = state.map { all -> all.sortedBy { it.name.lowercase() } }

    public val current: List<PlayerPreset> get() = state.value

    override suspend fun save(name: String, colorId: String, legendId: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (state.value.any { it.name == trimmed && it.colorId == colorId && it.legendId == legendId }) return
        state.update { it + PlayerPreset("preset${++created}", trimmed, colorId, legendId) }
    }

    override suspend fun delete(id: String) {
        state.update { all -> all.filterNot { it.id == id } }
    }
}
