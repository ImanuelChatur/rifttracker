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

    private fun event(id: String, seq: Int) = GameEventEntity(
        id = id,
        gameId = "g",
        seq = seq,
        timestamp = seq.toLong(),
        payload = "{}",
    )
}
