package com.rifttally.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.LegendBackdrop
import com.rifttally.core.ui.PlayerNameChip
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun HomeRoute(
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNewGameClick = onNewGameClick,
        onResumeClick = onResumeClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
    )
}

/**
 * Home: a hero "Resume" card with the Legends' art side by side (when a game is in progress), the
 * "New game" button, and recent games as a compact list with tiny Legend avatars, winner highlighted.
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RiftTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RiftTheme.spacing.xl, vertical = RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.feature_home_title),
                    style = RiftTheme.typography.headlineLarge,
                    color = RiftTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                RiftIconButton(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.feature_home_settings),
                    onClick = onSettingsClick,
                )
            }
            val success = uiState as? HomeUiState.Success
            success?.activeGame?.let { activeGame ->
                ResumeCard(game = activeGame, onResumeClick = { onResumeClick(activeGame.gameId) })
            }
            RiftButton(
                text = stringResource(R.string.feature_home_new_game),
                onClick = onNewGameClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = success != null,
            )
            val recentGames = success?.recentGames.orEmpty()
            if (recentGames.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.feature_home_recent_games),
                    style = RiftTheme.typography.titleLarge,
                    color = RiftTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = RiftTheme.spacing.sm)
                        .semantics { heading() },
                )
                Surface(
                    shape = RoundedCornerShape(RiftTheme.sizes.cardCorner),
                    color = RiftTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column {
                        recentGames.forEachIndexed { index, game ->
                            if (index > 0) HorizontalDivider(color = RiftTheme.colorScheme.outlineVariant)
                            RecentGameRow(game = game, now = success?.now ?: 0)
                        }
                    }
                }
            }
        }
    }
}

/** Hero card: each side's Legend art (or color fallback) with score and name, then "Resume". */
@Composable
private fun ResumeCard(game: ActiveGameUiState, onResumeClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RiftTheme.sizes.panelCorner),
        colors = CardDefaults.cardColors(containerColor = RiftTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RiftTheme.sizes.heroArtHeight),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
        ) {
            game.sides.forEachIndexed { index, side ->
                ResumeSide(
                    side = side,
                    seatIndex = index,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    text = stringResource(R.string.feature_home_game_in_progress),
                    style = RiftTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.feature_home_target, game.targetScore),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiftButton(
                text = stringResource(R.string.feature_home_resume),
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResumeSide(side: ResumeSideUiState, seatIndex: Int, modifier: Modifier = Modifier) {
    val playerColor = PlayerColors.fromId(side.colorId) ?: PlayerColors.forSeat(seatIndex)
    val tableColors = RiftTheme.tableColors
    LegendBackdrop(
        legendName = side.legendName,
        imageUri = side.legendImageUri,
        fallbackName = side.name,
        playerColor = playerColor,
        modifier = modifier,
    ) { hasArt ->
        val shadow = if (hasArt) {
            with(LocalDensity.current) {
                Shadow(tableColors.artTextShadow, Offset(0f, RiftTheme.spacing.xxs.toPx()), RiftTheme.spacing.md.toPx())
            }
        } else {
            null
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(RiftTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
        ) {
            Text(
                text = side.points.toString(),
                style = RiftTheme.scoreTypography.scoreMedium.copy(shadow = shadow),
                color = if (hasArt) tableColors.onArt else tableColors.onPanelBase,
            )
            PlayerNameChip(name = side.name, playerColor = playerColor)
        }
    }
}

/** One ended game: tiny Legend avatars, "Ana 8 · Ben 5" with the winner highlighted, then when. */
@Composable
private fun RecentGameRow(game: RecentGameUiState, now: Long, modifier: Modifier = Modifier) {
    val separator = stringResource(R.string.feature_home_recent_separator)
    val scoreLabels = game.sides.map { side ->
        stringResource(R.string.feature_home_recent_score, side.name, side.points)
    }
    val winnerStyle = SpanStyle(fontWeight = FontWeight.Bold, color = RiftTheme.colorScheme.primary)
    val scores = buildAnnotatedString {
        game.sides.forEachIndexed { index, side ->
            if (index > 0) append(separator)
            if (side.name ==
                game.winnerName
            ) {
                withStyle(winnerStyle) { append(scoreLabels[index]) }
            } else {
                append(scoreLabels[index])
            }
        }
    }
    val endedAgo = DateUtils.getRelativeTimeSpanString(game.endedAt, now, DateUtils.MINUTE_IN_MILLIS).toString()
    val result = game.winnerName?.let { stringResource(R.string.feature_home_recent_winner, it, endedAgo) }
        ?: stringResource(R.string.feature_home_recent_no_winner, endedAgo)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = RiftTheme.spacing.md, vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        // Overlapping tiny avatars, one per side.
        Row(horizontalArrangement = Arrangement.spacedBy(-RiftTheme.spacing.sm)) {
            game.sides.forEachIndexed { index, side ->
                LegendAvatar(
                    legendName = side.legendName,
                    imageUri = side.legendImageUri,
                    playerName = side.name,
                    playerColor = PlayerColors.fromId(side.colorId) ?: PlayerColors.forSeat(index),
                    size = RiftTheme.sizes.avatarTiny,
                    contentDescription = null,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scores,
                style = RiftTheme.typography.bodyLarge,
                color = RiftTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = result, style = RiftTheme.typography.bodySmall, color = RiftTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal val previewActiveGame = ActiveGameUiState(
    gameId = GameId("g1"),
    targetScore = 8,
    sides = persistentListOf(
        ResumeSideUiState("Ana", 6, "orange", "Jinx", null),
        ResumeSideUiState("Ben", 3, "sky", null, null),
    ),
)

internal val previewRecentGames = persistentListOf(
    RecentGameUiState(GameId("g2"), 3_000_000, previewActiveGame.sides, winnerName = "Ana"),
    RecentGameUiState(
        GameId("g1"),
        1_000_000,
        persistentListOf(
            ResumeSideUiState("Ana & Cleo", 11, "orange", null, null),
            ResumeSideUiState("Ben & Dan", 7, "sky", null, null),
        ),
        winnerName = null,
    ),
)

@PreviewRiftThemes
@Composable
private fun HomeScreenResumePreview() {
    RiftTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                activeGame = previewActiveGame,
                recentGames = previewRecentGames,
                now = 3_600_000,
            ),
            onNewGameClick = {},
            onResumeClick = {},
            onSettingsClick = {},
        )
    }
}

@PreviewRiftThemes
@Composable
private fun HomeScreenEmptyPreview() {
    RiftTheme {
        HomeScreen(
            uiState = HomeUiState.Success(activeGame = null),
            onNewGameClick = {},
            onResumeClick = {},
            onSettingsClick = {},
        )
    }
}

@PreviewRiftThemes
@Composable
private fun HomeScreenRecentGamesPreview() {
    RiftTheme {
        HomeScreen(
            uiState = HomeUiState.Success(activeGame = null, recentGames = previewRecentGames, now = 3_600_000),
            onNewGameClick = {},
            onResumeClick = {},
            onSettingsClick = {},
        )
    }
}
