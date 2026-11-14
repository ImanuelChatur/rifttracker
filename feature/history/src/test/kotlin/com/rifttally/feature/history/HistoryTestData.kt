package com.rifttally.feature.history

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId

internal val S1 = SideId("s1")
internal val S2 = SideId("s2")
internal val TEAM_A = SideId("team_a")
internal val TEAM_B = SideId("team_b")

internal val twoPlayers = GameConfig(
    format = GameFormat.TWO_PLAYERS,
    sides = listOf(Side(S1, listOf(PlayerId("p1"))), Side(S2, listOf(PlayerId("p2")))),
    players = listOf(
        Player(PlayerId("p1"), "Ana", "orange", legendId = "jinx"),
        Player(PlayerId("p2"), "Ben", "sky"),
    ),
)

internal val teams = GameConfig(
    format = GameFormat.TEAMS_2V2,
    sides = listOf(
        Side(TEAM_A, listOf(PlayerId("p1"), PlayerId("p2"))),
        Side(TEAM_B, listOf(PlayerId("p3"), PlayerId("p4"))),
    ),
    players = listOf(
        Player(PlayerId("p1"), "Ana", "orange", legendId = "jinx"),
        Player(PlayerId("p2"), "Cleo", "green", legendId = "viktor"),
        Player(PlayerId("p3"), "Ben", "sky"),
        Player(PlayerId("p4"), "Dan", "pink"),
    ),
)

/** An ended game: [points] are (side, delta) changes one second apart, then the end at [endedAt]. */
internal fun endedGame(
    id: String,
    endedAt: Long,
    config: GameConfig = twoPlayers,
    winner: SideId? = S1,
    createdAt: Long = 0,
    points: List<Pair<SideId, Int>> = emptyList(),
): GameRecord {
    val changes = points.mapIndexed { index, (side, delta) ->
        GameEvent.PointsAdjusted("$id-e$index", createdAt + (index + 1) * 1_000L, side, delta)
    }
    return GameRecord(
        id = GameId(id),
        config = config,
        events = changes + GameEvent.GameEnded("$id-end", endedAt, winner),
        createdAt = createdAt,
        endedAt = endedAt,
    )
}
