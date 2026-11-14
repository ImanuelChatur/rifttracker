package com.rifttally.core.stats

/**
 * Stats for every Legend picked in [games] (players without a Legend are skipped). Sorted by games
 * (most first), then win rate, then id.
 */
public fun legendStats(games: List<StatsGame>): List<LegendStats> {
    val picks = LinkedHashMap<String, Int>()
    val wins = LinkedHashMap<String, Int>()
    games.forEach { game ->
        val sideOf = game.sideOfPlayer()
        val winner = game.validWinner
        game.config.players.forEach { player ->
            val legendId = player.legendId ?: return@forEach
            val side = sideOf[player.id] ?: return@forEach
            picks[legendId] = (picks[legendId] ?: 0) + 1
            if (side == winner) wins[legendId] = (wins[legendId] ?: 0) + 1
        }
    }
    return picks.map { (id, count) -> LegendStats(legendId = id, games = count, wins = wins[id] ?: 0) }
        .sortedWith(
            compareByDescending<LegendStats> { it.games }
                .thenByDescending { it.winRate }
                .thenBy { it.legendId },
        )
}
