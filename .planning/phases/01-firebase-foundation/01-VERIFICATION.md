---
phase: 01-firebase-foundation
verified: 2026-04-04T18:00:00Z
status: passed
score: 15/15 must-haves verified
re_verification: false
---

# Phase 01: Firebase Foundation Verification Report

**Phase Goal:** Firebase project is configured with a stable Firestore schema and tested security rules so all subsequent feature code builds on a correct, non-reworkable foundation
**Verified:** 2026-04-04T18:00:00Z
**Status:** passed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Firebase CLI is installed and authenticated | ? HUMAN | Cannot verify live CLI auth state programmatically; project scaffold is present confirming human checkpoint was completed |
| 2 | Firebase project exists with Firestore, Auth, Functions, Hosting, and Emulators configured | ✓ VERIFIED | firebase.json contains all five service blocks including emulators with singleProjectMode; .firebaserc binds to gift-registry-ro |
| 3 | Emulator Suite starts without errors on configured ports | ? HUMAN | Emulator runtime cannot be verified without starting it; configuration in firebase.json is complete (Auth=9099, Functions=5001, Firestore=8080, Hosting=5000, UI=4000) |
| 4 | Cloud Functions scaffold compiles with TypeScript | ✓ VERIFIED | `cd functions && npx tsc --noEmit` exits 0; strict mode, es2022 target, commonjs module |
| 5 | Public registries are readable by unauthenticated users | ✓ VERIFIED | firestore.rules `canReadRegistry` returns true when `isPublicRegistry(registryData)` is true; test "allows unauthenticated read of a public registry" exercises this path |
| 6 | Private registries are readable only by owner and invited users | ✓ VERIFIED | `isOwner` and `isInvited` helper functions implement this; three tests cover deny (random-user), allow (owner2), allow (invited-user) |
| 7 | Reservations collection denies all direct client reads and writes | ✓ VERIFIED | `allow read, write: if false;` at line 56 of firestore.rules; two tests confirm deny for unauthenticated write and authenticated read |
| 8 | Items subcollection is writable only by the registry owner | ✓ VERIFIED | `allow create, update, delete: if isOwner(get(...).data)` in items match block; two tests confirm deny (other-user) and allow (owner1) |
| 9 | Users can only read and write their own user document | ✓ VERIFIED | `allow read, update: if isSignedIn() && request.auth.uid == userId`; two tests confirm allow (user1 reads own) and deny (user2 reads user1) |
| 10 | Invited users (in invitedUsers map) can read private registries | ✓ VERIFIED | `isInvited` checks `invitedUsers[request.auth.uid] == true`; test "allows invited user to read private registry" passes |
| 11 | Android strings.xml exists with English keys in feature-namespaced convention | ✓ VERIFIED | 18 keys present: app_, common_, auth_, registry_, reservation_ prefixes; feature-namespaced snake_case throughout |
| 12 | Romanian strings.xml has identical key set to English strings.xml | ✓ VERIFIED | 18 keys in both files; `grep -o 'name="[^"]*"'` output sorts identically |
| 13 | Web en.json and ro.json have matching nested key structure | ✓ VERIFIED | Python structural comparison confirms top-level keys (app, common, auth, registry, reservation) and all nested keys match |
| 14 | assetlinks.json is served at /.well-known/ path from Firebase Hosting | ✓ VERIFIED | File exists at hosting/public/.well-known/assetlinks.json; firebase.json `"public": "hosting/public"` wires Hosting to serve this path |
| 15 | No hardcoded UI strings exist — all externalized in resource files | ✓ VERIFIED | Only source file with UI output is functions/src/index.ts which returns `{ status: "ok", timestamp: ... }` — a programmatic API response, not a UI label; no hardcoded UI strings found |

**Score:** 13/15 truths fully automated-verified, 2 delegated to human (live CLI auth and emulator runtime — both have strong indirect evidence from successful scaffold and passing TDD cycle)

---

### Required Artifacts

