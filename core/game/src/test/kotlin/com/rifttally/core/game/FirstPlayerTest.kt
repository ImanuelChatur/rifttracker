package com.rifttally.core.game

import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirstPlayerTest {
    private val e = Events()

    @Test
    fun noPick_noFirstPlayer() {
        assertNull(reduce(ffaConfig(), listOf(e.points(S1, 1))).firstPlayer)
    }

    @Test
    fun latestPickWins() {
        assertEquals(P2, reduce(ffaConfig(), listOf(e.first(P1), e.points(S1, 1), e.first(P2))).firstPlayer)
    }

    @Test
    fun unknownPlayer_isIgnored() {
        assertEquals(P1, reduce(ffaConfig(), listOf(e.first(P1), e.first(UNKNOWN_PLAYER))).firstPlayer)
        // A player of the format that isn't in this 2-player game is unknown too.
        assertNull(reduce(ffaConfig(), listOf(e.first(P3))).firstPlayer)
    }

    @Test
    fun pickAfterTheEnd_isIgnored() {
        assertEquals(P1, reduce(ffaConfig(), listOf(e.first(P1), e.end(null), e.first(P2))).firstPlayer)
    }

    @Test
    fun undoingThePick_fallsBackToTheEarlierOne() {
        val events = listOf(e.first(P1), e.first(P2))

        assertEquals(P1, reduce(ffaConfig(), events.undo()).firstPlayer)
        assertNull(reduce(ffaConfig(), events.undo().undo()).firstPlayer)
    }

    @Test
    fun teams_pickAPlayer_notASide() {
        assertEquals(P4, reduce(teamConfig(), listOf(e.first(P4))).firstPlayer)
    }

    @Test
    fun pickFirstPlayer_usesTheRandomIndexOverAllPlayers() {
        val config = ffaConfig(playerCount = 4)
        val bounds = mutableListOf<Int>()

        val picked = (0 until 4).map { index ->
            pickFirstPlayer(config) { until -> index.also { bounds += until } }
        }

        assertEquals(ALL_PLAYERS, picked)
        assertEquals(listOf(4, 4, 4, 4), bounds)
    }

    @Test
    fun pickFirstPlayer_teams_picksAmongAllFourPlayers() {
        assertEquals(P2, pickFirstPlayer(teamConfig()) { 1 })
    }

    @Test
    fun pickFirstPlayer_outOfRangeIndex_isClamped() {
        assertEquals(P2, pickFirstPlayer(ffaConfig()) { 9 })
        assertEquals(P1, pickFirstPlayer(ffaConfig()) { -1 })
    }

    @Test
    fun pickFirstPlayer_noPlayers_returnsNull() {
        val empty = GameConfig(format = GameFormat.TWO_PLAYERS, sides = emptyList(), players = emptyList())

        assertNull(pickFirstPlayer(empty) { 0 })
    }
}
