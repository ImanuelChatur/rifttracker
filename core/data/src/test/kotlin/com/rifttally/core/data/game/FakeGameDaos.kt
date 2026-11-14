package com.rifttally.core.data.game

import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory stand-ins for the Room DAOs. Mirrors the SQL semantics the repository relies on. */
internal class FakeGameDao : GameDao {
    val games = MutableStateFlow<Map<String, GameEntity>>(emptyMap())

    override suspend fun insert(game: GameEntity) {
        check(game.id !in games.value) { "duplicate game ${game.id}" }
        games.update { it + (game.id to game) }
    }

    override fun observeGame(id: String): Flow<GameEntity?> = games.map { it[id] }

    override fun observeActiveGame(): Flow<GameEntity?> =
        games.map { all -> all.values.filter { it.endedAt == null }.maxByOrNull { it.updatedAt } }

    override fun observeEndedGames(limit: Int): Flow<List<GameEntity>> = games.map { all ->
        all.values.filter { it.endedAt != null }.sortedByDescending { it.endedAt }.take(limit)
    }

    override suspend fun updateTimestamps(id: String, updatedAt: Long, endedAt: Long?) {
        games.update { all ->
            val game = all[id]
            if (game == null) all else all + (id to game.copy(updatedAt = updatedAt, endedAt = endedAt))
        }
    }
}

internal class FakeGameEventDao(private val gameDao: FakeGameDao) : GameEventDao() {
    val events = MutableStateFlow<List<GameEventEntity>>(emptyList())

    override suspend fun insert(event: GameEventEntity) {
        events.update { it + event }
    }

    override fun observeEvents(gameId: String): Flow<List<GameEventEntity>> =
        events.map { all -> all.filter { it.gameId == gameId }.sortedBy { it.seq } }

    override suspend fun lastSeq(gameId: String): Int? =
        events.value.filter { it.gameId == gameId }.maxOfOrNull { it.seq }

    override suspend fun lastEvent(gameId: String): GameEventEntity? =
        events.value.filter { it.gameId == gameId }.maxByOrNull { it.seq }

    override suspend fun deleteLast(gameId: String) {
        val last = lastEvent(gameId) ?: return
        events.update { it - last }
    }

    override suspend fun touchGame(gameId: String, updatedAt: Long) {
        val game = gameDao.games.value[gameId] ?: return
        gameDao.updateTimestamps(gameId, updatedAt, game.endedAt)
    }

    override suspend fun setGameEnded(gameId: String, updatedAt: Long, endedAt: Long?) {
        gameDao.updateTimestamps(gameId, updatedAt, endedAt)
    }
}
