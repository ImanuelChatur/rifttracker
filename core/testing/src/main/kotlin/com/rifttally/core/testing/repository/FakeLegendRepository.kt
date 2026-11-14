package com.rifttally.core.testing.repository

import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.model.Legend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** An in-memory [LegendRepository] for ViewModel tests. Set the catalog with [setLegends]. */
public class FakeLegendRepository(initial: List<Legend> = emptyList()) : LegendRepository {
    private val state = MutableStateFlow(initial)

    override val legends: Flow<List<Legend>> = state

    override suspend fun getLegend(id: String): Legend? = state.value.firstOrNull { it.id == id }

    public fun setLegends(legends: List<Legend>) {
        state.value = legends
    }
}
