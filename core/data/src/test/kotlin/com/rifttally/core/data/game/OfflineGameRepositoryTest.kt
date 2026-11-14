package com.rifttally.core.data.game

import app.cash.turbine.test
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.TestClock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalTime::class)
class OfflineGameRepositoryTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()
    private val gameDao = FakeGameDao()
    private val eventDao = FakeGameEventDao(gameDao)
    private var nextId = 0
    private val repository = OfflineGameRepository(
        gameDao = gameDao,
        eventDao = eventDao,
        clock = clock,
        idGenerator = IdGenerator { "game${++nextId}" },
        ioDispatcher = dispatcher,
    )

    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(SIDE_1, listOf(PlayerId("p1"))), Side(SIDE_2, listOf(PlayerId("p2")))),
        players = listOf(
            Player(PlayerId("p1"), "Ana", "orange", legendId = "jinx"),
            Player(PlayerId("p2"), "Ben", "sky"),
        ),
    )

    private fun points(id: String, side: SideId = SIDE_1, delta: Int = 1, timestamp: Long = 10) =
        GameEvent.PointsAdjusted(id = id, timestamp = timestamp, sideId = side, delta = delta)

    @Test
    fun createGame_storesConfig_withEmptyLog() = runTest(dispatcher) {
        val id = repository.createGame(config)

        val record = assertNotNull(repository.observeGame(id).first())
        assertEquals(config, record.config)
        assertEquals(emptyList(), record.events)
        assertNull(record.endedAt)
    }

    @Test
    fun appendEvent_persistsInOrder_andRoundTripsPayload() = runTest(dispatcher) {
        val id = repository.createGame(config)
        val events = listOf(points("e1"), points("e2", SIDE_2, -1), GameEvent.TargetScoreChanged("e3", 11, 10))

        events.forEach { repository.appendEvent(id, it) }

        assertEquals(events, repository.observeGame(id).first()?.events)
    }

    @Test
    fun observeGame_emitsOnEveryAppend() = runTest(dispatcher) {
        val id = repository.createGame(config)
        repository.observeGame(id).test {
            assertEquals(0, awaitItem()?.events?.size)
            repository.appendEvent(id, points("e1"))
            assertEquals(1, awaitItem()?.events?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undo_removesNewestEvent_only() = runTest(dispatcher) {
        val id = repository.createGame(config)
        repository.appendEvent(id, points("e1"))
        repository.appendEvent(id, points("e2"))

        repository.undoLastEvent(id)

        assertEquals(listOf("e1"), repository.observeGame(id).first()?.events?.map { it.id })
    }

    @Test
    fun undo_onEmptyLog_isNoOp() = runTest(dispatcher) {
        val id = repository.createGame(config)

        repository.undoLastEvent(id)

        assertEquals(emptyList(), repository.observeGame(id).first()?.events)
    }

    @Test
    fun gameEnded_endsGame_andUndoReopensIt() = runTest(dispatcher) {
        val id = repository.createGame(config)
        repository.appendEvent(id, points("e1"))
        repository.appendEvent(id, GameEvent.GameEnded("e2", 20, winner = SIDE_1))

        assertNull(repository.activeGame.first())
        assertEquals(20L, repository.observeGame(id).first()?.endedAt)

        repository.undoLastEvent(id)

        assertEquals(id, repository.activeGame.first()?.id)
        assertNull(repository.observeGame(id).first()?.endedAt)
    }

    @Test
    fun activeGame_isMostRecentlyPlayedUnendedGame() = runTest(dispatcher) {
        val first = repository.createGame(config)
        clock.advanceBy(1.seconds)
        val second = repository.createGame(config)
        assertEquals(second, repository.activeGame.first()?.id)

        // Playing the older game makes it the one to resume.
        repository.appendEvent(first, points("e1", timestamp = 5_000))
        assertEquals(first, repository.activeGame.first()?.id)
    }

    @Test
    fun activeGame_isNull_withoutGames() = runTest(dispatcher) {
        assertNull(repository.activeGame.first())
    }

    @Test
    fun observeGame_unknownId_isNull() = runTest(dispatcher) {
        assertNull(repository.observeGame(GameId("missing")).first())
    }

    @Test
    fun corruptEventPayload_isSkipped() = runTest(dispatcher) {
        val id = repository.createGame(config)
        repository.appendEvent(id, points("e1"))
        eventDao.insert(GameEventEntity(id = "bad", gameId = id.value, seq = 99, timestamp = 0, payload = "{nope"))

        assertEquals(listOf("e1"), repository.observeGame(id).first()?.events?.map { it.id })
    }

    @Test
    fun corruptConfig_yieldsNull() = runTest(dispatcher) {
        val id = repository.createGame(config)
        gameDao.games.value = gameDao.games.value.mapValues { (_, game) -> game.copy(configJson = "{}") }

        assertNull(repository.observeGame(id).first())
    }

    private companion object {
        val SIDE_1 = SideId("s1")
        val SIDE_2 = SideId("s2")
    }
}
