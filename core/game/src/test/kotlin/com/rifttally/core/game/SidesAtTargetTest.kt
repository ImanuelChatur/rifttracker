package com.rifttally.core.game

import kotlin.test.Test
import kotlin.test.assertEquals

class SidesAtTargetTest {
    private val e = Events()

    @Test
    fun noEvents_nobodyAtTarget() {
        assertEquals(emptyList(), sidesAtTarget(reduce(ffaConfig(), emptyList())))
    }

    @Test
    fun belowTarget_notIncluded() {
        assertEquals(emptyList(), sidesAtTarget(reduce(ffaConfig(), listOf(e.points(S1, 7)))))
    }

    @Test
    fun exactlyAtTarget_included() {
        assertEquals(listOf(S1), sidesAtTarget(reduce(ffaConfig(), listOf(e.points(S1, 8)))))
    }

    @Test
    fun aboveTarget_included() {
        assertEquals(listOf(S2), sidesAtTarget(reduce(ffaConfig(), listOf(e.points(S2, 10)))))
    }

    @Test
    fun droppingBelowTarget_removesSide() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, 8), e.points(S1, -1)))

        assertEquals(emptyList(), sidesAtTarget(state))
    }

    @Test
    fun multipleSidesAtTarget_inConfigOrder() {
        val state = reduce(
            ffaConfig(playerCount = 4),
            listOf(e.points(S4, 9), e.points(S2, 8), e.points(S3, 7), e.points(S1, 8)),
        )

        assertEquals(listOf(S1, S2, S4), sidesAtTarget(state))
    }

    @Test
    fun targetRaisedMidGame_sidesNoLongerAtTarget() {
        val events = listOf(e.points(S1, 8), e.points(S2, 9))
        assertEquals(listOf(S1, S2), sidesAtTarget(reduce(ffaConfig(), events)))

        val raised = reduce(ffaConfig(), events + e.target(9))
        assertEquals(listOf(S2), sidesAtTarget(raised))

        val raisedMore = reduce(ffaConfig(), events + e.target(12))
        assertEquals(emptyList(), sidesAtTarget(raisedMore))
    }

    @Test
    fun targetLoweredMidGame_sidesNowAtTarget() {
        val events = listOf(e.points(S1, 5), e.points(S2, 3))
        assertEquals(emptyList(), sidesAtTarget(reduce(ffaConfig(), events)))

        assertEquals(listOf(S1), sidesAtTarget(reduce(ffaConfig(), events + e.target(5))))
        assertEquals(listOf(S1, S2), sidesAtTarget(reduce(ffaConfig(), events + e.target(3))))
    }

    @Test
    fun targetChangedThenScoring_usesNewTarget() {
        val state = reduce(ffaConfig(), listOf(e.target(3), e.points(S2, 3)))

        assertEquals(listOf(S2), sidesAtTarget(state))
    }

    @Test
    fun teams_sharedPointsReachTarget() {
        val notYet = reduce(teamConfig(), listOf(e.points(TEAM_A, 8)))
        assertEquals(emptyList(), sidesAtTarget(notYet))

        val reached = reduce(teamConfig(), listOf(e.points(TEAM_A, 8), e.points(TEAM_A, 3)))
        assertEquals(listOf(TEAM_A), sidesAtTarget(reached))
    }

    @Test
    fun stillReportedAfterGameEnded() {
        val state = reduce(ffaConfig(), listOf(e.points(S1, 8), e.end(S1)))

        assertEquals(listOf(S1), sidesAtTarget(state))
    }
}
