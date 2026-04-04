---
phase: 01-firebase-foundation
plan: 01
subsystem: infra
tags: [firebase, firestore, cloud-functions, typescript, emulators, hosting]

# Dependency graph
requires: []
provides:
  - Firebase project bound to gift-registry-ro (europe-west3)
  - firebase.json with Firestore, Functions, Hosting, and Emulator Suite configuration
  - Firestore security rules stub (deny-all)
  - Firestore composite indexes for items and reservations collections
  - Cloud Functions TypeScript scaffold (Node 22, firebase-functions v2)
  - .gitignore covering Firebase artifacts and Android build outputs
affects: [02-firestore-schema, 03-affiliate-functions, 04-reservation-expiry, 05-android-core, 06-notifications, 07-emag-catalog]

# Tech tracking
tech-stack:
  added: [firebase-functions@7.2.3, firebase-admin@13.7.0, typescript@5.7.0, node@22]
  patterns: [firebase-functions/v2 import pattern, admin.initializeApp() at module root]

key-files:
  created:
    - firebase.json
    - .firebaserc
    - firestore.rules
    - firestore.indexes.json
    - functions/package.json
    - functions/tsconfig.json
    - functions/src/index.ts
    - .gitignore
  modified: []

key-decisions:
  - "firebase-functions/v2 import used (2nd gen Cloud Functions) — 1st gen is deprecated"
  - "Firestore region europe-west3 (Frankfurt) — permanent, cannot be changed after creation"
  - "Emulator Suite on ports: Auth=9099, Functions=5001, Firestore=8080, Hosting=5000, UI=4000"
  - "singleProjectMode=true for emulators — prevents cross-project emulator confusion"
  - "functions/src/index.ts exports healthCheck placeholder — real functions added in Phase 3+"

patterns-established:
  - "Cloud Functions entry point: functions/src/index.ts with admin.initializeApp() at module root"
  - "TypeScript strict mode for functions (noImplicitReturns, noUnusedLocals, strict: true)"

requirements-completed: []

# Metrics
duration: 15min
completed: 2026-04-04
---

# Phase 01 Plan 01: Firebase Foundation Summary

**Firebase project gift-registry-ro scaffolded with Firestore, Cloud Functions (TypeScript/Node 22), Hosting, and Emulator Suite — all configuration files committed and TypeScript compiles cleanly**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-04T17:05:00Z
- **Completed:** 2026-04-04T17:20:18Z
- **Tasks:** 2 (Task 1: human-action checkpoint completed by user; Task 2: automated scaffold)
- **Files modified:** 9 (8 created + package-lock.json)

## Accomplishments
- Firebase project bound to `gift-registry-ro` with all required services enabled (Auth, Firestore europe-west3, App Check)
- Complete Firebase configuration: Firestore rules, composite indexes, Functions scaffold, Hosting, Emulator Suite
- Cloud Functions TypeScript scaffold compiles without errors (strict mode, Node 22, firebase-functions v2)
- Emulator Suite configured on all required ports with singleProjectMode enabled
- .gitignore protecting Firebase debug logs, compiled output, and Android build artifacts

## Task Commits

Each task was committed atomically:

1. **Task 1: Firebase CLI install and project creation** - completed by user (human-action checkpoint)
2. **Task 2: Scaffold Firebase project configuration** - `3cbc014` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `firebase.json` - Full Firebase configuration: Firestore, Functions, Hosting, Emulator Suite (ports configured)
- `.firebaserc` - Project alias binding: default → gift-registry-ro
- `firestore.rules` - Security rules stub with rules_version = '2', deny-all until Phase 02
- `firestore.indexes.json` - Composite indexes: items (status+addedAt), reservations (status+expiresAt)
- `functions/package.json` - Cloud Functions dependencies: firebase-functions@^7.2.3, firebase-admin@^13.7.0, node 22
- `functions/tsconfig.json` - TypeScript config: strict, es2022 target, commonjs module, outDir=lib
- `functions/src/index.ts` - Cloud Functions entry point with healthCheck placeholder
- `functions/package-lock.json` - Locked dependency tree
- `.gitignore` - Firebase artifacts, functions/lib/, .firebase/, Android build outputs

## Decisions Made
- Used `firebase-functions/v2` import (2nd gen) — 1st gen is deprecated per plan
- Firestore region `europe-west3` is permanent and was set correctly at project creation
- `singleProjectMode: true` in emulators config prevents emulator cross-project confusion
- `healthCheck` endpoint in index.ts is intentional placeholder — real functions added in Phase 3+

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all files were pre-created by the user and matched plan specifications exactly. TypeScript compilation passed on first run.

## User Setup Required

Task 1 was a human-action checkpoint. The user:
1. Installed Firebase CLI (v13+)
2. Authenticated via `firebase login`
3. Created Firebase project `gift-registry-ro`
4. Enabled Auth (Email/Password), Firestore (europe-west3, production mode), and App Check in the Firebase Console

## Known Stubs

- `functions/src/index.ts` `healthCheck` export — intentional placeholder. Real Cloud Functions (affiliate URL transformer, reservation expiry handler) are added in Phase 3+. This stub does not block any Phase 01 or Phase 02 goals.
- `firestore.rules` deny-all stub — intentional. Full rules are defined in Plan 02 (Firestore schema and security rules).

## Next Phase Readiness
- Firebase foundation is complete. Phase 02 (Firestore schema + security rules) can proceed immediately.
- All emulator ports are configured and ready for local development.
- Cloud Functions compile target is set up; Phase 03 can add functions directly to functions/src/index.ts.
- No blockers for Phase 02.

---
*Phase: 01-firebase-foundation*
*Completed: 2026-04-04*

## Self-Check: PASSED

- FOUND: firebase.json
- FOUND: .firebaserc
- FOUND: firestore.rules
- FOUND: firestore.indexes.json
- FOUND: functions/src/index.ts
- FOUND: functions/package.json
- FOUND: functions/tsconfig.json
- FOUND: .gitignore
- FOUND commit: 3cbc014 (feat(01-01): scaffold Firebase project configuration)
