# Apply Progress: UI Redesign Sync

## Change Readiness

**NOT READY.** `verify-report.md` remains `FAIL`; final independent verification task 7.4d is open. Completed
implementation slices do not make this change archive-ready.

## Slice Status

| Slice | Status | Notes |
|-------|--------|-------|
| 1 — Foundation/tokens (PR 1) | COMPLETE | Tasks 1.1–1.9 accepted. |
| 2 — Auth | COMPLETE | Tasks 2.1–2.4 accepted. |
| 3 — Profile | COMPLETE | Tasks 3.1–3.5 complete; 3.5 is the documented Profile v2 waiver. |
| 4 — Home | COMPLETE | Tasks 4.1–4.5 accepted. |
| 5 — Lesson map only | COMPLETE | Tasks 5.1–5.6 accepted. |
| 6 — Exercise player + TheorySheet + onboarding + states | COMPLETE | Tasks 6.1–6.7 accepted. Latest full JVM evidence: 31 suites / 158 tests, 0 failures/errors/skips. |
| 7 — Verification remediation | IN PROGRESS | Tasks 7.4a–7.4c complete; final independent verification remains pending. |

## Recovery Boundaries

Historical recovery notes may name unrelated paths that were observed in earlier snapshots. They are retained only as historical evidence and are **not current-worktree claims**. Current Git verification reports no change for `package.json`, `package-lock.json`, or `scripts/configure-android-wsl-portproxy.ps1`; do not treat those paths as part of the redesign scope, recovery boundary, staging set, or rollback boundary.

Do not infer current unrelated changes from historical snapshots; resolve separate ownership only from a fresh Git inspection.

## Mode

This change uses **Standard mode** (`strict_tdd: false` in `openspec/config.yaml`); whole-change RED/GREEN provenance is neither required nor claimed. Later corrective units retain any factual local TDD history, but the failed report's `Mode: Strict TDD` label is stale and must be superseded by task 7.4d rather than retroactively rewritten. Delivery: chained PR slices of 6, strategy `stacked-to-main` (per tasks.md Review Workload Forecast).

## Files Changed (Slice 1)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/ColorTokens.kt` | Modified | +4/-0 | `BrandTrack` #EADFD1, `BrandLock` #CBBEAE, `BrandStripe` #F2E9DD, `BrandCoralShadow` #6BF2654B |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/ShapeTokens.kt` | Modified | +14/-4 | Added `checkbox` 7, `iconBox` 13, `socialButton` 14, `stepSegment` 999; card 28→18, button 20→16, field 18→15 |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/TypeTokens.kt` | Modified | +36/-14 | `rememberSoraFontFamily()` (composeResources, 4 weights), pure `buildAppTypography(fontFamily)` with redesign scale, `AppTypography` now a `@Composable` getter |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/theme/AppTheme.kt` | Modified | +2/-0 | Wired `extraSmall`=checkbox, `extraLarge`=pill into MaterialTheme `Shapes` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modified | +8/-1 | `Modifier.shadow(12.dp, shape, ambient/spot = BrandCoralShadow)` on Filled variant |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MProgressIndicator.kt` | Modified | +10/-2 | Linear variant: 8dp height, pill clip, round caps, `BrandTrack` track color |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/theme/AppThemeTokensTest.kt` | Modified | +38/-11 | New semantic-color test; updated shape values; typography asserts redesign scale + family injection |
| `composeApp/src/commonMain/composeResources/font/sora_{regular,semibold,bold,extrabold}.ttf` | New (pre-existing from prior batch, verified) | — | Sora OFL, 4 weights |
| `composeApp/src/commonMain/composeResources/files/OFL.txt` | New (pre-existing from prior batch, verified) | — | Sora license |

Authored slice total: ~112 insertions + 32 deletions across 7 Kotlin files (well under the ~160-line forecast; 400-line budget not at risk).

`MCard.kt` and `MTextField.kt`: intentionally unchanged — card 18dp arrives via `shapes.large`, field 15dp via `shapes.small`; `authStyle` already hardcoded 15dp (now token-aligned).

## Files Changed (Slice 2)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/LoginScreen.kt` | Modified | +4/-8 | Forgot-password link → `bodyMedium` (13/600); divider text → `labelMedium` (12/600 muted, lines unchanged); footer → `titleSmall` (14/500 muted) + `titleMedium` (14/700 coral) "Registrate", underline removed per handoff |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/RegisterScreen.kt` | Modified | +64/-13 | Step segments 6dp/3dp → 5dp/`stepSegment` (999dp), gap 8→6dp; label "Paso X de 3" → "Paso X / 3" in `labelMedium` (12/600) with mono family; strength fill `primary` → `secondary` (teal #0E9E8E); M3 `Checkbox` → custom 22×22dp box, `checkbox` 7dp radius, coral + on-coral Canvas checkmark, row now single `toggleable(role = Checkbox)` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/AuthScreenScaffold.kt` | Modified | +24/-5 | Brand mark card radius `card` (18) → `button` token (16dp per handoff); wordmark → 22sp/800, −0.02em, two-tone "Mathim" ink + "App" muted/500 via `AnnotatedString` (single semantics node); form title kept 27/800 (see Deviations) |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MButton.kt` | Modified | +13/-3 | `Social` variant: shape `shapes.medium` (16dp) → `socialButton` token (14dp), border 1→1.5dp `outlineVariant`; Filled/Outline untouched (CTA 16dp + coral shadow already from Slice 1) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/AuthRedesignRenderTest.kt` | New | +109 | 3 render tests: login redesign copy + "Registrate" navigation; "Paso N / 3" copy follows wizard step; terms checkbox 22×22dp + role/toggle behavior through the row |

Authored slice total: ~114 insertions + 29 deletions across 4 main-source files (+109-line new test file) = ~252 changed lines. Above the ~120-line forecast but far under the 400-line budget — Low risk holds for PR 2.

Theme files (`ui/theme/*`, `MProgressIndicator.kt`): intentionally untouched in this slice — consumed Slice 1 tokens only (`socialButton`, `stepSegment`, `checkbox`, `button`, `secondary`).

## Files Changed (Slice 3)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/ProfileScreen.kt` | Modified | +151/-59 | Streak chip + restyled role chip (shared `ProfileChip`: surface pill, 1px line border, 11/700); nav-card icons → tinted vectors (coral/teal/rose/muted); logout → `Surface(onClick)` card 16dp + logout icon + ink text; version caption `"MathimApp · versión ${appVersionName()}"`; header bar 38×38/12dp/surface2 + centered 17/700 title + 38dp spacer; row icons on all 4 sub-screens; dark-mode stub toggle; initials 32/800 + avatar/badge 3px borders; `✎`/`←` glyphs → vectors |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileNavigationCard.kt` | Modified | +11/-5 | Icon box 48→42×42dp, bg primaryContainer→surface2, radius `iconBox` (13dp); title → `titleMedium` 14/700 (dropped SemiBold override); subtitle → `labelSmall` 11/500 muted |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileListRow.kt` | Modified | +14/-0 | New `leadingIcon: DrawableResource?` param — 18dp muted vector, `testTag("rowLeadingIcon")` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/ProfileToggleRow.kt` | Modified | +25/-2 | Same `leadingIcon` param; Switch colors: checked track teal (`secondary`), unchecked track `BrandTrack` (handoff toggle spec) |
| `composeApp/src/commonMain/composeResources/drawable/ic_*.xml` | New (17 files) | +368 | Lucide/Feather-style stroke vectors (#111827/1.8/round, tinted at use): flame, person, settings, help_circle, info, mail, lock, bell, volume, moon, globe, logout, flag, file_text, shield, edit, arrow_left |
| `composeApp/src/commonMain/kotlin/.../ui/AppVersion.kt` (+ android/jvm/ios actuals) | New (4 files) | +21 | `expect fun appVersionName()`; androidMain → `BuildConfig.VERSION_NAME`; jvm/ios pinned `"1.0"` (no BuildConfig there) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileScreenTest.kt` | Modified | +3/-3 | Version caption assertion → "MathimApp · versión 1.0" (visual copy only; behavior assertions untouched) |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/ProfileRedesignRenderTest.kt` | New | +135 | 5 render tests: hub chips/boxes/logout/version; streak-chip omission at 0; Cuenta header+3 row icons; Preferencias dark-mode no-op stub + 4 row icons; Ayuda/Acerca headers+icon rows |

Authored slice total: +204/-69 modified Kotlin (273) + 524 new (368 drawable assets, 21 platform glue, 135 tests) = **~797 changed lines vs ~250 forecast → over the 400 budget.** 46% of the diff is 17 independent vector assets; the logic-adjacent Kotlin core matches the forecast. See Slice 3 PR Boundary for the recommended handling.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`iconBox`, `button`, `pill`, `BrandTrack`, `secondary`, `error`, `outlineVariant`). No new Gradle dependency (no material-icons; resources via composeResources like existing tab icons).

## Test Evidence (Slice 3)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–2; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 2m 43s` — **25 suites, 115 tests, 0 failures, 0 errors, 0 skipped** (was 24 suites / 110 tests; +1 suite `ProfileRedesignRenderTest`, +5 tests).
- `ProfileRedesignRenderTest`: 5/5 green —
  - `hub renders streak chip 42dp nav icon boxes logout card and dynamic version` ("Racha 5 días" + `streakChip` tag; 4 `navIconBox` @ 42×42dp; "Cerrar sesión"; "MathimApp · versión 1.0")
  - `streak chip is omitted when the user has no streak` (handoff "omit if not exists" rule locked in)
  - `account sub screen renders header bar and leading row icons` ("Volver" + 3 `rowLeadingIcon`)
  - `preferences sub screen renders dark mode stub toggle as a no-op` (3 switches; stub `assertIsOff → click → assertIsOff`)
  - `help and about sub screens render header bars and icon rows`
