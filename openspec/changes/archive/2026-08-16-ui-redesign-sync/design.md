# Design: UI Redesign Sync

## Technical Approach

**Foundation-first visual sync with bounded reliability remediation**. Slices 1–6 apply tokens per feature; later remediation may wire already-specified navigation or retain screen-local state. Backend/shared contracts and dark mode remain excluded.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| Font strategy | Bundle Sora (OFL) in `composeResources/font/`, wire via `FontFamily` in `TypeTokens` | System Nunito only | Redesign mandates Sora; ~120KB per weight, acceptable APK impact. Nunito fallback declared in font stack. |
| ShapeTokens expansion | Add `checkbox`, `iconBox`, `socialButton`, `stepSegment` to `AppShapeTokens` data class | Hardcode radii per screen | Token expansion avoids per-screen magic numbers; single source of truth. |
| Shadow delivery | Compose `Modifier.shadow()` on `MButton.Filled` + new `MSelectedCard` glow variant | Elevation-based shadows | Rounded corner shadows not achievable with `elevation`; `shadow()` gives exact blur/spread control. |
| Lesson map geometry | Pure-Compose Canvas `drawPath()` for polyline + Box/offset for node placement. Serpentine: nodes alternate left/right every 2 nodes, ~120dp vertical step. | SVG asset import | Canvas is resolution-independent, themable, avoids asset management. Serpentine avoids horizontal overflow on narrow screens. |
| Exercise player grid | `LazyVerticalGrid(columns = Fixed(2))` wrapping existing `ChoiceOptionCard` with glow modifier | Rewrite answer section from scratch | Minimal refactor: reuse existing card + selection control, add glow. |
| Logout restyle | Surface `MCard` with icon + centered text, not a separate composable | Keep outline button | Profile design spec shows card-style logout; simpler than a dedicated `LogoutButton` composable. |
| OFL attribution | `composeResources/files/OFL.txt` | Embedded comment in theme file | Self-documenting, discoverable, matches iOS/Android bundling conventions. |

## Theme Foundation — Concrete Token Values

**Semantic colors** (add to `ColorTokens.kt`):
- `Track = Color(0xFFEADFD1)` — progress bar empty segment
- `Lock = Color(0xFFCBBEAE)` — locked node/path
- `Stripe = Color(0xFFF2E9DD)` — diagram/chart placeholder

**ShapeTokens** (update `AppShapeTokens` data class):
- `card: 18.dp` (was 28), `button: 16.dp` (was 20), `field: 15.dp` (was 18)
- `pill: 999.dp` (unchanged)
- **New**: `checkbox: 7.dp`, `iconBox: 13.dp`, `socialButton: 14.dp`, `stepSegment: 999.dp`

**TypeTokens** (Sora weights to bundle): Regular(400), SemiBold(600), Bold(700), ExtraBold(800). Wire as `FontFamily(listOf(Font("font/Sora-Regular.ttf"), ..., FontFamily.SansSerif))`. Scale: headLg 32/800, headMd 27/800, headSm 21/800, titleLg 17/700, titleMd 14/700, bodyLg 15/600, bodyMd 13/600, bodySm 12/500, labelMd 12/600.

**Shadows**: `MButton.Filled` gets `Modifier.shadow(12.dp, shape, spotColor = coral42alpha, ambientColor = coral42alpha)`. Selected answer cards get same pattern at `6.dp / -8.dp`. Coral alpha rgba: `Color(0x6BF2654B)`.

**ComposeResources wiring**: `composeResources/font/` for `.ttf` files, `composeResources/files/OFL.txt` for attribution. Generated accessor: `Res.font.sora_regular`, etc.

## Lesson Map Path Layout Algorithm

1. **Scrollable Column** (`verticalScroll`) containing a `Box` sized to total path height (`nodeCount * 120.dp`).
2. **Canvas layer** draws the polyline connecting all node centers. Path iterates nodes i→i+1: if node i is Locked → dashed gray (`lock` color, `PathEffect.dashPathEffect(floatArrayOf(8f, 6f))`); else solid, color teal for completed, coral for current/unlocked.
3. **Node positions**: alternating offset. Even-index nodes at x=72dp from left, odd at x=72dp from right. y = i * 120dp + 60dp.
4. **Node composables**: 56dp circles positioned via `Modifier.offset(x, y)`. Completed → teal circle + white checkmark icon. Current → coral circle + white play icon. Locked → gray (`lock`) circle + white lock icon, non-clickable.
5. **Theory pill** floats in header area, not inline with path. Scroll behavior: `rememberScrollState()`.

## File Changes

### Slice 1: Foundation (~160 lines)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/theme/ColorTokens.kt` | Modify | +5 | Add `Track`, `Lock`, `Stripe` |
| `ui/theme/ShapeTokens.kt` | Modify | +4 | Add checkbox, iconBox, socialButton, stepSegment; update card/button/field |
| `ui/theme/TypeTokens.kt` | Modify | ~30 | Replace SansSerif with Sora FontFamily, update scale values |
| `ui/theme/AppTheme.kt` | Modify | +2 | Pass new shape tokens to MaterialTheme shapes |
| `ui/primitives/MButton.kt` | Modify | +3 | Add `shadow()` modifier to Filled variant |
| `ui/primitives/MCard.kt` | Modify | +1 | Use new `card` radius (18dp via shapes.large) |
| `ui/primitives/MProgressIndicator.kt` | Modify | +6 | Add 8dp height linear variant with `track` color |
| `composeResources/font/` | New | — | Sora `.ttf` files (4 weights) |
| `composeResources/files/OFL.txt` | New | — | Sora OFL license |

