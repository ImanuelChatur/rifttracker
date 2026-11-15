package com.rifttally.feature.stats

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.TestClock
import java.time.ZoneId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Berlin: an offset from UTC, so "This year" really depends on the zone. */
internal val TEST_ZONE: ZoneId = ZoneId.of("Europe/Berlin")

/** A [StatsGamesFilter] at [nowMillis] in [TEST_ZONE]. */
@OptIn(ExperimentalTime::class)
internal fun testGamesFilter(nowMillis: Long = 0): StatsGamesFilter =
    StatsGamesFilter(TestClock(Instant.fromEpochMilliseconds(nowMillis)), { TEST_ZONE })

/**
 * A finished 1v1: [first] (side s1) vs [second] (side s2). [winner] is 0, 1 or `null`; [goesFirst] is
 * the seat (0 or 1) picked to go first, or `null` for no pick.
 */
internal fun duel(
    id: String,
    first: Pair<String, String?>,
    second: Pair<String, String?>,
    winner: Int?,
    endedAt: Long,
    firstPreset: String? = null,
    goesFirst: Int? = null,
): GameRecord {
    val sides = listOf(SideId("s1"), SideId("s2"))
    val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(sides[0], listOf(PlayerId("p1"))), Side(sides[1], listOf(PlayerId("p2")))),
        players = listOf(
            Player(PlayerId("p1"), first.first, "orange", legendId = first.second, presetId = firstPreset),
            Player(PlayerId("p2"), second.first, "sky", legendId = second.second),
        ),
    )
    return GameRecord(
        id = GameId(id),
        config = config,
        events = listOfNotNull(
            goesFirst?.let { GameEvent.FirstPlayerChosen("$id-first", 0, config.players[it].id) },
            GameEvent.GameEnded("$id-end", endedAt, winner?.let(sides::get)),
        ),
        createdAt = 0,
        endedAt = endedAt,
    )
}
