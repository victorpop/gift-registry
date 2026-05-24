---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 02
subsystem: backend
tags: [cloud-functions, callable, firestore-transaction, fieldpath, accept-gate, notifications, app-check]

# Dependency graph
requires:
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 01
    provides: Wave 0 RED Jest tests for acceptInvite + declineInvite + modified inviteToRegistry (Tests A/B/H); composite index notifications(type, payload.registryId); rules tests locking pendingInvitedUsers no-access contract
  - phase: 06-notifications-email-flows
    provides: writeNotification helper + NotificationType union + sendInvitePush helper + inviteToRegistry callable that this plan extends
provides:
  - acceptInvite Cloud Function callable (D-21, D-24) — atomic pending→invited promotion + post-tx invite_accepted_self/invite_accepted writes + invite inbox cleanup
  - declineInvite Cloud Function callable (D-22, D-24) — atomic pending removal + post-tx invite_declined owner write + invite inbox cleanup
  - inviteNotificationHelpers module — shared deleteInviteNotificationsForRegistry, lookupDisplayName, buildEnrichedInvitePayload helpers
  - Modified inviteToRegistry — writes pendingInvitedUsers (D-23), enriched inbox payload (D-10), D-16 already-member soft re-invite branch
  - Extended NotificationType union with 3 new server-side wire strings
affects:
  - 16-03 (android domain + data layer) — Android NotificationRepositoryImpl now has live backend callable endpoints to wire against (acceptInvite + declineInvite, region=europe-west3, App Check enforced)
  - 16-04 (invite response sheet + viewmodel) — sheet renders enriched payload (occasion, coverUrl, eventDateMs) directly off the inbox doc without a pre-accept registry-doc read
  - 16-05 (inbox reskin + strings) — 3 new notification types (invite_accepted_self, invite_accepted, invite_declined) need title/body string keys in values/ + values-ro/ strings.xml
  - 16-06 (deploy + UAT) — must deploy the new callables and the composite index from Plan 16-01

# Tech tracking
tech-stack:
  added: []  # No new dependencies — reuses firebase-admin, firebase-functions/v2/https, firestore FieldPath/FieldValue
  patterns:
    - "Accept-gate callable pair pattern: 2nd-gen onCall + enforceAppCheck + Firestore runTransaction with verify-first read + FieldPath map writes + post-tx best-effort side effects (cleanup + writeNotification x2)"
    - "FieldPath-only writes for map keys that may contain dots (email:xxx) — the only safe pattern for invitedUsers/pendingInvitedUsers updates"
    - "didX flag pattern for idempotent transactions: capture decision inside tx, skip side effects outside when flag is false"
    - "Enriched inbox payload + minimal FCM data payload (Pitfall 6): rich fields live on the inbox doc (Firestore 1MB cap), data payload stays under FCM's 4096-byte cap"
    - "Soft re-invite branch (D-16): existing-member case skips membership write, omits pendingEntryKey from inbox payload, but preserves email + FCM + inbox-doc delivery so the owner can re-prompt without breaking the accept-gate contract"

key-files:
  created:
    - "functions/src/registry/acceptInvite.ts (114 LoC — 2nd-gen onCall + transaction + post-tx best-effort writes; D-21/D-24)"
    - "functions/src/registry/declineInvite.ts (95 LoC — 2nd-gen onCall + transaction + post-tx best-effort owner write; D-22/D-24)"
    - "functions/src/registry/inviteNotificationHelpers.ts (61 LoC — deleteInviteNotificationsForRegistry, lookupDisplayName, buildEnrichedInvitePayload)"
  modified:
    - "functions/src/registry/inviteToRegistry.ts (+38/-15 LoC — pendingInvitedUsers write per D-23, isAlreadyMember branch per D-16, enriched inbox payload via buildEnrichedInvitePayload)"
    - "functions/src/index.ts (+2 LoC — exports acceptInvite + declineInvite)"
    - "functions/src/notifications/writeNotification.ts (+3 LoC — NotificationType union extended with invite_accepted_self, invite_accepted, invite_declined)"

