# onboarding-flow Specification

## Purpose

Define the mandatory multi-step onboarding flow shown after registration (and on first launch) before the user can access `CourseScreen`. The flow collects province, school year, and onboarding category to derive a curriculum-appropriate content list.

## Requirements

### Requirement: Mandatory Onboarding Gate

The system MUST display the onboarding flow after successful registration and MUST prevent access to `CourseScreen` until all onboarding steps are completed. The gate MUST evaluate onboarding completion for the currently authenticated user's `userId`, so a different account authenticating on the same device is never treated as onboarded based on another account's completion state. If the session's user id is missing or blank at gate-evaluation time, the system MUST treat the user as not onboarded and show the onboarding flow.
(Previously: the gate evaluated a single global onboarding-completion flag with no `userId` scoping.)

#### Scenario: Registration redirects to onboarding

- GIVEN the user completes registration successfully
- WHEN the auth session is established
- THEN the system SHALL navigate to the onboarding flow
- AND the system SHALL NOT show `CourseScreen`

#### Scenario: Incomplete onboarding blocks course access

- GIVEN the user has not completed onboarding
- WHEN the app attempts to resolve the post-auth view
- THEN the system SHALL display the onboarding flow
- AND the system SHALL NOT allow navigation to `CourseScreen`

#### Scenario: A different account on the same device is not treated as onboarded

- GIVEN account A previously completed onboarding on this device
- AND account B has never completed onboarding
- WHEN account B logs in on the same device
- THEN the system SHALL evaluate onboarding completion for account B's `userId`
- AND the system SHALL display the full onboarding wizard for account B
- AND the system SHALL NOT skip onboarding based on account A's completed state

#### Scenario: Returning to a previously onboarded account does not repeat onboarding

- GIVEN account A previously completed onboarding on this device
- WHEN account A logs back in on the same device
- THEN the system SHALL evaluate onboarding completion for account A's `userId`
- AND the system SHALL navigate directly to `CourseScreen`
- AND the system SHALL NOT re-display the onboarding flow

#### Scenario: A fresh registration always runs onboarding, even after a prior completed profile

- GIVEN a device has a prior completed profile belonging to account A
- WHEN a brand-new account is registered on that device
- THEN the system SHALL evaluate onboarding completion for the new account's `userId`
- AND the system SHALL display the full onboarding wizard for the new account

#### Scenario: Missing session user id fails toward showing onboarding

- GIVEN the auth session has no user id, or the user id is blank, at gate-evaluation time
- WHEN the app attempts to resolve the post-auth view
- THEN the system SHALL treat the user as not onboarded
- AND the system SHALL display the onboarding flow
- AND the system SHALL NOT navigate to `CourseScreen`

### Requirement: Province Selection Step

The system MUST present province selection as the first onboarding step and MUST require the user to select exactly one Argentine province before proceeding.

#### Scenario: Province step is displayed first

- GIVEN the user enters the onboarding flow
- WHEN the first step renders
- THEN the system SHALL display a list of Argentine provinces
- AND the system SHALL NOT show onboarding-category or school-year options yet

#### Scenario: Province selection enables next step

- GIVEN the province step is visible
- WHEN the user selects a valid province
- THEN the system SHALL advance to the onboarding-category selection step
- AND the system SHALL retain the selected province in onboarding state
(Previously: province selection advanced to the school-year step.)

### Requirement: Province-Based School-Year Rules

The system MUST derive valid `schoolYear` values from the selected province's school structure filtered by the selected onboarding category (`StudentTrack`). The school-year step MUST be the third step, presented after category selection. Validation SHALL use the following province mapping and year bands, further filtered to only the years whose `allowedTracks` include the selected category.

Primary-year mapping for this slice SHALL be:

| Primary years | Provinces |
|---|---|
| 6 | Buenos Aires, Catamarca, Chubut, Córdoba, Corrientes, Entre Ríos, Formosa, La Pampa, San Juan, San Luis, Tierra del Fuego, Tucumán |
| 7 | CABA, Chaco, Jujuy, La Rioja, Mendoza, Misiones, Neuquén, Río Negro, Salta, Santa Cruz, Santa Fe, Santiago del Estero |

Year-band rules for this slice SHALL be:

| Province structure | Primary | Secondary | Technical Secondary |
|---|---|---|---|
| 6-year primary | 1-6 | 7-12 | 7-13 |
| 7-year primary | 1-7 | 8-12 | 8-13 |

`Self-directed` is a non-narrowing category: the system MUST offer the full 1-12 range for `Self-directed`, unfiltered by the primary/secondary boundary, and MUST NOT include year 13 (technical-only) for `Self-directed`.

#### Scenario: Province defines the primary-to-secondary boundary

