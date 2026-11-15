package com.rifttally.core.testing.repository

import com.rifttally.core.data.timer.RoundTimer
import com.rifttally.core.data.timer.RoundTimerChoice
import com.rifttally.core.data.timer.RoundTimerRepository
import com.rifttally.core.model.GameId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [RoundTimerRepository] for ViewModel tests; "now" comes from [clock] (e.g. a `TestClock`). */
@OptIn(ExperimentalTime::class)
public class FakeRoundTimerRepository(private val clock: Clock, initialChoice: RoundTimerChoice = RoundTimerChoice()) :
    RoundTimerRepository {
    private val timers = MutableStateFlow<Map<GameId, RoundTimer>>(emptyMap())
    private val choiceState = MutableStateFlow(initialChoice)

    /** Every stored timer by game. */
    public val allTimers: Map<GameId, RoundTimer> get() = timers.value

    public val currentChoice: RoundTimerChoice get() = choiceState.value

    /** Stores [timer] for [gameId] as is. */
    public fun setTimer(gameId: GameId, timer: RoundTimer) {
        timers.update { it + (gameId to timer) }
    }

    override fun observeTimer(gameId: GameId): Flow<RoundTimer?> = timers.map { it[gameId] }.distinctUntilChanged()

    override suspend fun createTimer(gameId: GameId, minutes: Int) {
        val clamped = minutes.coerceIn(RoundTimerChoice.MIN_MINUTES, RoundTimerChoice.MAX_MINUTES)
        setTimer(gameId, RoundTimer.ofMinutes(clamped))
    }

    override suspend fun toggle(gameId: GameId): Unit = modify(gameId) { timer ->
        val now = clock.now().toEpochMilliseconds()
        if (timer.isRunning) timer.pausedAt(now) else timer.startedAt(now)
    }

    override suspend fun reset(gameId: GameId): Unit = modify(gameId) { it.reset() }

    override suspend fun copyTimer(fromGameId: GameId, toGameId: GameId) {
        timers.update { all -> all[fromGameId]?.let { all + (toGameId to it) } ?: all }
    }

    override val choice: Flow<RoundTimerChoice> = choiceState

    override suspend fun setChoiceEnabled(enabled: Boolean): Unit = choiceState.update { it.copy(enabled = enabled) }

    override suspend fun adjustChoiceMinutes(deltaMinutes: Int): Unit = choiceState.update {
        it.copy(
            minutes = (it.minutes + deltaMinutes).coerceIn(RoundTimerChoice.MIN_MINUTES, RoundTimerChoice.MAX_MINUTES),
        )
    }

    private fun modify(gameId: GameId, transform: (RoundTimer) -> RoundTimer) {
        timers.update { all -> all[gameId]?.let { all + (gameId to transform(it)) } ?: all }
    }
}
