package com.rifttally.core.game

import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.DEFAULT_TARGET_SCORE
import com.rifttally.core.model.DEFAULT_TEAM_TARGET_SCORE
import com.rifttally.core.model.SideId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReduceTest {
    private val e = Events()

    // region Empty / initial state

    @Test
    fun emptyEvents_allSidesAtZero_defaultTarget_notEnded() {
        val config = ffaConfig(playerCount = 4)
        val state = reduce(config, emptyList())

        assertEquals(config, state.config)
        assertEquals(ALL_SIDES.associateWith { 0 }, state.points)
        assertEquals(ALL_SIDES, state.points.keys.toList())
        assertEquals(ALL_PLAYERS.associateWith { emptyList<Counter>() }, state.counters)
        assertEquals(DEFAULT_TARGET_SCORE, state.targetScore)
        assertFalse(state.isEnded)
        assertNull(state.winner)
    }

    @Test
    fun emptyEvents_teams_defaultTargetIs11() {
        val state = reduce(teamConfig(), emptyList())

        assertEquals(DEFAULT_TEAM_TARGET_SCORE, state.targetScore)
        assertEquals(listOf(TEAM_A, TEAM_B), state.points.keys.toList())
    }

    @Test
    fun startingTargetFromConfigIsUsed() {
        assertEquals(15, reduce(ffaConfig(targetScore = 15), emptyList()).targetScore)
    }

    @Test
    fun invalidStartingTarget_isRaisedToOne() {
        assertEquals(1, reduce(ffaConfig(targetScore = 0), emptyList()).targetScore)
        assertEquals(1, reduce(ffaConfig(targetScore = -5), emptyList()).targetScore)
    }

    @Test
    fun initialCountersFromConfig_inOrder() {
        val gold = Counter(CounterId("gold"), "Gold", 2)
        val config = ffaConfig(
            initialCounters = mapOf(P1 to listOf(Counter(XP, "XP"), gold), P2 to listOf(Counter(XP, "XP", 3))),
        )
        val state = reduce(config, emptyList())

        assertEquals(listOf(Counter(XP, "XP", 0), gold), state.countersOf(P1))
        assertEquals(listOf(Counter(XP, "XP", 3)), state.countersOf(P2))
    }

    @Test
    fun initialCounters_negativeClampedAndDuplicatesDropped_unknownPlayerIgnored() {
        val config = ffaConfig(
            initialCounters = mapOf(
                P1 to listOf(Counter(XP, "XP", -4), Counter(XP, "Duplicate", 9)),
                UNKNOWN_PLAYER to listOf(Counter(XP, "XP")),
            ),
        )
        val state = reduce(config, emptyList())

        assertEquals(listOf(Counter(XP, "XP", 0)), state.countersOf(P1))
        assertEquals(setOf(P1, P2), state.counters.keys)
    }

    @Test
    fun initialCounters_canBeAdjustedAndRemoved() {
        val config = ffaConfig(initialCounters = mapOf(P1 to listOf(Counter(XP, "XP"))))

        assertEquals(2, reduce(config, listOf(e.adjustCounter(P1, 2))).countersOf(P1).single().value)
        assertEquals(emptyList(), reduce(config, listOf(e.removeCounter(P1))).countersOf(P1))
    }

    // endregion

    // region Points

    @Test
    fun points_upAndDown() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, 1), e.points(S1, 1), e.points(S2, 3), e.points(S1, -1)))

        assertEquals(1, state.pointsOf(S1))
        assertEquals(3, state.pointsOf(S2))
    }

    @Test
    fun points_flooredAtZero() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, -1), e.points(S2, 2), e.points(S2, -5)))

        assertEquals(0, state.pointsOf(S1))
        assertEquals(0, state.pointsOf(S2))
    }

    @Test
    fun points_floorAppliesPerEvent_notToTheSum() {
        // -3 is floored to 0 first, so the following +2 gives 2 (not 0).
        val state = reduce(ffaConfig(), listOf(e.points(S1, 1), e.points(S1, -3), e.points(S1, 2)))

        assertEquals(2, state.pointsOf(S1))
    }

    @Test
    fun points_saturateInsteadOfOverflowing() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, Int.MAX_VALUE), e.points(S1, Int.MAX_VALUE)))

        assertEquals(Int.MAX_VALUE, state.pointsOf(S1))
        assertEquals(0, reduce(ffaConfig(), listOf(e.points(S1, Int.MIN_VALUE))).pointsOf(S1))
    }

    @Test
    fun points_zeroDeltaIsNoOp() {
        assertEquals(reduce(ffaConfig(), emptyList()), reduce(ffaConfig(), listOf(e.points(S1, 0))))
    }

    @Test
    fun points_unknownSideIgnored() {
        val config = ffaConfig()
        val state = reduce(config, listOf(e.points(UNKNOWN_SIDE, 3)))

        assertEquals(reduce(config, emptyList()), state)
        assertEquals(0, state.pointsOf(UNKNOWN_SIDE))
    }

    @Test
    fun teams_sharePointsPerSide() {
        val state = reduce(teamConfig(), listOf(e.points(TEAM_A, 2), e.points(TEAM_B, 1), e.points(TEAM_A, 3)))

        assertEquals(mapOf(TEAM_A to 5, TEAM_B to 1), state.points)
    }

    @Test
    fun teams_countersStayPerPlayer() {
        val state = reduce(
            teamConfig(),
            listOf(e.addCounter(P1), e.addCounter(P3), e.adjustCounter(P1, 2), e.adjustCounter(P3, 5)),
        )

        assertEquals(2, state.countersOf(P1).single().value)
        assertEquals(5, state.countersOf(P3).single().value)
        assertEquals(emptyList(), state.countersOf(P2))
    }

    @Test
    fun teams_playerIdIsNotASideId() {
        // A player's id can't be used to score in 2v2: points belong to the team side.
        val state = reduce(teamConfig(), listOf(e.points(SideId(P1.value), 1)))

        assertEquals(mapOf(TEAM_A to 0, TEAM_B to 0), state.points)
    }

    // endregion

    // region Counters

    @Test
    fun counter_addedStartsAtZero_inInsertionOrder() {
        val gold = CounterId("gold")
        val state = reduce(ffaConfig(), listOf(e.addCounter(P1), e.addCounter(P1, gold, "Gold")))

        assertEquals(listOf(Counter(XP, "XP", 0), Counter(gold, "Gold", 0)), state.countersOf(P1))
        assertEquals(emptyList(), state.countersOf(P2))
    }

    @Test
    fun counter_sameIdOnDifferentPlayersIsIndependent() {
        val state = reduce(ffaConfig(), listOf(e.addCounter(P1), e.addCounter(P2), e.adjustCounter(P2, 4)))

        assertEquals(0, state.countersOf(P1).single().value)
        assertEquals(4, state.countersOf(P2).single().value)
    }

    @Test
    fun counter_duplicateAddIgnored() {
        val state =
            reduce(ffaConfig(), listOf(e.addCounter(P1), e.adjustCounter(P1, 3), e.addCounter(P1, label = "Again")))

        assertEquals(listOf(Counter(XP, "XP", 3)), state.countersOf(P1))
    }

    @Test
    fun counter_addForUnknownPlayerIgnored() {
        val config = ffaConfig()

        assertEquals(reduce(config, emptyList()), reduce(config, listOf(e.addCounter(UNKNOWN_PLAYER))))
    }

    @Test
    fun counter_adjustUpDown_flooredAtZero() {
        val state = reduce(
            ffaConfig(),
            listOf(e.addCounter(P1), e.adjustCounter(P1, 2), e.adjustCounter(P1, -5), e.adjustCounter(P1, 1)),
        )

        assertEquals(1, state.countersOf(P1).single().value)
    }

    @Test
    fun counter_adjustSaturates() {
        val state = reduce(
            ffaConfig(),
            listOf(e.addCounter(P1), e.adjustCounter(P1, Int.MAX_VALUE), e.adjustCounter(P1, 1)),
        )

        assertEquals(Int.MAX_VALUE, state.countersOf(P1).single().value)
    }

    @Test
    fun counter_adjustUnknownCounterOrPlayerIgnored() {
        val config = ffaConfig()
        val base = listOf(e.addCounter(P1))
        val expected = reduce(config, base)

        assertEquals(expected, reduce(config, base + e.adjustCounter(P1, 1, UNKNOWN_COUNTER)))
        assertEquals(expected, reduce(config, base + e.adjustCounter(P2, 1)))
        assertEquals(expected, reduce(config, base + e.adjustCounter(UNKNOWN_PLAYER, 1)))
    }

    @Test
    fun counter_removed() {
        val gold = CounterId("gold")
        val state = reduce(
            ffaConfig(),
            listOf(e.addCounter(P1), e.addCounter(P1, gold, "Gold"), e.removeCounter(P1)),
        )

        assertEquals(listOf(Counter(gold, "Gold", 0)), state.countersOf(P1))
    }

    @Test
    fun counter_adjustAfterRemoveIgnored() {
        val state = reduce(ffaConfig(), listOf(e.addCounter(P1), e.removeCounter(P1), e.adjustCounter(P1, 3)))

        assertEquals(emptyList(), state.countersOf(P1))
    }

    @Test
    fun counter_reAddAfterRemoveStartsAtZeroAtTheEnd() {
        val gold = CounterId("gold")
        val state = reduce(
            ffaConfig(),
            listOf(
                e.addCounter(P1),
                e.adjustCounter(P1, 4),
                e.addCounter(P1, gold, "Gold"),
                e.removeCounter(P1),
                e.addCounter(P1),
            ),
        )

        assertEquals(listOf(Counter(gold, "Gold", 0), Counter(XP, "XP", 0)), state.countersOf(P1))
    }

    @Test
    fun counter_removeUnknownCounterOrPlayerIgnored() {
        val config = ffaConfig()
        val base = listOf(e.addCounter(P1))
        val expected = reduce(config, base)

        assertEquals(expected, reduce(config, base + e.removeCounter(P1, UNKNOWN_COUNTER)))
        assertEquals(expected, reduce(config, base + e.removeCounter(UNKNOWN_PLAYER)))
    }

    @Test
    fun countersOf_unknownPlayerIsEmpty() {
        assertEquals(emptyList(), reduce(ffaConfig(), emptyList()).countersOf(UNKNOWN_PLAYER))
    }

    // endregion

    // region Target score

    @Test
    fun target_changed() {
        assertEquals(12, reduce(ffaConfig(), listOf(e.target(12))).targetScore)
        assertEquals(3, reduce(ffaConfig(), listOf(e.target(12), e.target(3))).targetScore)
        assertEquals(1, reduce(ffaConfig(), listOf(e.target(1))).targetScore)
    }

    @Test
    fun target_belowOneIgnored() {
        val state = reduce(ffaConfig(), listOf(e.target(10), e.target(0), e.target(-3)))

        assertEquals(10, state.targetScore)
    }

    // endregion

    // region Game ended

    @Test
    fun gameEnded_withWinner() {
        val state = reduce(ffaConfig(), listOf(e.points(S2, 8), e.end(S2)))

        assertTrue(state.isEnded)
        assertEquals(S2, state.winner)
        assertEquals(8, state.pointsOf(S2))
    }

    @Test
    fun gameEnded_withoutWinner() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, 2), e.end(null)))

        assertTrue(state.isEnded)
        assertNull(state.winner)
        assertEquals(2, state.pointsOf(S1))
    }

    @Test
    fun gameEnded_teamWinner() {
        val state = reduce(teamConfig(), listOf(e.points(TEAM_B, 11), e.end(TEAM_B)))

        assertTrue(state.isEnded)
        assertEquals(TEAM_B, state.winner)
    }

    @Test
    fun gameEnded_unknownWinnerIgnored() {
        val state = reduce(ffaConfig(), listOf(e.end(UNKNOWN_SIDE), e.points(S1, 1)))

        assertFalse(state.isEnded)
        assertNull(state.winner)
        assertEquals(1, state.pointsOf(S1))
    }

    @Test
    fun eventsAfterGameEndedIgnored() {
        val config = ffaConfig()
        val ended = listOf(e.points(S1, 3), e.addCounter(P1), e.end(S1))
        val after = listOf(
            e.points(S1, 5),
            e.addCounter(P2),
            e.adjustCounter(P1, 2),
            e.removeCounter(P1),
            e.target(20),
            e.end(S2),
        )

        assertEquals(reduce(config, ended), reduce(config, ended + after))
        assertEquals(S1, reduce(config, ended + after).winner)
    }

    // endregion

    @Test
    fun reduceIsPure_sameInputSameOutput() {
        val config = ffaConfig(playerCount = 3)
        val events = listOf(e.points(S1, 2), e.addCounter(P3), e.adjustCounter(P3, 1), e.target(5))

        assertEquals(reduce(config, events), reduce(config, events))
    }

    @Test
    fun fullGame_mixedEvents() {
        val config = ffaConfig(playerCount = 3, initialCounters = mapOf(P2 to listOf(Counter(XP, "XP"))))
        val state = reduce(
            config,
            listOf(
                e.points(S1, 2),
                e.points(S2, 4),
                e.adjustCounter(P2, 3),
                e.addCounter(P3, CounterId("gold"), "Gold"),
                e.points(S3, -1),
                e.target(6),
                e.points(S2, 2),
                e.end(S2),
            ),
        )

        assertEquals(mapOf(S1 to 2, S2 to 6, S3 to 0), state.points)
        assertEquals(listOf(Counter(XP, "XP", 3)), state.countersOf(P2))
        assertEquals(listOf(Counter(CounterId("gold"), "Gold", 0)), state.countersOf(P3))
        assertEquals(6, state.targetScore)
        assertEquals(listOf(S2), sidesAtTarget(state))
        assertTrue(state.isEnded)
        assertEquals(S2, state.winner)
    }
}
