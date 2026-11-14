# Handoff v2: Legend art, design pass, match history & stats

> v1 (Phases 0–5 in `HANDOFF.md`) is complete. This round adds three things:
> 1. **Legend art**: download official Legend art automatically.
> 2. **Design pass**: make the tracker look like a polished Riftbound table app.
> 3. **Match history & stats**: new features built on the games the app already stores.
>
> Read `CLAUDE.md` first; all its rules still apply. Follow the run plan in §5 exactly: one prep step, then **two parallel subagents in separate git worktrees**, then merge and review.

---

## 1. Legend art (main agent, prep step)

Fetch the art automatically from **Riot's Data Dragon**, the official League of Legends asset CDN. Riot's developer policies list Data Dragon assets as usable in fan products, and every Riftbound Legend is a League champion, so every Legend has art there. Use Data Dragon instead of scraping fan wikis: it's official, has stable URLs, and needs no API key.

### 1.1 Update the Legend list first
- `legends.json` currently has 15 seed entries. Check them against a current Riftbound Legend list (search the web, e.g. riftbound.gg's Legends page or Riot's set announcements). Add every Legend released through the latest set (Origins, Spiritforged, Unleashed, Vendetta, Radiance) and remove any that aren't real Riftbound Legends.
- Keep `id` = lowercase snake_case of the champion name (`kai_sa`, `lee_sin`, `master_yi`), sorted by name.

