package com.rifttally.feature.history

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.ui.PlayerNameChip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Score over time: one line per side in the player's color (accent, DESIGN.md §3), one step per points
 * change, with the [targetScore] as a dashed line and a dot on each final score. Drawn with a plain
 * [Canvas] (no chart library). The lines draw in from the left on the *standard* slow spatial spring;
 * static with reduce motion (DESIGN.md §7). Screen readers get the final scores instead of the drawing.
 */
@Composable
internal fun ScoreChart(series: ImmutableList<ChartSeriesUiState>, targetScore: Int, modifier: Modifier = Modifier) {
    val maxValue = maxOf(targetScore, series.maxOfOrNull { s -> s.points.maxOrNull() ?: 0 } ?: 0, 1)
    val finalScores = series.map { s ->
        val last = s.points.lastOrNull() ?: 0
        pluralStringResource(R.plurals.feature_history_side_points, last, s.name, last)
    }.joinToString(stringResource(R.string.feature_history_names_separator))
    val description = stringResource(R.string.feature_history_chart_description, finalScores)
    val gridColor = RiftTheme.colorScheme.outlineVariant
    val targetColor = RiftTheme.colorScheme.outline
    val strokeWidth = RiftTheme.sizes.borderThick
    val gridWidth = RiftTheme.sizes.borderThin
    val dash = RiftTheme.spacing.xs
    val dotRadius = RiftTheme.spacing.xs
    val lines = series.map { (PlayerColors.fromId(it.colorId) ?: PlayerColors.Grey).color to it.points }

    val reduceMotion = reduceMotionEnabled()
    val reveal = remember(series) { Animatable(if (reduceMotion) 1f else 0f) }
    val spec = RiftTheme.motion.standard.slowSpatial<Float>()
    LaunchedEffect(reveal, reduceMotion) {
        if (reduceMotion) reveal.snapTo(1f) else reveal.animateTo(1f, spec)
    }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Row(modifier = Modifier.fillMaxWidth().aspectRatio(CHART_ASPECT_RATIO)) {
            Column(
                modifier = Modifier
                    .padding(end = RiftTheme.spacing.sm)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                AxisLabel(maxValue.toString())
                AxisLabel(ZERO)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.BottomStart,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val inset = strokeWidth.toPx() + dotRadius.toPx()
                    val chartHeight = size.height - 2 * inset
                    fun y(value: Int): Float = inset + chartHeight * (1f - value.toFloat() / maxValue)

                    // Baseline (0) and the target score.
                    drawLine(gridColor, Offset(0f, y(0)), Offset(size.width, y(0)), gridWidth.toPx())
                    drawLine(
                        color = targetColor,
                        start = Offset(0f, y(targetScore)),
                        end = Offset(size.width, y(targetScore)),
                        strokeWidth = gridWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
                    )
                    // The spatial spring may overshoot slightly; never reveal past the full width.
                    val progress = reveal.value.coerceIn(0f, 1f)
                    clipRect(right = size.width * progress) {
                        lines.forEach { (color, points) ->
                            if (points.isEmpty()) return@forEach
                            val steps = (points.size - 1).coerceAtLeast(1)
                            val path = Path()
                            var end = Offset.Zero
                            points.forEachIndexed { index, value ->
                                val x = inset + (size.width - 2 * inset) * index / steps
                                end = Offset(x, y(value))
                                if (index == 0) path.moveTo(end.x, end.y) else path.lineTo(end.x, end.y)
                            }
                            if (points.size == 1) {
                                end = Offset(size.width - inset, y(points[0]))
                                path.lineTo(end.x, end.y)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(
                                    width = strokeWidth.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round,
                                ),
                            )
                            drawCircle(color = color, radius = dotRadius.toPx(), center = end)
                        }
                    }
                }
            }
        }
        // Legend: each side's name on a chip in its color (same as the tracker and Home).
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            series.forEach { s ->
                PlayerNameChip(name = s.name, playerColor = PlayerColors.fromId(s.colorId) ?: PlayerColors.Grey)
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        color = RiftTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private const val ZERO = "0"
private const val CHART_ASPECT_RATIO = 2f

internal val previewChart = persistentListOf(
    ChartSeriesUiState("Ana", "orange", persistentListOf(0, 1, 1, 3, 3, 4, 6, 6, 8)),
    ChartSeriesUiState("Ben", "sky", persistentListOf(0, 0, 2, 2, 3, 3, 3, 5, 5)),
)

@PreviewRiftThemes
@Composable
private fun ScoreChartPreview() {
    RiftTheme {
        Surface {
            ScoreChart(series = previewChart, targetScore = 8, modifier = Modifier.padding(RiftTheme.spacing.lg))
        }
    }
}

@PreviewRiftThemes
@Composable
private fun ScoreChartNoChangesPreview() {
    RiftTheme {
        Surface {
            ScoreChart(
                series = persistentListOf(
                    ChartSeriesUiState("Ana", "orange", persistentListOf(0)),
                    ChartSeriesUiState("Ben", "sky", persistentListOf(0)),
                ),
                targetScore = 8,
                modifier = Modifier.padding(RiftTheme.spacing.lg),
            )
        }
    }
}
