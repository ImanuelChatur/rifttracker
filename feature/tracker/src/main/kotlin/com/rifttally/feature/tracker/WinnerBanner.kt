package com.rifttally.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.component.RiftButton
import com.rifttally.core.designsystem.component.RiftTextButton
import com.rifttally.core.designsystem.theme.RiftTheme
import kotlinx.collections.immutable.persistentListOf

/** Small, dismissible "Winner?" prompt. Never blocks score changes. */
@Composable
internal fun WinnerBanner(
    banner: WinnerBannerUiState,
    onEndGame: () -> Unit,
    onKeepPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(RiftTheme.spacing.lg),
        color = RiftTheme.colorScheme.primaryContainer,
        contentColor = RiftTheme.colorScheme.onPrimaryContainer,
        shadowElevation = RiftTheme.spacing.sm,
    ) {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            Text(text = stringResource(R.string.feature_tracker_banner_title), style = RiftTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.feature_tracker_banner_body, banner.names.joinToString()),
                style = RiftTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm)) {
                RiftTextButton(
                    text = stringResource(R.string.feature_tracker_banner_keep_playing),
                    onClick = onKeepPlaying,
                )
                RiftButton(text = stringResource(R.string.feature_tracker_banner_end), onClick = onEndGame)
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun WinnerBannerPreview() {
    RiftTheme {
        WinnerBanner(banner = WinnerBannerUiState(persistentListOf("Ana")), onEndGame = {}, onKeepPlaying = {})
    }
}
