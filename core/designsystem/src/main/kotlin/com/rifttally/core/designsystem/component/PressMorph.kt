package com.rifttally.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled

/**
 * Press morph (DESIGN.md §4, house rule modeled on M3 Expressive buttons): a pill whose corners tighten
 * while pressed, on the expressive fast spatial spring. Pass the same [interactionSource] to the
 * pressable component. Never use on tracker tap zones (they must feel instant).
 */
@Composable
public fun rememberPressMorphShape(interactionSource: InteractionSource): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val target = if (pressed) PRESSED_PERCENT else FULL_PERCENT
    val spec = if (reduceMotionEnabled()) RiftTheme.motion.none() else RiftTheme.motion.expressive.fastSpatial<Float>()
    val percent by animateFloatAsState(targetValue = target, animationSpec = spec, label = "pressMorph")
    return RoundedCornerShape(percent.coerceIn(0f, FULL_PERCENT).toInt())
}

private const val FULL_PERCENT = 50f

/** ≈ 16dp corners on a 56dp button. */
private const val PRESSED_PERCENT = 29f
