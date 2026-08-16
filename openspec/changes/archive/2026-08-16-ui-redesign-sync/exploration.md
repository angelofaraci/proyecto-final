# Exploration: ui-redesign-sync

## Current State

### Implemented Screens (Compose UI under `composeApp/src/commonMain/kotlin/com/example/proyectofinal/ui/`)

| Screen / File | Path | Description |
|---|---|---|
| LoginScreen.kt | `ui/LoginScreen.kt` | Email + password form, social login row, recovery link, "Registrate" switcher. Uses `AuthScreenScaffold`. |
| RegisterScreen.kt | `ui/RegisterScreen.kt` | 3-step wizard (name → email → password/terms). Uses `AuthScreenScaffold` + `WizardStepIndicator`. |
| ProfileScreen.kt | `ui/ProfileScreen.kt` | Profile hub (identity + 4 nav cards + logout) + 4 sub-screens (Account, Preferences, Help, About). |
| HomeDashboardScreen.kt | `ui/home/HomeDashboardScreen.kt` | Greeting, school-year chip, progress summary card, continue-learning / join-course cards. |
| LessonMapScreen.kt | `ui/activities/LessonMapScreen.kt` | Lesson map list (card-based nodes with connectors), inline exercise player, error/retry. |
| TheorySheet.kt | `ui/activities/TheorySheet.kt` | `ModalBottomSheet` with lesson title and theory content. |
| OnboardingScreen.kt | `ui/OnboardingScreen.kt` | 4-step wizard (province → school year → track → confirmation) using `SelectionCard`. |
| PlaceholderScreen.kt | `ui/PlaceholderScreen.kt` | Generic centered title + message placeholder. Used only for **Progress** tab. |
| AuthScreenScaffold.kt | `ui/AuthScreenScaffold.kt` | Shared auth wrapper: brand logo, form title/subtitle, scrollable column. |
| AuthenticatedHomeScaffold.kt | `ui/AuthenticatedHomeScaffold.kt` | Root scaffold with `NavigationBar` (4 tabs: Home, Activities, Progress, Profile). |

### Shared Primitives

| Primitive | Path | Role |
|---|---|---|
| MButton | `ui/primitives/MButton.kt` | Filled / Outline / Social button variants. |
| MCard | `ui/primitives/MCard.kt` | Surface card with outline border, zero elevation. |
| MTextField | `ui/primitives/MTextField.kt` | Outlined text field; `authStyle` uses 15.dp radius + focus glow. |
| MProgressIndicator | `ui/primitives/MProgressIndicator.kt` | Circular + linear progress wrappers. |
| ProfileListRow | `ui/primitives/ProfileListRow.kt` | Label + value + chevron inside MCard. |
| ProfileNavigationCard | `ui/primitives/ProfileNavigationCard.kt` | Icon box + title/subtitle + chevron inside MCard. |
| ProfileToggleRow | `ui/primitives/ProfileToggleRow.kt` | Label + Switch inside MCard. |

### Theme Tokens

| Token File | Path | Current State |
|---|---|---|
| ColorTokens | `ui/theme/ColorTokens.kt` | Light-mode brand palette matches redesign specs (coral `#F2654B`, teal `#0E9E8E`, rose `#F0526A`, neutrals `#FBF6EF` → `#26333B`). **Missing** semantic tokens: `track` (`#EADFD1`), `lock` (`#CBBEAE`), `stripe` (`#F2E9DD`). |
| TypeTokens | `ui/theme/TypeTokens.kt` | Uses `FontFamily.SansSerif`. **Design requires `Sora` (fallback `Nunito`)**. All scale values are close but not exact (e.g., headlineSmall 22.sp/600 vs design 21px/800). |
| ShapeTokens | `ui/theme/ShapeTokens.kt` | Card 28.dp, button 20.dp, field 18.dp, pill 999.dp. **Design wants** card ~18–22px, button ~16–18px, field 15px, checkbox 7px. |
| AppTheme | `ui/theme/AppTheme.kt` | Wraps `MaterialTheme` with custom shape local. No dark-mode scheme. |

---

## Redesign Delta Summary

