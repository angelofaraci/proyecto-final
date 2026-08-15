# lesson-read-access Specification

## Purpose

Control who can read lesson content via GET `/lessons/{id}` and GET `/courses/{courseId}/lessons` based on role and enrollment.

## Requirements

### Requirement: Lesson Read Visibility Tiers

The system SHALL enforce four visibility tiers when a user requests lesson content:

| Tier | Who Can Read | Condition |
|------|-------------|-----------|
| **official** | Any authenticated user | Course `isOfficial = true` |
| **enrolled** | Enrolled students | User enrolled in the course |
| **owner** | Course creator (TEACHER) | `course.creatorId == userId` |
| **admin** | Any ADMIN user | Role is `ADMIN` |

A TEACHER MUST also be able to read any course where `isOfficial = true`, regardless of `creatorId`, consistent with the `official` tier already granted to any authenticated user.
(Previously: TEACHER access was granted only when `course.creatorId == userId`, contradicting the `official` tier for non-owned official courses and causing Forbidden on official-course reads by non-creator teachers.)

#### Scenario: Admin reads any lesson

- GIVEN a lesson exists in any course
- WHEN an ADMIN requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Teacher reads own course lessons

- GIVEN a lesson belongs to a course created by the TEACHER
- WHEN the TEACHER requests the lesson
- THEN the lesson is returned with exercises (answers visible)

#### Scenario: Student reads official course lesson

- GIVEN a lesson belongs to an official course
- WHEN any authenticated STUDENT requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Enrolled student reads private course lesson

- GIVEN a lesson belongs to a non-official course
- AND the STUDENT is enrolled in that course
- WHEN the STUDENT requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Outsider student denied private lesson

- GIVEN a lesson belongs to a non-official course
- AND the STUDENT is NOT enrolled
- WHEN the STUDENT requests the lesson
- THEN Forbidden is returned

#### Scenario: Other teacher denied private lesson

- GIVEN a lesson belongs to a course created by a different TEACHER
- WHEN a non-creator TEACHER requests the lesson
- THEN Forbidden is returned

#### Scenario: Non-creator teacher reads official course lesson

- GIVEN a lesson belongs to a course with `isOfficial = true` created by a different TEACHER
- WHEN a non-creator TEACHER requests the lesson
- THEN the lesson is returned with exercises (answers visible)

#### Scenario: Non-existent lesson returns NotFound

- GIVEN no lesson exists with the requested ID
- WHEN any user requests the lesson
- THEN NotFound is returned

### Requirement: Course Lesson List Read Access

The system SHALL apply the same visibility tiers for GET `/courses/{courseId}/lessons`.

#### Scenario: Enrolled student lists private course lessons

- GIVEN a non-official course with lessons
- AND the STUDENT is enrolled
- WHEN the STUDENT requests the lesson list
- THEN all lessons are returned in order

#### Scenario: Outsider student denied course lesson list

- GIVEN a non-official course
- AND the STUDENT is NOT enrolled
- WHEN the STUDENT requests the lesson list
- THEN Forbidden is returned

### Requirement: Exercise Answers Hidden for Students

The system SHALL hide exercise correct answer fields when the requester has role `STUDENT`. For typed payloads, this means stripping `correctOptionId`, `correctValue`, or `correctOptionIds` from the respective payload type. The options/content of the exercise SHALL remain visible.
(Previously: Exercise `correctAnswer` field was set to empty string for students.)

#### Scenario: Student receives exercises without correct answers

- GIVEN a lesson with exercises
- WHEN a STUDENT reads the lesson
- THEN each exercise payload SHALL have its correct answer field removed

#### Scenario: Teacher receives visible answers

- GIVEN a lesson with exercises
- WHEN the course-creator TEACHER reads the lesson
- THEN each exercise payload SHALL contain all correct answer fields

### Requirement: Standalone Lesson Read Visibility

The system SHALL enforce visibility rules for standalone lessons (`courseId = null`) based on ownership and role.

| Role | Can Read Standalone Lesson | Condition |
|------|--------------------------|-----------|
| **admin** | Always | Any ADMIN user |
| **creator** | Always | `lesson.creatorId == userId` |
| **other** | Never | All other users |

#### Scenario: Admin reads standalone lesson

- GIVEN a standalone lesson exists
- WHEN an ADMIN requests the lesson
- THEN the lesson is returned with exercises (answers hidden)

#### Scenario: Creator reads own standalone lesson

- GIVEN a standalone lesson with `creatorId = X`
- WHEN user X requests the lesson
- THEN the lesson is returned with exercises (answers visible)

#### Scenario: Non-creator denied standalone lesson

- GIVEN a standalone lesson with `creatorId = X`
- WHEN a different user Y requests the lesson
- THEN Forbidden is returned

### Requirement: Standalone Lesson List Access

The system SHALL provide an admin-only endpoint to list standalone lessons. Non-admin users SHALL NOT access this list.

#### Scenario: Admin lists standalone lessons

- WHEN an ADMIN requests standalone lessons
- THEN the system SHALL return all lessons where `courseId = null`

#### Scenario: Non-admin cannot list standalone lessons

- GIVEN a TEACHER or STUDENT user
- WHEN the user requests the standalone lesson list
- THEN the system SHALL reject with 403

### Requirement: Typed Payload Answer Stripping for Students

The system MUST strip correct answer fields from typed exercise payloads when returning lesson content to users with role `STUDENT`. The stripping rules per payload type are:

| Payload Type | Fields to Strip |
|---|---|
| `MultipleChoicePayload` | `correctOptionId` |
| `InputValuePayload` | `correctValue` |
| `MultiSelectPayload` | `correctOptionIds` |

#### Scenario: Student receives MultipleChoice payload without correct answer

- GIVEN a lesson with a MultipleChoice exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctOptionId`
- AND the `options` list SHALL remain intact

#### Scenario: Student receives InputValue payload without correct value

- GIVEN a lesson with an InputValue exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctValue`

#### Scenario: Student receives MultiSelect payload without correct IDs

- GIVEN a lesson with a MultiSelect exercise
- WHEN a STUDENT reads the lesson
- THEN the exercise payload SHALL omit `correctOptionIds`
- AND the `options` list SHALL remain intact

#### Scenario: Admin receives full typed payload

- GIVEN a lesson with exercises of any type
- WHEN an ADMIN reads the lesson
- THEN each exercise payload SHALL include all correct answer fields

#### Scenario: Teacher (creator) receives full typed payload

- GIVEN a lesson with exercises of any type
- WHEN the course-creator TEACHER reads the lesson
- THEN each exercise payload SHALL include all correct answer fields
