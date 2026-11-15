package com.rifttally.core.data.timer

import com.rifttally.core.common.dispatchers.IoDispatcher
import com.rifttally.core.database.dao.GameTimerDao
import com.rifttally.core.database.model.GameTimerEntity
import com.rifttally.core.datastore.RiftPreferencesDataSource
import com.rifttally.core.datastore.StoredRoundTimerChoice
import com.rifttally.core.model.GameId
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Room-backed timers (`game_timer`) and the DataStore-backed Setup choice. Every timer change is one
 * SQL statement or transaction (see [GameTimerDao]), with "now" from the injected [clock].
 */
@OptIn(ExperimentalTime::class)
internal class OfflineRoundTimerRepository @Inject constructor(
    private val timerDao: GameTimerDao,
    private val preferences: RiftPreferencesDataSource,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RoundTimerRepository {

    override fun observeTimer(gameId: GameId): Flow<RoundTimer?> = timerDao.observe(gameId.value)
        .map { it?.toRoundTimer() }
        // Writes to other games' timers re-run this query; only emit real changes.
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    override suspend fun createTimer(gameId: GameId, minutes: Int) {
        val timer = RoundTimer.ofMinutes(minutes.coerceIn(RoundTimerChoice.MIN_MINUTES, RoundTimerChoice.MAX_MINUTES))
        timerDao.upsert(GameTimerEntity(gameId = gameId.value, durationMs = timer.durationMs))
    }

    override suspend fun toggle(gameId: GameId): Unit = timerDao.toggle(gameId.value, now())

    override suspend fun reset(gameId: GameId): Unit = timerDao.reset(gameId.value)

    override suspend fun copyTimer(fromGameId: GameId, toGameId: GameId): Unit =
        timerDao.copy(fromGameId = fromGameId.value, toGameId = toGameId.value)

    override val choice: Flow<RoundTimerChoice> = preferences.roundTimerChoice
        .map { it.toChoice() }
        .distinctUntilChanged()

    override suspend fun setChoiceEnabled(enabled: Boolean): Unit =
        preferences.updateRoundTimerChoice { it.copy(enabled = enabled) }

    override suspend fun adjustChoiceMinutes(deltaMinutes: Int): Unit = preferences.updateRoundTimerChoice { stored ->
        stored.copy(minutes = clampMinutes(stored.toChoice().minutes + deltaMinutes))
    }

    private fun now(): Long = clock.now().toEpochMilliseconds()
}

private fun GameTimerEntity.toRoundTimer() =
    RoundTimer(durationMs = durationMs, elapsedBeforeMs = elapsedBeforeMs, runningSinceMs = runningSince)

private fun StoredRoundTimerChoice.toChoice() = RoundTimerChoice(
    enabled = enabled,
    minutes = minutes?.let(::clampMinutes) ?: RoundTimerChoice.DEFAULT_MINUTES,
)

private fun clampMinutes(minutes: Int): Int =
    minutes.coerceIn(RoundTimerChoice.MIN_MINUTES, RoundTimerChoice.MAX_MINUTES)
