package com.rifttally.core.model

import kotlinx.serialization.Serializable

/** A generic per-player counter shown under the points, e.g. XP. [value] is never below 0. */
@Serializable
public data class Counter(val id: CounterId, val label: String, val value: Int = 0)
