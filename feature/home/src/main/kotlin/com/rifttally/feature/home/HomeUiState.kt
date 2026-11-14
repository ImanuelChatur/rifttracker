package com.rifttally.feature.home

import com.rifttally.core.model.GameId
import kotlinx.collections.immutable.ImmutableList

internal sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [activeGame] is the game to resume, or `null` when there's none. */
    data class Success(val activeGame: ActiveGameUiState?) : HomeUiState
}

/** Summary of the game in progress for the "Resume" card. */
internal data class ActiveGameUiState(
    val gameId: GameId,
    val targetScore: Int,
    val sides: ImmutableList<ResumeSideUiState>,
)

internal data class ResumeSideUiState(
    val name: String,
    val points: Int,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String?,
)
