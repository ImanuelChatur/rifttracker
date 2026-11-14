package com.rifttally.core.stats

/**
 * Stats for everyone who played in [games], grouped by [PlayerKey]. Sorted by games played
 * (most first), then by name.
 */
public fun playerStats(games: List<StatsGame>): List<PlayerStats> = appearances(games)
    .groupBy { it.key }
    .map { (key, played) -> played.toPlayerStats(key) }
    .sortedWith(compareByDescending<PlayerStats> { it.games }.thenBy { it.name.lowercase() })

/** Stats for the person with [key], or `null` if they didn't play in [games]. */
public fun playerStats(games: List<StatsGame>, key: PlayerKey): PlayerStats? =
    appearances(games).filter { it.key == key }.takeIf { it.isNotEmpty() }?.toPlayerStats(key)

/** The minimum games a player needs to be ranked by win rate (see [topPlayersByWinRate]). */
public const val MIN_GAMES_FOR_RANKING: Int = 3

/**
 * Players with at least [minGames] games, best win rate first. Ties: more wins, then more games,
 * then name.
 */
public fun topPlayersByWinRate(games: List<StatsGame>, minGames: Int = MIN_GAMES_FOR_RANKING): List<PlayerStats> =
    playerStats(games)
        .filter { it.games >= minGames }
        .sortedWith(
            compareByDescending<PlayerStats> { it.winRate }
                .thenByDescending { it.wins }
                .thenByDescending { it.games }
                .thenBy { it.name.lowercase() },
        )

/** [this] must be one person's appearances, newest first. */
private fun List<Appearance>.toPlayerStats(key: PlayerKey): PlayerStats {
    // Insertion order = newest first, so maxBy (first max wins) prefers the most recently picked Legend.
    val legendCounts = LinkedHashMap<String, Int>()
    forEach { appearance -> appearance.legendIds.forEach { legendCounts[it] = (legendCounts[it] ?: 0) + 1 } }
    return PlayerStats(
        key = key,
        name = first().name,
        games = size,
        wins = count { it.won },
        currentWinStreak = takeWhile { it.won }.size,
        favoriteLegendId = legendCounts.maxByOrNull { it.value }?.key,
    )
}