- `ProfileScreenTest` 2/2 green (logout click + hub↔Cuenta↔back navigation preserved; only the version-caption string assertion updated).
- `ProfileViewModelTest` 3/3 untouched and green — no ViewModel contract change.
- No RED→GREEN cycles this batch — suite green on first run.
- **Runtime harness**: compose render tests exercise the real `ProfileContent` + `AppTheme` (Sora) + composeResources vectors on JVM; hub→sub-screen→back navigation, logout click, and the no-op dark-mode stub all executed.
- Not semantics-observable, deferred to manual check: chip border/hairline rendering, icon glyph fidelity vs handoff, switch track colors, avatar/badge 3px borders.
- **Rollback boundary**: revert touches only `ui/ProfileScreen.kt`, `ui/primitives/{ProfileNavigationCard,ProfileListRow,ProfileToggleRow}.kt`, `ui/AppVersion*.kt` (4), `composeResources/drawable/ic_*.xml` (17), `jvmTest/.../{ProfileScreenTest,ProfileRedesignRenderTest}.kt` — no theme token, navigation, ViewModel, or contract changes.

## Suggested PR / Commit Boundary (Slice 3)

- **Branch**: `feature/profile-redesign` → targets `feature/auth-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 2 branch tip, ends with this slice — profile hub + sub-screens visuals, icon assets, version glue, render tests. No home/map/exercise screens, no dark-mode behavior, no ViewModel or navigation changes.
- **Suggested single work-unit commit**: `feat(ui): sync profile hub and sub-screens to redesign handoff` — hub chips/cards/logout + sub-screen header/icons + 17 vector assets + `appVersionName()` glue + render tests (tests with the behavior they verify).
- **400-line budget**: actual ~797 vs forecast ~250 → **budget exceeded. Maintainer decision required**: recommend `size:exception` (46% of the diff is 17 self-contained ≤45-line vector assets with zero logic; the Kotlin core is ~273 lines, on-forecast). Alternative if the exception is declined: split 3a hub (`ProfileScreen` hub + `ProfileNavigationCard` + chips/logout + `AppVersion` + 8 hub drawables + hub tests) / 3b sub-screens (rows/toggles + header bar + 9 row drawables + sub-screen tests).
- **Rollback boundary**: per-file revert of the Files Changed (Slice 3) list; Slices 1–2 remain valid without this slice.

## Deviations from Design (Slice 3)

1. **`perfil-usuario.png` (Jul 21) conflicts STRUCTURALLY with the spec deltas — specs/handoff implemented, PNG flagged.** The PNG shows a different profile concept: "Mi Perfil" title, XP progress card ("340 / 500 XP" + teal bar), stat cards ("7 días / Racha actual 🔥", "42 / Lecciones completas"), "MIS LOGROS" achievements + "Ver todas", member-since subtitle — and **no nav cards, no logout card, no version caption**. The spec deltas + Jul 16 handoff describe the nav-hub implemented here (orchestrator scoped this slice "visual-only per specs"). The PNG-wins rule was applied only to styling cues (teal/coral accents, surface cards on cream), which already match the tokens. **Design review required before archive** (design.md Open Question anticipated this). Note: `ProfileViewModel` already exposes `streak`/`completedLessons`/`achievements`/`level`/`currentXp` — the data layer anticipates the PNG layout, suggesting a planned profile v3; if the PNG is confirmed authoritative, that is a new change with spec amendments (structural, not visual-only), not a fix to this slice.
2. **Logout card 16dp, NOT 18dp** — tasks.md 3.2 says 18dp, but the spec delta ("16px radius") and handoff ("radio 16px") say 16. Delivered via the existing `button` shape token (16dp; theme files frozen). tasks.md shorthand treated as stale (same pattern as Slice 2 logo 18dp).
3. **Streak chip omitted when `streak == 0`** — the requirement text reads unconditional, but the scenario conditions on "user with streak" and the handoff rule says "si un ítem mostrado no existe (p. ej. racha), omitir el chip". Resolved in favor of scenario+handoff; locked by test. If the requirement is literal, flip the `streak > 0` guard.
4. **Dynamic version via new `expect/actual appVersionName()` (4 files)** — design's Slice 3 file list omitted it, but "version dynamic string" is impossible cross-platform without it. androidMain is truly dynamic (`BuildConfig.VERSION_NAME`, same import pattern as `ApiBaseUrl.android.kt`); jvm/ios pin `"1.0"` to the declared `versionName` (no BuildConfig on those targets — a shared build-time source is follow-up work).
5. **`ProfileToggleRow.kt` edited (+25/-2) though absent from design's Slice 3 file list** — the spec scenario mandates bell/volume/moon icons on Preferencias rows (3 of 4 are toggle rows) and "Switch with track color". Added `leadingIcon` + teal-on/`BrandTrack`-off colors per the handoff toggle spec. Design table was incomplete; spec won.
6. **`✎` and `←` glyphs replaced by vectors** — Slice 1 switched the family to Sora; dingbat/arrow codepoints are not guaranteed in Sora and would fall back to system fonts nondeterministically. Semantics ("Editar avatar", "Volver") preserved. `›` chevrons kept as text (Latin-1 guillemet, universally present).
7. **Handoff-exact identity polish beyond spec minimum**: initials 27→32/800 (`headlineLarge`), removed `FontWeight.Bold` overrides that downgraded 800-weight tokens to 700 (name, initials, card titles), added 3px surface/app-bg borders on avatar + edit badge. Same rationale as Slice 2's wordmark: explicit in the handoff's component spec.
8. **Both chips restyled to handoff** — role chip was secondaryContainer/no-border/`labelLarge`; the "matches role chip styling" scenario + handoff chip spec require surface bg + 1px line border + pill + 11/700 for both. Role chip text teal, streak chip coral + flame.
9. **Icons are Lucide/Feather-style stroke vectors** (ISC/MIT geometry) matching the project's existing stroke style (#111827, 1.8dp, round caps/joins), tinted via `Icon(tint)`. No `material-icons` dependency added (AGENTS.md dependency rule).
10. **Version caption at 11sp (`labelSmall`), not the handoff's 10px** — no 10sp slot exists in the token scale and hardcoding `10.sp` would be a magic number; minor fidelity gap flagged for design review.

## Issues Found (Slice 3)

- **PNG↔spec structural drift (see Deviation 1)** — the Jul 21 `perfil-usuario.png` cannot be reconciled with the spec deltas within a visual-only slice. This blocks task 3.4's manual pixel comparison: there is no implemented screen comparable to the PNG. Escalate to design review before archive.
- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). androidMain actual reuses the verified `BuildConfig` import pattern from `ApiBaseUrl.android.kt`; iosMain actual is a trivial constant. Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 3 review closes.

## Risks / Pending (Slice 3 — Historical / Resolved or Superseded)

- **Historical manual visual verification** (task 3.4 second half): pixel comparison vs `perfil-usuario.png` was blocked by the structural conflict (Deviation 1); the canonical final state records task 3.5 as waived under Profile v2.
- **400-line budget exceeded** (~797 actual vs ~250 forecast) — maintainer must pick `size:exception` or the 3a/3b split (see Slice 3 PR Boundary) before PR creation.
- Switch unchecked track now uses `BrandTrack` and checked track teal for ALL `ProfileToggleRow`s — consistent with the handoff toggle spec; confirm no other screen consumes `ProfileToggleRow` expecting coral (only Preferencias uses it today).
- `streak > 0` visibility guard (Deviation 3) and 11sp caption (Deviation 10) are judgment calls flagged for design review.

## Files Changed (Slice 4)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardScreen.kt` | Modified | +127/-40 | Greeting row: `headlineSmall` (21/800) + " 👋" appended in screen layer, coral streak pill right (omitted at `streak == 0`, `homeStreakPill` tag, primary bg + `ic_flame` 14dp + "+N días" 12/700); subtitle "¡Es hora de practicar hoy!" replaces old copy, `schoolYearLabel` pill removed from UI; progress card → `MCard` default (18dp/1px border, surface) with "Nivel {level}" 14/700 + "{currentXp} / {xpForNextLevel} XP" 12/600 teal + teal 8dp bar; "MIS CURSOS EN PROGRESO" `labelMedium` muted, 1sp tracking; `CourseProgressCard` (44dp `secondaryContainer` circle + "÷" glyph, title, "Progreso: {N}%", teal "Ir" pill → `onOpenLessonMap`); catalog CTA + `ContinueLearningCard` + `JoinCourseCard` verbatim |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModel.kt` | Modified | +30/-3 | `activityCount` → `streak` (identical `min(completed, 7)` math); `currentXp`/`xpForNextLevel` mirror `ProfileViewModel` (`totalScore % XpPerLevel`, `XpPerLevel`); `HomeCourseProgress` + `inProgressCourses` from `courseRepository.getEnrolledCourses` (percent = completed∩lessons/lessons×100, 0-lesson guard), course-fetch failure degrades to `emptyList()` via `runCatching` |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/primitives/MProgressIndicator.kt` | Modified | +4/-2 | New `color: Color = primary` param on `MLinearProgressIndicator` (zero existing callers — additive, safe) |
| `composeApp/src/commonTest/kotlin/com/example/proyectofinal/ui/home/HomeDashboardViewModelTest.kt` | Modified | +88/-6 | `activityCount`→`streak` assertions renamed, same values (7, 0); +`currentXp`/`xpForNextLevel` asserts; 2 new tests: per-course completion percentages, course-fetch failure degradation; fake `getEnrolledCourses` backing |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/home/HomeDashboardRedesignRenderTest.kt` | New | +197 | 6 render tests: greeting/wave/pill; pill omitted at 0; Nivel/XP card + 68% bar via `ProgressBarRangeInfo`; zero → "0 / 100 XP" + empty bar; courses section + "Ir" pill + catalog CTA navigation; enrolled-no-progress keeps `ContinueLearningCard` |

Authored slice total: **497 changed lines** (300 modified Kotlin + 197 new tests) vs ~180 forecast and ~440–470 pre-approved estimate → **over the 400 budget; maintainer pre-approved `size:exception` for ONE PR (4a/4b split explicitly declined).** See Slice 4 PR Boundary.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`BrandTrack`, `secondary`, `secondaryContainer`, `onSecondary`, typography slots). `ic_flame` reused from Slice 3 assets (no new drawable). `MCard` default border/radius consumed unchanged.

## Test Evidence (Slice 4)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–3; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 1m 23s` — **27 suites, 123 tests, 0 failures, 0 errors, 0 skipped** (was 25 suites / 115 tests; +1 suite `HomeDashboardRedesignRenderTest` +6 tests, `HomeDashboardViewModelTest` 7→9 tests).
- `HomeDashboardRedesignRenderTest`: 6/6 green —
  - `greeting row renders wave subtitle and coral streak pill` ("Hola, María 👋", "¡Es hora de practicar hoy!", `homeStreakPill` tag, "+7 días")
  - `streak pill is omitted when the user has no streak` (Slice 3 handoff omission rule applied to home)
  - `progress card renders level XP text and a 68 percent bar` ("Nivel 5", "340 / 500 XP", `ProgressBarRangeInfo(340f/500f, 0f..1f, 0)` — spec mock literals passed straight to the content composable)
  - `zero progress renders nivel 0 and an empty bar` ("Nivel 0", "0 / 100 XP", bar 0f)
  - `courses section renders header course cards and ir pill opens the lesson map` ("MIS CURSOS EN PROGRESO", "Progreso: 45%", 2× "Ir" → `onOpenLessonMap` fired; "Abrir mapa de lecciones" → fired)
  - `enrolled dashboard without in-progress courses keeps the continue learning card` ("Ir al mapa" → `onContinueLearning`; secondary lesson-map CTA preserved)
