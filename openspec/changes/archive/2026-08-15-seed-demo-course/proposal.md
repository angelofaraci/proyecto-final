# Proposal: Seed a Demo/QA Course Visible to Every Role

## Intent

There is no throwaway course that exercises the full rendering path (course → lessons → theory markdown → each exercise payload type) for manual QA and demos. Two defects block it today:

1. `canReadCourseContent` grants TEACHER access only when `creatorId == userId`, so teachers get Forbidden on official-course detail and lessons even though the catalog (`getOfficialCourses`) lists them. This contradicts the existing `lesson-read-access` spec tier table ("official → any authenticated user").
2. `SeedData.seedOfficialCourses()` short-circuits on "admin email already exists", so any newly added seed course is silently skipped in every environment already seeded once.

## Scope

### In Scope
- Seed one demo/QA course (distinct id, `isOfficial = true`) with a handful of lessons carrying `theoryContent`, plus exercises covering MultipleChoice, InputValue and MultiSelect payloads.
- Fix TEACHER branch of `canReadCourseContent` to allow `isOfficial` courses.
- Make seeding per-entity idempotent (admin user, course, lessons, exercises each inserted only if absent) instead of one global short-circuit.
- Server tests covering teacher read of official content and repeat-seed idempotency.

### Out of Scope
- Admin-web content-authoring UI.
- New content types, entities, or `shared` model changes.
- Flyway INSERT migrations (no precedent; seeding stays Kotlin).
- Changing enrollment, answer-stripping, or write permissions.

## Capabilities

### New Capabilities
- `demo-course-seed`: startup seeding of a demo/QA course, its lessons, and one exercise per payload type, idempotent per entity.

### Modified Capabilities
- `lesson-read-access`: TEACHER may read content of any `isOfficial` course, not only own-created courses.

## Approach

Extend `SeedData.kt`: split `seedOfficialCourses()` into per-entity `ensureX` helpers keyed on primary id, then add a `seedDemoCourse()` block. Change the TEACHER branch to `access.creatorId == userId || access.isOfficial`. Answers stay visible to teachers (existing `shouldHideLessonAnswers` unchanged).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `server/.../seed/SeedData.kt` | Modified | Per-entity idempotency + demo course data |
| `server/.../service/ContentReadAccess.kt` | Modified | One-line TEACHER visibility fix |
| `server/src/test/.../ServiceLayerTest.kt` | Modified | Access + seed tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Demo course appears in production catalogs | Med | Distinct ids and clear "Demo" titling; gate via existing `seedData` flag |
| Broader teacher visibility misread as write access | Low | Change is read-only path; write guards untouched |
| Seed rewrite breaks existing arithmetic seed | Low | Keyed inserts, repeat-run test |

## Rollback Plan

Revert the commit. Seed rows are additive with distinct ids; delete them by id if a deployed DB must be cleaned. The access change is a single boolean clause with no persisted state.

## Dependencies

- `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD` must remain configured for startup seeding.

## Success Criteria

- [ ] Demo course, lessons and all three exercise payload types readable by STUDENT, TEACHER and ADMIN.
- [ ] Second server startup inserts nothing new and throws no constraint error.
- [ ] `./gradlew :server:test` passes.
