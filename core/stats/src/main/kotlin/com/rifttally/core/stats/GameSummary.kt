package com.rifttally.core.stats

import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.SideId
import kotlin.time.Duration

/**
 * Per-game stats, see [summarize].
 *
 * @property duration `endedAt − createdAt`, never negative.
 * @property sides final scores in config order.
 * @property winner the winning side, or `null` for none.
 * @property timeline scores at the start (all 0, at `createdAt`) and after each points change, for a chart.
 */
public data class GameSummary(
    val gameId: String,
    val format: GameFormat,
    val targetScore: Int,
    val createdAt: Long,
    val endedAt: Long,
    val duration: Duration,
    val sides: List<SideResult>,
    val winner: SideId?,
    val timeline: List<TimelinePoint>,
)
