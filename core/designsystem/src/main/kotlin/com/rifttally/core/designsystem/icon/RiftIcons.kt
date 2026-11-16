package com.rifttally.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes

/**
 * Icons the app needs beyond `material-icons-core`, drawn from the Material paths so the (large)
 * extended icon artifact isn't needed, plus the `material-icons-core` ones the app uses. Feature and UI
 * code takes every icon from here, never from [Icons] directly.
 */
public object RiftIcons {
    // Material core icons, re-exported so every screen takes its icons from RiftIcons.
    public val Add: ImageVector get() = Icons.Filled.Add
    public val ArrowBack: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    public val Check: ImageVector get() = Icons.Filled.Check
    public val Close: ImageVector get() = Icons.Filled.Close
    public val DateRange: ImageVector get() = Icons.Filled.DateRange
    public val Delete: ImageVector get() = Icons.Filled.Delete
    public val KeyboardArrowRight: ImageVector get() = Icons.AutoMirrored.Filled.KeyboardArrowRight
    public val List: ImageVector get() = Icons.AutoMirrored.Filled.List
    public val MoreVert: ImageVector get() = Icons.Filled.MoreVert
    public val Person: ImageVector get() = Icons.Filled.Person
    public val PlayArrow: ImageVector get() = Icons.Filled.PlayArrow
    public val Search: ImageVector get() = Icons.Filled.Search
    public val Settings: ImageVector get() = Icons.Filled.Settings
    public val Share: ImageVector get() = Icons.Filled.Share
    public val Star: ImageVector get() = Icons.Filled.Star

    /** Minus sign (Material "Remove"). */
    public val Remove: ImageVector by lazy {
        materialIcon(name = "Rift.Remove") {
            materialPath {
                moveTo(19.0f, 13.0f)
                horizontalLineTo(5.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
    }

    /** Curved back arrow (Material "Undo"). */
    public val Undo: ImageVector by lazy {
        materialIcon(name = "Rift.Undo", autoMirror = true) {
            materialPath {
                moveTo(12.5f, 8.0f)
                curveToRelative(-2.65f, 0.0f, -5.05f, 0.99f, -6.9f, 2.6f)
                lineTo(2.0f, 7.0f)
                verticalLineToRelative(9.0f)
                horizontalLineToRelative(9.0f)
                lineToRelative(-3.62f, -3.62f)
                curveToRelative(1.39f, -1.16f, 3.16f, -1.88f, 5.12f, -1.88f)
                curveToRelative(3.54f, 0.0f, 6.55f, 2.31f, 7.6f, 5.5f)
                lineToRelative(2.37f, -0.78f)
                curveTo(21.08f, 11.03f, 17.15f, 8.0f, 12.5f, 8.0f)
                close()
            }
        }
    }

    /** Clock with a counter-clockwise arrow (Material "History"): match history. */
    public val History: ImageVector by lazy {
        materialIcon(name = "Rift.History") {
            addPath(
                pathData = addPathNodes(
                    "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 " +
                        "-3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 " +
                        "9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Three bars (Material "Bar chart"): stats. */
    public val Stats: ImageVector by lazy {
        materialIcon(name = "Rift.Stats") {
            addPath(
                pathData = addPathNodes("M5,9.2h3V19H5V9.2zM10.6,5h2.8v14h-2.8V5zM16.2,13H19v6h-2.8V13z"),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Two vertical bars (Material "Pause"): pause the round timer. */
    public val Pause: ImageVector by lazy {
        materialIcon(name = "Rift.Pause") {
            addPath(
                pathData = addPathNodes("M6,19h4L10,5L6,5v14zM14,5v14h4L18,5h-4z"),
                fill = SolidColor(Color.Black),
            )
        }
    }
}
