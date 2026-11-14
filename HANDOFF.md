# Handoff: Riftbound Points Tracker (Android)

> Working name: **Rift Tally** (rename freely). Package: `com.rifttally.app`.
> Read this whole file, then read `CLAUDE.md` for conventions. Build **one phase at a time**. Stop at the end of each phase, run all checks, and summarize what was built before starting the next.

---

## 1. What we're building

A fast, offline, table-side **points tracker** for **Riftbound**, Riot Games' League of Legends trading card game. It's the "life counter" you leave on the table during a game. Each player's score is shown on their **Legend's portrait**.

v1 is **only the tracker**, and it should stay simple. Later the app may grow into a full Riftbound companion (card database, deck builder, collection, stats). The architecture must make those easy to add as new feature modules without touching the tracker.

### Product principles
1. **Simple.** This is a counter app, not a rules engine. Don't model game rules beyond what's listed below.
2. **Speed at the table.** Changing a score is one tap, with large hit targets and no confirmation dialogs (undo instead).
3. **Never lose a game.** State survives rotation, process death and app restarts. Reopening the app resumes the active game.
4. **Offline only.** No network permission in v1.

---

## 2. Game rules (keep it minimal)

- **Player counts:** 2, 3 or 4 players. Plus a **2v2 team** option, where teammates share one point total.
- **Points:** start at 0, tap to go up or down, never below 0.
- **Target score:** defaults to **8** (**11** for 2v2), editable in setup and from the in-game menu.
- **Reaching the target:** when someone's points hit the target score, highlight their panel and show a small, dismissible "Winner?" banner with "End game" / "Keep playing". Never auto-end, and never block changes.
- **No** best-of series, turn tracking or rules enforcement.

### Extra counters (XP and anything else)
- Each player can have **optional extra counters** shown **under their points**, e.g. **XP**.
- Counters are generic: `label` + `value` (min 0) + `−`/`+` buttons. XP is just a counter with the label "XP".
- Add a counter from a player's panel (small "+ counter" button, with "XP" as a one-tap preset, or a custom label). Remove by long-pressing → "Remove".
- Setup has a toggle "Add XP counter to all players" (remember the last choice).
- No special XP logic (no levels, thresholds or spending rules).

---

## 3. Legends (the player's points icon)

Each player picks a **Legend** in setup. The Legend's portrait is the **icon for that player's points**: the score number sits on/over the circular Legend portrait in the panel.

**The images aren't available yet.** The developer will add them later, so build it so adding them is just dropping in files:

- **Catalog file:** `core/data/src/main/assets/legends/legends.json`
  ```json
  [
    { "id": "jinx", "name": "Jinx" },
    { "id": "viktor", "name": "Viktor" }
  ]
  ```
  Seed it with a handful of example entries (ids lowercase, snake_case).
- **Images:** `core/data/src/main/assets/legends/<id>.webp` (also accept `.png`). **Ship none.** Code must work without them.
- **Loading:** `LegendRepository` (in `:core:data`) parses the JSON and exposes `Flow<List<Legend>>`. `Legend(id, name, imageUri: String?)`, where `imageUri` is the asset URI if the file exists, else `null`. Load images with **Coil** (Compose).
- **Fallback when there's no image or no Legend picked:** a circle filled with the player's color, plus the Legend's (or player's) initial. Must look intentional, not broken.
- **Readability:** the score over a portrait needs a dark scrim/gradient (or an outlined/shadowed number) so it's readable on any image.
- **Setup picker:** searchable grid of Legends (portrait or fallback + name), plus "None".
- **Future-proof:** `Player.legendId: String?` is the only link. A future card-data module can replace the JSON source behind `LegendRepository` without UI changes.

**Adding a Legend later = add one line to `legends.json` + drop `<id>.webp` into the folder.** Document this in the README.

---

## 4. Architecture

### 4.1 Stack (use latest stable versions at build time; verify, don't guess)
- Kotlin, **Jetpack Compose**, Material 3, edge-to-edge
- Gradle Kotlin DSL, **version catalog** (`gradle/libs.versions.toml`), **convention plugins** in `build-logic/`
- **Hilt** for dependency injection (KSP, not kapt)
- **Room** for game persistence, **Preferences DataStore** for settings
- **Navigation Compose** with type-safe `@Serializable` routes
- Coroutines and Flow, `kotlinx.serialization`, `kotlinx-collections-immutable`, **Coil 3** for images
- Testing: JUnit, kotlinx-coroutines-test, Turbine, Compose UI tests
- Static checks: Spotless + ktlint, Android Lint
- `minSdk 26`, `compileSdk`/`targetSdk` = latest stable

