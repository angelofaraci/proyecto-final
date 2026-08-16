# Delta for profile-hub-navigation

## MODIFIED Requirements

### Requirement: Stubbed Sub-Screen Composables

The system SHALL render stubbed composables for Cuenta, Preferencias, Ayuda, and Acerca de. Each stub SHALL display a header bar with back button (38×38px, 12px radius, surface2 background), centered title (17px/700), and 38px spacer. Content rows SHALL display within an 18px-radius surface card with 1px line border. **Preferencias stub SHALL include a dark-mode toggle row** (moon icon, "Modo oscuro" label, Switch with track color) as a visual placeholder — the toggle SHALL be a no-op with a TODO comment.
(Previously: Stubs displayed a title, placeholder text, and TODO comment with no header bar or icon rows.)

#### Scenario: Sub-screen header bar renders

- GIVEN a sub-screen (e.g., Cuenta) is active
- THEN a header bar with back button, centered title, and spacer renders at the top

#### Scenario: Preferencias stub includes dark-mode toggle placeholder

- GIVEN the Preferencias sub-screen is active
- THEN a row with moon icon, "Modo oscuro" label, and a Switch toggle renders
- AND the toggle SHALL be a no-op with a TODO comment

#### Scenario: Cuenta stub renders with icon rows

- GIVEN the Cuenta sub-screen is active
- THEN rows with leading icons for name, email, and password render inside an 18px-radius card

#### Scenario: Ayuda stub renders with TODO

- GIVEN the Ayuda sub-screen is active
- THEN a screen with header bar, placeholder content, and TODO comment renders

#### Scenario: AcercaDe stub renders with TODO

- GIVEN the AcercaDe sub-screen is active
- THEN a screen with header bar, placeholder content, and TODO comment renders
