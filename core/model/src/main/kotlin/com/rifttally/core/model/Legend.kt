package com.rifttally.core.model

/**
 * A Riftbound Legend a player can pick as their points icon.
 *
 * @property imageUri URI of the bundled square portrait, or `null` when no image exists (UI shows a fallback).
 * @property wideImageUri URI of the bundled wide art (`<id>_wide`), used as a panel background, or `null`.
 */
public data class Legend(
    val id: String,
    val name: String,
    val imageUri: String? = null,
    val wideImageUri: String? = null,
)
