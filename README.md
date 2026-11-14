# Rift Tally

A fast, offline, table-side **points tracker** for *Riftbound*, the League of Legends trading card game.
Each player's score sits on their Legend's portrait. See `HANDOFF.md` for the full spec, `CLAUDE.md` for
conventions and `AGENT_WORKFLOW.md` for how the work is run.

> Unofficial fan app. Not affiliated with or endorsed by Riot Games. Riftbound and League of Legends are
> trademarks of Riot Games, Inc.

## Build & check

Requires the Android SDK (compile SDK 37, min SDK 26). The Gradle daemon runs on JDK 25
(`gradle/gradle-daemon-jvm.properties`); Android Studio's bundled JBR works, otherwise Gradle downloads one.
Modules compile to Java 17 bytecode.

```sh
./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug
```

- `spotlessApply` / `spotlessCheck`: ktlint (+ Compose rules) formatting for all `.kt`, `.kts` and `.xml` files
- `testDebugUnitTest`: JVM unit tests in every module (pure-Kotlin modules alias it to `test`)
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
:core:ui                Shared composables
:core:testing           Test rules, test clock, fakes

:feature:home | :feature:setup | :feature:tracker | :feature:settings
```

Dependency rules: features depend on `:core:*` only, never on each other, and `:app` wires navigation.
`:core:model` and `:core:game` have no Android dependencies. Only `:core:data` touches the database,
DataStore and assets.

## How to add Legends

Legends are data files. You don't need to change any code.

1. Add a line to `core/data/src/main/assets/legends/legends.json`:
   ```json
   { "id": "jinx", "name": "Jinx" }
   ```
   Ids are lowercase `snake_case`.
2. Optionally, drop a portrait at `core/data/src/main/assets/legends/<id>.webp` (`.png` also works).
   Square images look best; they're cropped to a circle.

A Legend without an image shows a circle in the player's color with the Legend's initial. The repo ships no
Legend images or Riot art. Add your own.

*(The Legend catalog and avatar land in Phase 1 and 2.)*
