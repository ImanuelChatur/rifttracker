package com.rifttally.feature.setup

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.rematchConfig
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.SideId
import com.rifttally.core.stats.PlayerKey

/** At most this many lineups are offered at the top of Setup. */
internal const val MAX_RECENT_LINEUPS: Int = 5

/**
 * Up to [limit] distinct lineups from [games] (ended or not), most recently played first, each as the
 * rematch config (final target) of the most recent game that used it. Two games have the same lineup when
 * the format and every seat's player ([PlayerKey]), Legend, color and side are equal.
 */
internal fun recentLineups(games: List<GameRecord>, limit: Int = MAX_RECENT_LINEUPS): List<GameConfig> =
    games.sortedByDescending { it.lastPlayedAt() }
        .asSequence()
        .map { it.rematchConfig() }
        .distinctBy { it.lineupKey() }
        .take(limit)
        .toList()

/** When the game was last played: its end, else its newest event, else its creation. */
private fun GameRecord.lastPlayedAt(): Long = endedAt ?: events.maxOfOrNull { it.timestamp } ?: createdAt

private data class LineupKey(val format: GameFormat, val seats: List<SeatKey>)

private data class SeatKey(val player: PlayerKey, val legendId: String?, val colorId: String, val sideId: SideId?)

private fun GameConfig.lineupKey(): LineupKey = LineupKey(
    format = format,
    seats = players.map { player ->
        SeatKey(
            player = PlayerKey.of(player),
            legendId = player.legendId,
            colorId = player.colorId,
            sideId = sides.firstOrNull { player.id in it.playerIds }?.id,
        )
    },
)
