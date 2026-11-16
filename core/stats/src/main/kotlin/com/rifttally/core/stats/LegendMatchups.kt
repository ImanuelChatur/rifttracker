package com.rifttally.core.stats

/**
 * [legendId]'s record against every other Legend it met in [games], counted **once per game**, however
 * many times either Legend was picked. They met when a pick of each sat on **different** sides (a
 * "facing pair"; 2v2 teammates aren't opponents). The game is a win when [legendId]'s side of a facing
 * pair won, a loss when the opponent's side of one won, and neither when both are true (e.g. a full 2v2
 * mirror), nobody won, or someone else won. Mirror matches (the same Legend on the other side) and players
 * without a Legend are skipped; placeholder-named players count, as in [legendStats]. Most games first,
 * then most wins, then opponent id.
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
        // Facing pairs (my side, their side) per opponent Legend; each opponent counts once per game.
        val facing = picks
            .filter { (legend, _) -> legend != legendId }
            .flatMap { (other, otherSide) -> mySides.filter { it != otherSide }.map { other to (it to otherSide) } }
            .groupBy({ it.first }, { it.second })
        facing.forEach { (other, pairs) ->
            played[other] = (played[other] ?: 0) + 1
            val won = pairs.any { (mine, _) -> winner == mine }
            val lost = pairs.any { (_, theirs) -> winner == theirs }
            if (won && !lost) wins[other] = (wins[other] ?: 0) + 1
            if (lost && !won) losses[other] = (losses[other] ?: 0) + 1
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
