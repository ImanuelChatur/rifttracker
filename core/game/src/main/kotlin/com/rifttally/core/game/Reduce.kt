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
    val reducer = Reducer(config)
    events.forEach(reducer::apply)
    return reducer.snapshot()
}

/**
 * Replays [events] like [reduce] and pairs each event with the state right after it, oldest first.
 * Drives the history log ("Jinx +1 · 6").
 */
public fun history(config: GameConfig, events: List<GameEvent>): List<HistoryEntry> {
    val reducer = Reducer(config)
    return events.map { event ->
        reducer.apply(event)
        HistoryEntry(event = event, stateAfter = reducer.snapshot())
    }
}

/** Mutable accumulator behind [reduce] and [history]. Never escapes this file. */
private class Reducer(private val config: GameConfig) {
    private val points = LinkedHashMap<SideId, Int>().apply { config.sides.forEach { put(it.id, 0) } }

    private val counters = LinkedHashMap<PlayerId, MutableList<Counter>>().apply {
        config.players.forEach { player ->
            put(
                player.id,
                config.initialCounters[player.id].orEmpty()
                    .distinctBy { it.id }
                    .map { it.copy(value = it.value.coerceAtLeast(0)) }
                    .toMutableList(),
            )
        }
    }

    private var targetScore = config.targetScore.coerceAtLeast(MIN_TARGET_SCORE)
    private var winner: SideId? = null
    private var isEnded = false

    fun apply(event: GameEvent) {
        if (isEnded) return
        when (event) {
            is GameEvent.PointsAdjusted -> {
                val current = points[event.sideId] ?: return
                points[event.sideId] = current.plusFloored(event.delta)
            }

            is GameEvent.CounterAdded -> {
                val list = counters[event.playerId] ?: return
                if (list.none { it.id == event.counterId }) {
                    list += Counter(id = event.counterId, label = event.label, value = 0)
                }
            }

            is GameEvent.CounterAdjusted -> {
                val list = counters[event.playerId] ?: return
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

    fun snapshot(): GameState = GameState(
        config = config,
        points = points.toMap(),
        counters = counters.mapValuesTo(LinkedHashMap()) { (_, list) -> list.toList() },
        targetScore = targetScore,
        isEnded = isEnded,
        winner = winner,
    )
}

private fun Int.plusFloored(delta: Int): Int = (toLong() + delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
