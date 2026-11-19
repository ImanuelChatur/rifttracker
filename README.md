# Rift Tally

A fast, offline, table-side **points tracker** for *Riftbound*, the League of Legends trading card game.
Each player's score sits on their Legend's portrait.

## Features

- 2, 3 or 4 players, or 2v2 teams, each panel turned toward its seat; big score with tap zones (hold to
  repeat), unlimited undo, extra counters (e.g. XP), adjustable target and a "Winner?" banner.
- Legends with their art from Riot's Data Dragon (bundled at build time), saved players and recent lineups.
- Random first-player pick with a short reveal on the table, and an optional round timer.
- Game over screen with Rematch; every game is stored, so a crash or restart never loses the score.
- Match history (score chart, full log, Play again) and stats: players, head-to-head, Legends and
  matchups, first-player win rate, date filters and a shareable result card.
- Offline only: no network permission, no account, no ads.

> *Rift Tally was created under Riot Games' "Legal Jibber Jabber" policy using assets owned by Riot Games.
> Riot Games does not endorse or sponsor this project.*
>
> Rift Tally is free: no ads, paywalls or donations.

## Install

Download `RiftTally-<version>.apk` from the GitHub
[Releases](https://github.com/ImanuelChatur/rifttracker/releases) page and open it on your phone
(Android 8.0 or newer). Android asks you to allow installing apps from your browser or file manager the
first time. Later versions install over the old one and keep your games.

## Release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew :app:assembleRelease`. The APK is `app/build/outputs/apk/release/app-release.apk`
   (minified with R8). Smoke-test it on a device: start a game, finish it, open History and Stats.
3. Tag the commit (`git tag v<version>`), push it, create a GitHub release from the tag and attach the APK
   renamed to `RiftTally-<version>.apk`.

Signing reads the untracked `keystore.properties` in the project root (`storeFile`, `storePassword`,
`keyAlias`, `keyPassword`). Without it the release APK is built unsigned. **Back up the keystore and
`keystore.properties` somewhere safe:** an update signed with a different key won't install over an
existing install.

## Build & check

Requires the Android SDK (compile SDK 37, min SDK 26). The Gradle daemon runs on JDK 25
(`gradle/gradle-daemon-jvm.properties`); Android Studio's bundled JBR works, otherwise Gradle downloads one.
Modules compile to Java 17 bytecode.

```sh
./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug
```

- `spotlessApply` / `spotlessCheck`: ktlint (+ Compose rules) formatting for all `.kt`, `.kts` and `.xml` files
- `testDebugUnitTest`: JVM unit tests in every module (pure-Kotlin modules alias it to `test`). In `:core:game` and `:core:stats` it also runs Kover and fails below 95% line coverage (`./gradlew :core:game:koverHtmlReport` for the report)
- Instrumented tests (Compose UI, Room migrations): `./gradlew connectedDebugAndroidTest`

## Modules

```
:app                    Application, MainActivity, NavHost (wiring only)
build-logic/convention  Convention plugins (rifttally.android.application/library/feature/compose/room,
                        rifttally.hilt, rifttally.jvm.library)

:core:model             Pure Kotlin models
:core:game              Pure Kotlin game logic (events, reducer)
:core:common            Dispatcher/scope qualifiers and the injected Clock
:core:database          Room (schemas committed in core/database/schemas)
:core:datastore         Preferences DataStore (user settings)
:core:data              Repositories: the only module features use for data
:core:designsystem      Theme, tokens, player palette, base components
:core:ui                Shared composables, incl. the shareable ResultCard (its sharer needs :app's
                        FileProvider: authority <applicationId>.fileprovider, res/xml/file_paths.xml)
:core:stats             Pure Kotlin match history and stats, derived from stored games
:core:testing           Test rules, test clock, fakes

:feature:home | :feature:setup | :feature:tracker | :feature:settings
:feature:history | :feature:stats
```

Dependency rules: features depend on `:core:*` only, never on each other, and `:app` wires navigation.
`:core:model` and `:core:game` have no Android dependencies. Only `:core:data` touches the database,
DataStore and assets.

## How to add Legends

Legends are data files. You don't need to change any code.

1. Add a line to `core/data/src/main/assets/legends/legends.json` (keep it sorted by name):
   ```json
   { "id": "jinx", "name": "Jinx" }
   ```
   Ids are the champion name in lowercase `snake_case` (`kai_sa`, `lee_sin`, `jarvan_iv`).
2. Fetch its art (see below).

A Legend without an image shows a circle in the player's color with the Legend's initial, so a missing
file never breaks anything.

## Adding Legend art

Legend art comes from Riot's [Data Dragon](https://developer.riotgames.com/docs/lol#data-dragon) CDN and is
bundled at build time. The app itself never goes online.

```sh
pip install -r scripts/requirements.txt   # Pillow + requests, once
python scripts/fetch_legend_art.py         # add --force to re-download existing files
```

For every Legend in `legends.json` the script writes, into `core/data/src/main/assets/legends/`:

- `<id>.webp`: a 512×512 portrait cropped from the top of the champion's loading-screen art;
- `<id>_wide.webp`: the 960 px wide centered splash, used as the tracker panel background;
- `ATTRIBUTION.md`: every source URL and the Data Dragon version.

It matches Legends to champions by name, skips files that already exist, retries failed downloads and prints
a summary table. Commit the generated files.

**Using your own portrait:** drop `legend-art-inbox/<id>.png` (or `.jpg`/`.webp`), for example the Riftbound
card art, and run the script again with `--force`. The inbox folder is git-ignored; only the processed
`.webp` is committed.
