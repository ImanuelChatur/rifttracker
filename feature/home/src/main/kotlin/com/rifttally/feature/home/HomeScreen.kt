package com.rifttally.feature.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftHeroButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.component.RiftTonalButton
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.LegendBackdrop
import com.rifttally.core.ui.PlayerNameChip
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * @param onHistoryClick opens match history; `null` hides the History entry.
 * @param onStatsClick opens stats; `null` hides the Stats entry.
 * @param onRecentGameClick opens an ended game's detail; `null` keeps recent games read-only.
 */
@Composable
internal fun HomeRoute(
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHistoryClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onRecentGameClick: ((GameId) -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNewGameClick = onNewGameClick,
        onResumeClick = onResumeClick,
        onSettingsClick = onSettingsClick,
        onHistoryClick = onHistoryClick,
        onStatsClick = onStatsClick,
        onRecentGameClick = onRecentGameClick,
        modifier = modifier,
    )
}

/**
 * Home. The hero (DESIGN.md §2.2) is "Resume" when a game is in progress (a card with the Legends' art
 * side by side), otherwise "New game". Below it: History · Stats in one quiet container, then recent
 * games as a segmented list with tiny Legend avatars, winner highlighted. On wide windows the content
 * is a centered column of at most `RiftTheme.sizes.listMaxContentWidth` (§8).
 */
@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    onNewGameClick: () -> Unit,
    onResumeClick: (GameId) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHistoryClick: (() -> Unit)? = null,
    onStatsClick: (() -> Unit)? = null,
    onRecentGameClick: ((GameId) -> Unit)? = null,
) {
    val success = uiState as? HomeUiState.Success
    val hero: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg)) {
            success?.activeGame?.let { activeGame ->
                ResumeCard(game = activeGame, onResumeClick = { onResumeClick(activeGame.gameId) })
            }
            NewGameButton(
                hasActiveGame = success?.activeGame != null,
                enabled = success != null,
                onClick = onNewGameClick,
            )
        }
    }
    val rest: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg)) {
            if (onHistoryClick != null || onStatsClick != null) {
                BrowseRow(onHistoryClick = onHistoryClick, onStatsClick = onStatsClick)
            }
            val recentGames = success?.recentGames ?: persistentListOf()
            if (recentGames.isNotEmpty()) {
                RecentGames(games = recentGames, now = success?.now ?: 0, onGameClick = onRecentGameClick)
            } else if (success != null && success.activeGame == null) {
                // Empty state (DESIGN.md §9): one sentence; the action is the hero button above.
                Text(
                    text = stringResource(R.string.feature_home_empty),
                    style = RiftTheme.typography.bodyLarge,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Surface(modifier = modifier.fillMaxSize(), color = RiftTheme.colorScheme.background) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            val outer = Modifier.padding(horizontal = RiftTheme.spacing.xl, vertical = RiftTheme.spacing.lg)
            if (maxWidth >= RiftTheme.sizes.twoPaneMinWindowWidth && maxWidth > maxHeight) {
                // Landscape / wide (DESIGN.md §8): hero on the left, everything else on the right, so the
                // hero stays fully visible on a compact-height window. Each pane scrolls on its own.
                Column(
                    modifier = outer.widthIn(max = RiftTheme.sizes.twoPaneMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
                ) {
                    HomeHeader(onSettingsClick = onSettingsClick)
                    Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xl)) {
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) { hero() }
                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) { rest() }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .then(outer),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = RiftTheme.sizes.listMaxContentWidth),
                        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
                    ) {
                        HomeHeader(onSettingsClick = onSettingsClick)
                        hero()
                        rest()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
}

/**
 * One filled button per region (DESIGN.md §9): without a game in progress "New game" is the hero; with
 * one, Resume is the hero and New game steps back to tonal.
 */
@Composable
private fun NewGameButton(
    hasActiveGame: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.feature_home_new_game)
    val buttonModifier = modifier
        .fillMaxWidth()
        .heightIn(min = RiftTheme.sizes.heroButtonHeight)
    if (hasActiveGame) {
        RiftTonalButton(text = text, onClick = onClick, modifier = buttonModifier)
    } else {
        RiftHeroButton(text = text, onClick = onClick, modifier = buttonModifier, enabled = enabled)
    }
}

/**
 * History · Stats: two labeled text buttons contained in one quiet pill (DESIGN.md §8 containment, §2.4
 * keep labels), so they're easy to find without competing with the hero.
 */
@Composable
private fun BrowseRow(onHistoryClick: (() -> Unit)?, onStatsClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.full,
        color = RiftTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(RiftTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            onHistoryClick?.let { onClick ->
                RiftTextButton(
                    text = stringResource(R.string.feature_home_history),
                    onClick = onClick,
                    icon = RiftIcons.History,
                    modifier = Modifier.weight(1f),
                )
            }
            onStatsClick?.let { onClick ->
                RiftTextButton(
                    text = stringResource(R.string.feature_home_stats),
                    onClick = onClick,
                    icon = RiftIcons.Stats,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Hero card: each side's Legend art (or color fallback) with score and name, then "Resume". */
@Composable
private fun ResumeCard(game: ActiveGameUiState, onResumeClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.extraLargeIncreased,
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
            RiftHeroButton(
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
        val density = LocalDensity.current
        val shadow = if (hasArt) {
            with(density) {
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
            // The score is sized from the art strip, not the font scale (DESIGN.md §5): it is already
            // display-size, and scaling it 2× would push it out of the fixed-height strip.
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
                Text(
                    text = side.points.toString(),
                    style = RiftTheme.scoreTypography.scoreMedium.copy(shadow = shadow),
                    color = if (hasArt) tableColors.onArt else tableColors.onPanelBase,
                )
            }
            PlayerNameChip(name = side.name, playerColor = playerColor)
        }
    }
}

@Composable
private fun RecentGames(
    games: ImmutableList<RecentGameUiState>,
    now: Long,
    onGameClick: ((GameId) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
        Text(
            text = stringResource(R.string.feature_home_recent_games),
            style = RiftTheme.typography.titleLarge,
            color = RiftTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = RiftTheme.spacing.sm)
                .semantics { heading() },
        )
        // Segmented list (DESIGN.md §8): each game on its own tonal tile, small gaps, no dividers.
        Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs)) {
            games.forEachIndexed { index, game ->
                val shape = RiftTheme.shapes.segment(index, games.size)
                val color = RiftTheme.colorScheme.surfaceContainer
                if (onGameClick != null) {
                    Surface(onClick = { onGameClick(game.gameId) }, shape = shape, color = color) {
                        RecentGameRow(game = game, now = now)
                    }
                } else {
                    Surface(shape = shape, color = color) {
                        RecentGameRow(game = game, now = now)
                    }
                }
            }
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
            if (side.isWinner) {
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
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
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
    RecentGameUiState(
        GameId("g2"),
        3_000_000,
        persistentListOf(
            ResumeSideUiState("Ana", 8, "orange", "Jinx", null, isWinner = true),
            ResumeSideUiState("Ben", 5, "sky", null, null),
        ),
        winnerName = "Ana",
    ),
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
            onHistoryClick = {},
            onStatsClick = {},
            onRecentGameClick = {},
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
            onHistoryClick = {},
            onStatsClick = {},
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
            onHistoryClick = {},
            onStatsClick = {},
            onRecentGameClick = {},
        )
    }
}
