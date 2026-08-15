# demo-course-seed Specification

## Purpose

Seed one official demo/QA course — plus its lessons and one exercise per payload type — at server startup, so every role has a stable, identifiable course to exercise the full rendering path. Seeding MUST be per-entity idempotent so repeated startups and newly added seed entities behave correctly.

## Requirements

### Requirement: Demo Course Seeding

The system MUST seed exactly one demo course when the `seedData` flag is enabled, using the same existing flag as other startup seed data (no separate gate). The demo course MUST have `isOfficial = true` and a title that clearly identifies it as a demo (e.g. contains "Demo").

#### Scenario: Demo course created on first seeded startup

- GIVEN `seedData` is enabled
- AND no demo course exists yet
- WHEN the server starts
- THEN a course with `isOfficial = true` and a "Demo"-labeled title is inserted
- AND the course has a stable, distinct id

#### Scenario: Demo course skipped when seedData disabled

- GIVEN `seedData` is disabled
- WHEN the server starts
- THEN no demo course is inserted

### Requirement: Demo Lesson and Exercise Coverage

The demo course MUST include at least one lesson with non-empty `theoryContent`, and MUST include at least one exercise of each payload type: MultipleChoice, InputValue, and MultiSelect.

#### Scenario: Demo lessons carry theory content

- GIVEN the demo course has been seeded
- WHEN its lessons are read
- THEN at least one lesson has non-empty `theoryContent`

#### Scenario: All three exercise payload types present

- GIVEN the demo course has been seeded
- WHEN its exercises are read
- THEN at least one exercise has a MultipleChoice payload
- AND at least one exercise has an InputValue payload
- AND at least one exercise has a MultiSelect payload

### Requirement: Per-Entity Seed Idempotency

The system MUST check existence per entity (by primary/stable id) before inserting each seeded admin user, course, lesson, or exercise, instead of short-circuiting the entire seed routine on a single existence check (e.g. "admin email already exists"). A newly added seed entity MUST be inserted even when other seed entities already exist from a prior run.

#### Scenario: Repeat startup inserts nothing new

- GIVEN the server has already completed seeding once
- WHEN the server restarts with `seedData` enabled
- THEN no duplicate rows are inserted
- AND no unique-constraint error is thrown

#### Scenario: New seed entity added after prior seeding

- GIVEN the server was previously seeded before the demo course existed in seed code
- AND the admin user and prior seed courses already exist in the database
- WHEN the server restarts with the demo course now defined in seed code
- THEN the demo course, its lessons, and its exercises are inserted
- AND pre-existing seed rows are left unchanged

### Requirement: Demo Course Readable by Every Role

The demo course MUST be readable end-to-end (course detail, lessons, and exercises) by STUDENT, TEACHER, and ADMIN roles, consistent with the `official` visibility tier.

#### Scenario: Each role reads the demo course content

- GIVEN the demo course has been seeded
- WHEN a STUDENT, a TEACHER, and an ADMIN each request the course's lessons
- THEN each role receives the lesson content without Forbidden errors
