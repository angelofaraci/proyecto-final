# Lesson Map UI Specification

## Purpose

Render lesson map as graphical path with circular node icons, progress bar header, and theory pill button. Replaces card-list layout.

## Requirements

### Requirement: Lesson Map Graphical Path Layout

The system SHALL render lesson map per `docs/ui/screens/mapa-leccion.png`: header (back arrow, lesson title + unit info, coral "Ver teoría" pill); linear progress bar (8px, teal on track, "N% Completado" / "X/Y Lecciones"); scrollable path with circular nodes on SVG polyline. Completed: teal circle + white checkmark. Current: coral circle + white play. Locked: gray (#CBBEAE) circle + white lock, dashed connecting line. All text: Sora.

#### Scenario: Header displays title and theory button

- GIVEN lesson map renders
- THEN lesson title, unit info, coral "Ver teoría" pill visible

#### Scenario: Progress bar shows completion

- GIVEN 3 of 8 completed
- THEN teal bar at 3/8, "37% Completado", "3/8 Lecciones"

#### Scenario: Completed nodes are teal with checkmarks

- GIVEN completed lessons exist
- THEN teal circular nodes with white checkmarks

#### Scenario: Current node is coral with play icon

- GIVEN current lesson exists
- THEN coral circular node with white play icon

#### Scenario: Locked nodes are gray with lock icons

- GIVEN locked lessons exist
- THEN gray (#CBBEAE) circular nodes with white lock icons

#### Scenario: Locked path uses dashed line

- GIVEN locked lessons after current
- THEN connecting line is dashed gray

#### Scenario: Nodes connect via SVG polyline

- GIVEN multiple nodes exist
- THEN nodes connected by SVG polyline per design

#### Scenario: Bottom navigation visible

- GIVEN lesson map renders
- THEN 4-tab bottom nav visible, Actividades selected

### Requirement: Theory Pill Button

The system SHALL display "Ver teoría" pill (coral, white text, 999px radius) in header. Tapping opens TheorySheet modal.

#### Scenario: Theory button opens theory sheet

- GIVEN lesson map renders
- WHEN "Ver teoría" tapped
- THEN TheorySheet modal opens with lesson content

### Requirement: Node Tap Behavior

Current and unlocked nodes are tappable and open the exercise player. Completed and locked nodes are non-interactive.

#### Scenario: Completed node is non-interactive

- GIVEN completed node visible
- WHEN tapped
- THEN no action

#### Scenario: Tapping current node opens exercise

- GIVEN current node visible
- WHEN tapped
- THEN exercise player opens

#### Scenario: Locked node is non-interactive

- GIVEN locked node visible
- WHEN tapped
- THEN no action

### Requirement: Back Navigation

The system SHALL display a back arrow in the header. Tapping returns to home through the existing navigation callback.

#### Scenario: Back arrow navigates to previous screen

- GIVEN lesson map renders
- WHEN back arrow tapped
- THEN navigates to home
