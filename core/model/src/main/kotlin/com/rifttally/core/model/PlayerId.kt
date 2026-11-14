package com.rifttally.core.model

import kotlinx.serialization.Serializable

/** Stable identifier of a player. */
@Serializable
@JvmInline
public value class PlayerId(public val value: String)