- GIVEN a province has been selected
- WHEN the school-year and category rules are evaluated
- THEN the system SHALL use the configured 6-year or 7-year province mapping
- AND the system SHALL place the first non-primary year at 7 for 6-year-primary provinces or 8 for 7-year-primary provinces

#### Scenario: School-year selection is required

- GIVEN the school-year step is visible
- WHEN no year option is selected
- THEN the system SHALL NOT allow proceeding to the confirmation step

#### Scenario: School-year list reflects the selected category

- GIVEN a province and a category have been selected
- WHEN the school-year step renders
- THEN the system SHALL display only the year options whose `allowedTracks` include the selected category
- AND the system SHALL NOT display years that are invalid for that category

#### Scenario: Self-directed shows the full unfiltered year range

- GIVEN a province has been selected
- AND the selected category is `Self-directed`
- WHEN the school-year step renders
- THEN the system SHALL display years 1 through 12
- AND the system SHALL NOT display year 13
- AND this range SHALL NOT be narrowed by the province's primary/secondary boundary
(Previously: the school-year step was second, presented directly after province selection, and the year list was derived from province alone with no category filter; category selection followed as the third step, gated by the chosen year.)

### Requirement: Onboarding Category Classification

The system MUST present exactly four onboarding category options: `Primary`, `Secondary`, `Technical Secondary`, `Self-directed`. The onboarding-category step MUST be the second step, presented immediately after province selection and before school-year selection. All four category options MUST be enabled once a province is selected, since track availability does not vary by province. The user MUST select exactly one category before proceeding to school-year selection.

#### Scenario: Four onboarding categories are available

- GIVEN the onboarding-category step is visible
- WHEN the options are displayed
- THEN the system SHALL show exactly: Primary, Secondary, Technical Secondary, Self-directed
- AND no other options SHALL be available

#### Scenario: Category selection is required

- GIVEN the onboarding-category step is visible
- WHEN no category is selected
- THEN the system SHALL NOT allow proceeding to the school-year step

#### Scenario: All categories are enabled regardless of selected province

- GIVEN a province has been selected
- WHEN the onboarding-category step renders
- THEN all four category options SHALL be selectable
- AND no category SHALL be disabled based on the selected province
(Previously: category was the third step, gated by a previously selected school year, with only categories matching that year enabled.)

### Requirement: Onboarding Step Order

The system MUST present onboarding steps in this order: Province, Category (`StudentTrack`), School year, Confirmation. Each step MUST reflect its position in this order in its rendered step number and title, independent of the underlying `OnboardingStep` enum ordering.

#### Scenario: Steps render in the fixed order

- GIVEN the user progresses through onboarding from the start
- WHEN each step is completed and the next step renders
- THEN the steps SHALL appear in this order: Province, Category, School year, Confirmation

### Requirement: Onboarding Back-Navigation Reset Semantics

When the user navigates backward from a step, the system MUST clear the selection made on the step being left and MUST return to the immediately preceding step in the fixed order (Province, Category, School year, Confirmation).

#### Scenario: Back from school-year clears the selected year and returns to category

- GIVEN the user is on the school-year step, having already selected a category
- WHEN the user navigates back
- THEN the system SHALL clear the selected school year
- AND the system SHALL return to the category step
- AND the previously selected category SHALL remain selected

#### Scenario: Back from category clears the selected track and returns to province

- GIVEN the user is on the category step, having already selected a province
- WHEN the user navigates back
- THEN the system SHALL clear the selected category
- AND the system SHALL return to the province step
- AND the previously selected province SHALL remain selected

#### Scenario: Changing category after selecting a year clears the stale year

- GIVEN the user has selected a province, a category, and a school year
- WHEN the user navigates back to the category step and selects a different category
- THEN the system SHALL clear the previously selected school year
- AND the school-year step SHALL show the year list derived from the new category on re-entry

### Requirement: Category Semantics

The system MUST treat onboarding category as metadata that validates the selected year against the province-derived ranges, but course filtering in this slice SHALL continue to use only the selected numeric `schoolYear`.

#### Scenario: Technical secondary extends valid year availability by one year

- GIVEN the user selected a province with a resolved school-structure mapping
- WHEN the selected `schoolYear` is the extra upper year beyond standard secondary
- THEN the system SHALL accept that year only when the category is `Technical Secondary`
- AND the same year SHALL be invalid for `Secondary`

#### Scenario: Technical secondary does not change course selection semantics

- GIVEN two users select the same numeric `schoolYear`
- AND one category is `Secondary`
- AND the other category is `Technical Secondary`
- WHEN the app requests official courses
- THEN the system SHALL use the same `schoolYear` filter value for both
- AND any difference in this slice SHALL be limited to which year values are selectable

#### Scenario: Self-directed is an explicit category

