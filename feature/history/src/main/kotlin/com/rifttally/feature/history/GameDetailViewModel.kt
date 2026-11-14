package com.rifttally.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.model.GameId
import com.rifttally.core.stats.summarize
import com.rifttally.feature.history.navigation.GameDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One finished game: final scoreboard, duration, score-over-time chart and the full event log. */
@HiltViewModel
internal class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
) : ViewModel() {

    // Navigation stores route arguments under their property names (same approach as the tracker).
    private val gameId = GameId(checkNotNull(savedStateHandle.get<String>(GameDetailRoute::gameId.name)))

    val uiState: StateFlow<GameDetailUiState> = combine(
        gameRepository.observeGame(gameId),
        legendRepository.legends,
    ) { record, legends ->
        if (record == null) return@combine GameDetailUiState.NotFound
        val summary = summarize(record.toStatsGame())
        val sides = summary.sideUiStates(legends.associateBy { it.id })
        GameDetailUiState.Success(
            format = summary.format,
            endedAt = summary.endedAt,
            durationMinutes = summary.duration.inWholeMinutes,
            targetScore = summary.targetScore,
            sides = sides,
            winnerName = sides.firstOrNull { it.isWinner }?.name,
            chart = summary.sides.mapIndexed { index, side ->
                ChartSeriesUiState(
                    name = sides[index].name,
                    colorId = sides[index].colorId,
                    points = summary.timeline.map { it.scores[side.sideId] ?: 0 }.toImmutableList(),
                )
            }.toImmutableList(),
            log = gameLogItems(
                config = record.config,
                events = record.events,
                sideNames = summary.sides.mapIndexed { index, side -> side.sideId to sides[index].name }.toMap(),
            ).toImmutableList(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState.Loading)
}
