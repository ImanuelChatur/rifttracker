package com.rifttally.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Colorblind-friendly player palette, based on Okabe–Ito (blue lightened for dark backgrounds). */
public object PlayerColors {
    private val OnPlayerColor = Color(0xFF000000)

    public val Orange: PlayerColor = PlayerColor("orange", Color(0xFFE69F00), OnPlayerColor)
    public val Sky: PlayerColor = PlayerColor("sky", Color(0xFF56B4E9), OnPlayerColor)
    public val Green: PlayerColor = PlayerColor("green", Color(0xFF009E73), OnPlayerColor)
    public val Yellow: PlayerColor = PlayerColor("yellow", Color(0xFFF0E442), OnPlayerColor)
    public val Blue: PlayerColor = PlayerColor("blue", Color(0xFF6A9DFF), OnPlayerColor)
    public val Vermillion: PlayerColor = PlayerColor("vermillion", Color(0xFFE0661A), OnPlayerColor)
    public val Pink: PlayerColor = PlayerColor("pink", Color(0xFFCC79A7), OnPlayerColor)
    public val Grey: PlayerColor = PlayerColor("grey", Color(0xFFB4B4B4), OnPlayerColor)

    /**
     * Ordered so the first four (default seat colors) are the most distinct under common color-vision
     * deficiencies: orange / sky / green / pink. Similar pairs (orange–vermillion, sky–blue) are split.
     */
    public val all: List<PlayerColor> = listOf(Orange, Sky, Green, Pink, Yellow, Blue, Vermillion, Grey)

    /** Looks up a color by its stable [PlayerColor.id], or `null` if unknown. */
    public fun fromId(id: String): PlayerColor? = all.firstOrNull { it.id == id }

    /** Default color for the seat at [index] (wraps around). */
    public fun forSeat(index: Int): PlayerColor = all[Math.floorMod(index, all.size)]
}