- `HomeDashboardViewModelTest` 9/9 green — streak rename keeps identical math/values; no behavior assertion weakened.
- One RED→GREEN cycle this batch: first run failed compiling `HomeDashboardRedesignRenderTest` — `Unresolved reference 'onNode'`. In this Compose version `onNode`/`onAllNodes` are **members** of `SemanticsNodeInteractionsProvider` (no import exists); fixed test-side by dropping the bogus import (production code unchanged).
- **Runtime harness**: compose render tests exercise the real `HomeDashboardContent` + `AppTheme` (Sora) + Slice 3 vector assets on JVM; "Ir" pill, catalog CTA, and ContinueLearningCard callbacks all executed.
- Not semantics-observable, deferred to manual check: coral pill/teal bar colors, 44dp circle tint (`secondaryContainer` = teal@14%), "÷" glyph rendering in Sora, 1sp letter-spacing pixels.
- **Rollback boundary**: revert touches only `ui/home/{HomeDashboardScreen,HomeDashboardViewModel}.kt`, `ui/primitives/MProgressIndicator.kt` (the `color` param lines), `commonTest/.../HomeDashboardViewModelTest.kt`, `jvmTest/.../home/HomeDashboardRedesignRenderTest.kt` — no theme token, navigation, repository-contract, or persistence changes.

## Suggested PR / Commit Boundary (Slice 4)

- **Branch**: `feature/home-redesign` → targets `feature/profile-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 3 branch tip, ends with this slice — home dashboard visuals + VM state fields (streak rename, XP mirror, in-progress courses) + primitive `color` param + render/VM tests. No lesson-map/exercise screens, no navigation or repository-contract changes.
- **Suggested single work-unit commit**: `feat(ui): sync home dashboard to redesign handoff` — greeting/streak pill + Nivel/XP progress card + MIS CURSOS course cards + VM state + tests (tests with the behavior they verify).
- **400-line budget**: actual 497 vs forecast ~180 → **budget exceeded; maintainer pre-approved `size:exception` as ONE PR** (~440–470 estimated; actual +6% over the estimate, driven by the 197-line render test — the 6th test covers the enrolled-no-progress fallback the plan requires preserved). The 4a/4b split was explicitly declined. Production Kotlin is 206 lines, on-forecast; the remainder is verification.
- **Rollback boundary**: per-file revert of the Files Changed (Slice 4) list; Slices 1–3 remain valid without this slice.

## Deviations from Design (Slice 4)

1. **Spec-number mock literals are unreachable with `XpPerLevel = 100`** — "340/500 XP" at level 5 and "0/0 XP" cannot occur in real state (`currentXp ∈ [0,99]`, `xpForNextLevel = 100`). Implemented as formatted state values; the 68% bar scenario is honored by passing the mock literals straight to the content composable in the render test (`ProgressBarRangeInfo` semantics). Real zero progress renders "Nivel 0" + "0 / 100 XP" + empty bar (locked by test).
2. **`schoolYearLabel` pill removed from the home UI, VM field kept** — not present in `inicio-dashboard.png` (PNG-authoritative per resolved plan, Engram #69). The VM still computes it (existing VM test assertion untouched); removing the field would be a contract change beyond visual scope.
3. **Wave " 👋" appended in the screen layer only** — `greetingFor` untouched, preserving clock logic and the `endsWith(name)` behavior assertions. Spec scenario copy "Hola, María 👋" is mock copy; the time-based salutation requirement is unchanged.
4. **Streak pill omitted when `streak == 0`** — same resolution as Slice 3 Deviation 3 (scenario conditions on "user with streak"; handoff omits missing items); locked by test.
5. **All course cards use the static "÷" glyph** (Latin-1, Sora-safe) — the PNG shows "÷"/"×" per course but no per-course icon contract exists in spec/design; resolved plan picked the single glyph. Per-course deep-link from "Ir" is out of scope (no contract — pill navigates to the lesson map like the catalog CTA).
6. **"Ir"/streak pill text at 12sp/700 via `labelMedium` + `FontWeight.Bold` override** — the token slot is 12/600; the handoff weight (700) requires the override. No token edit (theme files frozen per slice boundary).

## Issues Found (Slice 4)

- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). New code is `commonMain`-only Compose + an `expect`-free VM; the only resource consumed (`ic_flame`) already shipped in Slice 3. Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 4 review closes.
- `HomeDashboardContent` branches on `inProgressCourses.isNotEmpty()`; an enrolled user whose courses all have 0% progress still sees the courses section (0% cards) rather than `ContinueLearningCard` — matches the resolved plan ("percent computed, no filter") and the PNG's 12% card, but flag for design review if 0% should fall back to the empty state.

## Risks / Pending (Slice 4 — Historical / Resolved or Superseded)

- **Historical manual visual verification** (task 4.4 second half): pixel comparison vs `inicio-dashboard.png` covered pill/bar colors, circle tint, glyph, and tracking; task 4.5 is accepted in the canonical final state.
- **`size:exception` accepted**: 497 changed lines in ONE PR (see Slice 4 PR Boundary). No further budget action needed; recorded for the archive ledger.
- `schoolYearLabel` is now dead UI state on home (Deviation 2) — a future contract cleanup could drop it from `HomeDashboardUiState`, but that is a behavior-adjacent change outside this visual slice.

## Files Changed (Slice 5)

| File | Action | Lines (+/-) | What |
|------|--------|-------------|------|
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapNode.kt` | Rewritten | +52/-155 (199 → 96 lines) | Card composable → 56dp circular node: Completed teal (`secondary`) + white `ic_check`; Current/Unlocked coral (`primary`) + white `ic_play`; Locked `BrandLock` + white `ic_lock`, non-clickable; `clickable(enabled = Unlocked \|\| Current)` preserves the VM gating contract; `testTag("lessonMapNode-{index}")`; `internal const LessonNodeSizeDp` shared with screen geometry |
| `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/activities/LessonMapScreen.kt` | Rewritten (map branch only) | +188/-91 (498 → 595) | Header: 38×38/12dp back box (`ic_arrow_left`, "Volver", → `onShowHome`) + title `headlineSmall` + "{N} Lecciones" `bodySmall` muted + coral "Ver teoría" Surface pill (`testTag("theoryPill")`, 12/700 white, `isTheoryAvailable` gating kept); `LessonMapProgress` (teal 8dp `MLinearProgressIndicator(color = secondary)` + derived "{P}% Completado" / "{C}/{N} Lecciones"); `LessonMapPath` (`BoxWithConstraints` + Canvas `testTag("lessonMapPath")`, 4dp round-cap `drawLine` segments, dash 8dp/6dp density-scaled, nodes via `absoluteOffset`); old header card, `ActiveExerciseCard`, `LessonMapConnector` out; exercise-player composables, loading/error branches, and `TheorySheet` wiring untouched |
| `composeApp/src/commonMain/composeResources/drawable/ic_check.xml` + `ic_play.xml` | New (2 files) | +26 | Slice 3 stroke style (#111827 / 1.8 / round caps+joins); `ic_lock` + `ic_arrow_left` reused from Slice 3 assets |
| `composeApp/src/jvmTest/kotlin/com/example/proyectofinal/ui/activities/LessonMapRedesignRenderTest.kt` | New | +193 | 5 render tests (see Test Evidence) |

Authored slice total: **~705 changed lines** (240 insertions + 246 deletions across the 2 Kotlin files, +26 vector assets, +193 tests) vs the ~775 approved `size:exception` envelope → within envelope.

Theme files (`ui/theme/*`): untouched — consumed Slice 1 tokens only (`secondary`, `primary`, `BrandLock`, `BrandTrack` via the primitive, `pill` shape, typography slots) and Slice 4's `color` param on `MLinearProgressIndicator`. No new Gradle dependency.

## Test Evidence (Slice 5)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slices 1–4; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 4m 5s` — **27 suites, 128 tests, 0 failures, 0 errors, 0 skipped** (Slice 4 reported 27 suites / 123 tests; +1 suite `LessonMapRedesignRenderTest`, +5 tests — test totals reconcile exactly: 123 + 5 = 128; suite count by the XML-counting method implies Slice 4's actual was 26).
- `LessonMapRedesignRenderTest`: 5/5 green —
  - `header renders title lesson count theory pill and back arrow` ("Fundamentos" + "4 Lecciones"; "Ver teoría" click → `onOpenTheory` fired; "Volver" click → `onShowHome` fired — rulings 2 & 4)
  - `progress bar derives percent and counts from node states` (3/8 → "37% Completado" + "3/8 Lecciones" + `ProgressBarRangeInfo(3f/8f, 0f..1f, 0)` — ruling 1)
  - `node states render and locked and completed nodes are non interactive` (Completed/Current/Locked tags exist; locked click → no selection; completed click → no selection — ruling 3)
  - `tapping an unlocked node selects its exercise` (node 3 click → `onExerciseSelected("ex-3")`)
  - `canvas path renders with height derived from node count` (`lessonMapPath` exists; height = 2 × 120dp)
- `LessonMapViewModelTest`: 7/7 untouched and green — no ViewModel contract change.
- One RED→GREEN cycle this batch: first run failed compiling `LessonMapScreen.kt` — `absoluteOffset` imported from `androidx.compose.ui.layout`; correct package is `androidx.compose.foundation.layout`. Fixed import only. Verbatim: `e: .../LessonMapScreen.kt:38:35 Unresolved reference 'absoluteOffset'`.
- **Runtime harness**: compose render tests exercise the real `LessonMapContent` + `AppTheme` (Sora) + composeResources vectors (`ic_check`/`ic_play` new; `ic_lock`/`ic_arrow_left` reused) on JVM; pill, back, and node tap callbacks all executed.
- Not semantics-observable, deferred to manual check: teal/coral/`BrandLock` node + segment colors, dash pattern and 4dp round caps, serpentine 72dp x positions, icon glyph fidelity vs handoff.
- **Rollback boundary**: revert touches only `ui/activities/{LessonMapNode,LessonMapScreen}.kt`, `composeResources/drawable/{ic_check,ic_play}.xml`, `jvmTest/.../activities/LessonMapRedesignRenderTest.kt` — no theme token, navigation, ViewModel, or contract changes; the exercise-player composables inside `LessonMapScreen.kt` are unchanged from Slice 4.

## Suggested PR / Commit Boundary (Slice 5)

- **Branch**: `feature/lesson-map-rewrite` → targets `feature/home-redesign` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 4 branch tip, ends with this slice — lesson map header + theory pill + progress block + Canvas serpentine path + circular node rewrite + 2 vector assets + render tests. No exercise-player restyle (Slice 6), no navigation, ViewModel, or contract changes.
- **Suggested single work-unit commit**: `feat(ui): rewrite lesson map as serpentine path per redesign handoff` — header/theory pill + progress + Canvas path + node rewrite + icons + render tests (tests with the behavior they verify).
- **400-line budget**: actual ~705 vs ~350 forecast → **budget exceeded; maintainer approved `size:exception` for ONE PR** (~775 approved envelope, Budget Gate option (a); the pre-approved 5a/5b split was declined). ~35% of the diff is wholesale-replacement deletions (246 lines shed by the two target files) plus self-contained vector assets and verification.
- **Rollback boundary**: per-file revert of the Files Changed (Slice 5) list; Slices 1–4 remain valid without this slice.

## Deviations from Design (Slice 5)

1. **Ruling 1 — progress percent derived from state**: `completed*100/total` (3/8 → "37% Completado", bar `3f/8f`); the PNG's "45%" is mock math. Spec scenario "teal bar at 45%" flagged as spec deviation; locked by test.
2. **Ruling 2 — subtitle is "{N} Lecciones" only**: `Lesson` has no `unit` field, so "Unidad 2 ·" would require a contract change (out of visual scope). The "Unidad" concept is flagged for the future learning-paths slice.
3. **Ruling 3 — node tap gating preserved**: nodes are clickable only when Unlocked/Current, mirroring `selectExercise`'s gate (`LessonMapViewModel.kt:64`). The spec scenario "tapping completed node opens exercise" is NOT implemented — flagged for a future contract change; locked by test.
4. **Ruling 4 — back arrow preserves the existing `onShowHome` callback** (spec says "course catalog"): navigation contract unchanged; spec scenario flagged.
5. **Ruling 5 — dash rule per PNG**: segment i→i+1 is dashed `BrandLock` when `nodes[i+1].state == Locked`; solid teal (`secondary`) when `nodes[i]` is Completed; solid coral (`primary`) otherwise. Overrides design.md's "if node i is Locked" wording.
6. **Serpentine parity read on the node's 1-based `index`** so the FIRST node sits on the RIGHT as in the authoritative PNG — design.md's literal 0-based reading ("even-index → left", `y = i*120+60`) would start the path on the left and fail the pixel check. Design's "even → left, odd → right" wording holds against the 1-based index; matches PNG nodes 1–4 (the PNG's 5th node sits left, likely mock imprecision). Flag for the manual pixel check.
7. **Unlocked renders identically to Current** (coral + play icon) — the PNG defines no separate unlocked look; the next actionable node is the coral one. Both states were already tappable, so behavior is unchanged.
8. **Fixed header + scrollable path** (header/progress live outside the `verticalScroll`) per the PNG; the old implementation scrolled the entire column. Interaction contracts unaffected.
9. **"Ver teoría" is a compact Surface pill, not `MButton`** — MButton's 56dp min-height + 12dp coral shadow contradict the handoff's compact pill; the Slice 3/4 chip pattern (12/700 white on coral, `pill` radius) is reused. `isTheoryAvailable` gating preserved (alpha 0.5 + disabled when unavailable).
10. **Map branch copy switched to Spanish** per the PNG ("Ver teoría", "% Completado", "Lecciones", "Volver"), consistent with Slices 2–4 Spanish redesign copy. Loading/error branches and the exercise player keep their pre-existing English copy — outside this slice's scope (player restyle is Slice 6).
11. **`ActiveExerciseCard` removed** per the approved Budget Gate plan — the PNG has no current-exercise card; the active exercise is already the coral node. `uiState.activeNode` remains consumed by the player header ("Exercise N").

