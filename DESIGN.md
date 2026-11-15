# DESIGN.md: Rift Tally design guidelines (Material 3 Expressive, 2026)

> **Status:** binding for all UI work. `CLAUDE.md` points here. When this file and `HANDOFF_V2.md` §2 disagree on
> theme, motion, shape, elevation or components, **this file wins**. Layout changes are a last resort: change a
> layout only when the current one breaks a rule below, and say which rule in the commit/report.
>
> Researched 2026-11-14. Every rule cites evidence in [§11 Evidence](#11-evidence); IDs like **[E3]** point there.
> Rules marked *(house rule)* are our own decisions where no source gives a value; they are not Material claims.

---

## 1. Where Material is in 2026, and what that means for us

- **Material 3 Expressive (M3E)** is the current Material direction: research-backed updates to "theming,
  components, motion, typography, and more" **[E1]**. Its building blocks are **color, shape, size, motion and
  containment** **[E2]**. It was introduced with Android 16 (May 2025) **[E5]**, and I/O 2026 added an expressive
  layout system, spacing system, and expressive lists and menus **[E6]**.
- **Android is Compose-first:** "All new Android APIs, libraries, tools, and developer guidance will be built
  exclusively for Jetpack Compose." **[E7]** We are already 100% Compose. Keep it that way.
- **Library reality (verified in source):** our Compose BOM `2026.09.00` ships `material3` **1.4.0**, the latest
  *stable* release. 1.5.0 (where M3E APIs become public) is only at **1.5.0-rc01 (Nov 4, 2026)** **[E8]**. In 1.4.0,
  `MaterialExpressiveTheme`, `MotionScheme`, `MotionScheme.expressive()`, `MaterialTheme.motionScheme` and the
  emphasized type styles are all **`internal`**; `ButtonGroup`, `FloatingToolbar`, `LoadingIndicator`,
  `MaterialShapes` and `ToggleButton` **don't exist** **[E9]**.

### Decision: "Expressive on stable"
`CLAUDE.md` requires stable libraries, so we **do not** move to the 1.5.0 RC. Instead `:core:designsystem`
re-creates the M3E *spec values* (the same generated token numbers Material ships, **[E10]–[E13]**) as our own
tokens and a few hand-built components. When `material3` 1.5.0 is stable, swap our tokens for
`MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())` and the official components (see §10).

---

## 2. Principles for a table-side score tracker

1. **Function first.** "No amount of expressive design will beat basic functionality." and "When basic interaction
   paradigms are broken, expressive design can lead to poor usability." **[E2]** The score must be readable from
   across a table in a dim room before anything else. Never trade legibility for decoration.
2. **Make the key thing stand out.** M3E's measured win is attention: people found key UI elements **up to 4×
   faster**, and users 45+ performed on par with younger users **[E2]**. On every screen, decide the *one* hero
   element (tracker: the score; Home: Resume/New game; detail: the final result) and give it the size, color and
   containment. Everything else steps back.
3. **Context decides intensity.** Expressive "might not be suitable for something like a banking interface", and "a
   strong minority of users preferred calmer, less intense versions" **[E2]**. Our split:
   - **Expressive:** tracker table, target-reached moment, Home hero, Legend picker.
   - **Calm/standard:** Settings, History list, Stats tables, dialogs. Utilitarian screens use the *standard*
     motion scheme and quieter shapes.
4. **Keep labels.** Removing text labels from actions hurt usability in Google's research **[E2]**. Icon-only buttons are
   allowed only where the meaning is universal (−, +, undo, back), and each has a `contentDescription`.
5. **The Legend art is the brand.** Big art, dark scrim, player color as an *accent* (HANDOFF_V2 §2). We don't
   invent a separate visual language on top of it.

---

## 3. Color

- **Scheme roles only.** Use `MaterialTheme.colorScheme` roles (`surfaceContainer*`, `primary`, `onPrimary`, …) and
  `RiftTheme` player colors. No hex values outside `:core:designsystem` (CLAUDE.md).
- **Dark-first** for dim rooms (existing v1 decision). Light theme must still pass contrast.
- **Contrast (hard rule):** text < 18sp (or bold < 14sp) needs **≥ 4.5:1**; other text and meaningful icons **≥ 3:1**
  **[E14]**. Scores over art are measured against the **scrim**, not the art. M3E deliberately *exceeds*
  contrast and target-size standards **[E2]**, and so do we.
- **Tonal surfaces over borders.** Group content by placing it on a different `surfaceContainer` tone
  (containment, **[E2]**), not by adding outlines. Outlines are for inputs and selection states.
- **Player color = accent.** Use the player color for the edge glow (3–4dp), the name chip, chart lines and
  selection rings, never as a full-panel fill (HANDOFF_V2 §2).
- **Secondary color for the primary action** is fine when it improves findability; Google's Send-button study
  used a larger button in a secondary color **[E2]**.
- **Dynamic color** stays opt-in (Settings), default off, because Legend art and player colors carry identity.

## 4. Shape

**Corner-radius scale** (M3 tokens, verified in source **[E11]**). Expose these as `RiftTheme.shapes` and nothing else:

| Token | Radius | Use in Rift Tally |
|---|---|---|
| none | 0dp | edge-to-edge surfaces with no container (backgrounds, scrims) |
| extraSmall | 4dp | tiny badges |
| small | 8dp | chips, inline tags |
| medium | 12dp | list cards (History rows), text fields |
| large | 16dp | standard cards, sheets' inner cards |
| largeIncreased | 20dp | Legend picker cards |
| extraLarge | 28dp | tracker panels (seats separated by gaps), dialogs, bottom sheets (top corners) |
| extraLargeIncreased | 32dp | Home hero (Resume) card |
| extraExtraLarge | 48dp | full-screen hero containers, if any |
| full | pill/circle | buttons, the floating control bar, counter pills, avatars |

- **Mix shapes on purpose.** M3E adds 35 shapes and shape morphing **[E4]**. On stable we only have rounded
  corners, so get the contrast from *pill vs. rounded-rect vs. circle*: buttons and bars are pills, cards are rounded
  rects, avatars are circles.
- **Press morph** *(house rule, modeled on M3E buttons)*: pressable pills/cards shrink their corner radius slightly
  when pressed (for example full → 16dp on a 56dp button) using the **fast spatial** spring. Never morph tracker
  tap zones, because they must feel instant.
- **No `MaterialShapes` polygons** until they're stable (the promotion was reverted in 1.5.0-alpha19) **[E8]**.

## 5. Typography

- **One display face** (bundled OFL font, HANDOFF_V2 §2) for scores, Legend names on hero surfaces and screen
  titles. **Body stays on the default** sans.
- **Score:** sized from the panel (auto-size to fit), **tabular figures** (`fontFeatureSettings = "tnum"`) so digits
  don't jump, soft shadow on art. Androidify does the same with `TextAutoSize.StepBased(maxFontSize = 220.sp)`
  **[E3]**.
- **Emphasized styles.** M3E pairs each type role with an *emphasized* variant (a heavier weight; for example
  body-emphasized = Medium) **[E12]**. Stable hides them, so `RiftTheme.typography` provides `xxxEmphasized` for the
  roles we use (display, headline, title, label). Use the emphasized variant for **one** element per surface (the
  hero), and the regular variant for everything else. "Guide attention with typography." **[E4]**
- Never drop below `labelMedium` for anything a player needs at the table. Support font scale to 200%: let text
  wrap, and don't fix heights around text.

## 6. Elevation, shadows and depth ("perspective")

- **Levels** (dp, verified in source **[E13]**): 0, 1, 3, 6, 8, 12. Expose as `RiftTheme.elevation.level0…5`.
- **Tonal first.** In M3, elevation is shown mainly by surface *tone*, and shadows are a secondary cue. Shadows are
  nearly invisible on dark backgrounds, which is our default theme **[E15]** (secondary sources; the official page
  is client-rendered and couldn't be fetched).
- **Where a real shadow is allowed:**
  1. **Floating** elements that sit *over* content: the center control bar (level 3), snackbars and FAB-like
     buttons (level 3), dialogs and sheets (level 1 tonal + scrim).
  2. **Text shadow on the score** over art, for legibility, not depth.
- **Never** stack a custom `Modifier.shadow` on an M3 container that already has elevation: that doubles the shadow
  and skips the tonal overlay **[E15]**. Cards in lists are **level 0–1, tonal**.
- **Perspective / 3D:** none of the guidance we found recommends perspective tilt, parallax or 3D transforms for
  M3E. Depth comes from tone, scale and shadow **[E13][E15]**. *(House rule.)* No `graphicsLayer` rotationX/Y,
  camera distance or tilt effects. The 180° rotation of far-side tracker panels is orientation for the reader,
  not perspective, and stays.
- **Scrims:** use a vertical gradient over Legend art (transparent → ~70–80% `scrim` at the bottom) so the score
  passes 4.5:1. Measure in screenshot tests on the brightest art.

## 7. Motion

**Springs, not durations.** M3E motion is physics-based ("springy") **[E5]**. A motion scheme gives six specs,
**spatial** (position, size, scale, rotation, shape) and **effects** (color, alpha), each at fast/default/slow.
Spatial springs can overshoot; effects springs never bounce **[E10]**.

Token values (generated Material tokens, verified in source **[E10]**):

| Spec | Expressive (damping / stiffness) | Standard (damping / stiffness) |
|---|---|---|
| fastSpatial | 0.6 / 800 | 0.9 / 1400 |
| defaultSpatial | 0.8 / 380 | 0.9 / 700 |
| slowSpatial | 0.8 / 200 | 0.9 / 300 |
| fastEffects | 1.0 / 3800 | 1.0 / 3800 |
| defaultEffects | 1.0 / 1600 | 1.0 / 1600 |
| slowEffects | 1.0 / 800 | 1.0 / 800 |

- `:core:designsystem` exposes these as `RiftTheme.motion` (`expressive` and `standard`, each with
  `fastSpatial<T>()` … `slowEffects<T>()` returning `spring(dampingRatio, stiffness)`). **No `tween(…)` with
  hand-picked durations** in feature code. One exception: infinite repeating effects (the target pulse) may use
  `infiniteRepeatable` with a tween.
- **Which scheme:** *expressive* for hero interactions on hero screens (score change, target reached, Home hero,
  picker selection); *standard* for "utilitarian UI elements and recurring interactions" **[E10]** (Settings,
  History, Stats, dialogs, list items).
- **Which speed:** fast = small or direct-touch feedback (score roll, press morph, counter pill); default = component
  enter/exit and state changes; slow = full-screen or hero transitions.
- **Tracker specifics** (keeps the HANDOFF_V2 caps): score roll uses **fastSpatial** for the vertical slide and
  **fastEffects** for the fade (it settles in ≈150ms). Panel entry is a staggered **defaultEffects** fade with
  ≤ 40ms stagger. Nothing on the tracker may delay input.
- **Reduce motion (house rule):** if `Settings.Global.ANIMATOR_DURATION_SCALE == 0`, use snap/no-op specs and a
  **static** gold border for target reached. No official Compose guidance was found for this, so this is our
  rule **[E16]**.
- **Predictive back:** it's on by default and has no developer-option toggle since Android 15. Use
  `PredictiveBackHandler`/`BackHandler` only when needed, because a default-priority callback disables the
  system back animation **[E17]**. Navigation transitions should be compatible with predictive back.

## 8. Layout, containment and adaptivity

- **Edge-to-edge** is enforced when targeting API 35+ **[E18]** (we call `enableEdgeToEdge()`). The tracker
  window background draws behind the system bars; the seats (28dp-rounded panels, §4) sit inside
  `WindowInsets.safeDrawing` so scores and controls never land under a cutout or a rounded display corner.
  *Decision (v2 design pass):* rounded seats with gaps were chosen over full-bleed panels because they read as
  separate seats at the table and match the device's rounded corners. Other screens use `Scaffold` insets.
- **Any window size.** Android 17 (API 37, our target) ignores `screenOrientation`, `resizeableActivity=false` and
  aspect-ratio limits on large screens (sw ≥ 600dp), and apps "must be ready to adapt to any window size" **[E7]**.
  Size from `currentWindowAdaptiveInfo()` window size classes. Width: compact < 600, medium 600–839,
  expanded 840–1199, large 1200–1599, extra-large ≥ 1600. Height: compact < 480, medium 480–899,
  expanded ≥ 900. Landscape phones are usually **medium width × compact height** **[E19]**.
  - Tracker: panel arrangement already depends on player count; make sure 2p/3p/4p also work in landscape
    (compact height) without clipped scores. The score auto-sizes.
  - List screens (History, Stats, Settings): single column on compact; on expanded width, limit content width
    (≈ 640dp) or use list-detail (History → Game detail, Stats → Player). Use list-detail only if it's cheap; a
    centered max-width column is acceptable.
- **Containment.** Group related controls into one container (M3E "contain content for emphasis" **[E4]**):
  - **Tracker control bar** = a hand-built **floating toolbar**: a full-radius pill, `surfaceContainer` at ~85%
    alpha, level-3 shadow, Undo · Menu with ≥ 48dp targets, centered on the seam between players. (`FloatingToolbar`
    is in 1.5.0 only **[E8]**; mirror its look.)
  - **Counters** = compact pills (full radius) with icon, value and −/+.
- **Spacing:** use `RiftTheme.spacing` only. (I/O 2026 introduced a new Material spacing system **[E6]**; its values
  weren't retrievable, so our scale stays as is.)
- **Lists** (History, Stats, Settings): M3E lists are *segmented*. Rows sit on their own rounded
  `surfaceContainer` tiles with small gaps, instead of dividers on one flat surface (expressive list items are
  non-experimental in 1.5.0 **[E8]**, and Gmail's 2026 redesign uses rounded cards and larger controls **[E20]**).
  First and last tiles in a group use larger outer corners (`large`) and inner corners `extraSmall`/`small`.

## 9. Components and interaction

- **Buttons:** pills (full radius), min height 48dp (56dp for the primary action on hero screens), press morph
  (§4). Primary action = filled; secondary = tonal; tertiary = text. One filled button per screen region.
- **Touch targets ≥ 48×48dp** on everything interactive **[E14]**. Tracker tap halves are much larger by design.
- **Icon buttons** always have a `contentDescription` (CLAUDE.md).
- **Chips** (History filters, Stats sort): `FilterChip`, small radius, single row that scrolls horizontally on
  compact width.
- **Dialogs and sheets:** extraLarge (28dp) corners. Pickers that need space (Legend picker) use a bottom sheet
  on compact width.
- **Haptics:** use predefined feedback constants for consistency ("If your action is covered by a predefined
  action … use that constant.") **[E21]**. Frequent events must be very subtle; "More important events … should be
  stronger" **[E21]**. Score ±1 = `SegmentTick` (subtle). Target reached = `Confirm`. Undo = `Reject`/`ContextClick`.
  No raw `Vibrator` one-shots **[E21]**. Respect the haptics setting.
- **Loading:** the app is local and fast. Avoid spinners; show content or skeleton tones. (`LoadingIndicator` isn't
  stable **[E8]**.)
- **Empty states:** an icon or Legend-art motif, one sentence and one action. Never a blank screen.

## 10. Implementation contract for `:core:designsystem`

Track A (or whoever owns the design system) provides:

```kotlin
RiftTheme.shapes        // §4 table: none … extraExtraLarge, full
RiftTheme.elevation     // level0..level5 = 0, 1, 3, 6, 8, 12 dp
RiftTheme.motion        // .expressive / .standard → fastSpatial<T>() … slowEffects<T>() springs (§7 values)
RiftTheme.typography    // M3 roles + displayLargeEmphasized … labelLargeEmphasized; score style (display font, tnum)
RiftTheme.spacing       // existing
reduceMotionEnabled()   // @Composable, reads ANIMATOR_DURATION_SCALE == 0
RiftFloatingToolbar     // pill container (§8)
Modifier.pressMorph()   // or a RiftPressableSurface (§4)
```

Feature modules use only these; they never call `spring()`, `tween()`, `RoundedCornerShape(n.dp)`, `.shadow()`
or raw `Color(...)` directly. **Upgrade path:** when `material3` 1.5.0 is stable, back `RiftTheme.motion` with
`MaterialTheme.motionScheme`, wrap the app in `MaterialExpressiveTheme`, and replace `RiftFloatingToolbar` with
`HorizontalFloatingToolbar`. Feature code shouldn't need changes.

### Per-screen checklist (use in reviews and screenshot checks)
- [ ] One hero element is obvious at a glance (§2.2).
- [ ] Uses the right motion scheme for the screen type (§2.3, §7), springs only.
- [ ] Shapes and elevations come from tokens, and only floating elements cast shadows (§4, §6).
- [ ] Text contrast ≥ 4.5:1 (≥ 3:1 large), checked on the brightest art (§3).
- [ ] Targets ≥ 48dp, icon buttons labeled, works at 200% font scale (§5, §9).
- [ ] Edge-to-edge with insets respected, landscape compact-height OK, no orientation lock assumed (§8).
- [ ] Reduce-motion path is static (§7).
- [ ] No perspective/3D transforms (§6).
- [ ] Light + dark Roborazzi screenshots recorded and *looked at*.

---

## 11. Evidence

Quotes are verbatim from the source on 2026-11-14. "Verified in source" means read from the AndroidX code/jar.

| ID | Source | What it supports |
|---|---|---|
| E1 | developer.android.com, *Material Design 3 in Compose* (developer.android.com/develop/ui/compose/designsystems/material3): "M3 Expressive is an expansion of Material Design 3, including research-backed updates to theming, components, motion, typography, and more" | §1 |
| E2 | Google Design, *Expressive design: Google's UX research* (design.google/library/expressive-material-design-google-research): 46 studies, 18,000+ participants; key elements spotted "up to four times faster"; age effects for 45+ erased; preference up to 87% (18–24); color, shape, size, motion and containment as the building blocks; exceeds tap-target and contrast standards; "No amount of expressive design will beat basic functionality."; "might not be suitable for something like a banking interface."; removing text labels reduced usability; "a strong minority of users preferred calmer, less intense versions." | §2, §3, §8, §9 |
| E3 | Android Developers Blog, *Androidify: Building delightful UIs with Compose* (May 2025): `MaterialExpressiveTheme` with `motionScheme = MotionScheme.expressive()`; press scale with `MaterialTheme.motionScheme.defaultSpatialSpec()`; `TextAutoSize.StepBased(maxFontSize = 220.sp)`; `HorizontalFloatingToolbar` | §5, §7, §8 |
| E4 | supercharge.design, *Material 3 Expressive* (updated June 2026, secondary): 35 new shapes + shape morphing; tactics: varied shapes, rich color, "Guide attention with typography", "Contain content for emphasis", fluid motion, hero moments | §4, §5, §8 |
| E5 | Google Keyword, *Material 3 Expressive* launch (May 13, 2025): "a system of more natural, springy animations"; launched with Android 16 / Wear OS 6 | §1, §7 |
| E6 | Material blog, *What's new at Google I/O 2026* (m3.material.io/blog/whats-new-at-io26), as summarized by search (page is client-rendered): expressive layout scaffold, new spacing system, expressive lists and menus, Compose-first; 1.5.0 promotes M3E to stable | §1, §8 |
| E7 | Android Developers, *Android 17 is here* (Jun 16, 2026): "Android development is now Compose-first."; API 37 removes the orientation/resizability opt-out on sw > 600dp; "Your app must be ready to adapt to any window size, respect the user's preferred device posture, and support free-form windowing natively." | §1, §8 |
| E8 | AndroidX *Compose Material3 releases* (developer.android.com/jetpack/androidx/releases/compose-material3): stable 1.4.0; 1.5.0-rc01 Nov 4, 2026; alpha19 "Revert `MaterialShapes` and `LoadingIndicator` promotions to stable."; alpha22 `ButtonGroup` stable and `FloatingToolbar` non-experimental; alpha23 "Expressive list item APIs are no longer experimental." | §1, §4, §8, §9 |
| E9 | material3-android **1.4.0** sources jar (dl.google.com maven), verified in source: `internal fun MaterialExpressiveTheme`, `internal interface MotionScheme`, `internal fun expressive()`, `internal val motionScheme`, `internal val displayLargeEmphasized`; no `ButtonGroup`, `HorizontalFloatingToolbar`, `LoadingIndicator`, `MaterialShapes`, `ToggleButton` | §1 |
| E10 | AndroidX `ExpressiveMotionTokens.kt` / `StandardMotionTokens.kt` (token v0_14_0) and `MotionScheme.kt` KDoc, verified in source: spring values in §7; spatial = "may change the shape or bounds", effects = "do not change the shape or bounds … For example, color animation"; standard = "for utilitarian UI elements and recurring interactions"; expressive = "recommended motion scheme for prominent UI elements and hero interactions" | §7 |
| E11 | AndroidX `ShapeTokens.kt`, verified in source: 0, 4, 8, 12, 16, 20 (LargeIncreased), 28, 32 (ExtraLargeIncreased), 48 (ExtraExtraLarge), Full | §4 |
| E12 | AndroidX `TypeScaleTokens.kt` / `TypographyTokens.kt`, verified in source: `*Emphasized` styles for every role, for example `BodyLargeEmphasizedWeight = WeightMedium` | §5 |
| E13 | AndroidX `ElevationTokens.kt` (v0_103), verified in source: Level0–5 = 0, 1, 3, 6, 8, 12 dp | §6 |
| E14 | Android Developers, *Make apps more accessible* (developer.android.com/guide/topics/ui/accessibility/apps): touch targets at least 48dp; contrast 4.5:1 for small text, 3:1 otherwise | §3, §9 |
| E15 | Secondary Compose/M3 elevation guides (for example skill-wiki "Elevation Tokens"): tonal color carries elevation in M3, with shadow as a hint; don't add a shadow modifier to an elevated M3 container; shadows hard to see on dark backgrounds. Official m3.material.io/styles/elevation is client-rendered and couldn't be fetched. | §6 |
| E16 | Android Developers, *Animations in Compose* intro: no reduce-motion guidance found (gap noted; our rule is a house rule) | §7 |
| E17 | Android Developers, *Predictive back gesture*: "Predictive back is enabled by default."; dev option removed in Android 15; a default-priority `OnBackPressedCallback` disables predictive animations; `PredictiveBackHandler` / `BackHandler` in Compose | §7 |
| E18 | Android Developers, *Set up edge-to-edge* (develop/ui/compose/system/setup-e2e): "If you target devices running Android 15 (API level 35) or higher, edge-to-edge is enforced by default." | §8 |
| E19 | Android Developers, *Window size classes*: width compact < 600, medium 600–839, expanded 840–1199, large 1200–1599, extra-large ≥ 1600; height compact < 480, medium 480–899, expanded ≥ 900; landscape phones are typically medium width × compact height | §8 |
| E20 | Android Authority, Gmail M3E settings redesign (Nov 13, 2026): rounded cards, larger controls, colorful icons | §8 |
| E21 | Android Developers, *Haptics design principles*: "If your action is covered by a predefined action present in HapticFeedbackConstants, use that constant."; frequent events subtle, important events stronger; avoid single-shot vibrations | §9 |

**Known gaps:** m3.material.io spec pages (motion, shape, elevation, I/O 2026 blog) render client-side and couldn't
be read directly. Their *values* were taken from the AndroidX generated tokens instead ([E10]–[E13]), which are
produced from the same spec. Re-check §4–§8 against m3.material.io in a browser when convenient.
