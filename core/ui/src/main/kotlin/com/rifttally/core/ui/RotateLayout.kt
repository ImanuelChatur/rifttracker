package com.rifttally.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

/**
 * Rotates content by [quarterTurns] × 90° clockwise **including its layout**: for odd turns the
 * child is measured with width and height swapped, so a tall slot gets landscape content that fills
 * it after rotation. (Plain `Modifier.rotate` only rotates the drawing and would clip.)
 */
public fun Modifier.rotateLayout(quarterTurns: Int): Modifier {
    val turns = quarterTurns.mod(FULL_TURN)
    if (turns == 0) return this
    val swap = turns % 2 == 1
    return layout { measurable, constraints ->
        val childConstraints = if (swap) constraints.swapped() else constraints
        val placeable = measurable.measure(childConstraints)
        val width = if (swap) placeable.height else placeable.width
        val height = if (swap) placeable.width else placeable.height
        layout(width, height) {
            placeable.placeWithLayer(
                x = (width - placeable.width) / 2,
                y = (height - placeable.height) / 2,
            ) {
                rotationZ = turns * QUARTER_DEGREES
            }
        }
    }
}

private fun Constraints.swapped(): Constraints = Constraints(
    minWidth = minHeight,
    maxWidth = maxHeight,
    minHeight = minWidth,
    maxHeight = maxWidth,
)

private const val FULL_TURN = 4
private const val QUARTER_DEGREES = 90f
