package com.rifttally.core.stats

import com.rifttally.core.model.Player
import com.rifttally.core.model.SideId

/** A side's final result in one game. [players] are in seat order (two in 2v2, otherwise one). */
public data class SideResult(val sideId: SideId, val players: List<Player>, val finalScore: Int, val isWinner: Boolean)
