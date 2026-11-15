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
        var writes = 0

        override fun observeAll(): Flow<List<PlayerPresetEntity>> = rows.map { all -> all.sortedBy { it.name } }

        override suspend fun upsert(preset: PlayerPresetEntity) {
            writes++
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
    fun save_returnsId_orNullWhenBlank() = runTest {
        assertEquals("id1", repository.save("Ana", "orange", "jinx"))
        assertEquals(null, repository.save("  ", "sky", null))
    }

    @Test
    fun save_sameNameAgain_updatesColorAndLegend_andKeepsTheId() = runTest {
        repository.save("Ana", "orange", "jinx")

        val id = repository.save(" ana ", "sky", "viktor")

        assertEquals("id1", id)
        assertEquals(listOf(PlayerPreset("id1", "Ana", "sky", "viktor")), repository.presets.first())
    }

    @Test
    fun save_unchanged_writesNothing() = runTest {
        repository.save("Ana", "orange", "jinx")
        val before = dao.writes

        repository.save("Ana", "orange", "jinx")

        assertEquals(before, dao.writes)
        assertEquals(1, repository.presets.first().size)
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

    @Test
    fun update_toAnotherPresetsName_isIgnored() = runTest {
        repository.save("Ana", "orange", null)
        repository.save("Ben", "sky", null)

        repository.update(PlayerPreset("id2", " ANA ", "sky", null))

        assertEquals(listOf("Ana", "Ben"), repository.presets.first().map { it.name })
    }
}
