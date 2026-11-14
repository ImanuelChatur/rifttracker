package com.rifttally.core.model

import kotlinx.serialization.Serializable

/** Stable identifier of a game. */
@Serializable
@JvmInline
public value class GameId(public val value: String)
