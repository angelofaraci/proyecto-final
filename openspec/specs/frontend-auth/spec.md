# frontend-auth Specification

## Purpose

Provide login/register entry points for public users, keep the active session in memory, and route authenticated users to `HomeDashboardScreen`.

## Requirements

### Requirement: Auth Entry Flow

The system MUST show the Login screen by default with Spanish copy and brand hero. Switching between Login and Register SHALL occur via a footer link on Login and back navigation from step 1 of the Register wizard.
(Previously: The system showed Login screen by default and allowed switching via in-screen text links.)

#### Scenario: Default state is login

- GIVEN the app starts with no in-memory token
- WHEN the auth area is rendered
- THEN the Login screen SHALL be visible with Spanish copy and brand hero

#### Scenario: Footer link switches to register

- GIVEN the Login screen is visible
- WHEN the user selects the register footer link
- THEN the Register screen SHALL replace it at step 1

### Requirement: Public Registration Uses Student Role

The system MUST NOT expose role selection during public registration and MUST create new public accounts as `STUDENT` users only.

#### Scenario: Register form has no role picker

- GIVEN the user opens the Register screen
- WHEN the form is displayed
- THEN the system SHALL not ask the user to choose a role

#### Scenario: Successful registration creates a student account

- GIVEN the user submits valid registration data
- WHEN the server returns success
- THEN the created account SHALL be treated as `STUDENT`
- AND the system SHALL continue with the authenticated flow

### Requirement: Successful Authentication Enters the App

The system MUST send login and registration requests through the auth API, MUST store the returned token in memory for the current app process, and MUST show the onboarding flow after registration if onboarding is not complete, or `HomeDashboardScreen` if onboarding is already complete.
(Previously: After successful authentication, the system showed `CourseScreen` directly if onboarding was complete.)

#### Scenario: New user registers and must complete onboarding

- GIVEN the user submits valid registration data
- WHEN the server returns an auth response
- THEN the system SHALL store the token in memory
- AND the system SHALL check if onboarding is complete
- AND if onboarding is NOT complete, the system SHALL show the onboarding flow
- AND the system SHALL NOT show `HomeDashboardScreen` until onboarding completes

#### Scenario: Returning user with completed onboarding enters dashboard

- GIVEN the user has a valid auth session
- AND onboarding was previously completed
- WHEN the app resolves the post-auth view
- THEN the system SHALL show `HomeDashboardScreen` directly
- AND the system SHALL NOT show the onboarding flow

#### Scenario: Login success with incomplete onboarding

- GIVEN the user logs in successfully
- AND onboarding is not complete for this session
- WHEN the auth response is received
- THEN the system SHALL store the token in memory
- AND the system SHALL show the onboarding flow

### Requirement: Raw Auth Errors Are Visible

The system MUST surface raw server error text for failed login or registration attempts.

#### Scenario: Duplicate email is shown

- GIVEN the server rejects registration with an error body
- WHEN the response is handled
- THEN the system SHALL display that text to the user

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

### Requirement: Auth Screen Primitives

`MTextField` MUST support focus glow, 15px corner radius, and leading/trailing icons. `MButton` MUST support disabled state opacity and social-button visual style.

#### Scenario: MTextField focus glow

- GIVEN an `MTextField` receives focus
- THEN the field SHALL display a focus glow and change border color

#### Scenario: MTextField trailing icon

- GIVEN an `MTextField` has a trailing icon configured
- WHEN rendered
- THEN the icon SHALL appear inside the field at the right edge

#### Scenario: MButton disabled state

- GIVEN an `MButton` is disabled
- WHEN rendered
- THEN the button SHALL display at ~0.5 opacity and SHALL NOT respond to tap

### Requirement: Auth Gate Survives Configuration Changes

The system MUST ensure the auth gate target and DI context survive device configuration changes (rotation, locale change, dark-mode toggle) without resetting the auth flow or creating divergent `AuthRepository` instances between the auth gate and child ViewModels. Koin startup SHALL occur at the platform entry point, not inside Compose. The auth gate router SHALL be backed by a `ViewModel` instance.

#### Scenario: Login survives device rotation

- GIVEN the user is on the Login screen with credentials entered
- WHEN the device rotates
- THEN the Login screen SHALL remain visible with entered data preserved
- AND the system SHALL NOT reset to a different auth target

#### Scenario: Registration step survives rotation

- GIVEN the user is mid-registration (step 2 or 3 of the wizard)
- WHEN the device rotates
- THEN the system SHALL remain on the same registration step
- AND the system SHALL NOT revert to the Login screen
- AND previously entered registration data SHALL be preserved

#### Scenario: DI singleton consistency after rotation

- GIVEN the app is running with an active Koin context
- WHEN a configuration change triggers recomposition
- THEN the `AuthRepository` instance observed by the auth gate SHALL be the same instance observed by child ViewModels
- AND no duplicate Koin context SHALL be created

#### Scenario: Auth gate target consistency after rotation

- GIVEN the auth gate has resolved to the Register target
- WHEN the device rotates
- THEN the auth gate SHALL continue targeting Register
- AND the system SHALL NOT revert to the Login target
