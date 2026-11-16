package com.rifttally.core.data.timer

import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow

/**
 * Round timers of games (stored per game, survive process death) and Setup's remembered timer choice.
 * Start and pause read "now" from the injected clock. A game's timer is created with the game, in the
 * same transaction (see [NewGameTimer] and [com.rifttally.core.data.game.GameRepository.createGame]).
 */
public interface RoundTimerRepository {
    /** [gameId]'s timer, or `null` if the game has none. */
    public fun observeTimer(gameId: GameId): Flow<RoundTimer?>

    /** Starts [gameId]'s timer if paused, pauses it if running. Does nothing without a timer. */
    public suspend fun toggle(gameId: GameId)

    /** Back to the full length, paused. Does nothing without a timer. */
    public suspend fun reset(gameId: GameId)

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
