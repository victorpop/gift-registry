---
phase: 06-notifications-email-flows
plan: "02"
subsystem: notifications
tags: [cloud-functions, firestore-trigger, fcm, push-notifications, email, cloud-tasks, d-01, d-02, d-04, d-10, d-11, d-13, d-14, d-15, notf-01, notf-02]
dependency_graph:
  requires:
    - phase: 06-00
      provides: Jest harness + in-memory Firestore double
    - phase: 06-01
      provides: sendEmail helper + purchaseTemplate + notifications_failures pattern
    - phase: 04-reservation-system
      provides: createReservation (cloudTaskName storage pattern), releaseReservation (guard-transaction pattern)
  provides:
    - functions/src/reservation/confirmPurchase.ts (confirmPurchase callable)
    - functions/src/notifications/onPurchaseNotification.ts (onPurchaseNotification trigger)
  affects:
    - Plan 06-03: Android ConfirmPurchaseUseCase will call confirmPurchase callable
    - Plan 06-04/05: Web giver-facing confirmation CTA calls the same callable
tech_stack:
  added: []
  patterns:
    - confirmPurchase: transaction-first, Cloud Task cancel post-commit; NOT_FOUND (code=5) swallowed
    - onPurchaseNotification: reserved→purchased guard + purchaseNotificationSentAt sentinel transaction
    - Failure logging to notifications_failures; never rethrow from trigger handlers (D-15)
    - Locale-aware FCM copy and purchaseTemplate selection from users/{uid}.preferredLocale (D-14)
    - Stale FCM token cleanup in onPurchaseNotification mirrors invitePush pattern
key_files:
  created:
    - functions/src/reservation/confirmPurchase.ts
    - functions/src/notifications/onPurchaseNotification.ts
    - functions/src/__tests__/confirmPurchase.test.ts
    - functions/src/__tests__/onPurchaseNotification.test.ts
  modified:
    - functions/src/index.ts (added confirmPurchase + onPurchaseNotification exports)
key_decisions:
  - "confirmPurchase allows unauthenticated callers — guest givers have no Firebase UID (CONTEXT.md guest access constraint)"
  - "Cloud Task deleteTask called AFTER transaction commit, never inside — prevents partial-success state if deleteTask itself throws"
  - "onPurchaseNotification reads registry after claiming sentinel so opt-out check happens only on first genuine invocation"
  - "Giver name resolved via reservations collection query (status==purchased, itemId match) — purchaseTemplate null-safe so miss is non-fatal"
  - "notifyOnPurchase defaults true when field absent from registry doc — opt-out is explicit, not default"
patterns_established:
  - "Callable guard pattern: check missing-arg → transaction guard → post-transaction side effect"
  - "Trigger sentinel pattern: after.field early-exit + transaction claim before any fanout"
  - "notifications_failures writes are best-effort (wrapped in try/catch); logging failure itself is also caught"
requirements-completed: [NOTF-01, NOTF-02]
duration: ~10min
completed: "2026-04-19"
---

# Phase 6 Plan 02: Purchase Confirmation Callable + Purchase Notification Trigger Summary

`confirmPurchase` onCall callable (D-01/D-02) and `onPurchaseNotification` Firestore trigger (D-04/D-10/D-11/D-13/D-14/D-15) fully implemented and unit tested — the two server primitives that complete NOTF-01 (push) and NOTF-02 (email).

## Performance

- **Duration:** ~10 min
- **Started:** 2026-04-19T19:00:00Z
- **Completed:** 2026-04-19T19:12:39Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- `confirmPurchase` callable transitions reservation + item to `purchased` atomically and cancels the stored Cloud Task after commit; giver-facing RESERVATION_EXPIRED surfaces as `failed-precondition` when timer lapsed first
- `onPurchaseNotification` trigger fires only on `reserved→purchased`; claims sentinel in a transaction to prevent duplicate notifications; fans out push and email to opted-in owners; stale FCM tokens are cleaned up; all failures logged to `notifications_failures` without rethrowing
- 17 new unit tests (7 for confirmPurchase, 10 for onPurchaseNotification) — all passing; full suite 54/54 green after additions

## API Reference

### confirmPurchase callable

**Request:** `{ reservationId: string }`

**Response:** `{ success: true }`

**Error codes:**
| Code | Message | When |
|------|---------|------|
| `invalid-argument` | `MISSING_RESERVATION_ID` | `reservationId` absent or not a string |
| `not-found` | `RESERVATION_NOT_FOUND` | Reservation doc does not exist |
| `failed-precondition` | `RESERVATION_EXPIRED` | `reservation.status !== "active"` |

