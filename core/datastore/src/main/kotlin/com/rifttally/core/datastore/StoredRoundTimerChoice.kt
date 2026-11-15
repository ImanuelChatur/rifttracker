package com.rifttally.core.datastore

/** The stored "Round timer" choice of Setup. [minutes] is `null` until a length was ever chosen. */
public data class StoredRoundTimerChoice(val enabled: Boolean = false, val minutes: Int? = null)
