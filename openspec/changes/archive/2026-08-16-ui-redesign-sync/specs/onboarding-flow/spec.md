# Delta for onboarding-flow

## MODIFIED Requirements

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
