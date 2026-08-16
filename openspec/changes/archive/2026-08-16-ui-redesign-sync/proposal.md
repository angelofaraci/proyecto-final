# Proposal: UI Redesign Sync

## Intent

Bring every already-implemented Compose screen into visual compliance with the updated redesign references in `docs/ui/screens/`, plus bounded remediation needed to keep exposed navigation and retained UI state truthful. Backend and shared contracts remain unchanged.

## Scope

### In Scope
- Foundation slice: Sora font bundling, theme token updates (typography, shape, semantic colors), primitive shadows.
- Per-feature slices: auth (login/register), profile hub + sub-screens, home dashboard, lesson map rewrite, exercise player, theory sheet, loading/empty states, onboarding token alignment.

### Out of Scope
- New screens from redesign (catalog, detail, progress, rewards, splash, full-screen feedback, per-type exercise players).
- Dark mode.
- Backend or shared contract changes.

## Capabilities

### New Capabilities
- `lesson-map-ui`: Graphical path-based lesson map with circular node icons, progress bar header, and pill theory button. Replaces card-list layout.

### Modified Capabilities
- `profile-screen`: Streak chip, colored SVG icon boxes, 18px radius cards, logout card, sub-screen list icons, dark-mode toggle stub.
- `profile-hub-navigation`: Sub-screen stubs get leading icons; Preferences stub gets dark-mode toggle placeholder.
- `frontend-auth`: Sora font, CTA shadow, social button radius, divider text, 22×22 coral checkbox, step indicator color/height, password strength teal.
- `home-dashboard`: Visual compliance against redesigned PNG (pending pixel verification in design phase).
- `onboarding-flow`: SelectionCard and wizard styling alignment with new tokens.
- `exercise-type-player`: Lives/hearts UI, header progress bar, close icon, question card restyle, 2-column answer grid with glow, hint link, bottom-fixed CTA shadow.

## Approach

Foundation-first delivery: Slice 1 updates `TypeTokens` (Sora), `ShapeTokens` (radii), `ColorTokens` (track, lock, stripe), and primitive shadows (`MButton`, `MCard`). Subsequent slices apply tokens to each feature area. Redesign PNGs are the visual source of truth except for Profile, where the approved Profile v2 navigation-hub delta is authoritative; `docs/ui/screens/old/` provides the design delta.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `composeApp/.../ui/theme/` | Modified | TypeTokens, ShapeTokens, ColorTokens. |
| `composeApp/.../ui/primitives/` | Modified | MButton, MCard, MProgressIndicator, ProfileNavigationCard, ProfileListRow. |
| `composeApp/.../ui/LoginScreen.kt` | Modified | Font, shadow, radii, checkbox, social buttons. |
| `composeApp/.../ui/RegisterScreen.kt` | Modified | Step indicator, password strength color, terms checkbox. |
| `composeApp/.../ui/ProfileScreen.kt` | Modified | Streak chip, icon boxes, logout card, list icons, toggle stub. |
| `composeApp/.../ui/home/HomeDashboardScreen.kt` | Modified | Layout vs new PNG. |
| `composeApp/.../ui/activities/LessonMapScreen.kt` | Modified | Full rewrite to graphical path. |
| `composeApp/.../ui/activities/LessonMapNode.kt` | Modified | Circular nodes, lock dashed stroke. |
| `composeApp/.../ui/activities/ExercisePlayerContent.kt` | Modified | Lives, progress bar, grid answers, hint, CTA shadow. |
| `composeApp/.../ui/activities/TheorySheet.kt` | Modified | Structured sections, card styling. |
| `composeApp/.../ui/OnboardingScreen.kt` | Modified | SelectionCard token alignment. |
| `composeApp/src/commonMain/composeResources/` | New | Sora font files (OFL), empty/loading illustrations. |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Lesson Map rewrite complexity | High | Dedicated slice; prototype path layout before full implementation. |
| Home Dashboard gap uncertainty | Med | Design phase must close exact gaps with pixel verification. |
| Sora bundling (OFL font files) | Low | Verify license attribution; check APK size impact. |
| Login-register handoff drift (Jul 16 vs Jul 12) | Med | Cross-check against Jul 16 `.dc.html`; accept minor functional deltas. |

## Rollback Plan

Revert implementing commits. No schema or backend contract changes means a clean git revert restores prior visuals. For the Lesson Map rewrite, maintain a backup branch from `main` before applying so the original card-list composable can be restored instantly.

## Dependencies

None external. Internal: Foundation slice merges before feature slices to avoid per-screen token duplication.

## Success Criteria

- [ ] Login and Register screens match Jul 16 handoff (font, shadows, radii, checkbox, step indicator).
- [x] Profile hub matches the approved Profile v2 navigation-hub delta; `perfil-usuario.png` is a superseded structural concept and is not a conformance target for this change.
- [ ] Home Dashboard matches `inicio-dashboard.png` (pending pixel confirmation).
- [ ] Lesson Map matches `mapa-leccion.png` (graphical path, circular nodes, progress bar, pill theory button).
- [ ] Exercise Player matches `ejercicio-gameplay.png` (lives, progress bar, close icon, grid answers, hint, CTA shadow).
- [ ] Theory Sheet, loading, and empty states match their redesign references.
- [ ] Onboarding wizard tokens align with design system.
- [ ] `:composeApp:jvmTest` passes.
- [ ] No functional behavior regressions (navigation, state contracts, validation untouched).
