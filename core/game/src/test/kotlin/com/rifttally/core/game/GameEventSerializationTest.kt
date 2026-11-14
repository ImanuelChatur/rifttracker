package com.rifttally.core.game

import com.rifttally.core.model.Counter
import com.rifttally.core.model.GameConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class GameEventSerializationTest {
    private val e = Events()
    private val json = Json

    @Test
    fun everyEventType_roundTrips() {
        val events: List<GameEvent> = listOf(
            e.points(S1, -2),
            e.addCounter(P1),
            e.adjustCounter(P1, 3),
            e.removeCounter(P1),
            e.target(11),
            e.end(S1),
            e.end(null),
        )

        events.forEach { event ->
            val encoded = json.encodeToString(GameEvent.serializer(), event)
            assertEquals(event, json.decodeFromString(GameEvent.serializer(), encoded))
        }
    }

    @Test
    fun usesTypeDiscriminatorAndInlineIds() {
        val event = GameEvent.PointsAdjusted("e1", 42, S1, 1)

        assertEquals(
            """{"type":"points_adjusted","id":"e1","timestamp":42,"sideId":"s1","delta":1}""",
            json.encodeToString(GameEvent.serializer(), event),
        )
    }

    @Test
    fun config_roundTrips() {
        val config = teamConfig(initialCounters = mapOf(P1 to listOf(Counter(XP, "XP", 2))))
        val encoded = json.encodeToString(GameConfig.serializer(), config)

        assertEquals(config, json.decodeFromString(GameConfig.serializer(), encoded))
    }
}
