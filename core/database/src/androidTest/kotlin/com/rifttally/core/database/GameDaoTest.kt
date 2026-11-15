package com.rifttally.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameDaoTest {
    private lateinit var db: RiftDatabase
    private lateinit var gameDao: GameDao
    private lateinit var eventDao: GameEventDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RiftDatabase::class.java).build()
        gameDao = db.gameDao()
        eventDao = db.gameEventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun activeGame_isLatestUpdatedGameWithoutEnd() = runTest {
        gameDao.insert(game("old", updatedAt = 10))
        gameDao.insert(game("new", updatedAt = 20))
        gameDao.insert(game("ended", updatedAt = 30, endedAt = 30))

        assertEquals("new", gameDao.observeActiveGame().first()?.id)

        gameDao.updateTimestamps("new", updatedAt = 40, endedAt = 40)
        assertEquals("old", gameDao.observeActiveGame().first()?.id)
    }

    @Test
    fun restart_endsOldGame_andInsertsNewOne_inOneTransaction() = runTest {
        gameDao.insert(game("old", updatedAt = 1))

        eventDao.restart(
            "old",
            endEventId = "end",
            endTimestamp = 7,
            endPayload = "{}",
            newGame = game("new", updatedAt = 7),
        )

        assertEquals(7L, gameDao.observeGame("old").first()?.endedAt)
        assertEquals(listOf("end"), eventDao.observeEvents("old").first().map { it.id })
        assertEquals("new", gameDao.observeActiveGame().first()?.id)
    }

    @Test
    fun restart_insertsTheNewGamesOpeningEvents() = runTest {
        gameDao.insert(game("old", updatedAt = 1))

        eventDao.restart(
            "old",
            endEventId = "end",
            endTimestamp = 7,
            endPayload = "{}",
            newGame = game("new", updatedAt = 7),
            newEvents = listOf(event("pick", seq = 1, gameId = "new")),
        )

        assertEquals(listOf("pick"), eventDao.observeEvents("new").first().map { it.id })
        assertEquals(listOf("end"), eventDao.observeEvents("old").first().map { it.id })
    }

    @Test
    fun restart_ofEndedGame_doesNotAppendASecondEnd() = runTest {
        gameDao.insert(game("old", updatedAt = 1, endedAt = 1))

        eventDao.restart(
            "old",
            endEventId = "end",
            endTimestamp = 7,
            endPayload = "{}",
            newGame = game("new", updatedAt = 7),
        )

        assertEquals(1L, gameDao.observeGame("old").first()?.endedAt)
        assertEquals(emptyList<String>(), eventDao.observeEvents("old").first().map { it.id })
        assertEquals("new", gameDao.observeActiveGame().first()?.id)
    }

    @Test
    fun activeGame_isNullWhenAllEnded() = runTest {
        gameDao.insert(game("g", updatedAt = 1, endedAt = 1))
        assertNull(gameDao.observeActiveGame().first())
    }

    @Test
    fun events_areOrderedBySeq_andDeleteLastRemovesNewest() = runTest {
        gameDao.insert(game("g", updatedAt = 1))
        eventDao.insert(event("e2", seq = 2))
        eventDao.insert(event("e1", seq = 1))
        eventDao.insert(event("e3", seq = 3))

        assertEquals(listOf("e1", "e2", "e3"), eventDao.observeEvents("g").first().map { it.id })
        assertEquals(3, eventDao.lastSeq("g"))

        eventDao.deleteLast("g")

        assertEquals(listOf("e1", "e2"), eventDao.observeEvents("g").first().map { it.id })
        assertEquals(2, eventDao.lastSeq("g"))
    }

    @Test
    fun append_assignsNextSeq_andTouchesGame() = runTest {
        gameDao.insert(game("g", updatedAt = 1))

        eventDao.append("g", eventId = "e1", timestamp = 5, payload = "{}", endsGame = false)
        eventDao.append("g", eventId = "e2", timestamp = 9, payload = "{}", endsGame = false)

        assertEquals(listOf(1, 2), eventDao.observeEvents("g").first().map { it.seq })
        assertEquals(9L, gameDao.observeGame("g").first()?.updatedAt)
        assertNull(gameDao.observeGame("g").first()?.endedAt)
    }

    @Test
    fun append_endingEvent_marksGameEnded() = runTest {
        gameDao.insert(game("g", updatedAt = 1))

        eventDao.append("g", eventId = "e1", timestamp = 7, payload = "{}", endsGame = true)

        assertEquals(7L, gameDao.observeGame("g").first()?.endedAt)
        assertNull(gameDao.observeActiveGame().first())
    }

    @Test
    fun undoLast_ofEndingEvent_reopensGame() = runTest {
        gameDao.insert(game("g", updatedAt = 1))
        eventDao.append("g", eventId = "e1", timestamp = 2, payload = "{}", endsGame = false)
        eventDao.append("g", eventId = "e2", timestamp = 3, payload = "{}", endsGame = true)

        assertEquals("e2", eventDao.lastEvent("g")?.id)
        eventDao.undoLast("g", updatedAt = 4, reopensGame = true)

        assertEquals(listOf("e1"), eventDao.observeEvents("g").first().map { it.id })
        assertEquals("g", gameDao.observeActiveGame().first()?.id)
        assertEquals(4L, gameDao.observeGame("g").first()?.updatedAt)
    }

    @Test
    fun undoLast_keepsEndedAt_whenNotReopening() = runTest {
        gameDao.insert(game("g", updatedAt = 1, endedAt = 1))
        eventDao.append("g", eventId = "e1", timestamp = 2, payload = "{}", endsGame = false)

        eventDao.undoLast("g", updatedAt = 3, reopensGame = false)

        assertEquals(1L, gameDao.observeGame("g").first()?.endedAt)
    }

    @Test
    fun endedGames_newestFirst_limited() = runTest {
        gameDao.insert(game("a", updatedAt = 1, endedAt = 10))
        gameDao.insert(game("b", updatedAt = 1, endedAt = 30))
        gameDao.insert(game("c", updatedAt = 1))

        assertEquals(listOf("b", "a"), gameDao.observeEndedGames(limit = 5).first().map { it.id })
        assertEquals(listOf("b"), gameDao.observeEndedGames(limit = 1).first().map { it.id })
    }

    @Test
    fun allEndedGames_newestFirst_withoutLimit() = runTest {
        gameDao.insert(game("a", updatedAt = 1, endedAt = 10))
        gameDao.insert(game("b", updatedAt = 1, endedAt = 30))
        gameDao.insert(game("c", updatedAt = 1))
        gameDao.insert(game("d", updatedAt = 1, endedAt = 20))

        assertEquals(listOf("b", "d", "a"), gameDao.observeAllEndedGames().first().map { it.id })
    }

    @Test
    fun endedGameEvents_onlyForEndedGames_orderedByGameThenSeq() = runTest {
        gameDao.insert(game("g", updatedAt = 1, endedAt = 5))
        gameDao.insert(game("active", updatedAt = 1))
        gameDao.insert(game("f", updatedAt = 1, endedAt = 9))
        eventDao.insert(event("g2", seq = 2))
        eventDao.insert(event("g1", seq = 1))
        eventDao.insert(event("x1", seq = 1, gameId = "active"))
        eventDao.insert(event("f1", seq = 1, gameId = "f"))

        assertEquals(listOf("f1", "g1", "g2"), gameDao.observeEndedGameEvents().first().map { it.id })
    }

    @Test
    fun delete_removesGameAndItsEvents_only() = runTest {
        gameDao.insert(game("g", updatedAt = 1, endedAt = 1))
        gameDao.insert(game("other", updatedAt = 1, endedAt = 2))
        eventDao.insert(event("e1", seq = 1))
        eventDao.insert(event("o1", seq = 1, gameId = "other"))

        gameDao.delete("g")
        gameDao.delete("missing")

        assertNull(gameDao.observeGame("g").first())
        assertEquals(emptyList<GameEventEntity>(), eventDao.observeEvents("g").first())
        assertEquals(listOf("other"), gameDao.observeAllEndedGames().first().map { it.id })
        assertEquals(listOf("o1"), eventDao.observeEvents("other").first().map { it.id })
    }

    @Test
    fun insertWithEvents_restoresDeletedGame() = runTest {
        val game = game("g", updatedAt = 3, endedAt = 3)
        val events = listOf(event("e1", seq = 1), event("e2", seq = 2))
        gameDao.insertWithEvents(game, events)
        gameDao.delete("g")

        gameDao.insertWithEvents(game, events)

        assertEquals(game, gameDao.observeGame("g").first())
        assertEquals(events, eventDao.observeEvents("g").first())
    }

    @Test
    fun lastSeq_isNullWithoutEvents() = runTest {
        gameDao.insert(game("g", updatedAt = 1))
        assertNull(eventDao.lastSeq("g"))
    }

    private fun game(id: String, updatedAt: Long, endedAt: Long? = null) = GameEntity(
        id = id,
        createdAt = 0,
        updatedAt = updatedAt,
        configJson = "{}",
        endedAt = endedAt,
    )

    private fun event(id: String, seq: Int, gameId: String = "g") = GameEventEntity(
        id = id,
        gameId = gameId,
        seq = seq,
        timestamp = seq.toLong(),
        payload = "{}",
    )
}
