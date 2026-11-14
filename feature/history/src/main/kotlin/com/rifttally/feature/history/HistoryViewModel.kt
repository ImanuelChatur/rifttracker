package com.rifttally.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.stats.summarize
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Match history: every ended game, newest first, filterable by format, deletable with undo. */
@HiltViewModel
internal class HistoryViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    legendRepository: LegendRepository,
) : ViewModel() {

    private val selectedFormat = MutableStateFlow<GameFormat?>(null)

    /** The last deleted game, kept in memory so "Undo" can restore it exactly. */
    private val recentlyDeleted = MutableStateFlow<GameRecord?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        gameRepository.endedGames(),
        legendRepository.legends,
        selectedFormat,
        recentlyDeleted,
    ) { games, legends, format, deleted ->
        val legendsById = legends.associateBy { it.id }
        HistoryUiState.Success(
            games = games
                .filter { format == null || it.config.format == format }
                .map { record ->
                    val summary = summarize(record.toStatsGame())
                    val sides = summary.sideUiStates(legendsById)
                    HistoryGameUiState(
                        gameId = record.id,
                        endedAt = summary.endedAt,
                        format = summary.format,
                        sides = sides,
                        winnerName = sides.firstOrNull { it.isWinner }?.name,
                    )
                }.toImmutableList(),
            selectedFormat = format,
            hasAnyGames = games.isNotEmpty(),
            recentlyDeleted = deleted?.id,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    /** Shows only games of [format], or all games for `null`. */
    fun onFormatSelect(format: GameFormat?) {
        selectedFormat.value = format
    }

    /** Deletes the game right away; [onUndoDelete] can bring it back until [onUndoDismiss]. */
    fun onDeleteGame(gameId: GameId) {
        viewModelScope.launch {
            val record = gameRepository.observeGame(gameId).first() ?: return@launch
            gameRepository.deleteGame(gameId)
            recentlyDeleted.value = record
        }
    }

    fun onUndoDelete() {
        val record = recentlyDeleted.value ?: return
        recentlyDeleted.value = null
        viewModelScope.launch { gameRepository.restoreGame(record) }
    }

    /** The undo snackbar went away without "Undo": the deletion stays. */
    fun onUndoDismiss() {
        recentlyDeleted.value = null
    }
}
