package com.rifttally.core.stats

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId

/** A seat in a test game: [name], optional [legend] and [preset]. */
internal data class Seat(val name: String, val legend: String? = null, val preset: String? = null)

internal fun sideId(index: Int) = SideId("s${index + 1}")

/**
 * A free-for-all game where seat N owns side N. [winner] is a seat index (or `null`). [ended] orders
 * games in time.
 */
internal fun ffa(
    vararg seats: Seat,
    winner: Int? = null,
    ended: Long = 1_000,
    id: String = "g$ended",
    changes: List<PointChange> = emptyList(),
): StatsGame {
    val players = seats.mapIndexed { index, seat ->
        Player(PlayerId("p${index + 1}"), seat.name, "orange", legendId = seat.legend, presetId = seat.preset)
    }
    val format = when (seats.size) {
        2 -> GameFormat.TWO_PLAYERS
        3 -> GameFormat.THREE_PLAYERS
        else -> GameFormat.FOUR_PLAYERS
    }
    return StatsGame(
        id = id,
        config = GameConfig(
            format = format,
            sides = players.mapIndexed { index, player -> Side(sideId(index), listOf(player.id)) },
            players = players,
        ),
        createdAt = 0,
        endedAt = ended,
        targetScore = format.defaultTargetScore,
        winner = winner?.let(::sideId),
        pointChanges = changes,
    )
}

internal val TEAM_A = SideId("team_a")
internal val TEAM_B = SideId("team_b")

/** A 2v2 game: [teamA] vs [teamB]; [winner] is [TEAM_A], [TEAM_B] or `null`. */
internal fun teams(teamA: Pair<Seat, Seat>, teamB: Pair<Seat, Seat>, winner: SideId?, ended: Long = 1_000): StatsGame {
    val seats = listOf(teamA.first, teamA.second, teamB.first, teamB.second)
    val players = seats.mapIndexed { index, seat ->
        Player(PlayerId("p${index + 1}"), seat.name, "orange", legendId = seat.legend, presetId = seat.preset)
    }
    return StatsGame(
        id = "t$ended",
        config = GameConfig(
            format = GameFormat.TEAMS_2V2,
            sides = listOf(
                Side(TEAM_A, listOf(players[0].id, players[1].id)),
                Side(TEAM_B, listOf(players[2].id, players[3].id)),
            ),
            players = players,
        ),
        createdAt = 0,
        endedAt = ended,
        targetScore = GameFormat.TEAMS_2V2.defaultTargetScore,
        winner = winner,
        pointChanges = emptyList(),
    )
}

internal val ANA = Seat("Ana")
internal val BEN = Seat("Ben")
internal val CLEO = Seat("Cleo")
internal val DAN = Seat("Dan")

internal fun key(name: String) = PlayerKey("name:${name.lowercase()}")
