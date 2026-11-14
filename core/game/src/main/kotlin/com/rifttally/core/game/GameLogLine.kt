package com.rifttally.core.game

/** One display line of a game's event log, built by [gameLog]. Names are display names, not ids. */
public sealed interface GameLogLine {
    /** The id of the event this line describes. */
    public val id: String

    public data class Points(override val id: String, val name: String, val delta: Int, val total: Int) : GameLogLine

    public data class CounterChanged(
        override val id: String,
        val name: String,
        val label: String,
        val delta: Int,
        val total: Int,
    ) : GameLogLine

    public data class CounterAdded(override val id: String, val name: String, val label: String) : GameLogLine

    public data class CounterRemoved(override val id: String, val name: String, val label: String) : GameLogLine

    public data class TargetChanged(override val id: String, val targetScore: Int) : GameLogLine

    /** [winnerName] is `null` when the game ended without a winner. */
    public data class GameEnded(override val id: String, val winnerName: String?) : GameLogLine
}
