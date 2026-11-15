package com.rifttally.feature.history

import com.rifttally.core.game.GameLogLine
import com.rifttally.core.model.GameFormat
import com.rifttally.core.ui.ResultCardState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface GameDetailUiState {
    data object Loading : GameDetailUiState

    /** The game doesn't exist (e.g. deleted) or can't be read. */
    data object NotFound : GameDetailUiState

    /**
     * @property durationMinutes how long the game took, in whole minutes.
     * @property sides the final scoreboard, in seat order.
     * @property chart one line per side for the score-over-time chart, in [sides] order.
     * @property log the full event log, oldest first.
     * @property resultCard what "Share" renders into a picture.
     */
    data class Success(
        val format: GameFormat,
        val endedAt: Long,
        val durationMinutes: Long,
        val targetScore: Int,
        val sides: ImmutableList<HistorySideUiState>,
        val winnerName: String?,
        val chart: ImmutableList<ChartSeriesUiState> = persistentListOf(),
        val log: ImmutableList<GameLogLine> = persistentListOf(),
        val resultCard: ResultCardState,
    ) : GameDetailUiState
}

/** One line of the score chart: [points] is the side's score at the start and after each points change. */
internal data class ChartSeriesUiState(val name: String, val colorId: String, val points: ImmutableList<Int>)
