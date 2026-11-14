package com.rifttally.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens. Transitions are springs that mirror the Material 3 motion scheme (fast/default ×
 * spatial/effects; `MaterialTheme.motionScheme` is not public in the Material 3 version we use), so
 * they can be swapped in one place. All of them settle in well under 300ms.
 *
 * The target-reached edge pulse is an ambient loop (springs can't loop), so it keeps a duration; it is
 * static when the system "remove animations" setting is on.
 */
public class RiftMotion internal constructor() {
    /** Small moves, e.g. the score's vertical roll. */
    public fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(SPATIAL_DAMPING, FAST_SPATIAL_STIFFNESS)

    /** Small fades / color changes. */
    public fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, FAST_EFFECTS_STIFFNESS)

    /** Fades of larger elements, e.g. panels entering. */
    public fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, DEFAULT_EFFECTS_STIFFNESS)

    /** Delay between consecutive panels' fade-in. */
    public val enterStaggerMillis: Long = 50L

    /** One half of the gold edge pulse (bright → dim). */
    public val pulseHalfPeriodMillis: Int = 900

    private companion object {
        // Material 3 standard motion scheme spring tokens.
        const val SPATIAL_DAMPING = 0.9f
        const val FAST_SPATIAL_STIFFNESS = 1400f
        const val EFFECTS_DAMPING = 1f
        const val FAST_EFFECTS_STIFFNESS = 3800f
        const val DEFAULT_EFFECTS_STIFFNESS = 1600f
    }
}
