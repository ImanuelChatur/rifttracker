package com.rifttally.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.LineHeightStyle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme

/** Test tag set on a [LegendBackdrop] once its art has loaded (screenshot tests wait for it). */
public const val LEGEND_ART_TEST_TAG: String = "legend_art"

/**
 * Full-bleed Legend art for a panel or card background (`ContentScale.Crop`), decorative.
 *
 * Tries [imageUri] (e.g. the wide art), then [fallbackImageUri] (e.g. the square portrait) if the
 * first is missing or fails. Over the art it draws a bottom-to-top dark scrim when [scrim] is true, so
 * light text stays readable. With no art at all (or while loading), it shows a subtle gradient in
 * [playerColor] with a large, faint initial of [legendName] (or [fallbackName]).
 *
 * [content] is drawn on top and receives `hasArt`, so it can pick light-on-scrim colors only when the
 * art is actually showing.
 */
@Composable
public fun LegendBackdrop(
    legendName: String?,
    imageUri: String?,
    fallbackName: String,
    playerColor: PlayerColor,
    modifier: Modifier = Modifier,
    fallbackImageUri: String? = null,
    scrim: Boolean = true,
    content: @Composable BoxScope.(hasArt: Boolean) -> Unit = {},
) {
    val uris = remember(imageUri, fallbackImageUri) { listOfNotNull(imageUri, fallbackImageUri).distinct() }
    var index by remember(uris) { mutableIntStateOf(0) }
    val uri = uris.getOrNull(index)
    Box(modifier = modifier) {
        val painter = uri?.let { rememberAsyncImagePainter(model = it, contentScale = ContentScale.Crop) }
        val painterState = painter?.state?.collectAsState()?.value
        LaunchedEffect(painterState) {
            if (painterState is AsyncImagePainter.State.Error) index++
        }
        val hasArt = painterState is AsyncImagePainter.State.Success
        if (!hasArt) {
            ColorFallback(
                initial = avatarInitial(legendName, fallbackName),
                playerColor = playerColor,
                modifier = Modifier.matchParentSize(),
            )
        }
        // Always drawn: Coil resolves its size from drawing. Empty until loaded.
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .then(if (hasArt) Modifier.testTag(LEGEND_ART_TEST_TAG) else Modifier),
            )
        }
        if (hasArt && scrim) {
            val colors = RiftTheme.tableColors
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to colors.scrimTop,
                            SCRIM_MIDDLE_STOP to colors.scrimMiddle,
                            1f to colors.scrimBottom,
                        ),
                    ),
            )
        }
        content(hasArt)
    }
}

/** Subtle vertical gradient in the player's color with a large, faint initial. */
@Composable
private fun ColorFallback(initial: String, playerColor: PlayerColor, modifier: Modifier = Modifier) {
    val base = RiftTheme.tableColors.panelBase
    BoxWithConstraints(
        modifier = modifier
            .background(base)
            .background(
                Brush.verticalGradient(
                    0f to playerColor.color.copy(alpha = FALLBACK_TOP_ALPHA),
                    1f to playerColor.color.copy(alpha = FALLBACK_BOTTOM_ALPHA),
                ),
            )
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        val fontSize = with(LocalDensity.current) { (minOf(maxWidth, maxHeight) * INITIAL_RATIO).toSp() }
        Text(
            text = initial,
            color = playerColor.color.copy(alpha = INITIAL_ALPHA),
            style = RiftTheme.scoreTypography.scoreHuge.copy(
                fontSize = fontSize,
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}

private const val SCRIM_MIDDLE_STOP = 0.55f
private const val FALLBACK_TOP_ALPHA = 0.10f
private const val FALLBACK_BOTTOM_ALPHA = 0.42f
private const val INITIAL_RATIO = 0.9f
private const val INITIAL_ALPHA = 0.12f

@PreviewRiftThemes
@Composable
private fun LegendBackdropPreview() {
    RiftTheme {
        Row(
            modifier = Modifier.padding(RiftTheme.spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            listOf(PlayerColors.Orange to "Jinx", PlayerColors.Sky to null).forEach { (color, legend) ->
                LegendBackdrop(
                    legendName = legend,
                    imageUri = null,
                    fallbackName = "Ana",
                    playerColor = color,
                    modifier = Modifier
                        .size(RiftTheme.sizes.avatarLarge)
                        .clip(RoundedCornerShape(RiftTheme.sizes.panelCorner)),
                )
            }
        }
    }
}
