package com.rifttally.core.model

/** User preferences. Defaults are what a fresh install uses (dark-first, for dim game stores). */
public data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val immersiveMode: Boolean = true,
    val defaultGameFormat: GameFormat = GameFormat.TWO_PLAYERS,
    val addXpCounterByDefault: Boolean = false,
    /** Randomly pick who goes first when a game is created. */
    val pickFirstPlayer: Boolean = true,
)
