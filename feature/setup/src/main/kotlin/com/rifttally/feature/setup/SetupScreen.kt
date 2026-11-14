package com.rifttally.feature.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftHeroButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LegendAvatar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
    val xpLabel = stringResource(R.string.feature_setup_xp_label)
    SetupScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onColorSelect = viewModel::onColorSelect,
        onFormatSelect = viewModel::onFormatSelect,
        onTeamSelect = viewModel::onTeamSelect,
        onPlayerSave = viewModel::onPlayerSave,
        onPresetApply = viewModel::onPresetApply,
        onLegendClick = viewModel::onLegendPickerOpen,
        onTargetIncrease = viewModel::onTargetScoreIncrease,
        onTargetDecrease = viewModel::onTargetScoreDecrease,
        onAddXpCounterToggle = viewModel::onAddXpCounterToggle,
        onStartClick = { viewModel.onStartGame(defaultNames, xpLabel) },
        modifier = modifier,
    )
    uiState.legendPicker?.let { picker ->
        val player = uiState.players.getOrNull(picker.playerIndex)
        LegendPickerSheet(
            picker = picker,
            playerName = player?.displayName().orEmpty(),
            playerColor = player.pickerColor(),
            onQueryChange = viewModel::onLegendQueryChange,
            onLegendSelect = viewModel::onLegendSelect,
            onDismiss = viewModel::onLegendPickerDismiss,
        )
    }
}

/**
 * New game setup. The hero (DESIGN.md §2.2) is the Start button, the only filled button and the only
 * emphasized type on the screen. Format is a connected button group; each player is a tonal tile in a
 * segmented group (§8) with a tappable Legend portrait, name, color swatches, team (2v2) and presets;
 * game options are a second segmented group. On wide windows the content is a centered column (§8).
 */
@Composable
internal fun SetupScreen(
    uiState: SetupUiState,
    onBackClick: () -> Unit,
    onNameChange: (index: Int, name: String) -> Unit,
    onColorSelect: (index: Int, colorId: String) -> Unit,
    onFormatSelect: (GameFormat) -> Unit,
    onTeamSelect: (index: Int, team: Team) -> Unit,
    onPlayerSave: (index: Int) -> Unit,
    onPresetApply: (index: Int, presetId: String) -> Unit,
    onLegendClick: (index: Int) -> Unit,
    onTargetIncrease: () -> Unit,
    onTargetDecrease: () -> Unit,
    onAddXpCounterToggle: (Boolean) -> Unit,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                RiftHeroButton(
                    text = stringResource(R.string.feature_setup_start),
                    onClick = onStartClick,
                    enabled = uiState.canStart,
                    modifier = Modifier
                        .widthIn(max = MaxContentWidth - RiftTheme.spacing.lg * 2)
                        .fillMaxWidth(),
                )
            }
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = RiftTheme.spacing.lg)
                    .padding(bottom = RiftTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
            ) {
                SectionHeader(text = stringResource(R.string.feature_setup_format_label))
                FormatSelector(selected = uiState.format, onSelect = onFormatSelect)
                if (uiState.teamsInvalid) {
                    Text(
                        text = stringResource(R.string.feature_setup_teams_invalid),
                        style = RiftTheme.typography.bodyMedium,
                        color = RiftTheme.colorScheme.error,
                    )
                }
                // Segmented group (DESIGN.md §8): one tonal tile per player, small gaps, no outlines.
                Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs)) {
                    uiState.players.forEach { player ->
                        PlayerSetupTile(
                            player = player,
                            shape = RiftTheme.shapes.segment(player.index, uiState.players.size),
                            onNameChange = { onNameChange(player.index, it) },
                            onColorSelect = { onColorSelect(player.index, it) },
                            onLegendClick = { onLegendClick(player.index) },
                            onTeamSelect = { onTeamSelect(player.index, it) },
                            presets = uiState.presets,
                            onSave = { onPlayerSave(player.index) },
                            onPresetApply = { onPresetApply(player.index, it) },
                        )
                    }
                }
                SectionHeader(
                    text = stringResource(R.string.feature_setup_game_label),
                    modifier = Modifier.padding(top = RiftTheme.spacing.sm),
                )
                Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs)) {
                    TargetScoreTile(
                        targetScore = uiState.targetScore,
                        canDecrease = uiState.canDecreaseTarget,
                        onIncrease = onTargetIncrease,
                        onDecrease = onTargetDecrease,
                        shape = RiftTheme.shapes.segment(0, GAME_OPTIONS),
                    )
                    XpCounterToggleTile(
                        checked = uiState.addXpCounter,
                        onCheckedChange = onAddXpCounterToggle,
                        shape = RiftTheme.shapes.segment(1, GAME_OPTIONS),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.titleMedium,
        color = RiftTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = RiftTheme.spacing.sm)
            .semantics { heading() },
    )
}

