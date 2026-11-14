package com.rifttally.core.game

import com.rifttally.core.model.SideId

/** Sides whose points are at or above the current target score, in config order. Drives the highlight/banner. */
public fun sidesAtTarget(state: GameState): List<SideId> =
    state.points.filterValues { it >= state.targetScore }.keys.toList()
