# Delta for profile-screen

> Authority: this Profile v2 navigation-hub delta is the conformance target. The structurally different `perfil-usuario.png` concept is superseded for this change and requires a separate specification if revived.

## MODIFIED Requirements

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

## ADDED Requirements

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
