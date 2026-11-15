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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.ImmersiveModeEffect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun TrackerRoute(
    onBackClick: () -> Unit,
    onExit: () -> Unit,
    onOpenGame: (GameId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrackerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnOpenGame by rememberUpdatedState(onOpenGame)
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is TrackerNavigationEvent.OpenGame -> currentOnOpenGame(event.gameId)
            }
        }
    }
    // Back from Game over goes Home without a BackHandler (which would turn off predictive back,
    // DESIGN.md §7): every way into the tracker leaves Home directly below it (see RiftNavHost).
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
        onPickFirstPlayer = viewModel::onPickFirstPlayer,
        onRematch = viewModel::onRematch,
        onHome = onExit,
        onFirstPlayerRevealStart = viewModel::onFirstPlayerRevealStart,
        onFirstPlayerRevealDismiss = viewModel::onFirstPlayerRevealDismiss,
        onCounterAdd = viewModel::onCounterAdd,
        onCounterChange = viewModel::onCounterChange,
        onCounterRemove = viewModel::onCounterRemove,
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
    onCounterAdd: (PlayerId, String) -> Unit,
    onCounterChange: (PlayerId, CounterId, Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    modifier: Modifier = Modifier,
    onPickFirstPlayer: () -> Unit = {},
    onRematch: () -> Unit = {},
    onHome: () -> Unit = {},
    onFirstPlayerRevealStart: (pickId: String) -> Unit = {},
    onFirstPlayerRevealDismiss: (pickId: String) -> Unit = {},
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
                onPickFirstPlayer = onPickFirstPlayer,
                onRematch = onRematch,
                onHome = onHome,
                onFirstPlayerRevealStart = onFirstPlayerRevealStart,
                onFirstPlayerRevealDismiss = onFirstPlayerRevealDismiss,
                onCounterAdd = onCounterAdd,
                onCounterChange = onCounterChange,
                onCounterRemove = onCounterRemove,
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
    onPickFirstPlayer: () -> Unit,
    onRematch: () -> Unit,
    onHome: () -> Unit,
    onFirstPlayerRevealStart: (pickId: String) -> Unit,
    onFirstPlayerRevealDismiss: (pickId: String) -> Unit,
    onCounterAdd: (PlayerId, String) -> Unit,
    onCounterChange: (PlayerId, CounterId, Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var overlay by rememberSaveable { mutableStateOf(TrackerOverlay.None) }
    val haptics = LocalHapticFeedback.current
    val undo = {
        if (state.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.Reject)
        onUndo()
    }
    val dismiss = { overlay = TrackerOverlay.None }
    // Player whose "+ Counter" dialog is open (id value, saveable), or null.
    var addCounterFor by rememberSaveable { mutableStateOf<String?>(null) }

    // The seat rotation of the player who asked, so the dialog faces them.
    var addCounterTurns by rememberSaveable { mutableIntStateOf(0) }
    val reveal = state.firstPlayerReveal
    val revealDisplay = rememberFirstPlayerReveal(
        reveal = reveal,
        panelCount = state.panels.size,
        onStart = onFirstPlayerRevealStart,
        onDismiss = onFirstPlayerRevealDismiss,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        TrackerTableLayout(
            panelCount = state.panels.size,
            modifier = Modifier
                .padding(RiftTheme.spacing.sm)
                // Under Game over the table is inert: screen readers skip it and land on the card.
                .then(if (state.gameOver != null) Modifier.clearAndSetSemantics {} else Modifier),
            strip = {
                ControlStrip(
                    targetScore = state.targetScore,
                    canUndo = state.canUndo,
                    isMenuOpen = overlay == TrackerOverlay.Menu,
                    onUndo = undo,
                    onMenuOpen = { overlay = TrackerOverlay.Menu },
                    onMenuDismiss = dismiss,
                    onChangeTargetClick = { overlay = TrackerOverlay.Target },
                    onHistoryClick = { overlay = TrackerOverlay.History },
                    onPickFirstPlayerClick = {
                        dismiss()
                        onPickFirstPlayer()
                    },
                    onRestartClick = { overlay = TrackerOverlay.Restart },
                    onEndGameClick = { overlay = TrackerOverlay.EndGame },
                )
            },
            panel = { index, panelModifier, stripInset, quarterTurns ->
                val panel = state.panels[index]
                Box(modifier = panelModifier) {
                    SidePanel(
                        panel = panel,
                        hapticsEnabled = state.hapticsEnabled,
                        onPointsChange = { delta -> onPointsChange(panel.sideId, delta) },
                        onCounterChange = onCounterChange,
                        onCounterRemove = onCounterRemove,
                        onAddCounterClick = { playerId ->
                            addCounterTurns = quarterTurns
                            addCounterFor = playerId.value
                        },
                        modifier = Modifier.fillMaxSize(),
                        topInset = stripInset,
                    )
                    if (reveal != null && revealDisplay != null) {
                        FirstPlayerRevealOverlay(
                            name = reveal.name,
                            highlighted = revealDisplay.highlighted == index,
                            showResult = revealDisplay.settled && index == reveal.panelIndex,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            },
        )
        if (reveal != null) {
            FirstPlayerRevealTapCatcher(
                name = reveal.name,
                onDismiss = { onFirstPlayerRevealDismiss(reveal.pickId) },
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(2f),
            )
        }
        state.gameOver?.let { gameOver ->
            GameOverOverlay(
                gameOver = gameOver,
                onRematch = onRematch,
                onHome = onHome,
                onUndo = undo,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(3f),
            )
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
    val counterOwner = addCounterFor?.let { id ->
        state.panels.asSequence().flatMap { it.players }.firstOrNull { it.playerId.value == id }
    }
    if (counterOwner != null) {
        AddCounterDialog(
            playerName = counterOwner.name,
            maxLabelLength = MAX_COUNTER_LABEL_LENGTH,
            quarterTurns = addCounterTurns,
            onAdd = { label ->
                onCounterAdd(counterOwner.playerId, label)
                addCounterFor = null
            },
            onDismiss = { addCounterFor = null },
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
            onPickFirstPlayer = {},
            onCounterAdd = { _, _ -> },
            onCounterChange = { _, _, _ -> },
            onCounterRemove = { _, _ -> },
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
            onPickFirstPlayer = {},
            onCounterAdd = { _, _ -> },
            onCounterChange = { _, _, _ -> },
            onCounterRemove = { _, _ -> },
        )
    }
}

private fun previewPlayer(index: Int, name: String, colorId: String, legend: String? = null) = PanelPlayerUiState(
    playerId = PlayerId("p$index"),
    name = name,
    colorId = colorId,
    legendName = legend,
    legendImageUri = null,
)

private fun previewSoloPanels(count: Int) = listOf(
    previewPlayer(1, "Ana", "orange", "Jinx"),
    previewPlayer(2, "Ben", "sky"),
    previewPlayer(3, "Cleo", "green", "Viktor"),
    previewPlayer(4, "Dan", "pink"),
).take(count).mapIndexed { index, player ->
    SidePanelUiState(
        sideId = SideId("s${index + 1}"),
        points = index * 2,
        isAtTarget = false,
        players = persistentListOf(player),
    )
}.toImmutableList()

private val previewTeamPanels = persistentListOf(
    SidePanelUiState(
        sideId = SideId("team_a"),
        points = 7,
        isAtTarget = false,
        players = persistentListOf(previewPlayer(1, "Ana", "orange", "Jinx"), previewPlayer(2, "Ben", "sky")),
    ),
    SidePanelUiState(
        sideId = SideId("team_b"),
        points = 11,
        isAtTarget = true,
        players = persistentListOf(previewPlayer(3, "Cleo", "green", "Viktor"), previewPlayer(4, "Dan", "pink")),
    ),
)

@Composable
private fun TrackerPreviewFor(panels: ImmutableList<SidePanelUiState>) {
    RiftTheme {
        TrackerScreen(
            uiState = previewTrackerState.copy(panels = panels),
            onBackClick = {},
            onPointsChange = { _, _ -> },
            onUndo = {},
            onTargetScoreChange = {},
            onEndGame = {},
            onKeepPlaying = {},
            onRestart = {},
            onPickFirstPlayer = {},
            onCounterAdd = { _, _ -> },
            onCounterChange = { _, _, _ -> },
            onCounterRemove = { _, _ -> },
        )
    }
}

@PreviewRiftThemes
@Composable
private fun TrackerScreenThreePlayersPreview() = TrackerPreviewFor(previewSoloPanels(3))

@PreviewRiftThemes
@Composable
private fun TrackerScreenFourPlayersPreview() = TrackerPreviewFor(previewSoloPanels(4))

@PreviewRiftThemes
@Composable
private fun TrackerScreenTeamsPreview() = TrackerPreviewFor(previewTeamPanels)