### Slice 2: Auth Sync (~120 lines)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/LoginScreen.kt` | Modify | ~30 | Social button 14dp radius, divider 12/600 muted + lines, CTA shadow via MButton, footer 14/700 coral |
| `ui/RegisterScreen.kt` | Modify | ~45 | Step indicator 5px/999px, "Paso X / 3" JetBrains Mono 12/600, password strength teal, checkbox 22×22 7px coral |
| `ui/AuthScreenScaffold.kt` | Modify | ~15 | Brand logo box 16dp radius (was card=28), formTitle 32/800 Sora |

### Slice 3: Profile Sync (~250 lines)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/ProfileScreen.kt` | Modify | ~100 | Streak chip (coral flame pill), logout card restyle, nav card icon SVGs (42×42, 13dp), sub-screen list icons, version dynamic string |
| `ui/primitives/ProfileNavigationCard.kt` | Modify | ~25 | 42×42 icon box, 13dp radius, colored SVG icons |
| `ui/primitives/ProfileListRow.kt` | Modify | +10 | Leading icon parameter (18dp muted SVG) |

### Slice 4: Home Dashboard Sync (~180 lines)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/home/HomeDashboardScreen.kt` | Modify | ~120 | Greeting + streak chip inline, progress card (level, 8dp teal bar, XP text), "Mis cursos en progreso" header + course cards, catalog CTA restyle |
| `ui/home/HomeDashboardViewModel.kt` | Modify | ~15 | Add course list state, streak computation |

### Slice 5: Lesson Map (~350-line forecast; ~705 actual)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/activities/LessonMapScreen.kt` | Modify | ~200 | Canvas path renderer, node placement algo, header (back + title + theory pill + progress bar), remove card-list scroll |
| `ui/activities/LessonMapNode.kt` | Modify | ~150 | Rewrite as circular path node (56dp circle, icon per state, non-interactive locked) |

### Slice 6: Exercise Player + Theory + Onboarding + States (~370 lines)

| File | Action | Lines | Description |
|------|--------|-------|-------------|
| `ui/activities/ExercisePlayerContent.kt` | Modify | ~200 | Header (X close, title, 3 hearts, 8dp progress bar), question card 22dp, 2-column grid answers, hint link, bottom-fixed CTA with shadow |
| `ui/activities/TheorySheet.kt` | Modify | ~50 | Structured sections, card styling, token alignment |
| `ui/OnboardingScreen.kt` | Modify | ~60 | SelectionCard 18dp radius/line border, action buttons 16dp radius + shadow, back button 38×38 12dp radius |
| `ui/PlaceholderScreen.kt` | Modify | ~20 | Branded empty-state illustration, token alignment |
| `composeResources/drawable/` | New | — | Empty/loading illustration SVGs |

## Testing Strategy

### jvmTest (automated, per slice)

| Slice | What to Test | Approach |
|-------|-------------|----------|
| Foundation | `AppThemeTokensTest` — assert new shape values (card=18, etc.), color additions (Track, Lock, Stripe), typography points to Sora FontFamily | Update existing assertions |
| Auth | ViewModel tests keep passing (no logic changes). Render tests: checkbox size, button shadow presence via semantics | `composeTestRule` semantics tree |
| Profile | `ProfileViewModelTest` — streak chip renders, logout card has text, sub-screen icons present | Semantics assertions |
| Home | `HomeDashboardViewModelTest` — streak computation, course list. Render: progress bar with `track` color | Existing VM test + composeTestRule |
| Lesson Map | Node state palette (teal/coral/gray assertions), path Drawing, locked node non-clickable | `composeTestRule.onNodeWithTag` |
| Exercise | Answer grid renders in 2 columns, hearts visible (3), hint link present, CTA has shadow | Compose semantics |

### Visual/Manual (per slice)

- **Auth**: Pixel comparison against Jul 16 `.dc.html` handoff — font rendering, shadow blur, divider alignment.
- **Profile**: Profile v2 navigation-hub delta vs rendered hub — icon box colors, card radii, navigation, and logout. The structurally different Jul 21 `perfil-usuario.png` is deferred to a future change.
- **Home**: `inicio-dashboard.png` pixel comparison — structural layout verification.
- **Lesson Map**: `mapa-leccion.png` — path curvature, node spacing, theory pill placement.
- **Exercise**: `ejercicio-gameplay.png` — heart positions, grid gap, hint link placement.
- **Onboarding**: Token alignment with design system — radius, shadow, font consistency.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

**PR order** (stacked to main):
1. `foundation/ui-redesign-tokens` ← merges first, all other slices depend on it
2. `feature/auth-redesign`
3. `feature/profile-redesign`
4. `feature/home-dashboard-redesign`
5. `feature/lesson-map-rewrite` (heaviest)
6. `feature/exercise-states-redesign`

**Merge dependencies**: Slices 2–6 all depend on Slice 1. Slices 2–6 are independent of each other and can be developed in parallel after Slice 1 lands. Slices 3 and 5 carry the most risk.

**Rollback**: Per-slice `git revert`. Since no schema or backend changes, revert is clean visual rollback. Lesson map has a backup branch from main (card-list implementation preserved).

**Feature flags**: None required. Visual-only; each slice can be merged individually without hiding behind flags.

## Open Questions

- [x] **Profile PNG vs handoff drift resolved**: the approved Profile v2 navigation-hub delta is authoritative; `perfil-usuario.png` is not a conformance target for this change.
- [ ] **Home Dashboard PNG** exact structural layering: precise widget positions from `inicio-dashboard.png` must be confirmed during implementation.
- [ ] **Sora OFL attribution**: Is `composeResources/files/OFL.txt` sufficient, or does the license require in-app attribution?
