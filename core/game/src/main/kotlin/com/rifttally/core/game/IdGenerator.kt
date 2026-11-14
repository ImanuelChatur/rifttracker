package com.rifttally.core.game

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Creates unique ids for events and counters. Inject a deterministic one in tests. */
public fun interface IdGenerator {
    public fun newId(): String

    public companion object {
        /** Random (version 4) UUIDs. */
        @OptIn(ExperimentalUuidApi::class)
        public val RandomUuid: IdGenerator = IdGenerator { Uuid.random().toString() }
    }
}
