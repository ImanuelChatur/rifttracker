package com.rifttally.core.data.legend

import app.cash.turbine.test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssetLegendRepositoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val warnings = mutableListOf<Pair<String, Throwable?>>()
    private val logger = LegendLogger { message, throwable -> warnings += message to throwable }

    private fun repository(source: LegendAssetSource) = AssetLegendRepository(source, logger, dispatcher)

    private fun test(block: suspend TestScope.() -> Unit) = runTest(dispatcher) { block() }

    @Test
    fun emitsParsedLegendsWithImages() = test {
        val source = FakeLegendAssetSource(
            catalog = """[{ "id": "viktor", "name": "Viktor" }, { "id": "jinx", "name": "Jinx" }]""",
            files = setOf("legends.json", "jinx.webp"),
        )

        repository(source).legends.test {
            assertEquals(
                listOf(
                    Legend("jinx", "Jinx", "file:///android_asset/legends/jinx.webp"),
                    Legend("viktor", "Viktor", null),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun missingCatalog_emitsEmptyListAndLogs() = test {
        repository(FakeLegendAssetSource(catalog = null)).legends.test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
        assertEquals(1, warnings.size)
    }

    @Test
    fun malformedCatalog_emitsEmptyListAndLogs() = test {
        repository(FakeLegendAssetSource(catalog = "{ not json")).legends.test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().second is IllegalArgumentException)
    }

    @Test
    fun readFailure_emitsEmptyListAndLogs() = test {
        val source = FakeLegendAssetSource(catalog = "[]", readError = IOException("disk"))

        repository(source).legends.test {
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
        assertTrue(warnings.single().second is IOException)
    }

    @Test
    fun invalidEntries_areSkippedAndLogged() = test {
        val source = FakeLegendAssetSource(catalog = """[{ "id": "jinx", "name": "Jinx" }, { "id": "bad" }]""")

        repository(source).legends.test {
            assertEquals(listOf("jinx"), awaitItem().map { it.id })
            awaitComplete()
        }
        assertEquals(1, warnings.size)
    }

    @Test
    fun catalogIsReadOnce() = test {
        val source = FakeLegendAssetSource(catalog = """[{ "id": "jinx", "name": "Jinx" }]""")
        val repository = repository(source)

        repository.legends.test {
            awaitItem()
            awaitComplete()
        }
        repository.legends.test {
            awaitItem()
            awaitComplete()
        }
        repository.getLegend("jinx")

        assertEquals(1, source.catalogReads)
        assertEquals(1, source.listCalls)
    }

    @Test
    fun getLegend_findsById() = test {
        val source = FakeLegendAssetSource(
            catalog = """[{ "id": "jinx", "name": "Jinx" }, { "id": "lee_sin", "name": "Lee Sin" }]""",
            files = setOf("lee_sin.png"),
        )
        val repository = repository(source)

        assertEquals(
            Legend("lee_sin", "Lee Sin", "file:///android_asset/legends/lee_sin.png"),
            repository.getLegend("lee_sin"),
        )
        assertNull(repository.getLegend("unknown"))
    }

    private class FakeLegendAssetSource(
        private val catalog: String?,
        private val files: Set<String> = setOf("legends.json"),
        private val readError: IOException? = null,
    ) : LegendAssetSource {
        var catalogReads = 0
            private set
        var listCalls = 0
            private set

        override fun readCatalog(): String? {
            catalogReads++
            readError?.let { throw it }
            return catalog
        }

        override fun listFileNames(): Set<String> {
            listCalls++
            return files
        }
    }
}
