# profile-screen Specification

## Purpose

Bottom-navigation shell for the authenticated area with four tabs and a Profile screen with user identity and client-derived gamification metrics. All logic in `composeApp`.

## Requirements

### Requirement: Bottom Navigation Shell

The system SHALL render a `Scaffold` with a `NavigationBar` of four tabs — Inicio, Actividades, Progreso, Perfil — as the authenticated area after `AuthGate` resolves. The shell's `MainRouterViewModel` SHALL be constructible from Koin DI without requiring a container-resolved `MainTab` binding.

#### Scenario: Authenticated area renders bottom nav

- GIVEN valid auth session and completed onboarding
- WHEN the authenticated area resolves
- THEN a Scaffold with four bottom-nav tabs displays with Inicio selected

#### Scenario: Tab selection switches content

- GIVEN bottom nav visible with Inicio selected
- WHEN the user taps the Perfil tab
- THEN the Profile screen displays and the Perfil tab is visually selected

#### Scenario: Rapid tab switching does not crash

- GIVEN bottom nav is visible
- WHEN the user taps three different tabs rapidly
- THEN the last selected tab displays without exception

#### Scenario: Router ViewModel resolves from DI without a MainTab binding

- GIVEN the Koin `appModule` graph as configured
- WHEN `MainRouterViewModel` is resolved from the container
- THEN resolution SHALL succeed without requiring any bound `MainTab` definition

#### Scenario: Entering the authenticated area does not crash after onboarding

- GIVEN a learner who just completed onboarding, or a cold start with onboarding already complete
- WHEN the authenticated area is entered
- THEN `AuthenticatedHomeScaffold` and `MainRouterViewModel` SHALL resolve without throwing
- AND the bottom-nav shell SHALL render with Inicio (HOME) selected by default

### Requirement: Profile Screen Layout

The system SHALL display hub layout: avatar (92px circle, coral initials, coral edit badge), name (21px/800), email (12px/500 muted), role chip (teal), **streak chip** ("Racha N días" coral with flame), four nav cards (18px radius, 42×42px colored SVG icon boxes, 13px radius), **logout card** (surface card, logout icon + centered text, 16px radius), version caption ("MathimApp · versión X.Y.Z"). Sub-screens via enum switcher. Loading/error preserved.
(Previously: Avatar, name, email, role chip, nav cards with text-symbol boxes, outline logout button, hardcoded "X" version.)

#### Scenario: Profile shows hub identity

- GIVEN user navigates to Perfil tab
- THEN avatar, name, email, role chip, streak chip visible

#### Scenario: Profile shows nav cards with icon boxes

- GIVEN hub view active
- THEN four nav cards with 18px radius, 42×42px colored SVG icon boxes

#### Scenario: Missing avatar uses placeholder

- GIVEN no `avatarUrl`
- THEN initials in coral on surface2 background

#### Scenario: Logout renders as card

- GIVEN hub view active
- THEN surface card with logout icon and "Cerrar sesión" text

#### Scenario: Version caption displays dynamic version

- GIVEN hub view active
- THEN "MathimApp · versión" + app version string

### Requirement: Client-Derived Gamification Metrics

The system SHALL derive level, XP, activity streak, and achievement status from existing local data. Level SHALL equal `totalScore / 100` (integer division). Activity streak SHALL equal `min(completedLessonIds.size, 7)`.

#### Scenario: Level derives from totalScore

- GIVEN user has `totalScore` of 350
- WHEN gamification metrics are computed
- THEN level SHALL be 3 and XP progress SHALL reflect 50 points toward next level

#### Scenario: Activity streak caps at 7

- GIVEN the user has 12 completed lessons
- WHEN the activity streak is computed
- THEN the system SHALL report a streak of 7

#### Scenario: Activity streak equals completed count when below cap

- GIVEN the user has 3 completed lessons
- WHEN the activity streak is computed
- THEN the system SHALL report a streak of 3

#### Scenario: Zero score yields level zero

- GIVEN `totalScore` is 0
- WHEN gamification metrics are computed
- THEN level SHALL be 0 and XP progress SHALL be 0%

### Requirement: Achievement Thresholds

The system SHALL evaluate achievements against progress thresholds. Each achievement SHALL have a name, icon placeholder, and locked/unlocked state.

#### Scenario: Achievement unlocks at threshold

- GIVEN an achievement requires 10 completed lessons
- AND the user has completed 10 lessons
- WHEN achievements are evaluated
- THEN the achievement SHALL be marked unlocked

#### Scenario: Achievement remains locked below threshold

