package com.rifttally.core.stats

/**
 * [legendId]'s record against every other Legend it met in [games], counted **once per game**: a game
 * counts for an opponent Legend when it was picked on a side **different** from one of [legendId]'s
 * (2v2 teammates aren't opponents), however many times either Legend was picked. It's a win when a side
 * with [legendId] won, a loss when a side with the opponent Legend won instead. Mirror matches (the same
 * Legend on the other side) and players without a Legend are skipped; placeholder-named players count, as
 * in [legendStats]. Most games first, then most wins, then opponent id.
 */
public fun legendMatchups(games: List<StatsGame>, legendId: String): List<LegendMatchup> {
    val played = LinkedHashMap<String, Int>()
    val wins = HashMap<String, Int>()
    val losses = HashMap<String, Int>()
    games.forEach { game ->
        val sideOf = game.sideOfPlayer()
        val winner = game.validWinner
        val picks = game.config.players.mapNotNull { player ->
            val legend = player.legendId ?: return@mapNotNull null
            val side = sideOf[player.id] ?: return@mapNotNull null
            legend to side
        }
        val mySides = picks.filter { it.first == legendId }.map { it.second }.toSet()
        if (mySides.isEmpty()) return@forEach
        // Each opponent Legend once, with the sides it was picked on that face one of legendId's.
        val opponents = picks
            .filter { (legend, side) -> legend != legendId && mySides.any { it != side } }
            .groupBy({ it.first }, { it.second })
        opponents.forEach { (other, otherSides) ->
            played[other] = (played[other] ?: 0) + 1
            when (winner) {
                in mySides -> wins[other] = (wins[other] ?: 0) + 1
                in otherSides -> losses[other] = (losses[other] ?: 0) + 1
                else -> Unit
            }
        }
    }
    return played.map { (other, count) ->
        LegendMatchup(
            legendId = legendId,
            opponentLegendId = other,
            games = count,
            wins = wins[other] ?: 0,
            losses = losses[other] ?: 0,
        )
    }.sortedWith(
        compareByDescending<LegendMatchup> { it.games }
            .thenByDescending { it.wins }
            .thenBy { it.opponentLegendId },
    )
}
