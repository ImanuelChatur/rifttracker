package com.rifttally.core.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Test-only placeholder "Legend art": generated gradients, never real art. [Bright] is the worst case
 * for the ring and the text next to it.
 */
internal enum class TestPortrait(private val colors: IntArray) {
    Warm(intArrayOf(0xFFFFD54F.toInt(), 0xFFFF7043.toInt(), 0xFF8E24AA.toInt())),
    Cool(intArrayOf(0xFF80DEEA.toInt(), 0xFF42A5F5.toInt(), 0xFF283593.toInt())),
    Bright(intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF59D.toInt(), 0xFFA5D6A7.toInt())),
    ;

    /** A square gradient bitmap, as the share helper hands portraits to the card. */
    fun image(size: Int = SIZE): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors, null, Shader.TileMode.CLAMP)
        Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bitmap.asImageBitmap()
    }

    /** The URI the card looks the portrait up by. */
    val uri: String get() = "file:///test/${name.lowercase()}.png"

    private companion object {
        const val SIZE = 128
    }
}
