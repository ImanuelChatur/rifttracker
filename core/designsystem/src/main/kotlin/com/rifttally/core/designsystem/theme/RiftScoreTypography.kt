package com.rifttally.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Large, high-contrast number styles for scores and counters. Tabular figures keep digits from jumping. */
@Immutable
public data class RiftScoreTypography(
    public val scoreLarge: TextStyle = scoreStyle(fontSize = 96, lineHeight = 104),
    public val scoreMedium: TextStyle = scoreStyle(fontSize = 64, lineHeight = 72),
    public val scoreSmall: TextStyle = scoreStyle(fontSize = 40, lineHeight = 48),
    public val counter: TextStyle = scoreStyle(fontSize = 22, lineHeight = 28, weight = FontWeight.SemiBold),
)

private fun scoreStyle(fontSize: Int, lineHeight: Int, weight: FontWeight = FontWeight.Bold): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    fontFeatureSettings = "tnum",
)

/** Material typography; defaults with slightly heavier titles for legibility at the table. */
internal val RiftMaterialTypography: Typography = Typography().let { base ->
    base.copy(
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
