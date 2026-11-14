package com.rifttally.core.data.game

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class GameJsonTest {
    @Test
    fun configWrittenBeforePresetIds_stillDecodes_withNullPresetId() {
        // Exactly what v1 stored in `config_json` (no `presetId` on players).
        val v1Json = """
            {"format":"TWO_PLAYERS",
             "sides":[{"id":"s1","playerIds":["p1"]},{"id":"s2","playerIds":["p2"]}],
             "players":[{"id":"p1","name":"Ana","colorId":"orange","legendId":"jinx"},
                        {"id":"p2","name":"Ben","colorId":"sky"}],
             "targetScore":8}
        """.trimIndent()

        val config = GameJson.decodeFromString(GameConfig.serializer(), v1Json)

        assertEquals(listOf("Ana", "Ben"), config.players.map { it.name })
        assertEquals("jinx", config.players[0].legendId)
        config.players.forEach { assertNull(it.presetId) }
    }

    @Test
    fun presetId_roundTrips() {
        val config = GameConfig(
            format = GameFormat.TWO_PLAYERS,
            sides = listOf(Side(SideId("s1"), listOf(PlayerId("p1"))), Side(SideId("s2"), listOf(PlayerId("p2")))),
            players = listOf(
                Player(PlayerId("p1"), "Ana", "orange", presetId = "preset-ana"),
                Player(PlayerId("p2"), "Ben", "sky"),
            ),
        )

        val decoded = GameJson.decodeFromString(
            GameConfig.serializer(),
            GameJson.encodeToString(GameConfig.serializer(), config),
        )

        assertEquals(config, decoded)
    }
}
