package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * A player's color. [onColor] is the text/icon color to use on top of [color].
 * Every entry passes WCAG AA (≥ 4.5:1) for [onColor] on [color] and for [color] on [RiftDarkBackground].
 *
 * These are bright fill colors: don't draw [color] as text on a light surface (it fails contrast there).
 * In the light theme, show player names on a [color] chip using [onColor] instead.
 */
@Immutable
public data class PlayerColor(public val id: String, public val color: Color, public val onColor: Color)
