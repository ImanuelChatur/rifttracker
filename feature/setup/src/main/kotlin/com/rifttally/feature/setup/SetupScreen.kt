package com.rifttally.feature.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LegendAvatar
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SetupRoute(
    onBackClick: () -> Unit,
    onGameStart: (GameId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnGameStart by rememberUpdatedState(onGameStart)
    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is SetupNavigationEvent.OpenTracker -> currentOnGameStart(event.gameId)
            }
        }
    }
    val defaultNames = uiState.players.map { stringResource(R.string.feature_setup_default_player_name, it.index + 1) }
    SetupScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onColorSelect = viewModel::onColorSelect,
        onLegendClick = viewModel::onLegendPickerOpen,
        onTargetIncrease = viewModel::onTargetScoreIncrease,
        onTargetDecrease = viewModel::onTargetScoreDecrease,
        onStartClick = { viewModel.onStartGame(defaultNames) },
        modifier = modifier,
    )
    uiState.legendPicker?.let { picker ->
        LegendPickerSheet(
            picker = picker,
            playerName = uiState.players.getOrNull(picker.playerIndex)?.displayName().orEmpty(),
            onQueryChange = viewModel::onLegendQueryChange,
            onLegendSelect = viewModel::onLegendSelect,
            onDismiss = viewModel::onLegendPickerDismiss,
        )
    }
}

@Composable
internal fun SetupScreen(
    uiState: SetupUiState,
    onBackClick: () -> Unit,
    onNameChange: (index: Int, name: String) -> Unit,
    onColorSelect: (index: Int, colorId: String) -> Unit,
    onLegendClick: (index: Int) -> Unit,
    onTargetIncrease: () -> Unit,
    onTargetDecrease: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_setup_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_setup_back),
                        onClick = onBackClick,
                    )
                },
            )
        },
        bottomBar = {
            RiftButton(
                text = stringResource(R.string.feature_setup_start),
                onClick = onStartClick,
                enabled = !uiState.isStarting,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(RiftTheme.spacing.lg),
            )
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        ) {
            uiState.players.forEach { player ->
                PlayerSetupCard(
                    player = player,
                    onNameChange = { onNameChange(player.index, it) },
                    onColorSelect = { onColorSelect(player.index, it) },
                    onLegendClick = { onLegendClick(player.index) },
                )
            }
            TargetScoreRow(
                targetScore = uiState.targetScore,
                canDecrease = uiState.canDecreaseTarget,
                onIncrease = onTargetIncrease,
                onDecrease = onTargetDecrease,
            )
        }
    }
}

@Composable
private fun SetupPlayerUiState.displayName(): String =
    name.trim().ifEmpty { stringResource(R.string.feature_setup_default_player_name, index + 1) }

@Composable
private fun PlayerSetupCard(
    player: SetupPlayerUiState,
    onNameChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    onLegendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColor = PlayerColors.fromId(player.colorId) ?: PlayerColors.forSeat(player.index)
    val displayName = player.displayName()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
            ) {
                val chooseLegend = stringResource(R.string.feature_setup_choose_legend, displayName)
                LegendAvatar(
                    legendName = player.legend?.name,
                    imageUri = player.legend?.imageUri,
                    playerName = displayName,
                    playerColor = playerColor,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onLegendClick)
                        .semantics { contentDescription = chooseLegend },
                )
                OutlinedTextField(
                    value = player.name,
                    onValueChange = onNameChange,
                    label = {
                        Text(stringResource(R.string.feature_setup_default_player_name, player.index + 1))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = player.legend?.name ?: stringResource(R.string.feature_setup_no_legend),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            ColorSwatches(selectedColorId = playerColor.id, onColorSelect = onColorSelect)
        }
    }
}

@Composable
private fun ColorSwatches(selectedColorId: String, onColorSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = RiftTheme.playerColors
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs)) {
        colors.forEachIndexed { index, color ->
            val selected = color.id == selectedColorId
            val description = stringResource(R.string.feature_setup_color_description, index + 1, colors.size)
            Box(
                modifier = Modifier
                    .size(RiftTheme.sizes.minTouchTarget)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onColorSelect(color.id) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                val ring = if (selected) RiftTheme.sizes.borderThick else RiftTheme.sizes.borderThin
                Box(
                    modifier = Modifier
                        .size(RiftTheme.sizes.icon + RiftTheme.spacing.md)
                        .border(
                            width = ring,
                            color = if (selected) RiftTheme.colorScheme.onSurface else RiftTheme.colorScheme.outline,
                            shape = CircleShape,
                        )
                        .padding(ring + RiftTheme.spacing.xxs)
                        .background(color.color, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun TargetScoreRow(
    targetScore: Int,
    canDecrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feature_setup_target_score),
                style = RiftTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            RiftIconButton(
                imageVector = RiftIcons.Remove,
                contentDescription = stringResource(R.string.feature_setup_target_decrease),
                onClick = onDecrease,
                enabled = canDecrease,
            )
            Text(
                text = targetScore.toString(),
                style = RiftTheme.scoreTypography.counter,
                modifier = Modifier.padding(horizontal = RiftTheme.spacing.sm),
            )
            RiftIconButton(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.feature_setup_target_increase),
                onClick = onIncrease,
            )
        }
    }
}

internal val previewSetupState = SetupUiState(
    players = persistentListOf(
        SetupPlayerUiState(index = 0, name = "Ana", colorId = "orange", legend = Legend("jinx", "Jinx", null)),
        SetupPlayerUiState(index = 1, name = "", colorId = "sky", legend = null),
    ),
    targetScore = 8,
    canDecreaseTarget = true,
)

@PreviewRiftThemes
@Composable
private fun SetupScreenPreview() {
    RiftTheme {
        SetupScreen(
            uiState = previewSetupState,
            onBackClick = {},
            onNameChange = { _, _ -> },
            onColorSelect = { _, _ -> },
            onLegendClick = {},
            onTargetIncrease = {},
            onTargetDecrease = {},
            onStartClick = {},
        )
    }
}
