package com.rifttally.feature.tracker

import com.rifttally.core.game.GameLogLine
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
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
        val history: ImmutableList<GameLogLine>,
        val hapticsEnabled: Boolean,
        val keepScreenOn: Boolean,
        val immersiveMode: Boolean,
        /** The "<Name> goes first" reveal to play, or `null`. */
        val firstPlayerReveal: FirstPlayerRevealUiState? = null,
    ) : TrackerUiState
}

/**
 * Reveals who goes first: a highlight cycles across the panels and settles on [panelIndex] (the side of
 * the picked player [name]), then "<Name> goes first" faces that seat. The result is decided before the
 * animation starts. [animate] is false once it has started (e.g. after rotation): show the result only.
 */
internal data class FirstPlayerRevealUiState(
    val pickId: String,
    val panelIndex: Int,
    val name: String,
    val animate: Boolean = true,
)

/** A point owner's panel: a player (or, from Phase 4, a team). */
internal data class SidePanelUiState(
    val sideId: SideId,
    val points: Int,
    val isAtTarget: Boolean,
    val players: ImmutableList<PanelPlayerUiState>,
    /** The player on this side who goes first, while the "1st" marker shows; `null` otherwise. */
    val firstPlayerName: String? = null,
) {
    /** "Ana", or "Ana & Ben" for a team. */
    val displayName: String get() = players.joinToString(separator = " & ") { it.name }
}

internal data class PanelPlayerUiState(
    val playerId: PlayerId,
    val name: String,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String?,
    /** Wide Legend art for the panel background (falls back to [legendImageUri]). */
    val legendWideImageUri: String? = null,
    /** Extra counters (e.g. XP) shown under the points, in the order they were added. */
    val counters: ImmutableList<CounterUiState> = persistentListOf(),
)

internal data class CounterUiState(val id: CounterId, val label: String, val value: Int)

/** Shown while at least one side is at the target and the banner hasn't been dismissed. */
internal data class WinnerBannerUiState(val names: ImmutableList<String> = persistentListOf())
