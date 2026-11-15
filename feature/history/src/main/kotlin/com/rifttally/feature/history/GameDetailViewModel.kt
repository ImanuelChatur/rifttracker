package com.rifttally.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.common.dispatchers.DefaultDispatcher
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.game.rematchConfig
import com.rifttally.core.data.game.toStatsGame
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.game.gameLog
import com.rifttally.core.game.history
import com.rifttally.core.model.GameId
import com.rifttally.core.stats.summarize
import com.rifttally.feature.history.navigation.GameDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One finished game: final scoreboard, duration, score-over-time chart and the full event log, plus
 * "Play again" (a new game with the same lineup).
 */
@HiltViewModel
internal class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    legendRepository: LegendRepository,
    // Stats replay every game's log: keep that work off the main thread.
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    private val firstPlayerPicker: FirstPlayerPicker,
) : ViewModel() {

    // Navigation stores route arguments under their property names (same approach as the tracker).
    private val gameId = GameId(checkNotNull(savedStateHandle.get<String>(GameDetailRoute::gameId.name)))

    /** The game as last read, for [onPlayAgain]. */
    @Volatile
    private var latestRecord: GameRecord? = null

    /** Set once "Play again" is creating a game, so a double tap creates one. */
    private var isStarting = false

    private val navigation = Channel<GameDetailNavigationEvent>(Channel.BUFFERED)

    /** One-off navigation requests (open the game "Play again" created). */
    val navigationEvents: Flow<GameDetailNavigationEvent> = navigation.receiveAsFlow()

    val uiState: StateFlow<GameDetailUiState> = combine(
        gameRepository.observeGame(gameId),
        legendRepository.legends,
    ) { record, legends ->
        latestRecord = record
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
            log = gameLog(
                config = record.config,
                entries = history(record.config, record.events),
                sideNames = summary.sides.mapIndexed { index, side -> side.sideId to sides[index].name }.toMap(),
            ).toImmutableList(),
        )
    }.flowOn(defaultDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState.Loading)

    /**
     * Starts a new game with this game's lineup and final target (as Rematch does on the tracker), with
     * its own first-player pick when that setting is on, and asks to open it.
     */
    fun onPlayAgain() {
        val record = latestRecord ?: return
        if (isStarting) return
        isStarting = true
        val config = record.rematchConfig()
        viewModelScope.launch {
            val newGameId = try {
                gameRepository.createGame(config, firstPlayerPicker.openingEvents(config))
            } catch (e: Exception) {
                isStarting = false // let "Play again" be tried again
                throw e
            }
            navigation.send(GameDetailNavigationEvent.OpenTracker(newGameId))
        }
    }
}

/** Navigation requested by [GameDetailViewModel]. */
internal sealed interface GameDetailNavigationEvent {
    /** "Play again" created [gameId]: open it in the tracker. */
    data class OpenTracker(val gameId: GameId) : GameDetailNavigationEvent
}
