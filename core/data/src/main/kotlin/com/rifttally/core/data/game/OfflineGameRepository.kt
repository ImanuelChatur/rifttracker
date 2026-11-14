package com.rifttally.core.data.game

import com.rifttally.core.common.dispatchers.IoDispatcher
import com.rifttally.core.database.dao.GameDao
import com.rifttally.core.database.dao.GameEventDao
import com.rifttally.core.database.model.GameEntity
import com.rifttally.core.database.model.GameEventEntity
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException

/** Room-backed [GameRepository]. Configs and events are stored as JSON (see [GameJson]). */
@OptIn(ExperimentalTime::class)
@Singleton
internal class OfflineGameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val eventDao: GameEventDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GameRepository {

    // Serializes undo's read-then-delete against concurrent writes (rapid taps).
    private val writeMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val activeGame: Flow<GameRecord?> = gameDao.observeActiveGame()
        // Every append bumps updated_at; only re-subscribe when the active game itself changes.
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else observeGame(GameId(id)) }

    override fun observeGame(id: GameId): Flow<GameRecord?> =
        combine(gameDao.observeGame(id.value), eventDao.observeEvents(id.value)) { game, events ->
            game?.toRecord(events)
        }.flowOn(ioDispatcher)

    override suspend fun createGame(config: GameConfig): GameId {
        val id = GameId(idGenerator.newId())
        val now = clock.now().toEpochMilliseconds()
        gameDao.insert(
            GameEntity(
                id = id.value,
                createdAt = now,
                updatedAt = now,
                configJson = GameJson.encodeToString(GameConfig.serializer(), config),
            ),
        )
        return id
    }

    override suspend fun appendEvent(gameId: GameId, event: GameEvent) {
        writeMutex.withLock {
            eventDao.append(
                gameId = gameId.value,
                eventId = event.id,
                timestamp = event.timestamp,
                payload = GameJson.encodeToString(GameEvent.serializer(), event),
                endsGame = event is GameEvent.GameEnded,
            )
        }
    }

    override suspend fun undoLastEvent(gameId: GameId) {
        writeMutex.withLock {
            val last = eventDao.lastEvent(gameId.value) ?: return
            eventDao.undoLast(
                gameId = gameId.value,
                updatedAt = clock.now().toEpochMilliseconds(),
                reopensGame = last.decodeOrNull() is GameEvent.GameEnded,
            )
        }
    }

    private fun GameEntity.toRecord(events: List<GameEventEntity>): GameRecord? {
        val config = try {
            GameJson.decodeFromString(GameConfig.serializer(), configJson)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return GameRecord(
            id = GameId(id),
            config = config,
            // An unreadable event is skipped rather than losing the whole game.
            events = events.mapNotNull { it.decodeOrNull() },
            createdAt = createdAt,
            endedAt = endedAt,
        )
    }

    private fun GameEventEntity.decodeOrNull(): GameEvent? = try {
        GameJson.decodeFromString(GameEvent.serializer(), payload)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
