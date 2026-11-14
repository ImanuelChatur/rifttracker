# CLAUDE.md: Project conventions

Riftbound points tracker for Android (future Riftbound companion app). The full spec and phase plan is in `HANDOFF.md`. How to run the work (single agent vs parallel subagents, reviews) is in `AGENT_WORKFLOW.md`. **Work one phase at a time.**

## Workflow
- Before coding a phase, restate its scope in 3–5 bullets and list the files/modules you'll touch.
- After each phase run: `./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug`. Fix everything before reporting done.
- Commit at the end of each phase: `feat(phase-N): <summary>`.
- Use the latest **stable** library versions; check them rather than relying on memory. All versions go in `gradle/libs.versions.toml`, never inline.
- Don't add a dependency without a clear reason; mention any new one in your summary.
- If the spec is ambiguous, pick the simplest option consistent with `HANDOFF.md` and note the decision in your summary.

## Adding Legends (keep this working)
- Legends live in `core/data/src/main/assets/legends/legends.json` (`id`, `name`) with optional images `<id>.webp`/`.png` in the same folder.
- A missing image must fall back to a colored circle with an initial. Never crash or show a broken image.

## Architecture rules
- Module boundaries per `HANDOFF.md` §4.2. Features never depend on other features.
- `:core:game` and `:core:model` are **pure Kotlin**: no `android.*`, no Compose, no Hilt annotations.
- The game state is **derived from events** (`reduce(config, events)`). Never mutate scores directly; always append an event.
- Repositories: interface `public`, implementation `internal`, bound with Hilt `@Binds` in the same module.
- Inject `CoroutineDispatcher`s (qualifiers `@IoDispatcher`, `@DefaultDispatcher`) and a `Clock`; never hardcode `Dispatchers.IO` or `System.currentTimeMillis()`.
- Use KSP for Hilt and Room (no kapt).

## Compose / UI conventions
- Screen = `XxxRoute` (stateful, `hiltViewModel()`, `collectAsStateWithLifecycle()`) wrapping `XxxScreen` (stateless, `uiState` + lambdas).
- `UiState` is an immutable `data class` (or `sealed interface` for Loading/Success/Error). Use `ImmutableList` from kotlinx-collections-immutable for lists in state.
- ViewModels expose `StateFlow` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- User actions are named ViewModel functions (`onIncrementPoints(sideId)`), not a generic `onEvent`.
- Composables: first optional param is `modifier: Modifier = Modifier`; hoist state; no business logic inside composables.
- Use `:core:designsystem` tokens (`RiftTheme.spacing`, typography, colors). No hardcoded colors/dimens in feature modules.
- All user-facing strings in `strings.xml`; plurals via `pluralStringResource`.
- Every screen and reusable component gets `@Preview`s (light + dark; 2/3/4 players for tracker panels).
- Touch targets ≥ 48dp; every icon button has a `contentDescription`.
- Navigation: type-safe `@Serializable` routes. Each feature exposes `fun NavGraphBuilder.xxxScreen(...)` and `fun NavController.navigateToXxx(...)`; `:app` wires them together.

## Kotlin style
- Official Kotlin style, enforced by Spotless. Explicit API visibility in core modules (`internal` by default).
- Prefer `sealed interface` over `sealed class`; `value class` for IDs (`PlayerId`, `SideId`, `GameId`).
- No `!!`. No `GlobalScope`. No `runBlocking` outside tests.

## Testing
- `:core:game`: exhaustive unit tests (see `HANDOFF.md` Phase 1 list). Write tests alongside, not after.
- ViewModels: test with fake repositories from `:core:testing`, `kotlinx-coroutines-test`, and Turbine.
- Room: DAO tests plus a migration test for every schema version.
- Compose: at least one UI test per screen (semantics-based, not screenshot).

## Naming
- Packages: `com.rifttally.app` (app), `com.rifttally.core.<name>`, `com.rifttally.feature.<name>`.
- Files: one public top-level class/composable per file, named after it.

## Don'ts
- No network permission or network code in v1.
- Don't download, generate or bundle Legend images or any Riot logos/art. The developer adds Legend images to `core/data/src/main/assets/legends/` themselves; code must work with or without them.
- Keep the tracker simple: no best-of series, turn tracking or rules enforcement.
- Don't build future roadmap features (cards, decks, collection, stats) or add placeholder code for them.
