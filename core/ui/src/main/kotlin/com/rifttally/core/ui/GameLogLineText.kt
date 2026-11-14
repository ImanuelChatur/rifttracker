package com.rifttally.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rifttally.core.game.GameLogLine

/** The display text of a game log line, e.g. "Ana +1 · 6" (tracker history sheet, game detail). */
@Composable
public fun GameLogLine.text(): String = when (this) {
    is GameLogLine.Points -> stringResource(R.string.core_ui_log_points, name, delta, total)

    is GameLogLine.CounterChanged -> stringResource(R.string.core_ui_log_counter, name, label, delta, total)

    is GameLogLine.CounterAdded -> stringResource(R.string.core_ui_log_counter_added, name, label)

    is GameLogLine.CounterRemoved -> stringResource(R.string.core_ui_log_counter_removed, name, label)

    is GameLogLine.TargetChanged -> stringResource(R.string.core_ui_log_target, targetScore)

    is GameLogLine.GameEnded -> winnerName?.let { winner -> stringResource(R.string.core_ui_log_ended_winner, winner) }
        ?: stringResource(R.string.core_ui_log_ended)
}
