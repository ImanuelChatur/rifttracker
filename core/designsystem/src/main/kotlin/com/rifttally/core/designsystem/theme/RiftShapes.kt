package com.rifttally.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale (Material 3 shape tokens, including the Expressive additions). DESIGN.md §4.
 * Feature code uses these instead of `RoundedCornerShape(n.dp)`.
 */
@Immutable
public class RiftShapes internal constructor() {
    /** 0dp: full-bleed surfaces. */
    public val none: Shape = RectangleShape

    /** 4dp: tiny badges; inner corners of segmented list tiles. */
    public val extraSmall: Shape = RoundedCornerShape(4.dp)

    /** 8dp: chips, inline tags. */
    public val small: Shape = RoundedCornerShape(8.dp)

    /** 12dp: list cards, text fields. */
    public val medium: Shape = RoundedCornerShape(12.dp)

    /** 16dp: standard cards; outer corners of segmented list groups. */
    public val large: Shape = RoundedCornerShape(16.dp)

    /** 20dp: Legend picker cards. */
    public val largeIncreased: Shape = RoundedCornerShape(20.dp)

    /** 28dp: dialogs, bottom sheets, tracker panels. */
    public val extraLarge: Shape = RoundedCornerShape(28.dp)

    /** 32dp: Home hero (Resume) card. */
    public val extraLargeIncreased: Shape = RoundedCornerShape(32.dp)

    /** 48dp: full-screen hero containers. */
    public val extraExtraLarge: Shape = RoundedCornerShape(48.dp)

    /** Pill / circle: buttons, the floating control bar, counter pills, avatars. */
    public val full: Shape = CircleShape

    /**
     * A tile in a segmented list (DESIGN.md §8): [large] outer corners at the ends of the group,
     * [extraSmall] where it meets its neighbors.
     */
    public fun segment(index: Int, count: Int): Shape {
        val outer = 16.dp
        val inner = 4.dp
        val top = if (index == 0) outer else inner
        val bottom = if (index == count - 1) outer else inner
        return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }
}
