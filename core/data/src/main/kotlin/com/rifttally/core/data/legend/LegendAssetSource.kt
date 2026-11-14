package com.rifttally.core.data.legend

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject

/** Raw access to the legends asset folder. Blocking: call it on the IO dispatcher. */
internal interface LegendAssetSource {
    /** Contents of `legends.json`, or `null` if the file doesn't exist. */
    fun readCatalog(): String?

    /** Names of the files in the legends asset folder (e.g. `jinx.webp`). */
    fun listFileNames(): Set<String>
}

internal class AndroidLegendAssetSource @Inject constructor(@ApplicationContext private val context: Context) :
    LegendAssetSource {
    override fun readCatalog(): String? = try {
        context.assets.open("$LEGENDS_ASSET_DIR/$LEGENDS_CATALOG_FILE").bufferedReader().use { it.readText() }
    } catch (e: FileNotFoundException) {
        null
    }

    override fun listFileNames(): Set<String> = context.assets.list(LEGENDS_ASSET_DIR)?.toSet().orEmpty()
}
