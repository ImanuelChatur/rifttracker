package com.rifttally.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.PlaceholderScreen

/** Phase 0 placeholder for settings and about. */
@Composable
internal fun SettingsScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_settings_title),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.feature_settings_disclaimer),
            style = RiftTheme.typography.bodySmall,
            color = RiftTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        RiftIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.feature_settings_back),
            onClick = onBackClick,
        )
    }
}

@PreviewRiftThemes
@Composable
private fun SettingsScreenPreview() {
    RiftTheme {
        SettingsScreen(onBackClick = {})
    }
}
