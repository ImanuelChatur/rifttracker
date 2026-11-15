package com.rifttally.feature.tracker

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.icon.RiftIcons
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.CounterRow
import com.rifttally.core.ui.LegendBackdrop
import com.rifttally.core.ui.PlayerNameChip
import com.rifttally.core.ui.RepeatingClickBox
import com.rifttally.core.ui.edgeGlow
import kotlinx.collections.immutable.persistentListOf

/**
 * One side's panel, drawn in its own (upright) orientation; the caller turns it toward its seat (see
 * [TrackerTableLayout]).
 *
 * - Background: the Legend art full-bleed under a bottom-to-top scrim (a team splits it between both
 *   teammates), or a subtle gradient in the player's color with a large faint initial.
 * - The score is the hero, sized to the panel. Counters sit under it as pills; the name chip is at the
 *   bottom, nearest the player.
 * - Two tap zones (left half −, right half +, each with a faint glyph and its own ripple). Holding a
 *   zone repeats.
 * - The player's color is a thin edge glow; reaching the target adds a pulsing gold edge.
 *
 * @param topInset space kept free at the top for the floating control bar.
 */
@Composable
internal fun SidePanel(
    panel: SidePanelUiState,
    hapticsEnabled: Boolean,
    onPointsChange: (delta: Int) -> Unit,
    onCounterChange: (PlayerId, CounterId, delta: Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    onAddCounterClick: (PlayerId) -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
) {
    val haptics = LocalHapticFeedback.current
    val reducedMotion = reduceMotionEnabled()
    val change: (Int) -> Unit = { delta ->
        if (hapticsEnabled && (delta > 0 || panel.points > 0)) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        onPointsChange(delta)
    }
    // A stronger haptic when this side reaches the target (not when the table first shows it).
    var wasAtTarget by remember { mutableStateOf(panel.isAtTarget) }
    LaunchedEffect(panel.isAtTarget) {
        if (panel.isAtTarget && !wasAtTarget && hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
        wasAtTarget = panel.isAtTarget
    }
    val shape = RiftTheme.shapes.extraLarge
    val colors = panel.players.map { it.playerColor() }
    val edgeBrush = if (colors.size > 1) {
        Brush.horizontalGradient(colors.map { it.color })
    } else {
        SolidColor(colors.firstOrNull()?.color ?: PlayerColors.Grey.color)
    }
    // Each backdrop reports whether its art loaded; the panel is "on art" when any is showing.
    var artShown by remember { mutableStateOf(emptySet<Int>()) }
    val onArt = artShown.isNotEmpty()
    val tableColors = RiftTheme.tableColors
    val contentColor = if (onArt) tableColors.onArt else tableColors.onPanelBase
    Box(
        modifier = modifier
            .clip(shape)
            // The gold pulse is the outer modifier, so it draws over the player-color edge.
            .then(
                if (panel.isAtTarget) {
                    Modifier.edgeGlow(
                        brush = SolidColor(tableColors.targetGlow),
                        width = RiftTheme.sizes.edgeGlow * TARGET_GLOW_FACTOR,
                        shape = shape,
                        alpha = pulseAlpha(reducedMotion),
                    )
                } else {
                    Modifier
                },
            )
            .edgeGlow(brush = edgeBrush, width = RiftTheme.sizes.edgeGlow, shape = shape),
    ) {
        Row(modifier = Modifier.matchParentSize()) {
            panel.players.forEachIndexed { index, player ->
                LegendBackdrop(
                    legendName = player.legendName,
                    imageUri = player.legendWideImageUri,
                    fallbackImageUri = player.legendImageUri,
                    fallbackName = player.name,
                    playerColor = colors[index],
                    // One scrim over the whole panel (below), so a teammate without art is darkened too.
                    scrim = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) { hasArt ->
                    LaunchedEffect(hasArt) { artShown = if (hasArt) artShown + index else artShown - index }
                }
            }
        }
        if (onArt) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to tableColors.scrimTop,
                            SCRIM_MIDDLE_STOP to tableColors.scrimMiddle,
                            1f to tableColors.scrimBottom,
                        ),
                    ),
            )
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            TapZones(panel = panel, onChange = change, modifier = Modifier.matchParentSize())
            // Not clickable itself, so taps on the score fall through to the zones underneath.
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Short panels (seats in 3–4 player games, landscape): everything under the score shares ONE
                // dense row (name, counters without their label, round "+★"), so the score stays the one
                // hero element (DESIGN.md §2.2) and keeps most of the height (§5).
                val compact =
                    maxHeight - topInset < RiftTheme.sizes.trackerCompactPanelHeight && panel.players.size == 1
                val footerArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg, Alignment.CenterHorizontally)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInset)
                        .padding(if (compact) RiftTheme.spacing.sm else RiftTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
                ) {
                    HeroScore(
                        points = panel.points,
                        description = pluralStringResource(
                            R.plurals.feature_tracker_side_points,
                            panel.points,
                            panel.displayName,
                            panel.points,
                        ),
                        color = contentColor,
                        shadow = onArt,
                        reducedMotion = reducedMotion,
                        horizontalInset = RiftTheme.sizes.tapGlyph + RiftTheme.spacing.md,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                    if (panel.isAtTarget) TargetReachedLabel()
                    Row(
                        horizontalArrangement = footerArrangement,
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        panel.players.forEachIndexed { index, player ->
                            PlayerFooter(
                                player = player,
                                playerColor = colors[index],
                                pillColor = if (onArt) tableColors.artPill else tableColors.panelPill,
                                hapticsEnabled = hapticsEnabled,
                                onCounterChange = onCounterChange,
                                onCounterRemove = onCounterRemove,
                                onAddCounterClick = onAddCounterClick,
                                compact = compact,
                                modifier = if (panel.players.size > 1) Modifier.weight(1f) else Modifier,
                            )
                        }
                    }
                }
            }
        }
        panel.firstPlayerName?.let { name ->
            FirstPlayerMarker(
                name = name,
                isTeam = panel.players.size > 1,
                pillColor = if (onArt) tableColors.artPill else tableColors.panelPill,
                contentColor = contentColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = topInset)
                    .padding(RiftTheme.spacing.lg),
            )
        }
        DeltaBadge(
            value = panel.points,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset)
                .padding(RiftTheme.spacing.lg),
        )
    }
}

