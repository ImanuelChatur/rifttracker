package com.rifttally.feature.home

import com.rifttally.core.model.GameId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** [activeGame] is the game to resume, or `null` when there's none. */
    data class Success(
        val activeGame: ActiveGameUiState?,
        /** Ended games, newest first (read-only). */
        val recentGames: ImmutableList<RecentGameUiState> = persistentListOf(),
        /** "Now" (epoch ms) from the injected clock, for "2 hours ago" labels. */
        val now: Long = 0,
    ) : HomeUiState
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
    /** True for the side that won an ended game (by id, so equal names can't both be highlighted). */
    val isWinner: Boolean = false,
)

/** One ended game: final scores per side, who won (if anyone was named), and when it ended (epoch ms). */
internal data class RecentGameUiState(
    val gameId: GameId,
    val endedAt: Long,
    val sides: ImmutableList<ResumeSideUiState>,
    val winnerName: String?,
)
