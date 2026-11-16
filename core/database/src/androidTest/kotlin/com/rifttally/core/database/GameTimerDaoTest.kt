package com.rifttally.core.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameTimerDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameTimerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameTimerDaoTest {
    private lateinit var db: RiftDatabase
    private lateinit var gameDao: GameDao
    private lateinit var timerDao: GameTimerDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RiftDatabase::class.java).build()
        gameDao = db.gameDao()
        timerDao = db.gameTimerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startPause_banksElapsedTime() = runTest {
        gameDao.insert(game("g"))
        timerDao.upsert(GameTimerEntity(gameId = "g", durationMs = 60_000))

        timerDao.start("g", now = 1_000)
        timerDao.start("g", now = 5_000) // already running: keeps the first start
        assertEquals(1_000L, timerDao.get("g")?.runningSince)

        timerDao.pause("g", now = 11_000)
        timerDao.pause("g", now = 99_000) // already paused: nothing banked
        assertEquals(GameTimerEntity("g", 60_000, elapsedBeforeMs = 10_000, runningSince = null), timerDao.get("g"))
    }

    @Test
    fun pause_withClockBehindStart_banksNothing() = runTest {
        gameDao.insert(game("g"))
        timerDao.upsert(GameTimerEntity("g", 60_000, elapsedBeforeMs = 2_000, runningSince = 10_000))

        timerDao.pause("g", now = 4_000)

        assertEquals(2_000L, timerDao.get("g")?.elapsedBeforeMs)
    }

    @Test
    fun toggle_startsThenPauses() = runTest {
        gameDao.insert(game("g"))
        timerDao.upsert(GameTimerEntity("g", 60_000))

        timerDao.toggle("g", now = 100)
        assertEquals(100L, timerDao.observe("g").first()?.runningSince)
        timerDao.toggle("g", now = 400)
        assertEquals(GameTimerEntity("g", 60_000, elapsedBeforeMs = 300), timerDao.observe("g").first())
    }

    @Test
    fun reset_isFullLengthPaused() = runTest {
        gameDao.insert(game("g"))
        timerDao.upsert(GameTimerEntity("g", 60_000, elapsedBeforeMs = 70_000, runningSince = 5))

        timerDao.reset("g")

        assertEquals(GameTimerEntity("g", 60_000), timerDao.get("g"))
    }

    @Test
    fun copy_carriesTheTimerOverUnchanged_andReplaces() = runTest {
        gameDao.insert(game("old"))
        gameDao.insert(game("new"))
        timerDao.upsert(GameTimerEntity("old", 60_000, elapsedBeforeMs = 1_234, runningSince = 777))
        timerDao.upsert(GameTimerEntity("new", 5_000))

        gameDao.copyTimer(fromGameId = "old", toGameId = "new")

        assertEquals(GameTimerEntity("new", 60_000, elapsedBeforeMs = 1_234, runningSince = 777), timerDao.get("new"))
        assertEquals(GameTimerEntity("old", 60_000, elapsedBeforeMs = 1_234, runningSince = 777), timerDao.get("old"))
    }

    @Test
    fun copy_fromGameWithoutTimer_doesNothing() = runTest {
        gameDao.insert(game("old"))
        gameDao.insert(game("new"))

        gameDao.copyTimer(fromGameId = "old", toGameId = "new")

        assertNull(timerDao.get("new"))
    }

    @Test
    fun deletingTheGame_deletesItsTimer() = runTest {
        gameDao.insert(game("g"))
        timerDao.upsert(GameTimerEntity("g", 60_000))

        gameDao.delete("g")

        assertNull(timerDao.get("g"))
    }

    @Test
    fun timerForUnknownGame_isRejected() = runTest {
        val failure = runCatching { timerDao.upsert(GameTimerEntity("missing", 60_000)) }.exceptionOrNull()
        assertTrue(failure.toString(), failure is SQLiteConstraintException)
    }

    private fun game(id: String) = GameEntity(id = id, createdAt = 0, updatedAt = 0, configJson = "{}")
}
