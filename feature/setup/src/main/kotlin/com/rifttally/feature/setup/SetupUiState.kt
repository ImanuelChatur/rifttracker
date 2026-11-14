package com.rifttally.feature.setup

import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Everything the setup screen shows. */
internal data class SetupUiState(
    val format: GameFormat = GameFormat.TWO_PLAYERS,
    /** Players in seat order. */
    val players: ImmutableList<SetupPlayerUiState> = persistentListOf(),
    val targetScore: Int = 0,
    val canDecreaseTarget: Boolean = false,
    /** Open Legend picker, or `null` when closed. */
    val legendPicker: LegendPickerUiState? = null,
    /** "Add XP counter to all players" (remembered across games). */
    val addXpCounter: Boolean = false,
    /** 2v2 only: teams aren't two players each, so the game can't start yet. */
    val teamsInvalid: Boolean = false,
    val canStart: Boolean = true,
    val isStarting: Boolean = false,
)

/**
 * One player row. [name] may be blank: the screen shows "Player N" and uses it on start.
 * [team] is set only in 2v2.
 */
internal data class SetupPlayerUiState(
    val index: Int,
    val name: String,
    val colorId: String,
    val legend: Legend?,
    val team: Team? = null,
)

/** The two 2v2 teams. [sideId] is the stable id of the team's shared point total. */
internal enum class Team(val sideId: String) {
    A("team_a"),
    B("team_b"),
}

/** The searchable Legend grid for [playerIndex]. [legends] is already filtered by [query]. */
internal data class LegendPickerUiState(
    val playerIndex: Int,
    val query: String,
    val legends: ImmutableList<Legend>,
    val selectedLegendId: String?,
)
