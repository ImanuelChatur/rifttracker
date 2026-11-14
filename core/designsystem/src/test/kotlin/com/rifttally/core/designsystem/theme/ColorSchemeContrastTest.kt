package com.rifttally.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemeContrastTest {

    @Test
    fun darkScheme_contentColorsPassWcagAA() = assertSchemePassesAA("dark", DarkColorScheme)

    @Test
    fun lightScheme_contentColorsPassWcagAA() = assertSchemePassesAA("light", LightColorScheme)

    @Test
    fun portraitScore_onScrimOverWhiteImage_passesWcagAA() {
        val colors = RiftPortraitColors()
        listOf(Color.White, Color(0xFFF0E442), Color(0xFF56B4E9)).forEach { image ->
            val scrimmed = colors.scrimCenter.compositeOver(image)
            val ratio = contrastRatio(colors.onScrim, scrimmed)
            assertTrue("score on scrim over $image: $ratio < 4.5", ratio >= AA)
        }
    }

    private fun assertSchemePassesAA(name: String, scheme: ColorScheme) {
        val pairs = listOf(
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
            "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "onError/error" to (scheme.onError to scheme.error),
            "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "onBackground/background" to (scheme.onBackground to scheme.background),
            "onSurface/surface" to (scheme.onSurface to scheme.surface),
            "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
            "onSurfaceVariant/surfaceContainerHighest" to
                (scheme.onSurfaceVariant to scheme.surfaceContainerHighest),
            "primary/surface" to (scheme.primary to scheme.surface),
        )
        pairs.forEach { (label, colors) ->
            val ratio = contrastRatio(colors.first, colors.second)
            assertTrue("$name $label: $ratio < 4.5", ratio >= AA)
        }
    }

    private companion object {
        const val AA = 4.5
    }
}
