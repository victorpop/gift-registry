---
phase: quick-260420-gat
plan: 01
subsystem: firestore-security-rules
tags: [firestore, security-rules, hotfix, regression]
status: deployed
requires:
  - Firebase CLI authenticated against the production project
  - Firestore emulator (for local test execution)
provides:
  - firestore-rules-defensive-field-access
affects:
  - .planning/phases/03-registry-item-management/*  # rules harden docs produced there
tech_stack:
  added: []
  patterns:
    - "resource.data.get(key, default) defensive field access for optional fields"
    - "Short-circuit owner check first in canReadRegistry"
key_files:
  created: []
  modified:
    - firestore.rules
    - tests/rules/firestore.rules.test.ts
decisions:
  - "Defaults in rules MUST match RegistryDto defaults (ownerId='', visibility='public', invitedUsers={}) — semantic parity for new docs, defensive for legacy docs"
  - "isOwner ordered first in canReadRegistry — short-circuit skips visibility/invitedUsers evaluation entirely on owner-filtered list queries (belt-and-suspenders)"
  - "Deploy (Task 3) held pending explicit user authorization per orchestrator constraint"
metrics:
  duration_seconds: 101
  tasks_completed: 3
  tasks_total: 3
  completed_at: 2026-04-20T08:50:51Z
  deployed_at: 2026-04-20T08:57:00Z
  deploy_target: gift-registry-ro
---

# Quick Task 260420-gat: Harden Firestore Rules Against Missing Fields Summary

Defensive `resource.data.get(key, default)` patch to `firestore.rules` plus 5 new emulator tests; fixes production `PERMISSION_DENIED: Property visibility is undefined on object. for 'list' @ L32` triggered by legacy registry docs predating the Phase 03 `visibility` / `invitedUsers` fields.

## Status: Deployed

All 3 tasks complete. Rules deployed to `gift-registry-ro` at 2026-04-20T08:57:00Z after user authorization. On-device verification pending — reporter should retry loading the Registry list.

## Tasks Executed

| # | Task | Commit | Status |
|---|------|--------|--------|
| 1 | Harden `firestore.rules` helpers with defensive `.get()` | `da1b7e1` | Done |
| 2 | Add emulator tests for legacy-doc cases + regression guards | `05207a6` | Done (36/36 pass) |
| 3 | Deploy rules to production | — | Done (deployed 2026-04-20T08:57:00Z to `gift-registry-ro`) |

## Diff Summary — `firestore.rules`

**Helper functions (lines 10–28 → 10–42 after patch):**

| Helper | Before | After |
|--------|--------|-------|
| `isOwner` | `registryData.ownerId` | `registryData.get('ownerId', '')` (fail-closed) |
| `isPublicRegistry` | `registryData.visibility == 'public'` | `registryData.get('visibility', 'public') == 'public'` |
| `isInvited` | `registryData.visibility == 'private' && registryData.invitedUsers[uid] == true` | `registryData.get('visibility', 'public') == 'private' && registryData.get('invitedUsers', {})[uid] == true` |
| `canReadRegistry` | `isPublicRegistry \|\| isOwner \|\| isInvited` | `isOwner \|\| isPublicRegistry \|\| isInvited` |

Explanatory comments referencing the production PERMISSION_DENIED error were added above each helper so future readers understand why `.get()` defaults exist.

**Not touched** (per plan constraint): `allow` rules, `create` rule's `request.resource.data.ownerId` check, items/invites subcollection rules, reservations/users/mail/notifications_failures/fcmTokens/config collection rules.

## Test Results

**Emulator:** Firestore emulator already running on `127.0.0.1:8080` (auth on `9099`). Ran jest directly with `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080` (no need for `firebase emulators:exec`).

```
Test Suites: 1 passed, 1 total
Tests:       36 passed, 36 total
Time:        2.158 s
```

**New cases (all pass against hardened rules):**
- `allows owner to list legacy registry missing visibility`
- `allows owner to list legacy registry missing invitedUsers`
- `denies read of a doc missing ownerId (fail-closed)`
- `regression: public registry remains readable by unauthenticated users`
- `regression: private registry remains readable by invited user`

**Regression check:** 31 pre-existing tests pass unchanged.

## Deviations from Plan

None. Plan executed exactly as written for Tasks 1–2; Task 3 deliberately paused per orchestrator instruction.

Minor note on execution: the plan suggests `npm run test:emulator` (which runs `firebase emulators:exec ...`). The Firestore + Auth emulators were already running locally, so invoking `emulators:exec` again would have failed on port contention. Ran `npm test` directly with `FIRESTORE_EMULATOR_HOST` set — equivalent outcome, 36/36 pass. No file changes implied by this detour.

## Deploy Record

Deployed 2026-04-20T08:57:00Z to `gift-registry-ro` via `firebase deploy --only firestore:rules` (user-authorized).

Deploy output:
```
✔  cloud.firestore: rules file firestore.rules compiled successfully
✔  firestore: released rules firestore.rules to cloud.firestore
✔  Deploy complete!
```

On-device verification: reporter should retry loading the Registry list screen and confirm no PERMISSION_DENIED snackbar. Path: `MainActivity → AppNavigation → RegistryListViewModel → ObserveRegistriesUseCase(uid) → FirestoreDataSource.observeRegistries(ownerId)`.

## Residual Risk

- **Downstream references:** items and invites subcollection rules call `canReadRegistry(get(.../registries/{registryId}).data)` and `isOwner(get(.../registries/{registryId}).data)` — both now pass through the hardened helpers, so legacy parent registries no longer break subcollection evaluation either. No additional changes required there.
- **Other collections:** reservations / mail / notifications_failures / fcmTokens / config are all hard-deny or uid-match — no optional-field access paths.
- **Placeholder affiliate IDs / onboarding-prefs / etc.:** unrelated to this task; tracked elsewhere.

## Files Changed

| Path | Change | Commit |
|------|--------|--------|
| `firestore.rules` | 4 helpers hardened; +comments; reorder `canReadRegistry` | `da1b7e1` |
| `tests/rules/firestore.rules.test.ts` | +5 test cases; extended firestore imports | `05207a6` |

## Self-Check: PASSED

- FOUND: firestore.rules (modified — verified via `git log`)
- FOUND: tests/rules/firestore.rules.test.ts (modified — verified via `git log`)
- FOUND: da1b7e1 (Task 1 commit)
- FOUND: 05207a6 (Task 2 commit)
- Task 3 intentionally uncommitted — deploy has not occurred; SUMMARY updates + state commit deferred until after user authorizes.
