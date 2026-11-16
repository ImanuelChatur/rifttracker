package com.rifttally.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled

/**
 * The palette as 48dp radio swatches, split into even rows (4 + 4 on a phone, one row when wide). The
 * selected swatch grows a little on the expressive spring and shows a check (not color alone).
 */
@Composable
public fun PlayerColorPicker(
    selectedColorId: String,
    onColorSelect: (colorId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RiftTheme.playerColors
    val target = RiftTheme.sizes.minTouchTarget
    val gap = RiftTheme.spacing.xs
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fit = ((maxWidth + gap) / (target + gap)).toInt().coerceIn(1, colors.size)
        val rows = (colors.size + fit - 1) / fit
        val perRow = (colors.size + rows - 1) / rows
        FlowRow(
            modifier = Modifier.selectableGroup(),
            maxItemsInEachRow = perRow,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            colors.forEachIndexed { index, color ->
                ColorSwatch(
                    color = color,
                    selected = color.id == selectedColorId,
                    description = stringResource(R.string.core_ui_color_description, index + 1, colors.size),
                    onClick = { onColorSelect(color.id) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: PlayerColor,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else UNSELECTED_SWATCH_SCALE,
        animationSpec = if (reduceMotionEnabled()) {
            RiftTheme.motion.none()
        } else {
            RiftTheme.motion.expressive.fastSpatial()
        },
        label = "swatchScale",
    )
    Box(
        modifier = modifier
            .size(RiftTheme.sizes.minTouchTarget)
            .clip(RiftTheme.shapes.full)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(RiftTheme.sizes.minTouchTarget - RiftTheme.spacing.xs)
                .scale(scale)
                .then(
                    if (selected) {
                        Modifier.border(
                            RiftTheme.sizes.borderThick,
                            RiftTheme.colorScheme.onSurface,
                            RiftTheme.shapes.full,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(if (selected) RiftTheme.sizes.borderThick + RiftTheme.spacing.xxs else 0.dp)
                .background(color.color, RiftTheme.shapes.full),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = RiftIcons.Check,
                    contentDescription = null,
                    tint = color.onColor,
                    modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                )
            }
        }
    }
}

private const val UNSELECTED_SWATCH_SCALE = 0.86f

@PreviewRiftThemes
@Composable
private fun PlayerColorPickerPreview() {
    RiftTheme {
        Surface { PlayerColorPicker(selectedColorId = "orange", onColorSelect = {}) }
    }
}