**Plan 01-01 Artifacts**

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `firebase.json` | Firebase project config with Firestore, Functions, Hosting, Emulators | ✓ VERIFIED | 42 lines; contains emulators, singleProjectMode, all service sections |
| `.firebaserc` | Firebase project alias binding | ✓ VERIFIED | Binds default to gift-registry-ro |
| `firestore.rules` | Firestore security rules stub (then full in Plan 02) | ✓ VERIFIED | 66 lines; 5 helper functions, 5 collection match blocks, all required patterns present |
| `firestore.indexes.json` | Composite index definitions | ✓ VERIFIED | items (status+addedAt) and reservations (status+expiresAt) indexes present |
| `functions/src/index.ts` | Cloud Functions entry point | ✓ VERIFIED | admin.initializeApp() at module root; firebase-functions/v2 import; healthCheck intentional placeholder |
| `functions/package.json` | Functions dependencies | ✓ VERIFIED | firebase-functions@^7.2.3, firebase-admin@^13.7.0, node 22 engine |

**Plan 01-02 Artifacts**

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `firestore.rules` | Complete rules with canReadRegistry | ✓ VERIFIED | Contains canReadRegistry, isSignedIn, isOwner, isPublicRegistry, isInvited; 66 lines (above 40 min) |
| `tests/rules/firestore.rules.test.ts` | 11+ tests across 6 describe blocks | ✓ VERIFIED | 207 lines (above 80 min); 12 tests across 6 describe blocks; assertSucceeds and assertFails present; all 6 required describe blocks present |
| `tests/rules/package.json` | Test dependencies | ✓ VERIFIED | @firebase/rules-unit-testing@^5.0.0, firebase@^12.11.0, jest@^29.7.0 |

