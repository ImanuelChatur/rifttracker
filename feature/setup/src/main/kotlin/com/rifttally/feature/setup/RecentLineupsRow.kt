package com.rifttally.feature.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * "Recent lineups": one tonal tile per lineup in a sideways-scrolling row, newest first. Tapping a tile
 * fills the whole form. Each tile reads "Ana · Ben · Cleo, 3 players" with a dot per player color.
 */
@Composable
internal fun RecentLineupsRow(
    lineups: ImmutableList<RecentLineupUiState>,
    onApply: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        lineups.forEach { lineup -> LineupTile(lineup = lineup, onClick = { onApply(lineup.index) }) }
    }
}

@Composable
private fun LineupTile(lineup: RecentLineupUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val names = lineup.names.joinToString(separator = stringResource(R.string.feature_setup_lineup_separator))
    val format = if (lineup.format.isTeams) {
        stringResource(R.string.feature_setup_format_teams)
    } else {
        pluralStringResource(
            R.plurals.feature_setup_format_players_description,
            lineup.format.playerCount,
            lineup.format.playerCount,
        )
    }
    val label = stringResource(R.string.feature_setup_lineup_label, names, format)
    val description = stringResource(R.string.feature_setup_lineup_apply, label)
    Surface(
        onClick = onClick,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick {
                onClick()
                true
            }
        },
        shape = RiftTheme.shapes.large,
        color = RiftTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = RiftTheme.sizes.minTouchTarget)
                .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs)) {
                lineup.colorIds.forEach { id ->
                    Box(
                        modifier = Modifier
                            .size(RiftTheme.spacing.sm)
                            .background((PlayerColors.fromId(id) ?: PlayerColors.Grey).color, CircleShape),
                    )
                }
            }
            Text(
                text = names,
                style = RiftTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = format,
                style = RiftTheme.typography.bodySmall,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@PreviewRiftThemes
@Composable
private fun RecentLineupsRowPreview() {
    RiftTheme {
        RecentLineupsRow(
            lineups = persistentListOf(
                RecentLineupUiState(
                    0,
                    persistentListOf("Ana", "Ben"),
                    persistentListOf("orange", "sky"),
                    GameFormat.TWO_PLAYERS,
                ),
                RecentLineupUiState(
                    1,
                    persistentListOf("Ana", "Ben", "Cleo"),
                    persistentListOf("orange", "sky", "green"),
                    GameFormat.THREE_PLAYERS,
                ),
            ),
            onApply = {},
        )
    }
}