- GIVEN the user is learning outside formal school
- WHEN the onboarding-category step is displayed
- THEN the system SHALL allow selecting `Self-directed`
- AND the user SHALL still complete province and school-year selection for content recommendation

### Requirement: Diagnostic Questions Deferred

The system MUST NOT ask mastery, level, or diagnostic questions in this onboarding slice.

#### Scenario: No diagnostic questions are shown

- GIVEN the user completes the onboarding flow
- WHEN all steps are rendered in this slice
- THEN the system SHALL only ask for province, school year, and onboarding category
- AND no mastery or level questionnaire SHALL be shown

### Requirement: Onboarding Copy Renders in Spanish

The system MUST render all onboarding flow copy (step labels, options, buttons, helper text) in neutral Latin American Spanish by default, resolved via `stringResource(Res.string.*)` from `composeResources/values/strings.xml`. `values-en/strings.xml` exists only as a fallback resource set and MUST NOT be exposed via an in-app language switcher.

#### Scenario: Onboarding screen text is in Spanish

- GIVEN the user is on any onboarding step (province, school year, or onboarding category)
- WHEN the step renders under the default (no-qualifier) resource resolution
- THEN all visible labels, options, and buttons SHALL be Spanish text sourced from `values/strings.xml`
- AND no English literal SHALL be hardcoded in the onboarding composables

#### Scenario: No language switcher is exposed during onboarding

- GIVEN the user is anywhere in the onboarding flow
- WHEN the onboarding UI is inspected
- THEN no control SHALL allow switching the onboarding flow's display language
- AND English text SHALL only be reachable via device-level English locale resolution

#### Scenario: Interpolated onboarding copy remains correct in Spanish

- GIVEN an onboarding step displays interpolated text (e.g. a step counter like "Paso %d / 3")
- WHEN the step renders with a runtime step number
- THEN the Spanish string resource SHALL substitute the number via its placeholder
- AND the rendered sentence SHALL read as grammatically correct Spanish

### Requirement: Onboarding Completion and Navigation

The system MUST persist the onboarding outcome (province, school year, onboarding category) and MUST navigate to the authenticated home scaffold after all steps are completed. The persisted school year SHALL remain available to authenticated surfaces through `LearnerProfileRepository`; this change does not claim a `CourseScreen` or catalog-filter route that production does not expose. Onboarding state SHALL survive recomposition and device configuration changes without losing partial selections. The state holder SHALL be backed by a `ViewModel`. **SelectionCard components SHALL use 18px corner radius, 1px line border, and surface background**. **Action buttons SHALL use 16px corner radius with coral shadow**. All text SHALL use Sora.
(Previously: State survived recomposition and device configuration changes; no specific SelectionCard or button styling requirements.)

#### Scenario: Complete onboarding enters authenticated home

- GIVEN the user has selected province, school year, and onboarding category
- WHEN the user confirms completion
- THEN the system SHALL persist the onboarding profile locally
- AND the system SHALL navigate to the authenticated home scaffold
- AND the persisted school-year value SHALL remain available in the learner profile

#### Scenario: Onboarding state survives recomposition

- GIVEN the user is mid-onboarding (partial selections made)
- WHEN the composable recomposes
- THEN the system SHALL retain previously selected values
- AND the user SHALL NOT need to restart from the first step

#### Scenario: Onboarding state survives device rotation

- GIVEN the user is mid-onboarding with partial selections (e.g., province selected, school-year pending)
- WHEN the device rotates
- THEN the system SHALL remain on the current step
- AND all previously selected values SHALL be preserved
- AND the user SHALL NOT need to restart from the first step

### Requirement: Action Buttons Always Reachable

The system MUST ensure all step action buttons (Continue, Back, Complete) remain visible and reachable on screen at all times during the onboarding flow, regardless of content length or screen size. Scrollable step content SHALL occupy remaining space without pushing action buttons off-screen. **Continue and Complete buttons SHALL have 16px corner radius and coral shadow**. **Back buttons SHALL be 38×38px with 12px radius and surface2 background**.
(Previously: Buttons remained visible; no specific radius or shadow requirements.)

#### Scenario: Province list with Continue button visible

- GIVEN the user is on the province selection step
- WHEN the province list is rendered on any screen size
- THEN the Continue button SHALL remain visible at the bottom of the screen
- AND the province list SHALL scroll within the remaining space above the button

#### Scenario: Long content does not hide buttons

- GIVEN any onboarding step with scrollable content exceeding screen height
- WHEN the user scrolls to the bottom of the content
- THEN all action buttons SHALL remain visible and tappable
- AND no button SHALL be positioned below the visible viewport

#### Scenario: SelectionCard uses 18px radius

- GIVEN a SelectionCard renders for a province or school-year option
- WHEN inspected visually
- THEN the card SHALL have 18px corner radius and 1px line border
