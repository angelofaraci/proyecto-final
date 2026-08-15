# Design: Seed a Demo/QA Course Visible to Every Role

## Technical Approach

Two isolated server-side edits plus tests. `SeedData.seedOfficialCourses()` is refactored from one global "admin email exists → return" short-circuit into per-entity `ensureX` helpers keyed on primary id, all inside the existing single `transaction { }`. A new private `seedDemoCourse(adminId)` block appends the demo course. `canReadCourseContent`'s TEACHER branch gains `|| access.isOfficial`. No new gate: seeding still runs only when `Main.kt` passes `seedData = true`. No schema change, no Flyway migration, no `shared` model change.

## Architecture Decisions

| Decision | Choice | Rejected alternative | Rationale |
|---|---|---|---|
| Idempotency granularity | `ensureUser/ensureCourse/ensureLesson/ensureLegacyExercise/ensureTypedExercise`, each `if (table.selectAll().where { id eq x }.empty()) insert` | Keep global short-circuit; or `insertIgnore`/upsert | Per-id checks satisfy spec "new seed entity added after prior seeding"; explicit selects match the existing Exposed usage in this file and stay portable across H2/PostgreSQL. |
| Two exercise helpers | `ensureLegacyExercise(... optionsCsv, correctAnswer)` for the arithmetic course (delegates to `ExercisePayloadSupport.legacyPayloadJson` exactly as today) and `ensureTypedExercise(... payload: ExercisePayload)` for demo rows | One typed helper used everywhere | Keeps every existing "Basic Arithmetic" row (including the `TRUE_FALSE` one) byte-identical, so `ServerIntegrationTest`/`ServiceLayerTest` assertions and legacy backfill behaviour do not shift. |
| Typed legacy columns | `ensureTypedExercise` derives `options`/`correctAnswer` via `ExercisePayloadSupport.toLegacyOptionsCsv` / `toLegacyCorrectAnswer`, and `payload` via `serializePayload` | Hand-written CSV literals | Single source of truth; guarantees legacy columns and typed payload agree. |
| Admin resolution | `ensureAdminUser()` returns the effective admin id: reuse the existing row's id when the configured email or id already exists, else insert | Always assume configured id | Prevents a duplicate-email insert and keeps `Courses.creatorId` pointing at the real admin row on already-seeded databases. |
| Seed logging | Keep printing `Seeding official courses...` and `Seed data created successfully!` unconditionally; drop the "already exists, skipping" early return | Log only on actual inserts | `ServerIntegrationTest` asserts both lines on a seeded startup; keeping them unconditional avoids touching that test. |
| Demo gating | Existing `seedData` flag + `course-demo-*` ids + "Demo" in the title | New `SEED_DEMO_COURSE` env var | Resolved decision; avoids a new config surface, and identity is carried by naming. |

## Data Flow

    Main.module(seedData=true) ──→ SeedData.seedOfficialCourses()
                                        │  transaction {
                                        │    ensureAdminUser() ──→ adminId
                                        │    seedBasicArithmetic(adminId)   (unchanged data)
                                        │    seedDemoCourse(adminId)        (new)
                                        │  }
                                        ↓
    Courses / Lessons / Exercises ──→ CourseService / LessonService
                                        └─→ canReadCourseContent(role) ──→ STUDENT | TEACHER | ADMIN

## File Changes

| File | Action | Description |
|---|---|---|
| `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` | Modify | Add `ensureX` helpers; wrap existing arithmetic inserts in them unchanged; add `seedDemoCourse` |
| `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt` | Modify | TEACHER branch → `access.creatorId == userId \|\| access.isOfficial` |
| `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` | Modify | Teacher-on-official read assertions + seed idempotency/demo-content tests |

## Interfaces / Contracts

Access fix (only functional line changed):

```kotlin
UserRole.TEACHER -> access.creatorId == userId || access.isOfficial
```

Seed helper shape (all called inside the existing single transaction):

```kotlin
private fun ensureCourse(id: String, block: Courses.(InsertStatement<Number>) -> Unit) {
    if (Courses.selectAll().where { Courses.id eq id }.empty()) Courses.insert(block)
}
```

Demo entities (stable ids, `isOfficial = true`, `creatorId = adminId`, `joinCode = "DEMOQA1"`):

| Id | Kind | Notes |
|---|---|---|
| `course-demo-qa` | Course | Title `Demo QA Course`, topic `Demo`, schoolYear 3 |
| `lesson-demo-theory` | Lesson | orderIndex 0, markdown `theoryContent`, no exercises |
| `lesson-demo-exercises` | Lesson | orderIndex 1, markdown `theoryContent` |
| `ex-demo-mc-1` | Exercise | `MULTIPLE_CHOICE` + `MultipleChoicePayload` (≥2 `ChoiceOption`, `correctOptionId`) |
| `ex-demo-input-1` | Exercise | `INPUT_VALUE` + `InputValuePayload(placeholder, correctValue)` |
| `ex-demo-multi-1` | Exercise | `MULTI_SELECT` + `MultiSelectPayload` (≥2 options, ≥1 `correctOptionIds`) |

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit (`ServiceLayerTest`) | TEACHER reads official content | Extend `lesson read access follows role and enrollment visibility`: `getLessonByIdForUser("official-lesson", "teacher-other", TEACHER)` → `Success`; add `getCourseByIdForUser("official-course", "teacher-other", TEACHER)` → `Success`; keep the existing `teacher-lesson`/`teacher-other` → `Forbidden` assertion so non-official ownership stays enforced |
| Integration (`ServiceLayerTest`, new seed test class) | Demo seed content | `initServiceTestDatabase()`, set `seed.admin.*` system properties, call `SeedData.seedOfficialCourses()`, assert `course-demo-qa` is official, both demo lessons have non-blank `theoryContent`, and `materializePayload` on the three demo exercises yields `MultipleChoicePayload`, `InputValuePayload`, `MultiSelectPayload` |
| Integration | Repeat-seed idempotency | Call `seedOfficialCourses()` twice; assert no exception and identical `Users`/`Courses`/`Lessons`/`Exercises` row counts |
| Integration | New entity after prior seed | Pre-insert the admin user (configured email) manually, then seed once; assert `course-demo-qa` and its lessons/exercises exist and the pre-existing admin row is unchanged |
| Regression | Arithmetic seed intact | Same seed test asserts `course-basic-arithmetic` plus `ex-add-3` (`TRUE_FALSE`) still present with unchanged legacy columns; `./gradlew :server:test` covers `ServerIntegrationTest` seed-output assertions |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. Inserts are additive with distinct ids; the demo course lands on the next seeded startup of already-seeded environments. Rollback = revert commit, optionally `DELETE FROM courses WHERE id = 'course-demo-qa'` (lessons/exercises cascade).

## Open Questions

- None.
