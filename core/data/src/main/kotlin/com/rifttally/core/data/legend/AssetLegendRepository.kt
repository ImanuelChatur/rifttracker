package com.rifttally.core.data.legend

import com.rifttally.core.common.dispatchers.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads the Legend catalog from assets once and caches it. A missing or malformed catalog is logged
 * and treated as empty; it never crashes the app.
 */
@Singleton
internal class AssetLegendRepository @Inject constructor(
    private val assetSource: LegendAssetSource,
    private val logger: LegendLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LegendRepository {

    private val mutex = Mutex()
    private var cache: List<Legend>? = null

    override val legends: Flow<List<Legend>> = flow { emit(loadCatalog()) }

    override suspend fun getLegend(id: String): Legend? = loadCatalog().firstOrNull { it.id == id }

    private suspend fun loadCatalog(): List<Legend> = mutex.withLock {
        cache ?: withContext(ioDispatcher) { readCatalog() }.also { cache = it }
    }

    private fun readCatalog(): List<Legend> = try {
        val json = assetSource.readCatalog()
        if (json == null) {
            logger.warn("$LEGENDS_ASSET_DIR/$LEGENDS_CATALOG_FILE not found; no Legends available", null)
            emptyList()
        } else {
            val parsed = parseLegendCatalog(json, assetSource.listFileNames())
            if (parsed.skippedEntries > 0) {
                logger.warn("Skipped ${parsed.skippedEntries} invalid entries in $LEGENDS_CATALOG_FILE", null)
            }
            parsed.legends
        }
    } catch (e: IOException) {
        logger.warn("Couldn't read $LEGENDS_CATALOG_FILE", e)
        emptyList()
    } catch (e: IllegalArgumentException) {
        // Also covers kotlinx.serialization's SerializationException.
        logger.warn("Malformed $LEGENDS_CATALOG_FILE", e)
        emptyList()
    }
}
