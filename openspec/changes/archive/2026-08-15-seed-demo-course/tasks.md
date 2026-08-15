# Tasks: Seed a Demo/QA Course Visible to Every Role

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~220-280 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Refactor seed + fix TEACHER access + add tests | PR 1 | `./gradlew :server:test --tests "*ServiceLayerTest*"` | `./gradlew :server:test` (in-memory H2, ServerIntegrationTest boots real app) | Revert commit; demo rows are additive with distinct ids (`course-demo-qa`, `lesson-demo-*`, `ex-demo-*`) |

## Phase 1: Refactor Seed Idempotency (Foundation)

- [x] 1.1 In `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt`, add `ensureAdminUser()` returning the effective admin id (reuse existing row by email/id, else insert); remove the global "Seed data already exists, skipping..." short-circuit (line ~42).
- [x] 1.2 Add `ensureCourse(id, block)`, `ensureLesson(id, block)`, `ensureLegacyExercise(id, ...)` (delegates to `ExercisePayloadSupport.legacyPayloadJson` exactly as today), each doing `if (table.selectAll().where { id eq x }.empty()) insert`.
- [x] 1.3 Wrap the existing "Basic Arithmetic" course/lessons/exercises (including the `TRUE_FALSE` row `ex-add-3`) in the new `ensureX` helpers with byte-identical data — no field value changes.
- [x] 1.4 Keep `println("Seeding official courses...")` and `println("Seed data created successfully!")` unconditional and unchanged so `ServerIntegrationTest` stdout assertions keep passing.

## Phase 2: Demo Course Content

- [x] 2.1 Add `ensureTypedExercise(id, ..., payload: ExercisePayload)` deriving legacy `options`/`correctAnswer` via `ExercisePayloadSupport.toLegacyOptionsCsv`/`toLegacyCorrectAnswer`, and `payload` via `serializePayload`.
- [x] 2.2 Add private `seedDemoCourse(adminId)` inserting course `course-demo-qa` (title contains "Demo", `isOfficial = true`, `creatorId = adminId`, `joinCode = "DEMOQA1"`, topic `Demo`, schoolYear 3) via `ensureCourse`.
- [x] 2.3 Add lesson `lesson-demo-theory` (orderIndex 0, non-empty `theoryContent`, no exercises) and `lesson-demo-exercises` (orderIndex 1, non-empty `theoryContent`) via `ensureLesson`.
- [x] 2.4 Add exercise `ex-demo-mc-1` (`MULTIPLE_CHOICE`, `MultipleChoicePayload` with ≥2 `ChoiceOption` + `correctOptionId`) via `ensureTypedExercise`.
- [x] 2.5 Add exercise `ex-demo-input-1` (`INPUT_VALUE`, `InputValuePayload(placeholder, correctValue)`) via `ensureTypedExercise`.
- [x] 2.6 Add exercise `ex-demo-multi-1` (`MULTI_SELECT`, `MultiSelectPayload` with ≥2 options and ≥1 `correctOptionIds`) via `ensureTypedExercise`.
- [x] 2.7 Call `seedDemoCourse(adminId)` from `seedOfficialCourses()` after the arithmetic seed block, still gated only by the existing `seedData` flag (no new env var).

## Phase 3: Fix TEACHER Read Access

- [x] 3.1 In `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt`, change the TEACHER branch (line ~24) from `access.creatorId == userId` to `access.creatorId == userId || access.isOfficial`.
- [x] 3.2 Verify the sibling branch at line ~33 (answer-visibility `shouldHideLessonAnswers`) is unaffected — confirm it still keys off `TEACHER, STUDENT -> access.creatorId == userId` unchanged (owner-only hides nothing extra; official-teacher answers remain visible per spec).

## Phase 4: Testing

- [x] 4.1 RED: In `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt`, extend `lesson read access follows role and enrollment visibility` with `getLessonByIdForUser("official-lesson", "teacher-other", TEACHER)` expecting `Success` — run and confirm it fails against current code.
- [x] 4.2 GREEN: confirm 4.1 passes after Phase 3 fix; keep the existing `teacher-lesson`/`teacher-other` non-official-course case asserting `Forbidden`.
- [x] 4.3 Add `getCourseByIdForUser("official-course", "teacher-other", TEACHER)` → `Success` assertion alongside 4.1.
- [x] 4.4 Add a seed idempotency test: call `SeedData.seedOfficialCourses()` twice against `initServiceTestDatabase()` with `seed.admin.*` system properties set; assert no exception and identical `Users`/`Courses`/`Lessons`/`Exercises` row counts after the second call.
- [x] 4.5 Add a "new entity after prior seed" test: pre-insert the admin user manually (configured email), seed once, then assert `course-demo-qa` and its lessons/exercises now exist and the pre-existing admin row id is unchanged.
- [x] 4.6 Add a demo-content materialization test: assert `course-demo-qa.isOfficial == true`, both demo lessons have non-blank `theoryContent`, and `materializePayload` on the three demo exercises yields `MultipleChoicePayload`, `InputValuePayload`, and `MultiSelectPayload` respectively.
- [x] 4.7 Add a regression assertion in the same seed test: `course-basic-arithmetic` and `ex-add-3` (`TRUE_FALSE`) still exist with unchanged legacy `options`/`correctAnswer` columns.
- [x] 4.8 Run `./gradlew :server:test` (covers `ServerIntegrationTest` stdout-log assertions plus all `ServiceLayerTest` cases) and confirm full green.
