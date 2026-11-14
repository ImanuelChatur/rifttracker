package com.rifttally.core.stats

import com.rifttally.core.model.SideId

/** One points change: [delta] (may be negative) added to [sideId] at [timestamp] (epoch millis). */
public data class PointChange(val timestamp: Long, val sideId: SideId, val delta: Int)
