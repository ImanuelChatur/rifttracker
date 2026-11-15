package com.rifttally.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.MAX_PLAYER_NAME_LENGTH
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * App settings (DataStore) and the saved players list. Every setting is persisted immediately; a saved
 * player is edited in a draft that's only written on "Save".
 */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val presetRepository: PlayerPresetRepository,
    legendRepository: LegendRepository,
) : ViewModel() {

    private val draft = MutableStateFlow<PresetDraft?>(null)

    /** Saved players as last emitted, for the synchronous duplicate-name check on "Save". */
    private var latestPresets: List<PlayerPreset> = emptyList()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userSettings,
        presetRepository.presets,
        legendRepository.legends,
        draft,
    ) { settings, presets, legends, draft ->
        latestPresets = presets
        val legendsById = legends.associateBy { it.id }
        SettingsUiState.Success(
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            hapticsEnabled = settings.hapticsEnabled,
            keepScreenOn = settings.keepScreenOn,
            immersiveMode = settings.immersiveMode,
            defaultGameFormat = settings.defaultGameFormat,
            addXpCounterByDefault = settings.addXpCounterByDefault,
            presets = presets.map { preset ->
                val legend = preset.legendId?.let(legendsById::get)
                PresetUiState(
                    id = preset.id,
                    name = preset.name,
                    colorId = preset.colorId,
                    legendName = legend?.name,
                    legendImageUri = legend?.imageUri,
                )
            }.toImmutableList(),
            presetEditor = draft?.let { edit ->
                PresetEditorUiState(
                    id = edit.id,
                    name = edit.name,
                    colorId = edit.colorId,
                    legend = edit.legendId?.let(legendsById::get),
                    duplicateName = edit.duplicateName,
                    legendPicker = edit.legendQuery?.let { query ->
                        val trimmed = query.trim()
                        PresetLegendPickerUiState(
                            query = query,
                            legends = legends.filter {
                                trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true)
                            }
                                .toImmutableList(),
                        )
                    },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onThemeModeSelect(mode: ThemeMode) = launch { settingsRepository.setThemeMode(mode) }

    fun onDynamicColorToggle(enabled: Boolean) = launch { settingsRepository.setUseDynamicColor(enabled) }

    fun onHapticsToggle(enabled: Boolean) = launch { settingsRepository.setHapticsEnabled(enabled) }

    fun onKeepScreenOnToggle(enabled: Boolean) = launch { settingsRepository.setKeepScreenOn(enabled) }

    fun onImmersiveModeToggle(enabled: Boolean) = launch { settingsRepository.setImmersiveMode(enabled) }

    fun onDefaultFormatSelect(format: GameFormat) = launch { settingsRepository.setDefaultGameFormat(format) }

    fun onAddXpByDefaultToggle(enabled: Boolean) = launch { settingsRepository.setAddXpCounterByDefault(enabled) }

    fun onPresetDelete(id: String) = launch { presetRepository.delete(id) }

    /** Opens the editor on saved player [id] (ignored if it no longer exists). */
    fun onPresetEdit(id: String) = launch {
        val preset = presetRepository.presets.first().firstOrNull { it.id == id } ?: return@launch
        draft.value = PresetDraft(preset.id, preset.name, preset.colorId, preset.legendId)
    }

    fun onPresetNameChange(name: String) =
        editDraft { it.copy(name = name.take(MAX_PLAYER_NAME_LENGTH), duplicateName = null) }

    fun onPresetColorSelect(colorId: String) = editDraft { it.copy(colorId = colorId) }

    fun onPresetLegendPickerOpen() = editDraft { it.copy(legendQuery = "") }

    fun onPresetLegendQueryChange(query: String) = editDraft { it.copy(legendQuery = query) }

    fun onPresetLegendSelect(legendId: String?) = editDraft { it.copy(legendId = legendId, legendQuery = null) }

    fun onPresetLegendPickerDismiss() = editDraft { it.copy(legendQuery = null) }

    /**
     * Writes the draft (same id, so the player's stats stay linked) and closes the editor. A name another
     * saved player already has (names are unique) is rejected with an inline error instead.
     */
    fun onPresetEditSave() {
        val edit = draft.value ?: return
        val name = edit.name.trim()
        if (name.isEmpty()) return
        // Checked synchronously, against the name as it is now (a later edit can't get a stale error).
        val clash = latestPresets.firstOrNull { it.id != edit.id && it.name.trim().equals(name, ignoreCase = true) }
        if (clash != null) {
            editDraft { it.copy(duplicateName = clash.name) }
            return
        }
        draft.value = null
        launch { presetRepository.update(PlayerPreset(edit.id, name, edit.colorId, edit.legendId)) }
    }

    fun onPresetEditDismiss() {
        draft.value = null
    }

    private fun editDraft(change: (PresetDraft) -> PresetDraft) = draft.update { it?.let(change) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

/**
 * The unsaved edit of a saved player. [legendQuery] is non-null while the Legend sheet is open;
 * [duplicateName] is set when "Save" hit another saved player's name, until the name is edited.
 */
private data class PresetDraft(
    val id: String,
    val name: String,
    val colorId: String,
    val legendId: String?,
    val legendQuery: String? = null,
    val duplicateName: String? = null,
)
