package com.rifttally.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.game.reduce
import com.rifttally.core.model.Legend
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class HomeViewModel @Inject constructor(gameRepository: GameRepository, legendRepository: LegendRepository) :
    ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        gameRepository.activeGame,
        legendRepository.legends,
    ) { game, legends -> HomeUiState.Success(activeGame = game?.toUiState(legends)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private fun GameRecord.toUiState(legends: List<Legend>): ActiveGameUiState {
        val state = reduce(config, events)
        val legendsById = legends.associateBy { it.id }
        val playersById = config.players.associateBy { it.id }
        return ActiveGameUiState(
            gameId = id,
            targetScore = state.targetScore,
            sides = config.sides.map { side ->
                val players = side.playerIds.mapNotNull(playersById::get)
                val first = players.firstOrNull()
                val legend = first?.legendId?.let(legendsById::get)
                ResumeSideUiState(
                    name = players.joinToString(separator = " & ") { it.name },
                    points = state.pointsOf(side.id),
                    colorId = first?.colorId.orEmpty(),
                    legendName = legend?.name,
                    legendImageUri = legend?.imageUri,
                )
            }.toImmutableList(),
        )
    }
}