/** The color the Legend picker uses for its selection ring. */
internal fun SetupPlayerUiState?.pickerColor(): PlayerColor =
    this?.let { PlayerColors.fromId(it.colorId) ?: PlayerColors.forSeat(it.index) } ?: PlayerColors.Grey

@Composable
private fun SetupPlayerUiState.displayName(): String =
    name.trim().ifEmpty { stringResource(R.string.feature_setup_default_player_name, index + 1) }

@Composable
private fun PlayerSetupTile(
    player: SetupPlayerUiState,
    shape: Shape,
    onNameChange: (String) -> Unit,
    onColorSelect: (String) -> Unit,
    onLegendClick: () -> Unit,
    onTeamSelect: (Team) -> Unit,
    presets: ImmutableList<SavedPlayerUiState>,
    onSave: () -> Unit,
    onPresetApply: (presetId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerColor = PlayerColors.fromId(player.colorId) ?: PlayerColors.forSeat(player.index)
    val displayName = player.displayName()
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = RiftTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
                LegendButton(
                    legend = player.legend,
                    playerName = displayName,
                    playerColor = playerColor,
                    onClick = onLegendClick,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
                ) {
                    OutlinedTextField(
                        value = player.name,
                        onValueChange = onNameChange,
                        label = {
                            Text(stringResource(R.string.feature_setup_default_player_name, player.index + 1))
                        },
                        singleLine = true,
                        shape = RiftTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ColorSwatches(selectedColorId = playerColor.id, onColorSelect = onColorSelect)
                }
            }
            player.team?.let { team -> TeamSelector(selected = team, onSelect = onTeamSelect) }
            PresetActions(
                playerName = displayName,
                presets = presets,
                canSave = player.name.isNotBlank(),
                onSave = onSave,
                onPresetApply = onPresetApply,
            )
        }
    }
}

/**
 * The player's Legend portrait (circle, player-color ring with art) and its name underneath; one
 * button that opens the picker ("Choose Legend for Ana").
 */
