---
phase: 06-notifications-email-flows
plan: "01"
subsystem: email-transport
tags: [email, fcm, notifications, templates, firestore-mail, d-05, d-17, d-18, notf-03]
dependency_graph:
  requires:
    - functions/jest harness (06-00)
    - functions/src/reservation/releaseReservation.ts (Phase 4 stub)
    - functions/src/registry/inviteToRegistry.ts (Phase 3 stub)
  provides:
    - functions/src/email/send.ts (sendEmail helper)
    - functions/src/email/templates/_shell.ts (renderShell)
    - functions/src/email/templates/expiry.ts (expiryTemplate)
    - functions/src/email/templates/invite.ts (inviteTemplate)
    - functions/src/email/templates/purchase.ts (purchaseTemplate)
    - functions/src/email/devMailLogger.ts (emulator mail logger)
    - functions/src/notifications/invitePush.ts (sendInvitePush)
  affects:
    - Plan 06-02: imports purchaseTemplate + sendEmail (already authored)
    - Plan 06-03+: any plan needing email delivery imports sendEmail
tech_stack:
  added: []
  patterns:
    - Firestore mail collection write as single email transport (D-05) — no SMTP SDK imported
    - sendEachForMulticast + stale-token cleanup for FCM fanout (D-17)
    - Conditional module.exports in index.ts for dev-only emulator listener (D-08)
    - Best-effort FCM: failures logged to notifications_failures, never propagated
key_files:
  created:
    - functions/src/email/send.ts
    - functions/src/email/templates/_shell.ts
    - functions/src/email/templates/expiry.ts
    - functions/src/email/templates/invite.ts
    - functions/src/email/templates/purchase.ts
    - functions/src/email/devMailLogger.ts
    - functions/src/notifications/invitePush.ts
    - functions/src/__tests__/emailTemplates.test.ts
    - functions/src/__tests__/sendEmail.test.ts
    - functions/src/__tests__/releaseReservation.test.ts
    - functions/src/__tests__/inviteToRegistry.test.ts
  modified:
    - functions/src/index.ts (added devMailLogger conditional export)
    - functions/src/reservation/releaseReservation.ts (stub replaced)
    - functions/src/registry/inviteToRegistry.ts (stub replaced + FCM push added)
decisions:
  - sendEmail validates `to` and `subject` presence; throws Error (not HttpsError) since it is a module-level helper, not a callable
  - releaseReservation defaults locale to 'en' for giver emails — giver locale not stored on reservation doc (D-14 fallback)
  - inviteToRegistry defaults locale to 'en' for invite email — recipient locale unknown at invite time (D-14 fallback)
  - invitePush.ts handles both sendEachForMulticast failure AND batch.commit cleanup failure — each writes to notifications_failures independently
  - Test strategy uses mutable module-level store variable to allow per-test data overrides without re-importing (jest.mock hoisting prevents import-time store injection)
metrics:
  duration: ~7min
  completed_date: "2026-04-19T19:19:00Z"
  tasks_completed: 2
  files_modified: 14
---

# Phase 6 Plan 01: Email Transport Layer + Stub Replacement Summary

sendEmail helper + three locale-aware email templates (expiry, invite, purchase) via Firestore mail collection write (D-05), dev-only emulator mail logger, and stub replacement in releaseReservation + inviteToRegistry with existing-user FCM push (D-17) and stale-token cleanup.

## What Was Built

### Task 1: sendEmail Helper + Templates + Emulator Mail Logger

**sendEmail contract** (`functions/src/email/send.ts`):
- Input: `{ to: string, subject: string, html: string, text: string }`
- Output: `Promise<void>` — writes one doc to `mail/` collection with shape `{ to, message: { subject, html, text } }`
- Error behaviour: throws `Error("sendEmail: 'to' and 'subject' are required")` if either is empty; propagates Firestore errors to caller
- No SMTP SDK import — the Firebase Trigger Email extension handles delivery (D-05, D-06)

**Template interfaces:**

| Template | Interface | Export |
|----------|-----------|--------|
| Expiry | `ExpiryVars { itemName, registryName, reReserveUrl }` | `expiryTemplate(vars, locale)` |
| Invite | `InviteVars { ownerName, registryName, registryUrl }` | `inviteTemplate(vars, locale)` |
| Purchase | `PurchaseVars { giverFirstName, giverLastName, itemName, registryName, registryUrl }` | `purchaseTemplate(vars, locale)` |

All three: `locale: "en" | "ro"`, return `{ subject: string, html: string, text: string }`.

**Shared shell** (`functions/src/email/templates/_shell.ts`):
- `renderShell({ heading, bodyHtml, ctaLabel, ctaUrl, footerText, preheader })` — complete 600px HTML email document with bulletproof CTA table, `#6750A4` header band, `#FFFBFE` outer background

**Null giver name fallback** (purchaseTemplate):
- `giverFirstName === null || empty` → body uses `"Someone purchased {itemName}"` (en) / `"Cineva a cumpărat {itemName}"` (ro); never renders "null null"

