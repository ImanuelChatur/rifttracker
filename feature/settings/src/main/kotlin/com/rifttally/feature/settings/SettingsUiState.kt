package com.rifttally.feature.settings

import com.rifttally.core.model.GameFormat
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
        val presets: ImmutableList<PresetUiState> = persistentListOf(),
    ) : SettingsUiState
}

/** A saved player as listed in Settings. [legendName] is `null` when none is set (or it's unknown). */
internal data class PresetUiState(val id: String, val name: String, val colorId: String, val legendName: String?)
