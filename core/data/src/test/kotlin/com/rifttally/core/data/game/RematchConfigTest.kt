package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import kotlin.test.assertEquals
import org.junit.Test

class RematchConfigTest {
    private val p1 = PlayerId("p1")
    private val p2 = PlayerId("p2")
    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(SideId("s1"), listOf(p1)), Side(SideId("s2"), listOf(p2))),
        players = listOf(
            Player(p1, "Ana", "orange", legendId = "jinx", presetId = "ana"),
            Player(p2, "Player 2", "sky", hasDefaultName = true),
        ),
        targetScore = 8,
        initialCounters = mapOf(p1 to listOf(Counter(CounterId("xp"), "XP"))),
    )

    @Test
    fun keepsThePlayersAndInitialCounters_withTheFinalTarget() {
        val record = GameRecord(
            id = GameId("g"),
            config = config,
            events = listOf(
                GameEvent.TargetScoreChanged("e1", 1, 10),
                // Counters added mid-game aren't part of the rematch.
                GameEvent.CounterAdded("e2", 2, p2, CounterId("gold"), "Gold"),
                GameEvent.GameEnded("e3", 3, winner = null),
            ),
            createdAt = 0,
            endedAt = 3,
        )

        assertEquals(config.copy(targetScore = 10), record.rematchConfig())
    }

    @Test
    fun noEvents_keepsTheConfig() {
        assertEquals(config, GameRecord(GameId("g"), config, emptyList(), createdAt = 0).rematchConfig())
    }
}
