package com.rifttally.core.data.legend

import com.rifttally.core.model.Legend
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/** Folder inside `assets/` that holds the catalog and the optional images. */
internal const val LEGENDS_ASSET_DIR: String = "legends"

/** Catalog file name inside [LEGENDS_ASSET_DIR]. */
internal const val LEGENDS_CATALOG_FILE: String = "legends.json"

/** Image extensions checked for `<id>.<ext>`, in order of preference. */
internal val LEGEND_IMAGE_EXTENSIONS: List<String> = listOf("webp", "png")

private const val ANDROID_ASSET_URI_PREFIX = "file:///android_asset/"

private val LEGEND_ID_PATTERN = Regex("[a-z0-9_]+")

/** Result of parsing the catalog: the valid Legends plus how many entries were skipped. */
internal data class ParsedLegendCatalog(val legends: List<Legend>, val skippedEntries: Int)

@Serializable
private data class LegendEntry(val id: String, val name: String)

private val catalogJson = Json { ignoreUnknownKeys = true }

/**
 * Parses `legends.json` (a JSON array of `{ "id", "name" }` objects) and resolves each image against
 * [assetFileNames] (the file names in the legends asset folder).
 *
 * Invalid entries (missing/blank fields, an id that isn't lowercase snake_case, or a duplicate id) are
 * skipped and counted, so one bad line doesn't hide the whole catalog. Legends are sorted by name.
 *
 * @throws IllegalArgumentException if [json] isn't valid JSON or isn't a top-level array.
 */
internal fun parseLegendCatalog(json: String, assetFileNames: Set<String>): ParsedLegendCatalog {
    val root = catalogJson.parseToJsonElement(json)
    require(root is JsonArray) { "$LEGENDS_CATALOG_FILE must be a JSON array" }

    val seenIds = HashSet<String>()
    var skipped = 0
    val legends = root.mapNotNull { element ->
        val entry = try {
            catalogJson.decodeFromJsonElement(LegendEntry.serializer(), element)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
        val id = entry?.id?.trim().orEmpty()
        val name = entry?.name?.trim().orEmpty()
        if (!LEGEND_ID_PATTERN.matches(id) || name.isEmpty() || !seenIds.add(id)) {
            skipped++
            null
        } else {
            Legend(id = id, name = name, imageUri = resolveLegendImageUri(id, assetFileNames))
        }
    }
    return ParsedLegendCatalog(
        legends = legends.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
        skippedEntries = skipped,
    )
}

/** The asset URI of `<id>.webp` (preferred) or `<id>.png` if present in [assetFileNames], else `null`. */
internal fun resolveLegendImageUri(id: String, assetFileNames: Set<String>): String? = LEGEND_IMAGE_EXTENSIONS
    .map { ext -> "$id.$ext" }
    .firstOrNull { it in assetFileNames }
    ?.let { fileName -> "$ANDROID_ASSET_URI_PREFIX$LEGENDS_ASSET_DIR/$fileName" }
