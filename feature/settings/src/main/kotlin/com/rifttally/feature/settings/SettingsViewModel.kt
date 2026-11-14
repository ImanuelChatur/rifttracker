package com.rifttally.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rifttally.core.data.legend.LegendRepository
import com.rifttally.core.data.preset.PlayerPresetRepository
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App settings (DataStore) and the saved players list. Every change is persisted immediately. */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
    private val presetRepository: PlayerPresetRepository,
    legendRepository: LegendRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userSettings,
        presetRepository.presets,
        legendRepository.legends,
    ) { settings, presets, legends ->
        val legendNames = legends.associate { it.id to it.name }
        SettingsUiState.Success(
            themeMode = settings.themeMode,
            useDynamicColor = settings.useDynamicColor,
            hapticsEnabled = settings.hapticsEnabled,
            keepScreenOn = settings.keepScreenOn,
            immersiveMode = settings.immersiveMode,
            defaultGameFormat = settings.defaultGameFormat,
            addXpCounterByDefault = settings.addXpCounterByDefault,
            presets = presets.map { preset ->
                PresetUiState(
                    id = preset.id,
                    name = preset.name,
                    colorId = preset.colorId,
                    legendName = preset.legendId?.let(legendNames::get),
                )
            }.toImmutableList(),
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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
