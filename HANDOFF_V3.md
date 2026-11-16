# Handoff v3: fixes, first player, rematch, round timer, stats upgrades

> v1 (`HANDOFF.md`) and v2 (`HANDOFF_V2.md`) are complete. This round adds:
> 1. **Fixes** for four bugs found in review (§1).
> 2. **First-player picker** (§2).
> 3. **Rematch and recent lineups** (§3).
> 4. **Round timer** (§4).
> 5. **Stats upgrades**: Legend matchups, date filter, shareable result card, first-player win rate (§5).
>
> Read `CLAUDE.md`, `DESIGN.md` and `AGENT_WORKFLOW.md` first; all their rules still apply. Follow the run plan in §7: one phase at a time, gate → review → commit → stop and report.

---

## 0. Decisions already made (don't re-ask)

- A game ended with no side at the target (a concession) is still saved **without a winner**. That is intended; don't add a winner picker.
- No backup/restore, no deck names, no gesture lock, no "reason for a point" this round.
- Stats performance (re-reading every ended game's events) is fine for now; don't optimize it.
- `CLAUDE.md` says "no turn tracking". The first-player pick and the round timer are **not** turn tracking and are allowed. Update that line in `CLAUDE.md` to say so ("…no turn tracking or rules enforcement. A first-player pick and an optional round timer are fine.").

---

## 1. Fixes

### F1. Saving a player in Setup splits their stats

**Bug:** `SetupViewModel.onPlayerSave` creates a preset but doesn't link the seat to it. That game is counted under the name key (`name:ana`), later games loaded from the preset under `preset:<id>`, so the same person appears twice in Stats. Also, `OfflinePlayerPresetRepository.save` only dedupes an exact name + color + Legend match, so saving "Ana" again with another Legend creates a second "Ana".

**Fix:**
- `PlayerPresetRepository.save(name, colorId, legendId)` returns `String?`: the preset id, or `null` for a blank name. If a preset with the same trimmed name (case-insensitive) exists, **update it** (new color/Legend, same id) instead of adding one. Skip the write if nothing changed. Update `FakePlayerPresetRepository` the same way.
- `onPlayerSave`: after `save` returns an id, set that seat's `presetId`.
- Linking: replace `presetNameMatches` (which looks the name up in `latestPresets`, so a just-saved preset may not be there yet at Start) with a `presetName` field on `PlayerForm`, set whenever `presetId` is set (by `onPresetApply` or `onPlayerSave`). In `toConfig`, keep `presetId` only while `presetName.equals(typedName.trim(), ignoreCase = true)`.
- Settings → edit saved player: renaming a preset to a name another preset already has must be rejected with an inline error ("You already have a saved player called %1$s"), since names are now unique.

**Tests:** repository: same name again returns the same id and updates color/Legend; blank returns null. Setup: saving seat 0 then starting links it (`presetId` = the saved id); saving then renaming the seat unlinks it; the existing `loadedPreset_isLinkedInConfig_unlessTheNameChanges` still passes. Settings: rename collision shows the error and doesn't save.

### F2. Placeholder names are counted as people in Stats

**Bug:** blank names become "Player 1", "Player 2", and Stats groups by name, so unrelated people merge into one "Player 1".

**Fix:**
- `Player` (`:core:model`) gets `val hasDefaultName: Boolean = false` (stored in `config_json`; old games decode as `false`, no migration).
- `SetupViewModel.toConfig` sets it when the trimmed name is empty (then fills the placeholder as today). Rematch (§3) copies it.
- `:core:stats` `appearances()` skips players with `hasDefaultName`. That removes them from player stats, top players and head-to-head. **Legend stats still count their picks** (the Legend really played).
- Old games already stored with "Player 1" are left as they are.

**Tests (`:core:stats`):** placeholder seats are missing from `playerStats`, `headToHeads`, `topPlayersByWinRate`; their Legend still counts in `legendStats`. Add `default: Boolean` to the test `Seat` fixture. Setup test: blank name → `hasDefaultName = true` with the localized placeholder name.

### F3. Fast tapping can store a no-op "−"

**Bug:** `TrackerViewModel` keeps an optimistic `latest` snapshot, but every Room emission overwrites it, including **stale** emissions that don't contain events still in flight. Example at 0 points: tap `+` (A), `−` (B), `−` (C). C should be blocked (points are 0 after B). If Room delivers the emission containing only A before C is handled, `latest` goes back to 1 point, C is allowed, and a `−1` that changes nothing is stored. The next Undo then appears to do nothing.

**Fix:** treat the ViewModel's optimistic log as the truth while writes are pending.
- Add `awaitingSync: Boolean`, set to `true` by every `append` and `onUndo`.
- In the `combine` block: if the emitted record's event **ids** equal `latest`'s event ids, set `awaitingSync = false`. While `awaitingSync` is true (and `latest` isn't null), build state and UI from `latest.record` (with the emitted record's config/timestamps) and **don't** overwrite `latest`. Otherwise use the emitted record as now.
- This is safe because the repository serializes writes in order (`writeMutex`, FIFO), so the database converges to exactly the optimistic log. Writes are the only way the log changes while the tracker is open.

**Tests:** extend `FakeGameRepository` with an opt-in way to emit an older snapshot of a game (e.g. `emitStale(gameId, eventCount)`) or to hold emissions until `flush()`. Test the A/B/C scenario above: no third event is stored and points stay 0. Test that the UI keeps showing the optimistic score during a stale emission, and that it follows the database again once ids match.

### F4. Restart can leave a game open forever

**Bug:** `TrackerViewModel.onRestart` calls `createGame` and *then* appends `GameEnded` to the old game. If the process dies in between there are two active games; Home resumes the new one and the old one never ends (never shows in History).

**Fix:** do both in one transaction.
- `GameRepository.restartGame(oldGameId: GameId, endEvent: GameEvent.GameEnded, newConfig: GameConfig): GameId`: in one Room `@Transaction`, append the end event to the old game (unless it already ended) and insert the new game. A DAO can touch both tables (e.g. add an `@Insert` for `GameEntity` to `GameEventDao` and a `@Transaction` method that does both). Update `FakeGameRepository`.
- `onRestart` calls it, then navigates to the new game.

**Tests:** repository: after `restartGame` the old game is ended and the new one is the active game; restarting an already-ended game doesn't add a second `GameEnded`. DAO instrumented test for the transaction. Tracker VM test still passes (double restart → one game).

---

## 2. First-player picker

**Goal:** at the start of a game the app randomly picks who goes first, with a short reveal on the table.

### Randomness
- Use **`java.security.SecureRandom`**. The app has no network permission, so an online "true random" service is out. `SecureRandom` on Android draws from the kernel's cryptographic RNG, which is seeded from hardware entropy: the closest to true random available offline, and plenty fast.
- Add `fun interface RandomSource { fun nextInt(until: Int): Int }` in `:core:game` (pure Kotlin). Provide the `SecureRandom` implementation via Hilt in `:core:common` (or `:core:data`). Tests inject a deterministic one.

### Model
- New event `GameEvent.FirstPlayerChosen(id, timestamp, playerId: PlayerId)`, `@SerialName("first_player_chosen")`. It picks a **player** (in 2v2 that's the player who starts; their team is their side).
- Reducer: `GameState.firstPlayer: PlayerId?`. The latest valid pick wins; unknown players are ignored; ignored after the game ended like every event.
- `gameLog`: `GameLogLine.FirstPlayer(id, name)` → "Ana goes first".
- `GameEventFactory.firstPlayerChosen(playerId)`.

### Behaviour
- **Setting** "Pick first player at game start", default **on** (DataStore + Settings toggle).
- When on, **game creation** (Setup start, Rematch, Restart, Play again) appends a `FirstPlayerChosen` event right after creating the game, picked with `RandomSource` over all players. Doing it at creation (not when the tracker opens) means undoing the pick doesn't make it re-pick in a loop.
- **Reveal:** when the tracker opens a game whose log contains a pick that hasn't been revealed yet (remember "revealed" in `SavedStateHandle` so rotation or process death doesn't replay it), play a short highlight that cycles across the panels and settles on the chosen player's panel, then shows "<Name> goes first" **facing that seat**. Tap anywhere to dismiss; it also dismisses itself after a few seconds. The animation is cosmetic: the result is decided before it starts. Respect reduce motion (no cycling, show the result directly). Use `RiftTheme.motion` tokens only (DESIGN.md §7).
- **Menu item** "Pick first player" re-rolls at any time (appends a new pick and plays the reveal).
- The picked player's panel shows a small "1st" marker until the first points change, so a late arrival can see who started.
- Undo removes a pick like any other event.

**Tests:** reducer (latest pick wins, unknown player ignored, after end ignored), `gameLog` line, serialization round-trip, VM: creation adds a pick when the setting is on and none when off, menu re-roll, reveal shown once.

---

## 3. Rematch and recent lineups

### Rematch
- **Ending no longer leaves the tracker.** After `GameEnded` (from the banner or the menu) the tracker stays open and shows a **Game over** overlay: winner (or "No winner"), final scores, and buttons **Rematch**, **Home**, and **Undo** (removes the `GameEnded` event, so an accidental end is fixable; the repository already reopens the game on that undo). Points and counters are disabled while ended (the VM already ignores them).
- **Rematch** creates a new game with the same `GameConfig`: same players (names, colors, Legends, `presetId`, `hasDefaultName`), same seats and teams, the **final** target score, and the config's original initial counters (not counters added mid-game). Then it picks a first player (§2) and opens the new game, replacing the old tracker on the back stack (`popUpTo<TrackerRoute> { inclusive = true }`). Guard against double taps (one new game).
- **Back** from the Game over overlay goes Home.
- **Game detail** (History) gets a **Play again** button that does the same from a past game. `:feature:history` can't depend on the tracker: `GameDetailViewModel` creates the game and emits a navigation event with the new id; `:app` wires it to `navigateToTracker`.
- Keep **Restart** in the menu as it is (ends without a winner, starts the same lineup).

### Recent lineups (Setup)
- A "Recent lineups" row at the top of Setup: up to 5 distinct lineups from the most recent games (ended or active), newest first. Label: player names joined with " · " plus the format ("Ana · Ben · Cleo, 3 players").
- A lineup is: format, seat order, and per seat name, color, Legend, team, `presetId`, `hasDefaultName`. Two games have the same lineup when format and the per-seat (`PlayerKey`, Legend, color, team) are equal.
- Tapping one fills the whole form: format, seats, teams, target score of that game. A seat with `hasDefaultName` is filled with a **blank** name. A seat's `presetId` link is kept only if that preset still exists and still has that name; otherwise it's a typed player.
- Setup reads games through `GameRepository` only (add `recentGames` to `SetupViewModel`'s inputs). Hide the row when there are no games.

**Tests:** VM tests for game over → rematch (config copied, new game opened, one game on double tap), undo of end, Play again, and recent lineups (dedupe, limit 5, placeholder → blank, stale preset unlinked). Roborazzi screenshots: Game over overlay (2p, 4p, 2v2; light/dark; with and without a winner), Setup with recent lineups.

---

## 4. Round timer

**Goal:** an optional countdown for timed store rounds. It never ends the game or blocks anything.

- **Setup:** a "Round timer" row: Off, or a length in minutes (stepper, 5–120, default **50**). Remember the last choice in DataStore. Default **Off**.
- **Tracker:** when the game has a timer, the control strip shows the remaining time (mm:ss, tabular digits) between Undo and Menu. Tap to **start/pause**. The menu gets **Reset timer**. The timer starts **paused**, so the table starts it when the round begins.
- **5 minutes left:** the time switches to a warning color. **At 0:** one strong haptic (if haptics are on), then it counts **up** as overtime ("+02:13") in the error color. No sound.
- **Rematch** carries the timer over unchanged (a round can hold several games): same length, same elapsed time, still running if it was. New games from Setup start fresh. Restart also carries it over.
- **Persistence:** the timer must survive rotation, process death and restarts (`HANDOFF.md` principle 3), but it is **not** an event (Undo must never touch it). Add a `game_timer` table (`game_id` PK/FK with cascade delete, `duration_ms`, `elapsed_before_ms`, `running_since` nullable). Remaining time = `duration − elapsed_before − (now − running_since if running)`, computed with the injected `Clock`. This is a schema change: follow the rule in `RiftDatabase` (bump the version, add a migration and a migration test, commit the new schema JSON), unless the app hasn't had a public release yet, in which case the in-place exception applies. Ask the developer which, if unsure.
- The UI ticks about once per second only while the tracker is visible; nothing runs in the background.
- Accessibility: the time has a content description ("12 minutes 30 seconds left in round"), announced politely at the 5-minute mark and at 0, not every second.

**Tests:** remaining-time math (paused, running, overtime) with `TestClock`; start/pause/reset; carried over by rematch; survives VM recreation; DAO + migration tests. Screenshots: strip with timer (normal, warning, overtime), Setup timer row.

---

## 5. Stats upgrades

### 5.1 Date filter
- Filter chips at the top of Stats: **All time** (default), **Last 30 days**, **This year** (local time zone, injected `Clock`). The filter applies to the overview, the players list, the Legend tab, Legend detail (§5.2) and Player detail. Pass it to the detail routes as a route argument so the detail screens show the same range.
- Filtering happens before the pure `:core:stats` functions run (filter `StatsGame`s by `endedAt`). Empty range → the existing empty state, with "No games in this period".

### 5.2 Legend matchups
- Tapping a Legend on the Legend tab opens **Legend detail**: its games and win rate, then a list of opposing Legends with games, wins, losses and win rate, most games first.
- New pure function in `:core:stats`: `legendMatchups(games, legendId): List<LegendMatchup>`. Count each opposing Legend **once per game** when a pick of it and a pick of this Legend sat **on different sides** (a "facing pair"; 2v2 teammates aren't opponents). The game is a win if this Legend's side of a facing pair won, a loss if the other's side of one won, neither otherwise (both at once, e.g. a full 2v2 mirror; 3–4 player games won by someone else; no winner). *(Changed after v3 from one count per pair, which counted a game twice when a Legend was picked twice.)* Mirror matches (same Legend on both sides) are excluded. Fully unit tested, Kover ≥ 95% as for the rest of `:core:stats`.
- Placeholder-named players' Legends still count here (same rule as Legend stats).

### 5.3 First-player win rate
- Overview: "Going first wins X% (N games)", over games that have a first-player pick and a winner.
- Player detail: their win rate when going first vs not.
- Pure functions in `:core:stats`, with tests. `StatsGame` needs the first player (map it from the event log in `toStatsGame()`).

### 5.4 Shareable result card
- A **Share** button on Game detail and on the tracker's Game over overlay.
- It renders a `ResultCard` composable (Legend portraits or fallbacks, names in player colors, final scores, winner highlighted, format, date, and a small footer "Rift Tally · unofficial fan app") to an image off-screen, writes a PNG to the cache dir, and opens the Android share sheet through a `FileProvider` (declared in `:app`). **No network permission** is needed or allowed.
- `ResultCard` and the share helper live in `:core:ui` (used by tracker and history). Fixed size so it looks the same on every phone; dark theme.
- Screenshot tests for the card (2p, 4p, 2v2, with/without art, with/without winner), using the test placeholder images, never real Riot art.

---

## 6. Docs to update

- `CLAUDE.md`: the "turn tracking" line (§0).
- `README.md`: the module list (add the v2 modules `:core:stats`, `:feature:history`, `:feature:stats` if missing) and a short feature list.
- `HANDOFF_V3.md` stays as the spec for this round.

---

## 7. Run plan

Single agent for Phases 1–3 (they cut across Setup, Tracker and the data layer). After **every** phase: `./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug`, record and look at Roborazzi screenshots for UI changes (`DESIGN.md` §10 checklist), run the **review subagent** from `AGENT_WORKFLOW.md` scoped to the phase's diff, fix Blocking items, re-run the gate, commit, **stop and report**.

| Phase | Agents | Scope | Commit |
|---|---|---|---|
| 1 Fixes | 1 | F1–F4 | `fix(v3): stats identity, placeholder names, tracker sync, atomic restart` |
| 2 First player | 1 | §2 | `feat(v3): first-player picker` |
| 3 Rematch | 1 | §3 | `feat(v3): game over, rematch, recent lineups` |
| 4 + 5 | 2 parallel (worktrees) + merge | Timer (§4) and Stats upgrades (§5) | `feat(v3): round timer, stats upgrades` |

### Phase 4 + 5 ownership (parallel, each in its own git worktree)
- **Subagent T (timer):** owns `core/database/**` (timer table, DAO, migration), `core/data/**/timer/**`, `core/datastore/**` (timer default), `feature/setup/**` (timer row), `feature/tracker/**`. Must not touch `core/stats/**`, `feature/stats/**`, `feature/history/**`, `core/ui/**`.
- **Subagent S (stats):** owns `core/stats/**`, `feature/stats/**`, `feature/history/**`, `core/ui/**/ResultCard*` and the share helper. Must not touch anything Subagent T owns. The Share button on the tracker's Game over overlay is **not** S's: S reports the `ResultCard` API, and the main agent adds that button during the merge.
- Both: `libs.versions.toml`, `settings.gradle.kts`, `:app` (FileProvider, navigation for Legend detail) are main-agent only. List needed changes in the report.
- **Merge:** merge T, then S; apply requested out-of-scope changes; wire navigation and the tracker Share button; gate → review → commit → stop and report.

### Final report
What shipped per phase, decisions taken on anything ambiguous, screenshot paths, test results, and anything skipped.
