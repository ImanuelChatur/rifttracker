package com.rifttally.core.data.preset

import com.rifttally.core.database.dao.PlayerPresetDao
import com.rifttally.core.database.model.PlayerPresetEntity
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.PlayerPreset
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflinePlayerPresetRepositoryTest {
    private val dao = object : PlayerPresetDao {
        val rows = MutableStateFlow<List<PlayerPresetEntity>>(emptyList())

        override fun observeAll(): Flow<List<PlayerPresetEntity>> = rows.map { all -> all.sortedBy { it.name } }

        override suspend fun upsert(preset: PlayerPresetEntity) {
            rows.update { all -> all.filterNot { it.id == preset.id } + preset }
        }

        override suspend fun delete(id: String) {
            rows.update { all -> all.filterNot { it.id == id } }
        }
    }
    private var ids = 0
    private val repository = OfflinePlayerPresetRepository(dao, IdGenerator { "id${++ids}" })

    @Test
    fun save_trimsName_andIgnoresBlank() = runTest {
        repository.save("  Ana ", "orange", "jinx")
        repository.save("   ", "sky", null)

        assertEquals(listOf(PlayerPreset("id1", "Ana", "orange", "jinx")), repository.presets.first())
    }

    @Test
    fun delete_removesPreset() = runTest {
        repository.save("Ana", "orange", null)
        repository.save("Ben", "sky", null)

        repository.delete("id1")

        assertEquals(listOf("Ben"), repository.presets.first().map { it.name })
    }

    @Test
    fun save_samePlayerTwice_keepsOne() = runTest {
        repository.save("Ana", "orange", "jinx")
        repository.save("Ana", "orange", "jinx")
        repository.save("Ana", "sky", "jinx")

        assertEquals(2, repository.presets.first().size)
    }

    @Test
    fun update_keepsId_trimsName_andReplacesColorAndLegend() = runTest {
        repository.save("Ana", "orange", "jinx")

        repository.update(PlayerPreset("id1", "  Anna ", "sky", null))

        assertEquals(listOf(PlayerPreset("id1", "Anna", "sky", null)), repository.presets.first())
    }

    @Test
    fun update_blankName_orUnknownId_isIgnored() = runTest {
        repository.save("Ana", "orange", "jinx")

        repository.update(PlayerPreset("id1", "   ", "sky", null))
        repository.update(PlayerPreset("nope", "Ben", "sky", null))

        assertEquals(listOf(PlayerPreset("id1", "Ana", "orange", "jinx")), repository.presets.first())
    }
}
