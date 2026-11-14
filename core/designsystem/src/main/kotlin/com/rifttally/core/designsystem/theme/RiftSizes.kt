package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Component size tokens. */
@Immutable
public data class RiftSizes(
    public val minTouchTarget: Dp = 48.dp,
    public val iconSmall: Dp = 18.dp,
    public val icon: Dp = 24.dp,
    /** Faint −/+ glyph in each tap half of a tracker panel. */
    public val tapGlyph: Dp = 40.dp,
    public val avatarTiny: Dp = 28.dp,
    public val avatarSmall: Dp = 40.dp,
    public val avatarMedium: Dp = 72.dp,
    public val avatarLarge: Dp = 160.dp,
    public val borderThin: Dp = 1.dp,
    public val borderThick: Dp = 3.dp,
    /** Player-color edge glow around a tracker panel. */
    public val edgeGlow: Dp = 4.dp,
    /** Height of the floating control bar between the panels. */
    public val controlBarHeight: Dp = 56.dp,
    /** Min height of the primary action on hero screens (DESIGN.md §9). */
    public val heroButtonHeight: Dp = 56.dp,
    /** Height of the art strip on Home's "Resume" card. */
    public val heroArtHeight: Dp = 168.dp,
    /** Max width of a screen's content column on wide windows (DESIGN.md §8: ≈ 640dp, centered). */
    public val listMaxContentWidth: Dp = 640.dp,
)
