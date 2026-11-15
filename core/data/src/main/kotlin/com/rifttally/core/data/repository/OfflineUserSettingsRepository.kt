package com.rifttally.core.data.repository

import com.rifttally.core.datastore.RiftPreferencesDataSource
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class OfflineUserSettingsRepository @Inject constructor(private val dataSource: RiftPreferencesDataSource) :
    UserSettingsRepository {
    override val userSettings: Flow<UserSettings> = dataSource.userSettings

    override suspend fun setThemeMode(mode: ThemeMode): Unit = dataSource.setThemeMode(mode)

    override suspend fun setUseDynamicColor(enabled: Boolean): Unit = dataSource.setUseDynamicColor(enabled)

    override suspend fun setHapticsEnabled(enabled: Boolean): Unit = dataSource.setHapticsEnabled(enabled)

    override suspend fun setKeepScreenOn(enabled: Boolean): Unit = dataSource.setKeepScreenOn(enabled)

    override suspend fun setImmersiveMode(enabled: Boolean): Unit = dataSource.setImmersiveMode(enabled)

    override suspend fun setDefaultGameFormat(format: GameFormat): Unit = dataSource.setDefaultGameFormat(format)

    override suspend fun setAddXpCounterByDefault(enabled: Boolean): Unit = dataSource.setAddXpCounterByDefault(enabled)

    override suspend fun setPickFirstPlayer(enabled: Boolean): Unit = dataSource.setPickFirstPlayer(enabled)
}
