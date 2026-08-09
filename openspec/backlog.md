# OpenSpec Backlog

## Candidate Changes

### Teacher course ownership and progress visibility

- Define teacher-owned courses with Google Classroom-style behavior.
- Allow teachers to create and manage their own courses.
- Allow teachers to view student progress only for courses they own.
- Clarify enrollment, ownership transfer, and admin override rules.

### Learning paths

- Reference brief: `openspec/learning-paths-brief.md`
- Introduce platform-curated learning paths as the default learner experience; do not treat them as teacher-created private courses.
- Use school year as the primary axis, with one default platform path per year.
- During onboarding, automatically recommend/assign the default path for the selected year with no extra confirmation.
- If the learner changes school year later, recalculate the recommended path but ask for confirmation before switching.
- Let learners manually switch paths, remember the last opened path, and keep access to other paths lightweight.
- Keep progression linear lesson-by-lesson without hard-locking future lessons; show the full ordered lesson list from the start and visually emphasize the next recommended lesson.
- Reuse the existing lesson/exercise completion model: lesson completion still derives from exercises, path progress is percentage-based, and completed lessons count across every path that includes them.
- Avoid special migration when switching paths; shared completed lessons should simply count.
- Model each path with a name, description, visible objective, and structured objective label; v1 structured objective type is only `grade-level`.
- Open a path on a summary screen first, then use a primary CTA such as "Start" to focus the path view on the first incomplete lesson instead of deep-linking straight into lesson content.
- Keep completed lessons accessible for review.
- Defer 100% completion rewards to `gamification-rewards`, not `learning-paths` v1.

### Localize ViewModel error messages via sealed error types

- Deferred from `localizacion-espanol`. ViewModel `errorMessage: String` fields (`OnboardingViewModel`, `LoginViewModel`, `RegisterViewModel`, `HomeDashboardViewModel`, `ProfileViewModel`, `CourseViewModel`, `LessonMapViewModel`) and raw `Throwable.message` propagation are still hardcoded/English.
- Requires converting error state to a sealed/enum domain type resolved and localized at the UI layer (ViewModels can't call `stringResource()` directly), plus migrating the unit tests that currently assert exact English error-message literals.
- A `PlaceholderScreen`/screen render step for `ProfileViewModel`/`HomeDashboardViewModel`'s new `schoolYear`/`studentTrack` state (added by `localizacion-espanol` but not yet consumed by any screen) can be picked up alongside this work if convenient.

### Production backend readiness

- Add basic observability for server runtime failures.
- Consider `CallLogging`, `StatusPages`, health checks, and consistent error responses.
- Document operational expectations for auth, database, and seed startup paths.

### ~~Web admin panel~~ ✅ (archived 2026-06-27)

- Delivered the administrator SPA with login gating, paginated user management, role updates, and all-course visibility.
- Archive evidence: `openspec/changes/archive/2026-06-27-admin-web-panel/` (16/16 tasks; backend and SPA verification passed).

### ~~Versioned database migrations~~ ✅ (archived 2026-06-22)

- Replaced `SchemaUtils.create(...)` with Flyway programmatic migration on startup.
- Baseline V1 (`V1__baseline_current_schema.sql`) + guarded V2 (`V2__ensure_courses_school_year.sql`) in place.
- `openspec/backlog.md` documents the rule: every future server schema change needs a matching Flyway migration script.
- CI drift/checksum validation remains deferred.

### ~~Configurable KMP API base URL~~ ✅ (archived 2026-06-22)

- Delivered configurable API base URL resolution for Android, iOS, and JVM targets.
- Archive evidence: `openspec/changes/archive/2026-06-22-configurable-api-base-url/` (8/8 tasks; verification passed with the documented manual iOS check).

### ~~Role naming cleanup~~ ✅ (archived 2026-06-22)

- Standardized product and code terminology on `STUDENT`, retaining backward-compatible parsing for legacy `LEARNER` values.
- Archive evidence: `openspec/changes/archive/2026-06-22-role-naming-cleanup/` (23/23 tasks; 65 tests passing).

### Onboarding and navigation bug fixes

Four issues found by manual QA on the current onboarding/auth flow. Tackle one at a time.

1. ~~**App is not localized to Spanish.**~~ ✅ Resolved (`sdd` change `localizacion-espanol`). All commonMain Compose screens now resolve user-facing copy via Compose Multiplatform string resources (`composeApp/src/commonMain/composeResources/values/strings.xml` as neutral Latin American Spanish default, `values-en/strings.xml` as English fallback). `StudentTrack.displayName`/`parse()` kept untouched as a persistence contract; a separate `StudentTrack.localizedLabel()` UI-layer function was added instead. `ProfileViewModel`/`HomeDashboardViewModel` now expose typed `schoolYear`/`studentTrack` state instead of building an English label string internally (no screen currently renders it — tracked as debt, not a defect). ViewModel `errorMessage` fields and raw `Throwable.message` propagation remain hardcoded/English — deliberately out of scope, see new backlog item below.
2. ~~**Onboarding asks school year before education level.**~~ ✅ Resolved (`sdd` change `onboarding-step-order-fix`). Reordered onboarding steps to Province → Category (`StudentTrack`) → School year → Confirmation. Updated `OnboardingViewModel` to derive `availableSchoolYears` from the selected track, updated screen branching order and step copy resource mappings, and added comprehensive test coverage for the new flow including back-navigation semantics.
3. ~~**Logout button is visible during onboarding.**~~ ✅ Resolved. Removed the Logout button and `onLogout` plumbing from `OnboardingScreen`/`OnboardingContent`, the `App.kt` call site, and `OnboardingScreenTest`. Logout is only reachable from the Profile screen once onboarding is complete. Also removed the now-unused `onboarding_action_logout` string resource (Spanish and English).
4. ~~**App crashes when tapping "Continue to courses" after finishing onboarding.**~~ ✅ Resolved (`sdd` change `post-onboarding-course-navigation-crash`, archived). Root cause: `di/AppModule.kt` registered `MainRouterViewModel` with reflective `viewModelOf`, which tried to resolve its defaulted `MainTab` constructor param through Koin and threw `NoDefinitionFoundException`. Fixed with an explicit `viewModel { MainRouterViewModel() }` registration.
5. ~~**Onboarding silently skipped for a second account on the same device.**~~ ✅ Resolved (`sdd` change `onboarding-profile-scoped-to-user`, archived). Found while verifying #4 on device: `LearnerProfileEntity` was a single global row (`profileId = 1`, no `userId`), so any account logging in after another had completed onboarding inherited its `onboardingComplete = true` and skipped the wizard. Fixed by re-keying the table on `userId` and scoping `LearnerProfileRepository`/`AuthGate` to the authenticated user, failing toward "show onboarding" on a missing/blank session id.