**devMailLogger** (`functions/src/email/devMailLogger.ts`):
- `onDocumentCreated` on `mail/{docId}` — logs `To`, `Subject`, `Text` to console
- Only exported from `index.ts` when `process.env.FUNCTIONS_EMULATOR === "true"` (D-08)

### Task 2: Stub Replacement + invitePush Helper

**sendInvitePush contract** (`functions/src/notifications/invitePush.ts`):
- Input: `{ invitedUid, registryId, registryName, locale }`
- Reads all tokens from `users/{invitedUid}/fcmTokens` subcollection
- If `tokens.length === 0` → returns without calling FCM (not an error)
- Calls `getMessaging().sendEachForMulticast({ tokens, notification: { title, body: registryName }, data: { type: "invite", registryId } })`
- Stale-token cleanup: deletes docs for tokens receiving `messaging/registration-token-not-registered` or `messaging/invalid-registration-token`
- Failure semantics: any error (FCM throw OR cleanup throw) → logs doc to `notifications_failures` with `{ type: "invite_push", userId, registryId, error, timestamp }` and returns (never propagates)

**Stub replacement summary:**

| File | Before | After |
|------|--------|-------|
| `releaseReservation.ts` | `console.log("[STUB] Email would be sent...")` | `expiryTemplate(...) → sendEmail(...)` in try/catch |
| `inviteToRegistry.ts` | `console.log("[STUB] Invite email would be sent...")` | `inviteTemplate(...) → sendEmail(...)` + `sendInvitePush(...)` guarded by `isExistingUser && invitedUid` |

**D-17 guard pattern:**
```ts
if (isExistingUser && invitedUid) {
  await sendInvitePush({ invitedUid, registryId, registryName, locale });
}
```
D-18: when `isExistingUser === false`, the block is skipped entirely — non-user invites are email-only.

**releaseReservation data widening:** `emailData` now captures `itemName` (from item doc inside transaction) and `registryName` (from registry doc inside transaction); both read within the same transaction to maintain consistency. Fallbacks: `"your gift"` for missing item title, `"a registry"` for missing registry title.

**Owner name resolution** (inviteToRegistry): best-effort `admin.auth().getUser(request.auth.uid)` — uses `displayName || email.split("@")[0] || "Someone"`. Wrapped in try/catch; failures warn-log and use fallback.

### How Downstream Plans Should Import

```ts
// Email delivery
import { sendEmail } from "../email/send";

// Templates
import { expiryTemplate, ExpiryVars } from "../email/templates/expiry";
import { inviteTemplate, InviteVars } from "../email/templates/invite";
import { purchaseTemplate, PurchaseVars } from "../email/templates/purchase";

// FCM invite push (existing-user only)
import { sendInvitePush } from "../notifications/invitePush";
```

Plan 06-02 imports `purchaseTemplate` and `sendEmail` directly for the `onPurchaseNotification` trigger.

## Test Coverage

37 total tests (all passing), 5 test suites:

| Suite | Tests | Coverage |
|-------|-------|---------|
| emailTemplates.test.ts | 16 | All 3 templates × 2 locales; null giver fallback |
| sendEmail.test.ts | 3 | Happy path, empty `to`, empty `subject` |
| releaseReservation.test.ts | 4 | Active→expired (mail written), already-expired (no mail), missing itemName fallback, sendEmail-throws recovery |
| inviteToRegistry.test.ts | 7 | Existing user (email + FCM), non-user (email-only), sendEmail-error recovery, FCM failure logging, stale-token cleanup, no-tokens short-circuit, permission-denied regression |
| smoke.test.ts | 2 | Pre-existing from Plan 06-00 |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Type] makeCallableRequest type compatibility**
- **Found during:** Task 2, inviteToRegistry test
- **Issue:** `inviteToRegistry.run()` expects full `CallableRequest<any>` type including `rawRequest` and `acceptsStreaming` fields that the plan's test scaffold omitted
- **Fix:** Added `rawRequest: {}` and `acceptsStreaming: false` to `makeCallableRequest()` helper; removed `// @ts-ignore` comments; declared return type as `any`
- **Files modified:** `functions/src/__tests__/inviteToRegistry.test.ts`
- **Commit:** d1ecdb3

**2. [Rule 2 - Missing guard] mutable store for releaseReservation tests**
- **Found during:** Task 2, releaseReservation test
- **Issue:** Original plan called for `firebase-functions-test` wrapping, but the handler directly calls `db.runTransaction()` — using a module-level mutable store variable enables per-test data variants (e.g. already-expired status) without re-requiring
- **Fix:** Used mutable `mockStore` variable reset in `beforeEach` with a `shouldMailThrow` flag for error injection
- **Files modified:** `functions/src/__tests__/releaseReservation.test.ts`
- **Commit:** d1ecdb3

## Known Stubs

None — all Phase 3/4 email stubs replaced. purchaseTemplate is fully authored and ready for Plan 06-02 to consume.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 1ad0ef9 | feat(06-01): email send helper + 3 locale-aware templates + emulator mail logger |
| 2 | d1ecdb3 | feat(06-01): replace stubs with email + invite FCM push (D-17/D-18) |

## Self-Check: PASSED
