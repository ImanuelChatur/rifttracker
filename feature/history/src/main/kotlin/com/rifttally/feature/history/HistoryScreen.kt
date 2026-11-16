package com.rifttally.feature.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.list.CenteredList
import com.rifttally.core.ui.list.EmptyState
import com.rifttally.core.ui.list.EmptyStateContent
import com.rifttally.core.ui.list.skeletonTiles
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun HistoryRoute(
    onBackClick: () -> Unit,
    onGameClick: (GameId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        actions = HistoryActions(
            onBackClick = onBackClick,
            onGameClick = onGameClick,
            onFormatSelect = viewModel::onFormatSelect,
            onDeleteGame = viewModel::onDeleteGame,
            onUndoDelete = viewModel::onUndoDelete,
            onUndoDismiss = viewModel::onUndoDismiss,
        ),
        modifier = modifier,
    )
}

/** Every user action on the history screen. */
internal data class HistoryActions(
    val onBackClick: () -> Unit = {},
    val onGameClick: (GameId) -> Unit = {},
    val onFormatSelect: (GameFormat?) -> Unit = {},
    val onDeleteGame: (GameId) -> Unit = {},
    val onUndoDelete: () -> Unit = {},
    val onUndoDismiss: () -> Unit = {},
)

/**
 * Match history: format filter chips, then ended games as a calm segmented list (DESIGN.md §2.3, §8).
 * Each game is a tonal tile; the winner's score carries the emphasis.
 */
@Composable
internal fun HistoryScreen(uiState: HistoryUiState, actions: HistoryActions, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    UndoDeleteSnackbar(
        deleted = (uiState as? HistoryUiState.Success)?.recentlyDeleted,
        snackbarHostState = snackbarHostState,
        onUndo = actions.onUndoDelete,
        onDismiss = actions.onUndoDismiss,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_history_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = RiftIcons.ArrowBack,
                        contentDescription = stringResource(R.string.feature_history_back),
                        onClick = actions.onBackClick,
                    )
                },
            )
        },
        // The snackbar floats over content: the one element here allowed a shadow (DESIGN.md §6).
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        if (uiState is HistoryUiState.Success && !uiState.hasAnyGames) {
            EmptyState(
                icon = RiftIcons.DateRange,
                text = stringResource(R.string.feature_history_empty),
                actionLabel = stringResource(R.string.feature_history_empty_action),
                onAction = actions.onBackClick,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        CenteredList(scaffoldPadding = padding) {
            when (uiState) {
                HistoryUiState.Loading -> skeletonTiles()

                is HistoryUiState.Success -> {
                    item(key = FILTER_KEY) {
                        FormatFilterRow(selected = uiState.selectedFormat, onSelect = actions.onFormatSelect)
                    }
                    if (uiState.games.isEmpty()) {
                        item(key = EMPTY_FILTER_KEY) {
                            EmptyStateContent(
                                icon = RiftIcons.List,
                                text = stringResource(R.string.feature_history_empty_filter),
                                actionLabel = stringResource(R.string.feature_history_empty_filter_action),
                                onAction = { actions.onFormatSelect(null) },
                                modifier = Modifier.padding(vertical = RiftTheme.spacing.xxl),
                            )
                        }
                    } else {
                        itemsIndexed(uiState.games, key = { _, game -> game.gameId.value }) { index, game ->
                            HistoryGameCard(
                                game = game,
                                index = index,
                                count = uiState.games.size,
                                onClick = { actions.onGameClick(game.gameId) },
                                onDelete = { actions.onDeleteGame(game.gameId) },
                                modifier = Modifier.animateHistoryItem(this),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Item placement and fade on the *standard* springs; none with reduce motion (DESIGN.md §7). */
@Composable
private fun Modifier.animateHistoryItem(scope: LazyItemScope): Modifier {
    val standard = RiftTheme.motion.standard
    val reduceMotion = reduceMotionEnabled()
    return with(scope) {
        if (reduceMotion) {
            animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
        } else {
            animateItem(
                fadeInSpec = standard.defaultEffects(),
                placementSpec = standard.defaultSpatial(),
                fadeOutSpec = standard.defaultEffects(),
            )
        }
    }
}

/** Shows "Game deleted · Undo" whenever a new game is deleted. */
@Composable
private fun UndoDeleteSnackbar(
    deleted: GameId?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.feature_history_deleted)
    val undoLabel = stringResource(R.string.feature_history_undo)
    val currentOnUndo by rememberUpdatedState(onUndo)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndo() else currentOnDismiss()
    }
}

/** Format filter chips (DESIGN.md §9): small radius, one row that scrolls horizontally. */
@Composable
private fun FormatFilterRow(selected: GameFormat?, onSelect: (GameFormat?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = RiftTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.feature_history_filter_all)) },
            shape = RiftTheme.shapes.small,
        )
        GameFormat.entries.forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(formatLabel(format)) },
                shape = RiftTheme.shapes.small,
            )
        }
    }
}

private const val FILTER_KEY = "filter"
private const val EMPTY_FILTER_KEY = "empty_filter"

internal val previewHistoryState = HistoryUiState.Success(
    games = persistentListOf(
        previewGame,
        previewGame.copy(
            gameId = GameId("g2"),
            format = GameFormat.TEAMS_2V2,
            sides = persistentListOf(
                HistorySideUiState("Ana & Cleo", "Jinx & Viktor", "orange", "Jinx", null, 9, false),
                HistorySideUiState("Ben & Dan", null, "sky", null, null, 11, true),
            ),
            winnerName = "Ben & Dan",
        ),
        previewGame.copy(gameId = GameId("g3"), winnerName = null, sides = persistentListOf()),
    ),
    hasAnyGames = true,
)

@PreviewRiftThemes
@Composable
private fun HistoryScreenPreview() {
    RiftTheme {
        HistoryScreen(uiState = previewHistoryState, actions = HistoryActions())
    }
}

@PreviewRiftThemes
@Composable
private fun HistoryScreenEmptyPreview() {
    RiftTheme {
        HistoryScreen(uiState = HistoryUiState.Success(), actions = HistoryActions())
    }
}

@PreviewRiftThemes
@Composable
private fun HistoryScreenEmptyFilterPreview() {
    RiftTheme {
        HistoryScreen(
            uiState = HistoryUiState.Success(selectedFormat = GameFormat.THREE_PLAYERS, hasAnyGames = true),
            actions = HistoryActions(),
        )
    }
}
