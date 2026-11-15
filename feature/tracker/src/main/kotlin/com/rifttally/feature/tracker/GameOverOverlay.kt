package com.rifttally.feature.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftOutlinedButton
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.designsystem.theme.titleMediumEmphasized
import kotlinx.collections.immutable.persistentListOf

/**
 * Shown over the table once the game has ended: the winner (or "No winner"), the final scores, and
 * Rematch (the one filled button), Home and Undo (an accidental end is fixable). Blocks taps on the
 * panels underneath. A dialog-like surface, so it uses the standard motion scheme (DESIGN.md §7).
 */
@Composable
internal fun GameOverOverlay(
    gameOver: GameOverUiState,
    onRematch: () -> Unit,
    onHome: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: () -> Unit = {},
) {
    val reducedMotion = reduceMotionEnabled()
    val motion = RiftTheme.motion.standard
    val visible = remember { MutableTransitionState(reducedMotion) }.apply { targetState = true }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible.targetState) SCRIM_ALPHA else 0f,
        animationSpec = if (reducedMotion) RiftTheme.motion.none() else motion.defaultEffects(),
        label = "gameOverScrim",
    )
    Box(
        modifier = modifier
            .background(RiftTheme.colorScheme.scrim.copy(alpha = scrimAlpha))
            // Swallows taps so nothing on the ended table reacts (the VM ignores them anyway).
            .pointerInput(Unit) { detectTapGestures {} },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = visible,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                scaleIn(motion.fastSpatial()) +
                    fadeIn(motion.defaultEffects())
            },
        ) {
            GameOverCard(
                gameOver = gameOver,
                onRematch = onRematch,
                onHome = onHome,
                onUndo = onUndo,
                onShare = onShare,
                modifier = Modifier.padding(RiftTheme.spacing.lg),
            )
        }
    }
}

@Composable
private fun GameOverCard(
    gameOver: GameOverUiState,
    onRematch: () -> Unit,
    onHome: () -> Unit,
    onUndo: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.feature_tracker_game_over)
    Surface(
        modifier = modifier
            .widthIn(max = RiftTheme.sizes.listMaxContentWidth)
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            },
        shape = RiftTheme.shapes.extraLarge,
        color = RiftTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = RiftTheme.elevation.level3,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(RiftTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.feature_tracker_game_over),
                style = RiftTheme.typography.labelLarge,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when {
                    gameOver.winnerName == null -> stringResource(R.string.feature_tracker_game_over_no_winner)

                    gameOver.winnerIsTeam -> stringResource(
                        R.string.feature_tracker_game_over_team_winner,
                        gameOver.winnerName,
                    )

                    else -> stringResource(R.string.feature_tracker_game_over_winner, gameOver.winnerName)
                },
                style = RiftTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs)) {
                gameOver.sides.forEach { side -> FinalScoreRow(side) }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
                modifier = Modifier.padding(top = RiftTheme.spacing.sm),
            ) {
                RiftTextButton(text = stringResource(R.string.feature_tracker_undo), onClick = onUndo)
                if (gameOver.resultCard != null) {
                    RiftTextButton(text = stringResource(R.string.feature_tracker_game_over_share), onClick = onShare)
                }
                RiftOutlinedButton(text = stringResource(R.string.feature_tracker_game_over_home), onClick = onHome)
                RiftButton(text = stringResource(R.string.feature_tracker_game_over_rematch), onClick = onRematch)
            }
        }
    }
}

/** "● Ana ······ 8", the winner's row emphasized. */
@Composable
private fun FinalScoreRow(side: GameOverSideUiState, modifier: Modifier = Modifier) {
    val color = PlayerColors.fromId(side.colorId) ?: PlayerColors.Grey
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
            .then(
                if (side.isWinner) {
                    Modifier.background(RiftTheme.colorScheme.secondaryContainer, RiftTheme.shapes.full)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = RiftTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(RiftTheme.spacing.md)
                .background(color.color, CircleShape),
        )
        Text(
            text = side.name,
            style = if (side.isWinner) RiftTheme.typography.titleMediumEmphasized else RiftTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = side.points.toString(),
            style = RiftTheme.typography.titleLarge,
        )
    }
}

private const val SCRIM_ALPHA = 0.6f

@PreviewRiftThemes
@Composable
private fun GameOverOverlayPreview() {
    RiftTheme {
        GameOverOverlay(
            gameOver = GameOverUiState(
                winnerName = "Ana",
                sides = persistentListOf(
                    GameOverSideUiState("Ana", "orange", 8, isWinner = true),
                    GameOverSideUiState("Ben", "sky", 5, isWinner = false),
                ),
            ),
            onRematch = {},
            onHome = {},
            onUndo = {},
        )
    }
}
