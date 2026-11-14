package com.rifttally.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x relative luminance of an sRGB color (alpha ignored). */
public fun Color.wcagLuminance(): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
}

/** WCAG 2.x contrast ratio between two colors, in the range 1.0..21.0. */
public fun contrastRatio(a: Color, b: Color): Double {
    val la = a.wcagLuminance()
    val lb = b.wcagLuminance()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}
