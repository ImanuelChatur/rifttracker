package com.rifttally.core.testing.repository

import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [UserSettingsRepository] for ViewModel tests. */
public class FakeUserSettingsRepository(initial: UserSettings = UserSettings()) : UserSettingsRepository {
    private val state = MutableStateFlow(initial)

    override val userSettings: Flow<UserSettings> = state

    public val current: UserSettings get() = state.value

    override suspend fun setThemeMode(mode: ThemeMode): Unit = state.update { it.copy(themeMode = mode) }

    override suspend fun setUseDynamicColor(enabled: Boolean): Unit = state.update {
        it.copy(useDynamicColor = enabled)
    }

    override suspend fun setHapticsEnabled(enabled: Boolean): Unit = state.update { it.copy(hapticsEnabled = enabled) }

    override suspend fun setKeepScreenOn(enabled: Boolean): Unit = state.update { it.copy(keepScreenOn = enabled) }

    override suspend fun setImmersiveMode(enabled: Boolean): Unit = state.update { it.copy(immersiveMode = enabled) }

    override suspend fun setDefaultGameFormat(format: GameFormat): Unit =
        state.update { it.copy(defaultGameFormat = format) }

    override suspend fun setAddXpCounterByDefault(enabled: Boolean): Unit =
        state.update { it.copy(addXpCounterByDefault = enabled) }

    override suspend fun setPickFirstPlayer(enabled: Boolean): Unit =
        state.update { it.copy(pickFirstPlayer = enabled) }
}
