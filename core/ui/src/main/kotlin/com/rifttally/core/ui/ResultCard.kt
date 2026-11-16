package com.rifttally.core.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.headlineMediumEmphasized
import com.rifttally.core.model.GameFormat
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * A shareable picture of one finished game: date and format, who won, every side with Legend portraits
 * (or the fallback circle with an initial), names in player colors and final scores, the winner
 * highlighted, and a small "unofficial fan app" footer.
 *
 * Always dark and always [ResultCardWidth] × [ResultCardHeight], so the image looks the same from every
 * phone (the share helper also fixes density and font scale). Portraits come from [portraits] (keyed by
 * [ResultCardPlayer.legendImageUri]) rather than being loaded here, so the card is complete on its first
 * frame; a missing one shows the fallback.
 */
@Composable
public fun ResultCard(
    state: ResultCardState,
    modifier: Modifier = Modifier,
    portraits: ImmutableMap<String, ImageBitmap> = persistentMapOf(),
) {
    RiftTheme(darkTheme = true) {
        Surface(
            modifier = modifier.requiredSize(ResultCardWidth, ResultCardHeight),
            color = RiftTheme.colorScheme.background,
            contentColor = RiftTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier.padding(RiftTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
            ) {
                Text(
                    text = stringResource(
                        R.string.core_ui_result_header,
                        dateText(state.endedAt),
                        formatText(state.format),
                    ),
                    style = RiftTheme.typography.labelLarge,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.winner?.let { stringResource(R.string.core_ui_result_won, it.name) }
                        ?: stringResource(R.string.core_ui_result_no_winner),
                    style = RiftTheme.typography.headlineMediumEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(RiftTheme.spacing.xs))
                state.sides.forEach { side -> ResultCardSideRow(side = side, portraits = portraits) }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.core_ui_result_footer),
                    style = RiftTheme.typography.labelMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Width of [ResultCard] (4:5 with [ResultCardHeight], a common format for shared pictures). */
public val ResultCardWidth: Dp = 360.dp

/** Height of [ResultCard]. */
public val ResultCardHeight: Dp = 450.dp

/**
 * One side: portraits, names in player colors, Legends, and the score. The winner sits on a lighter
 * container with a primary outline, a star and an emphasized score; player colors keep AA contrast on
 * both containers (see `ResultCardTest`).
 */
@Composable
private fun ResultCardSideRow(side: ResultCardSide, portraits: ImmutableMap<String, ImageBitmap>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.large,
        color = if (side.isWinner) {
            RiftTheme.colorScheme.surfaceContainer
        } else {
            RiftTheme.colorScheme.surfaceContainerLow
        },
        border = if (side.isWinner) BorderStroke(RiftTheme.sizes.borderThick, RiftTheme.colorScheme.primary) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.md, vertical = RiftTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs)) {
                side.players.forEach { player ->
                    ResultCardPortrait(player = player, image = player.legendImageUri?.let(portraits::get))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coloredNames(side),
                    style = RiftTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val legends = side.players.mapNotNull { it.legendName }
                if (legends.isNotEmpty()) {
                    Text(
                        text = legends.joinToString(TEAM_SEPARATOR),
                        style = RiftTheme.typography.bodySmall,
                        color = RiftTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (side.isWinner) {
                Icon(
                    imageVector = RiftIcons.Star,
                    contentDescription = stringResource(R.string.core_ui_result_winner_icon),
                    tint = RiftTheme.colorScheme.primary,
                    modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                )
            }
            Text(
                text = side.score.toString(),
                style = if (side.isWinner) {
                    RiftTheme.typography.headlineMediumEmphasized
                } else {
                    RiftTheme.typography.headlineMedium
                }.copy(fontFeatureSettings = "tnum"),
                color = if (side.isWinner) RiftTheme.colorScheme.primary else RiftTheme.colorScheme.onSurface,
            )
        }
    }
}

/** "Ana & Ben", each name in its player color and the separator muted. */
@Composable
private fun coloredNames(side: ResultCardSide) = buildAnnotatedString {
    val separatorColor = RiftTheme.colorScheme.onSurfaceVariant
    side.players.forEachIndexed { index, player ->
        if (index > 0) withStyle(SpanStyle(color = separatorColor)) { append(TEAM_SEPARATOR) }
        withStyle(SpanStyle(color = player.color.color)) { append(player.name) }
    }
}

/** The Legend portrait with a ring in the player's color, or the fallback circle with an initial. */
@Composable
private fun ResultCardPortrait(player: ResultCardPlayer, image: ImageBitmap?) {
    val color = player.color
    val size = RiftTheme.sizes.avatarSmall
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RiftTheme.shapes.full)
                .border(RiftTheme.sizes.borderThick, color.color, RiftTheme.shapes.full),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(color.color, RiftTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitial(player.legendName, player.name),
                style = RiftTheme.typography.titleMedium,
                color = color.onColor,
            )
        }
    }
}

private val ResultCardPlayer.color get() = PlayerColors.fromId(colorId) ?: PlayerColors.Grey

/** Localized date without time, e.g. "Nov 14, 2026". */
@Composable
private fun dateText(epochMillis: Long): String = DateUtils.formatDateTime(
    LocalContext.current,
    epochMillis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
)

/** "2 players", "3 players", "4 players" or "2v2". */
@Composable
private fun formatText(format: GameFormat): String = if (format.isTeams) {
    stringResource(R.string.core_ui_result_format_teams)
} else {
    pluralStringResource(R.plurals.core_ui_result_format_players, format.playerCount, format.playerCount)
}

// region Previews

internal val previewResultCardState = ResultCardState(
    format = GameFormat.FOUR_PLAYERS,
    endedAt = 1_763_150_400_000,
    sides = persistentListOf(
        ResultCardSide(persistentListOf(ResultCardPlayer("Ana", "orange", "Jinx", null)), 8, isWinner = true),
        ResultCardSide(persistentListOf(ResultCardPlayer("Ben", "sky", null, null)), 5, isWinner = false),
        ResultCardSide(persistentListOf(ResultCardPlayer("Cleo", "green", "Viktor", null)), 3, isWinner = false),
        ResultCardSide(persistentListOf(ResultCardPlayer("Dan", "pink", "Ahri", null)), 0, isWinner = false),
    ),
)

@PreviewRiftThemes
@Composable
private fun ResultCardPreview() {
    ResultCard(state = previewResultCardState)
}

@PreviewRiftThemes
@Composable
private fun ResultCardTeamsPreview() {
    ResultCard(
        state = ResultCardState(
            format = GameFormat.TEAMS_2V2,
            endedAt = 1_763_150_400_000,
            sides = persistentListOf(
                ResultCardSide(
                    persistentListOf(
                        ResultCardPlayer("Ana", "orange", "Jinx", null),
                        ResultCardPlayer("Ben", "sky", null, null),
                    ),
                    11,
                    isWinner = false,
                ),
                ResultCardSide(
                    persistentListOf(
                        ResultCardPlayer("Cleo", "green", "Viktor", null),
                        ResultCardPlayer("Dan", "pink", "Ahri", null),
                    ),
                    7,
                    isWinner = false,
                ),
            ),
        ),
    )
}

// endregion
