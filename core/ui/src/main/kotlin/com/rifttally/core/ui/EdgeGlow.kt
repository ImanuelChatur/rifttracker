package com.rifttally.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * Draws a soft glowing border of [width] in [brush] along [shape], on top of the content: a solid edge
 * plus two fainter, wider strokes that fade inward. Apply after `clip(shape)` so the outer half of each
 * stroke is clipped and the glow sits inside the bounds. [alpha] fades the whole glow (for pulses).
 */
public fun Modifier.edgeGlow(brush: Brush, width: Dp, shape: Shape, alpha: Float = 1f): Modifier = drawWithContent {
    drawContent()
    if (alpha <= 0f) return@drawWithContent
    val outline = shape.createOutline(size, layoutDirection, this)
    val px = width.toPx()
    // Strokes are centered on the outline, so the visible (inner) half is width / 2.
    drawOutline(outline, brush, alpha = alpha * OUTER_ALPHA, style = Stroke(width = px * OUTER_FACTOR))
    drawOutline(outline, brush, alpha = alpha * MIDDLE_ALPHA, style = Stroke(width = px * MIDDLE_FACTOR))
    drawOutline(outline, brush, alpha = alpha, style = Stroke(width = px * SOLID_FACTOR))
}

// Visible widths: solid ≈ 0.75 × width, glow fading out to ≈ 2.5 × width.
private const val SOLID_FACTOR = 1.5f
private const val MIDDLE_FACTOR = 3f
private const val MIDDLE_ALPHA = 0.35f
private const val OUTER_FACTOR = 5f
private const val OUTER_ALPHA = 0.15f
