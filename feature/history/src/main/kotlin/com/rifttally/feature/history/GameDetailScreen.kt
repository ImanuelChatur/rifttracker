package com.rifttally.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.headlineMediumEmphasized
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

/**
 * A finished game. The hero (DESIGN.md §2.2) is the final result: who won and the scoreboard, on one
 * emphasized container. Duration, the score chart and the event log follow as calm tonal tiles.
 */
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
        when (uiState) {
            GameDetailUiState.Loading -> CenteredList(scaffoldPadding = padding) { skeletonTiles() }

            GameDetailUiState.NotFound -> EmptyState(
                icon = Icons.Filled.Search,
                text = stringResource(R.string.feature_history_not_found),
                actionLabel = stringResource(R.string.feature_history_empty_action),
                onAction = onBackClick,
                modifier = Modifier.padding(padding),
            )

            is GameDetailUiState.Success -> CenteredList(scaffoldPadding = padding) {
                item(key = "result") { FinalResultCard(state = uiState) }
                item(key = "facts") { GameFacts(state = uiState) }
                item(key = "chart_header") {
                    ListSectionHeader(stringResource(R.string.feature_history_section_chart))
                }
                item(key = "chart") {
                    SegmentedTile(index = 0, count = 1) {
                        ScoreChart(
                            series = uiState.chart,
                            targetScore = uiState.targetScore,
                            modifier = Modifier.padding(RiftTheme.spacing.lg),
                        )
                    }
                }
                item(key = "log_header") { ListSectionHeader(stringResource(R.string.feature_history_section_log)) }
                if (uiState.log.isEmpty()) {
                    item(key = "log_empty") {
                        SegmentedTile(index = 0, count = 1) {
                            LogText(text = stringResource(R.string.feature_history_log_empty), muted = true)
                        }
                    }
                } else {
                    itemsIndexed(uiState.log, key = { _, item -> item.id }) { index, item ->
                        SegmentedTile(index = index, count = uiState.log.size) { LogText(text = item.text()) }
                    }
                }
            }
        }
    }
}

/** The hero: date and format, "Ana won" (emphasized display type), then each side's final score. */
@Composable
private fun FinalResultCard(state: GameDetailUiState.Success, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.extraLarge,
        color = RiftTheme.colorScheme.primaryContainer,
        contentColor = RiftTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs)) {
                Text(
                    text = stringResource(
                        R.string.feature_history_row_header,
                        dateTimeText(state.endedAt),
                        formatLabel(state.format),
                    ),
                    style = RiftTheme.typography.labelLarge,
                )
                Text(
                    text = state.winnerName?.let { stringResource(R.string.feature_history_winner, it) }
                        ?: stringResource(R.string.feature_history_no_winner),
                    style = RiftTheme.typography.headlineMediumEmphasized,
                    modifier = Modifier.semantics { heading() },
                )
            }
            state.sides.forEach { side ->
                SideResultRow(side = side)
            }
        }
    }
}

/** Duration and target, side by side on two quiet tiles. */
@Composable
private fun GameFacts(state: GameDetailUiState.Success, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = RiftTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
    ) {
        FactTile(
            text = stringResource(R.string.feature_history_duration, durationText(state.durationMinutes)),
            modifier = Modifier.weight(1f),
        )
        FactTile(
            text = stringResource(R.string.feature_history_target, state.targetScore),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FactTile(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RiftTheme.shapes.large,
        color = RiftTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = text,
            style = RiftTheme.typography.bodyLarge,
            modifier = Modifier
                .heightIn(min = RiftTheme.sizes.minTouchTarget)
                .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        )
    }
}

@Composable
private fun LogText(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Text(
        text = text,
        style = RiftTheme.typography.bodyLarge,
        color = if (muted) RiftTheme.colorScheme.onSurfaceVariant else RiftTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
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
