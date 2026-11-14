package com.rifttally.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 elevation levels (DESIGN.md §6). Elevation is shown mainly by surface tone; a real shadow
 * is only for floating elements (the tracker control bar, banners over content) at [level3].
 */
@Immutable
public class RiftElevation internal constructor() {
    public val level0: Dp = 0.dp
    public val level1: Dp = 1.dp
    public val level2: Dp = 3.dp
    public val level3: Dp = 6.dp
    public val level4: Dp = 8.dp
    public val level5: Dp = 12.dp
}
