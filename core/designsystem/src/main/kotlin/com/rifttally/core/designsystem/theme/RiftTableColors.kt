package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for the table screen (tracker panels, counters, target glow) and other places Legend art is shown.
 *
 * Art is arbitrary, so anything drawn over it ([onArt], [artPill], the scrim) is the same in light and
 * dark: light text over a bottom-to-top dark scrim. [onArt] on [scrimMiddle] composited over pure white
 * is ≈ 5.7:1 (≥ 4.5:1, DESIGN.md §3), so the score passes on the brightest art; it also gets
 * [artTextShadow]. The scrim fades from light at the top to 80% black at the bottom (DESIGN.md §6).
 */
@Immutable
public data class RiftTableColors(
    /** Scrim at the top of a panel (local orientation): light, so the art shows. */
    public val scrimTop: Color = Color(0x40000000),
    /** Scrim behind the score (panel center). */
    public val scrimMiddle: Color = Color(0x99000000),
    /** Scrim at the bottom, behind the counters and name. */
    public val scrimBottom: Color = Color(0xCC000000),
    public val onArt: Color = Color(0xFFFFFFFF),
    public val artTextShadow: Color = Color(0xCC000000),
    /** Translucent pill behind counters drawn over art. */
    public val artPill: Color = Color(0x8C000000),
    /** Gold edge pulse when a side reaches the target. */
    public val targetGlow: Color = Color(0xFFFFC94A),
    public val onTargetGlow: Color = Color(0xFF000000),
    /** Panel base under the art (or under the color fallback when there is no art). */
    public val panelBase: Color,
    /** Text on the color fallback (no art). */
    public val onPanelBase: Color,
    /** Translucent pill behind counters on the color fallback. */
    public val panelPill: Color,
    /**
     * Round timer text in its last 5 minutes, on the control bar (surfaceContainer): amber, distinct from
     * the error color used for overtime. Dark ≈ 10:1, light ≈ 5.6:1 on the bar (DESIGN.md §3).
     */
    public val timerWarning: Color,
)

internal val DarkTableColors: RiftTableColors = RiftTableColors(
    panelBase = DarkSurfaceContainerLow,
    onPanelBase = DarkOnSurface,
    panelPill = Color(0x66000000),
    timerWarning = Color(0xFFFFB74D),
)

internal val LightTableColors: RiftTableColors = RiftTableColors(
    panelBase = LightSurfaceContainerLowest,
    onPanelBase = LightOnSurface,
    panelPill = Color(0x14000000),
    timerWarning = Color(0xFF8A4B00),
)
