package com.rifttally.core.ui

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ResultCardStateTest {
    private val a = SideId("a")
    private val b = SideId("b")
    private val teams = GameConfig(
        format = GameFormat.TEAMS_2V2,
        sides = listOf(
            Side(a, listOf(PlayerId("p1"), PlayerId("p2"))),
            // p9 isn't a player of the config (corrupt data): skipped.
            Side(b, listOf(PlayerId("p3"), PlayerId("p9"), PlayerId("p4"))),
        ),
        players = listOf(
            Player(PlayerId("p1"), "Ana", "orange", legendId = "jinx"),
            Player(PlayerId("p2"), "Ben", "sky"),
            Player(PlayerId("p3"), "Player 3", "green", legendId = "zed", hasDefaultName = true),
            Player(PlayerId("p4"), "Dan", "pink", legendId = "viktor"),
        ),
    )
    private val legends = mapOf(
        "jinx" to Legend("jinx", "Jinx", "file:///jinx.webp"),
        "viktor" to Legend("viktor", "Viktor", "file:///viktor.webp"),
    )

    @Test
    fun of_mapsEverySideAndPlayer_withLegendNamesAndArt() {
        val card = ResultCardState.of(teams, mapOf(a to 11, b to 7), winner = a, endedAt = 42, legendsById = legends)

        assertEquals(GameFormat.TEAMS_2V2, card.format)
        assertEquals(42, card.endedAt)
        assertEquals(listOf("Ana & Ben", "Player 3 & Dan"), card.sides.map { it.name })
        assertEquals(listOf(11, 7), card.sides.map { it.score })
        assertEquals("Ana & Ben", card.winner?.name)
        assertEquals(
            listOf(
                ResultCardPlayer("Ana", "orange", "Jinx", "file:///jinx.webp"),
                ResultCardPlayer("Ben", "sky", null, null),
            ),
            card.sides[0].players,
        )
        // Unknown Legend: its id, no art. Placeholder names are shown as they are.
        assertEquals(ResultCardPlayer("Player 3", "green", "zed", null), card.sides[1].players[0])
        assertEquals(listOf("file:///jinx.webp", "file:///viktor.webp"), card.imageUris)
    }

    @Test
    fun of_noWinner_unknownWinner_andMissingScores() {
        val none = ResultCardState.of(teams, emptyMap(), winner = null, endedAt = 0, legendsById = emptyMap())
        val unknown = ResultCardState.of(teams, emptyMap(), winner = SideId("x"), endedAt = 0, legendsById = emptyMap())

        assertNull(none.winner)
        assertNull(unknown.winner)
        assertEquals(listOf(0, 0), none.sides.map { it.score })
        assertEquals(emptyList(), none.imageUris)
    }

    @Test
    fun imageUris_areDistinct_forAMirrorMatch() {
        val config = teams.copy(players = teams.players.map { it.copy(legendId = "jinx") })

        val card = ResultCardState.of(config, emptyMap(), winner = b, endedAt = 0, legendsById = legends)

        assertEquals(listOf("file:///jinx.webp"), card.imageUris)
    }
}
