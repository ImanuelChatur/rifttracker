package com.rifttally.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.ui.LegendAvatar
import com.rifttally.core.ui.list.CenteredList
import com.rifttally.core.ui.list.ListSectionHeader
import com.rifttally.core.ui.list.SegmentedTile
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    SettingsScreen(
        uiState = uiState,
        versionName = versionName,
        actions = SettingsActions(
            onThemeModeSelect = viewModel::onThemeModeSelect,
            onDynamicColorToggle = viewModel::onDynamicColorToggle,
            onHapticsToggle = viewModel::onHapticsToggle,
            onKeepScreenOnToggle = viewModel::onKeepScreenOnToggle,
            onImmersiveModeToggle = viewModel::onImmersiveModeToggle,
            onDefaultFormatSelect = viewModel::onDefaultFormatSelect,
            onAddXpByDefaultToggle = viewModel::onAddXpByDefaultToggle,
            onPresetDelete = viewModel::onPresetDelete,
        ),
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

/** Every user action on the settings screen (named, so the screen keeps a short signature). */
internal data class SettingsActions(
    val onThemeModeSelect: (ThemeMode) -> Unit = {},
    val onDynamicColorToggle: (Boolean) -> Unit = {},
    val onHapticsToggle: (Boolean) -> Unit = {},
    val onKeepScreenOnToggle: (Boolean) -> Unit = {},
    val onImmersiveModeToggle: (Boolean) -> Unit = {},
    val onDefaultFormatSelect: (GameFormat) -> Unit = {},
    val onAddXpByDefaultToggle: (Boolean) -> Unit = {},
    val onPresetDelete: (id: String) -> Unit = {},
)

/**
 * Settings: a calm segmented list (DESIGN.md §2.3, §8). Each section is a group of tonal tiles; no
 * dividers, no hero.
 */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    versionName: String,
    actions: SettingsActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RiftTopAppBar(
                title = stringResource(R.string.feature_settings_title),
                navigationIcon = {
                    RiftIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.feature_settings_back),
                        onClick = onBackClick,
                    )
                },
            )
        },
        containerColor = RiftTheme.colorScheme.background,
    ) { padding ->
        CenteredList(scaffoldPadding = padding) {
            when (uiState) {
                // Local data loads near-instantly: show tonal placeholders, not a spinner (DESIGN.md §9).
                SettingsUiState.Loading -> skeleton()

                is SettingsUiState.Success -> settingsItems(
                    state = uiState,
                    versionName = versionName,
                    actions = actions,
                )
            }
        }
    }
}

private fun LazyListScope.skeleton() {
    items(SKELETON_TILES) { index ->
        SegmentedTile(index = index, count = SKELETON_TILES) {
            Box(modifier = Modifier.heightIn(min = RiftTheme.sizes.avatarMedium))
        }
    }
}

private fun LazyListScope.settingsItems(state: SettingsUiState.Success, versionName: String, actions: SettingsActions) {
    // Wallpaper colors only exist on Android 12+; hide the switch where it would do nothing.
    val showDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appearanceCount = if (showDynamicColor) 2 else 1
    item { ListSectionHeader(stringResource(R.string.feature_settings_section_appearance)) }
    item {
        SegmentedTile(index = 0, count = appearanceCount) {
            ThemeSelector(selected = state.themeMode, onSelect = actions.onThemeModeSelect)
        }
    }
    if (showDynamicColor) {
        item {
            SegmentedTile(index = 1, count = appearanceCount) {
                SwitchRow(R.string.feature_settings_dynamic_color, state.useDynamicColor, actions.onDynamicColorToggle)
            }
        }
    }

    item { ListSectionHeader(stringResource(R.string.feature_settings_section_table)) }
    val tableSwitches = listOf(
        Triple(R.string.feature_settings_haptics, state.hapticsEnabled, actions.onHapticsToggle),
        Triple(R.string.feature_settings_keep_screen_on, state.keepScreenOn, actions.onKeepScreenOnToggle),
        Triple(R.string.feature_settings_immersive, state.immersiveMode, actions.onImmersiveModeToggle),
    )
    itemsIndexed(tableSwitches) { index, (title, checked, onToggle) ->
        SegmentedTile(index = index, count = tableSwitches.size) { SwitchRow(title, checked, onToggle) }
    }

    item { ListSectionHeader(stringResource(R.string.feature_settings_section_defaults)) }
    item {
        SegmentedTile(index = 0, count = 2) {
            DefaultFormatSelector(selected = state.defaultGameFormat, onSelect = actions.onDefaultFormatSelect)
        }
    }
    item {
        SegmentedTile(index = 1, count = 2) {
            SwitchRow(R.string.feature_settings_default_xp, state.addXpCounterByDefault, actions.onAddXpByDefaultToggle)
        }
    }

    item { ListSectionHeader(stringResource(R.string.feature_settings_section_presets)) }
    if (state.presets.isEmpty()) {
        item { SegmentedTile(index = 0, count = 1) { PresetsEmpty() } }
    } else {
        itemsIndexed(state.presets, key = { _, preset -> preset.id }) { index, preset ->
            SegmentedTile(index = index, count = state.presets.size) {
                PresetRow(preset = preset, onDelete = { actions.onPresetDelete(preset.id) })
            }
        }
    }

    item { ListSectionHeader(stringResource(R.string.feature_settings_section_about)) }
    item {
        SegmentedTile(index = 0, count = ABOUT_TILES) {
            TileText(title = stringResource(R.string.feature_settings_version, versionName))
        }
    }
    item {
        SegmentedTile(index = 1, count = ABOUT_TILES) {
            TileText(
                title = stringResource(R.string.feature_settings_licenses_title),
                body = stringResource(R.string.feature_settings_licenses),
            )
        }
    }
    item {
        SegmentedTile(index = 2, count = ABOUT_TILES) {
            TileText(body = stringResource(R.string.feature_settings_disclaimer))
        }
    }
}

