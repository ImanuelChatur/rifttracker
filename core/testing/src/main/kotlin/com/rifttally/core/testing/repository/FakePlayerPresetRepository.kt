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

    override suspend fun save(name: String, colorId: String, legendId: String?): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = state.value.firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }
        val saved = PlayerPreset(existing?.id ?: "preset${++created}", existing?.name ?: trimmed, colorId, legendId)
        if (saved == existing) return saved.id
        state.update { all -> if (existing == null) all + saved else all.map { if (it.id == saved.id) saved else it } }
        return saved.id
    }

    override suspend fun update(preset: PlayerPreset) {
        val trimmed = preset.name.trim()
        if (trimmed.isEmpty()) return
        val all = state.value
        if (all.none { it.id == preset.id }) return
        if (all.any { it.id != preset.id && it.name.trim().equals(trimmed, ignoreCase = true) }) return
        state.update { list -> list.map { if (it.id == preset.id) preset.copy(name = trimmed) else it } }
    }

    override suspend fun delete(id: String) {
        state.update { all -> all.filterNot { it.id == id } }
    }
}
