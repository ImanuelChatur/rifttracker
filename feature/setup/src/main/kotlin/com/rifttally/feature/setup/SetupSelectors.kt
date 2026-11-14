package com.rifttally.feature.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftConnectedToggleButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat

/**
 * 2 / 3 / 4 players / 2v2 as a connected button group (DESIGN.md §9; selection morphs on the expressive
 * spring, §7). Each option is announced in full ("4 players").
 */
@Composable
internal fun FormatSelector(selected: GameFormat, onSelect: (GameFormat) -> Unit, modifier: Modifier = Modifier) {
    val formats = GameFormat.entries
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
    ) {
        formats.forEachIndexed { index, format ->
            val description = if (format.isTeams) {
                stringResource(R.string.feature_setup_format_teams_description)
            } else {
                pluralStringResource(
                    R.plurals.feature_setup_format_players_description,
                    format.playerCount,
                    format.playerCount,
                )
            }
            RiftConnectedToggleButton(
                text = stringResource(format.labelRes),
                selected = format == selected,
                onClick = { onSelect(format) },
                index = index,
                count = formats.size,
                contentDescription = description,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Team A / Team B for one player in 2v2, as a connected button group. */
@Composable
internal fun TeamSelector(selected: Team, onSelect: (Team) -> Unit, modifier: Modifier = Modifier) {
    val teams = Team.entries
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
    ) {
        teams.forEachIndexed { index, team ->
            RiftConnectedToggleButton(
                text = stringResource(team.labelRes),
                selected = team == selected,
                onClick = { onSelect(team) },
                index = index,
                count = teams.size,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@get:StringRes
private val GameFormat.labelRes: Int
    get() = when (this) {
        GameFormat.TWO_PLAYERS -> R.string.feature_setup_format_two
        GameFormat.THREE_PLAYERS -> R.string.feature_setup_format_three
        GameFormat.FOUR_PLAYERS -> R.string.feature_setup_format_four
        GameFormat.TEAMS_2V2 -> R.string.feature_setup_format_teams
    }

@get:StringRes
private val Team.labelRes: Int
    get() = when (this) {
        Team.A -> R.string.feature_setup_team_a
        Team.B -> R.string.feature_setup_team_b
    }

@PreviewRiftThemes
@Composable
private fun SelectorsPreview() {
    RiftTheme {
        Surface {
            Column(
                modifier = Modifier.padding(RiftTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                FormatSelector(selected = GameFormat.TEAMS_2V2, onSelect = {})
                TeamSelector(selected = Team.B, onSelect = {})
            }
        }
    }
}
