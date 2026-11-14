package com.rifttally.feature.history

import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    /**
     * @property games ended games matching [selectedFormat], newest first.
     * @property selectedFormat the format filter, or `null` for all formats.
     * @property hasAnyGames whether any game ended at all (empty history vs. empty filter result).
     * @property recentlyDeleted a game that was just deleted and can still be restored (undo snackbar).
     */
    data class Success(
        val games: ImmutableList<HistoryGameUiState> = persistentListOf(),
        val selectedFormat: GameFormat? = null,
        val hasAnyGames: Boolean = false,
        val recentlyDeleted: GameId? = null,
    ) : HistoryUiState
}

/** One ended game in the list. [winnerName] is `null` when it ended without a winner. */
internal data class HistoryGameUiState(
    val gameId: GameId,
    val endedAt: Long,
    val format: GameFormat,
    val sides: ImmutableList<HistorySideUiState>,
    val winnerName: String?,
)

/**
 * A side's final result. [name] joins teammates ("Ana & Ben"); [legendNames] likewise, or `null` when
 * nobody on the side picked a Legend. The avatar shows the first player's Legend and color.
 */
internal data class HistorySideUiState(
    val name: String,
    val legendNames: String?,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String?,
    val score: Int,
    val isWinner: Boolean,
)
