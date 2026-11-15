package com.rifttally.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.game.GameState
import com.rifttally.core.game.reduce
import com.rifttally.core.model.Legend
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
@OptIn(ExperimentalTime::class)
internal class HomeViewModel @Inject constructor(
    gameRepository: GameRepository,
    legendRepository: LegendRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        gameRepository.activeGame,
        gameRepository.recentGames(RECENT_GAMES_LIMIT),
        legendRepository.legends,
    ) { game, recent, legends ->
        HomeUiState.Success(
            activeGame = game?.let { record ->
                val state = reduce(record.config, record.events)
                ActiveGameUiState(record.id, state.targetScore, record.sides(state, legends))
            },
            recentGames = recent.map { record ->
                val state = reduce(record.config, record.events)
                val sides = record.sides(state, legends)
                RecentGameUiState(
                    gameId = record.id,
                    endedAt = record.endedAt ?: record.createdAt,
                    sides = sides,
                    winnerName = sides.firstOrNull { it.isWinner }?.name,
                )
            }.toImmutableList(),
            now = clock.now().toEpochMilliseconds(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private fun GameRecord.sides(state: GameState, legends: List<Legend>): ImmutableList<ResumeSideUiState> {
        val legendsById = legends.associateBy { it.id }
        val playersById = config.players.associateBy { it.id }
        return config.sides.map { side ->
            val players = side.playerIds.mapNotNull(playersById::get)
            val first = players.firstOrNull()
            val legend = first?.legendId?.let(legendsById::get)
            ResumeSideUiState(
                name = players.joinToString(separator = " & ") { it.name },
                points = state.pointsOf(side.id),
                colorId = first?.colorId.orEmpty(),
                legendName = legend?.name,
                legendImageUri = legend?.imageUri,
                isWinner = side.id == state.winner,
            )
        }.toImmutableList()
    }

    private companion object {
        const val RECENT_GAMES_LIMIT = 20
    }
}
