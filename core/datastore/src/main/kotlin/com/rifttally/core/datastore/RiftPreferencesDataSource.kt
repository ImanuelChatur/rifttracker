package com.rifttally.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Reads and writes [UserSettings] in Preferences DataStore. Missing or unknown values fall back to defaults,
 * and a read error yields defaults instead of crashing the app at launch.
 */
public class RiftPreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {
    public val userSettings: Flow<UserSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            val defaults = UserSettings()
            UserSettings(
                themeMode = prefs[Keys.THEME_MODE].toEnumOrNull<ThemeMode>() ?: defaults.themeMode,
                useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.useDynamicColor,
                hapticsEnabled = prefs[Keys.HAPTICS] ?: defaults.hapticsEnabled,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
                immersiveMode = prefs[Keys.IMMERSIVE] ?: defaults.immersiveMode,
                defaultGameFormat = prefs[Keys.DEFAULT_GAME_FORMAT].toEnumOrNull<GameFormat>()
                    ?: defaults.defaultGameFormat,
                addXpCounterByDefault = prefs[Keys.XP_BY_DEFAULT] ?: defaults.addXpCounterByDefault,
                pickFirstPlayer = prefs[Keys.PICK_FIRST_PLAYER] ?: defaults.pickFirstPlayer,
            )
        }

    public suspend fun setThemeMode(mode: ThemeMode): Unit = update { it[Keys.THEME_MODE] = mode.name }

    public suspend fun setUseDynamicColor(enabled: Boolean): Unit = update { it[Keys.DYNAMIC_COLOR] = enabled }

    public suspend fun setHapticsEnabled(enabled: Boolean): Unit = update { it[Keys.HAPTICS] = enabled }

    public suspend fun setKeepScreenOn(enabled: Boolean): Unit = update { it[Keys.KEEP_SCREEN_ON] = enabled }

    public suspend fun setImmersiveMode(enabled: Boolean): Unit = update { it[Keys.IMMERSIVE] = enabled }

    public suspend fun setDefaultGameFormat(format: GameFormat): Unit =
        update { it[Keys.DEFAULT_GAME_FORMAT] = format.name }

    public suspend fun setAddXpCounterByDefault(enabled: Boolean): Unit = update { it[Keys.XP_BY_DEFAULT] = enabled }

    public suspend fun setPickFirstPlayer(enabled: Boolean): Unit = update { it[Keys.PICK_FIRST_PLAYER] = enabled }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        dataStore.edit { block(it) }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } }

    internal object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAPTICS = booleanPreferencesKey("haptics")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val IMMERSIVE = booleanPreferencesKey("immersive_mode")
        val DEFAULT_GAME_FORMAT = stringPreferencesKey("default_game_format")
        val XP_BY_DEFAULT = booleanPreferencesKey("xp_counter_by_default")
        val PICK_FIRST_PLAYER = booleanPreferencesKey("pick_first_player")
    }
}
