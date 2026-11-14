package com.rifttally.feature.history

import com.rifttally.core.model.Legend
import com.rifttally.core.stats.GameSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Separator between teammates' names and Legends ("Ana & Ben"), same as the tracker and Home. */
internal const val TEAM_SEPARATOR = " & "

/** The summary's sides as UI rows, resolving Legend names and portraits from [legendsById]. */
internal fun GameSummary.sideUiStates(legendsById: Map<String, Legend>): ImmutableList<HistorySideUiState> =
    sides.map { side ->
        val first = side.players.firstOrNull()
        val firstLegend = first?.legendId?.let(legendsById::get)
        val legendNames = side.players.mapNotNull { player -> player.legendId?.let { legendsById[it]?.name ?: it } }
        HistorySideUiState(
            name = side.players.joinToString(TEAM_SEPARATOR) { it.name },
            legendNames = legendNames.takeIf { it.isNotEmpty() }?.joinToString(TEAM_SEPARATOR),
            colorId = first?.colorId.orEmpty(),
            legendName = firstLegend?.name,
            legendImageUri = firstLegend?.imageUri,
            score = side.finalScore,
            isWinner = side.isWinner,
        )
    }.toImmutableList()
