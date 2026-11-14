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

/** A finished 1v1: [first] (side s1) vs [second] (side s2). [winner] is 0, 1 or `null`. */
internal fun duel(
    id: String,
    first: Pair<String, String?>,
    second: Pair<String, String?>,
    winner: Int?,
    endedAt: Long,
    firstPreset: String? = null,
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
        events = listOf(GameEvent.GameEnded("$id-end", endedAt, winner?.let(sides::get))),
        createdAt = 0,
        endedAt = endedAt,
    )
}
