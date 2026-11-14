package com.rifttally.feature.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.PlaceholderScreen

/** Phase 0 placeholder. Resume card and recent games arrive in later phases. */
@Composable
internal fun HomeScreen(onNewGameClick: () -> Unit, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_home_title),
        modifier = modifier,
    ) {
        RiftButton(text = stringResource(R.string.feature_home_new_game), onClick = onNewGameClick)
        RiftIconButton(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.feature_home_settings),
            onClick = onSettingsClick,
        )
    }
}

@PreviewRiftThemes
@Composable
private fun HomeScreenPreview() {
    RiftTheme {
        HomeScreen(onNewGameClick = {}, onSettingsClick = {})
    }
}