- GIVEN an achievement requires 10 completed lessons
- AND the user has completed 3 lessons
- WHEN achievements are evaluated
- THEN the achievement SHALL be marked locked

### Requirement: Placeholder Tabs

The system SHALL render non-empty placeholder screens for Actividades and Progreso tabs with a title and "under development" message.

#### Scenario: Actividades tab shows placeholder

- GIVEN the user selects the Actividades tab
- WHEN the tab content renders
- THEN the system SHALL display a placeholder with title and "under development" message

#### Scenario: Progreso tab shows placeholder

- GIVEN the user selects the Progreso tab
- WHEN the tab content renders
- THEN the system SHALL display a placeholder with title and "under development" message

### Requirement: Inicio Tab Hosts HomeDashboardScreen

The system SHALL render the `HomeDashboardScreen` as Inicio tab content, replacing the legacy `CourseScreen`.
(Previously: Inicio tab hosted `CourseScreen` with its existing behavior.)

#### Scenario: Inicio displays dashboard content

- GIVEN the user selects the Inicio tab
- WHEN the tab content renders
- THEN the system SHALL display the `HomeDashboardScreen` with greeting, progress summary, and empty-state card

#### Scenario: Dashboard navigation to Activities works

- GIVEN the dashboard is visible on the Inicio tab
- WHEN the user taps a CTA that targets the Activities tab
- THEN the system SHALL switch to the Activities tab and display the course catalog

### Requirement: Hub Identity Data Fields

The system SHALL display `email` and `role` from `AuthSession.user` in the profile hub identity section. `ProfileUiState` SHALL expose `email` and `role` fields alongside existing gamification fields.

#### Scenario: Email and role render from auth session

- GIVEN an authenticated user with email and role populated
- WHEN the hub view composes
- THEN email and role chip display with values from `AuthSession.user`

#### Scenario: Missing role uses fallback

- GIVEN the user model has no role value
- THEN the role chip SHALL display a default or empty state without crashing

### Requirement: Loading and Error States Preserved

The system SHALL preserve the current loading indicator and error-message branches. Hub identity content SHALL render only after loading succeeds without an error.

#### Scenario: Loading state remains visible

- GIVEN `ProfileUiState.isLoading` is true
- THEN the loading indicator renders instead of hub content

#### Scenario: Error state remains visible

- GIVEN `ProfileUiState.errorMessage` is populated
- THEN the error message renders instead of hub content

### Requirement: Bottom Nav Shell Preserved

The system SHALL preserve the existing `Scaffold` with `NavigationBar` of four tabs (Inicio, Actividades, Progreso, Perfil) unchanged. Profile tab selection SHALL continue to show the ProfileScreen composable as before.

#### Scenario: Bottom nav structure unchanged

- GIVEN the authenticated area renders
- THEN the four-tab bottom navigation displays with identical tab labels and behavior

#### Scenario: Profile tab selection unchanged

- GIVEN bottom nav visible with another tab selected
- WHEN the user taps the Perfil tab
- THEN the ProfileScreen composes with Hub as the default sub-screen

### Requirement: Streak Chip Visual

The system SHALL display streak chip next to role chip: "Racha N días" coral text with flame icon, pill styling (surface bg, 1px line border, 999px radius).

#### Scenario: Streak chip renders with flame icon

- GIVEN authenticated user with streak
- WHEN hub composes
- THEN coral pill with flame icon and "Racha N días" next to role chip

#### Scenario: Streak chip matches role chip styling

- GIVEN both chips render
- THEN same height, padding, radius, border

### Requirement: Navigation Card Icon Boxes

Each nav card SHALL have colored SVG icon box: Cuenta (coral, person), Preferencias (teal, gear), Ayuda (rose, help-circle), Acerca de (muted, info). Box: 42×42px, 13px radius, surface2 bg.

#### Scenario: Cuenta card has coral icon box

- GIVEN hub renders
- THEN Cuenta card has 42×42px coral icon box with person SVG

#### Scenario: Preferencias card has teal icon box

- GIVEN hub renders
- THEN Preferencias card has 42×42px teal icon box with gear SVG

### Requirement: Sub-Screen List Row Icons

Sub-screen rows SHALL have leading 18px muted SVG icon matching row purpose.

#### Scenario: Cuenta rows have leading icons

- GIVEN Cuenta sub-screen renders
- THEN rows have leading 18px muted SVG icons (person, mail, lock)

#### Scenario: Preferencias rows have leading icons

- GIVEN Preferencias sub-screen renders
- THEN rows have leading 18px muted SVG icons (bell, volume, moon, globe)
