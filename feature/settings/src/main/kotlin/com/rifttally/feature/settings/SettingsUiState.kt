package com.rifttally.feature.settings

import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.model.ThemeMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val themeMode: ThemeMode,
        val useDynamicColor: Boolean,
        val hapticsEnabled: Boolean,
        val keepScreenOn: Boolean,
        val immersiveMode: Boolean,
        val defaultGameFormat: GameFormat,
        val addXpCounterByDefault: Boolean,
        val pickFirstPlayer: Boolean,
        val presets: ImmutableList<PresetUiState> = persistentListOf(),
        /** The saved player being edited, or `null` when the editor is closed. */
        val presetEditor: PresetEditorUiState? = null,
    ) : SettingsUiState
}

/** A saved player as listed in Settings. [legendName] is `null` when none is set (or it's unknown). */
internal data class PresetUiState(
    val id: String,
    val name: String,
    val colorId: String,
    val legendName: String?,
    val legendImageUri: String? = null,
)

/**
 * The edit dialog for saved player [id]. [legend] is the chosen Legend (`null` = none). [legendPicker] is
 * set while the Legend sheet is open. [duplicateName] is the existing saved player whose name "Save" hit
 * (shown as an inline error), or `null`.
 */
internal data class PresetEditorUiState(
    val id: String,
    val name: String,
    val colorId: String,
    val legend: Legend?,
    val legendPicker: PresetLegendPickerUiState? = null,
    val duplicateName: String? = null,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** The Legend sheet's search [query] and the Legends matching it. */
internal data class PresetLegendPickerUiState(val query: String, val legends: ImmutableList<Legend>)
