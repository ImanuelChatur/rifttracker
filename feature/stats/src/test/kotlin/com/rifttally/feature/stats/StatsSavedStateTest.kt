package com.rifttally.feature.stats

import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.model.Legend
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tab, sort and period survive process death: the saved state goes through a real Bundle, parceled and
 * read back, into a new [SavedStateHandle], as Android does. (Robolectric for Bundle and Parcel.)
 */
@RunWith(RobolectricTestRunner::class)
class StatsSavedStateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null), Legend("viktor", "Viktor", null)))

    private fun newViewModel(savedState: SavedStateHandle) =
        StatsViewModel(savedState, games, legends, testGamesFilter(NOW), mainDispatcherRule.testDispatcher)

    @Test
    fun periodTabAndSort_surviveProcessDeath() = runTest {
        games.addGame(duel("recent", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = NOW - DAY))
        val before = SavedStateHandle()
        newViewModel(before).run {
            onPeriodSelect(StatsPeriod.THIS_YEAR)
            onTabSelect(StatsTab.LEGENDS)
            onLegendSortSelect(LegendSort.NAME)
        }

        newViewModel(before.afterProcessDeath()).uiState.test {
            val state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(StatsPeriod.THIS_YEAR, state.period)
            assertEquals(StatsTab.LEGENDS, state.selectedTab)
            assertEquals(LegendSort.NAME, state.legendSort)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Saves this handle into a Bundle, parcels it, reads it back and restores a new handle from it. */
    @Suppress("RestrictedApi")
    private fun SavedStateHandle.afterProcessDeath(): SavedStateHandle {
        val saved = savedStateProvider().saveState()
        val parcel = Parcel.obtain()
        val restored = try {
            parcel.writeBundle(saved)
            parcel.setDataPosition(0)
            requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
        return SavedStateHandle.createHandle(restored, Bundle.EMPTY)
    }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L

        /** 2026-11-15 12:00 in Berlin. */
        val NOW = java.time.ZonedDateTime.of(2026, 11, 15, 12, 0, 0, 0, TEST_ZONE).toInstant().toEpochMilli()
    }
}
