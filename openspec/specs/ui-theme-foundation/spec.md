# Delta for UI Theme Foundation

## ADDED Requirements

### Requirement: Sora Typography System

The system SHALL use **Sora** bundled in `composeResources`. Scale: headlineLarge 32/800, headlineMedium 27/800, headlineSmall 21/800, titleLarge 17/700, titleMedium 14/700, bodyLarge 15/600, bodyMedium 13/600, bodySmall 12/500, labelMedium 12/600.

#### Scenario: Sora renders on Login screen

- GIVEN the Login screen composes
- THEN all text SHALL use Sora typeface

#### Scenario: Headline weights match design

- GIVEN a headline renders (e.g., "Hola de nuevo")
- THEN font weight SHALL be 800

### Requirement: Shape Token Alignment

The system SHALL update `ShapeTokens`: card 18dp, button 16dp, text field 15dp, checkbox 7dp, pill 999dp, icon box 13dp, social button 14dp, step segment 999dp.

#### Scenario: Card radius matches design

- GIVEN an MCard composes
- THEN corner radius SHALL be 18dp

#### Scenario: Button radius matches design

- GIVEN a filled MButton composes
- THEN corner radius SHALL be 16dp

#### Scenario: Text field radius matches design

- GIVEN an MTextField composes
- THEN corner radius SHALL be 15dp

#### Scenario: Checkbox radius matches design

- GIVEN terms checkbox on Register step 3
- THEN corner radius SHALL be 7dp

### Requirement: Semantic Color Tokens

The system SHALL add to `ColorTokens`: `track` (#EADFD1) for progress-bar empty, `lock` (#CBBEAE) for locked nodes, `stripe` (#F2E9DD) for diagram placeholders. Existing brand colors unchanged.

#### Scenario: Track color renders in progress bar

- GIVEN a linear progress bar at 45%
- THEN empty segment SHALL use `track` (#EADFD1)

#### Scenario: Lock color renders on locked nodes

- GIVEN a locked lesson node
- THEN node icon SHALL use `lock` (#CBBEAE)

### Requirement: CTA Button Shadow

The system SHALL add coral shadow (`0 12px 24px -10px rgba(242,101,75,0.42)`) to filled MButton via Compose `shadow` modifier.

#### Scenario: Login CTA has coral shadow

- GIVEN "Iniciar sesión" button renders
- THEN button SHALL display coral shadow

#### Scenario: Register CTA has coral shadow

- GIVEN "Continuar" or "Crear cuenta" renders
- THEN button SHALL display coral shadow

### Requirement: Selected Answer Glow

The system SHALL add coral glow (`0 6px 16px -8px rgba(242,101,75,0.42)`) to selected answer cards in Exercise Player.

#### Scenario: Selected answer card has glow

- GIVEN an answer is selected
- THEN card SHALL display coral border and glow

### Requirement: Divider Text Styling

The system SHALL render divider text at 12px/600 muted (#7C8790), flanked by 1px lines in line color (#EBE3D7).

#### Scenario: Auth divider matches design

- GIVEN social-login divider renders
- THEN text SHALL be 12px/600 muted with horizontal lines
