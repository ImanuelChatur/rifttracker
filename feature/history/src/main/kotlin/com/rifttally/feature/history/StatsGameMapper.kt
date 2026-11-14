package com.rifttally.feature.history

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.reduce
import com.rifttally.core.stats.PointChange
import com.rifttally.core.stats.StatsGame

/**
 * Turns a stored game into the stats input. Only events before the (valid) end count, like in
 * [reduce]; the winner and final target come from replaying the log.
 *
 * Same as `StatsGameMapper` in `:feature:stats` (features can't share code); move it to `:core:data`
 * once that module may depend on `:core:stats`.
 */
internal fun GameRecord.toStatsGame(): StatsGame {
    val state = reduce(config, events)
    val sideIds = config.sides.map { it.id }.toSet()
    val endIndex = events.indexOfFirst { it is GameEvent.GameEnded && (it.winner == null || it.winner in sideIds) }
    val played = if (endIndex >= 0) events.subList(0, endIndex) else events
    return StatsGame(
        id = id.value,
        config = config,
        createdAt = createdAt,
        endedAt = endedAt ?: createdAt,
        targetScore = state.targetScore,
        winner = state.winner,
        pointChanges = played.filterIsInstance<GameEvent.PointsAdjusted>()
            .map { PointChange(timestamp = it.timestamp, sideId = it.sideId, delta = it.delta) },
    )
}
