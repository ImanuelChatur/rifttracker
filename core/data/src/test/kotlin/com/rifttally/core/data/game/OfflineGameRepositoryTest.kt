package com.rifttally.core.data.game

import app.cash.turbine.test
import com.rifttally.core.data.timer.NewGameTimer
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.database.model.GameTimerEntity
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
        val id = repository.createGame(config, emptyList())

        val record = assertNotNull(repository.observeGame(id).first())
        assertEquals(config, record.config)
        assertEquals(emptyList(), record.events)
        assertNull(record.endedAt)
    }

    @Test
    fun appendEvent_persistsInOrder_andRoundTripsPayload() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        val events = listOf(points("e1"), points("e2", SIDE_2, -1), GameEvent.TargetScoreChanged("e3", 11, 10))

        events.forEach { repository.appendEvent(id, it) }

        assertEquals(events, repository.observeGame(id).first()?.events)
    }

    @Test
    fun observeGame_emitsOnEveryAppend() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        repository.observeGame(id).test {
            assertEquals(0, awaitItem()?.events?.size)
            repository.appendEvent(id, points("e1"))
            assertEquals(1, awaitItem()?.events?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undo_removesNewestEvent_only() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        repository.appendEvent(id, points("e1"))
        repository.appendEvent(id, points("e2"))

        repository.undoLastEvent(id)

        assertEquals(listOf("e1"), repository.observeGame(id).first()?.events?.map { it.id })
    }

    @Test
    fun undo_onEmptyLog_isNoOp() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())

        repository.undoLastEvent(id)

        assertEquals(emptyList(), repository.observeGame(id).first()?.events)
    }

    @Test
    fun gameEnded_endsGame_andUndoReopensIt() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        repository.appendEvent(id, points("e1"))
        repository.appendEvent(id, GameEvent.GameEnded("e2", 20, winner = SIDE_1))

        assertNull(repository.activeGame.first())
        assertEquals(20L, repository.observeGame(id).first()?.endedAt)

        repository.undoLastEvent(id)

        assertEquals(id, repository.activeGame.first()?.id)
        assertNull(repository.observeGame(id).first()?.endedAt)
    }

    @Test
    fun restartGame_endsOldGame_andNewOneIsActive() = runTest(dispatcher) {
        val old = repository.createGame(config, emptyList())
        repository.appendEvent(old, points("e1"))

        val new = repository.restartGame(old, GameEvent.GameEnded("end", 30, winner = null), config, emptyList())

        val oldRecord = assertNotNull(repository.observeGame(old).first())
        assertEquals(30L, oldRecord.endedAt)
        assertEquals(listOf("e1", "end"), oldRecord.events.map { it.id })
        assertEquals(new, repository.activeGame.first()?.id)
        assertEquals(config, repository.observeGame(new).first()?.config)
    }

    @Test
    fun createGame_withOpeningEvents_storesThemInOrder() = runTest(dispatcher) {
        val pick = GameEvent.FirstPlayerChosen("pick", 5, PlayerId("p2"))

        val id = repository.createGame(config, listOf(pick, points("e1")))

        assertEquals(listOf(pick, points("e1")), repository.observeGame(id).first()?.events)
        assertEquals(listOf(1, 2), eventDao.events.value.map { it.seq })
        // The log continues after them.
        repository.appendEvent(id, points("e2"))
        assertEquals(listOf("pick", "e1", "e2"), repository.observeGame(id).first()?.events?.map { it.id })
    }

    @Test
    fun restartGame_withOpeningEvents_startsTheNewGameWithThem() = runTest(dispatcher) {
        val old = repository.createGame(config, emptyList())
        val pick = GameEvent.FirstPlayerChosen("pick", 31, PlayerId("p1"))

        val new = repository.restartGame(old, GameEvent.GameEnded("end", 30, winner = null), config, listOf(pick))

        assertEquals(listOf(pick), repository.observeGame(new).first()?.events)
        assertEquals(listOf("end"), repository.observeGame(old).first()?.events?.map { it.id })
    }

    @Test
    fun createGame_withFreshTimer_storesItWithTheGame_fullLengthPausedAndClamped() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList(), NewGameTimer.Fresh(50))
        val long = repository.createGame(config, emptyList(), NewGameTimer.Fresh(500))
        val none = repository.createGame(config, emptyList())

        assertEquals(GameTimerEntity(id.value, durationMs = 3_000_000), gameDao.timers.value[id.value])
        assertEquals(120 * 60_000L, gameDao.timers.value[long.value]?.durationMs)
        assertNull(gameDao.timers.value[none.value])
    }

    @Test
    fun createGame_copyOf_carriesTheTimerOverUnchanged() = runTest(dispatcher) {
        val old = repository.createGame(config, emptyList(), NewGameTimer.Fresh(50))
        val running = GameTimerEntity(old.value, durationMs = 3_000_000, elapsedBeforeMs = 1_234, runningSince = 777)
        gameDao.upsertTimer(running)
        val withoutTimer = repository.createGame(config, emptyList())

        val new = repository.createGame(config, emptyList(), NewGameTimer.CopyOf(old))
        val fromNone = repository.createGame(config, emptyList(), NewGameTimer.CopyOf(withoutTimer))

        assertEquals(running.copy(gameId = new.value), gameDao.timers.value[new.value])
        assertNull(gameDao.timers.value[fromNone.value])
    }

    @Test
    fun restartGame_carryOverTimer_copiesIt_otherwiseNot() = runTest(dispatcher) {
        val old = repository.createGame(config, emptyList(), NewGameTimer.Fresh(30))
        val end = GameEvent.GameEnded("end", 30, winner = null)

        val carried = repository.restartGame(old, end, config, emptyList(), carryOverTimer = true)
        val plain = repository.restartGame(carried, end.copy(id = "end2"), config, emptyList())

        assertEquals(1_800_000L, gameDao.timers.value[carried.value]?.durationMs)
        assertNull(gameDao.timers.value[plain.value])
    }

    @Test
    fun restartGame_onEndedGame_doesNotEndItTwice() = runTest(dispatcher) {
        val old = repository.createGame(config, emptyList())
        repository.appendEvent(old, GameEvent.GameEnded("end1", 20, winner = SIDE_1))

        repository.restartGame(old, GameEvent.GameEnded("end2", 30, winner = null), config, emptyList())

        val oldRecord = assertNotNull(repository.observeGame(old).first())
        assertEquals(listOf("end1"), oldRecord.events.map { it.id })
        assertEquals(20L, oldRecord.endedAt)
    }

    @Test
    fun activeGame_isMostRecentlyPlayedUnendedGame() = runTest(dispatcher) {
        val first = repository.createGame(config, emptyList())
        clock.advanceBy(1.seconds)
        val second = repository.createGame(config, emptyList())
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
        val id = repository.createGame(config, emptyList())
        repository.appendEvent(id, points("e1"))
        eventDao.insert(GameEventEntity(id = "bad", gameId = id.value, seq = 99, timestamp = 0, payload = "{nope"))

        assertEquals(listOf("e1"), repository.observeGame(id).first()?.events?.map { it.id })
    }

    @Test
    fun corruptConfig_yieldsNull() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        gameDao.games.value = gameDao.games.value.mapValues { (_, game) -> game.copy(configJson = "{}") }

        assertNull(repository.observeGame(id).first())
    }

    @Test
    fun recentGames_areEndedGames_newestFirst_withEvents() = runTest(dispatcher) {
        val first = repository.createGame(config, emptyList())
        val second = repository.createGame(config, emptyList())
        repository.createGame(config, emptyList()) // still active: not listed
        repository.appendEvent(first, points("e1"))
        repository.appendEvent(first, GameEvent.GameEnded("e2", 100, SIDE_1))
        repository.appendEvent(second, GameEvent.GameEnded("e3", 200, null))

        val recent = repository.recentGames(limit = 10).first()

        assertEquals(listOf(second, first), recent.map { it.id })
        assertEquals(2, recent[1].events.size)
        assertEquals(listOf(second), repository.recentGames(limit = 1).first().map { it.id })
    }

    @Test
    fun endedGames_areAllEndedGames_newestFirst_withTheirOwnEvents() = runTest(dispatcher) {
        val first = repository.createGame(config, emptyList())
        val second = repository.createGame(config, emptyList())
        val active = repository.createGame(config, emptyList())
        repository.appendEvent(first, points("e1"))
        repository.appendEvent(first, GameEvent.GameEnded("e2", 100, SIDE_1))
        repository.appendEvent(active, points("e3"))
        repository.appendEvent(second, points("e4", SIDE_2))
        repository.appendEvent(second, GameEvent.GameEnded("e5", 200, SIDE_2))

        val ended = repository.endedGames().first()

        assertEquals(listOf(second, first), ended.map { it.id })
        assertEquals(listOf("e4", "e5"), ended[0].events.map { it.id })
        assertEquals(listOf("e1", "e2"), ended[1].events.map { it.id })
        assertEquals(listOf(200L, 100L), ended.map { it.endedAt })
    }

    @Test
    fun endedGames_skipsGamesWithUnreadableConfig() = runTest(dispatcher) {
        val good = repository.createGame(config, emptyList())
        val bad = repository.createGame(config, emptyList())
        repository.appendEvent(good, GameEvent.GameEnded("e1", 100, null))
        repository.appendEvent(bad, GameEvent.GameEnded("e2", 200, null))
        gameDao.games.value = gameDao.games.value.mapValues { (id, game) ->
            if (id == bad.value) game.copy(configJson = "{nope") else game
        }

        assertEquals(listOf(good), repository.endedGames().first().map { it.id })
    }

    @Test
    fun endedGames_emitsWhenAGameEnds() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        repository.endedGames().test {
            assertEquals(emptyList(), awaitItem())
            repository.appendEvent(id, GameEvent.GameEnded("e1", 100, SIDE_1))
            assertEquals(listOf(id), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteGame_removesGameAndEvents_only() = runTest(dispatcher) {
        val keep = repository.createGame(config, emptyList())
        val drop = repository.createGame(config, emptyList())
        repository.appendEvent(keep, points("e1"))
        repository.appendEvent(drop, points("e2"))
        repository.appendEvent(drop, GameEvent.GameEnded("e3", 100, SIDE_1))

        repository.deleteGame(drop)
        repository.deleteGame(GameId("missing"))

        assertNull(repository.observeGame(drop).first())
        assertEquals(emptyList(), repository.endedGames().first())
        assertEquals(listOf("e1"), eventDao.events.value.map { it.id })
        assertEquals(keep, repository.activeGame.first()?.id)
    }

    @Test
    fun restoreGame_bringsBackTheDeletedGame_exactly() = runTest(dispatcher) {
        val id = repository.createGame(config, emptyList())
        repository.appendEvent(id, points("e1"))
        repository.appendEvent(id, points("e2", SIDE_2, delta = 3))
        repository.appendEvent(id, GameEvent.GameEnded("e3", 100, SIDE_2))
        val record = assertNotNull(repository.observeGame(id).first())

        repository.deleteGame(id)
        repository.restoreGame(record)

        assertEquals(record, repository.observeGame(id).first())
        assertEquals(listOf(record), repository.endedGames().first())
        // Restoring a game that still exists does nothing (no duplicate).
        repository.restoreGame(record)
        assertEquals(3, eventDao.events.value.size)
    }

    private companion object {
        val SIDE_1 = SideId("s1")
        val SIDE_2 = SideId("s2")
    }
}
