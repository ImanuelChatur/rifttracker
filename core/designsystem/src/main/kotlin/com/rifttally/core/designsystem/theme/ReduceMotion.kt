package com.rifttally.core.designsystem.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `true` when the system "Remove animations" setting is on (animator duration scale 0). Use snap/no-op
 * specs ([RiftMotion.none]) and static states (e.g. a static gold border) then. DESIGN.md §7 (house rule).
 */
@Composable
public fun reduceMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
