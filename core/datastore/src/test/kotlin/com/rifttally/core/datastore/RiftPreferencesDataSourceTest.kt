package com.rifttally.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RiftPreferencesDataSourceTest {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    private val testScope = TestScope(UnconfinedTestDispatcher())

    private val dataStore = PreferenceDataStoreFactory.create(scope = testScope.backgroundScope) {
        tmp.newFile("test.preferences_pb")
    }

    private val dataSource = RiftPreferencesDataSource(dataStore)

    @Test
    fun emptyStore_emitsDefaults() = testScope.runTest {
        assertEquals(UserSettings(), dataSource.userSettings.first())
    }

    @Test
    fun setters_arePersisted() = testScope.runTest {
        dataSource.setThemeMode(ThemeMode.LIGHT)
        dataSource.setUseDynamicColor(true)
        dataSource.setHapticsEnabled(false)
        dataSource.setKeepScreenOn(false)
        dataSource.setImmersiveMode(false)
        dataSource.setDefaultGameFormat(GameFormat.TEAMS_2V2)
        dataSource.setAddXpCounterByDefault(true)
        dataSource.setPickFirstPlayer(false)

        assertEquals(
            UserSettings(
                themeMode = ThemeMode.LIGHT,
                useDynamicColor = true,
                hapticsEnabled = false,
                keepScreenOn = false,
                immersiveMode = false,
                defaultGameFormat = GameFormat.TEAMS_2V2,
                addXpCounterByDefault = true,
                pickFirstPlayer = false,
            ),
            dataSource.userSettings.first(),
        )
    }

    @Test
    fun unknownEnumValues_fallBackToDefaults() = testScope.runTest {
        dataStore.edit {
            it[RiftPreferencesDataSource.Keys.THEME_MODE] = "NEON"
            it[RiftPreferencesDataSource.Keys.DEFAULT_GAME_FORMAT] = "FIVE_PLAYERS"
        }

        val settings = dataSource.userSettings.first()

        assertEquals(UserSettings().themeMode, settings.themeMode)
        assertEquals(UserSettings().defaultGameFormat, settings.defaultGameFormat)
    }
}
