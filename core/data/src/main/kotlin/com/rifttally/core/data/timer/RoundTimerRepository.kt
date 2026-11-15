package com.rifttally.core.data.timer

import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow

/**
 * Round timers of games (stored per game, survive process death) and Setup's remembered timer choice.
 * Start and pause read "now" from the injected clock.
 */
public interface RoundTimerRepository {
    /** [gameId]'s timer, or `null` if the game has none. */
    public fun observeTimer(gameId: GameId): Flow<RoundTimer?>

    /** Gives [gameId] a fresh timer of [minutes]: full length, paused. Replaces any timer it had. */
    public suspend fun createTimer(gameId: GameId, minutes: Int)

    /** Starts [gameId]'s timer if paused, pauses it if running. Does nothing without a timer. */
    public suspend fun toggle(gameId: GameId)

    /** Back to the full length, paused. Does nothing without a timer. */
    public suspend fun reset(gameId: GameId)

    /**
     * Carries [fromGameId]'s timer over to [toGameId] unchanged (same length and elapsed time, still
     * running if it was), for Rematch and Restart. Does nothing if [fromGameId] has no timer.
     */
    public suspend fun copyTimer(fromGameId: GameId, toGameId: GameId)

    /** Setup's remembered choice (default: off, 50 minutes). */
    public val choice: Flow<RoundTimerChoice>

    public suspend fun setChoiceEnabled(enabled: Boolean)

    /**
     * Changes the remembered length by [deltaMinutes], kept within
     * [RoundTimerChoice.MIN_MINUTES]..[RoundTimerChoice.MAX_MINUTES]. Applied atomically to the stored
     * value, so fast taps don't get lost.
     */
    public suspend fun adjustChoiceMinutes(deltaMinutes: Int)
}
