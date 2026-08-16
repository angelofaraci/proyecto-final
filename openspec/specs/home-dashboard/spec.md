# home-dashboard Specification

## Purpose

Provide the authenticated landing screen on the HOME tab with greeting, progress summary, empty-state learning card, and navigation CTA. Frontend-only — no new backend contracts.

## Requirements

### Requirement: Dashboard Greeting

The system SHALL display time-based greeting with user name. **Streak chip inline** ("+N días" coral pill). **Subtitle below greeting**. All text: Sora.
(Previously: Greeting with name only, no streak chip or subtitle.)

#### Scenario: Greeting shows name with streak

- GIVEN user "María" with streak 7 at 10:00
- THEN "Hola, María 👋" with coral "+7 días" pill

#### Scenario: Missing name uses fallback

- GIVEN no display name
- THEN generic salutation without name

### Requirement: Progress Summary Chip

The system SHALL render a progress card: level label (14px/700), linear progress bar (8px, 999px radius, teal on track), XP text (12px/600 teal). Card: 18px radius, 1px line border.
(Previously: Compact chip with level and activity count.)

#### Scenario: Level and XP with progress bar

- GIVEN level 5, 340/500 XP
- THEN "Nivel 5", teal bar at 68%, "340 / 500 XP"

#### Scenario: Zero progress uses the configured XP denominator

- GIVEN `currentXp` is 0 and `xpForNextLevel` is 100
- THEN "Nivel 0", empty bar, "0 / 100 XP"

### Requirement: Empty-State Learning Card

The system SHALL render "Continuar aprendiendo" card when no in-progress lesson. **When courses in progress: "MIS CURSOS EN PROGRESO" header + course cards**. Course card: circular icon, title, progress %, teal "Ir" pill. All cards: 18px radius, 1px line border.
(Previously: Single card with illustration, title, description, CTA.)

#### Scenario: Empty-state card when no lesson

- GIVEN no in-progress lesson
- THEN empty-state card with illustration, title, description, CTA

#### Scenario: CTA navigates to Activities

- GIVEN empty-state card visible
- WHEN CTA tapped
- THEN navigates to Activities tab

#### Scenario: In-progress courses render as cards

- GIVEN courses with progress
- THEN "MIS CURSOS EN PROGRESO" header and course cards

#### Scenario: Course card shows progress and "Ir"

- GIVEN "Fracciones - Básico" at 45%
- THEN icon, title, "Progreso: 45%", teal "Ir" pill

### Requirement: Lesson Map CTA

The system SHALL provide a secondary CTA navigating to the lesson map in the Activities tab. This change does not claim a catalog destination; a course catalog requires its own production route.

#### Scenario: Lesson map CTA navigates correctly

- GIVEN dashboard visible
- WHEN lesson map CTA tapped
- THEN navigates to Activities with the lesson map view

### Requirement: Synthetic Streak Display

The system SHALL display streak from `completedLessonIds.size`, capped at 7. **Streak appears as coral pill ("+N días") inline with greeting**.
(Previously: Count without date-continuity implication.)

#### Scenario: Streak equals completed count below cap

- GIVEN 3 completed lessons
- THEN reports 3

#### Scenario: Streak caps at 7

- GIVEN 12 completed lessons
- THEN reports 7

#### Scenario: Streak chip renders on dashboard

- GIVEN computed streak
- THEN coral "+N días" pill inline with greeting
