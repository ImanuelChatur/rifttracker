package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for text drawn over a Legend portrait. Theme-independent: a portrait is an arbitrary image,
 * so the score always uses light text on a dark scrim (darkest at the center, behind the number).
 *
 * [onScrim] on [scrimCenter] composited over pure white still passes WCAG AA (≥ 4.5:1).
 */
@Immutable
public data class RiftPortraitColors(
    public val scrimCenter: Color = Color(0xA6000000),
    public val scrimEdge: Color = Color(0x33000000),
    public val onScrim: Color = Color(0xFFFFFFFF),
    public val textShadow: Color = Color(0xCC000000),
)
