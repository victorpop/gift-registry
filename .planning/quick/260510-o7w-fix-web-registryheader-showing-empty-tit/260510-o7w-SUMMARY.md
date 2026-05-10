---
phase: quick/260510-o7w
plan: 01
subsystem: web/registry-mapping
tags:
  - bugfix
  - schema-mismatch
  - tdd
dependency-graph:
  requires: []
  provides:
    - Web RegistryHeader renders Android-written title/occasion/date
    - Regression suite for mapRegistrySnapshot canonical schema
  affects:
    - web/src/lib/firestore-mapping.ts
    - web/src/features/registry/__tests__/useRegistryQuery.test.ts
tech-stack:
  added: []
  patterns:
    - Android-canonical Firestore schema is the source of truth; web mapper translates to web domain
key-files:
  created:
    - web/src/lib/__tests__/firestore-mapping.test.ts
  modified:
    - web/src/lib/firestore-mapping.ts
    - web/src/features/registry/__tests__/useRegistryQuery.test.ts
decisions:
  - Web Registry domain interface naming (name, occasionType, eventDate) intentionally kept; only mapper read path changed
  - Did NOT add legacy-name fallback — mapper is Android-canonical only
  - Existing useRegistryQuery test stub updated to emit canonical schema (was relying on the same buggy mapping)
metrics:
  duration: 111s
  completed-date: 2026-05-10
  tasks-completed: 2
  files-created: 1
  files-modified: 2
  tests-before: 107
  tests-after: 111
  tests-added: 4
requirements:
  - QUICK-260510-o7w
---

# Quick Task 260510-o7w: Fix Web RegistryHeader Empty Title — Summary

Fixed schema mismatch in `mapRegistrySnapshot` so the web fallback reads
the field names the Android app actually writes (`title`, `occasion`,
`eventDateMs` per `RegistryDto.kt`) instead of incorrect names (`name`,
`occasionType`, `eventDate`).

## What Changed

### 1. Mapper read-path fix (`web/src/lib/firestore-mapping.ts`)

Three lines inside `mapRegistrySnapshot` updated to read Android-canonical fields:

| Web domain field   | Was reading (wrong)   | Now reading (canonical) |
| ------------------ | --------------------- | ----------------------- |
| `name`             | `d.name`              | `d.title`               |
| `occasionType`     | `d.occasionType`      | `d.occasion`            |
| `eventDate` (Date) | `timestampToDate(d.eventDate)` | `typeof d.eventDateMs === 'number' ? new Date(d.eventDateMs) : null` |

Inline comment added linking the read path to `RegistryDto.kt` so future
readers see why these specific names.

`mapItemSnapshot` is unchanged. `timestampToDate` helper is preserved (still
used for item `reservedAt`/`expiresAt` and registry `createdAt`/`updatedAt`).
The `Registry` TypeScript interface is unchanged — consumers (`RegistryHeader.tsx`,
existing tests) untouched.

### 2. New regression test file (`web/src/lib/__tests__/firestore-mapping.test.ts`)

86 lines, 4 cases:

1. Mapper reads `title`/`occasion`/`eventDateMs` onto web domain (`name`/`occasionType`/`eventDate`).
2. Null/missing `eventDateMs` maps to `null` `eventDate` while preserving other fields.
3. `exists() === false` returns `null` (regression guard for existing behavior).
4. Mapper does NOT fall back to legacy web-domain field names — documents Android-canonical-only contract.

Uses minimal hand-rolled `DocumentSnapshot` stubs. Type-only import of Firebase
types.

### 3. Existing test stub update (`web/src/features/registry/__tests__/useRegistryQuery.test.ts`)

The "returns registry data when first snapshot arrives" test was emitting
the OLD schema (`name`, `occasionType`, `eventDate`) — it had been relying
on the same bug. Updated the stub to emit the canonical schema (`title`,
`occasion`, `eventDateMs`). Tracked as a Rule 1 deviation below.

## TDD Cycle

Red → Green confirmed:

- **RED** (commit `064bf66`): wrote `firestore-mapping.test.ts` first; ran `npm run test:run -- firestore-mapping`. Tests 1, 2, 4 failed as expected (`expected '' to be 'Sara birthday'`, etc.); test 3 (null on !exists) passed.
- **GREEN** (commit `b0b7d24`): applied 3-line mapper fix + updated `useRegistryQuery` stub. All 4 new tests pass; full suite passes (111/111); typecheck clean.

## Test Counts

| Metric                | Before | After |
| --------------------- | ------ | ----- |
| Web test files        | 21     | 22    |
| Web tests             | 107    | 111   |
| New tests added       | —      | 4     |
| Pre-existing failures | 0      | 0     |
| Typecheck errors      | 0      | 0     |

## Verification Evidence

```
$ grep -nE "d\.(title|occasion|eventDateMs)" web/src/lib/firestore-mapping.ts
34:    name: (d.title as string) ?? '',
35:    occasionType: (d.occasion as string) ?? '',
36:    eventDate: typeof d.eventDateMs === 'number' ? new Date(d.eventDateMs) : null,
75:    title: (d.title as string) ?? '',

$ grep -nE "d\.(name|occasionType|eventDate)[^M]" web/src/lib/firestore-mapping.ts
(no matches — legacy reads removed)

$ cd web && npm run test:run | tail -3
 Test Files  22 passed (22)
      Tests  111 passed (111)
   Duration  2.49s

$ cd web && npm run typecheck
(clean — no output)
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated `useRegistryQuery.test.ts` stub to use canonical schema**

- **Found during:** Task 2 (GREEN — full suite run after mapper fix)
- **Issue:** `src/features/registry/__tests__/useRegistryQuery.test.ts` "returns registry data when first snapshot arrives" emitted a Firestore document stub with the OLD/wrong schema (`name: 'Test Registry'`, `occasionType: 'Wedding'`, `eventDate: null`). The test was passing only because the mapper had the same bug — the stub and the mapper were "matching" via the same wrong field names. After the mapper was corrected to read canonical fields, the stub's old-schema document yielded an empty `name`, breaking the test.
- **Fix:** Updated the stub to emit the canonical Android schema (`title: 'Test Registry'`, `occasion: 'Wedding'`, `eventDateMs: null`). Same logical fix — both files are part of the same bug.
- **Files modified:** `web/src/features/registry/__tests__/useRegistryQuery.test.ts`
- **Commit:** `b0b7d24` (combined with the mapper fix since it is the same logical change)

## Out-of-Scope Observations (Not Fixed)

Per orchestrator constraint to fix only the 3 diagnosed lines, the following
latent schema mismatch was observed in the same file but explicitly NOT
fixed here:

- **`createdAt` / `updatedAt`:** `mapRegistrySnapshot` still calls `timestampToDate(d.createdAt)` / `timestampToDate(d.updatedAt)`, but `RegistryDto.kt` declares both as `Long = 0L` (epoch ms, not Firestore Timestamp). Symptom: registry `createdAt` / `updatedAt` always render as `null` on the web. Same root cause as the title bug, but did not surface as a UX issue and was excluded from this quick task's scope.

This is a known follow-up; if the web ever needs to display registry created/updated timestamps, a future quick task should apply the same `typeof === 'number' ? new Date(...) : null` pattern.

## Self-Check: PASSED

- FOUND: `web/src/lib/__tests__/firestore-mapping.test.ts`
- FOUND: `web/src/lib/firestore-mapping.ts` (modified)
- FOUND: `web/src/features/registry/__tests__/useRegistryQuery.test.ts` (modified)
- FOUND: commit `064bf66` (RED)
- FOUND: commit `b0b7d24` (GREEN + Rule 1 fix)
