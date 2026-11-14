package com.rifttally.core.model

/**
 * A Riftbound Legend a player can pick as their points icon.
 *
 * @property imageUri URI of the bundled portrait, or `null` when no image exists (UI shows a fallback).
 */
public data class Legend(val id: String, val name: String, val imageUri: String? = null)
