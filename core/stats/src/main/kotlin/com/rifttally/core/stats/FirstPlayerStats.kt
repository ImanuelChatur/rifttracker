package com.rifttally.core.stats

import com.rifttally.core.model.SideId

/**
 * A player's record when they went first vs when someone else did, see [firstPlayerSplit]. Both only
 * count games with a first-player pick and a winner.
 */
public data class FirstPlayerSplit(val goingFirst: WinRecord, val notFirst: WinRecord)

/**
 * How often the player who went first won: over [games] with a valid first-player pick and a valid
 * winner, a win when the first player's side won (in 2v2 that's their team).
 */
public fun firstPlayerRecord(games: List<StatsGame>): WinRecord {
    var played = 0
    var wins = 0
    games.forEach { game ->
        val side = game.firstPlayerSide() ?: return@forEach
        val winner = game.validWinner ?: return@forEach
        played++
        if (side == winner) wins++
    }
    return WinRecord(games = played, wins = wins)
}

/**
 * [player]'s record going first vs not, over the games they played that have a valid first-player pick
 * and a valid winner. "Going first" means one of their own seats was picked; a 2v2 teammate's pick counts
 * as not first. Placeholder-named seats are nobody (see [playerStats]).
 */
public fun firstPlayerSplit(games: List<StatsGame>, player: PlayerKey): FirstPlayerSplit {
    val byId = games.associateBy { it.id }
    var first = WinRecord(0, 0)
    var notFirst = WinRecord(0, 0)
    appearances(games).filter { it.key == player }.forEach { appearance ->
        val game = byId.getValue(appearance.gameId)
        if (game.firstPlayerSide() == null || game.validWinner == null) return@forEach
        val picked = game.config.players.first { it.id == game.firstPlayer }
        val isFirst = !picked.hasDefaultName && PlayerKey.of(picked) == player
        val win = if (appearance.won) 1 else 0
        if (isFirst) {
            first = WinRecord(first.games + 1, first.wins + win)
        } else {
            notFirst = WinRecord(notFirst.games + 1, notFirst.wins + win)
        }
    }
    return FirstPlayerSplit(goingFirst = first, notFirst = notFirst)
}

/** The side of the game's first player, or `null` without a pick or when the pick isn't seated. */
private fun StatsGame.firstPlayerSide(): SideId? = firstPlayer?.let { sideOfPlayer()[it] }
