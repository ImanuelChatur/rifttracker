package com.rifttally.core.testing.repository

import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [GameRepository] for ViewModel tests. Games get ids `game1`, `game2`, … The active game is
 * the most recently created or modified one that hasn't ended.
 */
public class FakeGameRepository : GameRepository {
    private val games = MutableStateFlow<List<GameRecord>>(emptyList())
    private var created = 0

    /** Every game, oldest first, most recently touched last. */
    public val allGames: List<GameRecord> get() = games.value

    override val activeGame: Flow<GameRecord?> = games.map { all -> all.lastOrNull { it.endedAt == null } }

    override fun observeGame(id: GameId): Flow<GameRecord?> = games.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun createGame(config: GameConfig): GameId {
        val id = GameId("game${++created}")
        games.update { it + GameRecord(id = id, config = config, events = emptyList(), createdAt = 0) }
        return id
    }

    override suspend fun appendEvent(gameId: GameId, event: GameEvent) {
        modify(gameId) { record ->
            record.copy(
                events = record.events + event,
                endedAt = if (event is GameEvent.GameEnded) event.timestamp else record.endedAt,
            )
        }
    }

    override suspend fun undoLastEvent(gameId: GameId) {
        modify(gameId) { record ->
            val last = record.events.lastOrNull() ?: return@modify record
            record.copy(
                events = record.events.dropLast(1),
                endedAt = if (last is GameEvent.GameEnded) null else record.endedAt,
            )
        }
    }

    /** Applies [transform] and moves the game to the end, like `updated_at` ordering. */
    private fun modify(gameId: GameId, transform: (GameRecord) -> GameRecord) {
        games.update { all ->
            val record = all.firstOrNull { it.id == gameId } ?: return@update all
            all.filterNot { it.id == gameId } + transform(record)
        }
    }
}
