package com.rifttally.feature.history

/**
 * One line of a finished game's event log. A minimal copy of the tracker's `HistoryItemUiState`
 * (the tracker's formatting belongs to another module); dedupe into `:core:ui` at the merge.
 */
internal sealed interface GameLogItem {
    val id: String

    data class Points(override val id: String, val name: String, val delta: Int, val total: Int) : GameLogItem

    data class CounterChanged(
        override val id: String,
        val name: String,
        val label: String,
        val delta: Int,
        val total: Int,
    ) : GameLogItem

    data class CounterAdded(override val id: String, val name: String, val label: String) : GameLogItem

    data class CounterRemoved(override val id: String, val name: String, val label: String) : GameLogItem

    data class TargetChanged(override val id: String, val targetScore: Int) : GameLogItem

    data class GameEnded(override val id: String, val winnerName: String?) : GameLogItem
}