/** "1st" (or "1st · Ana" on a team) on the starting player's panel, until the first points change. */
@Composable
private fun FirstPlayerMarker(
    name: String,
    isTeam: Boolean,
    pillColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.feature_tracker_first_marker_description, name)
    Surface(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = RiftTheme.shapes.full,
        color = pillColor,
        contentColor = contentColor,
    ) {
        Text(
            text = if (isTeam) {
                stringResource(R.string.feature_tracker_first_marker_team, name)
            } else {
                stringResource(R.string.feature_tracker_first_marker)
            },
            style = RiftTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.sm, vertical = RiftTheme.spacing.xxs),
        )
    }
}

/** Left half −, right half +; each a separate ripple surface with a faint glyph near its outer edge. */
@Composable
private fun TapZones(panel: SidePanelUiState, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        TapZone(
            description = pluralStringResource(
                R.plurals.feature_tracker_decrease,
                panel.points,
                panel.displayName,
                panel.points,
            ),
            glyph = { Icon(imageVector = RiftIcons.Remove, contentDescription = null, modifier = it) },
            onClick = { onChange(-1) },
            alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        )
        TapZone(
            description = pluralStringResource(
                R.plurals.feature_tracker_increase,
                panel.points,
                panel.displayName,
                panel.points,
            ),
            glyph = { Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = it) },
            onClick = { onChange(1) },
            alignment = Alignment.CenterEnd,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TapZone(
    description: String,
    glyph: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    RepeatingClickBox(
        contentDescription = description,
        onClick = onClick,
        // The ripple is bounded to this half (and clipped by the panel's rounded corners).
        modifier = modifier.fillMaxHeight(),
        contentAlignment = alignment,
    ) {
        glyph(
            Modifier
                .padding(horizontal = RiftTheme.spacing.md)
                .size(RiftTheme.sizes.tapGlyph)
                .graphicsLayer { alpha = GLYPH_ALPHA },
        )
    }
}

/** A pulsing gold edge (static when animations are off). */
@Composable
private fun pulseAlpha(reducedMotion: Boolean): Float = if (reducedMotion) {
    1f
} else {
    val transition = rememberInfiniteTransition(label = "targetPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = RiftTheme.motion.pulse(),
        label = "targetPulseAlpha",
    )
    pulse
}

@Composable
private fun TargetReachedLabel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RiftTheme.shapes.full,
        color = RiftTheme.tableColors.targetGlow,
        contentColor = RiftTheme.tableColors.onTargetGlow,
    ) {
        Text(
            text = stringResource(R.string.feature_tracker_at_target),
            style = RiftTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.md, vertical = RiftTheme.spacing.xxs),
        )
    }
}

