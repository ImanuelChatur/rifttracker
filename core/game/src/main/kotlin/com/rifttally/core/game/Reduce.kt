package com.rifttally.core.game

import com.rifttally.core.model.Counter
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.MIN_TARGET_SCORE
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId

/**
 * Derives the [GameState] from [config] and the ordered [events]. Pure: same input, same output.
 *
 * Rules:
 * - Points and counters never go below 0 (and saturate at [Int.MAX_VALUE] instead of overflowing).
 * - Events that reference an unknown side, player or counter are ignored, as is a
 *   [GameEvent.CounterAdded] whose counter id the player already has.
 * - [GameEvent.TargetScoreChanged] below [MIN_TARGET_SCORE] is ignored. An invalid starting target in
 *   [config] is raised to [MIN_TARGET_SCORE].
 * - [GameEvent.GameEnded] with an unknown winner is ignored. Once the game has ended, every later
 *   event is ignored.
 * - Initial counters from [config] are clamped at 0; for duplicate counter ids the first one wins.
 */
public fun reduce(config: GameConfig, events: List<GameEvent>): GameState {
    val points = LinkedHashMap<SideId, Int>()
    config.sides.forEach { points[it.id] = 0 }

    val counters = LinkedHashMap<PlayerId, MutableList<Counter>>()
    config.players.forEach { player ->
        counters[player.id] = config.initialCounters[player.id].orEmpty()
            .distinctBy { it.id }
            .map { it.copy(value = it.value.coerceAtLeast(0)) }
            .toMutableList()
    }

    var targetScore = config.targetScore.coerceAtLeast(MIN_TARGET_SCORE)
    var winner: SideId? = null
    var isEnded = false

    for (event in events) {
        if (isEnded) break
        when (event) {
            is GameEvent.PointsAdjusted -> {
                val current = points[event.sideId] ?: continue
                points[event.sideId] = current.plusFloored(event.delta)
            }

            is GameEvent.CounterAdded -> {
                val list = counters[event.playerId] ?: continue
                if (list.none { it.id == event.counterId }) {
                    list += Counter(id = event.counterId, label = event.label, value = 0)
                }
            }

            is GameEvent.CounterAdjusted -> {
                val list = counters[event.playerId] ?: continue
                val index = list.indexOfFirst { it.id == event.counterId }
                if (index >= 0) {
                    list[index] = list[index].copy(value = list[index].value.plusFloored(event.delta))
                }
            }

            is GameEvent.CounterRemoved -> {
                counters[event.playerId]?.removeAll { it.id == event.counterId }
            }

            is GameEvent.TargetScoreChanged -> {
                if (event.newScore >= MIN_TARGET_SCORE) targetScore = event.newScore
            }

            is GameEvent.GameEnded -> {
                if (event.winner == null || event.winner in points) {
                    isEnded = true
                    winner = event.winner
                }
            }
        }
    }

    return GameState(
        config = config,
        points = points.toMap(),
        counters = counters.mapValuesTo(LinkedHashMap()) { (_, list) -> list.toList() },
        targetScore = targetScore,
        isEnded = isEnded,
        winner = winner,
    )
}

private fun Int.plusFloored(delta: Int): Int = (toLong() + delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
