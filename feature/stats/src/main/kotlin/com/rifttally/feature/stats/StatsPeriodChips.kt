package com.rifttally.feature.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.stats.StatsPeriod

/**
 * Date range chips (DESIGN.md §9: filter chips, small radius): one row that scrolls horizontally at large
 * font scales, centered with the list's max width.
 */
@Composable
internal fun StatsPeriodChips(selected: StatsPeriod, onSelect: (StatsPeriod) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .widthIn(max = RiftTheme.sizes.listMaxContentWidth)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = RiftTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            StatsPeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == selected,
                    onClick = { onSelect(period) },
                    label = { Text(stringResource(period.label)) },
                    shape = RiftTheme.shapes.small,
                )
            }
        }
    }
}

/** "All time", "Last 30 days" or "This year". */
@get:StringRes
internal val StatsPeriod.label: Int
    get() = when (this) {
        StatsPeriod.ALL_TIME -> R.string.feature_stats_period_all_time
        StatsPeriod.LAST_30_DAYS -> R.string.feature_stats_period_last_30_days
        StatsPeriod.THIS_YEAR -> R.string.feature_stats_period_this_year
    }

/** [label] followed by the date range, e.g. "Win rate · This year"; just [label] for all time. */
@Composable
internal fun labelWithPeriod(label: String, period: StatsPeriod): String = if (period == StatsPeriod.ALL_TIME) {
    label
} else {
    stringResource(R.string.feature_stats_label_with_period, label, stringResource(period.label))
}

@PreviewRiftThemes
@Composable
private fun StatsPeriodChipsPreview() {
    RiftTheme {
        Surface(color = RiftTheme.colorScheme.background) {
            StatsPeriodChips(selected = StatsPeriod.LAST_30_DAYS, onSelect = {})
        }
    }
}
