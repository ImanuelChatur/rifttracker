package com.rifttally.core.data.timer

import com.rifttally.core.model.GameId

/**
 * The round timer a new game starts with. [com.rifttally.core.data.game.GameRepository] writes it in the
 * same transaction as the game, so a process death can't leave the game without its timer.
 */
public sealed interface NewGameTimer {
    /** No timer. */
    public data object None : NewGameTimer

    /** A fresh timer of [minutes] (clamped to the Setup limits), full length and paused. */
    public data class Fresh(val minutes: Int) : NewGameTimer {
        /** The timer as stored. */
        public val timer: RoundTimer
            get() = RoundTimer.ofMinutes(minutes.coerceIn(RoundTimerChoice.MIN_MINUTES, RoundTimerChoice.MAX_MINUTES))
    }

    /** [gameId]'s timer exactly as it is (still running if it was), e.g. for a rematch. None if it has none. */
    public data class CopyOf(val gameId: GameId) : NewGameTimer
}