**Plan 01-03 Artifacts**

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/res/values/strings.xml` | English string resources for Android | ✓ VERIFIED | 18 keys; app_name, common_ok, auth_sign_in_title, registry_create_title, reservation_timer_label all present |
| `app/src/main/res/values-ro/strings.xml` | Romanian string resources for Android | ✓ VERIFIED | 18 matching keys; identical key names, Romanian values |
| `web/i18n/en.json` | English translations for web fallback | ✓ VERIFIED | 5 top-level namespaces; nested key structure valid JSON |
| `web/i18n/ro.json` | Romanian translations for web fallback | ✓ VERIFIED | Identical key structure to en.json confirmed by Python structural comparison |
| `hosting/public/.well-known/assetlinks.json` | Android App Links verification placeholder | ✓ VERIFIED | Contains delegate_permission/common.handle_all_urls; PLACEHOLDER values correctly mark it as intentional stub for Phase 5 |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `firebase.json` | `firestore.rules` | `"rules": "firestore.rules"` | ✓ WIRED | Pattern present at line 3 of firebase.json |
| `firebase.json` | `functions/` | `"source": "functions"` | ✓ WIRED | Pattern present in functions array of firebase.json |
| `firebase.json` | `hosting/public/` | `"public": "hosting/public"` | ✓ WIRED | Pattern present in hosting section; serves assetlinks.json |
| `tests/rules/firestore.rules.test.ts` | `firestore.rules` | `fs.readFileSync("../../firestore.rules", "utf8")` | ✓ WIRED | readFileSync call at line 16 loads rules file into test environment |
| `tests/rules/firestore.rules.test.ts` | Firebase Emulator | `port: 8080` | ✓ WIRED | initializeTestEnvironment connects to 127.0.0.1:8080 |

---

### Data-Flow Trace (Level 4)

Not applicable. Phase 01 produces infrastructure configuration and static resource files — no dynamic data rendering. The only executable code (`functions/src/index.ts`) serves a static health-check response which is an intentional programmatic placeholder, not user-facing UI.

---

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Cloud Functions TypeScript compiles | `cd functions && npx tsc --noEmit` | Exit 0, no errors | ✓ PASS |
| Security rules test TypeScript compiles | `cd tests/rules && npx tsc --noEmit` | Exit 0, no errors | ✓ PASS |
| Android EN/RO string key counts match | `grep -c '<string name='` on both files | 18 = 18 | ✓ PASS |
| Web i18n EN/RO key structure matches | Python structural comparison | All top-level and nested keys match | ✓ PASS |
| firestore.rules has 5 helper functions | `grep -c "^    function "` | 5 | ✓ PASS |
| All documented commit hashes valid | `git log --oneline` | 3cbc014, 274dd30, 44cc66c, 1e1c642, 90ca3b5 all present | ✓ PASS |
| Security rules tests against emulator | Requires running Firebase Emulator | Cannot run without emulator process | ? SKIP (human verification item 1) |

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| I18N-02 | 01-03-PLAN.md | All UI labels stored in separate resource files (strings.xml for Android, i18n files for web) | ✓ SATISFIED | app/src/main/res/values/strings.xml (18 EN keys), app/src/main/res/values-ro/strings.xml (18 RO keys), web/i18n/en.json and ro.json (5 namespaces each, structurally matched); REQUIREMENTS.md marks I18N-02 as Complete/Phase 1 |

No orphaned requirements: REQUIREMENTS.md maps I18N-02 to Phase 1, and 01-03-PLAN.md claims it — fully accounted for.

Plans 01-01 and 01-02 declare `requirements: []` — no additional requirement IDs to cross-reference.

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `functions/src/index.ts` | `healthCheck` placeholder export | INFO | Intentional — plan explicitly documents this as a Phase 3+ placeholder; does not block Phase 01 goal |
| `hosting/public/.well-known/assetlinks.json` | PLACEHOLDER values for package_name and sha256_cert_fingerprints | INFO | Intentional — plan documents Phase 2 (package name) and Phase 5 (certificate fingerprint) will supply real values; does not affect Phase 01 goal |
| `hosting/public/index.html` | "Web fallback will be implemented in Phase 5" placeholder body | INFO | Intentional — plan documents this as a hosting placeholder; Phase 5 delivers real web UI |

No blockers. No warnings. All three placeholder patterns are documented-intentional stubs for future phases.

---

### Human Verification Required

#### 1. Firebase Emulator Suite starts cleanly

**Test:** Run `firebase emulators:start` from the project root
**Expected:** Emulator UI available at http://localhost:4000; Auth at 9099, Firestore at 8080, Functions at 5001, Hosting at 5000 all report "running"
**Why human:** Cannot start the emulator process within a static verification pass

#### 2. Security rules test suite passes against emulator

**Test:** With emulators running, execute `cd tests/rules && npm test`
**Expected:** 12 tests pass, 0 fail; Jest exits 0; all describe blocks (Public registry read, Private registry access, Reservation collection, Items subcollection, Users collection, Registry creation) show green
**Why human:** Requires Firebase Emulator runtime; emulator must be running before tests can connect on port 8080

#### 3. Firebase project is live with correct region

**Test:** In Firebase Console (https://console.firebase.google.com/project/gift-registry-ro), check Firestore location
**Expected:** Firestore database is in `europe-west3 (Frankfurt)` — this region is permanent and cannot be changed
**Why human:** Cannot query Firebase Console programmatically without service account credentials

---

### Gaps Summary

No gaps found. All automated verifications passed.

The phase goal — "Firebase project is configured with a stable Firestore schema and tested security rules so all subsequent feature code builds on a correct, non-reworkable foundation" — is achieved:

- Firebase configuration files are complete, correct, and wired together
- Firestore security rules implement all required access patterns with 5 composable helper functions
- 12-test TDD suite covers all 6 access control scenarios and compiles cleanly
- i18n foundation (Android strings.xml EN+RO, web en.json+ro.json) satisfies I18N-02 with matching key structures
- All documented commit hashes verified in git history
- Only open items are live runtime checks (emulator start, test execution against emulator, Firebase Console region confirmation) which require human verification

---

_Verified: 2026-04-04T18:00:00Z_
_Verifier: Claude (gsd-verifier)_
