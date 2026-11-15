package com.rifttally.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * Motion tokens (DESIGN.md §7): springs, not durations. Mirrors the Material 3 motion schemes, which
 * are internal in material3 1.4.0. When 1.5.0 is stable, back these with `MaterialTheme.motionScheme`.
 *
 * - [expressive]: hero interactions on hero screens (score change, target reached, Home hero, picker).
 * - [standard]: utilitarian UI and recurring interactions (Settings, History, Stats, dialogs, lists).
 */
@Immutable
public class RiftMotion internal constructor() {
    public val expressive: RiftMotionScheme = RiftMotionScheme(
        fastSpatial = SpringValues(dampingRatio = 0.6f, stiffness = 800f),
        defaultSpatial = SpringValues(dampingRatio = 0.8f, stiffness = 380f),
        slowSpatial = SpringValues(dampingRatio = 0.8f, stiffness = 200f),
    )

    public val standard: RiftMotionScheme = RiftMotionScheme(
        fastSpatial = SpringValues(dampingRatio = 0.9f, stiffness = 1400f),
        defaultSpatial = SpringValues(dampingRatio = 0.9f, stiffness = 700f),
        slowSpatial = SpringValues(dampingRatio = 0.9f, stiffness = 300f),
    )

    /** Delay between consecutive tracker panels' entry fade (≤ 40ms, DESIGN.md §7). */
    public val enterStaggerMillis: Long = 40L

    /**
     * One step of the first-player reveal's highlight hopping between panels (it slows down toward the
     * end). Discrete hops, not a transition; each hop's fade still uses an effects spring.
     */
    public val revealStepMillis: Long = 90L

    /**
     * The target-reached edge pulse: the one allowed infinite tween (DESIGN.md §7). Use a static value
     * instead when [reduceMotionEnabled] is true.
     */
    public fun <T> pulse(): InfiniteRepeatableSpec<T> =
        infiniteRepeatable(animation = tween(PULSE_HALF_PERIOD_MILLIS), repeatMode = RepeatMode.Reverse)

    /** No animation (reduce-motion path). */
    public fun <T> none(): FiniteAnimationSpec<T> = snap()

    private companion object {
        const val PULSE_HALF_PERIOD_MILLIS = 900
    }
}

/** Damping ratio and stiffness of one spring token. */
@Immutable
public data class SpringValues(public val dampingRatio: Float, public val stiffness: Float)

/**
 * Six springs: spatial (position, size, scale, shape; may overshoot) and effects (color, alpha; never
 * bounce), each fast / default / slow. Effects springs are the same in both schemes.
 */
@Immutable
public class RiftMotionScheme internal constructor(
    private val fastSpatial: SpringValues,
    private val defaultSpatial: SpringValues,
    private val slowSpatial: SpringValues,
) {
    public fun <T> fastSpatial(): FiniteAnimationSpec<T> = fastSpatial.spec()

    public fun <T> defaultSpatial(): FiniteAnimationSpec<T> = defaultSpatial.spec()

    public fun <T> slowSpatial(): FiniteAnimationSpec<T> = slowSpatial.spec()

    public fun <T> fastEffects(): FiniteAnimationSpec<T> = FAST_EFFECTS.spec()

    public fun <T> defaultEffects(): FiniteAnimationSpec<T> = DEFAULT_EFFECTS.spec()

    public fun <T> slowEffects(): FiniteAnimationSpec<T> = SLOW_EFFECTS.spec()

    private fun <T> SpringValues.spec(): FiniteAnimationSpec<T> = spring(dampingRatio, stiffness)

    private companion object {
        val FAST_EFFECTS = SpringValues(dampingRatio = 1f, stiffness = 3800f)
        val DEFAULT_EFFECTS = SpringValues(dampingRatio = 1f, stiffness = 1600f)
        val SLOW_EFFECTS = SpringValues(dampingRatio = 1f, stiffness = 800f)
    }
}
