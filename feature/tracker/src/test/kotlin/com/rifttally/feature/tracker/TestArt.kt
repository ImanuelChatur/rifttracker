package com.rifttally.feature.tracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import java.io.File

/**
 * Test-only placeholder "Legend art": generated gradients (never real art), written to temp PNG files
 * and handed to Coil as `file://` URIs. [Bright] is the worst case for score readability.
 */
internal enum class TestArt(private val colors: IntArray) {
    Warm(intArrayOf(0xFFFFD54F.toInt(), 0xFFFF7043.toInt(), 0xFF8E24AA.toInt())),
    Cool(intArrayOf(0xFF80DEEA.toInt(), 0xFF42A5F5.toInt(), 0xFF283593.toInt())),
    Bright(intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF59D.toInt(), 0xFFA5D6A7.toInt())),
    ;

    /** A `file://` URI to a [width]×[height] PNG of this gradient with a soft highlight. */
    fun uri(width: Int = WIDE_WIDTH, height: Int = WIDE_HEIGHT): String {
        val file = File.createTempFile("test_art_${name.lowercase()}_${width}x$height", ".png")
        file.deleteOnExit()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = RadialGradient(
            width * HIGHLIGHT_X,
            height * HIGHLIGHT_Y,
            height * HIGHLIGHT_RADIUS,
            0xCCFFFFFF.toInt(),
            0x00FFFFFF,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        // file:///C:/… on Windows, file:///tmp/… elsewhere.
        return "file:///" + file.absolutePath.replace(File.separatorChar, '/').removePrefix("/")
    }

    private companion object {
        const val WIDE_WIDTH = 480
        const val WIDE_HEIGHT = 270
        const val HIGHLIGHT_X = 0.35f
        const val HIGHLIGHT_Y = 0.4f
        const val HIGHLIGHT_RADIUS = 0.6f
        const val PNG_QUALITY = 100
    }
}
