package com.rifttally.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun GameDetailRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GameDetailScreen(uiState = uiState, onBackClick = onBackClick, modifier = modifier)
}

@Composable
internal fun GameDetailScreen(uiState: GameDetailUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_history_detail_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_history_back),
                        onClick = onBackClick,
                    )
                },
            )
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (uiState) {
            GameDetailUiState.Loading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            GameDetailUiState.NotFound -> Box(contentModifier.padding(RiftTheme.spacing.xl), Alignment.Center) {
                Text(
                    text = stringResource(R.string.feature_history_not_found),
                    style = RiftTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            is GameDetailUiState.Success -> GameDetailContent(state = uiState, modifier = contentModifier)
        }
    }
}

@Composable
private fun GameDetailContent(state: GameDetailUiState.Success, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(RiftTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs)) {
                Text(
                    text = stringResource(
                        R.string.feature_history_row_header,
                        dateTimeText(state.endedAt),
                        formatLabel(state.format),
                    ),
                    style = RiftTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.feature_history_duration, durationText(state.durationMinutes)),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.feature_history_target, state.targetScore),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { SectionHeading(R.string.feature_history_section_scoreboard) }
        items(state.sides.size) { index -> SideResultRow(side = state.sides[index]) }
        item {
            Text(
                text = state.winnerName?.let { stringResource(R.string.feature_history_winner, it) }
                    ?: stringResource(R.string.feature_history_no_winner),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SectionHeading(R.string.feature_history_section_chart) }
        item { ScoreChart(series = state.chart, targetScore = state.targetScore) }
        item { SectionHeading(R.string.feature_history_section_log) }
        if (state.log.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.feature_history_log_empty),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.log, key = { it.id }) { item ->
                Column {
                    Text(
                        text = item.text(),
                        style = RiftTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = RiftTheme.spacing.sm),
                    )
                    HorizontalDivider(color = RiftTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = RiftTheme.typography.titleSmall,
        color = RiftTheme.colorScheme.primary,
        modifier = modifier
            .padding(top = RiftTheme.spacing.lg)
            .semantics { heading() },
    )
}

@Composable
private fun GameLogItem.text(): String = when (this) {
    is GameLogItem.Points -> stringResource(R.string.feature_history_log_points, name, delta, total)

    is GameLogItem.CounterChanged -> stringResource(R.string.feature_history_log_counter, name, label, delta, total)

    is GameLogItem.CounterAdded -> stringResource(R.string.feature_history_log_counter_added, name, label)

    is GameLogItem.CounterRemoved -> stringResource(R.string.feature_history_log_counter_removed, name, label)

    is GameLogItem.TargetChanged -> stringResource(R.string.feature_history_log_target, targetScore)

    is GameLogItem.GameEnded -> if (winnerName != null) {
        stringResource(R.string.feature_history_log_ended_winner, winnerName)
    } else {
        stringResource(R.string.feature_history_log_ended)
    }
}

internal val previewDetailState = GameDetailUiState.Success(
    format = GameFormat.TWO_PLAYERS,
    endedAt = 1_763_150_400_000,
    durationMinutes = 27,
    targetScore = 8,
    sides = previewSides,
    winnerName = "Ana",
    chart = previewChart,
    log = persistentListOf(
        GameLogItem.Points("e1", "Ana", 1, 1),
        GameLogItem.CounterChanged("e2", "Ben", "XP", 2, 2),
        GameLogItem.Points("e3", "Ben", 2, 2),
        GameLogItem.TargetChanged("e4", 8),
        GameLogItem.GameEnded("e5", "Ana"),
    ),
)

@PreviewRiftThemes
@Composable
private fun GameDetailScreenPreview() {
    RiftTheme {
        GameDetailScreen(uiState = previewDetailState, onBackClick = {})
    }
}

@PreviewRiftThemes
@Composable
private fun GameDetailScreenNotFoundPreview() {
    RiftTheme {
        GameDetailScreen(uiState = GameDetailUiState.NotFound, onBackClick = {})
    }
}
