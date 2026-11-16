package com.rifttally.core.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The app's `@IoDispatcher`, for UI helpers that write files (e.g. [rememberResultCardSharer]). `:app`
 * provides it from Hilt at the root, so screens and ViewModels don't pass it along.
 */
public val LocalIoDispatcher: ProvidableCompositionLocal<CoroutineDispatcher> = staticCompositionLocalOf {
    error(
        "LocalIoDispatcher isn't provided: wrap the content in CompositionLocalProvider(LocalIoDispatcher provides ...)",
    )
}
