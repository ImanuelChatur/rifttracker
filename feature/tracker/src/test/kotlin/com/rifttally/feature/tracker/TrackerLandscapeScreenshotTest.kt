package com.rifttally.feature.tracker

import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** Landscape phone (compact height, DESIGN.md §8): scores must not clip. Records `tracker_*_land_*.png`. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w891dp-h411dp-land-xhdpi")
class TrackerLandscapeScreenshotTest(case: Case) : TrackerScreenScreenshotTest(case) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun landscapeCases(): List<Case> = listOf(
            Case(Format.TwoPlayers, withArt = true, darkTheme = true, landscape = true),
            Case(Format.ThreePlayers, withArt = false, darkTheme = false, landscape = true),
            Case(Format.FourPlayers, withArt = true, darkTheme = true, landscape = true),
        )
    }
}
