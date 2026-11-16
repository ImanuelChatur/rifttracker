package com.rifttally.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.data.timer.NewGameTimer
import com.rifttally.core.data.timer.RoundTimerChoice
import com.rifttally.core.data.timer.RoundTimerRepository
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.MAX_PLAYER_NAME_LENGTH
import com.rifttally.core.model.MIN_TARGET_SCORE
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Game setup: format (2/3/4 players or 2v2), players in seat order (name, color, Legend, team in 2v2),
 * the target score, and the remembered "XP counter for all" toggle and "Round timer" choice.
 */
@HiltViewModel
internal class SetupViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    legendRepository: LegendRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val presetRepository: PlayerPresetRepository,
    private val firstPlayerPicker: FirstPlayerPicker,
    private val timerRepository: RoundTimerRepository,
) : ViewModel() {

    private val form = MutableStateFlow(SetupForm(format = GameFormat.TWO_PLAYERS).resizedTo(GameFormat.TWO_PLAYERS))

    /** Saved players as last emitted, for [onPresetApply]. */
    private var latestPresets: List<PlayerPreset> = emptyList()

    /** Recent lineups as last shown, for [onLineupApply]. */
    private var latestLineups: List<GameConfig> = emptyList()

    /** The active game (if any) and recent ended games: the source of "Recent lineups". */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val lineups = combine(gameRepository.activeGame, gameRepository.recentGames(LINEUP_SOURCE_GAMES)) {
            active,
            ended,
        ->
        recentLineups(listOfNotNull(active) + ended)
    }

    /** Whether the user picked a format, so the saved default doesn't override their choice. */
    private var formatChosen = false

    private val navigation = Channel<SetupNavigationEvent>(Channel.BUFFERED)

    /** One-off navigation requests (e.g. open the tracker for a new game). */
    val navigationEvents: Flow<SetupNavigationEvent> = navigation.receiveAsFlow()

    /** The remembered options: "XP counter for all" and the round timer. */
    private val options = combine(userSettingsRepository.userSettings, timerRepository.choice) { settings, timer ->
        RememberedOptions(addXpCounter = settings.addXpCounterByDefault, roundTimer = timer)
    }

    val uiState: StateFlow<SetupUiState> = combine(
        form,
        legendRepository.legends,
        options,
        presetRepository.presets,
        lineups,
    ) { form, legends, options, presets, lineups ->
        latestLineups = lineups
        form.toUiState(legends, options, presets, lineups)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            form.value.toUiState(emptyList(), RememberedOptions(), emptyList(), emptyList()),
        )

    /**
     * Remembers player [index] (name, color, Legend) as a saved player (updating the one with that name,
     * if any) and links the seat to it, so this game already counts for that saved player in stats.
     * Blank names are ignored.
     */
    fun onPlayerSave(index: Int) {
        val player = form.value.players.getOrNull(index) ?: return
        if (player.name.isBlank()) return
        viewModelScope.launch {
            val presetId = presetRepository.save(player.name, player.colorId, player.legendId) ?: return@launch
            // The form may have changed meanwhile (e.g. another format); link only the same player.
            updatePlayer(index) { seat ->
                if (seat.name.trim() == player.name.trim()) {
                    seat.copy(presetId = presetId, presetName = player.name.trim())
                } else {
                    seat
                }
            }
        }
    }

    /** Fills player [index] from saved player [presetId] and links the seat to it (for stats). */
    fun onPresetApply(index: Int, presetId: String) {
        val preset = latestPresets.firstOrNull { it.id == presetId } ?: return
        updatePlayer(index) {
            it.copy(
                name = preset.name,
                colorId = preset.colorId,
                legendId = preset.legendId,
                presetId = preset.id,
                presetName = preset.name,
            )
        }
    }

    /**
     * Fills the whole form from recent lineup [index]: format, seats (name, color, Legend, team) and that
     * game's target. A seat that had a placeholder name ("Player 2") gets a blank name; a seat stays
     * linked to its saved player only if that saved player still exists under the same name.
     */
    fun onLineupApply(index: Int) {
        val config = latestLineups.getOrNull(index) ?: return
        formatChosen = true
        form.update { current ->
            val count = config.players.size
            current.copy(
                format = config.format,
                players = config.players.mapIndexed { seat, player ->
                    val preset = player.presetId
                        ?.let { id -> latestPresets.firstOrNull { it.id == id } }
                        ?.takeIf { it.name.trim().equals(player.name.trim(), ignoreCase = true) }
                    val sideId = config.sides.firstOrNull { player.id in it.playerIds }?.id?.value
                    PlayerForm(
                        name = if (player.hasDefaultName) "" else player.name,
                        colorId = player.colorId,
                        legendId = player.legendId,
                        team = Team.entries.firstOrNull { it.sideId == sideId }
                            ?: if (seat < count / 2) Team.A else Team.B,
                        presetId = preset?.id,
                        presetName = preset?.name,
                    )
                },
                targetScore = config.targetScore,
                pickerFor = null,
                legendQuery = "",
            )
        }
    }

    init {
        viewModelScope.launch {
            val defaultFormat = userSettingsRepository.userSettings.first().defaultGameFormat
            if (!formatChosen) applyFormat(defaultFormat)
        }
    }

    /** Switches format: resizes the player list (keeping entered players) and resets the target score. */
    fun onFormatSelect(format: GameFormat) {
        formatChosen = true
        applyFormat(format)
    }

    /** Renaming a seat to a different name unlinks it from its saved player (checked in toConfig). */
    fun onNameChange(index: Int, name: String) =
        updatePlayer(index) { it.copy(name = name.take(MAX_PLAYER_NAME_LENGTH)) }

    fun onColorSelect(index: Int, colorId: String) = updatePlayer(index) { it.copy(colorId = colorId) }

    fun onTeamSelect(index: Int, team: Team) = updatePlayer(index) { it.copy(team = team) }

    fun onLegendPickerOpen(index: Int) = form.update { it.copy(pickerFor = index, legendQuery = "") }

    fun onLegendQueryChange(query: String) = form.update { it.copy(legendQuery = query) }

    /** Picks [legendId] (or none, if `null`) for the player whose picker is open, and closes the picker. */
    fun onLegendSelect(legendId: String?) {
        val index = form.value.pickerFor ?: return
        updatePlayer(index) { it.copy(legendId = legendId) }
        onLegendPickerDismiss()
    }

    fun onLegendPickerDismiss() = form.update { it.copy(pickerFor = null, legendQuery = "") }

    fun onTargetScoreIncrease() = form.update { it.copy(targetScore = it.targetScore + 1) }

    fun onTargetScoreDecrease() =
        form.update { it.copy(targetScore = (it.targetScore - 1).coerceAtLeast(MIN_TARGET_SCORE)) }

    /** Remembered for next time. */
    fun onAddXpCounterToggle(enabled: Boolean) {
        viewModelScope.launch { userSettingsRepository.setAddXpCounterByDefault(enabled) }
    }

    /** "Round timer" on or off. Remembered for next time, with the last length. */
    fun onRoundTimerToggle(enabled: Boolean) {
        viewModelScope.launch { timerRepository.setChoiceEnabled(enabled) }
    }

    /** Round timer length +5 minutes (up to 120). Remembered. */
    fun onRoundTimerIncrease() {
        viewModelScope.launch { timerRepository.adjustChoiceMinutes(RoundTimerChoice.STEP_MINUTES) }
    }

    /** Round timer length −5 minutes (down to 5). Remembered. */
    fun onRoundTimerDecrease() {
        viewModelScope.launch { timerRepository.adjustChoiceMinutes(-RoundTimerChoice.STEP_MINUTES) }
    }

    /**
     * Creates the game and asks to open the tracker. [defaultNames] are the localized "Player N" names
     * used for players left blank (index-aligned); [xpLabel] is the localized "XP" for the optional counter.
     */
    fun onStartGame(defaultNames: List<String>, xpLabel: String) {
        val current = form.value
        if (current.isStarting || !current.canStart()) return
        form.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            val addXp = userSettingsRepository.userSettings.first().addXpCounterByDefault
            val config = current.toConfig(defaultNames, xpLabel.takeIf { addXp })
            // A new game from Setup always starts a fresh timer (full length, paused), stored with the game.
            val timerChoice = timerRepository.choice.first()
            val timer = if (timerChoice.enabled) NewGameTimer.Fresh(timerChoice.minutes) else NewGameTimer.None
            val gameId = gameRepository.createGame(config, firstPlayerPicker.openingEvents(config), timer)
            navigation.send(SetupNavigationEvent.OpenTracker(gameId))
        }
    }

    private fun applyFormat(format: GameFormat) = form.update { state ->
        if (state.format == format) state else state.resizedTo(format)
    }

    private fun updatePlayer(index: Int, transform: (PlayerForm) -> PlayerForm) = form.update { state ->
        if (index !in state.players.indices) return@update state
        state.copy(players = state.players.mapIndexed { i, player -> if (i == index) transform(player) else player })
    }

    private fun SetupForm.toUiState(
        legends: List<Legend>,
        options: RememberedOptions,
        presets: List<PlayerPreset>,
        lineups: List<GameConfig>,
    ): SetupUiState {
        latestPresets = presets
        val byId = legends.associateBy { it.id }
        val pickerIndex = pickerFor
        return SetupUiState(
            format = format,
            players = players.mapIndexed { index, player ->
                SetupPlayerUiState(
                    index = index,
                    name = player.name,
                    colorId = player.colorId,
                    legend = player.legendId?.let(byId::get),
                    team = if (format.isTeams) player.team else null,
                )
            }.toImmutableList(),
            targetScore = targetScore,
            canDecreaseTarget = targetScore > MIN_TARGET_SCORE,
            legendPicker = pickerIndex?.let { index ->
                val query = legendQuery.trim()
                LegendPickerUiState(
                    playerIndex = index,
                    query = legendQuery,
                    legends = legends.filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
                        .toImmutableList(),
                    selectedLegendId = players.getOrNull(index)?.legendId,
                )
            },
            addXpCounter = options.addXpCounter,
            roundTimer = RoundTimerSetupUiState(
                enabled = options.roundTimer.enabled,
                minutes = options.roundTimer.minutes,
                canDecrease = options.roundTimer.minutes > RoundTimerChoice.MIN_MINUTES,
                canIncrease = options.roundTimer.minutes < RoundTimerChoice.MAX_MINUTES,
            ),
            presets = presets.map { SavedPlayerUiState(id = it.id, name = it.name) }.toImmutableList(),
            recentLineups = lineups.mapIndexed { index, config ->
                RecentLineupUiState(
                    index = index,
                    names = config.players.map { it.name }.toImmutableList(),
                    colorIds = config.players.map { it.colorId }.toImmutableList(),
                    format = config.format,
                )
            }.toImmutableList(),
            teamsInvalid = format.isTeams && !canStart(),
            canStart = canStart() && !isStarting,
            isStarting = isStarting,
        )
    }

    private fun SetupForm.canStart(): Boolean =
        !format.isTeams || Team.entries.all { team -> players.count { it.team == team } == TEAM_SIZE }

    private fun SetupForm.toConfig(defaultNames: List<String>, xpLabel: String?): GameConfig {
        val gamePlayers = players.mapIndexed { index, player ->
            val typed = player.name.trim()
            Player(
                id = PlayerId("p${index + 1}"),
                name = typed.ifEmpty { defaultNames.getOrElse(index) { "${index + 1}" } },
                colorId = player.colorId,
                legendId = player.legendId,
                presetId = player.presetId?.takeIf { player.presetName.equals(typed, ignoreCase = true) },
                hasDefaultName = typed.isEmpty(),
            )
        }
        val sides = if (format.isTeams) {
            Team.entries.map { team ->
                Side(
                    id = SideId(team.sideId),
                    playerIds = gamePlayers.filterIndexed { index, _ -> players[index].team == team }.map { it.id },
                )
            }
        } else {
            gamePlayers.mapIndexed { index, player -> Side(SideId("s${index + 1}"), listOf(player.id)) }
        }
        return GameConfig(
            format = format,
            sides = sides,
            players = gamePlayers,
            targetScore = targetScore,
            initialCounters = if (xpLabel == null) {
                emptyMap()
            } else {
                gamePlayers.associate { it.id to listOf(Counter(id = XP_COUNTER_ID, label = xpLabel)) }
            },
        )
    }

    /**
     * One seat. [presetId] links it to a saved player (for stats) while the typed [name] still equals
     * [presetName], the saved player's name when it was loaded or saved (trimmed, ignoring case); renaming
     * the seat unlinks it. Kept here rather than looked up, so a just-saved player is linked at once.
     */
    private data class PlayerForm(
        val name: String,
        val colorId: String,
        val legendId: String?,
        val team: Team,
        val presetId: String? = null,
        val presetName: String? = null,
    )

    private data class SetupForm(
        val format: GameFormat,
        val players: List<PlayerForm> = emptyList(),
        val targetScore: Int = format.defaultTargetScore,
        val pickerFor: Int? = null,
        val legendQuery: String = "",
        val isStarting: Boolean = false,
    ) {
        /**
         * Same entered players (extra seats get defaults), the new format's default target, and the
         * default teams: the first half of the seats on Team A, the rest on Team B.
         */
        fun resizedTo(newFormat: GameFormat): SetupForm {
            val count = newFormat.playerCount
            val kept = players.take(count)
            // New seats get the first colors nobody kept is using, so no two players start alike.
            val freeColors = PlayerColors.all.map { it.id }.filterNot { id -> kept.any { it.colorId == id } }
            val resized = List(count) { index ->
                val team = if (index < count / 2) Team.A else Team.B
                kept.getOrNull(index)?.copy(team = team)
                    ?: PlayerForm(
                        name = "",
                        colorId = freeColors.getOrElse(index - kept.size) { PlayerColors.forSeat(index).id },
                        legendId = null,
                        team = team,
                    )
            }
            return copy(
                format = newFormat,
                players = resized,
                targetScore = newFormat.defaultTargetScore,
                pickerFor = null,
            )
        }
    }

    private data class RememberedOptions(
        val addXpCounter: Boolean = false,
        val roundTimer: RoundTimerChoice = RoundTimerChoice(),
    )

    private companion object {
        const val TEAM_SIZE = 2

        /** Recent ended games scanned for distinct lineups (repeats of one lineup are common). */
        const val LINEUP_SOURCE_GAMES = 50
        val XP_COUNTER_ID = CounterId("xp")
    }
}

/** Navigation requested by [SetupViewModel]. */
internal sealed interface SetupNavigationEvent {
    data class OpenTracker(val gameId: GameId) : SetupNavigationEvent
}
