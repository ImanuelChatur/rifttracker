package com.rifttally.core.data.repository

import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import kotlinx.coroutines.flow.Flow

/** App-wide user settings. */
public interface UserSettingsRepository {
    public val userSettings: Flow<UserSettings>

    public suspend fun setThemeMode(mode: ThemeMode)

    public suspend fun setUseDynamicColor(enabled: Boolean)

    public suspend fun setHapticsEnabled(enabled: Boolean)

    public suspend fun setKeepScreenOn(enabled: Boolean)

    public suspend fun setImmersiveMode(enabled: Boolean)

    public suspend fun setDefaultGameFormat(format: GameFormat)

    public suspend fun setAddXpCounterByDefault(enabled: Boolean)
}
