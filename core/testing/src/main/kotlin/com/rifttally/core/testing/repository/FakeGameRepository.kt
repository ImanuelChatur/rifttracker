package com.rifttally.core.testing.repository

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [GameRepository] for ViewModel tests. Games get ids `game1`, `game2`, … The active game is
 * the most recently created or modified one that hasn't ended.
 */
public class FakeGameRepository : GameRepository {
    private val games = MutableStateFlow<List<GameRecord>>(emptyList())
    private var created = 0

    /** Stale snapshots [observeGame] emits instead of the real game, until [flush]. */
    private val stale = MutableStateFlow<Map<GameId, GameRecord>>(emptyMap())

    /** Every game, oldest first, most recently touched last. */
    public val allGames: List<GameRecord> get() = games.value

    /** Stores [record] as is (e.g. an ended game with a full log), replacing any game with the same id. */
    public fun addGame(record: GameRecord) {
        games.update { all -> all.filterNot { it.id == record.id } + record }
    }

    override val activeGame: Flow<GameRecord?> = games.map { all -> all.lastOrNull { it.endedAt == null } }

    override fun recentGames(limit: Int): Flow<List<GameRecord>> = endedGames().map { it.take(limit) }

    override fun endedGames(): Flow<List<GameRecord>> = games.map { all ->
        all.filter { it.endedAt != null }.sortedByDescending { it.endedAt }
    }

    override fun observeGame(id: GameId): Flow<GameRecord?> =
        combine(games, stale) { all, stale -> stale[id] ?: all.firstOrNull { it.id == id } }

    /**
     * Makes [observeGame] emit the game as it was with only its first [eventCount] events (like a late
     * Room emission), and keep emitting that, even across writes, until [flush].
     */
    public fun emitStale(gameId: GameId, eventCount: Int) {
        val record = games.value.firstOrNull { it.id == gameId } ?: return
        emitStale(gameId, record.events.take(eventCount))
    }

    /** Like [emitStale] with a count, but with an exact older log (e.g. one the game no longer has). */
    public fun emitStale(gameId: GameId, events: List<GameEvent>) {
        val record = games.value.firstOrNull { it.id == gameId } ?: return
        val endedAt = (events.lastOrNull() as? GameEvent.GameEnded)?.timestamp
        stale.update { it + (gameId to record.copy(events = events, endedAt = endedAt)) }
    }

    /** Runs before every append/undo; set it to suspend to keep writes in flight. */
    public var beforeWrite: (suspend () -> Unit)? = null

    /** Ends [emitStale]: [observeGame] emits the real games again. */
    public fun flush() {
        stale.value = emptyMap()
    }

    override suspend fun createGame(config: GameConfig, openingEvents: List<GameEvent>): GameId {
        val id = GameId("game${++created}")
        games.update { it + GameRecord(id = id, config = config, events = openingEvents, createdAt = 0) }
        return id
    }

    override suspend fun appendEvent(gameId: GameId, event: GameEvent) {
        beforeWrite?.invoke()
        modify(gameId) { record ->
            record.copy(
                events = record.events + event,
                endedAt = if (event is GameEvent.GameEnded) event.timestamp else record.endedAt,
            )
        }
    }

    override suspend fun undoLastEvent(gameId: GameId) {
        beforeWrite?.invoke()
        modify(gameId) { record ->
            val last = record.events.lastOrNull() ?: return@modify record
            record.copy(
                events = record.events.dropLast(1),
                endedAt = if (last is GameEvent.GameEnded) null else record.endedAt,
            )
        }
    }

    override suspend fun restartGame(
        oldGameId: GameId,
        endEvent: GameEvent.GameEnded,
        newConfig: GameConfig,
        openingEvents: List<GameEvent>,
    ): GameId {
        if (games.value.any { it.id == oldGameId && it.endedAt == null }) appendEvent(oldGameId, endEvent)
        return createGame(newConfig, openingEvents)
    }

    override suspend fun deleteGame(id: GameId) {
        games.update { all -> all.filterNot { it.id == id } }
    }

    override suspend fun restoreGame(record: GameRecord) {
        games.update { all -> if (all.any { it.id == record.id }) all else all + record }
    }

    /** Applies [transform] and moves the game to the end, like `updated_at` ordering. */
    private fun modify(gameId: GameId, transform: (GameRecord) -> GameRecord) {
        games.update { all ->
            val record = all.firstOrNull { it.id == gameId } ?: return@update all
            all.filterNot { it.id == gameId } + transform(record)
        }
    }
}
