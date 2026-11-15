package com.rifttally.core.data.timer

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rifttally.core.database.dao.GameTimerDao
import com.rifttally.core.database.model.GameTimerEntity
import com.rifttally.core.datastore.RiftPreferencesDataSource
import com.rifttally.core.model.GameId
import com.rifttally.core.testing.TestClock
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class OfflineRoundTimerRepositoryTest {
    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val dispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val clock = TestClock()
    private val dao = InMemoryTimerDao()
    private val repository = OfflineRoundTimerRepository(
        timerDao = dao,
        preferences = RiftPreferencesDataSource(
            PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) { tmp.newFile("t.preferences_pb") },
        ),
        clock = clock,
        ioDispatcher = dispatcher,
    )
    private val game = GameId("g1")

    @Test
    fun createTimer_isFullLengthAndPaused_clampedToLimits() = testScope.runTest {
        repository.createTimer(game, 50)
        assertEquals(RoundTimer(durationMs = 3_000_000), repository.observeTimer(game).first())

        repository.createTimer(game, 500)
        assertEquals(120 * 60_000L, repository.observeTimer(game).first()?.durationMs)
    }

    @Test
    fun toggle_usesTheInjectedClock() = testScope.runTest {
        repository.createTimer(game, 5)
        clock.advanceBy(10.seconds)
        repository.toggle(game)
        assertEquals(10_000L, repository.observeTimer(game).first()?.runningSinceMs)

        clock.advanceBy(30.seconds)
        repository.toggle(game)
        assertEquals(RoundTimer(durationMs = 300_000, elapsedBeforeMs = 30_000), repository.observeTimer(game).first())
    }

    @Test
    fun noTimer_isNull_andChangesDoNothing() = testScope.runTest {
        repository.toggle(game)
        repository.reset(game)
        repository.copyTimer(game, GameId("g2"))
        assertNull(repository.observeTimer(game).first())
        assertNull(repository.observeTimer(GameId("g2")).first())
    }

    @Test
    fun choice_defaultsToOff50_andIsRemembered() = testScope.runTest {
        assertEquals(RoundTimerChoice(enabled = false, minutes = 50), repository.choice.first())

        repository.setChoiceEnabled(true)
        repository.adjustChoiceMinutes(-5)
        assertEquals(RoundTimerChoice(enabled = true, minutes = 45), repository.choice.first())

        // Turning it off keeps the length for next time.
        repository.setChoiceEnabled(false)
        assertEquals(RoundTimerChoice(enabled = false, minutes = 45), repository.choice.first())
    }

    @Test
    fun adjustChoiceMinutes_staysWithin5To120() = testScope.runTest {
        repeat(30) { repository.adjustChoiceMinutes(-5) }
        assertEquals(5, repository.choice.first().minutes)
        repeat(30) { repository.adjustChoiceMinutes(5) }
        assertEquals(120, repository.choice.first().minutes)
    }

    /** Mirrors the SQL of [GameTimerDao] (the real statements are covered by `GameTimerDaoTest`). */
    private class InMemoryTimerDao : GameTimerDao {
        private val rows = MutableStateFlow<Map<String, GameTimerEntity>>(emptyMap())

        override fun observe(gameId: String): Flow<GameTimerEntity?> = rows.map { it[gameId] }

        override suspend fun get(gameId: String): GameTimerEntity? = rows.value[gameId]

        override suspend fun upsert(timer: GameTimerEntity) = rows.update { it + (timer.gameId to timer) }

        override suspend fun start(gameId: String, now: Long) = edit(gameId) {
            if (it.runningSince == null) it.copy(runningSince = now) else it
        }

        override suspend fun pause(gameId: String, now: Long) = edit(gameId) { row ->
            val since = row.runningSince ?: return@edit row
            row.copy(elapsedBeforeMs = row.elapsedBeforeMs + maxOf(0, now - since), runningSince = null)
        }

        override suspend fun reset(gameId: String) = edit(gameId) { it.copy(elapsedBeforeMs = 0, runningSince = null) }

        override suspend fun copy(fromGameId: String, toGameId: String) = rows.update { all ->
            all[fromGameId]?.let { all + (toGameId to it.copy(gameId = toGameId)) } ?: all
        }

        private fun edit(gameId: String, transform: (GameTimerEntity) -> GameTimerEntity) = rows.update { all ->
            all[gameId]?.let { all + (gameId to transform(it)) } ?: all
        }
    }
}
