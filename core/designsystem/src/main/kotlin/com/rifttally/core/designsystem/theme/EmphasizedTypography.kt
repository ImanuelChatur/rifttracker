package com.rifttally.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

// Emphasized type styles (Material 3 Expressive; internal in material3 1.4.0). DESIGN.md §5.
// A heavier weight of each role. Use for the ONE hero element of a surface, e.g.
// `RiftTheme.typography.titleLargeEmphasized`; everything else uses the regular role.

private fun TextStyle.emphasized(weight: FontWeight): TextStyle = copy(fontWeight = weight)

public val Typography.displayLargeEmphasized: TextStyle get() = displayLarge.emphasized(FontWeight.Bold)
public val Typography.displayMediumEmphasized: TextStyle get() = displayMedium.emphasized(FontWeight.Bold)
public val Typography.displaySmallEmphasized: TextStyle get() = displaySmall.emphasized(FontWeight.Bold)
public val Typography.headlineLargeEmphasized: TextStyle get() = headlineLarge.emphasized(FontWeight.Bold)
public val Typography.headlineMediumEmphasized: TextStyle get() = headlineMedium.emphasized(FontWeight.Bold)
public val Typography.headlineSmallEmphasized: TextStyle get() = headlineSmall.emphasized(FontWeight.Bold)
public val Typography.titleLargeEmphasized: TextStyle get() = titleLarge.emphasized(FontWeight.Bold)
public val Typography.titleMediumEmphasized: TextStyle get() = titleMedium.emphasized(FontWeight.Bold)
public val Typography.titleSmallEmphasized: TextStyle get() = titleSmall.emphasized(FontWeight.Bold)
public val Typography.labelLargeEmphasized: TextStyle get() = labelLarge.emphasized(FontWeight.Bold)
public val Typography.labelMediumEmphasized: TextStyle get() = labelMedium.emphasized(FontWeight.Bold)
public val Typography.bodyLargeEmphasized: TextStyle get() = bodyLarge.emphasized(FontWeight.Medium)
public val Typography.bodyMediumEmphasized: TextStyle get() = bodyMedium.emphasized(FontWeight.Medium)
