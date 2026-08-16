# Delta for frontend-auth

## MODIFIED Requirements

### Requirement: Login Screen UX

The system MUST render Login with Spanish copy, brand hero (6d infinity logo, 52×52px, 16px radius), field icons, password toggle, email validation, forgot-password link (13px/600 coral, right-aligned), visual-only social buttons. Footer link navigates to registration. **CTA ("Iniciar sesión"): 16px radius + coral shadow**. **Social buttons: 14px radius**. **Divider ("o continuá con"): 12px/600 muted with lines**. **"Registrate": 14px/700 coral**. All text: Sora.
(Previously: CTA 20dp radius no shadow; social 20dp; divider bodySmall; footer bold.)

#### Scenario: Password visibility toggle

- WHEN user toggles visibility on password field
- THEN field switches between masked and plain text

#### Scenario: Email-format validation

- WHEN non-email text entered
- THEN validation error displays and login button disabled

#### Scenario: Social buttons are non-functional

- WHEN Google or Apple button tapped
- THEN no OAuth or navigation occurs

#### Scenario: Forgot-password link

- WHEN forgot-password link tapped
- THEN system navigates to password recovery

#### Scenario: CTA has coral shadow

- GIVEN "Iniciar sesión" renders
- THEN 16px radius and coral shadow visible

#### Scenario: Social buttons have 14px radius

- GIVEN social buttons render
- THEN 14px corner radius visible

### Requirement: Register Screen 3-Step Wizard

The system MUST render Register as 3-step wizard with indicators, back nav, validation, password toggle, strength meter, terms acceptance. Data fields unchanged. **Step segments: 5px height, 999px radius**. **Step label: 12px/600 JetBrains Mono muted**. **Password strength: teal (#0E9E8E)**. **Terms checkbox: 22×22px, 7px radius, coral when checked**. **CTA: 16px radius + coral shadow**. All text: Sora.
(Previously: 6dp height primary color; labelMedium; primary strength; M3 Checkbox.)

#### Scenario: Step progression with valid input

- WHEN continue on step N with valid fields
- THEN advances to step N+1, indicator updates

#### Scenario: Back from step 1 to login

- WHEN back on step 1
- THEN navigates to Login, clears state

#### Scenario: Back from steps 2-3

- WHEN back on step 2 or 3
- THEN returns to previous step with data

#### Scenario: Password strength uses teal

- WHEN strong password typed
- THEN meter fills with teal (#0E9E8E)

#### Scenario: Terms checkbox is 22×22 coral

- WHEN terms checked on step 3
- THEN 22×22px box, 7px radius, coral with white checkmark

#### Scenario: Step segments are 5px

- GIVEN step indicator visible
- THEN segments 5px height, 999px radius

#### Scenario: Step label uses JetBrains Mono

- GIVEN step label visible
- THEN "Paso X / 3" in 12px/600 JetBrains Mono muted

#### Scenario: Per-step validation blocks progression

- WHEN continue with invalid fields
- THEN validation errors display, no advance