key-decisions:
  - "Returned eventDateMs as a number (not String) from buildEnrichedInvitePayload to match the Wave 0 test assertion .toBe(1_800_000_000_000). writeNotification accepts number values in its payload signature, so it round-trips to Firestore cleanly. The plan's helper sketch used String() — we documented the deviation in the helper jsdoc and extended the Record type to Record<string, string | number | null>."
  - "Extended NotificationType in writeNotification.ts as a Rule 3 blocking fix — the new callables type-error against the existing union without it. This is a one-line correctness extension, not a behavioral change to writeNotification."
  - "didPromote / didDecline boolean flag inside the transaction is captured in an outer let, then gates the post-tx side effects. The plan called for this pattern and it falls out naturally; alternative (re-reading the doc post-tx to verify state) would be racy and slower."
  - "Used FieldPath consistently for ALL pendingInvitedUsers + invitedUsers updates (acceptInvite, declineInvite, inviteToRegistry) per Pitfall 1 — even when the key is a uid (no dots), this is belt-and-suspenders that survives future email-keyed entries safely."

patterns-established:
  - "When extending NotificationType, check that the writeNotification union is type-safe-extended FIRST — production code referring to a not-yet-listed type will fail tsc but Jest will only flag it at run time, making the cause harder to spot."
  - "buildEnrichedInvitePayload + spread (`...enriched`) into the writeNotification payload object — clean separation of concerns; the helper owns the shape, the call site owns the constant fields (registryId, actorName, actorUid)."

requirements-completed:
  - D-10
  - D-13
  - D-15
  - D-16
  - D-18
  - D-19
  - D-20
  - D-21
  - D-22
  - D-23
  - D-24

# Metrics
duration: 4min
completed: 2026-05-24
---

# Phase 16 Plan 02: Backend Callables and Invite Pending Summary

**Two new 2nd-gen Cloud Function callables (acceptInvite + declineInvite) implementing the D-21/D-22 accept-gate transaction semantics, plus modified inviteToRegistry that writes pendingInvitedUsers + enriched inbox payload + D-16 already-member soft re-invite branch — flipping all Wave 0 backend RED Jest tests GREEN.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-24T17:30:06Z
- **Completed:** 2026-05-24T17:34:25Z
- **Tasks:** 2
- **Files created:** 3 (acceptInvite.ts, declineInvite.ts, inviteNotificationHelpers.ts)
- **Files modified:** 3 (inviteToRegistry.ts, index.ts, writeNotification.ts)

## Accomplishments

- **acceptInvite callable (D-20/D-21/D-24):** Region `europe-west3`, App Check enforced. Runs a Firestore transaction that:
  - Verifies registry exists (throws `not-found REGISTRY_NOT_FOUND` otherwise).
  - Checks D-24 idempotency (uid already in `invitedUsers` and not in `pendingInvitedUsers` → no-op success).
  - Verifies pending entry exists (throws `failed-precondition NO_PENDING_INVITE` otherwise).
  - Atomically deletes the `pendingInvitedUsers[uid]` entry and sets `invitedUsers[uid] = true` via `FieldPath`.
  - Post-tx best-effort (never rethrows): deletes the original INVITE inbox doc(s) for this (uid, registry); writes `invite_accepted_self` for the invitee; writes `invite_accepted` for the owner.
