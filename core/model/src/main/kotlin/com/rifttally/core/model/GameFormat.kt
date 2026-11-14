package com.rifttally.core.model

/** Table format chosen in setup. [TEAMS_2V2] is four players in two teams sharing points. */
public enum class GameFormat(public val playerCount: Int, public val defaultTargetScore: Int) {
    TWO_PLAYERS(2, DEFAULT_TARGET_SCORE),
    THREE_PLAYERS(3, DEFAULT_TARGET_SCORE),
    FOUR_PLAYERS(4, DEFAULT_TARGET_SCORE),
    TEAMS_2V2(4, DEFAULT_TEAM_TARGET_SCORE),
    ;

    /** Whether teammates share one point total (two sides of two players). */
    public val isTeams: Boolean get() = this == TEAMS_2V2
}

/** Default target score for free-for-all games. */
public const val DEFAULT_TARGET_SCORE: Int = 8

/** Default target score for 2v2 team games. */
public const val DEFAULT_TEAM_TARGET_SCORE: Int = 11

/** Smallest valid target score. */
public const val MIN_TARGET_SCORE: Int = 1
