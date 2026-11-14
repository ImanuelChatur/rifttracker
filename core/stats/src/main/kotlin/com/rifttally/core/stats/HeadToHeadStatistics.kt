package com.rifttally.core.stats

/** [player]'s record against [opponent] in [games]. All zero if they never faced each other. */
public fun headToHead(games: List<StatsGame>, player: PlayerKey, opponent: PlayerKey): HeadToHead {
    val byOpponent = headToHeads(games, player).firstOrNull { it.opponent == opponent }
    return byOpponent ?: HeadToHead(
        player = player,
        opponent = opponent,
        opponentName = appearances(games).firstOrNull { it.key == opponent }?.name.orEmpty(),
        games = 0,
        wins = 0,
        opponentWins = 0,
    )
}

/** [player]'s record against everyone they faced in [games]. Most games first, then by name. */
public fun headToHeads(games: List<StatsGame>, player: PlayerKey): List<HeadToHead> {
    val byGame = appearances(games).groupBy { it.gameId }
    // Newest game first (appearances are), so the first name seen per opponent is their latest.
    val names = LinkedHashMap<PlayerKey, String>()
    val played = HashMap<PlayerKey, Int>()
    val wins = HashMap<PlayerKey, Int>()
    val opponentWins = HashMap<PlayerKey, Int>()
    byGame.values.forEach { inGame ->
        val me = inGame.firstOrNull { it.key == player } ?: return@forEach
        inGame.filter { it.key != player && it.sides.none(me.sides::contains) }.forEach { other ->
            names.getOrPut(other.key) { other.name }
            played[other.key] = (played[other.key] ?: 0) + 1
            if (me.won) wins[other.key] = (wins[other.key] ?: 0) + 1
            if (other.won) opponentWins[other.key] = (opponentWins[other.key] ?: 0) + 1
        }
    }
    return names.map { (key, name) ->
        HeadToHead(
            player = player,
            opponent = key,
            opponentName = name,
            games = played[key] ?: 0,
            wins = wins[key] ?: 0,
            opponentWins = opponentWins[key] ?: 0,
        )
    }.sortedWith(compareByDescending<HeadToHead> { it.games }.thenBy { it.opponentName.lowercase() })
}
