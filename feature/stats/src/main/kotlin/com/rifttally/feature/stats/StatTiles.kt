package com.rifttally.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.displayLargeEmphasized

/**
 * A detail screen's hero (DESIGN.md §2.2): [label] over a win rate in emphasized display type, on an
 * emphasized container. [leading] (e.g. a Legend portrait) sits before the text.
 */
@Composable
internal fun WinRateHero(
    label: String,
    winRatePercent: Int,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RiftTheme.shapes.extraLarge,
        color = RiftTheme.colorScheme.primaryContainer,
        contentColor = RiftTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
        ) {
            leading?.invoke()
            Column {
                Text(text = label, style = RiftTheme.typography.labelLarge)
                Text(
                    text = stringResource(R.string.feature_stats_percent, winRatePercent),
                    style = RiftTheme.typography.displayLargeEmphasized.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

/** Equal-height tonal tiles in one row (give each [StatTile] `Modifier.weight(1f)`). */
@Composable
internal fun StatTileRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xxs),
        content = content,
    )
}

/** A quiet tile: [label], a big [value] and an optional [supporting] line. */
@Composable
internal fun StatTile(label: String, value: String, modifier: Modifier = Modifier, supporting: String? = null) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RiftTheme.shapes.large,
        color = RiftTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(RiftTheme.spacing.lg),
        ) {
            Text(
                text = label,
                style = RiftTheme.typography.labelLarge,
                color = RiftTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = RiftTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = RiftTheme.typography.bodyMedium,
                    color = RiftTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun StatTilesPreview() {
    RiftTheme {
        Column(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            WinRateHero(label = "Win rate", winRatePercent = 71)
            StatTileRow {
                StatTile(label = "Games", value = "7", modifier = Modifier.weight(1f))
                StatTile(label = "Wins", value = "5", modifier = Modifier.weight(1f), supporting = "3 games")
            }
        }
    }
}
