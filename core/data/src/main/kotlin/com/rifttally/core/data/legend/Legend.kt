package com.rifttally.core.data.legend

// TEMPORARY (Phase 1B): the canonical home is :core:model (com.rifttally.core.model.Legend).
// Move it there during the Phase 1 merge and delete this file.

/**
 * A Legend a player can pick. [imageUri] is a Coil-loadable asset URI
 * (`file:///android_asset/legends/<id>.webp` or `.png`), or `null` when no image was added.
 */
public data class Legend(public val id: String, public val name: String, public val imageUri: String?)
