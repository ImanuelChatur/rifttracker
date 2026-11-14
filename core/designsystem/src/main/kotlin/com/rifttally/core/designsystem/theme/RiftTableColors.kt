package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors for the table screen (tracker panels, control bar) and other places Legend art is shown.
 *
 * Art is arbitrary, so anything drawn over it ([onArt], [artPill], the scrim) is the same in light and
 * dark: light text over a bottom-to-top dark scrim. [onArt] on [scrimMiddle] composited over pure white
 * still passes WCAG AA for large text (≥ 3:1); the score also gets [artTextShadow].
 */
@Immutable
public data class RiftTableColors(
    /** Scrim at the top of a panel (local orientation): light, so the art shows. */
    public val scrimTop: Color = Color(0x40000000),
    /** Scrim behind the score (panel center). */
    public val scrimMiddle: Color = Color(0x99000000),
    /** Scrim at the bottom, behind the counters and name. */
    public val scrimBottom: Color = Color(0xE0000000),
    public val onArt: Color = Color(0xFFFFFFFF),
    public val artTextShadow: Color = Color(0xCC000000),
    /** Translucent pill behind counters drawn over art. */
    public val artPill: Color = Color(0x8C000000),
    /** Gold edge pulse when a side reaches the target. */
    public val targetGlow: Color = Color(0xFFFFC94A),
    public val onTargetGlow: Color = Color(0xFF000000),
    /** Floating control bar: translucent surface between the panels. */
    public val controlBar: Color,
    public val onControlBar: Color,
    /** Panel base under the art (or under the color fallback when there is no art). */
    public val panelBase: Color,
    /** Text on the color fallback (no art). */
    public val onPanelBase: Color,
    /** Translucent pill behind counters on the color fallback. */
    public val panelPill: Color,
)

internal val DarkTableColors: RiftTableColors = RiftTableColors(
    controlBar = Color(0xD91A1D23),
    onControlBar = DarkOnSurface,
    panelBase = DarkSurfaceContainerLow,
    onPanelBase = DarkOnSurface,
    panelPill = Color(0x66000000),
)

internal val LightTableColors: RiftTableColors = RiftTableColors(
    controlBar = Color(0xE6FFFFFF),
    onControlBar = LightOnSurface,
    panelBase = LightSurfaceContainerLowest,
    onPanelBase = LightOnSurface,
    panelPill = Color(0x14000000),
)
