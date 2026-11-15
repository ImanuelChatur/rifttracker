package com.rifttally.core.stats

/**
 * [legendId]'s record against every other Legend it met in [games]. Every pick of [legendId] is paired
 * with every pick on a **different** side of the same game (2v2 teammates aren't opponents). Mirror
 * matches (the same Legend on the other side) and players without a Legend are skipped; placeholder-named
 * players count, as in [legendStats]. Most games first, then most wins, then opponent id.
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
        picks.filter { it.first == legendId }.forEach { (_, mySide) ->
            picks.filter { (legend, side) -> side != mySide && legend != legendId }.forEach { (other, otherSide) ->
                played[other] = (played[other] ?: 0) + 1
                if (winner == mySide) wins[other] = (wins[other] ?: 0) + 1
                if (winner == otherSide) losses[other] = (losses[other] ?: 0) + 1
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