### Global Changes (affect every screen)
1. **Typography family**: Must switch from `SansSerif` to `Sora` (with `Nunito` fallback). This is a single-token change with massive visual impact.
2. **Shape radii**: Cards should shrink from 28.dp to ~18–20.dp. Buttons from 20.dp to ~16.dp. These are token-level but require checking every screen for clipping/layout issues.
3. **Shadow / glow system**: Primary CTA buttons require `0 12px 24px -10px rgba(coral,0.42)` shadow. Selected answer cards require `0 6px 16px -8px rgba(coral,0.42)` glow. Current primitives do not support shadows.
4. **New semantic colors**: `track` (progress-bar empty segment), `lock` (locked node/path), `stripe` (exercise diagram placeholder). Needed for Lesson Map and Exercise player.
5. **Progress indicator language**: Design uses **linear** 8.dp-rounded progress bars in headers; app currently uses circular spinners for loading and no progress bars in exercise headers.

### File-level Redesign Evidence (PNG hashes)
- **Changed** (new ≠ old, Jul 21 vs Jul 6): `inicio-dashboard`, `catalogo-de-cursos`, `detalle-curso`, `mapa-leccion`, `teoria-leccion`, `estado-carga`, `estado-vacio`, `splash-bienvenida`, `resultados-progreso`, `retroalimentacion-correcta`, `retroalimentacion-incorrecta`, `logro-recompensa`, `perfil-usuario`, `ejercicio-gameplay`.
- **Unchanged** (same hash): all `ej-*` and most `ejercicio-*` specific exercise type PNGs (they were copied but not redesigned this round).

---

## Per-Screen Gap Analysis