### 4.2 Module layout (Now in Android style)

```
:app                    → Application, MainActivity, NavHost, top-level wiring only
build-logic/convention  → android-application, android-library, android-feature,
                          android-compose, hilt, room, jvm-library convention plugins

:core:model             → pure Kotlin models (Player, Side, Counter, Legend, GameConfig…)
:core:game              → pure Kotlin (JVM): events, reducer, target-reached check. NO Android deps.
:core:database          → Room DB, entities, DAOs, converters, migrations
:core:datastore         → user settings
:core:data              → repositories (interfaces + internal impls), Legend assets + JSON, Hilt bindings
:core:designsystem      → theme, colors, typography, spacing tokens, base components
:core:ui                → shared composables (PlayerPanel, LegendAvatar, CounterRow…)
:core:testing           → fakes, test dispatchers, test rules

:feature:home           → resume active game, new game, recent games (Phase 5)
:feature:setup          → player count / 2v2, names, colors, Legends, target score, XP toggle
:feature:tracker        → the live table screen
:feature:settings       → settings + about
```

**Dependency rules**
- `feature:*` modules depend on `core:*` only, **never on other features**. `:app` wires navigation between them.
- `core:game` and `core:model` are pure Kotlin/JVM and fully unit-testable.
- Only `core:data` touches `core:database`, `core:datastore` and assets. Features talk to repositories.

