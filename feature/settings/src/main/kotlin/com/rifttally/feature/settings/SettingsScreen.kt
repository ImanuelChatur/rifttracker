package com.rifttally.feature.settings

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.component.RiftTopAppBar
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
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
        when (uiState) {
            SettingsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is SettingsUiState.Success -> SettingsList(
                state = uiState,
                versionName = versionName,
                actions = actions,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun SettingsList(
    state: SettingsUiState.Success,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
    ) {
        item { SectionHeading(R.string.feature_settings_section_appearance) }
        item { ThemeSelector(selected = state.themeMode, onSelect = actions.onThemeModeSelect) }
        // Wallpaper colors only exist on Android 12+; hide the switch where it would do nothing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            item {
                SwitchRow(
                    title = R.string.feature_settings_dynamic_color,
                    checked = state.useDynamicColor,
                    onCheckedChange = actions.onDynamicColorToggle,
                )
            }
        }

        item { SectionHeading(R.string.feature_settings_section_table) }
        item { SwitchRow(R.string.feature_settings_haptics, state.hapticsEnabled, actions.onHapticsToggle) }
        item { SwitchRow(R.string.feature_settings_keep_screen_on, state.keepScreenOn, actions.onKeepScreenOnToggle) }
        item { SwitchRow(R.string.feature_settings_immersive, state.immersiveMode, actions.onImmersiveModeToggle) }

        item { SectionHeading(R.string.feature_settings_section_defaults) }
        item { DefaultFormatSelector(selected = state.defaultGameFormat, onSelect = actions.onDefaultFormatSelect) }
        item {
            SwitchRow(R.string.feature_settings_default_xp, state.addXpCounterByDefault, actions.onAddXpByDefaultToggle)
        }

        item { SectionHeading(R.string.feature_settings_section_presets) }
        if (state.presets.isEmpty()) {
            item { BodyText(stringResource(R.string.feature_settings_presets_empty)) }
        } else {
            items(state.presets, key = { it.id }) { preset ->
                PresetRow(preset = preset, onDelete = { actions.onPresetDelete(preset.id) })
            }
        }

        item { SectionHeading(R.string.feature_settings_section_about) }
        item { BodyText(stringResource(R.string.feature_settings_version, versionName)) }
        item {
            Column(modifier = Modifier.padding(horizontal = RiftTheme.spacing.lg)) {
                Text(
                    text = stringResource(R.string.feature_settings_licenses_title),
                    style = RiftTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.feature_settings_licenses),
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(RiftTheme.spacing.lg)) }
        item { BodyText(stringResource(R.string.feature_settings_disclaimer)) }
    }
}

@Composable
private fun SectionHeading(@StringRes text: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(text),
        style = RiftTheme.typography.titleSmall,
        color = RiftTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = RiftTheme.spacing.lg, end = RiftTheme.spacing.lg, top = RiftTheme.spacing.xl)
            .semantics { heading() },
    )
}

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = RiftTheme.typography.bodyMedium,
        color = RiftTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.sm),
    )
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes summary: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = RiftTheme.spacing.lg, vertical = RiftTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(title), style = RiftTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = stringResource(summary),
                    style = RiftTheme.typography.bodySmall,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The row handles the toggle, so the switch itself is not separately focusable.
        Switch(checked = checked, onCheckedChange = null)
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
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = RiftTheme.spacing.lg),
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultFormatSelector(selected: GameFormat, onSelect: (GameFormat) -> Unit, modifier: Modifier = Modifier) {
    val formats = GameFormat.entries
    Column(modifier = modifier.padding(horizontal = RiftTheme.spacing.lg)) {
        Text(text = stringResource(R.string.feature_settings_default_format), style = RiftTheme.typography.bodyLarge)
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
            .padding(start = RiftTheme.spacing.lg, end = RiftTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md),
    ) {
        Surface(
            modifier = Modifier.size(RiftTheme.sizes.icon),
            shape = CircleShape,
            color = (PlayerColors.fromId(preset.colorId) ?: PlayerColors.Grey).color,
            content = {},
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = preset.name, style = RiftTheme.typography.bodyLarge)
            Text(
                text = preset.legendName ?: stringResource(R.string.feature_settings_preset_no_legend),
                style = RiftTheme.typography.bodySmall,
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
