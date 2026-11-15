package com.rifttally.feature.tracker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.GameState
import com.rifttally.core.game.HistoryEntry
import com.rifttally.core.game.gameLog
import com.rifttally.core.game.history
import com.rifttally.core.game.reduce
import com.rifttally.core.game.sidesAtTarget
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.MIN_TARGET_SCORE
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.model.UserSettings
import com.rifttally.feature.tracker.navigation.TrackerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The live table. State is derived from the stored event log; every action appends an event (or, for
 * undo, removes the newest one). Never mutates scores directly.
 */
@HiltViewModel
internal class TrackerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    legendRepository: LegendRepository,
    userSettingsRepository: UserSettingsRepository,
    private val eventFactory: GameEventFactory,
) : ViewModel() {

    // Navigation stores route arguments under their property names. Read by key (not toRoute()) so the
    // ViewModel is testable on the JVM without Android bundles.
    private val gameId = GameId(checkNotNull(savedStateHandle.get<String>(TrackerRoute::gameId.name)))

    /**
     * The game as last read from storage, advanced optimistically by every event this ViewModel sends,
     * so rapid taps (faster than Room re-emits) still see the floor at 0 and don't append no-op events.
     */
    private var latest: Snapshot? = null

    /**
     * True from every write (append, undo, restart) until all of them have finished and Room has emitted
     * a log with exactly [latest]'s event ids. Meanwhile [latest] is the truth: an older emission that
     * lacks events still in flight must not roll it back, or a blocked "−" at 0 could slip through. Safe
     * because the repository applies writes in order, so the database converges to exactly this log, and
     * only this screen writes to the game.
     *
     * Ids alone aren't enough: after "+" then Undo, an emission from before the "+" has the same ids.
     * Requiring [pendingWrites] == 0 rules that out: a Room query flow emits in order, so once every
     * write has committed, an emission matching the log can't be followed by an older one.
     */
    private var awaitingSync = false

    /** Writes sent to the repository that haven't returned yet. */
    private var pendingWrites = 0

    /** Bumped when [pendingWrites] drops to 0, to re-check the last emission against [latest]. */
    private val writesSettled = MutableStateFlow(0)

    /** Sides whose "Winner?" banner was dismissed with "Keep playing". Pruned to sides still at target. */
    private val dismissedBanner = MutableStateFlow<Set<SideId>>(emptySet())

    /** Set while ending or restarting, so a double tap can't end twice or create two new games. */
    private var isLeaving = false

    private val navigation = Channel<TrackerNavigationEvent>(Channel.BUFFERED)

    /** One-off navigation requests (leave after ending, open a restarted game). */
    val navigationEvents: Flow<TrackerNavigationEvent> = navigation.receiveAsFlow()

    val uiState: StateFlow<TrackerUiState> = combine(
        gameRepository.observeGame(gameId),
        legendRepository.legends,
        userSettingsRepository.userSettings,
        dismissedBanner,
        writesSettled,
    ) { record, legends, settings, dismissed, _ ->
        if (record == null) {
            latest = null
            awaitingSync = false
            TrackerUiState.NotFound
        } else {
            val pending = latest
            val inSync = pending != null && record.events.map { it.id } == pending.record.events.map { it.id }
            if (pendingWrites == 0 && inSync) awaitingSync = false
            // While writes are in flight, show the optimistic log (with the emitted config/timestamps).
            val source = if (awaitingSync && pending != null) record.copy(events = pending.record.events) else record
            val entries = history(source.config, source.events)
            val initial = reduce(source.config, emptyList())
            val state = entries.lastOrNull()?.stateAfter ?: initial
            if (!awaitingSync || pending == null) latest = Snapshot(source, state)
            // A side that left the target (by points, undo or a target change) can trigger the banner again.
            val stillDismissed = dismissed intersect sidesAtTarget(state).toSet()
            if (stillDismissed != dismissed) dismissedBanner.value = stillDismissed
            buildUiState(source, initial, entries, state, legends, settings, stillDismissed)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState.Loading)

    fun onPointsChange(sideId: SideId, delta: Int) {
        val state = latest?.state ?: return
        if (state.isEnded || delta == 0 || sideId !in state.points) return
        if (delta < 0 && state.pointsOf(sideId) == 0) return
        append(eventFactory.pointsAdjusted(sideId, delta))
    }

    fun onUndo() {
        val snapshot = latest ?: return
        if (snapshot.record.events.isEmpty()) return
        val events = snapshot.record.events.dropLast(1)
        latest = Snapshot(snapshot.record.copy(events = events), reduce(snapshot.record.config, events))
        awaitingSync = true
        write { gameRepository.undoLastEvent(gameId) }
    }

    fun onTargetScoreChange(newScore: Int) {
        val state = latest?.state ?: return
        if (state.isEnded || newScore < MIN_TARGET_SCORE || newScore == state.targetScore) return
        append(eventFactory.targetScoreChanged(newScore))
    }

    /** Adds a counter (e.g. "XP") under [playerId]'s points. Blank labels are ignored. */
    fun onCounterAdd(playerId: PlayerId, label: String) {
        val state = latest?.state ?: return
        val trimmed = label.trim().take(MAX_COUNTER_LABEL_LENGTH)
        if (state.isEnded || trimmed.isEmpty() || playerId !in state.counters) return
        if (state.countersOf(playerId).size >= MAX_COUNTERS_PER_PLAYER) return
        append(eventFactory.counterAdded(playerId, trimmed))
    }

    fun onCounterChange(playerId: PlayerId, counterId: CounterId, delta: Int) {
        val state = latest?.state ?: return
        if (state.isEnded || delta == 0) return
        val counter = state.countersOf(playerId).firstOrNull { it.id == counterId } ?: return
        if (delta < 0 && counter.value == 0) return
        append(eventFactory.counterAdjusted(playerId, counterId, delta))
    }

    fun onCounterRemove(playerId: PlayerId, counterId: CounterId) {
        val state = latest?.state ?: return
        if (state.isEnded || state.countersOf(playerId).none { it.id == counterId }) return
        append(eventFactory.counterRemoved(playerId, counterId))
    }

    /** "Keep playing": hides the banner until another side reaches the target. */
    fun onKeepPlaying() {
        val state = latest?.state ?: return
        dismissedBanner.value = sidesAtTarget(state).toSet()
    }

    /** Ends the game. The winner is the only side at the target, or none if zero or several are. */
    fun onEndGame() {
        val state = latest?.state ?: return
        if (state.isEnded || isLeaving) return
        isLeaving = true
        val winner = sidesAtTarget(state).singleOrNull()
        append(eventFactory.gameEnded(winner)) { navigation.send(TrackerNavigationEvent.Exit) }
    }

    /**
     * Ends this game without a winner and starts a new one with the same players and current target, in
     * one transaction (a crash can't leave two open games).
     */
    fun onRestart() {
        val snapshot = latest ?: return
        if (isLeaving) return
        isLeaving = true
        val config = snapshot.record.config.copy(targetScore = snapshot.state.targetScore)
        val end = eventFactory.gameEnded(winner = null)
        if (!snapshot.state.isEnded) applyOptimistically(end)
        write {
            val newGameId = gameRepository.restartGame(gameId, end, config)
            navigation.send(TrackerNavigationEvent.OpenGame(newGameId))
        }
    }

    /**
     * Applies [event] to [latest] right away, then persists it and runs [afterWrite]. Room's re-emission
     * confirms it (see [awaitingSync]).
     */
    private fun append(event: GameEvent, afterWrite: suspend () -> Unit = {}) {
        applyOptimistically(event)
        write {
            gameRepository.appendEvent(gameId, event)
            afterWrite()
        }
    }

    /** Runs [block] (a repository write), counted in [pendingWrites]. */
    private fun write(block: suspend () -> Unit) {
        pendingWrites++
        viewModelScope.launch {
            try {
                block()
            } finally {
                pendingWrites--
                if (pendingWrites == 0) writesSettled.update { it + 1 }
            }
        }
    }

    private fun applyOptimistically(event: GameEvent) {
        latest?.let { snapshot ->
            val events = snapshot.record.events + event
            latest = Snapshot(snapshot.record.copy(events = events), reduce(snapshot.record.config, events))
        }
        awaitingSync = true
    }

    private data class Snapshot(val record: GameRecord, val state: GameState)
}

@Suppress("LongParameterList")
private fun buildUiState(
    record: GameRecord,
    initial: GameState,
    entries: List<HistoryEntry>,
    state: GameState,
    legends: List<Legend>,
    settings: UserSettings,
    dismissed: Set<SideId>,
): TrackerUiState.Success {
    val legendsById = legends.associateBy { it.id }
    val playersById = record.config.players.associateBy { it.id }
    val atTarget = sidesAtTarget(state)
    val panels = record.config.sides.map { side ->
        SidePanelUiState(
            sideId = side.id,
            points = state.pointsOf(side.id),
            isAtTarget = side.id in atTarget,
            players = side.playerIds.mapNotNull(playersById::get).map { player ->
                val legend = player.legendId?.let(legendsById::get)
                PanelPlayerUiState(
                    playerId = player.id,
                    name = player.name,
                    colorId = player.colorId,
                    legendName = legend?.name,
                    legendImageUri = legend?.imageUri,
                    legendWideImageUri = legend?.wideImageUri,
                    counters = state.countersOf(player.id)
                        .map { CounterUiState(id = it.id, label = it.label, value = it.value) }
                        .toImmutableList(),
                )
            }.toImmutableList(),
        )
    }
    val sideNames = panels.associate { it.sideId to it.displayName }
    val showBanner = !state.isEnded && atTarget.isNotEmpty() && !dismissed.containsAll(atTarget)
    return TrackerUiState.Success(
        panels = panels.toImmutableList(),
        targetScore = state.targetScore,
        canUndo = record.events.isNotEmpty(),
        winnerBanner = if (showBanner) {
            WinnerBannerUiState(
                atTarget.mapNotNull(sideNames::get).toImmutableList(),
            )
        } else {
            null
        },
        history = gameLog(record.config, entries, sideNames, initial).asReversed().toImmutableList(),
        hapticsEnabled = settings.hapticsEnabled,
        keepScreenOn = settings.keepScreenOn,
        immersiveMode = settings.immersiveMode,
    )
}

/** Navigation requested by [TrackerViewModel]. */
internal sealed interface TrackerNavigationEvent {
    /** The game ended: leave the tracker. */
    data object Exit : TrackerNavigationEvent

    /** The game was restarted as [gameId]: show that game instead. */
    data class OpenGame(val gameId: GameId) : TrackerNavigationEvent
}
