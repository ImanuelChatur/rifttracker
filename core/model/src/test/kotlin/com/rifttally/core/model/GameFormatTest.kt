package com.rifttally.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameFormatTest {
    @Test
    fun defaultTargets() {
        assertEquals(8, GameFormat.TWO_PLAYERS.defaultTargetScore)
        assertEquals(8, GameFormat.THREE_PLAYERS.defaultTargetScore)
        assertEquals(8, GameFormat.FOUR_PLAYERS.defaultTargetScore)
        assertEquals(11, GameFormat.TEAMS_2V2.defaultTargetScore)
    }

    @Test
    fun onlyTwoVsTwoIsTeams() {
        assertTrue(GameFormat.TEAMS_2V2.isTeams)
        GameFormat.entries.filter { it != GameFormat.TEAMS_2V2 }.forEach { assertFalse(it.isTeams) }
    }

    @Test
    fun configDefaultsTargetFromFormat() {
        val config = GameConfig(format = GameFormat.TEAMS_2V2, sides = emptyList(), players = emptyList())

        assertEquals(DEFAULT_TEAM_TARGET_SCORE, config.targetScore)
    }
}
