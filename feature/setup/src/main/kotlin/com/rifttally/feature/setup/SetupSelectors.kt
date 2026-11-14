package com.rifttally.feature.setup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat

/** 2 / 3 / 4 players / 2v2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormatSelector(selected: GameFormat, onSelect: (GameFormat) -> Unit, modifier: Modifier = Modifier) {
    val formats = GameFormat.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
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
            SegmentedButton(
                selected = format == selected,
                modifier = Modifier.semantics { contentDescription = description },
                onClick = { onSelect(format) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
                label = { Text(stringResource(format.labelRes)) },
            )
        }
    }
}

/** Team A / Team B for one player in 2v2. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeamSelector(selected: Team, onSelect: (Team) -> Unit, modifier: Modifier = Modifier) {
    val teams = Team.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        teams.forEachIndexed { index, team ->
            SegmentedButton(
                selected = team == selected,
                onClick = { onSelect(team) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = teams.size),
                label = { Text(stringResource(team.labelRes)) },
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
            Column {
                FormatSelector(selected = GameFormat.TEAMS_2V2, onSelect = {})
                TeamSelector(selected = Team.B, onSelect = {})
            }
        }
    }
}