**Reserved for future (don't create yet):** `:feature:cards`, `:feature:decks`, `:feature:collection`, `:feature:stats`, `:core:network`, `:core:carddata`.

### 4.3 Game state: event log

Keep it small, but model changes as **events** reduced into state. This gives undo, a history log and crash-safe persistence for free.

```kotlin
// :core:game
sealed interface GameEvent {
    val id: String        // UUID
    val timestamp: Long   // epoch millis (inject a Clock)

    data class PointsAdjusted(val sideId: SideId, val delta: Int) : GameEvent
    data class CounterAdded(val playerId: PlayerId, val counterId: CounterId, val label: String) : GameEvent
    data class CounterAdjusted(val playerId: PlayerId, val counterId: CounterId, val delta: Int) : GameEvent
    data class CounterRemoved(val playerId: PlayerId, val counterId: CounterId) : GameEvent
    data class TargetScoreChanged(val newScore: Int) : GameEvent
    data class GameEnded(val winner: SideId?) : GameEvent
}
```

- **`SideId`** = whoever owns a point total: a player normally, a team in 2v2. This one abstraction handles shared team points.
- `fun reduce(config: GameConfig, events: List<GameEvent>): GameState` is a **pure function**. Floors at 0 are applied here.
- `fun sidesAtTarget(state: GameState): List<SideId>` drives the highlight/banner.
- **Undo** removes the last event (unlimited undo within a game).
- `GameConfig` = sides, players (name, color, `legendId`), starting target score, initial counters.

### 4.4 Persistence
- Room tables: `game`, `game_event` (payload as JSON via kotlinx.serialization, polymorphic `type` discriminator), `player_preset` (name, color, legendId).
- Write each event **immediately**.
- Active game = latest game without `GameEnded`. Home offers "Resume".
- Ended games stay stored (seed for future history/stats).
- Room `exportSchema = true` with schemas committed; every schema change has a migration and a migration test.

### 4.5 UI pattern (per screen)
- `XxxRoute` (stateful: `hiltViewModel()`, `collectAsStateWithLifecycle`) wraps `XxxScreen` (stateless: `uiState` plus lambdas).
- ViewModel exposes `StateFlow<XxxUiState>` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- User actions are named ViewModel functions (`onPointsChange(sideId, delta)`).
- `@Preview`s for every screen: light, dark, 2/3/4 players, with and without Legend images.

---

## 5. Screens (v1)

### Home
- "Resume" card for the active game (players, Legends, scores) if one exists
- "New game" → Setup
- Settings icon

### Setup
- Player count: 2 / 3 / 4, or 2v2 (assign players to Team A / Team B)
- Per player: name, color, Legend (picker), all with sensible defaults; load from saved presets
- Target score (prefilled 8, or 11 for 2v2)
- Toggle: "Add XP counter to all players"
- "Start" → Tracker

### Tracker (the core screen)
- **2 players:** vertical split; the top panel is **rotated 180°** for the opponent across the table.
- **3–4 players:** grid of panels, each rotated toward its seat (0°/90°/180°/270°), with seat order set in setup.
- **2v2:** two team panels with the shared score; each shows both teammates' Legends and names. Extra counters stay **per player** under their name.
- **Each panel:**
  - **Legend portrait** (or fallback) as the points icon, with the **score number big on top of it**
  - Large `−` / `+` hit zones (tap the lower/upper or left/right half of the panel, plus visible buttons)
  - Player name in their color
  - Extra counters (e.g. XP) in a compact row **under the points**: `XP  − 3 +`
  - Small "+ counter" button
- **Center control strip:** Undo · Menu (change target score, history log, restart, end game).
- **History log** bottom sheet: chronological events, newest first ("Jinx +1 · 6", "Viktor XP +2 · 4").
- **Target reached:** highlight that panel + dismissible "Winner?" banner with "End game" / "Keep playing".
- Keep screen on while visible; immersive mode (setting to disable). Haptic tick on each change (setting). Long-press `+`/`−` repeats.
- Score changes animate briefly (number crossfade + fading "+1" badge).

### Settings
- Theme: system / light / dark; dynamic color on/off
- Haptics, keep-screen-on, immersive mode toggles
- Default player count, default "XP counter" toggle
- Manage player presets (name, color, Legend)
- About: version, licenses, disclaimer: *"Unofficial fan app. Not affiliated with or endorsed by Riot Games. Riftbound and League of Legends are trademarks of Riot Games, Inc."*

### Design
- Material 3, dark-first look (used in dim game stores), and high-contrast score numbers.
- Player palette: 6–8 distinct, colorblind-friendly colors; text must pass WCAG AA contrast on them.
- Spacing/type tokens from `:core:designsystem` only; no magic numbers in features.

---

## 6. Build phases

Each phase ends with: `./gradlew spotlessCheck lint testDebugUnitTest assembleDebug` passing, a short summary, and a git commit.

### Phase 0: Foundation
- Scaffold, `build-logic` convention plugins, version catalog, all v1 modules (empty but compiling)
- Theme and tokens, edge-to-edge `MainActivity`, Hilt, NavHost with type-safe routes to placeholder screens
- Room (empty schema v1) and DataStore wired with Hilt; Spotless; `.gitignore`; README
- **Done when:** the app launches and navigates to each placeholder screen.

### Phase 1: Game state (`:core:game`, pure Kotlin)
- Models, events, reducer, target check, undo
- Unit tests: floors at 0, 2v2 shared points, target score changed mid-game, multiple sides at target, counters add/adjust/remove, undo of every event type
- **Done when:** fully tested with no Android dependency.

### Phase 2: 2-player tracker, end to end
- `LegendRepository` + `legends.json` seed + `LegendAvatar` with fallback (no images shipped)
- Setup (2 players: name, color, Legend), Tracker with rotated top panel, Legend-as-points-icon, undo, history sheet, target banner
- Events persisted to Room; resume from Home; survives process death (test with "Don't keep activities")
- Keep-screen-on, haptics, immersive mode
- **Done when:** a full 2-player game can be played, killed mid-game, resumed and ended.

### Phase 3: Extra counters
- Per-player counters (XP preset + custom label), add/remove, setup toggle, shown under the points

### Phase 4: 3 players, 4 players and 2v2
- Layouts, seat rotation, team assignment, shared team points

### Phase 5: Settings and polish
- Settings + DataStore, player presets, theme options, about/disclaimer
- "Recent games" list on Home (read-only, from stored games)
- Accessibility: TalkBack labels ("Jinx, 6 points, increase"), font scaling to 200%, 48dp+ touch targets
- App icon, splash screen, animations
- README section: "How to add Legend images"

---

## 7. Future roadmap (context only; do NOT build now)
Card database → deck builder → match stats → collection → events. These will be new `:feature:*` / `:core:*` modules. Don't add speculative code for them.

---

## 8. Definition of done (v1)
- All phases complete and checks passing
- `:core:game` test coverage ≥ 90%; ViewModel tests for Setup and Tracker; at least one Compose UI test per screen
- No crashes on rotation, process death, or rapid tapping
- Looks right with **no** Legend images, and with images added
- Works on a small phone (360dp wide) and a tablet laid flat on the table