**No auth required** — guest givers (anonymous) may confirm purchase.

### onPurchaseNotification trigger

**Document:** `registries/{registryId}/items/{itemId}`

**Guard sequence:**
1. `before.status === "reserved" && after.status === "purchased"` — otherwise no-op
2. `after.purchaseNotificationSentAt` truthy — no-op (own write re-trigger)
3. Transaction: read item; if `purchaseNotificationSentAt` already set → `__ALREADY_CLAIMED__`; else write sentinel
4. Read `registries/{id}` → `ownerId`, `notifyOnPurchase` (default true); if `false` → log `[NOTF] Skipped for owner {uid} — opted out` and return
5. Read `users/{uid}` → `preferredLocale` (default "en"), `email`; fallback to `admin.auth().getUser(uid)` for email
6. Query `reservations` for most-recent purchased reservation for this item → giver name (non-fatal on miss)
7. FCM fanout to all `users/{uid}/fcmTokens` tokens; delete stale UNREGISTERED/INVALID_ARGUMENT tokens
8. Write purchase email via `purchaseTemplate(vars, locale) → sendEmail()`
9. Catch each phase separately; write to `notifications_failures` on error; never rethrow

### notifications_failures document shape

```ts
{
  type: "fcm_batch" | "email_send" | "fcm_cleanup" | "read_error";
  userId: string;
  itemId: string;
  registryId: string;
  error: string;   // Error.message or String(unknown)
  timestamp: FieldValue.serverTimestamp();
}
```

### index.ts exports after Phase 6 additions (so far)

```ts
export { fetchOgMetadata } from "./registry/fetchOgMetadata";
export { inviteToRegistry } from "./registry/inviteToRegistry";
export { createReservation } from "./reservation/createReservation";
export { releaseReservation } from "./reservation/releaseReservation";
export { resolveReservation } from "./reservation/resolveReservation";
export { confirmPurchase } from "./reservation/confirmPurchase";     // 06-02
export { onPurchaseNotification } from "./notifications/onPurchaseNotification"; // 06-02
// devMailLogger conditionally exported when FUNCTIONS_EMULATOR=true (06-01)
```

## Task Commits

1. **Task 1: confirmPurchase callable + unit tests** - `2bc1721` (feat)
2. **Task 2: onPurchaseNotification trigger + unit tests** - `196aebf` (feat)

**Plan metadata:** (docs commit — see state updates below)

## Files Created/Modified

- `functions/src/reservation/confirmPurchase.ts` — onCall callable implementing D-01/D-02
- `functions/src/notifications/onPurchaseNotification.ts` — onDocumentUpdated trigger implementing D-04/D-10/D-11/D-13/D-14/D-15
- `functions/src/__tests__/confirmPurchase.test.ts` — 7 unit tests
- `functions/src/__tests__/onPurchaseNotification.test.ts` — 10 unit tests
- `functions/src/index.ts` — added two new exports

## Decisions Made

- `confirmPurchase` has no auth check — guest givers are explicitly supported (CONTEXT.md constraint: "Guest access without account creation to reduce friction for gift givers")
- Cloud Task `deleteTask` is called after the transaction commits, never inside — prevents leaving the reservation in a committed-but-partially-cancelled state if the `deleteTask` network call fails
- `onPurchaseNotification` reads the registry doc (for opt-out flag) only after the idempotency sentinel is claimed — ensures the opt-out check only runs once even under concurrent trigger invocations
- `notifyOnPurchase` defaults to `true` when absent from registry doc — opt-out is explicit; opted-in by default aligns with owner expectation
- Giver name sourced from `reservations` collection query rather than from the item doc — reservation holds `giverName` with full name; `purchaseTemplate` handles null gracefully when query misses

## Deviations from Plan

None — plan executed exactly as written. The implementation matches all `<action>` steps and all `<acceptance_criteria>` checks pass.

## Known Stubs

None — both functions are fully wired. No placeholder returns or hardcoded empty data paths.

## Issues Encountered

None — first run of both test suites produced the expected RED (missing module) then GREEN (all pass) in a single implementation cycle.

## Next Phase Readiness

- `confirmPurchase` callable is ready for Android (Plan 06-03) to call via `httpsCallable`
- `onPurchaseNotification` trigger is deployed automatically with `firebase deploy --only functions`
- Plans 06-04/06-05 (web giver confirmation CTA) can wire directly to `confirmPurchase` — same callable, no changes needed

---
*Phase: 06-notifications-email-flows*
*Completed: 2026-04-19*
