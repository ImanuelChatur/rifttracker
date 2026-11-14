package com.rifttally.core.stats

import com.rifttally.core.model.SideId
import kotlin.time.Duration.Companion.milliseconds

/**
 * Final scores, winner, duration and points timeline of [game].
 *
 * Replays [StatsGame.pointChanges] with the tracker's rules: scores never go below 0 (and saturate at
 * [Int.MAX_VALUE]) and changes for unknown sides are ignored. A winner that isn't one of the game's
 * sides counts as no winner.
 */
public fun summarize(game: StatsGame): GameSummary {
    val scores = LinkedHashMap<SideId, Int>()
    game.config.sides.forEach { scores[it.id] = 0 }
    val timeline = ArrayList<TimelinePoint>(game.pointChanges.size + 1)
    timeline += TimelinePoint(game.createdAt, scores.toMap())
    game.pointChanges.forEach { change ->
        val current = scores[change.sideId] ?: return@forEach
        scores[change.sideId] = (current.toLong() + change.delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        timeline += TimelinePoint(change.timestamp, scores.toMap())
    }
    val winner = game.winner?.takeIf { it in scores }
    val playersById = game.config.players.associateBy { it.id }
    return GameSummary(
        gameId = game.id,
        format = game.config.format,
        targetScore = game.targetScore,
        createdAt = game.createdAt,
        endedAt = game.endedAt,
        duration = (game.endedAt - game.createdAt).coerceAtLeast(0).milliseconds,
        sides = game.config.sides.map { side ->
            SideResult(
                sideId = side.id,
                players = side.playerIds.mapNotNull(playersById::get),
                finalScore = scores.getValue(side.id),
                isWinner = side.id == winner,
            )
        },
        winner = winner,
        timeline = timeline,
    )
}
