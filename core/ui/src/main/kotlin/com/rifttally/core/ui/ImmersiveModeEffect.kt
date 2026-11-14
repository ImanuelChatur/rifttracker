package com.rifttally.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.WeakHashMap

/**
 * Hides the system bars while this is in the composition and [enabled] is true. A swipe from the edge
 * shows them temporarily. Requests are reference-counted per window, so when two screens overlap
 * (e.g. during a navigation transition) the bars only come back when the last one leaves.
 */
@Composable
public fun ImmersiveModeEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val window = view.context.findActivity()?.window
        if (!enabled || window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        ImmersiveRequests.acquire(window) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            ImmersiveRequests.release(window) { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
}

/** Main-thread only (composition effects), so no synchronization is needed. */
private object ImmersiveRequests {
    private val counts = WeakHashMap<Window, Int>()

    fun acquire(window: Window, onFirst: () -> Unit) {
        val count = counts[window] ?: 0
        counts[window] = count + 1
        // Re-apply on every acquire: harmless, and keeps bars hidden if something else showed them.
        onFirst()
    }

    fun release(window: Window, onLast: () -> Unit) {
        val count = (counts[window] ?: 1) - 1
        if (count <= 0) {
            counts.remove(window)
            onLast()
        } else {
            counts[window] = count
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