| Screen | Redesign Reference | Implementation File | Verdict | Specific Gaps |
|---|---|---|---|---|
| **Login** | `login-register/Auth · Login y Register.dc.html` (mtime **Jul 16**, after handoff date 2026-07-12) | `ui/LoginScreen.kt` + `AuthScreenScaffold.kt` | **minor-drift** | ① Font family mismatch (SansSerif vs Sora). ② No CTA shadow/glow. ③ Social button radius 20.dp vs design 14px. ④ Divider text styling uses `bodySmall` instead of 12px/500 muted. ⑤ "Registrate" link font weight bold vs 700. Handoff was updated post-build; verify if layout proportions changed. |
| **Register** | Same handoff (Jul 16) | `ui/RegisterScreen.kt` + `AuthScreenScaffold.kt` | **minor-drift** | ① Same font/shadow/radii gaps as Login. ② Step indicator: 6.dp height vs design 5px; uses primary color segments vs coral. ③ Step label "Paso X de 3" uses `labelMedium` vs design 12px mono. ④ Password strength meter uses primary color; design wants teal. ⑤ Terms checkbox uses default M3 Checkbox; design wants 22×22 coral box. |
| **Profile Hub** | `profile/design_handoff_perfil/Perfil v2.dc.html` (Jul 16) **and** `perfil-usuario.png` (Jul 21 — **newer than handoff**) | `ui/ProfileScreen.kt` + `ProfileNavigationCard.kt` | **major-drift** | ① Missing **streak chip** ("Racha 12 días") next to role chip. ② Navigation card icon boxes: app uses text symbols ("C","P"…) in 48.dp `surface2` boxes; design wants **42×42 px, 13px radius, colored SVG icons** (coral/teal/rose/muted). ③ Card radius 28.dp vs design 18px. ④ Logout is an `Outline` MButton; design wants a **surface card with logout icon** + centered text. ⑤ Version text hardcoded "X"; design shows "1.4.2" (dynamic). ⑥ Sub-screen list rows lack leading icons. ⑦ Preferences screen missing **dark-mode toggle** and leading icons. ⑧ New PNG (Jul 21) may contain additional deltas not present in the Jul 16 handoff — **requires visual diff**. |
| **Home Dashboard** | `inicio-dashboard.png` (changed significantly: 171KB vs 45KB old) | `ui/home/HomeDashboardScreen.kt` | **unknown / likely major-drift** | ① The new PNG is ~4× larger, strongly suggesting new illustrations, layout sections, or data widgets not present in current implementation. ② Current screen is text-heavy with minimal visual elements. ③ Cannot determine exact gaps without pixel inspection, but structural drift is highly probable. **Recommendation: treat as major-drift pending visual review.** |
| **Lesson Map** | `mapa-leccion.png` (changed: 137KB vs 390KB old — simplified/recomposed) | `ui/activities/LessonMapScreen.kt` + `LessonMapNode.kt` | **major-drift** | ① **Layout language changed entirely**: app uses vertical scrollable **card list** with linear connectors; design shows a **graphical path** (SVG polyline) with absolutely-positioned circular node icons (teal checkmarks, coral play, gray locks). ② Header: design has a top progress bar (8px, teal) + "Ver teoría" pill button; app uses an MCard header + full-width outline button. ③ Node representation: app uses text-heavy cards with number badge + title + summary + state label; design uses minimal circular icons on a path. ④ Missing **bottom navigation bar** context in map view (design shows 4-tab nav). ⑤ Colors: locked path uses `outlineVariant`; design uses dedicated `lock` gray (`#CBBEAE`) with dashed stroke. |
| **Theory Sheet** | `teoria-leccion.png` (changed) | `ui/activities/TheorySheet.kt` | **minor-drift** | ① Current sheet is extremely minimal (title + body text). Redesign likely adds structured sections, card styling, or typography hierarchy. ② Token updates (font, radii) will improve fidelity. Without pixel access, assume token-level drift unless the PNG shows a radically different layout. |
| **Exercise Player** | `ejercicio-gameplay.png` (changed: 38KB vs 24KB old) | `LessonMapScreen.kt` → `ExercisePlayerContent` | **major-drift** | ① **No lives/hearts UI**: design shows 3 hearts (rose) at top right. App has no life system UI. ② **No header progress bar**: design shows 8px coral progress bar under the nav area. ③ **Navigation**: app uses "Back to lesson map" outline button; design uses **X close icon** at top left. ④ **Question card**: app uses `secondaryContainer` background; design uses **surface card with 22px radius and 1px line border**. ⑤ **Answer layout**: app uses **single-column** list with radio buttons/checkboxes on left; design uses **2-column grid** with 16px radius cards, coral border + glow on selected. ⑥ **Missing hint**: design has a "Pista" link with lightbulb icon; app has none. ⑦ **CTA**: app uses full-width MButton (20.dp radius, no shadow); design wants bottom-fixed "Confirmar" with 18px radius + coral shadow. ⑧ **Feedback**: app shows inline text; design references (`retroalimentacion-*.png`) imply **full-screen feedback overlays** which do not exist. |
| **Onboarding** | `splash-bienvenida.png` (changed dramatically: 189KB vs 20KB old) | `ui/OnboardingScreen.kt` | **not-implemented / scope mismatch** | The app has a **functional 4-step wizard** (province, year, track, confirm). The redesign reference is named "splash-bienvenida" (welcome splash) and is 9× larger than the old version, suggesting a rich marketing-style splash screen rather than the existing data-collection wizard. **No dedicated splash screen exists** in the current Compose tree. The onboarding wizard should be compared against a separate onboarding reference if one exists; only `splash-bienvenida.png` is present. |
| **Loading States** | `estado-carga.png` (changed: 101KB vs 17KB old) | Used across screens via `MProgressIndicator()` | **minor-drift** | App uses circular spinner. Design likely specifies a **branded linear or animated loading state** given the 6× size increase. Update `MProgressIndicator` or add a new branded loading composable. |
| **Empty States** | `estado-vacio.png` (changed: 150KB vs 37KB old) | `HomeDashboardScreen.kt` (ContinueLearningCard, JoinCourseCard), `PlaceholderScreen.kt` | **minor-drift → major-drift** | The new PNG is 4× larger, suggesting illustrated empty states. Current app uses text-only placeholders inside cards. If the redesign introduces illustrations/characters, this becomes structural. |
| **Progress Tab** | `resultados-progreso.png` (changed) | `ui/PlaceholderScreen.kt` (via `AuthenticatedHomeScaffold`) | **not-implemented / future slice** | The Progress tab is literally a `PlaceholderScreen(title = "Progreso")`. The redesign has a full results screen. Out of scope for `ui-redesign-sync`; belongs to a dedicated progress/gamification slice. |
| **Course Catalog** | `catalogo-de-cursos.png` (changed) | **None** | **not-implemented / future slice** | No course browsing/catalog screen exists. |
| **Course Detail** | `detalle-curso.png` (changed) | **None** | **not-implemented / future slice** | No course detail screen exists. |
| **Reward / Achievement** | `logro-recompensa.png` (changed) | **None** | **not-implemented / future slice** | No reward/achievement overlay or screen exists. |
| **Full-screen Feedback** | `retroalimentacion-correcta.png`, `retroalimentacion-incorrecta.png` (changed) | Inline feedback in `ExercisePlayerContent` | **not-implemented / future slice** | App shows feedback as a text line below the answer section. The redesign references are **full-screen overlays** (or large modals) with illustrations, animations, and score context. These do not exist as standalone screens/composables. |
| **Exercise Type Designs** | `ej-*.png`, `ejercicio-*.png` (13 files, mostly unchanged) | Generic `ExercisePlayerContent` | **not-implemented / future slice** | The app has a **single generic player** for MultipleChoice / InputValue / MultiSelect. The redesign provides **dedicated visual treatments** for drag-drop, number line, ordering, text completion, etc. Building per-type UIs belongs to `exercise-practice-ui`. |

