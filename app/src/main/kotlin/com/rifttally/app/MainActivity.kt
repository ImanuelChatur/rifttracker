package com.rifttally.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rifttally.app.navigation.RiftTallyApp
import com.rifttally.core.common.dispatchers.IoDispatcher
import com.rifttally.core.data.repository.UserSettingsRepository
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.model.UserSettings
import com.rifttally.core.ui.LocalIoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    /** Handed to the UI through [LocalIoDispatcher] (writing shared result pictures). */
    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings: UserSettings? by userSettingsRepository.userSettings
                .collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = when (settings?.themeMode ?: UserSettings().themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // System bar icons follow the app's theme setting, not just the system one.
            DisposableEffect(darkTheme) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            RiftTheme(
                darkTheme = darkTheme,
                dynamicColor = settings?.useDynamicColor ?: false,
            ) {
                CompositionLocalProvider(LocalIoDispatcher provides ioDispatcher) {
                    RiftTallyApp()
                }
            }
        }
    }
}
