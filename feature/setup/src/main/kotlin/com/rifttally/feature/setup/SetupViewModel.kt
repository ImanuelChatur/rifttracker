package com.rifttally.feature.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.game.GameRepository
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.MIN_TARGET_SCORE
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Game setup: format (2/3/4 players or 2v2), players in seat order (name, color, Legend, team in 2v2),
 * the target score, and the remembered "XP counter for all" toggle.
 */
@HiltViewModel
internal class SetupViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    legendRepository: LegendRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val presetRepository: PlayerPresetRepository,
) : ViewModel() {

    private val form = MutableStateFlow(SetupForm(format = GameFormat.TWO_PLAYERS).resizedTo(GameFormat.TWO_PLAYERS))

    /** Saved players as last emitted, for [onPresetApply]. */
    private var latestPresets: List<PlayerPreset> = emptyList()

    /** Whether the user picked a format, so the saved default doesn't override their choice. */
    private var formatChosen = false

    private val navigation = Channel<SetupNavigationEvent>(Channel.BUFFERED)

    /** One-off navigation requests (e.g. open the tracker for a new game). */
    val navigationEvents: Flow<SetupNavigationEvent> = navigation.receiveAsFlow()

    val uiState: StateFlow<SetupUiState> = combine(
        form,
        legendRepository.legends,
        userSettingsRepository.userSettings,
        presetRepository.presets,
    ) { form, legends, settings, presets -> form.toUiState(legends, settings.addXpCounterByDefault, presets) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            form.value.toUiState(emptyList(), false, emptyList()),
        )

    /** Remembers player [index] (name, color, Legend) as a saved player. Blank names are ignored. */
    fun onPlayerSave(index: Int) {
        val player = form.value.players.getOrNull(index) ?: return
        if (player.name.isBlank()) return
        viewModelScope.launch { presetRepository.save(player.name, player.colorId, player.legendId) }
    }

    /** Fills player [index] from saved player [presetId] and links the seat to it (for stats). */
    fun onPresetApply(index: Int, presetId: String) {
        val preset = latestPresets.firstOrNull { it.id == presetId } ?: return
        updatePlayer(index) {
            it.copy(name = preset.name, colorId = preset.colorId, legendId = preset.legendId, presetId = preset.id)
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

    /** Renaming a seat makes it someone else, so it's no longer linked to a loaded saved player. */
    fun onNameChange(index: Int, name: String) =
        updatePlayer(index) { it.copy(name = name.take(MAX_NAME_LENGTH), presetId = null) }

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
            val gameId = gameRepository.createGame(current.toConfig(defaultNames, xpLabel.takeIf { addXp }))
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
        addXpCounter: Boolean,
        presets: List<PlayerPreset>,
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
            addXpCounter = addXpCounter,
            presets = presets.map { SavedPlayerUiState(id = it.id, name = it.name) }.toImmutableList(),
            teamsInvalid = format.isTeams && !canStart(),
            canStart = canStart() && !isStarting,
            isStarting = isStarting,
        )
    }

    private fun SetupForm.canStart(): Boolean =
        !format.isTeams || Team.entries.all { team -> players.count { it.team == team } == TEAM_SIZE }

    private fun SetupForm.toConfig(defaultNames: List<String>, xpLabel: String?): GameConfig {
        val gamePlayers = players.mapIndexed { index, player ->
            Player(
                id = PlayerId("p${index + 1}"),
                name = player.name.trim().ifEmpty { defaultNames.getOrElse(index) { "${index + 1}" } },
                colorId = player.colorId,
                legendId = player.legendId,
                presetId = player.presetId,
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

    private data class PlayerForm(
        val name: String,
        val colorId: String,
        val legendId: String?,
        val team: Team,
        val presetId: String? = null,
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

    private companion object {
        const val MAX_NAME_LENGTH = 24
        const val TEAM_SIZE = 2
        val XP_COUNTER_ID = CounterId("xp")
    }
}

/** Navigation requested by [SetupViewModel]. */
internal sealed interface SetupNavigationEvent {
    data class OpenTracker(val gameId: GameId) : SetupNavigationEvent
}