---

## Out of Scope (Future Slices)

| Reference | Why Out of Scope | Suggested Future Change |
|---|---|---|
| `catalogo-de-cursos.png` | No catalog screen exists. | `course-catalog-ui` |
| `detalle-curso.png` | No detail screen exists. | `course-detail-ui` |
| `resultados-progreso.png` | Progress tab is a placeholder. | `progress-dashboard-ui` |
| `logro-recompensa.png` | No reward system UI exists. | `gamification-rewards-ui` |
| `splash-bienvenida.png` | No splash screen composable exists (app goes straight to onboarding wizard). | `onboarding-splash-ui` or merge with onboarding redesign |
| `retroalimentacion-*.png` | Feedback is inline text only; no full-screen overlay exists. | `exercise-feedback-ui` |
| `ej-*.png` / `ejercicio-*.png` (specific types) | Generic player only; per-type layouts not built. | `exercise-practice-ui` |

---

## Affected Areas

1. **`ui/theme/`** — `TypeTokens` (Sora), `ShapeTokens` (radii), `ColorTokens` (semantic additions).
2. **`ui/primitives/`** — `MButton` (shadows), `MCard` (radius), `MProgressIndicator` (branded loading), `ProfileNavigationCard` (icons, sizing), `ProfileListRow` (icons).
3. **`ui/ProfileScreen.kt`** — Add streak chip, swap logout to card style, add sub-screen icons, add dark-mode toggle in Preferences.
4. **`ui/home/HomeDashboardScreen.kt`** — Likely structural changes pending visual review of new PNG.
5. **`ui/activities/LessonMapScreen.kt`** + **`LessonMapNode.kt`** — Complete layout rewrite from card list to graphical path.
6. **`ui/activities/TheorySheet.kt`** — Enrich content styling.
7. **`ui/LoginScreen.kt`** / **`ui/RegisterScreen.kt`** — Token-level alignment (fonts, shadows, radii, checkbox style).
8. **`ui/OnboardingScreen.kt`** — May require a separate splash screen or significant visual upgrade if the wizard itself is also redesigned.
9. **Compose Resources** — New illustration assets for empty/loading states, exercise hints, lives/hearts, etc.

---

## Approaches

| Approach | Description | Pros | Cons | Effort |
|---|---|---|---|---|
| **A. Token-first, then screen-by-screen** | 1) Update theme tokens + primitives first (Sora, radii, shadows, semantic colors). 2) Apply to each screen in dependency order. | Centralizes visual changes; one token update fixes 80% of drift on simple screens; reduces regression. | High-risk screens (Lesson Map, Exercise) still need heavy rework; token-only PR is large but shallow. | Medium |
| **B. Screen-first, tokens per screen** | Each screen PR carries its own token tweaks and primitive changes. | Smaller, reviewable PRs; easy to QA per screen. | Duplicated token work; inconsistent radii/fonts across screens if merged separately; merge conflicts on `ColorTokens`/`TypeTokens`. | Medium-High |
| **C. Foundation PR + Major Rewrite PRs** | PR 1: Theme + primitives. PR 2: Auth sync. PR 3: Profile sync. PR 4: Home Dashboard sync. PR 5: Lesson Map + Exercise Player rewrite. | Clean separation of concerns; reviewers can focus on layout logic in PR 5 without token noise. | More PRs to manage; intermediate states may look inconsistent if PRs land out of order. | Medium |

