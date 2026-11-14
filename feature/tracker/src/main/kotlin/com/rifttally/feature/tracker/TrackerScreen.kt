package com.rifttally.feature.tracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftIconButton
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.PlaceholderScreen

/** Phase 0 placeholder for the live table screen. */
@Composable
internal fun TrackerScreen(gameId: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = stringResource(R.string.feature_tracker_title),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.feature_tracker_game_id, gameId),
            style = RiftTheme.typography.bodyLarge,
            color = RiftTheme.colorScheme.onSurfaceVariant,
        )
        RiftIconButton(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.feature_tracker_back),
            onClick = onBackClick,
        )
    }
}

@PreviewRiftThemes
@Composable
private fun TrackerScreenPreview() {
    RiftTheme {
        TrackerScreen(gameId = "placeholder", onBackClick = {})
    }
}
