package com.rifttally.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftOutlinedButton
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.LegendAvatar
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
                .padding(RiftTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.feature_home_title),
                    style = RiftTheme.typography.headlineMedium,
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
            val activeGame = (uiState as? HomeUiState.Success)?.activeGame
            if (activeGame != null) {
                ResumeCard(game = activeGame, onResumeClick = { onResumeClick(activeGame.gameId) })
                RiftOutlinedButton(
                    text = stringResource(R.string.feature_home_new_game),
                    onClick = onNewGameClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                RiftButton(
                    text = stringResource(R.string.feature_home_new_game),
                    onClick = onNewGameClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState is HomeUiState.Success,
                )
            }
        }
    }
}

@Composable
private fun ResumeCard(game: ActiveGameUiState, onResumeClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.feature_home_game_in_progress),
                style = RiftTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg)) {
                game.sides.forEachIndexed { index, side ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
                        modifier = Modifier.weight(1f),
                    ) {
                        LegendAvatar(
                            legendName = side.legendName,
                            imageUri = side.legendImageUri,
                            playerName = side.name,
                            playerColor = PlayerColors.fromId(side.colorId) ?: PlayerColors.forSeat(index),
                            score = side.points,
                        )
                        Text(
                            text = side.name,
                            style = RiftTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.feature_home_target, game.targetScore),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            RiftButton(
                text = stringResource(R.string.feature_home_resume),
                onClick = onResumeClick,
                modifier = Modifier.fillMaxWidth(),
            )
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

@PreviewRiftThemes
@Composable
private fun HomeScreenResumePreview() {
    RiftTheme {
        HomeScreen(
            uiState = HomeUiState.Success(activeGame = previewActiveGame),
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
