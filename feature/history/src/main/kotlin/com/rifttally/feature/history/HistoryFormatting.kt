package com.rifttally.feature.history

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rifttally.core.model.GameFormat

/** "2 players", "3 players", "4 players" or "2v2". */
@Composable
internal fun formatLabel(format: GameFormat): String = if (format.isTeams) {
    stringResource(R.string.feature_history_format_teams)
} else {
    pluralStringResource(R.plurals.feature_history_format_players, format.playerCount, format.playerCount)
}

/** Localized date and time, e.g. "Nov 14, 2026, 7:20 PM". */
@Composable
internal fun dateTimeText(epochMillis: Long): String = DateUtils.formatDateTime(
    LocalContext.current,
    epochMillis,
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME or
        DateUtils.FORMAT_ABBREV_MONTH,
)

/** "Under a minute", "25 minutes" under an hour, "1 h 5 min" from an hour on. */
@Composable
internal fun durationText(minutes: Long): String {
    if (minutes < 1) return stringResource(R.string.feature_history_duration_under_minute)
    val hours = (minutes / MINUTES_PER_HOUR).toInt()
    val rest = (minutes % MINUTES_PER_HOUR).toInt()
    return if (hours == 0) {
        pluralStringResource(R.plurals.feature_history_duration_minutes, rest, rest)
    } else {
        stringResource(R.string.feature_history_duration_hours, hours, rest)
    }
}

private const val MINUTES_PER_HOUR = 60
