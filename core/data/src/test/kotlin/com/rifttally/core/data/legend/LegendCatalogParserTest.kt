package com.rifttally.core.data.legend

import com.rifttally.core.model.Legend
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class LegendCatalogParserTest {

    @Test
    fun parsesEntriesSortedByName() {
        val json = """[{ "id": "viktor", "name": "Viktor" }, { "id": "ahri", "name": "Ahri" }]"""

        val parsed = parseLegendCatalog(json, emptySet())

        assertEquals(
            listOf(Legend("ahri", "Ahri", imageUri = null), Legend("viktor", "Viktor", imageUri = null)),
            parsed.legends,
        )
        assertEquals(0, parsed.skippedEntries)
    }

    @Test
    fun sortIgnoresCase() {
        val json = """[{ "id": "b", "name": "beta" }, { "id": "a", "name": "Alpha" }, { "id": "c", "name": "C" }]"""

        assertEquals(listOf("a", "b", "c"), parseLegendCatalog(json, emptySet()).legends.map { it.id })
    }

    @Test
    fun emptyArray_isEmptyCatalog() {
        assertEquals(ParsedLegendCatalog(emptyList(), 0), parseLegendCatalog("[]", emptySet()))
    }

    @Test
    fun ignoresUnknownKeys_andTrimsValues() {
        val json = """[{ "id": " jinx ", "name": "  Jinx ", "region": "Zaun" }]"""

        assertEquals(listOf(Legend("jinx", "Jinx", null)), parseLegendCatalog(json, emptySet()).legends)
    }

    @Test
    fun skipsInvalidEntries_andKeepsValidOnes() {
        val json = """
            [
              { "id": "jinx", "name": "Jinx" },
              { "id": "no_name" },
              { "name": "No id" },
              { "id": "", "name": "Blank id" },
              { "id": "blank_name", "name": "   " },
              { "id": "Lee Sin", "name": "Bad id" },
              { "id": "jinx", "name": "Duplicate" },
              "not an object",
              42,
              { "id": 7, "name": "Numeric id" },
              { "id": "teemo", "name": "Teemo" }
            ]
        """.trimIndent()

        val parsed = parseLegendCatalog(json, emptySet())

        assertEquals(listOf("jinx", "teemo"), parsed.legends.map { it.id })
        assertEquals("Jinx", parsed.legends.first().name)
        assertEquals(9, parsed.skippedEntries)
    }

    @Test
    fun malformedJson_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> { parseLegendCatalog("""[{ "id": "jinx", """, emptySet()) }
        assertFailsWith<IllegalArgumentException> { parseLegendCatalog("", emptySet()) }
    }

    @Test
    fun nonArrayRoot_throwsIllegalArgument() {
        assertFailsWith<IllegalArgumentException> {
            parseLegendCatalog("""{ "id": "jinx", "name": "Jinx" }""", emptySet())
        }
    }

    @Test
    fun resolvesWebpImage() {
        assertEquals(
            "file:///android_asset/legends/jinx.webp",
            resolveLegendImageUri("jinx", setOf("legends.json", "jinx.webp")),
        )
    }

    @Test
    fun resolvesPngImage() {
        assertEquals("file:///android_asset/legends/jinx.png", resolveLegendImageUri("jinx", setOf("jinx.png")))
    }

    @Test
    fun prefersWebpOverPng() {
        assertEquals(
            "file:///android_asset/legends/jinx.webp",
            resolveLegendImageUri("jinx", setOf("jinx.png", "jinx.webp")),
        )
    }

    @Test
    fun missingImage_isNull() {
        assertNull(resolveLegendImageUri("jinx", emptySet()))
        assertNull(resolveLegendImageUri("jinx", setOf("jinx.jpg", "jinx_old.webp", "JINX.webp", "viktor.webp")))
    }

    @Test
    fun parse_attachesImageUrisOnlyWhereFilesExist() {
        val json = """[{ "id": "jinx", "name": "Jinx" }, { "id": "viktor", "name": "Viktor" }]"""

        val legends = parseLegendCatalog(json, setOf("legends.json", "viktor.png")).legends

        assertEquals(
            listOf(
                Legend("jinx", "Jinx", imageUri = null),
                Legend("viktor", "Viktor", imageUri = "file:///android_asset/legends/viktor.png"),
            ),
            legends,
        )
    }

    @Test
    fun resolvesWideImage_webpPreferred() {
        assertEquals(
            "file:///android_asset/legends/jinx_wide.webp",
            resolveLegendWideImageUri("jinx", setOf("jinx.webp", "jinx_wide.png", "jinx_wide.webp")),
        )
        assertEquals(
            "file:///android_asset/legends/jinx_wide.png",
            resolveLegendWideImageUri("jinx", setOf("jinx_wide.png")),
        )
    }

    @Test
    fun missingWideImage_isNull() {
        assertNull(resolveLegendWideImageUri("jinx", setOf("jinx.webp", "jinx_wide.jpg", "viktor_wide.webp")))
    }

    @Test
    fun parse_attachesWideUrisIndependentlyOfPortraits() {
        val json = """[{ "id": "jinx", "name": "Jinx" }, { "id": "viktor", "name": "Viktor" }]"""

        val legends = parseLegendCatalog(json, setOf("jinx_wide.webp", "viktor.webp")).legends

        assertEquals(
            listOf(
                Legend("jinx", "Jinx", imageUri = null, wideImageUri = "file:///android_asset/legends/jinx_wide.webp"),
                Legend("viktor", "Viktor", imageUri = "file:///android_asset/legends/viktor.webp", wideImageUri = null),
            ),
            legends,
        )
    }

    @Test
    fun shippedArt_resolvesForEveryLegend() {
        val dir = File("src/main/assets/legends")
        val fileNames = dir.list().orEmpty().toSet()

        val legends = parseLegendCatalog(File(dir, "legends.json").readText(), fileNames).legends

        val missing = legends.filter { it.imageUri == null || it.wideImageUri == null }.map { it.id }
        assertTrue(missing.isEmpty(), "Legends without bundled art: $missing")
    }

    @Test
    fun shippedSeedCatalog_isValid() {
        // Unit tests run with the module directory as the working directory.
        val seed = File("src/main/assets/legends/legends.json")
        assertTrue(seed.exists(), "seed catalog not found at ${seed.absolutePath}")

        val parsed = parseLegendCatalog(seed.readText(), emptySet())

        assertEquals(0, parsed.skippedEntries)
        assertTrue(parsed.legends.size >= 57, "catalog should list every Legend through Radiance")
        assertTrue(parsed.legends.all { it.imageUri == null })
    }
}
