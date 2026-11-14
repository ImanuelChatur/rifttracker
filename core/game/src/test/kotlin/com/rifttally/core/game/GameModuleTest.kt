package com.rifttally.core.game

import kotlin.test.Test
import kotlin.test.assertEquals

class GameModuleTest {
    @Test
    fun moduleCompiles() {
        assertEquals("game", GameModule.NAME)
    }
}