## Issues Found (Slice 5)

- iOS/Android compilation not verified locally (Linux env runs `jvmTest` only). New code is `commonMain`-only Compose + composeResources vectors (same delivery mechanism as Slice 3's 17 icons). Recommend CI or a local `:composeApp:assembleDebug` + iOS compile before PR 5 review closes.
- `BoxWithConstraints` subcomposition + Canvas redraw per scroll frame is fine for realistic lesson sizes (< 20 exercises); a path with hundreds of nodes would want virtualization — not a real scenario today.
- Spec scenarios deliberately NOT implemented per maintainer rulings (all flagged for archive-time spec amendments): "tapping completed node opens exercise" (ruling 3), "navigates to course catalog" (ruling 4), "Unidad 2 ·" subtitle (ruling 2), "45%" mock percent (ruling 1).

## Risks / Pending (Slice 5 — Historical / Resolved or Superseded)

- **Historical manual visual verification** (task 5.4 second half): pixel comparison vs `mapa-leccion.png` covered serpentine positions, dash density, node colors, and pill shape; task 5.6 is accepted in the canonical final state.
- **`size:exception` accepted**: ~705 changed lines in ONE PR (approved envelope ~775). No further budget action needed; recorded for the archive ledger.
- Parity call (Deviation 6) is the one geometry judgment not covered by an explicit ruling — if the maintainer prefers design.md's literal 0-based reading, flipping is a one-line change (`node.index % 2` → position parity); the Canvas and node placement share the same helper, so they cannot drift apart.

## Test Evidence (Historical Slice 1)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain`
- **Result**: `BUILD SUCCESSFUL in 2m 57s` — **23 suites, 107 tests, 0 failures, 0 errors, 0 skipped** (was 106; +1 new `semanticFoundationColorsMatchRedesign`).
- `AppThemeTokensTest`: 4/4 green — `lightColorSchemeMatchesBrandedFoundationPalette`, `semanticFoundationColorsMatchRedesign`, `shapeTokensExposeReviewableFoundationValues`, `typographyMatchesSoraScaleWithInjectedFamily`.
- Existing compose-rule render tests (`OnboardingScreenTest`, `ProfileScreenTest`, etc.) exercise `AppTheme` → Sora `Font()` resource loading path on JVM — all green, proving the font wiring resolves at runtime.
- **Runtime harness**: `N/A` for Slice 1 in-app navigation — visual-only tokens; runtime boundary exercised indirectly via the 107-test jvmTest suite (compose render tests). Historical manual device check was pending (see historical Risks).
- **SDK workaround**: `sdk.dir` patched to `/mnt/c/Users/Nahuel/AppData/Local/Android/Sdk` before the run, restored after (verified 0 occurrences remain).

## Test Evidence (Slice 2)

- **Focused test command**: `./gradlew :composeApp:jvmTest --console=plain` (same SDK workaround as Slice 1; `local.properties` restored, 0 occurrences verified).
- **Result**: `BUILD SUCCESSFUL in 1m 6s` — **24 suites, 110 tests, 0 failures, 0 errors, 0 skipped** (was 23 suites / 107 tests; +1 suite `AuthRedesignRenderTest`, +3 tests).
- `AuthRedesignRenderTest`: 3/3 green —
  - `login renders redesign copy and footer link navigates to register` (handoff copy present; "Registrate" click → `onSwitchToRegister` fired — behavior preserved)
  - `register step label uses handoff copy and follows the wizard step` ("Paso 1 / 3" → drive VM → "Paso 2 / 3")
  - `terms checkbox is 22 by 22 dp and toggles acceptance from the row` (`assertWidthIsEqualTo(22.dp)`/`assertHeightIsEqualTo(22.dp)` on the tagged box; `Role.Checkbox` node `assertIsOff` → `performClick` → `assertIsOn` + `acceptedTerms == true`)
- One RED→GREEN cycle during the batch: first run failed on `onNodeWithTag("termsCheckboxBox")` — the `toggleable` row merges descendants, so the tag only exists in the unmerged tree. Fixed with `useUnmergedTree = true` (test-side fix; production code unchanged). Verbatim failure: `Expected exactly '1' node but could not find any node that satisfies: (TestTag = 'termsCheckboxBox'). However, the unmerged tree contains '1' node that matches.`
- Existing suites untouched and still green: `LoginViewModelTest`, `RegisterViewModelTest`, `AuthGateViewModelTest`, `AppThemeTokensTest`, `OnboardingScreenTest`, `ProfileScreenTest` — no behavior assertion was weakened or edited.
- **Runtime harness**: compose render tests exercise the real `LoginScreen`/`RegisterScreen` + ViewModel + `AppTheme` (Sora loading) path on JVM. Historical manual pixel comparison vs Jul 16 `.dc.html` was pending (see historical Risks).
- Not semantics-observable, deferred to manual check: coral shadow blur on CTA, teal segment color, Sora/mono glyph rendering, 14dp social radius pixels.
- **Rollback boundary**: revert touches only `ui/LoginScreen.kt`, `ui/RegisterScreen.kt`, `ui/AuthScreenScaffold.kt`, `ui/primitives/MButton.kt` (Social branch), `src/jvmTest/.../AuthRedesignRenderTest.kt` — no theme token, navigation, ViewModel, or contract changes.

## Suggested PR / Commit Boundary

- **Branch**: `foundation/ui-redesign-tokens` → targets `main` (first in the stacked chain; slices 2–6 branch off it per design Migration section).
- **Boundary**: starts at `main` tip, ends with this slice — tokens, primitives, font resources, updated token tests. No feature-screen restyle, no dark mode.
- **Suggested single work-unit commit**: `feat(ui): add Sora typography and semantic foundation tokens for redesign` — includes resources + tokens + primitives + tests (tests stay with the behavior they verify).
- **Rollback boundary**: revert touches only `ui/theme/*`, `ui/primitives/{MButton,MProgressIndicator}.kt`, `ui/theme/AppThemeTokensTest.kt`, `composeResources/{font,files}/` — no screen, navigation, contract, or persistence changes.

## Suggested PR / Commit Boundary (Slice 2)

- **Branch**: `feature/auth-redesign` → targets `foundation/ui-redesign-tokens` (stacked-to-main, per design Migration order).
- **Boundary**: starts at Slice 1 branch tip, ends with this slice — auth screens + Social button variant + render tests. No profile/home/map/exercise screens, no dark mode, no ViewModel or navigation changes.
- **Suggested single work-unit commit**: `feat(ui): sync auth screens to Jul 16 redesign handoff` — Login/Register/scaffold visuals + `MButton.Social` radius + `AuthRedesignRenderTest` (tests with the behavior they verify).
- **Rollback boundary**: per-file revert of the 5 files listed in Files Changed (Slice 2); Slice 1 tokens remain valid without this slice.
- **Drift identified vs Jul 12 implementation** (for reviewer focus): social radius 16→14dp, divider 12/500→12/600, footer 13sp+underline→14sp no-underline, forgot link 12→13sp/600, step segments 6dp/3dp→5dp/999dp, step label copy+mono, strength coral→teal, M3 checkbox→22×22/7dp coral, logo card 18→16dp, wordmark flat-17→two-tone 22/800.

## Deviations from Design

1. **`AppTypography` became a `@Composable` getter** — composeResources `Font()` is composable, so a top-level non-composable `val` is impossible. Added pure `buildAppTypography(fontFamily)` to keep the scale unit-testable; `AppTheme.kt` usage unchanged in shape. Only callers were `AppTheme.kt` and the token test — both updated.
2. **Line heights not specified in design** — picked ~1.25–1.45× per slot (headLg 32/40, headMd 27/34, headSm 21/27, titleLg 17/23, titleMd 14/20, bodyLg 15/22, bodyMd 13/18, bodySm 12/16, labelMd 12/16). Flag for design review.
3. **Unspecified Typography slots** (display*, titleSmall, labelLarge, labelSmall) keep prior sizes/weights, family switched to Sora — design only specified 9 slots.
4. **`MCard.kt` edited 0 lines** (design predicted +1) — 18dp arrives transitively via `shapes.large`; behavior identical, no magic numbers introduced.
5. **Font fallback**: design pseudo-code `FontFamily(listOf(..., FontFamily.SansSerif))` is not valid API (a `FontFamily` is not a `Font`). Bundled resources cannot go missing at runtime, so the "Nunito fallback" scenario is satisfied by packaging; unmapped weights (e.g. Medium 500, no 500 ttf bundled) resolve to the nearest bundled Sora weight via Compose font resolution — no crash path.
6. **Naming**: `BrandTrack`/`BrandLock`/`BrandStripe`/`BrandCoralShadow` follow the file's existing `Brand*` convention (design text said `Track`/`Lock`/`Stripe`).
7. **Historical Slice 1 observation (superseded; not a current-worktree claim)**: an earlier snapshot recorded unrelated `docs/ui/screens/*.png`, `openspec/backlog.md`, and a deleted `scripts/configure-android-wsl-portproxy.ps1`; this does not assert that any of those paths are currently changed.

## Deviations from Design (Slice 2)

1. **Form title kept at 27/800, NOT 32/800** — design.md ("formTitle 32/800 Sora") and tasks.md 2.3 ("title 32/800") predate the **Jul 16 handoff**, which specifies 27px/800 for "Hola de nuevo" and "Creá tu cuenta" (README: "Título de pantalla: 27px / 800… login y 'Creá tu cuenta'"). Current code already renders 27/800 via `headlineMedium` + `ExtraBold`. Kept the Jul 16 value; **flag for design review** — if 32/800 was intentional, that's a regression against the handoff and needs a design amendment.
2. **Logo mark card radius 16dp, NOT 18dp** — tasks.md 2.3 says "logo 18dp", but the spec scenario ("52×52px, 16px radius"), design.md ("Brand logo box 16dp radius"), and the Jul 16 handoff (`border-radius:16px`) all say 16. Delivered 16dp via the existing `button` shape token (no new token — theme files frozen per slice boundary). tasks.md shorthand treated as stale.
3. **JetBrains Mono delivered as `FontFamily.Monospace`** — the font is not bundled (only Sora is), and adding a `.ttf` + accessor would cross the "do not edit theme files" slice boundary (new composeResources asset + theme-area helper). `FontFamily.Monospace` preserves the mono aesthetic at 12/600. **Follow-up candidate**: bundle `jetbrains_mono` (OFL) in a foundation PR if pixel-perfect matters.
4. **Wordmark two-tone 22/800 implemented** — beyond the spec scenario minimum (spec's brand-hero parenthetical only mandates logo/52×52/16px), but it is explicit in the Jul 16 handoff brand block (22px/800, "Mathim" ink + "App" muted/500, −0.02em). Delivered as a single `AnnotatedString` node so any `onNodeWithText("MathimApp")` lookup keeps working.
5. **Checkbox is a custom composable, not M3 `Checkbox`** — spec scenario mandates 22×22px/7px/coral, which M3 Checkbox cannot express. Toggle behavior consolidated into a single `Modifier.toggleable(role = Checkbox)` on the row (previously two handlers: row `clickable` + Checkbox `onCheckedChange`; net behavior identical — one toggle per tap, same `onAcceptedTermsChange` contract).
6. **`MButton.Social` border 1→1.5dp** — handoff shows `1.5px solid line` on social buttons; radius (14dp) is the spec-mandated part, border width is handoff fidelity. Filled/Outline variants untouched.
7. **Password-strength label kept muted, not teal** — spec scenario mandates teal for the meter *fill* only ("meter fills with teal"). Label copy/logic preserved exactly (behavior); the handoff's teal "Buena" label is per-state styling not required by the spec.
8. **Not implemented (out of spec scope, noted for transparency)**: register header back-arrow + one-question-per-screen restructure (spec says "Data fields unchanged"; step→field mapping preserved), "Continuar" arrow glyph on CTA (not in spec scenarios), footer bottom-anchoring (structural layout, not mandated).

## Issues Found

- Prior apply batch left the font assets but no code/test/progress changes — recovered in this batch (this is the corrective run).
- `MLinearProgressIndicator` had zero callers before this change; it is the building block for slices 2–6 (header bars).
- Slice 2: design.md/tasks.md carry **stale pre-Jul-16 values** (title 32/800, logo 18dp) that conflict with the Jul 16 handoff and the spec delta — resolved in favor of handoff+spec (see Slice 2 Deviations 1–2). Recommend amending design.md at archive time.
- Slice 2: register "Back" outline button uses English copy ("Back") on a Spanish screen — pre-existing, out of scope, noted for a future copy pass.

## Risks / Pending (Historical / Resolved or Superseded)

- **Manual visual verification pending** (task 1.8 second half): Sora rendering + CTA coral shadow on device/emulator — cannot be done headless. Required before PR 1 review closes.
- **Manual visual verification pending** (task 2.3 second half): pixel comparison vs Jul 16 `.dc.html` — shadow blur, teal strength fill, mono step label, 22×22 checkbox, two-tone wordmark. Required before PR 2 review closes.
- Weight 500 (Medium) has no dedicated Sora ttf — resolves to nearest weight; visually indistinguishable at 11–12sp but flag if pixel-perfect matters.
- Line-height picks (deviation 2) are the only unconfirmed token values.
- Slice 2: `FontFamily.Monospace` renders a system mono, not JetBrains Mono glyphs — visible in side-by-side pixel comparison if inspected closely (see Slice 2 Deviation 3).
- Slice 2: button text remains `labelLarge` (14/600) while the handoff shows 16/700 button text — that is a theme-level slot change (Slice 1 territory, affects every button) and was intentionally not done in a feature slice. Flag for design review.

## Superseded Snapshot: Slice 5 Budget Gate Before Implementation

**Historical resolution**: maintainer approved `size:exception` for ONE PR 5 (~775-line envelope; actual ~705), and Slice 5 implementation plus automated tests were complete at this checkpoint. The pre-approved 5a/5b split (option b) was declined. The 5 spec/design conflicts below were ruled on by the maintainer and are recorded as binding in Slice 5 Deviations 1–5. The then-remaining Slice 5 requirement was the manual `mapa-leccion.png` visual check; task 5.6 is now accepted.

> **Historical snapshot, superseded by the current status above:** At the budget-gate checkpoint, the apply batch had stopped before writing code because the estimate exceeded 400 lines. At that time no Slice 5 files had been created or modified and tasks 5.1–5.4 were unchecked. This statement is retained only as decision and budget evidence; it does not describe the recovered worktree's current state.

### Disciplined estimate (additions + deletions, per work-unit-commits counting rule)

| Work item | + | − | Diff lines |
|---|---|---|---|
| `ui/activities/LessonMapNode.kt` — wholesale rewrite (199-line card composable → ~100-line circular node) | ~100 | ~180 | ~280 |
| `ui/activities/LessonMapScreen.kt` — map branch rewrite (header/progress/path composables in; old header card, `ActiveExerciseCard`, `LessonMapConnector` out; exercise-player code untouched) | ~198 | ~131 | ~329 |
| `composeResources/drawable/ic_check.xml` + `ic_play.xml` (new vectors, Slice 3 stroke style) | ~26 | 0 | ~26 |
| `jvmTest/.../activities/LessonMapRedesignRenderTest.kt` (5 tests: header/theory/back, progress bar, node states + locked gating, tap wiring, canvas) | ~140 | 0 | ~140 |
| **Total** | | | **~775** |

~775 vs 400 budget (~2×) and vs ~350 forecast (~2.2×). Deletions (~310) are irreducible: the slice is the approved structural rewrite and both target files must shed the card-list implementation. No honest trim (fewer tests, partial node rewrite, dead code) brings ONE PR under 400.

### Options for the maintainer (pick one)

- **(a) `size:exception` — ONE PR 5 (~775 lines).** Same resolution as Slice 3 (~797) and Slice 4 (497); ~40% of the diff is wholesale-replacement deletions + self-contained vector assets. Branch `feature/lesson-map-rewrite` → `feature/home-redesign`.
- **(b) Pre-approved 2-way split.** 5a geometry/layout (~255–300): screen-only — scrollable Box + Canvas polyline + serpentine offsets (120dp step) + tap wiring with a temporary minimal private node circle (old `LessonMapNode.kt` untouched); tasks 5.3, 5.4 + placement half of 5.2. 5b node polish/states (~480–520, targets 5a branch): full node rewrite (56dp, state icons, locked non-clickable) + `ic_check`/`ic_play` + header/progress/theory pill + state tests; task 5.1 + 5.2 remainder. **Warning**: 5b still exceeds 400 (node-file deletions + assets + honest tests cannot subdivide further).
- **(c) 3-way split** (geometry ~260 / header+progress ~210 / node+icons+tests ~420): all PRs ≤ ~420, higher process cost.

### Analysis completed (implementation resumes immediately on decision)

- PNG delta (`mapa-leccion.png` vs `old/`): structure unchanged (serpentine path, back header, theory pill, progress block, dashed locked segments); palette/typography migrate to light theme (teal completed, coral current, `BrandLock` gray locked, Sora).
- Geometry per design: 120dp vertical step, node centers y = i*120+60dp, even-index x = 72dp from left / odd-index 72dp from right; Canvas polyline 4dp round-cap; dash effect `dashPathEffect` density-scaled from design's 8f/6f (design chose Canvas for resolution independence — raw px contradicts it).
- Tokens consumed (no theme edits): `secondary` (teal), `primary` (coral), `BrandLock`, `BrandTrack`, shapes `pill`/`button`; `MLinearProgressIndicator(color = secondary)`; vectors `ic_arrow_left`/`ic_lock` reused.
- Bottom-nav spec scenario needs no screen work — host `AuthenticatedHomeScaffold` already renders 4 tabs with Actividades selected.

### Spec/design conflicts to rule on WITH the workload call

1. **"45% Completado" at 3/8 is mock math** (3/8 = 37.5%) — derive percent from state (`completed*100/total`); same precedent as Slice 4 Deviation 1.
2. **"Unidad 2 · 8 Lecciones"** — `Lesson` model has NO unit field; subtitle can only be `"{N} Lecciones"` without a contract change (out of visual scope).
3. **Spec scenario "tapping completed node opens exercise" conflicts with the VM contract** — `selectExercise` gates to Unlocked/Current (`LessonMapViewModel.kt:64`); the slice mandate ("preserve ALL behavior… visual/structural, not functional") keeps gating as-is; flag scenario for a future contract change.
4. **Back arrow → "course catalog" per spec vs existing `onShowHome` callback** — preserve `onShowHome` (navigation contract unchanged); flag.
5. **Dashed-segment rule** — PNG (authoritative) dashes the segment INTO a locked node (current→locked dashed in both PNGs); design.md says "if node i is Locked". PNG wins: dashed when `nodes[i+1].state == Locked`; solid teal when `nodes[i]` Completed, solid coral otherwise.

## Remaining Tasks

- [x] Slice 1 implementation and automated tests (PR 1): foundation/tokens
- [x] Slice 1 manual visual check: accepted
- [x] Slice 2 implementation and automated tests (PR 2): auth screens
- [x] Slice 2 manual visual check: accepted
- [x] Slice 3 implementation and automated tests (PR 3): profile
- [x] Slice 3 manual visual check: waived; Profile v2 authoritative
- [x] Slice 4 implementation and automated tests (PR 4): home
- [x] Slice 4 manual visual check: accepted
- [x] Slice 5 implementation and automated tests (PR 5): lesson map only
- [x] Slice 5 manual visual check: accepted
- [x] Slice 6.1–6.3 automated exercise-player corrective work: header/lives/progress, question card/grid/glow, hint/CTA
- [x] Slice 6.4: `TheorySheet.kt` sections; `OnboardingScreen.kt` SelectionCard/buttons
- [x] Slice 6.5: `PlaceholderScreen.kt` empty/loading states
- [x] Slice 6.6: broad automated exercise/player/state tests
- [x] Slice 6.7: manual visual checks accepted

## Slice 6a Corrective Apply — Exercise Header and Hint Contract

### Completed Tasks

- [x] **6.1** Header now renders the lesson title, `current/total` question counter, state-derived progress, and the remaining lives; incorrect attempts decrement lives without going below zero.
- [x] **6.2** Question-card/grid/glow work was already complete in the candidate and remains unchanged by this corrective unit.
- [x] **6.3** `Pista` is a clickable link that invokes the injected hint callback (the production callback surfaces informational feedback); the drafting CTA renders `Confirmar` while existing submitting/retry labels and submission behavior remain intact.

### TDD Cycle Evidence

| Task | Test file / layer | Safety net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|
| 6.1 | `ExercisePlayerRedesignRenderTest` (Compose render), `LessonMapViewModelTest` (unit) | `*ExercisePlayerRedesignRenderTest` green before the corrective cycle | **Contemporaneous corrective RED**: focused compilation failed because `remainingLives` and `onHintRequested` did not exist (this is distinct from reconstructed historical Slice 6 styling work) | Focused 11 tests green after state/ViewModel/header implementation | 1/4 with 2 hearts and 3/4 with 1 heart; wrong-answer ViewModel assertion verifies 3→2 | Extracted local question number/count values; focused tests stayed green |
| 6.3 | `ExercisePlayerRedesignRenderTest` (Compose render) | Same focused safety net | **Contemporaneous corrective RED**: missing `onHintRequested` parameter prevented compilation | Focused 11 tests green after clickable `exerciseHint`, production callback, and `Confirmar` resource update | Hint callback fires without submitting; confirm then submits exactly once | Removed obsolete header resource imports; focused tests stayed green |

### Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused exact result | `bash ./gradlew :composeApp:jvmTest --console=plain --tests '*ExercisePlayerRedesignRenderTest' --tests '*LessonMapViewModelTest'` → **BUILD SUCCESSFUL**, 11 tests, 0 failures/errors (post-refactor) |
| Runtime harness result | Same JVM Compose render harness exercised the real `LessonMapContent` header, progress semantics, hearts, clickable hint callback, CTA, plus the real `LessonMapViewModel` wrong-answer transition → **BUILD SUCCESSFUL** |
| Historical full required result | `bash ./gradlew :composeApp:jvmTest --console=plain` → **BUILD SUCCESSFUL**, 29 suites / 147 tests, 0 failures/errors/skips |
| Historical rollback boundary | Revert only the Slice 6a hunks in `LessonMapScreen.kt`, `LessonMapUiState.kt`, `LessonMapViewModel.kt`, exercise resources, and the two exercise tests; existing Slice 6.2 grid/card changes and then-pending 6.4–6.7 remained isolated |

### Corrective Budget and Scope

- Corrective delta from the received candidate: **approximately 145 changed lines**, within the 200-line correction cap; the larger pre-existing uncommitted Slice 6 candidate is excluded from this bound.
- Historical Slice 6a state: no commit, push, or native review was started; manual visual acceptance was pending.

## Slice 6b Apply — Supporting Surfaces and Render Coverage

### Completed Tasks

- [x] **6.4** `TheorySheet` now separates the lesson title from a bordered surface content section. Onboarding selection cards expose their real click target and retain the existing 18dp theme-card shape and 1dp border; Continue/Complete retain the existing 16dp, coral-shadow `MButton` primitive, while Back is the required reachable 38dp square with a 12dp surface-variant treatment.
- [x] **6.5** `PlaceholderScreen` now provides a branded empty-state surface and an opt-in loading state through `PlaceholderState.Loading`, using the existing progress primitive without changing current callers' empty-state behavior.
- [x] **6.6** Added Compose render coverage for theory content structure, interactive onboarding selection cards and compact Back control, and both empty/loading placeholder variants.

### TDD Cycle Evidence

| Task | Test file / layer | Safety net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|
| 6.4 | `SupportingSurfacesRedesignRenderTest` (JVM Compose render) | N/A — no pre-existing supporting-surfaces test suite | Focused command failed with 3 behavioral failures: the theory title/content and onboarding selection/back tags were absent | 3/3 focused render tests passed after adding structured theory surface and onboarding semantics/control | Selection tap invokes its callback; a later onboarding step asserts the 38dp Back control | Removed obsolete full-width Back-button imports; focused tests stayed green |
| 6.5 | `SupportingSurfacesRedesignRenderTest` (JVM Compose render) | N/A — new loading-state API | Focused compilation failed because `PlaceholderState` and its `state` parameter did not exist | Loading illustration/progress test passed after the minimal state API and surface implementation | Empty-state case asserts the supplied copy and no loading indicator; loading case asserts the progress indicator | Kept the API defaulted to `Empty`, preserving every existing caller |
| 6.6 | `SupportingSurfacesRedesignRenderTest` (JVM Compose render) | Focused 4/4 green before the final empty-state case | Acceptance coverage was written before the corresponding production behavior | 5/5 focused tests passed | 5 behavior cases across theory, onboarding, loading, and empty-state branches | Imports and test assertions cleaned; focused tests remained green |

### Work Unit Evidence

| Evidence | Result |
|---|---|
| Initial fixture RED | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --tests '*SupportingSurfacesRedesignRenderTest'` → **BUILD FAILED** (exit 1): the inherited RED fixture used the obsolete `Lesson.teacherId` name. Corrected to the current `creatorId` before evaluating behavior; no production code changed. |
| Behavioral RED | Same focused command → **BUILD FAILED** (exit 1), 3 tests / 3 failures: absent theory section, selection-card, and compact-back semantics. |
| Loading-state RED | Same focused command → **BUILD FAILED** (exit 1): `PlaceholderState` and the `state` parameter were unresolved. |
| Focused GREEN | Same focused command → **BUILD SUCCESSFUL** (exit 0), 5 tests, 0 failures/errors/skips. |
| Full required result | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon` → **BUILD SUCCESSFUL** (exit 0), 30 suites / 152 tests, 0 failures/errors/skips. |
| Runtime harness | The focused JVM Compose harness rendered the real `TheorySheet`, `OnboardingContent`, and `PlaceholderScreen`; it exercised selection callback dispatch, 38dp Back geometry, title/content separation, loading progress, and empty-state copy → **BUILD SUCCESSFUL**. |
| Rollback boundary | Revert only `TheorySheet.kt`, `OnboardingScreen.kt`, `PlaceholderScreen.kt`, and `SupportingSurfacesRedesignRenderTest.kt` plus the three PR 6 task/progress entries; recovered Slice 6a exercise work and all unrelated work remain isolated. |

### Scope, Budget, and Pending Work

- Historical Slice 6b status: corrected the stale PR 6 `NOT STARTED` heading to state that implementation was complete and manual visual verification was pending; the canonical final state records the acceptance/waiver outcomes.
- This batch added **104 authored code/test diff lines** from its received reset baseline (`+92/-12`), for **193/400 cumulative** objective lines including the supplied 89-line prior total; OpenSpec progress/task evidence is excluded from the code budget.
- No commit, push, staging, native review, archive, or task 6.7 work was performed.
- Historical Slice 6b state: the then-remaining manual checks were 6.7, 1.9, 2.4, 3.5, 4.5, and 5.6; all are resolved in the canonical final state (3.5 waived under Profile v2).

## Slice 6c Visual Capture Validation

### Scope Confirmation

- Verified the current production corrections without modifying production: structured `TheorySheetContent`, branded Empty/Loading `PlaceholderScreen`, real `LessonMapContent` loading integration, and `homeBottomNavigationVisible` hiding navigation while the exercise player is active.
- Prior Strict-TDD evidence remains authoritative: missing `homeBottomNavigationVisible` compilation RED, intermediate 8-test/2-failure evidence, then the supplied focused GREEN. This validation did not reopen production scope.

### Capture and Test Evidence

| Evidence | Result |
|---|---|
| Deterministic capture test | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --tests '*Slice6VisualAcceptanceCaptureTest'` → **BUILD SUCCESSFUL** (exit 0), 1 test, 0 failures/errors; generated five density-1 RGBA PNGs, each 300×624. |
| Capture paths | `composeApp/build/visual-acceptance/current/task-6.7-{exercise,theory,onboarding,empty,loading}.png` |
| Full JVM suite | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon` → **BUILD SUCCESSFUL** (exit 0), 31 suites / 156 tests, 0 failures/errors/skips. |
| Runtime harness | The capture test rendered the real exercise player, theory content, onboarding, and empty/loading states at the requested fixed viewport. |
| Rollback boundary | Revert only `Slice6VisualAcceptanceCaptureTest.kt` and this Slice 6c progress entry; generated images are build outputs, and no production correction was changed. |

### Budget and Pending Acceptance

- This validation adds 104 changed lines (80-line capture test, 22-line evidence entry, and a 2-line status replacement), for 309/400 cumulative against the supplied 205/400 starting total.
- Historical Slice 6c state: task 6.7 was intentionally unchecked because the generated captures were evidence for the parent visual judgment, not a substitute for it.
- No stage, commit, push, review, archive, runtime-ledger edit, or production edit occurred in this validation.

## Slice 6d Manual Visual Acceptance

- **Compared** regenerated opaque 300×624 captures with canonical `docs/ui/screens` references.
- **Exercise — PASS**: immersive player hierarchy, title/question, hearts/progress, unique options, hint, and CTA match the required structure. Fixture copy and option count differ from the reference, but the component supports type-specific content.
- **Theory — PASS**: concept, numbered steps, example, and navigation hierarchy match while preserving the sheet architecture.
- **Loading — PASS**: exercise skeleton structure matches. **Empty — PASS**: scoped branded activity-empty card matches; this is not a full dashboard host capture.
- **Onboarding — PASS**: approved spec/design tokens and hierarchy match. No valid canonical wizard PNG exists; `splash-bienvenida` is explicitly out of scope.
- **Historical supporting evidence**: 14 focused GREEN tests; full JVM suite 31 suites / 157 tests; five opaque 300×624 captures.
- Historical acceptance record: task 6.7 was accepted. Earlier manual tasks were subsequently resolved in the canonical final state.

## Slice 6d Corrective Apply — Visual-Fidelity Defects

### Scope and Outcome

- Historical Slice 6d state: task **6.7 was unchecked**. This corrective batch addressed only defects observed in the deterministic 300×624 captures; the parent subsequently completed the manual comparison.
- The exercise player now uses a single labelled answer string (`A: …` through `D: …`) per option rather than rendering a selection control, a letter badge, and the same option text independently. Dynamic draft selection, hint, progress, hearts, and submit behavior remain unchanged.
- The capture root and direct Slice 6 surfaces use the app background, so every pixel in all five emitted PNGs is opaque.
- Theory now has a chapter label, distinct concept/example cards, numbered steps, and its existing navigation controls. Loading is a screen-shaped activity skeleton; Empty is a branded activity card with artwork, hierarchy, and action.

### TDD Cycle Evidence

| Task | Test file / layer | Safety net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|
| Capture opacity | `Slice6VisualAcceptanceCaptureTest` (JVM Compose + emitted PNG) | Existing capture suite green in Slice 6c | Focused run failed: the alpha assertion detected a transparent capture-root pixel | All five captures are 300×624 RGBA with every alpha value 255 | Checks exercise, theory, onboarding, empty, and loading roots | Shared themed capture root; direct screen roots also use `background`/`Surface` |
| Exercise hierarchy | `ExercisePlayerRedesignRenderTest` (JVM Compose render) | Existing render cases green before this batch | Focused run failed: `exerciseQuestion` tag was absent | 5 exercise tests green | Four independently-labelled options must each appear exactly once | Removed redundant radio/checkbox and duplicated letter-badge text while preserving card semantics |
| Theory and states | `SupportingSurfacesRedesignRenderTest` (JVM Compose render) | Existing supporting-surface cases green before this batch | Focused run failed: chapter/numbered-step and activity skeleton/empty-card nodes were absent | 8 supporting-surface tests green | Three numbered steps, two loading choice skeletons, plus branded empty action | Reused theme surfaces/cards; no new route or state contract |

### Work Unit Evidence

| Evidence | Result |
|---|---|
| Observed RED | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --tests '*ExercisePlayerRedesignRenderTest' --tests '*SupportingSurfacesRedesignRenderTest' --tests '*Slice6VisualAcceptanceCaptureTest'` → **BUILD FAILED**, 14 tests / 5 expected assertion failures (opaque capture, missing exercise hierarchy, missing theory/state hierarchy). |
| Focused GREEN + runtime harness | Same command → **BUILD SUCCESSFUL**, 14 tests, 0 failures/errors. It rendered the real player, theory, onboarding, and state composables and regenerated all five captures. |
| Historical full required result | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon` → **BUILD SUCCESSFUL**, 31 suites / 157 tests, 0 failures/errors/skips. |
| Capture outputs | `composeApp/build/visual-acceptance/current/task-6.7-{exercise,theory,onboarding,empty,loading}.png`; `file` verified each as 300×624 8-bit RGBA PNG. |
| Rollback boundary | Revert only Slice 6d hunks in `LessonMapScreen.kt`, `TheorySheet.kt`, `PlaceholderScreen.kt`, `OnboardingScreen.kt`, and the three Slice 6 JVM render/capture tests; no model, route, persistence, or backend contract changes. |

### Scope, Budget, and Pending Acceptance

- Authored Slice 6d source/test delta: **322 additions/deletions from the generation's received worktree snapshot**, within the 400-line cap; existing uncommitted Slice 6a–6c worktree changes remain outside this corrective bound. Documentation and generated PNGs are excluded from the source budget.
- No staging, commit, push, review, archive, delegation, or runtime-ledger edit occurred.
- Historical pending state: parent visual comparison against `ejercicio-gameplay.png`, `teoria-leccion.png`, `estado-carga.png`, and `estado-vacio.png` was required; task 6.7 was later accepted.

## Slice 6e Parent Manual Visual Acceptance (Historical; Superseded by Canonical Final State)

- This parent acceptance supersedes the historical Slice 6d pending-acceptance wording above: task 6.7 is now accepted and complete.
- Compared regenerated opaque 300×624 captures against canonical `docs/ui/screens` references.
- **Exercise — PASS**: immersive player, title/question, hearts/progress, unique options, hint, and CTA hierarchy match; fixture copy/option count differs, while type-specific content remains supported.
- **Theory — PASS**: concept, numbered steps, example, and navigation hierarchy match while preserving the sheet architecture.
- **Loading — PASS**: exercise skeleton structure matches. **Empty — PASS**: scoped branded activity-empty card matches, not a full dashboard host.
- **Onboarding — PASS**: approved spec/design tokens and hierarchy match. No valid canonical wizard PNG exists; `splash-bienvenida` is explicitly out of scope.
- Historical test support was 14 focused GREEN tests and full JVM 31 suites / 157 tests. Current final evidence is 31 suites / 158 tests; earlier manual tasks are resolved.

## Remaining Visual Capture Evidence

- Generated opaque 300×624 RGBA captures for parent comparison: `task-1.9-login.png`, `task-2.4-register-step{1,2,3}.png`, `task-4.5-home.png`, and `task-5.6-lesson-map.png` under `composeApp/build/visual-acceptance/current/`.
- Historical focused capture test: **BUILD SUCCESSFUL**; historical full JVM suite: **BUILD SUCCESSFUL**, 31 suites / 157 tests, 0 failures/errors/skips.
- Historical capture record only: parent subsequently accepted 1.9, 2.4, 4.5, and 5.6; 3.5 was waived under Profile v2. No production or profile changes were made.

## Generation 8 Remaining Visual Defects

- RED: focused render suite exposed the unsupported greeting emoji assertion; capture review exposed Google wrapping, compact-header title wrapping, and step-3 viewport pressure.
- Historical GREEN: Google is single-line, Auth scaffold vertical spacing is compacted, greeting uses supported text, and lesson titles use one-line ellipsis. Focused 14-test suite and full JVM suite both **BUILD SUCCESSFUL**; historical full result 31 suites / 157 tests, 0 failures/errors/skips.
- Regenerated captures remain under `composeApp/build/visual-acceptance/current/`: tasks 1.9, 2.4 steps 1–3, 4.5, and 5.6; each is 300×624 RGBA. These manual tasks are now accepted.

### Final Generation-8 Evidence

- Historical handoff: parent accepted regenerated captures for tasks 1.9, 2.4, 4.5, and 5.6; the parent subsequently persisted their checkboxes as complete.
- Historical full `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon` → **BUILD SUCCESSFUL**, 31 suites / 157 tests, 0 failures/errors/skips. `git diff --check` passed.
- Generation-8 authored correction remains approximately 21 changed lines, below the 200-line cap.

## Parent Manual Acceptance — Remaining Visual Tasks

- **1.9, 2.4, 4.5, 5.6 — ACCEPTED**: parent inspected deterministic opaque 300×624 captures for login, register steps 1–3, home, and lesson map. Historical Generation-8 focused GREEN and full JVM evidence recorded 31 suites / 157 tests; canonical final state is 31 suites / 158 tests.
- **3.5 — WAIVED/COMPLETE**: canonical `perfil-usuario.png` conflicts structurally with the approved Profile v2 navigation-hub specification. Profile v2 is authoritative; no PNG conformance is claimed and no profile production change was made.
- All parent manual visual tasks are now complete; this record preserves prior conflict analysis and does not alter code, tests, runtime ledger, or delivery state.

## Generation 10 Apply — Exercise Verification Remediation

### Authority and Scope

- Native request: `ui-redesign-exercise-coverage-20260815`; attempt token `sha256:28b6e880c673cefd6e58c85edb8750cc92d6a1081eb6141583f7e629b056eb6e`.
- Failed evidence being remediated: `sha256:59b07a56b4ef475c7ffae340da181e21b8d5c1ba173e7c190407228cebf9ced5`.
- Delivery remains `stacked-to-main`; this bounded unit covers exercise rendering, submission shaping, retry behavior, existing server validation, and role filtering. It makes no backend/shared production change and does not claim final verification.
- Mode: **Standard** (`strict_tdd: false` in `openspec/config.yaml`).

### Completed Task

- [x] **7.4a** Added runtime coverage for the `InputValue` field/callback, `MultiSelect` grid/selection callback, safe incompatible payload/draft fallback, trimmed input submissions, partial then exact multi-select submissions, and three consecutive incorrect attempts followed by a successful advance.
- [x] **7.4b** Added focused server tests proving single-choice correctness, trimmed-input correctness, exact-set acceptance, partial-set rejection, and STUDENT-hidden versus ADMIN-visible correct fields for all three typed payloads.
- Added the stable `exerciseInputValue` semantics tag. The existing confirmation test now clicks its stable tag instead of locale-specific Spanish copy.

### Exercise Scenario Evidence

| Scenario | Evidence | Status |
|---|---|---|
| InputValue renders text input | Real `LessonMapContent` render finds `exerciseInputValue` and dispatches typed text | Covered |
| MultiSelect renders in answer grid | Real render finds four answer cards, two selected nodes, and dispatches an independent toggle | Covered |
| Unknown/fallback payload | Incompatible typed payload/draft reaches the production fallback without crashing | Partial: the sealed shared payload model cannot construct an unknown subtype; unknown serialization behavior is outside this Compose-only unit |
| MultipleChoice single-option submission | Existing client dispatch test plus `ExercisePayloadSupport.evaluateAttempt` with correct option B | Covered |
| InputValue trimmed submission | ViewModel sends trimmed input; server evaluator accepts `" 42 "` for correct value `"42"` | Covered |
| MultiSelect exact set / partial rejection | Client submits partial then changed exact set; server evaluator accepts `{A,C}` and rejects `{A}` | Covered |
| Multiple wrong attempts do not block | Three incorrect responses reduce lives to zero without closing the player; the fourth correct response completes and advances | Covered |
| Student/admin answer hiding | `LessonService.getLessonByIdForUser` returns null correct fields for STUDENT and populated fields for ADMIN across all payload types | Covered without production changes |

### Work Unit Evidence

| Evidence | Result |
|---|---|
| Baseline focused command | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --tests '*ExercisePlayerRedesignRenderTest' --tests '*LessonMapViewModelTest'` → **BUILD FAILED**, 12 tests / 1 failure because the pre-existing confirmation test searched Spanish `Confirmar` while the active resources rendered another locale. This was test coupling, not a production failure. |
| Focused test + runtime harness | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --rerun-tasks --tests '*ExercisePlayerRedesignRenderTest' --tests '*LessonMapViewModelTest'` → **BUILD SUCCESSFUL**, 2 suites / 18 tests, 0 failures/errors/skips. The JVM Compose harness rendered the real player branches and the ViewModel harness exercised real draft-to-submission and retry transitions. |
| Focused server runtime | `bash ./gradlew :server:test --console=plain --no-daemon --rerun-tasks --tests '*LessonExerciseServiceTest'` → **BUILD SUCCESSFUL**, 1 suite / 12 tests, 0 failures/errors/skips. |
| Full required command | `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --rerun-tasks` → **BUILD SUCCESSFUL**, 31 suites / 164 tests, 0 failures/errors/skips. |
| Runtime boundary | Real `LessonMapContent`, `LessonMapViewModel`, `ExercisePayloadSupport`, and role-aware `LessonService` paths were exercised; server correctness was not inferred from fake client responses. |
| Rollback boundary | Revert only the Generation 10 hunks in `LessonMapScreen.kt`, `LessonMapViewModelTest.kt`, `ExercisePlayerRedesignRenderTest.kt`, and the 7.4 task/progress entries. Earlier redesign implementation and evidence remain intact. |

### Budget and Remaining Work

- Complete authored objective delta: **386 changed lines** (`+365/-21`, including task/progress evidence), below the 400-line cap. No generated golden was added.
- Remaining: 7.4c other capability gaps; 7.4d independent final verification.
- No stage, commit, review, finalize, settle, archive, or modification outside `/tmp/mathimapp-ui-redesign-native2` occurred.

## Generation 11 Apply — UI Lifecycle Verification Remediation

### Authority and Scope

- Native request: `ui-redesign-ui-lifecycle-20260815`; attempt token `sha256:2ab8e0c413460057a8ff80cc11834386c2cea91ab3365fcd93a9bff23761163b`.
- Work unit: `verification-remediation-ui-lifecycle`; delivery remains `stacked-to-main`; mode is **Standard** (`strict_tdd: false`).
- This unit completes 7.4c only. Task 7.4d and archive readiness remain pending.

### Runtime Coverage

- Auth render tests prove both social providers are no-ops, forgot-password dispatches its recovery callback and state, and Register step-1 Back clears wizard state before returning to Login.
- JetBrains Mono SemiBold and its official OFL are bundled; the step label now loads that resource instead of generic system monospace.
- Existing Home render tests directly invoke empty-learning and lesson-map callbacks; no catalog destination is claimed. New runtime proof locks a streak of 3 below the cap while retaining the existing 12→7 proof.
- Lesson-map tests now invoke the Current node callback and the production path-style decision for locked (dashed) versus actionable/completed (solid) destinations.
- Onboarding uses `SavedStateHandle` primitives to restore step, province, category, available years, and selected year after ViewModel recreation. Completion persists the learner profile and refreshes `AuthGate` into `AuthenticatedHomeScaffold`; no production `CourseScreen` or catalog-filter route is claimed.
- Profile v2 is now explicitly authoritative in proposal, design, and profile spec; the conflicting PNG concept is deferred rather than silently claimed compliant.

### Work Unit Evidence

| Evidence | Result |
|---|---|
| First focused compile | Failed on a nullable-list assertion type in the new handoff probe; corrected without a production workaround. |
| Second focused run | 46 tests, 2 test-fixture assertion failures (merged summary text and probing the wrong dashboard repository); both probes were corrected to exercise the actual contract. |
| Focused runtime harness | `:composeApp:jvmTest --rerun-tasks` across Auth, Onboarding, Course, Home, and Lesson-map suites → **BUILD SUCCESSFUL**, 7 suites / 48 tests. |
| Font identity | `jetbrains_mono_semibold.ttf` SHA-256 `12d4b18fe6e1af528e4bea69cb0997aeff22f9e52fccffcf66342dd88aa32ab8`; `JetBrainsMono-OFL.txt` SHA-256 `a76abf002c49097d146e86740a3105a5d00450b1592e820a1109a8c5680cd697`. |
| Rollback | Revert only Generation 11 hunks in App/Auth/Register/Onboarding/Home/Lesson-map source and focused tests, remove the two JetBrains resource files, and revert the Generation 11 task/spec/progress entries. |

- Register's three deterministic captures were regenerated because the intentional step-1 Back control and bundled font change the surface. New SHA-256 values: step 1 `e55a6c...54f7`, step 2 `cc4ae6...0c67`, step 3 `bfbd5c...ab8a`; the focused capture harness passes with these candidate-generated images.
- Required full `bash ./gradlew :composeApp:jvmTest --console=plain --no-daemon --rerun-tasks` → **BUILD SUCCESSFUL**, 31 suites / 171 tests, 0 failures/errors/skips.
- `git diff --check` passes. Objective delta is 269 tracked changed lines plus 93 verbatim OFL lines = **362 authored/text lines**; the downloaded TTF binary is excluded by policy. No size exception is used.
- No Gradle/Java process remains; no stage, commit, review, finalize, settle, or archive occurred.

## RDD Correction — Final Review R3

- Authority: lineage `review-89c318a5d1780c7b`, generation 1, revision `sha256:a792ed66f9055deffd5d5f5a44803b0dd0b6f2fffa81d9f01f6a899fc5bfb4fe`.
- R3-001: forgot-password now enters a production-wired recovery destination with a real Back route; no nonexistent reset endpoint is claimed.
- R3-002/R3-003: the delta specs now name the actual Lesson Map and authenticated-home destinations; catalog, `CourseScreen`, and catalog-filter claims were removed and tracked as separate debt.
- R3-004: restored the localized greeting wave and refreshed only the affected deterministic Home capture baseline (`7d4558...b9df`).
- Warning remediation: stale saved enum names safely fall back; proposal/design acknowledge bounded reliability behavior. Full hardcoded-copy resource migration is deferred as a separate localization unit.
- Focused runtime: 5 suites / 38 tests passed. Full `:composeApp:jvmTest --rerun-tasks`: 31 suites / 174 tests passed. `git diff --check` passed.