@Composable
private fun LegendButton(
    legend: Legend?,
    playerName: String,
    playerColor: PlayerColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chooseLegend = stringResource(R.string.feature_setup_choose_legend, playerName)
    val avatarSize = RiftTheme.sizes.avatarMedium
    Column(
        modifier = modifier
            .width(avatarSize + RiftTheme.spacing.lg)
            .clip(RiftTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = chooseLegend }
            .padding(vertical = RiftTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
    ) {
        LegendAvatar(
            legendName = legend?.name,
            imageUri = legend?.imageUri,
            playerName = playerName,
            playerColor = playerColor,
            size = avatarSize,
            contentDescription = null,
        )
        Text(
            text = legend?.name ?: stringResource(R.string.feature_setup_no_legend),
            style = RiftTheme.typography.labelMedium,
            color = RiftTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The palette as 48dp radio swatches, split into even rows (4 + 4 on a phone, one row when wide). The
 * selected swatch grows a little on the expressive spring and shows a check (not color alone).
 */
@Composable
private fun ColorSwatches(selectedColorId: String, onColorSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = RiftTheme.playerColors
    val target = RiftTheme.sizes.minTouchTarget
    val gap = RiftTheme.spacing.xs
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fit = ((maxWidth + gap) / (target + gap)).toInt().coerceIn(1, colors.size)
        val rows = (colors.size + fit - 1) / fit
        val perRow = (colors.size + rows - 1) / rows
        FlowRow(
            modifier = Modifier.selectableGroup(),
            maxItemsInEachRow = perRow,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            colors.forEachIndexed { index, color ->
                ColorSwatch(
                    color = color,
                    selected = color.id == selectedColorId,
                    description = stringResource(R.string.feature_setup_color_description, index + 1, colors.size),
                    onClick = { onColorSelect(color.id) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: PlayerColor,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else UNSELECTED_SWATCH_SCALE,
        animationSpec = if (reduceMotionEnabled()) {
            RiftTheme.motion.none()
        } else {
            RiftTheme.motion.expressive.fastSpatial()
        },
        label = "swatchScale",
    )
    Box(
        modifier = modifier
            .size(RiftTheme.sizes.minTouchTarget)
            .clip(RiftTheme.shapes.full)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RiftTheme.sizes.minTouchTarget - RiftTheme.spacing.xs)
                .scale(scale)
                .then(
                    if (selected) {
                        Modifier.border(
                            RiftTheme.sizes.borderThick,
                            RiftTheme.colorScheme.onSurface,
                            RiftTheme.shapes.full,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(if (selected) RiftTheme.sizes.borderThick + RiftTheme.spacing.xxs else 0.dp)
                .background(color.color, RiftTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = color.onColor,
                    modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                )
            }
        }
    }
}

@Composable
private fun TargetScoreTile(
    targetScore: Int,
    canDecrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = RiftTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .heightIn(min = RiftTheme.sizes.heroButtonHeight)
                .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.feature_setup_target_score),
                style = RiftTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            StepButton(
                icon = { Icon(RiftIcons.Remove, contentDescription = null) },
                description = stringResource(R.string.feature_setup_target_decrease),
                onClick = onDecrease,
                enabled = canDecrease,
            )
            Text(
                text = targetScore.toString(),
                style = RiftTheme.scoreTypography.counter,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = RiftTheme.sizes.minTouchTarget),
            )
            StepButton(
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                description = stringResource(R.string.feature_setup_target_increase),
                onClick = onIncrease,
            )
        }
    }
}

/** A round tonal −/+ (48dp), labeled for accessibility. */
@Composable
private fun StepButton(
    icon: @Composable () -> Unit,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = RiftTheme.shapes.full,
        modifier = modifier
            .size(RiftTheme.sizes.minTouchTarget)
            .semantics { contentDescription = description },
        content = icon,
    )
}

@Composable
private fun XpCounterToggleTile(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = RiftTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .heightIn(min = RiftTheme.sizes.heroButtonHeight)
                .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
        ) {
            Text(
                text = stringResource(R.string.feature_setup_xp_toggle),
                style = RiftTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            // The row handles the toggle, so the switch itself is not separately focusable.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

/** Content width cap on wide windows (DESIGN.md §8: ≈ 640dp centered column). */
private val MaxContentWidth = 640.dp

private const val GAME_OPTIONS = 2
private const val UNSELECTED_SWATCH_SCALE = 0.86f

internal val previewSetupState = SetupUiState(
    players = persistentListOf(
        SetupPlayerUiState(index = 0, name = "Ana", colorId = "orange", legend = Legend("jinx", "Jinx", null)),
        SetupPlayerUiState(index = 1, name = "", colorId = "sky", legend = null),
    ),
    targetScore = 8,
    canDecreaseTarget = true,
)

/** A 4-player preview/test state with the XP toggle on. */
internal val previewSetupFourPlayersState = SetupUiState(
    format = GameFormat.FOUR_PLAYERS,
    players = listOf("Ana" to "orange", "Ben" to "sky", "" to "green", "Dan" to "pink")
        .mapIndexed { index, (name, color) ->
            SetupPlayerUiState(
                index = index,
                name = name,
                colorId = color,
                legend = if (index == 1) Legend("viktor", "Viktor", null) else null,
            )
        }.toImmutableList(),
    targetScore = 8,
    canDecreaseTarget = true,
    addXpCounter = true,
    presets = persistentListOf(SavedPlayerUiState("1", "Ana")),
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
            onFormatSelect = {},
            onTeamSelect = { _, _ -> },
            onPlayerSave = {},
            onPresetApply = { _, _ -> },
            onLegendClick = {},
            onTargetIncrease = {},
            onTargetDecrease = {},
            onAddXpCounterToggle = {},
            onStartClick = {},
        )
    }
}

@PreviewRiftThemes
@Composable
private fun SetupScreenFourPlayersPreview() {
    RiftTheme {
        SetupScreen(
            uiState = previewSetupFourPlayersState,
            onBackClick = {},
            onNameChange = { _, _ -> },
            onColorSelect = { _, _ -> },
            onFormatSelect = {},
            onTeamSelect = { _, _ -> },
            onPlayerSave = {},
            onPresetApply = { _, _ -> },
            onLegendClick = {},
            onTargetIncrease = {},
            onTargetDecrease = {},
            onAddXpCounterToggle = {},
            onStartClick = {},
        )
    }
}
