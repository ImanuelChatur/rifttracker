package com.rifttally.core.model

import kotlinx.serialization.Serializable

/** Stable identifier of a side. */
@Serializable
@JvmInline
public value class SideId(public val value: String)
