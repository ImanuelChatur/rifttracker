package com.rifttally.core.stats

import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId

/**
 * One person's part in one game. Seats that share a [PlayerKey] in the same game (e.g. two players
 * both called "Ana") are merged into one appearance, so a game is never counted twice for a person.
 *
 * @property sides the sides this person played on (normally one).
 * @property legendIds the Legends this person picked (non-null only), in seat order.
 * @property won whether one of [sides] was named the winner.
 */
internal data class Appearance(
    val gameId: String,
    val key: PlayerKey,
    val name: String,
    val sides: Set<SideId>,
    val legendIds: List<String>,
    val won: Boolean,
)

/** Newest-first order used everywhere a "latest" or "current streak" matters. Ties are broken by id. */
internal fun List<StatsGame>.newestFirst(): List<StatsGame> =
    sortedWith(compareByDescending<StatsGame> { it.endedAt }.thenBy { it.id })

/** The side each player sits on, for players that are on a side. */
internal fun StatsGame.sideOfPlayer(): Map<PlayerId, SideId> =
    config.sides.flatMap { side -> side.playerIds.map { it to side.id } }.toMap()

/** The game's valid winner, or `null` (none named, or not one of its sides). */
internal val StatsGame.validWinner: SideId?
    get() = winner?.takeIf { w -> config.sides.any { it.id == w } }

/**
 * Every appearance in [games], newest game first. Players not on any side are ignored, and so are
 * placeholder-named ones ([com.rifttally.core.model.Player.hasDefaultName]): they aren't a person, so
 * they're left out of player stats and head-to-head (their Legend still counts in [legendStats]).
 */
internal fun appearances(games: List<StatsGame>): List<Appearance> = games.newestFirst().flatMap { game ->
    val sideOf = game.sideOfPlayer()
    val winner = game.validWinner
    game.config.players
        .filter { it.id in sideOf && !it.hasDefaultName }
        .groupBy { PlayerKey.of(it) }
        .map { (key, seats) ->
            val sides = seats.mapNotNull { sideOf[it.id] }.toSet()
            Appearance(
                gameId = game.id,
                key = key,
                name = seats.first().name.trim(),
                sides = sides,
                legendIds = seats.mapNotNull { it.legendId },
                won = winner != null && winner in sides,
            )
        }
}
