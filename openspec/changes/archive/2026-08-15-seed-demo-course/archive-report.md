# Archive Report: seed-demo-course

**Date**: 2026-08-15  
**Change**: seed-demo-course  
**Archived to**: `openspec/changes/archive/2026-08-15-seed-demo-course/`  
**Archive Mode**: openspec (filesystem)  

## Final State Authority

This report reflects the state of the change AT CLOSE, per the Final-State Authority hierarchy:

1. **Explicit final-state facts from orchestrator launch prompt**: All 27 tasks complete, spec conformance confirmed, tests passing (BUILD SUCCESSFUL, 69/69 tests).
2. **Persisted tasks artifact**: `tasks.md` shows 21 marked implementation tasks across 4 phases, all checked `[x]`.
3. **Repository evidence**: No verify-report.md was generated; final test run confirmed no failures.

## Specification Sync Summary

### Delta Specs Processed

| Domain | Status | Action | Details |
|--------|--------|--------|---------|
| `demo-course-seed` | ✅ Created | Mechanical copy | New spec; copied from delta to `openspec/specs/demo-course-seed/spec.md` |
| `lesson-read-access` | ✅ Updated | Merged | Existing spec updated with new requirement text and scenario at line 20 & 61 |

### Merged Changes

#### 1. demo-course-seed (NEW)
- **Source**: `openspec/changes/seed-demo-course/specs/demo-course-seed/spec.md`
- **Destination**: `openspec/specs/demo-course-seed/spec.md`
- **Copy Method**: Mechanical shell copy (cp -R) with integrity verification (diff -r, empty diff)
- **Requirements Added**: 4
  - Demo Course Seeding (with 2 scenarios)
  - Demo Lesson and Exercise Coverage (with 2 scenarios)
  - Per-Entity Seed Idempotency (with 2 scenarios)
  - Demo Course Readable by Every Role (with 1 scenario)

#### 2. lesson-read-access (MODIFIED)
- **Source**: `openspec/changes/seed-demo-course/specs/lesson-read-access/spec.md` (delta)
- **Destination**: `openspec/specs/lesson-read-access/spec.md` (main)
- **Merge Method**: Requirement replacement + new scenario insertion
- **Changes**:
  - **Requirement: Lesson Read Visibility Tiers** - Added clarification paragraph stating: "A TEACHER MUST also be able to read any course where `isOfficial = true`, regardless of `creatorId`" with historical context.
  - **New Scenario**: "Non-creator teacher reads official course lesson" added after "Other teacher denied private lesson" scenario, confirming that non-creator teachers can read official courses.
- **Preserved**: All existing requirements and scenarios remain intact; 5 scenarios already in main spec still present.

## Archive Contents

- ✅ `proposal.md` — Proposal defining scope and approach
- ✅ `design.md` — Design decisions and architecture
- ✅ `tasks.md` — All 21 implementation tasks marked complete `[x]`
- ✅ `specs/demo-course-seed/spec.md` — New spec with 4 requirements, 7 scenarios
- ✅ `specs/lesson-read-access/spec.md` — Delta spec (archived as reference; merged into main)

## Task Completion Gate

**Status**: ✅ PASSED

- **Total tasks**: 21 implementation tasks
- **Completed**: 21 (100%)
- **Verified**: All marked `[x]` in `tasks.md`
- **Phases**:
  - Phase 1 (Refactor Seed Idempotency): 4/4 tasks ✅
  - Phase 2 (Demo Course Content): 7/7 tasks ✅
  - Phase 3 (Fix TEACHER Read Access): 2/2 tasks ✅
  - Phase 4 (Testing): 8/8 tasks ✅

## Native Review Gate

**Status**: Not applicable

- No review receipt gateway was discovered (native review receipt gate absent per openspec mode).
- Archive proceeds under ordinary repository policy.
- No `reviewGate` or verification receipt artifacts to validate.

## Change Closure Summary

### What Was Changed

1. **SeedData.kt**: Refactored seed routine to per-entity idempotency; added `seedDemoCourse()` with demo course, two lessons (theory + exercises), and three exercise types (MultipleChoice, InputValue, MultiSelect).
2. **ContentReadAccess.kt**: Extended TEACHER read access to include official courses (`access.creatorId == userId || access.isOfficial`).
3. **ServiceLayerTest.kt**: Added 8 tests covering teacher access to official content, seed idempotency, and demo content materialization.

### Verification Status

Per orchestrator final-state facts:
- **Spec Conformance**: ✅ Confirmed for both delta specs against actual code
- **Test Status**: ✅ BUILD SUCCESSFUL, 69/69 tests passing, 0 failures/errors
- **Source Files Verified**:
  - `server/src/main/kotlin/com/example/proyectofinal/seed/SeedData.kt` — Demo seed and idempotency helpers present
  - `server/src/main/kotlin/com/example/proyectofinal/service/ContentReadAccess.kt` — TEACHER read access fixed
  - `server/src/test/kotlin/com/example/proyectofinal/ServiceLayerTest.kt` — All required tests present

### Risks & Mitigations

| Risk | Likelihood | Mitigation | Residual |
|------|------------|------------|----------|
| Demo course appears in production | Med | Distinct ids (`course-demo-qa`), clear "Demo" titling, gated by `seedData` flag | Low |
| Teacher visibility misread as write access | Low | Change is read-only path; write guards untouched | None |

All mitigations in place per proposal.

### Rollback Information

Revert the commit:
```bash
git revert <commit-hash>
```

Seed rows are additive with distinct ids (`course-demo-qa`, `lesson-demo-*`, `ex-demo-*`). If deployed DB cleanup is needed, delete by id. The access change is a single boolean clause with no persisted state.

## SDD Cycle Complete

- ✅ Proposed
- ✅ Specified (2 specs)
- ✅ Designed
- ✅ Tasked (21 tasks)
- ✅ Applied
- ✅ Verified
- ✅ Archived

**Next Step**: Ready for the next change.

---

**Archive Created**: 2026-08-15T14:12:03Z  
**Artifact Store**: openspec  
**Mechanical Operations**: shell `cp -R`, shell `mv` with `diff -r` integrity verification  