### 1.2 `scripts/fetch_legend_art.py` (Python 3 + Pillow + requests)
1. Get the latest version: `https://ddragon.leagueoflegends.com/api/versions.json` → first entry.
2. Get the champion list: `https://ddragon.leagueoflegends.com/cdn/{version}/data/en_US/champion.json`. Map each Legend to its Data Dragon key by matching the champion `name` field (e.g. Kai'Sa → `KaiSa`, Lee Sin → `LeeSin`, Wukong → `MonkeyKing`). Warn on any Legend with no match.
3. For each Legend, download:
   - **Portrait:** `https://ddragon.leagueoflegends.com/cdn/img/champion/loading/{Key}_0.jpg` (tall loading-screen art). Crop a square from the top (where the face is), resize to **512×512**, save as `core/data/src/main/assets/legends/<id>.webp` (WebP quality 82).
   - **Wide:** `https://ddragon.leagueoflegends.com/cdn/img/champion/centered/{Key}_0.jpg` (centered splash). Resize to **960 px wide**, save as `<id>_wide.webp` (quality 80). Used as the tracker panel background.
4. **Manual override:** if `legend-art-inbox/<id>.(png|jpg|webp)` exists (e.g. the developer prefers the actual Riftbound card art for a Legend), process that file as the portrait instead of downloading. Add `legend-art-inbox/` to `.gitignore`.
5. Skip Legends whose files already exist unless `--force` is passed. Retry failed downloads up to 3 times. Sleep ~200 ms between requests.
6. Write `core/data/src/main/assets/legends/ATTRIBUTION.md` listing each Legend, its source URL and the Data Dragon version.
7. Print a summary table (Legend → key → portrait/wide → downloaded/override/skipped/failed).

Run it, then commit the generated `.webp` files and `ATTRIBUTION.md`. **The app itself stays offline:** art is bundled at build time, never fetched at runtime.

### 1.3 App changes for wide art
- `Legend` gets `wideImageUri: String? = null`. `LegendCatalogParser` resolves `<id>_wide.webp` the same way it resolves `<id>.webp`. Update its tests.
- Track A uses `wideImageUri` for panel backgrounds, falling back to `imageUri` (cropped), then to the color fallback.

### 1.4 Legal notice
Riot's "Legal Jibber Jabber" fan policy allows free, non-commercial fan projects that show its notice. Replace the About screen disclaimer, and add the same text to the README:

> *Rift Tally was created under Riot Games' "Legal Jibber Jabber" policy using assets owned by Riot Games. Riot Games does not endorse or sponsor this project.*

Keep the app **free**: no ads, paywalls or donations.

### 1.5 README
Add an "Adding Legend art" section: add the Legend to `legends.json`, run `python scripts/fetch_legend_art.py`, and optionally drop an override image in `legend-art-inbox/`.

---

## 2. Track A: Design pass (subagent A)

> **Superseded in part by [`DESIGN.md`](DESIGN.md):** for theme, motion, shape, elevation and components, `DESIGN.md` wins. Durations below (≤150ms, ≤300ms) become caps on the spring specs it defines.

**Goal:** the table screen should feel like a premium game accessory, not a generic Material app, while staying fast and readable in a dim store.

### Tracker panels (`SidePanel`, `LegendAvatar`, `TrackerTableLayout`)
- **Legend art as the panel's background:** full-bleed, `ContentScale.Crop`, with a bottom-to-top dark gradient scrim so the score stays readable on any art. Without art, use a subtle gradient in the player's color plus a large faint initial.
- **The score is the hero:** largest possible number that fits the panel (scale with panel size, not a fixed sp), tabular/monospaced digits so numbers don't jump, soft shadow.
- **Player color is an accent:** a 3–4dp edge glow/border and the name chip. Don't flood-fill the panel.
- **Target reached:** animated gold edge pulse (respect "reduce motion" / `animatorDurationScale == 0`) in place of a static highlight.
- **Counters (XP)** become compact pill rows under the score with icon + value; `−`/`+` stay ≥ 48dp touch targets.
- **Visible feedback for the tap zones:** a faint `−` / `+` glyph in each half and a ripple confined to that half.
- **Control strip:** a floating, rounded center bar (Undo · Menu) with a translucent surface, so it reads as "between" the players.

### Other screens
- **Home:** a hero "Resume" card showing both Legends' art side by side, then a "New game" primary button. Recent games become a compact list with tiny Legend avatars and the winner highlighted.
- **Setup / Legend picker:** a 3-column grid of portrait cards (art, or the fallback), name underneath, selected state with a color ring, search at the top.
- **Typography:** pick one display font for scores and headings (bundle an OFL-licensed font, e.g. via `res/font`, and record its license in About). Keep the body text on the default.
- **Motion:** score change = quick vertical roll (≤150ms). Panels enter with a short staggered fade. Nothing longer than 300ms.

### Verification (required, since you can't eyeball a device)
- Add **Roborazzi** screenshot tests (JVM, Robolectric) for: tracker 2p / 3p / 4p / 2v2, with art and without art, light and dark, and target-reached state; Home with/without an active game; Setup with the picker open. Use a bundled **test-only** placeholder image (generated gradient PNG in `src/test/resources`), never real Riot art.
- Record the baselines, then **look at the PNGs yourself** and iterate until the design matches this section. Report the screenshot paths.

### Ownership
- **Owns:** `core/designsystem/**`, `core/ui/**`, `feature/tracker/**`, `feature/home/src/main/kotlin/**/HomeScreen.kt` + its previews and strings, `feature/setup/**/LegendPickerSheet.kt`, `feature/setup/**/SetupScreen.kt`, `app/src/main/res/font/**`, screenshot test files.
- **Must not touch:** ViewModels' public behavior, `core/model`, `core/game`, `core/data`, `core/database`, navigation, `settings.gradle.kts`, `libs.versions.toml`. Ask for anything else in your report.

---

## 3. Track B: Match history & stats (subagent B)

**Goal:** let players browse past games and see simple stats. Everything is derived from the stored event logs; no new tracking at the table.

### Data
- **Player identity across games:** add `presetId: String? = null` to `Player` (`core/model`). It's stored inside `config_json`, so **no Room migration** is needed, and old games decode fine because the field defaults to null. Setup sets it when a player is loaded from a preset.
- Stats group players by `presetId`, else by trimmed, case-insensitive name.
- `GameRepository`: add `endedGames(): Flow<List<GameRecord>>` (all ended games, newest first) and `deleteGame(id)`. Add a DAO query + test.

### `:core:stats` (new, pure Kotlin/JVM)
Pure functions over `List<GameRecord>`-like inputs (define a small input model in `:core:stats` so it doesn't depend on `:core:data`). Fully unit tested:
- **Per player:** games played, wins, win rate, current win streak, favorite Legend.
- **Per Legend:** games played, win rate.
- **Head-to-head:** for two players, wins each.
- **Per game:** duration (`endedAt − createdAt`), final scores, winner, points timeline (score after each event, for a small chart).
- Games ended with no winner count as played, not won.

### `:feature:history` (new)
- **History list:** ended games, newest first. Each row shows date, format, Legends + names, final score and winner. Filter chips by format. Swipe or menu to delete, with an undo snackbar.
- **Game detail:** final scoreboard, duration, a **score-over-time line chart** (draw with Compose `Canvas`, no chart library), and the full event log (reuse the history item formatting from the tracker by moving it to `:core:ui` *only if* Track A doesn't own it; otherwise copy a minimal version and note it for the merge).

### `:feature:stats` (new)
- **Overview:** total games, most-played Legend, top players by win rate (minimum 3 games).
- **Player detail:** stats from above plus a head-to-head list vs each opponent.
- **Legend tab:** a sortable list of Legends with games and win rate.
- Empty states for zero games.

### Navigation
Expose `NavGraphBuilder.historyScreen(...)`, `gameDetailScreen(...)`, `statsScreen(...)`, `playerStatsScreen(...)` plus `navigateTo…` functions. **Don't edit Home or `:app`.** List the entry points needed (e.g. "Home needs a History button and a Stats button") in your report, and the main agent wires them during the merge.

### Ownership
- **Owns:** `core/stats/**`, `feature/history/**`, `feature/stats/**`, `core/model/**/Player.kt`, `core/data/**/game/**`, `core/database/**/dao/GameDao.kt` + DAO tests, `feature/setup/**/SetupViewModel.kt` (presetId only), and tests for all of these.
- **Must not touch:** anything Track A owns, `:app`, `settings.gradle.kts`, `libs.versions.toml`.

---

## 4. Shared rules for both tracks
- Read `CLAUDE.md` and this file before starting. Follow the Route/Screen split, design tokens, `strings.xml`, and previews (light + dark).
- Don't add dependencies yourself. The prep step adds everything needed; ask in your report if something's missing.
- **Report at the end:** what you built, decisions, out-of-scope changes requested, test results, and screenshot paths (Track A).

---

## 5. Run plan

### Step 1: Prep (main agent, single)
1. Legend list update + art fetch + wide-art support + legal notice (§1).
2. Add to `libs.versions.toml` + convention plugins: **Roborazzi** (+ Robolectric if missing). Nothing else should be needed.
3. Create empty, compiling modules `:core:stats` (jvm-library), `:feature:history`, `:feature:stats` (android-feature), and register them in `settings.gradle.kts`.
4. Gate: `./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug` → commit `chore(v2): prep, legend art`.

### Step 2: Parallel (two subagents, **each in its own git worktree**, spawned together)
- Subagent A: §2 Design pass. Prompt it with §2 + §4 and the paths it owns.
- Subagent B: §3 Match history & stats. Prompt it with §3 + §4 and the paths it owns.
- Each runs the gate in its worktree and commits on its own branch (`v2-design`, `v2-history-stats`).

### Step 3: Merge (main agent)
1. Merge `v2-history-stats`, then `v2-design`. Resolve conflicts, keeping Track A's visuals and Track B's behavior.
2. Apply both reports' requested out-of-scope changes. Wire History and Stats into Home and `:app` navigation, styled to match Track A's Home.
3. Make the new History/Stats screens follow Track A's design tokens and components. Add Roborazzi screenshots for them.
4. Gate → **review subagent** (same prompt as in `AGENT_WORKFLOW.md`, scoped to `git diff <prep-commit>..HEAD`) → fix blocking items → gate → commit `feat(v2): design pass, history and stats`.

### Step 4: Stop and report
Summarize what shipped, link the screenshot folder, list anything skipped, and list any Legends whose art failed to download.

| Step | Agents | Output |
|---|---|---|
| 1 Prep | 1 | Legend list, art fetch script + art, deps, empty modules |
| 2 Build | 2 parallel (worktrees) | `v2-design`, `v2-history-stats` branches |
| 3 Merge | 1 + 1 reviewer | Integrated, reviewed, committed |
