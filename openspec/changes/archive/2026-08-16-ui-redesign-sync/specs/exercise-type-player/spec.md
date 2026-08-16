# Delta for exercise-type-player

## MODIFIED Requirements

### Requirement: Type-Specific Player Dispatch

The system MUST render player composable based on `ExercisePayload` type (MultipleChoice, InputValue, MultiSelect). **Header**: close X (left), lesson title + question counter (center), **3 rose hearts** (right, lives), **linear progress bar** (8px, coral on track) below. **Question card**: surface, 22px radius, 1px line border. **Answers**: 2-column grid, 16px radius cards. **Hint**: "Pista" link with lightbulb below answers. **CTA**: bottom-fixed "Confirmar", 18px radius, coral shadow. All text: Sora.
(Previously: "Back to lesson map" button, no lives, no progress bar, single-column list, no hint, full-width MButton 20dp no shadow.)

#### Scenario: MultipleChoice renders in 2-column grid

- GIVEN MultipleChoicePayload with N options
- THEN N answer cards in 2-column grid, single-select

#### Scenario: InputValue renders text input

- GIVEN InputValuePayload
- THEN text input and submit button render

#### Scenario: MultiSelect renders in 2-column grid

- GIVEN MultiSelectPayload with N options
- THEN N answer cards in 2-column grid, multi-select

#### Scenario: Unknown payload shows error

- GIVEN unrecognized payload type
- THEN fallback error placeholder without crash

#### Scenario: Header shows hearts

- GIVEN player renders
- THEN 3 rose hearts at top-right

#### Scenario: Header shows progress bar

- GIVEN player renders
- THEN linear coral progress bar (8px) below title

#### Scenario: Question card has 22px radius

- GIVEN question area renders
- THEN 22px radius, 1px line border

#### Scenario: Selected answer has coral glow

- GIVEN answer selected
- THEN coral border and glow

#### Scenario: Hint link appears

- GIVEN answer section renders
- THEN "Pista" link with lightbulb below grid

#### Scenario: Confirmar CTA bottom-fixed with shadow

- GIVEN player renders
- THEN "Confirmar" button, 18px radius, coral shadow, fixed bottom

### Requirement: Type-Specific Answer Validation

Validation logic unchanged (functional behavior preserved).

#### Scenario: MultipleChoice validates single option

- GIVEN correctOptionId = "B"
- WHEN option B selected and submitted
- THEN marked correct

#### Scenario: InputValue validates trimmed text

- GIVEN correctValue = "42"
- WHEN " 42 " submitted
- THEN marked correct

#### Scenario: MultiSelect validates exact set

- GIVEN correctOptionIds = ["A", "C"]
- WHEN exactly A and C selected
- THEN marked correct

#### Scenario: MultiSelect rejects partial

- GIVEN correctOptionIds = ["A", "C"]
- WHEN only A selected
- THEN marked incorrect

#### Scenario: InputValue rejects empty

- GIVEN InputValue exercise
- WHEN empty string submitted
- THEN rejected client-side

### Requirement: Wrong-Answer Immediate Retry

System provides feedback and allows retry. **Lives decrement visually on wrong answers**.

#### Scenario: Wrong answer shows feedback, stays on exercise

- GIVEN incorrect answer submitted
- THEN feedback displays, exercise remains, one heart removed

#### Scenario: Correct answer advances

- GIVEN correct answer submitted
- THEN marked completed, advances to next

#### Scenario: Retry allows selection change

- GIVEN wrong option selected
- THEN student can change selection

#### Scenario: Multiple wrong attempts don't block

- GIVEN multiple incorrect answers
- WHEN eventually correct
- THEN marked completed, advances

### Requirement: Answer Hiding in Player Payload

No correct answer data in student payloads (functional behavior preserved).

#### Scenario: Student payload omits correct answer

- GIVEN student loads lesson
- THEN correctOptionId, correctValue, correctOptionIds absent or null

#### Scenario: Admin payload includes correct answer

- GIVEN admin loads exercise for editing
- THEN all correct answer fields present