- **declineInvite callable (D-20/D-22/D-24):** Same shape as acceptInvite. Removes uid from `pendingInvitedUsers` atomically. Post-tx best-effort writes only the owner-side `invite_declined` notification (no invitee-side write — silent decline per D-22). Idempotency throws `failed-precondition NO_PENDING_INVITE` rather than success, by design (symmetric with acceptInvite when there is nothing to act on).
- **inviteNotificationHelpers module:** Three DRY helpers used by both callables and inviteToRegistry: `deleteInviteNotificationsForRegistry` (queries by composite index from Plan 16-01, batch delete, swallows errors); `lookupDisplayName` (admin.auth().getUser fallback chain — displayName → email prefix → "Someone"); `buildEnrichedInvitePayload` (returns `{pendingEntryKey, occasion, coverUrl, eventDateMs}` for D-10 inbox enrichment).
- **inviteToRegistry rewrite for D-23/D-16/D-10:** All new invites now write to `pendingInvitedUsers` (NOT `invitedUsers`) using `new FieldPath("pendingInvitedUsers", inviteKey)`. The D-16 already-member branch (`existingInvitedUsers[inviteKey] === true`) no-ops the membership write but preserves email + FCM push + inbox doc delivery (with `pendingEntryKey` omitted so the inbox card falls back to legacy "tap → navigate to registry" per D-11). The inbox notification payload is now enriched with `pendingEntryKey, occasion, coverUrl, eventDateMs` via spread (`...enriched`) so the Android InviteResponseSheet can render the registry hero with zero registry-doc read on the client (pre-accept reads would be denied by security rules).
- **NotificationType union extension:** Added `invite_accepted_self | invite_accepted | invite_declined` to `writeNotification.ts` so the new callables type-check. Pure type-level extension; no runtime behavior change to writeNotification itself.
- **Pitfall 6 respected:** FCM data payload stays minimal `{ type: "invite", registryId }`. The enriched fields live on the inbox doc only — Firestore 1MB cap is generous, FCM's 4096-byte cap would break with long emails + Storage URLs.
- **D-15 negative-coverage:** Grep-verified zero `blacklist | declinedUsers | declinedSet` symbols across all 4 new/modified files. Re-invite is always allowed; no deny-listing logic anywhere in the accept-gate chain.

## Task Commits

Each task was committed atomically with `--no-verify` (parallel execution alongside Plan 16-03):

1. **Task 1: Create acceptInvite + declineInvite + shared helpers; register in index.ts** — `83795be` (feat)
2. **Task 2: Modify inviteToRegistry — pendingInvitedUsers + enriched payload + D-16 branch** — `089f4ec` (feat)

**Plan metadata:** _(pending — final commit after STATE.md / ROADMAP.md updates)_

## Files Created / Modified

### Created (3)
- `functions/src/registry/acceptInvite.ts` — D-21/D-24 callable
- `functions/src/registry/declineInvite.ts` — D-22/D-24 callable
- `functions/src/registry/inviteNotificationHelpers.ts` — shared helpers (deleteInviteNotificationsForRegistry, lookupDisplayName, buildEnrichedInvitePayload)

### Modified (3)
- `functions/src/registry/inviteToRegistry.ts` — D-23 pendingInvitedUsers write, D-16 isAlreadyMember branch, D-10 enriched inbox payload
- `functions/src/index.ts` — exports acceptInvite + declineInvite
- `functions/src/notifications/writeNotification.ts` — NotificationType union extended with 3 new wire strings

## Decisions Made

