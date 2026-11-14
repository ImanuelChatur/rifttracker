package com.rifttally.core.ui

import kotlin.test.assertEquals
import org.junit.Test

class AvatarInitialTest {

    @Test
    fun usesLegendName() {
        assertEquals("J", avatarInitial(legendName = "Jinx", fallbackName = "Ana"))
    }

    @Test
    fun uppercases() {
        assertEquals("K", avatarInitial(legendName = "kai'Sa", fallbackName = "Ana"))
    }

    @Test
    fun noLegend_usesPlayerName() {
        assertEquals("A", avatarInitial(legendName = null, fallbackName = "ana"))
    }

    @Test
    fun blankLegend_usesPlayerName() {
        assertEquals("A", avatarInitial(legendName = "  ", fallbackName = "Ana"))
    }

    @Test
    fun skipsLeadingPunctuationAndSpaces() {
        assertEquals("L", avatarInitial(legendName = " \"Lee Sin\"", fallbackName = "Ana"))
        assertEquals("2", avatarInitial(legendName = null, fallbackName = "#2 Player"))
    }

    @Test
    fun nothingUsable_isEmpty() {
        assertEquals("", avatarInitial(legendName = null, fallbackName = ""))
        assertEquals("", avatarInitial(legendName = "!!", fallbackName = "  "))
    }

    @Test
    fun handlesNonAsciiLetters() {
        assertEquals("É", avatarInitial(legendName = null, fallbackName = "élise"))
        assertEquals("𐐀", avatarInitial(legendName = null, fallbackName = "𐐨bc"))
    }
}
