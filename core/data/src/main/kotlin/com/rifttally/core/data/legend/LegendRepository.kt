package com.rifttally.core.data.legend

import com.rifttally.core.model.Legend
import kotlinx.coroutines.flow.Flow

/**
 * The Legend catalog. Today it's backed by `assets/legends/legends.json`; a future card-data source
 * can replace it without UI changes.
 */
public interface LegendRepository {
    /** All Legends, sorted by name. Emits an empty list if the catalog is missing or malformed. */
    public val legends: Flow<List<Legend>>

    /** The Legend with [id], or `null` if it isn't in the catalog. */
    public suspend fun getLegend(id: String): Legend?
}