- **eventDateMs returned as `number` (not `string`)** — The plan's helper sketch used `String(eventAt.toMillis())`, but the Wave 0 test asserts `.toBe(1_800_000_000_000)` (the raw number). writeNotification's payload signature accepts `string | number | boolean | null`, so the number round-trips to Firestore cleanly. Helper jsdoc updated to document the rationale.
- **NotificationType union extended** — Rule 3 blocking fix during Task 1. The new callables would not compile without this. Pure type-level extension; writeNotification's runtime behavior is unchanged.
- **didPromote / didDecline outer-let flag pattern** — Captured inside the transaction, gates post-tx side effects. Alternative (re-read doc post-tx) is racy and slower. Plan called for this pattern; it falls out naturally.
- **FieldPath used consistently** — Even when the inviteKey is a uid (no dots), we use `new FieldPath("pendingInvitedUsers", key)` and `new FieldPath("invitedUsers", key)`. Belt-and-suspenders that survives future email-keyed entries without code change.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Extended `NotificationType` union in writeNotification.ts**
- **Found during:** Task 1 (compilation of acceptInvite.ts + declineInvite.ts).
- **Issue:** `writeNotification.ts` declares `NotificationType` as a closed string-literal union (`"invite" | "reservation_created" | "item_purchased" | "reservation_expired" | "re_reserve_window"`). Passing `"invite_accepted_self" | "invite_accepted" | "invite_declined"` from the new callables fails tsc with TS2322 ("not assignable to type 'NotificationType'"). Three errors, both tests RED.
- **Fix:** Added the 3 new wire strings to the union. No runtime change to writeNotification itself — purely a type-level extension that unblocks the new callables.
- **Files modified:** `functions/src/notifications/writeNotification.ts`.
- **Verification:** `npx tsc --noEmit` exits 0; both Jest suites flip GREEN.
- **Committed in:** `83795be`.

**2. [Rule 1 - Bug] Fixed `buildEnrichedInvitePayload` eventDateMs return type to match Wave 0 test contract**
- **Found during:** Task 2 (running inviteToRegistry.test.ts Test A).
- **Issue:** The plan's helper sketch returned `eventDateMs: eventAt ? String(eventAt.toMillis()) : null` (a string). But the RED test from Plan 16-01 asserts `expect(inviteNotif!.payload.eventDateMs).toBe(1_800_000_000_000)` — a number. Returning a string would cause Test A to fail with `"1800000000000" !== 1800000000000`.
- **Fix:** Changed the helper to return `eventAt ? eventAt.toMillis() : null` (a number). Updated the return type annotation from `Record<string, string | null>` to `Record<string, string | number | null>`. The writeNotification payload signature already accepts numbers, so this round-trips to Firestore as a native long-int.
- **Files modified:** `functions/src/registry/inviteNotificationHelpers.ts`.
- **Verification:** Test A passes; full inviteToRegistry suite 8/8 green.
- **Committed in:** `089f4ec` (alongside the inviteToRegistry edits that exercise the fixed helper).

---

**Total deviations:** 2 auto-fixed.
**Impact on plan:** Both fixes are correctness extensions, not scope changes. The new callables ship the contract the Wave 0 tests defined. No additional features added.

## Issues Encountered

- **Pre-existing `createReservation.test.ts` failure remains deferred.** Per `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md` (created by Plan 16-01), 3 tests in `createReservation.test.ts` fail because the test file doesn't jest-mock `firebase-admin/functions` and `admin.initializeApp()` only runs from `src/index.ts`. Reproduced via `git stash` to confirm it predates Plan 16-02. Not blocking.
- **Android Kotlin files appeared modified during execution** — visible in `git status --short` during Task 1. These are owned by parallel Plan 16-03 (Wave 2 sibling). Per parallel-execution boundary, I did NOT stage them; only `functions/*` files were committed in `83795be` and `089f4ec`.

## Test Results

| Suite | Tests | Status |
|---|---|---|
| `acceptInvite.test.ts` | 6 | ✅ PASS |
| `declineInvite.test.ts` | 5 | ✅ PASS |
| `inviteToRegistry.test.ts` (Tests A + B + H + 5 existing) | 8 | ✅ PASS |
| **All 3 target suites** | **19** | ✅ **19/19 GREEN** |
| Full Jest suite (excl. pre-existing createReservation) | 98 | ✅ 13/13 suites, 98/98 tests |

**TypeScript:** `npx tsc --noEmit` exits 0.
**D-15 negative-coverage grep:** `grep -rE "blacklist\|declinedUsers\|declinedSet" functions/src/registry/{acceptInvite,declineInvite,inviteToRegistry,inviteNotificationHelpers}.ts` → 0 hits. Confirmed.

