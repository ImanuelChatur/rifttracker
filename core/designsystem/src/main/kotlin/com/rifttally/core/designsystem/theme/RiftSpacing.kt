package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Feature modules use these instead of literal dp values. */
@Immutable
public data class RiftSpacing(
    public val none: Dp = 0.dp,
    public val xxs: Dp = 2.dp,
    public val xs: Dp = 4.dp,
    public val sm: Dp = 8.dp,
    public val md: Dp = 12.dp,
    public val lg: Dp = 16.dp,
    public val xl: Dp = 24.dp,
    public val xxl: Dp = 32.dp,
    public val xxxl: Dp = 48.dp,
)
