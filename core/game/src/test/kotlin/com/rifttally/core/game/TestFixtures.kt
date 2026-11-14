package com.rifttally.core.game

import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId

internal val P1 = PlayerId("p1")
internal val P2 = PlayerId("p2")
internal val P3 = PlayerId("p3")
internal val P4 = PlayerId("p4")
internal val ALL_PLAYERS = listOf(P1, P2, P3, P4)

internal val S1 = SideId("s1")
internal val S2 = SideId("s2")
internal val S3 = SideId("s3")
internal val S4 = SideId("s4")
internal val ALL_SIDES = listOf(S1, S2, S3, S4)

internal val TEAM_A = SideId("team_a")
internal val TEAM_B = SideId("team_b")

internal val XP = CounterId("xp")
internal val UNKNOWN_SIDE = SideId("nobody")
internal val UNKNOWN_PLAYER = PlayerId("nobody")
internal val UNKNOWN_COUNTER = CounterId("nothing")

private fun player(id: PlayerId): Player = Player(id = id, name = "Player ${id.value}", colorId = "orange")

/** Free-for-all with [playerCount] players; player N owns side N. */
internal fun ffaConfig(
    playerCount: Int = 2,
    targetScore: Int? = null,
    initialCounters: Map<PlayerId, List<Counter>> = emptyMap(),
): GameConfig {
    val format = when (playerCount) {
        2 -> GameFormat.TWO_PLAYERS
        3 -> GameFormat.THREE_PLAYERS
        else -> GameFormat.FOUR_PLAYERS
    }
    val players = ALL_PLAYERS.take(playerCount)
    return GameConfig(
        format = format,
        sides = players.mapIndexed { index, id -> Side(ALL_SIDES[index], listOf(id)) },
        players = players.map(::player),
        targetScore = targetScore ?: format.defaultTargetScore,
        initialCounters = initialCounters,
    )
}

/** 2v2: P1 + P3 are [TEAM_A], P2 + P4 are [TEAM_B]. */
internal fun teamConfig(initialCounters: Map<PlayerId, List<Counter>> = emptyMap()): GameConfig = GameConfig(
    format = GameFormat.TEAMS_2V2,
    sides = listOf(Side(TEAM_A, listOf(P1, P3)), Side(TEAM_B, listOf(P2, P4))),
    players = ALL_PLAYERS.map(::player),
    initialCounters = initialCounters,
)

/** Builds events with sequential ids and timestamps. */
internal class Events {
    private var sequence = 0L

    private fun nextId(): String = "e${++sequence}"

    fun points(side: SideId, delta: Int) = GameEvent.PointsAdjusted(nextId(), sequence, side, delta)

    fun addCounter(player: PlayerId, counter: CounterId = XP, label: String = "XP") =
        GameEvent.CounterAdded(nextId(), sequence, player, counter, label)

    fun adjustCounter(player: PlayerId, delta: Int, counter: CounterId = XP) =
        GameEvent.CounterAdjusted(nextId(), sequence, player, counter, delta)

    fun removeCounter(player: PlayerId, counter: CounterId = XP) =
        GameEvent.CounterRemoved(nextId(), sequence, player, counter)

    fun target(newScore: Int) = GameEvent.TargetScoreChanged(nextId(), sequence, newScore)

    fun end(winner: SideId?) = GameEvent.GameEnded(nextId(), sequence, winner)
}
