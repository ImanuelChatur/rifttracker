package com.rifttally.core.stats

import com.rifttally.core.model.SideId

/** Every side's score at [timestamp] (epoch millis): at the start, or right after one points change. */
public data class TimelinePoint(val timestamp: Long, val scores: Map<SideId, Int>)
