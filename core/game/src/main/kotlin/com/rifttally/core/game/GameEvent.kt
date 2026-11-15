package com.rifttally.core.game

import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One change to a game. The [GameState] is always derived from the config plus the event log
 * (see [reduce]); scores are never mutated directly.
 *
 * Serializable as polymorphic JSON with the default `type` class discriminator, e.g.
 * `{"type":"points_adjusted","id":"…","timestamp":0,"sideId":"a","delta":1}`.
 */
@Serializable
public sealed interface GameEvent {
    /** Unique id (a UUID). */
    public val id: String

    /** Epoch millis when the event happened. */
    public val timestamp: Long

    /** Adds [delta] (may be negative) to a side's points. The result is floored at 0. */
    @Serializable
    @SerialName("points_adjusted")
    public data class PointsAdjusted(
        override val id: String,
        override val timestamp: Long,
        val sideId: SideId,
        val delta: Int,
    ) : GameEvent

    /** Appends a counter starting at 0 to a player's counters. */
    @Serializable
    @SerialName("counter_added")
    public data class CounterAdded(
        override val id: String,
        override val timestamp: Long,
        val playerId: PlayerId,
        val counterId: CounterId,
        val label: String,
    ) : GameEvent

    /** Adds [delta] (may be negative) to a player's counter. The result is floored at 0. */
    @Serializable
    @SerialName("counter_adjusted")
    public data class CounterAdjusted(
        override val id: String,
        override val timestamp: Long,
        val playerId: PlayerId,
        val counterId: CounterId,
        val delta: Int,
    ) : GameEvent

    /** Removes a player's counter. */
    @Serializable
    @SerialName("counter_removed")
    public data class CounterRemoved(
        override val id: String,
        override val timestamp: Long,
        val playerId: PlayerId,
        val counterId: CounterId,
    ) : GameEvent

    /** Sets the target score. Values below [com.rifttally.core.model.MIN_TARGET_SCORE] are ignored. */
    @Serializable
    @SerialName("target_score_changed")
    public data class TargetScoreChanged(override val id: String, override val timestamp: Long, val newScore: Int) :
        GameEvent

    /**
     * Records who goes first (a player; in 2v2 their team is their side). The latest valid pick wins;
     * an unknown player is ignored.
     */
    @Serializable
    @SerialName("first_player_chosen")
    public data class FirstPlayerChosen(override val id: String, override val timestamp: Long, val playerId: PlayerId) :
        GameEvent

    /** Ends the game, optionally naming a [winner]. Later events are ignored by [reduce]. */
    @Serializable
    @SerialName("game_ended")
    public data class GameEnded(override val id: String, override val timestamp: Long, val winner: SideId? = null) :
        GameEvent
}
