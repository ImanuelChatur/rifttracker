package com.rifttally.core.game

import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Builds [GameEvent]s stamped with a fresh id from [idGenerator] and the current time from [clock].
 *
 * Callers should not append an event that would change nothing (e.g. `−` at 0 points or on a counter
 * at 0). [reduce] tolerates such events, but they would make the next undo look like it did nothing.
 */
@OptIn(ExperimentalTime::class)
public class GameEventFactory(
    private val clock: Clock,
    private val idGenerator: IdGenerator = IdGenerator.RandomUuid,
) {
    public fun pointsAdjusted(sideId: SideId, delta: Int): GameEvent.PointsAdjusted =
        GameEvent.PointsAdjusted(id = newId(), timestamp = now(), sideId = sideId, delta = delta)

    /** Adds a counter. A new [counterId] is generated unless one is given. */
    public fun counterAdded(
        playerId: PlayerId,
        label: String,
        counterId: CounterId = CounterId(idGenerator.newId()),
    ): GameEvent.CounterAdded = GameEvent.CounterAdded(
        id = newId(),
        timestamp = now(),
        playerId = playerId,
        counterId = counterId,
        label = label,
    )

    public fun counterAdjusted(playerId: PlayerId, counterId: CounterId, delta: Int): GameEvent.CounterAdjusted =
        GameEvent.CounterAdjusted(
            id = newId(),
            timestamp = now(),
            playerId = playerId,
            counterId = counterId,
            delta = delta,
        )

    public fun counterRemoved(playerId: PlayerId, counterId: CounterId): GameEvent.CounterRemoved =
        GameEvent.CounterRemoved(id = newId(), timestamp = now(), playerId = playerId, counterId = counterId)

    public fun targetScoreChanged(newScore: Int): GameEvent.TargetScoreChanged =
        GameEvent.TargetScoreChanged(id = newId(), timestamp = now(), newScore = newScore)

    public fun firstPlayerChosen(playerId: PlayerId): GameEvent.FirstPlayerChosen =
        GameEvent.FirstPlayerChosen(id = newId(), timestamp = now(), playerId = playerId)

    public fun gameEnded(winner: SideId?): GameEvent.GameEnded =
        GameEvent.GameEnded(id = newId(), timestamp = now(), winner = winner)

    private fun newId(): String = idGenerator.newId()

    private fun now(): Long = clock.now().toEpochMilliseconds()
}
