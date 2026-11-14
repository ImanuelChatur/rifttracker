package com.rifttally.core.stats

/**
 * The stats front page, see [statsOverview].
 *
 * @property mostPlayedLegend the most picked Legend, or `null` when no Legend was ever picked.
 * @property topPlayers players with at least [MIN_GAMES_FOR_RANKING] games, best win rate first.
 * @property players everyone, most games first.
 */
public data class StatsOverview(
    val totalGames: Int,
    val mostPlayedLegend: LegendStats?,
    val topPlayers: List<PlayerStats>,
    val players: List<PlayerStats>,
)

/** Builds the [StatsOverview] of [games]. */
public fun statsOverview(games: List<StatsGame>): StatsOverview = StatsOverview(
    totalGames = games.size,
    mostPlayedLegend = legendStats(games).firstOrNull(),
    topPlayers = topPlayersByWinRate(games),
    players = playerStats(games),
)