### Recommendation
**Approach C (Foundation PR + Feature PRs)** is the best fit:
- The `ui-redesign-sync` change should be scoped to **already-implemented screens**.
- Slice 1: **Foundation** — `TypeTokens` (Sora), `ShapeTokens` alignment, `ColorTokens` semantic additions, primitive shadows (`MButton`, `MCard`). ~150–200 lines.
- Slice 2: **Auth sync** — Login + Register token alignment (font, shadows, checkbox, step indicator styling). ~100–150 lines.
- Slice 3: **Profile sync** — Profile hub + sub-screens (streak chip, icon boxes, logout card, list icons, dark mode toggle). ~200–300 lines.
- Slice 4: **Home Dashboard sync** — Update layout against new PNG (pending pixel verification). ~150–300 lines.
- Slice 5: **Lesson Map & Exercise sync** — The heaviest slice. Rewrite `LessonMapNode` as path-based nodes, redesign `ExercisePlayerContent` (grid answers, lives, progress bar, hint, close icon, CTA shadow). ~600–900 lines.
- Slice 6: **States & Theory** — Loading/empty branded states + theory sheet enrichment. ~100–200 lines.

**Rough size estimate**: ~1,300–2,100 lines across 6 slices. The Lesson Map + Exercise slice is the bulk (~40–45% of total effort).

---

## Risks

1. **Lesson Map structural rewrite** is high-touch and may break existing scroll/click logic. The current card-based list is simple; a path-based canvas/absolute layout is harder to make responsive and accessible.
2. **Sora font availability** — If the font is not bundled in `composeResources`, adding it requires resource plumbing and may increase APK size. Fallback to `Nunito` (system or bundled) must be verified.
3. **New PNGs without pixel access** — Several verdicts (especially Home Dashboard) are inferred from file-size deltas rather than direct visual comparison. There is a risk of underestimating drift.
4. **Login-register handoff updated post-implementation** (Jul 16 vs Jul 12 build date). The implementation may miss subtle requirements introduced in the Jul 16 revision. A manual diff of the `.dc.html` versions is not possible without version history.
5. **Profile PNG newer than handoff** (`perfil-usuario.png` Jul 21 vs `Perfil v2.dc.html` Jul 16). The PNG may contain deltas not documented in the handoff HTML.
6. **Exercise player generic vs per-type** — The redesign implies per-exercise-type layouts. Keeping a generic player while redesigning it may result in a "half-measure" that still doesn't match the specific `ej-*.png` references.
7. **Dark mode** — The design system documents both light and dark tokens. The current app has **no dark-mode scheme**. If the redesign mandate includes dark mode, effort multiplies significantly. The scope should explicitly exclude dark mode unless requested.

---

## Ready for Proposal

**Yes** — with the following clarifications the orchestrator should surface to the user:

1. **Scope boundary**: Should `ui-redesign-sync` include only already-implemented screens, or also build the missing screens (catalog, detail, progress, splash, full-screen feedback, per-type exercise players)? The exploration recommends **already-implemented only**.
2. **Dark mode**: The design system has full dark-mode specs. Is dark mode in scope? Current app is light-mode only.
3. **Lesson Map scope**: The redesign changes the lesson map from a card list to a graphical path. Is a full layout rewrite approved, or should we stick to token-level changes on the existing card list?
4. **Sora font**: Do we have licensing / bundling clearance for Sora, or should we use the system `Nunito` fallback?
5. **Home Dashboard & Profile PNGs**: The new PNGs are significantly different from old versions (and the profile PNG is newer than its handoff). We need **visual review** of these two files to confirm exact gaps before finalizing task breakdown.

Once clarified, the proposal can proceed with the 6-slice plan outlined above.
