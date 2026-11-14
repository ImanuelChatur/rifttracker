package com.rifttally.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rifttally.core.designsystem.R

/**
 * Display font for scores and headings: Barlow Condensed (SIL Open Font License 1.1), bundled in
 * `res/font`. Condensed, so large numbers fit narrow panels; tabular figures keep digits from jumping.
 * Body text stays on the platform default.
 */
public val RiftDisplayFontFamily: FontFamily = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/**
 * Large, high-contrast number styles for scores and counters. Tabular figures keep digits from jumping.
 * [scoreHuge] is the table-side score on a full player panel (panels scale it to fit; see `SidePanel`).
 */
@Immutable
public data class RiftScoreTypography(
    public val scoreHuge: TextStyle = scoreStyle(fontSize = 136, lineHeight = 144),
    public val scoreLarge: TextStyle = scoreStyle(fontSize = 96, lineHeight = 104),
    public val scoreMedium: TextStyle = scoreStyle(fontSize = 64, lineHeight = 72),
    public val scoreSmall: TextStyle = scoreStyle(fontSize = 40, lineHeight = 48),
    public val counter: TextStyle = scoreStyle(fontSize = 22, lineHeight = 28, weight = FontWeight.SemiBold),
)

private fun scoreStyle(fontSize: Int, lineHeight: Int, weight: FontWeight = FontWeight.Bold): TextStyle = TextStyle(
    fontFamily = RiftDisplayFontFamily,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Material typography: display, headline and large title styles use the display font; everything else
 * (body, labels, smaller titles) stays on the default font, slightly heavier for legibility at the table.
 */
internal val RiftMaterialTypography: Typography = Typography().let { base ->
    fun TextStyle.display(): TextStyle = copy(fontFamily = RiftDisplayFontFamily, fontWeight = FontWeight.Bold)
    base.copy(
        displayLarge = base.displayLarge.display(),
        displayMedium = base.displayMedium.display(),
        displaySmall = base.displaySmall.display(),
        headlineLarge = base.headlineLarge.display(),
        headlineMedium = base.headlineMedium.display(),
        headlineSmall = base.headlineSmall.display(),
        titleLarge = base.titleLarge.copy(fontFamily = RiftDisplayFontFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
