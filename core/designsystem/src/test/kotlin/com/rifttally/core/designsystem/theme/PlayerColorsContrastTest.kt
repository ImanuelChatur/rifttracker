package com.rifttally.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerColorsContrastTest {

    @Test
    fun palette_hasEightUniqueColors() {
        assertEquals(8, PlayerColors.all.size)
        assertEquals(8, PlayerColors.all.map { it.id }.toSet().size)
        assertEquals(8, PlayerColors.all.map { it.color }.toSet().size)
    }

    @Test
    fun onColor_onPlayerColor_passesWcagAA() {
        PlayerColors.all.forEach { pc ->
            val ratio = contrastRatio(pc.onColor, pc.color)
            assertTrue("${pc.id}: onColor contrast $ratio < 4.5", ratio >= AA)
        }
    }

    @Test
    fun playerColor_onDarkBackground_passesWcagAA() {
        PlayerColors.all.forEach { pc ->
            val ratio = contrastRatio(pc.color, RiftDarkBackground)
            assertTrue("${pc.id}: contrast on dark background $ratio < 4.5", ratio >= AA)
        }
    }

    @Test
    fun contrastRatio_blackOnWhite_is21() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.0001)
    }

    @Test
    fun lookup_byIdAndSeat() {
        assertEquals(PlayerColors.Blue, PlayerColors.fromId("blue"))
        assertEquals(null, PlayerColors.fromId("nope"))
        assertEquals(PlayerColors.all[0], PlayerColors.forSeat(PlayerColors.all.size))
        assertEquals(PlayerColors.all.last(), PlayerColors.forSeat(-1))
    }

    private companion object {
        const val AA = 4.5
    }
}
