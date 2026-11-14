package com.rifttally.feature.setup

import com.rifttally.core.model.Legend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Everything the setup screen shows. */
internal data class SetupUiState(
    val players: ImmutableList<SetupPlayerUiState> = persistentListOf(),
    val targetScore: Int = 0,
    val canDecreaseTarget: Boolean = false,
    /** Open Legend picker, or `null` when closed. */
    val legendPicker: LegendPickerUiState? = null,
    val isStarting: Boolean = false,
)

/** One player row. [name] may be blank: the screen shows "Player N" and uses it on start. */
internal data class SetupPlayerUiState(val index: Int, val name: String, val colorId: String, val legend: Legend?)

/** The searchable Legend grid for [playerIndex]. [legends] is already filtered by [query]. */
internal data class LegendPickerUiState(
    val playerIndex: Int,
    val query: String,
    val legends: ImmutableList<Legend>,
    val selectedLegendId: String?,
)