/**
 * One player's counters (keyed, so state follows the counter when one is removed), "+ Counter" and name.
 *
 * Normal panels: counters wrap in rows under the score, the name chip below them. [compact] panels: one
 * single dense row (name chip, label-less counter pills, round "+★") that scrolls sideways if it ever
 * overflows, so it never grows into a second row and steals height from the score. −/+ stay 48dp.
 */
@Composable
private fun PlayerFooter(
    player: PanelPlayerUiState,
    playerColor: PlayerColor,
    pillColor: Color,
    hapticsEnabled: Boolean,
    onCounterChange: (PlayerId, CounterId, delta: Int) -> Unit,
    onCounterRemove: (PlayerId, CounterId) -> Unit,
    onAddCounterClick: (PlayerId) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val controls: @Composable () -> Unit = {
        player.counters.forEach { counter ->
            key(player.playerId, counter.id) {
                CounterRow(
                    label = counter.label,
                    value = counter.value,
                    ownerName = player.name,
                    onValueChange = { delta ->
                        if (hapticsEnabled && (delta > 0 || counter.value > 0)) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                        onCounterChange(player.playerId, counter.id, delta)
                    },
                    onRemove = { onCounterRemove(player.playerId, counter.id) },
                    containerColor = pillColor,
                    contentColor = LocalContentColor.current,
                    dense = compact,
                )
            }
        }
        if (player.counters.size < MAX_COUNTERS_PER_PLAYER) {
            AddCounterButton(
                playerName = player.name,
                compact = compact || player.counters.isNotEmpty(),
                pillColor = pillColor,
                onClick = { onAddCounterClick(player.playerId) },
            )
        }
    }
    if (compact) {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerNameChip(
                name = player.name,
                playerColor = playerColor,
                modifier = Modifier.widthIn(max = RiftTheme.sizes.trackerCompactChipMaxWidth),
            )
            controls()
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                controls()
            }
            PlayerNameChip(name = player.name, playerColor = playerColor)
        }
    }
}

/**
 * "+ Counter" pill, or a round "+★" once the player has a counter (saves room on small panels; the
 * star tells it apart from the counter's own "+").
 * Announced as "Add counter for Ana" (the visible text alone is ambiguous with 2+ players).
 */
@Composable
private fun AddCounterButton(
    playerName: String,
    compact: Boolean,
    pillColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.feature_tracker_add_counter_for, playerName)
    Surface(
        onClick = onClick,
        shape = RiftTheme.shapes.full,
        color = pillColor,
        contentColor = LocalContentColor.current,
        modifier = modifier
            .height(RiftTheme.sizes.minTouchTarget)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (compact) {
                Modifier.size(RiftTheme.sizes.minTouchTarget)
            } else {
                Modifier.padding(horizontal = RiftTheme.spacing.lg)
            },
        ) {
            if (compact) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                    )
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(RiftTheme.sizes.iconSmall),
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.feature_tracker_add_counter),
                    style = RiftTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun PanelPlayerUiState.playerColor(): PlayerColor = PlayerColors.fromId(colorId) ?: PlayerColors.Grey

private const val GLYPH_ALPHA = 0.4f
private const val SCRIM_MIDDLE_STOP = 0.55f
private const val PULSE_MIN_ALPHA = 0.35f
private const val TARGET_GLOW_FACTOR = 1.5f

internal val previewPanels = persistentListOf(
    SidePanelUiState(
        sideId = SideId("s1"),
        points = 6,
        isAtTarget = false,
        players = persistentListOf(
            PanelPlayerUiState(
                playerId = PlayerId("p1"),
                name = "Ana",
                colorId = "orange",
                legendName = "Jinx",
                legendImageUri = null,
                counters = persistentListOf(CounterUiState(CounterId("xp"), "XP", 3)),
            ),
        ),
    ),
    SidePanelUiState(
        sideId = SideId("s2"),
        points = 8,
        isAtTarget = true,
        players = persistentListOf(PanelPlayerUiState(PlayerId("p2"), "Ben", "sky", null, null)),
    ),
)

@PreviewRiftThemes
@Composable
private fun SidePanelPreview() {
    RiftTheme {
        Column(verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.sm)) {
            previewPanels.forEach { panel ->
                SidePanel(
                    panel = panel,
                    hapticsEnabled = false,
                    onPointsChange = {},
                    onCounterChange = { _, _, _ -> },
                    onCounterRemove = { _, _ -> },
                    onAddCounterClick = {},
                    modifier = Modifier.size(RiftTheme.sizes.avatarLarge * 2),
                )
            }
        }
    }
}
