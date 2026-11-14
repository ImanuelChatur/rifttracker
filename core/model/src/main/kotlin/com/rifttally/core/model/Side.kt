package com.rifttally.core.model

import kotlinx.serialization.Serializable

/**
 * Whoever owns a point total: a single player normally, or a team of players in 2v2.
 *
 * @property playerIds members of this side, in seat order.
 */
@Serializable
public data class Side(val id: SideId, val playerIds: List<PlayerId>)
