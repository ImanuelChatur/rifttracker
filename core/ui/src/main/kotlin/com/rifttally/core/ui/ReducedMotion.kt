package com.rifttally.core.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * `true` when the system "Remove animations" setting is on (animator duration scale 0). Looping or
 * decorative motion (e.g. the target-reached pulse) should then be static.
 */
@Composable
public fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
