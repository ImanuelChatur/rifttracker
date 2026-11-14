package com.rifttally.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Score over time: one line per side in the player's color, one step per points change, with the
 * [targetScore] as a dashed line. Drawn with a plain [Canvas] (no chart library). Screen readers get
 * the final scores instead of the drawing.
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
    val lines = series.map { (PlayerColors.fromId(it.colorId) ?: PlayerColors.Grey).color to it.points }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        Row {
            Box(modifier = Modifier.padding(end = RiftTheme.spacing.sm)) {
                Text(
                    text = maxValue.toString(),
                    style = RiftTheme.typography.labelSmall,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(CHART_ASPECT_RATIO),
                contentAlignment = Alignment.BottomStart,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val inset = strokeWidth.toPx()
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
                    lines.forEach { (color, points) ->
                        if (points.isEmpty()) return@forEach
                        val steps = (points.size - 1).coerceAtLeast(1)
                        val path = Path()
                        points.forEachIndexed { index, value ->
                            val x = inset + (size.width - 2 * inset) * index / steps
                            if (index == 0) path.moveTo(x, y(value)) else path.lineTo(x, y(value))
                        }
                        if (points.size == 1) path.lineTo(size.width - inset, y(points[0]))
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        ) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(RiftTheme.spacing.md)
                            .background((PlayerColors.fromId(s.colorId) ?: PlayerColors.Grey).color, CircleShape),
                    )
                    Text(
                        text = s.name,
                        style = RiftTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = RiftTheme.spacing.xs),
                    )
                }
            }
        }
    }
}

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
