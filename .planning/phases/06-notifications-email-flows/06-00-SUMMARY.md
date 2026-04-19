---
phase: 06-notifications-email-flows
plan: "00"
subsystem: foundation
tags: [jest, firestore-rules, firebase-messaging, i18n, strings]
dependency_graph:
  requires: []
  provides:
    - functions/jest harness (ts-jest + smoke tests + fake Firestore fixture)
    - firestore.rules mail/notifications_failures/fcmTokens deny rules
    - firebase-messaging Android dependency
    - Phase 6 string keys in 4 resource files (Android en/ro, web en/ro)
  affects:
    - All subsequent Phase 6 Cloud Functions plans (need Jest harness)
    - Phase 6 UI plans (need string resources)
    - Firestore security rules (new deny paths enforced)
tech_stack:
  added:
    - jest@^29.7.0 (Cloud Functions unit test runner)
    - ts-jest@^29.2.0 (TypeScript transform for Jest)
    - "@types/jest@^29.5.0"
    - firebase-functions-test@^3.3.0
    - firebase-messaging (via Firebase BoM 34.11.0, no explicit version)
  patterns:
    - In-memory Firestore double (createFakeFirestore) for unit tests — no emulator needed
    - testEnv.ts provides ensureAdminInitialized/cleanupAdmin for admin SDK tests
key_files:
  created:
    - functions/jest.config.js
    - functions/src/__tests__/smoke.test.ts
    - functions/src/__tests__/fixtures/firestore.ts
    - functions/src/__tests__/fixtures/testEnv.ts
  modified:
    - functions/package.json (added test scripts + devDependencies)
    - firestore.rules (added mail, notifications_failures, fcmTokens rules)
    - tests/rules/firestore.rules.test.ts (added 9 Phase 6 test cases)
    - gradle/libs.versions.toml (added firebase-messaging alias)
    - app/build.gradle.kts (added implementation(libs.firebase.messaging))
    - app/src/main/res/values/strings.xml (added 10 Phase 6 keys, en)
    - app/src/main/res/values-ro/strings.xml (added 10 Phase 6 keys, ro)
    - web/src/i18n/en.json (added confirm_purchase_* + notifications + settings keys)
    - web/src/i18n/ro.json (added same keys with Romanian translations)
    - web/i18n/en.json (legacy seed sync)
    - web/i18n/ro.json (legacy seed sync)
decisions:
  - firebase-functions-test requires --legacy-peer-deps during npm install due to firebase-admin peer dep range declaration (functional at runtime with admin@13.x)
  - Firestore emulator was already running on port 8080; rules tests run directly without emulators:exec
  - Legacy web/i18n/*.json seed files synced byte-identical to web/src/i18n/*.json per Phase 5 decision
metrics:
  duration: ~7min
  completed_date: "2026-04-19T18:57:49Z"
  tasks_completed: 3
  files_modified: 11
---

# Phase 6 Plan 00: Wave 0 Foundation — Jest Harness, Rules, Dependencies, Strings

Cloud Functions Jest test harness installed (ts-jest preset, in-memory Firestore double), Firestore rules extended with hard-deny for mail/notifications_failures and owner-only fcmTokens subcollection (9 new passing tests), firebase-messaging declared via BoM, and all Phase 6 string keys added to 4 resource files (Android en/ro, web en/ro).

## What Was Built

### Task 1: Cloud Functions Jest Harness

- **Test command:** `cd functions && npm test` (exits 0, 2 passing smoke tests)
- **Config:** `functions/jest.config.js` — ts-jest preset, `testMatch: ['**/__tests__/**/*.test.ts']`, `testTimeout: 10000`
- **Fixtures exported:**
  - `createFakeFirestore(seed)` — in-memory Firestore double with `collection()`, `doc()`, `runTransaction()`. Exposes `__store` for test assertions.
  - `ensureAdminInitialized()` — idempotent firebase-admin app init for unit tests
  - `cleanupAdmin()` — deletes all admin apps after test suite
- **devDependencies added:** jest@^29.7.0, ts-jest@^29.2.0, @types/jest@^29.5.0, firebase-functions-test@^3.3.0

### Task 2: Firestore Rules Extensions

Three new rule blocks added inside `match /databases/{database}/documents`:

| Path | Rule |
|------|------|
| `match /mail/{docId}` | `allow read, write: if false` |
| `match /notifications_failures/{docId}` | `allow read, write: if false` |
| `match /users/{userId}/fcmTokens/{tokenId}` | `allow read, write, delete: if isSignedIn() && request.auth.uid == userId` |

**Rules test results:** 27 total (18 pre-existing + 9 new) — all pass.

New test coverage:
- Mail: denies unauth write, auth write, auth read (3 tests)
- notifications_failures: denies auth read, auth write (2 tests)
- fcmTokens: allows owner write, allows owner read; denies other user read, other user write, unauthenticated read (5 tests — note: 4 listed in plan, 5 actual — plan counted 4 but spec had 5 cases)

### Task 3: Android Dependency + String Resources

**firebase-messaging:** Added to `libs.versions.toml` and `app/build.gradle.kts` — version managed by Firebase BoM 34.11.0.

**Android string keys added (10 keys, both en + ro):**
- `reservation_confirm_purchase_heading`
- `reservation_confirm_purchase_cta`
- `reservation_confirm_purchase_loading`
- `reservation_confirm_purchase_success`
- `reservation_confirm_purchase_error`
- `notifications_purchase_snackbar`
- `notifications_purchase_snackbar_action`
- `notifications_channel_purchase_name`
- `notifications_channel_purchase_description`
- `settings_email_language_label`

**Web i18n keys added:**
- `reservation.confirm_purchase_*` (5 keys in reservation namespace)
- `notifications.purchase_snackbar` (new top-level namespace)
- `settings.email_language_label` (new top-level namespace)

Both en.json and ro.json parse as valid JSON.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] npm install required --legacy-peer-deps**
- **Found during:** Task 1
- **Issue:** firebase-functions-test@3.3.0+ resolves to 3.4.1 which declares `peer firebase-admin@^8-^13` but we have admin@^13.7.0 — npm strict mode rejects this even though ^13 satisfies the range
- **Fix:** Used `--legacy-peer-deps` to install; peer dep is satisfied at runtime (13.x is within ^8-^13 range — npm's strict resolver was being overly cautious)
- **Files modified:** functions/package-lock.json

**2. [Rule 3 - Blocking] npm cache permission error**
- **Found during:** Task 1
- **Issue:** npm cache at /Users/victorpop/.npm had root-owned files blocking writes (no sudo available)
- **Fix:** Set npm cache to `/tmp/npm-cache-gsd` for this session
- **Files modified:** none

**3. [Rule 3 - Info] Firestore emulator already running**
- **Found during:** Task 2
- **Issue:** `firebase emulators:exec --only firestore` failed because port 8080 was taken
- **Fix:** Ran rules tests directly (`cd tests/rules && npm test`) against the already-running emulator — same verification outcome
- **Files modified:** none

## Known Stubs

None — this plan installs infrastructure only. No UI or data-flow stubs.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 59d1ef5 | chore(06-00): install Cloud Functions Jest harness |
| 2 | cc609ad | feat(06-00): extend firestore.rules + rules tests |
| 3 | 99bdf38 | chore(06-00): add firebase-messaging + Phase 6 strings |

## Self-Check: PASSED
