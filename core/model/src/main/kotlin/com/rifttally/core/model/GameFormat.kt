package com.rifttally.core.model

/** Table format chosen in setup. [TEAMS_2V2] is four players in two teams sharing points. */
public enum class GameFormat(public val playerCount: Int) {
    TWO_PLAYERS(2),
    THREE_PLAYERS(3),
    FOUR_PLAYERS(4),
    TEAMS_2V2(4),
}
