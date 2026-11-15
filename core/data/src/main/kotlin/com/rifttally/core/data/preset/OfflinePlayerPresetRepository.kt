package com.rifttally.core.data.preset

import com.rifttally.core.database.dao.PlayerPresetDao
import com.rifttally.core.database.model.PlayerPresetEntity
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.PlayerPreset
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Room-backed [PlayerPresetRepository]. */
internal class OfflinePlayerPresetRepository @Inject constructor(
    private val dao: PlayerPresetDao,
    private val idGenerator: IdGenerator,
) : PlayerPresetRepository {
    override val presets: Flow<List<PlayerPreset>> = dao.observeAll().map { entities ->
        entities.map { PlayerPreset(id = it.id, name = it.name, colorId = it.colorId, legendId = it.legendId) }
    }

    override suspend fun save(name: String, colorId: String, legendId: String?): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val existing = dao.observeAll().first().firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }
        val saved = PlayerPresetEntity(
            id = existing?.id ?: idGenerator.newId(),
            // Same person: keep how their name was saved ("Ana"), even if typed "ana" this time.
            name = existing?.name ?: trimmed,
            colorId = colorId,
            legendId = legendId,
        )
        // Saving the same player twice (e.g. a double tap) writes nothing.
        if (saved != existing) dao.upsert(saved)
        return saved.id
    }

    override suspend fun update(preset: PlayerPreset) {
        val trimmed = preset.name.trim()
        if (trimmed.isEmpty()) return
        val all = dao.observeAll().first()
        if (all.none { it.id == preset.id }) return
        if (all.any { it.id != preset.id && it.name.trim().equals(trimmed, ignoreCase = true) }) return
        dao.upsert(
            PlayerPresetEntity(id = preset.id, name = trimmed, colorId = preset.colorId, legendId = preset.legendId),
        )
    }

    override suspend fun delete(id: String) = dao.delete(id)
}
