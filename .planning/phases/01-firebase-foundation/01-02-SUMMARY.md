---
phase: 01-firebase-foundation
plan: 02
subsystem: infra
tags: [firebase, firestore, security-rules, testing, rules-unit-testing, jest, typescript]

# Dependency graph
requires:
  - phase: 01-firebase-foundation/01-01
    provides: firebase.json, firestore.rules stub (deny-all), Emulator Suite configuration on ports Auth=9099, Firestore=8080

provides:
  - Complete Firestore security rules with canReadRegistry helper and all 5 collection match blocks
  - Automated security rules test suite (12 tests, 6 describe blocks) using @firebase/rules-unit-testing v5
  - TDD RED-to-GREEN cycle verified: stub rules fail expected tests, complete rules pass all 12

affects: [02-firestore-schema, 03-affiliate-functions, 04-reservation-expiry, 05-android-core, 06-notifications, 07-emag-catalog]

# Tech tracking
tech-stack:
  added: ["@firebase/rules-unit-testing@5.x", "firebase@12.11.0 (test)", "jest@29.x", "ts-jest@29.x", "@types/jest@29.x"]
  patterns:
    - "Security rules tests use initializeTestEnvironment with fs.readFileSync to load rules from disk"
    - "testEnv.withSecurityRulesDisabled() for test data seeding"
    - "testEnv.clearFirestore() in afterEach, testEnv.cleanup() in afterAll"
    - "canReadRegistry helper function pattern for composable access control"

key-files:
  created:
    - tests/rules/package.json
    - tests/rules/tsconfig.json
    - tests/rules/jest.config.js
    - tests/rules/firestore.rules.test.ts
    - tests/rules/package-lock.json
  modified:
    - firestore.rules

key-decisions:
  - "Separate tests/rules/ package with its own package.json — keeps test dependencies isolated from functions/"
  - "invitedUsers map pattern (not array) for invited users — enables O(1) membership check in Firestore rules"
  - "Reservations hard-deny all client access (allow read, write: if false) — Admin SDK bypasses rules for Cloud Functions"
  - "Users collection disables delete (allow delete: if false) — account deletion requires backend logic, not direct client delete"

patterns-established:
  - "Security rules helper function pattern: isSignedIn(), isOwner(data), isPublicRegistry(data), isInvited(data), canReadRegistry(data)"
  - "Subcollection rules use get() to fetch parent document data for access control checks"

requirements-completed: []

# Metrics
duration: 7min
completed: 2026-04-04
---

# Phase 01 Plan 02: Firestore Security Rules Summary

**Complete Firestore security rules with canReadRegistry helper, invitedUsers map access control, and 12-test TDD suite passing against Firebase Emulator — public read, private invite-only, reservation hard-deny, owner-only item write all enforced**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-04-04T17:25:40Z
- **Completed:** 2026-04-04T17:32:30Z
- **Tasks:** 2 (Task 1: test scaffold + TDD RED; Task 2: complete rules + TDD GREEN)
- **Files modified:** 6 (5 created + firestore.rules updated)

## Accomplishments
- Security rules enforce all 4 required access patterns: public unauthenticated read, private owner+invited-only, reservation client denial, owner-only item write
- 12 automated tests across 6 describe blocks, all passing against Firebase Emulator Suite
- TDD cycle completed: RED (stub deny-all rules fail 6 assertSucceeds tests) then GREEN (complete rules pass all 12)
- User document isolation enforced: users can only read/write their own document, delete blocked

## Task Commits

Each task was committed atomically:

1. **Task 1: Create security rules test scaffold (TDD RED)** - `274dd30` (test)
2. **Task 2: Write complete Firestore security rules (TDD GREEN)** - `44cc66c` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `tests/rules/package.json` - Test package with @firebase/rules-unit-testing@^5.0.0, firebase@^12.11.0, jest@^29.7.0
- `tests/rules/tsconfig.json` - TypeScript config: strict, es2022, commonjs module
- `tests/rules/jest.config.js` - Jest config: ts-jest preset, node environment, 30s timeout
- `tests/rules/firestore.rules.test.ts` - 12 tests across 6 describe blocks: public read, private access, reservations, items subcollection, users, registry creation
- `tests/rules/package-lock.json` - Locked dependency tree (348 packages)
- `firestore.rules` - Complete security rules: 5 helper functions, 5 collection match blocks, 60 lines

## Decisions Made
- Separate `tests/rules/` package with its own `package.json` isolates test dependencies from `functions/`
- `invitedUsers` map (not array) enables O(1) lookup in Firestore rules (`invitedUsers[uid] == true`)
- `reservations` collection uses hard `allow read, write: if false` — Admin SDK bypasses rules, clients should never touch this collection directly
- `users` collection `allow delete: if false` — account deletion needs backend business logic (cleanup reservations, registries), not raw client delete

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

Minor: npm cache was root-owned (a known npm bug), causing initial `npm install` failure. Resolved by using `--cache /tmp/npm-cache` flag. No impact on test results.

## User Setup Required

None - no external service configuration required. Tests run against Firebase Emulator locally.

## Known Stubs

None - all security rules are fully implemented and tested.

## Next Phase Readiness
- Firestore security rules are complete. Phase 02 (Firestore schema) can proceed with the knowledge that access control is correctly enforced.
- All 12 automated tests serve as regression protection for rules changes in future phases.
- `tests/rules/` test infrastructure is ready to extend with additional test scenarios as new collections are added.

---
*Phase: 01-firebase-foundation*
*Completed: 2026-04-04*
