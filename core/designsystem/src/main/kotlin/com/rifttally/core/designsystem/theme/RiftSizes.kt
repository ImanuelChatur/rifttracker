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
    public val avatarSmall: Dp = 40.dp,
    public val avatarMedium: Dp = 72.dp,
    public val avatarLarge: Dp = 160.dp,
    public val borderThin: Dp = 1.dp,
    public val borderThick: Dp = 3.dp,
)
