package com.rifttally.feature.setup

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.PlaceholderScreen

/** Phase 0 placeholder for game setup. */
@Composable
internal fun SetupScreen(onBackClick: () -> Unit, onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_setup_title),
        modifier = modifier,
    ) {
        RiftButton(text = stringResource(R.string.feature_setup_start), onClick = onStartClick)
        RiftIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.feature_setup_back),
            onClick = onBackClick,
        )
    }
}

@PreviewRiftThemes
@Composable
private fun SetupScreenPreview() {
    RiftTheme {
        SetupScreen(onBackClick = {}, onStartClick = {})
    }
}