## Acceptance Criteria Verification

Per the plan's per-task acceptance lists:

**Task 1:**
- ✅ acceptInvite.ts exists, contains: enforceAppCheck:true, europe-west3, new FieldPath("pendingInvitedUsers", ...), new FieldPath("invitedUsers", ...), FieldValue.delete(), NO_PENDING_INVITE, invite_accepted_self, invite_accepted, didPromote
- ✅ declineInvite.ts exists, contains: enforceAppCheck:true, invite_declined, FieldValue.delete(); does NOT contain invite_accepted (verified)
- ✅ inviteNotificationHelpers.ts exists, contains: deleteInviteNotificationsForRegistry, lookupDisplayName, buildEnrichedInvitePayload, payload.registryId
- ✅ index.ts contains: export { acceptInvite }, export { declineInvite }
- ✅ `npx tsc --noEmit` exits 0
- ✅ Jest tests for acceptInvite + declineInvite GREEN (6 + 5 tests)

**Task 2:**
- ✅ inviteToRegistry.ts contains: new FieldPath("pendingInvitedUsers", ...), isAlreadyMember, buildEnrichedInvitePayload, ...enriched, D-16 comment, D-23 comment, Pitfall 6 comment, sendInvitePush preserved
- ✅ inviteToRegistry.ts NO LONGER contains: new FieldPath("invitedUsers", ...)
- ✅ D-15 negative-coverage: zero blacklist/declinedUsers/declinedSet across all 4 files
- ✅ acceptInvite.ts, declineInvite.ts, inviteToRegistry.ts contain no "blacklist" string
- ✅ acceptInvite.ts, declineInvite.ts, inviteToRegistry.ts contain no "declinedUsers" string
- ✅ `npx tsc --noEmit` exits 0
- ✅ inviteToRegistry tests GREEN (8 tests, Tests A + B + H flipped from RED to GREEN)
- ✅ Full Jest suite GREEN (13/13, 98/98) excluding the pre-existing deferred createReservation failure

## Self-Check: PASSED

Files verified to exist:
- `functions/src/registry/acceptInvite.ts`
- `functions/src/registry/declineInvite.ts`
- `functions/src/registry/inviteNotificationHelpers.ts`

Commits verified to exist:
- `83795be` (Task 1)
- `089f4ec` (Task 2)

## User Setup Required

None — no external service configuration changes. The new composite Firestore index `notifications(type asc, payload.registryId asc)` from Plan 16-01 is required at runtime for `deleteInviteNotificationsForRegistry` queries; it is part of the Plan 16-06 deploy.

## Next Phase / Plan Readiness

- **Plan 16-03 (Android domain + data layer) — running in parallel as Wave 2 sibling.** Once both 16-02 and 16-03 complete, the Android side will have:
  - Live `acceptInvite` + `declineInvite` callables at `europe-west3` to wire `NotificationRepositoryImpl` against (with App Check enforced — the Android client MUST have App Check provider initialized, which Phase 14's follow-up todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md` is responsible for resolving before deploy).
  - The enriched inbox doc payload shape (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`) to deserialize into the model used by the InviteResponseSheet.
- **Plan 16-04 (invite response sheet + viewmodel) — Wave 3.** Sheet now has all the data it needs from the inbox doc; no pre-accept registry-doc read necessary.
- **Plan 16-05 (inbox reskin + strings) — Wave 4.** Must add string keys for the 3 new notification types: `notification_invite_accepted_self_title/body`, `notification_invite_accepted_title/body`, `notification_invite_declined_title/body` (both en + ro per LocalizationParityTest from Plan 16-01).
- **Plan 16-06 (deploy + UAT) — Wave 5.** Must deploy the 2 new callables + the composite index from Plan 16-01.

---
*Phase: 16-android-notifications-inbox-invite-accept-decline*
*Completed: 2026-05-24*