/** Plain text in a tile: an optional [title] line and an optional [body] paragraph. */
@Composable
private fun TileText(modifier: Modifier = Modifier, title: String? = null, body: String? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.minTouchTarget)
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs, Alignment.CenterVertically),
    ) {
        if (title != null) Text(text = title, style = RiftTheme.typography.bodyLarge)
        if (body != null) {
            Text(text = body, style = RiftTheme.typography.bodyMedium, color = RiftTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RiftTheme.sizes.heroButtonHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
    ) {
        Text(text = stringResource(title), style = RiftTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // The row handles the toggle, so the switch itself is not separately focusable.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A labelled control inside a tile: [label] above [content]. */
@Composable
private fun LabelledControl(@StringRes label: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
    ) {
        Text(text = stringResource(label), style = RiftTheme.typography.bodyLarge)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = listOf(
        ThemeMode.SYSTEM to R.string.feature_settings_theme_system,
        ThemeMode.LIGHT to R.string.feature_settings_theme_light,
        ThemeMode.DARK to R.string.feature_settings_theme_dark,
    )
    LabelledControl(label = R.string.feature_settings_theme, modifier = modifier) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    label = { Text(stringResource(label)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultFormatSelector(selected: GameFormat, onSelect: (GameFormat) -> Unit, modifier: Modifier = Modifier) {
    val formats = GameFormat.entries
    LabelledControl(label = R.string.feature_settings_default_format, modifier = modifier) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            formats.forEachIndexed { index, format ->
                val description = if (format.isTeams) {
                    stringResource(R.string.feature_settings_format_teams_description)
                } else {
                    pluralStringResource(
                        R.plurals.feature_settings_format_players_description,
                        format.playerCount,
                        format.playerCount,
                    )
                }
                SegmentedButton(
                    selected = format == selected,
                    onClick = { onSelect(format) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
                    modifier = Modifier.semantics { contentDescription = description },
                    label = {
                        Text(
                            if (format.isTeams) {
                                stringResource(R.string.feature_settings_format_teams)
                            } else {
                                format.playerCount.toString()
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PresetRow(preset: PresetUiState, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = RiftTheme.spacing.lg,
                end = RiftTheme.spacing.xs,
                top = RiftTheme.spacing.sm,
                bottom = RiftTheme.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        LegendAvatar(
            legendName = preset.legendName,
            imageUri = null,
            playerName = preset.name,
            playerColor = PlayerColors.fromId(preset.colorId) ?: PlayerColors.Grey,
            size = RiftTheme.sizes.avatarSmall,
            // The name is shown next to it.
            contentDescription = null,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                style = RiftTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = preset.legendName ?: stringResource(R.string.feature_settings_preset_no_legend),
                style = RiftTheme.typography.bodyMedium,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
        }
        RiftIconButton(
            imageVector = Icons.Filled.Delete,
            contentDescription = stringResource(R.string.feature_settings_preset_delete, preset.name),
            onClick = onDelete,
        )
    }
}

/** Inline empty state for saved players: an icon and one sentence (DESIGN.md §9). */
@Composable
private fun PresetsEmpty(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = RiftTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(RiftTheme.sizes.icon),
        )
        Text(
            text = stringResource(R.string.feature_settings_presets_empty),
            style = RiftTheme.typography.bodyMedium,
            color = RiftTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val SKELETON_TILES = 4
private const val ABOUT_TILES = 3

internal val previewSettingsState = SettingsUiState.Success(
    themeMode = ThemeMode.DARK,
    useDynamicColor = false,
    hapticsEnabled = true,
    keepScreenOn = true,
    immersiveMode = true,
    defaultGameFormat = GameFormat.TWO_PLAYERS,
    addXpCounterByDefault = false,
    presets = persistentListOf(
        PresetUiState("1", "Ana", "orange", "Jinx"),
        PresetUiState("2", "Ben", "sky", null),
    ),
)

@PreviewRiftThemes
@Composable
private fun SettingsScreenPreview() {
    RiftTheme {
        SettingsScreen(
            uiState = previewSettingsState,
            versionName = "0.1.0",
            actions = SettingsActions(),
            onBackClick = {},
        )
    }
}

@PreviewRiftThemes
@Composable
private fun SettingsScreenNoPresetsPreview() {
    RiftTheme {
        SettingsScreen(
            uiState = previewSettingsState.copy(presets = persistentListOf()),
            versionName = "0.1.0",
            actions = SettingsActions(),
            onBackClick = {},
        )
    }
}

@PreviewRiftThemes
@Composable
private fun SettingsScreenLoadingPreview() {
    RiftTheme {
        SettingsScreen(
            uiState = SettingsUiState.Loading,
            versionName = "0.1.0",
            actions = SettingsActions(),
            onBackClick = {},
        )
    }
}
