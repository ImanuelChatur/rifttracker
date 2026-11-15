package com.rifttally.feature.setup

import app.cash.turbine.test
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.data.game.GameRecord
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakePlayerPresetRepository
import com.rifttally.core.testing.repository.FakeRoundTimerRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class RecentLineupsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val settings = FakeUserSettingsRepository()
    private val presets = FakePlayerPresetRepository(
        listOf(PlayerPreset("ana", "Ana", "orange", "jinx"), PlayerPreset("ben", "Benny", "sky", null)),
    )
    private val picker = FirstPlayerPicker(settings, { 0 }, GameEventFactory(TestClock(), IdGenerator { "pick" }))
    private val viewModel by lazy {
        SetupViewModel(
            games,
            FakeLegendRepository(emptyList()),
            settings,
            presets,
            picker,
            FakeRoundTimerRepository(TestClock()),
        )
    }

    private fun duel(
        a: String,
        b: String,
        aColor: String = "orange",
        bDefault: Boolean = false,
        aPreset: String? = null,
        bPreset: String? = null,
        target: Int = 8,
    ) = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(SideId("s1"), listOf(PlayerId("p1"))), Side(SideId("s2"), listOf(PlayerId("p2")))),
        players = listOf(
            Player(PlayerId("p1"), a, aColor, legendId = "jinx", presetId = aPreset),
            Player(PlayerId("p2"), b, "sky", presetId = bPreset, hasDefaultName = bDefault),
        ),
        targetScore = target,
    )

    private val teams = GameConfig(
        format = GameFormat.TEAMS_2V2,
        // Seats 1 and 3 on team A, 2 and 4 on team B.
        sides = listOf(
            Side(SideId("team_a"), listOf(PlayerId("p1"), PlayerId("p3"))),
            Side(SideId("team_b"), listOf(PlayerId("p2"), PlayerId("p4"))),
        ),
        players = listOf("Ana", "Ben", "Cleo", "Dan").mapIndexed { i, name ->
            Player(PlayerId("p${i + 1}"), name, listOf("orange", "sky", "green", "pink")[i])
        },
    )

    private var ended = 0L

    private fun addEnded(config: GameConfig) {
        ended += 1_000
        games.addGame(
            GameRecord(
                id = GameId("g$ended"),
                config = config,
                events = listOf(GameEvent.GameEnded("end$ended", ended, winner = null)),
                createdAt = ended - 1,
                endedAt = ended,
            ),
        )
    }

    @Test
    fun noGames_noRow() = runTest {
        viewModel.uiState.test {
            assertEquals(emptyList(), expectMostRecentItem().recentLineups)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun distinctLineups_mostRecentlyPlayedFirst_atMostFive() = runTest {
        // An open game last played before everything else: it sorts by its newest event, not first.
        games.addGame(
            GameRecord(
                GameId("old-open"),
                duel("Ivy", "Jo"),
                listOf(GameEvent.PointsAdjusted("p", 500, SideId("s1"), 1)),
                createdAt = 400,
            ),
        )
        addEnded(duel("Ana", "Ben"))
        addEnded(duel("ana", "Ben")) // same person (name ignoring case): same lineup
        addEnded(duel("Ana", "Ben", aColor = "pink")) // another color: another lineup
        listOf("Cleo", "Dan", "Eve", "Finn").forEach { addEnded(duel("Ana", it)) }
        // An open game played most recently comes first.
        games.addGame(
            GameRecord(
                GameId("open"),
                duel("Gus", "Hal"),
                listOf(GameEvent.PointsAdjusted("q", 99_000, SideId("s1"), 1)),
                createdAt = 98_000,
            ),
        )

        viewModel.uiState.test {
            val lineups = expectMostRecentItem().recentLineups
            assertEquals(
                listOf(
                    listOf("Gus", "Hal"),
                    listOf("Ana", "Finn"),
                    listOf("Ana", "Eve"),
                    listOf("Ana", "Dan"),
                    listOf("Ana", "Cleo"),
                ),
                lineups.map { it.names },
            )
            assertEquals(MAX_RECENT_LINEUPS, lineups.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recentLineups_dedupe_keepsTheNewestGamesSpelling() {
        val games = listOf(duel("ANA", "Ben"), duel("Ana", "Ben")).mapIndexed { i, config ->
            GameRecord(GameId("g$i"), config, emptyList(), createdAt = 0)
        }

        assertEquals(listOf("ANA", "Ben"), recentLineups(games).single().players.map { it.name })
    }

    @Test
    fun applying_fillsTheForm_placeholderBlank_staleOrRenamedPresetUnlinked() = runTest {
        // "ana" still exists as "Ana"; "ben" was renamed to "Benny"; seat 2's name was a placeholder.
        addEnded(duel("Ana", "Player 2", bDefault = true, aPreset = "ana", target = 12))
        addEnded(duel("Ana", "Ben", aPreset = "gone", bPreset = "ben"))

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLineupApply(1)
            val state = expectMostRecentItem()
            assertEquals(GameFormat.TWO_PLAYERS, state.format)
            assertEquals(listOf("Ana", ""), state.players.map { it.name })
            assertEquals(12, state.targetScore)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onStartGame(listOf("Player 1", "Player 2"), "XP")
        val created = games.allGames.last().config
        assertEquals(listOf("ana", null), created.players.map { it.presetId })
        assertEquals(listOf(false, true), created.players.map { it.hasDefaultName })
    }

    @Test
    fun applying_usesTheTargetTheGameEndedOn() = runTest {
        games.addGame(
            GameRecord(
                id = GameId("g"),
                config = duel("Ana", "Ben", target = 8),
                events = listOf(GameEvent.TargetScoreChanged("t", 1, 10), GameEvent.GameEnded("end", 2, winner = null)),
                createdAt = 0,
                endedAt = 2,
            ),
        )

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLineupApply(0)
            assertEquals(10, expectMostRecentItem().targetScore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun applying_stalePresets_areTypedPlayers() = runTest {
        addEnded(duel("Ana", "Ben", aPreset = "gone", bPreset = "ben"))

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLineupApply(0)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onStartGame(listOf("Player 1", "Player 2"), "XP")

        // "gone" no longer exists; "ben" is now called "Benny": both seats are typed players.
        assertEquals(listOf(null, null), games.allGames.last().config.players.map { it.presetId })
    }

    @Test
    fun applying_aTeamsLineup_keepsSeatsAndTeams() = runTest {
        addEnded(teams)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLineupApply(0)
            val state = expectMostRecentItem()
            assertEquals(GameFormat.TEAMS_2V2, state.format)
            assertEquals(listOf("Ana", "Ben", "Cleo", "Dan"), state.players.map { it.name })
            assertEquals(listOf(Team.A, Team.B, Team.A, Team.B), state.players.map { it.team })
            assertEquals(11, state.targetScore)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onStartGame(listOf("P1", "P2", "P3", "P4"), "XP")
        assertEquals(teams.sides, games.allGames.last().config.sides)
    }
}
