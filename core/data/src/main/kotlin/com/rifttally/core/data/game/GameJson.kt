package com.rifttally.core.data.game

import kotlinx.serialization.json.Json

/**
 * JSON used for stored configs and event payloads. Unknown keys are ignored so data written by a
 * newer app version can still be read after a downgrade.
 */
internal val GameJson: Json = Json { ignoreUnknownKeys = true }
