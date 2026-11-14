package com.rifttally.feature.tracker

import com.rifttally.core.model.SideId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The live table screen. */
internal sealed interface TrackerUiState {
    data object Loading : TrackerUiState

    /** The game doesn't exist (or can't be read). */
    data object NotFound : TrackerUiState

    data class Success(
        /** One panel per side, in seat order. For 2 players the first panel faces the far side (rotated). */
        val panels: ImmutableList<SidePanelUiState>,
        val targetScore: Int,
        val canUndo: Boolean,
        /** Sides at or above the target, for the "Winner?" banner. Empty hides the banner. */
        val winnerBanner: WinnerBannerUiState?,
        /** History log, newest first. */
        val history: ImmutableList<HistoryItemUiState>,
        val hapticsEnabled: Boolean,
        val keepScreenOn: Boolean,
        val immersiveMode: Boolean,
    ) : TrackerUiState
}

/** A point owner's panel: a player (or, from Phase 4, a team). */
internal data class SidePanelUiState(
    val sideId: SideId,
    val points: Int,
    val isAtTarget: Boolean,
    val players: ImmutableList<PanelPlayerUiState>,
) {
    /** "Ana", or "Ana & Ben" for a team. */
    val displayName: String get() = players.joinToString(separator = " & ") { it.name }
}

internal data class PanelPlayerUiState(
    val name: String,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String?,
)

/** Shown while at least one side is at the target and the banner hasn't been dismissed. */
internal data class WinnerBannerUiState(val names: ImmutableList<String> = persistentListOf())

/** One history line. Formatted by the screen ("Ana +1 · 6"). */
internal sealed interface HistoryItemUiState {
    val id: String

    data class Points(override val id: String, val name: String, val delta: Int, val total: Int) : HistoryItemUiState

    data class CounterChanged(
        override val id: String,
        val name: String,
        val label: String,
        val delta: Int,
        val total: Int,
    ) : HistoryItemUiState

    data class CounterAdded(override val id: String, val name: String, val label: String) : HistoryItemUiState

    data class CounterRemoved(override val id: String, val name: String, val label: String) : HistoryItemUiState

    data class TargetChanged(override val id: String, val targetScore: Int) : HistoryItemUiState

    /** [winnerName] is `null` when the game ended without a winner. */
    data class GameEnded(override val id: String, val winnerName: String?) : HistoryItemUiState
}
