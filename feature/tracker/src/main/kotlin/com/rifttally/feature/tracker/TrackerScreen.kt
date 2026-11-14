package com.rifttally.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.ImmersiveModeEffect
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TrackerRoute(
    onBackClick: () -> Unit,
    onExit: () -> Unit,
    onOpenGame: (GameId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnExit by rememberUpdatedState(onExit)
    val currentOnOpenGame by rememberUpdatedState(onOpenGame)
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                TrackerNavigationEvent.Exit -> currentOnExit()
                is TrackerNavigationEvent.OpenGame -> currentOnOpenGame(event.gameId)
            }
        }
    }
    val keepScreenOn = (uiState as? TrackerUiState.Success)?.keepScreenOn == true
    (uiState as? TrackerUiState.Success)?.let { success ->
        ImmersiveModeEffect(enabled = success.immersiveMode)
    }
    TrackerScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onPointsChange = viewModel::onPointsChange,
        onUndo = viewModel::onUndo,
        onTargetScoreChange = viewModel::onTargetScoreChange,
        onEndGame = viewModel::onEndGame,
        onKeepPlaying = viewModel::onKeepPlaying,
        onRestart = viewModel::onRestart,
        // Ref-counted by Compose, so overlapping trackers (restart transition) don't release it early.
        modifier = if (keepScreenOn) modifier.keepScreenOn() else modifier,
    )
}

/** Which menu, dialog or sheet is open. Only one at a time. */
private enum class TrackerOverlay { None, Menu, Target, History, EndGame, Restart }

@Composable
internal fun TrackerScreen(
    uiState: TrackerUiState,
    onBackClick: () -> Unit,
    onPointsChange: (SideId, Int) -> Unit,
    onUndo: () -> Unit,
    onTargetScoreChange: (Int) -> Unit,
    onEndGame: () -> Unit,
    onKeepPlaying: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = RiftTheme.colorScheme.background) {
        when (uiState) {
            TrackerUiState.Loading -> Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            TrackerUiState.NotFound -> NotFound(onBackClick = onBackClick)

            is TrackerUiState.Success -> TrackerTable(
                state = uiState,
                onPointsChange = onPointsChange,
                onUndo = onUndo,
                onTargetScoreChange = onTargetScoreChange,
                onEndGame = onEndGame,
                onKeepPlaying = onKeepPlaying,
                onRestart = onRestart,
            )
        }
    }
}

@Composable
private fun TrackerTable(
    state: TrackerUiState.Success,
    onPointsChange: (SideId, Int) -> Unit,
    onUndo: () -> Unit,
    onTargetScoreChange: (Int) -> Unit,
    onEndGame: () -> Unit,
    onKeepPlaying: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlay by rememberSaveable { mutableStateOf(TrackerOverlay.None) }
    val dismiss = { overlay = TrackerOverlay.None }
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(RiftTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
        ) {
            // Phase 2: two players face each other; the far player's panel is upside down.
            state.panels.forEachIndexed { index, panel ->
                if (index == 1) {
                    ControlStrip(
                        targetScore = state.targetScore,
                        canUndo = state.canUndo,
                        isMenuOpen = overlay == TrackerOverlay.Menu,
                        onUndo = onUndo,
                        onMenuOpen = { overlay = TrackerOverlay.Menu },
                        onMenuDismiss = dismiss,
                        onChangeTargetClick = { overlay = TrackerOverlay.Target },
                        onHistoryClick = { overlay = TrackerOverlay.History },
                        onRestartClick = { overlay = TrackerOverlay.Restart },
                        onEndGameClick = { overlay = TrackerOverlay.EndGame },
                    )
                }
                SidePanel(
                    panel = panel,
                    hapticsEnabled = state.hapticsEnabled,
                    onPointsChange = { delta -> onPointsChange(panel.sideId, delta) },
                    rotationDegrees = if (index == 0 && state.panels.size == 2) FACING_AWAY_DEGREES else 0f,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
        state.winnerBanner?.let { banner ->
            WinnerBanner(
                banner = banner,
                onEndGame = onEndGame,
                onKeepPlaying = onKeepPlaying,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(RiftTheme.spacing.lg),
            )
        }
    }
    when (overlay) {
        TrackerOverlay.None, TrackerOverlay.Menu -> Unit

        TrackerOverlay.Target -> TargetScoreDialog(
            currentTarget = state.targetScore,
            onConfirm = { score ->
                onTargetScoreChange(score)
                dismiss()
            },
            onDismiss = dismiss,
        )

        TrackerOverlay.History -> HistorySheet(items = state.history, onDismiss = dismiss)

        TrackerOverlay.EndGame -> ConfirmDialog(
            title = R.string.feature_tracker_end_dialog_title,
            body = R.string.feature_tracker_end_dialog_body,
            confirmLabel = R.string.feature_tracker_menu_end,
            onConfirm = {
                dismiss()
                onEndGame()
            },
            onDismiss = dismiss,
        )

        TrackerOverlay.Restart -> ConfirmDialog(
            title = R.string.feature_tracker_restart_dialog_title,
            body = R.string.feature_tracker_restart_dialog_body,
            confirmLabel = R.string.feature_tracker_menu_restart,
            onConfirm = {
                dismiss()
                onRestart()
            },
            onDismiss = dismiss,
        )
    }
}

@Composable
private fun NotFound(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(RiftTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.feature_tracker_not_found), style = RiftTheme.typography.titleMedium)
        RiftIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.feature_tracker_back),
            onClick = onBackClick,
        )
    }
}

private const val FACING_AWAY_DEGREES = 180f

internal val previewTrackerState = TrackerUiState.Success(
    panels = previewPanels,
    targetScore = 8,
    canUndo = true,
    winnerBanner = null,
    history = previewHistory,
    hapticsEnabled = false,
    keepScreenOn = false,
    immersiveMode = false,
)

@PreviewRiftThemes
@Composable
private fun TrackerScreenTwoPlayersPreview() {
    RiftTheme {
        TrackerScreen(
            uiState = previewTrackerState,
            onBackClick = {},
            onPointsChange = { _, _ -> },
            onUndo = {},
            onTargetScoreChange = {},
            onEndGame = {},
            onKeepPlaying = {},
            onRestart = {},
        )
    }
}

@Preview(name = "Winner banner", showBackground = true, backgroundColor = 0xFF0E0F13, heightDp = 720)
@Composable
private fun TrackerScreenBannerPreview() {
    RiftTheme(darkTheme = true) {
        TrackerScreen(
            uiState = previewTrackerState.copy(winnerBanner = WinnerBannerUiState(persistentListOf("Ben"))),
            onBackClick = {},
            onPointsChange = { _, _ -> },
            onUndo = {},
            onTargetScoreChange = {},
            onEndGame = {},
            onKeepPlaying = {},
            onRestart = {},
        )
    }
}
