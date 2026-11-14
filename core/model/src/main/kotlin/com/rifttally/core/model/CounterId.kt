package com.rifttally.core.model

import kotlinx.serialization.Serializable

/** Stable identifier of a counter. */
@Serializable
@JvmInline
public value class CounterId(public val value: String)
