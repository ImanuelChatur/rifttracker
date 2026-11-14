package com.rifttally.feature.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_history_back),
                        onClick = actions.onBackClick,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        when (uiState) {
            HistoryUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is HistoryUiState.Success -> if (uiState.hasAnyGames) {
                HistoryList(state = uiState, actions = actions, modifier = Modifier.padding(padding))
            } else {
                EmptyMessage(
                    text = stringResource(R.string.feature_history_empty),
                    modifier = Modifier.padding(padding),
                )
            }
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

@Composable
private fun HistoryList(state: HistoryUiState.Success, actions: HistoryActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        FormatFilterRow(selected = state.selectedFormat, onSelect = actions.onFormatSelect)
        if (state.games.isEmpty()) {
            EmptyMessage(text = stringResource(R.string.feature_history_empty_filter))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(RiftTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                items(state.games, key = { it.gameId.value }) { game ->
                    HistoryGameCard(
                        game = game,
                        onClick = { actions.onGameClick(game.gameId) },
                        onDelete = { actions.onDeleteGame(game.gameId) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatFilterRow(selected: GameFormat?, onSelect: (GameFormat?) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = RiftTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.feature_history_filter_all)) },
        )
        GameFormat.entries.forEach { format ->
            FilterChip(
                selected = selected == format,
                onClick = { onSelect(format) },
                label = { Text(formatLabel(format)) },
            )
        }
    }
}

@Composable
private fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(RiftTheme.spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = RiftTheme.typography.bodyLarge,
            color = RiftTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

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
