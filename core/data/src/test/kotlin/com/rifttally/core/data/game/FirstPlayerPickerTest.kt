package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalTime::class)
class FirstPlayerPickerTest {
    private val settings = FakeUserSettingsRepository()
    private var index = 1
    private val picker = FirstPlayerPicker(
        settingsRepository = settings,
        random = { index },
        eventFactory = GameEventFactory(TestClock(), IdGenerator { "pick" }),
    )

    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(SideId("s1"), listOf(PlayerId("p1"))), Side(SideId("s2"), listOf(PlayerId("p2")))),
        players = listOf(Player(PlayerId("p1"), "Ana", "orange"), Player(PlayerId("p2"), "Ben", "sky")),
    )

    @Test
    fun settingOn_byDefault_opensWithAPick() = runTest {
        val events = picker.openingEvents(config)

        assertEquals(listOf(PlayerId("p2")), events.map { (it as GameEvent.FirstPlayerChosen).playerId })
    }

    @Test
    fun settingOff_opensWithNothing() = runTest {
        settings.setPickFirstPlayer(false)

        assertEquals(emptyList(), picker.openingEvents(config))
    }

    @Test
    fun pick_usesTheRandomSource() {
        index = 0
        assertEquals(PlayerId("p1"), picker.pick(config)?.playerId)
    }

    @Test
    fun noPlayers_noPick() = runTest {
        val empty = config.copy(sides = emptyList(), players = emptyList())

        assertNull(picker.pick(empty))
        assertEquals(emptyList(), picker.openingEvents(empty))
    }
}
