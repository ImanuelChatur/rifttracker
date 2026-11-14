package com.rifttally.core.model

/** A saved player (name, color, Legend) that setup can fill in with one tap. */
public data class PlayerPreset(val id: String, val name: String, val colorId: String, val legendId: String? = null)
