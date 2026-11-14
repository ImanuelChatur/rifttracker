# Agent workflow: how to build this project

> Read `HANDOFF.md` (what to build) and `CLAUDE.md` (conventions) first. This file says **how to run the work**: when to use one agent, when to use parallel subagents, and how to review.
> Default to **one agent**. Only parallelize where this file says to.

---

## Ground rules

1. **One phase at a time.** Never start a phase until the previous one passes its gate.
2. **Gate for every phase:** `./gradlew spotlessApply spotlessCheck lint testDebugUnitTest assembleDebug` passes, the review subagent's blocking issues are fixed, and the work is committed.
3. **Parallel subagents only in their own git worktree.** Never have two agents editing the same checkout. Add `.claude/worktrees/` to `.gitignore`.
4. **File ownership:** each parallel subagent may only edit the paths listed for it. If it needs a change outside its paths (e.g. a new entry in `libs.versions.toml`), it **lists the change in its final report** instead of making it, and the main agent applies it during the merge.
5. **Subagents start without this conversation.** Every subagent prompt must tell it to read `HANDOFF.md`, `CLAUDE.md` and (for any UI work) `DESIGN.md` and must state its exact scope, owned paths and expected report.
6. **Stop and report** at the end of each phase with: what was built, decisions made, anything skipped, and test results. Then wait for the developer to say "continue".

---

## Phase 0: Foundation · **single agent**

The main agent does all of Phase 0 itself (`HANDOFF.md` §6). No subagents: almost every file here is shared (`settings.gradle.kts`, `libs.versions.toml`, `build-logic/`).

Before finishing Phase 0, also:
- Add **every library needed for v1** to `libs.versions.toml` now (Compose, Hilt, Room, DataStore, Navigation, serialization, immutable collections, Coil 3, test libs), so later parallel agents don't need to touch it.
- Create every v1 module with its `build.gradle.kts` and an empty package, wired into `settings.gradle.kts`.

→ Gate → review → commit `feat(phase-0): foundation` → **stop and report.**

---

## Phase 1: two parallel subagents (worktrees)

The main agent spawns **two subagents in parallel, each in its own git worktree**, then merges.

### Subagent 1A: Game state
- **Scope:** `HANDOFF.md` Phase 1: models, events, reducer, target check, undo, all unit tests.
- **Owns:** `core/model/**`, `core/game/**`
- **Must not touch:** anything else.
- **Report:** public API summary (types and functions), test list and results, requested changes outside owned paths.

### Subagent 1B: Design system + Legends
- **Scope:**
  - `:core:designsystem`: theme (dark-first, dynamic color option), color palette (6–8 colorblind-friendly player colors, AA contrast), typography incl. a huge score style, spacing tokens, base buttons.
  - `:core:data` Legend pieces: `legends.json` seed, `LegendRepository` (interface + internal impl + Hilt binding), asset-existence check for `<id>.webp`/`.png`.
  - `:core:ui`: `LegendAvatar` (Coil image, colored-circle-with-initial fallback, score overlay with scrim) and previews with and without an image.
- **Owns:** `core/designsystem/**`, `core/ui/**`, `core/data/src/main/assets/legends/**`, `core/data/**/legend/**`
- **Must not touch:** `core/model/**`, `core/game/**`, build files. If it needs a `Legend` model before 1A finishes, it defines it in `core/data/**/legend/` and notes this so the main agent can move it to `:core:model` during merge.
- **Report:** components created, screenshots of previews if possible, requested changes outside owned paths.

### Main agent: merge
1. Merge 1A, then 1B. Resolve conflicts, apply the requested out-of-scope changes, and move any duplicated models into `:core:model`.
2. Run the gate. Fix failures.
3. Review → commit `feat(phase-1): game state, design system, legends` → **stop and report.**

---

## Phases 2–5 · **single agent**

The main agent builds each phase itself, in order (`HANDOFF.md` §6). These phases cut across modules (setup → tracker → persistence → navigation), so parallel agents would collide.

- **Phase 2:** the Legend pieces already exist from 1B. Wire them into Setup and Tracker; don't rebuild them.
- After each phase: gate → review → commit `feat(phase-N): <summary>` → **stop and report.**

Exception: in **Phase 5**, writing the **README** and **accessibility audit** may go to one subagent in parallel, since they're read-mostly. It owns only `README.md` and reports accessibility issues as a list instead of fixing them.

---

## Review subagent (end of every phase)

After the gate passes, spawn **one fresh subagent** (it must not be the one that wrote the code) with this prompt:

> You are reviewing Phase N of an Android app. Read `HANDOFF.md`, `CLAUDE.md`, `DESIGN.md` and `AGENT_WORKFLOW.md`, then review the changes in this phase (`git diff <phase-start-commit>..HEAD`). **Do not edit any files.** Report:
> 1. **Blocking:** bugs, crashes, spec mismatches with HANDOFF.md, violations of CLAUDE.md rules or `DESIGN.md` rules (motion/shape/elevation tokens, contrast, touch targets, §10 checklist), module boundaries, pure-Kotlin core, event log, Route/Screen split, hardcoded strings/colors/dimens).
> 2. **Should fix:** missing tests for listed cases, missing previews, accessibility gaps, unclear naming.
> 3. **Nice to have:** anything else, briefly.
> For each item give the file, the line, and a one-line fix. Keep it under 40 items. Don't flag style issues Spotless already handles.

The main agent fixes all **Blocking** items and anything **Should fix** that's quick, re-runs the gate, then commits.

---

## Quick reference

| Phase | Agents | Notes |
|---|---|---|
| 0 Foundation | 1 | Add all v1 libraries + modules up front |
| 1 Game state + design system/Legends | 2 parallel (worktrees) + merge | Strict file ownership |
| 2 2-player tracker | 1 | Reuse 1B components |
| 3 Extra counters | 1 | |
| 4 3p / 4p / 2v2 | 1 | |
| 5 Settings & polish | 1 (+1 optional for README/a11y audit) | |
| Every phase | +1 reviewer | Read-only, fresh context |
