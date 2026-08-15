# Delta for lesson-read-access

## MODIFIED Requirements

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

- GIVEN a lesson belongs to a non-official, non-owned course created by a different TEACHER
- WHEN a non-creator TEACHER requests the lesson
- THEN Forbidden is returned

#### Scenario: Non-existent lesson returns NotFound

- GIVEN no lesson exists with the requested ID
- WHEN any user requests the lesson
- THEN NotFound is returned

#### Scenario: Non-creator teacher reads official course lesson

- GIVEN a lesson belongs to a course with `isOfficial = true` created by a different TEACHER
- WHEN a non-creator TEACHER requests the lesson
- THEN the lesson is returned with exercises (answers visible)
