---
phase: 06-notifications-email-flows
verified: 2026-04-19T22:32:00Z
status: human_needed
score: 10/10 must-haves verified
human_verification:
  - test: "FCM push on real Android device (foreground + background)"
    expected: "Foreground: Material3 snackbar appears at top of screen when owner is in-app. Background: notification appears in system tray. Both routes only fire when registry.notifyOnPurchase === true."
    why_human: "Cannot invoke FirebaseMessagingService.onMessageReceived or verify system-tray rendering without a connected device and a real FCM token."
  - test: "Email rendering in Gmail and Outlook"
    expected: "Expiry, invite, and purchase emails render the 600px centred shell correctly with #6750A4 header, bulletproof CTA button, and bilingual footer. No broken images or clipped content."
    why_human: "Email client rendering requires an actual send via the Trigger Email extension (or SendGrid). The unit tests verify the HTML structure but cannot verify client-specific rendering quirks."
  - test: "Re-reserve deep link opens app from email on a real device"
    expected: "Tapping the 'Re-reserve this gift' CTA in the expiry email opens the app (or web fallback) at the re-reserve screen for the correct reservationId. URL format: https://giftregistry.app/reservation/{reservationId}/re-reserve"
    why_human: "Deep link routing requires Android App Links configuration and a physical device. Cannot be verified programmatically."
  - test: "Owner opt-out end-to-end: toggle notification setting off then purchase a gift"
    expected: "After owner disables 'Email language' / push notifications in SettingsScreen, completing a purchase triggers no FCM push and no mail doc written to the mail collection."
    why_human: "The opt-out guard (notifyOnPurchase === false) is unit-tested in onPurchaseNotification.test.ts, but the full chain from UI toggle -> Firestore write -> trigger skip requires a live emulator run."
  - test: "ConfirmPurchaseBanner appears and dismisses on Android"
    expected: "A giver with an active reservation sees the banner sticky at top of RegistryDetailScreen. Tapping 'Confirm purchase' calls the callable; success snackbar 'Purchase confirmed. Thank you!' appears and banner unmounts. Tapping on an expired reservation shows 'Could not confirm. Please try again.'"
    why_human: "Compose UI rendering and snackbar display require an emulator or device."
---

# Phase 6: Notifications & Email Flows Verification Report

**Phase Goal:** Owners receive timely purchase notifications (if opted in) and the expiry and invite email flows are fully operational end-to-end
**Verified:** 2026-04-19T22:32:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Owner receives push notification + email when a gift is purchased (opted-in only, NOTF-01/NOTF-02) | VERIFIED | `onPurchaseNotification.ts` fans out FCM via `sendEachForMulticast` + email via `sendEmail` + `purchaseTemplate`; guarded by `notifyOnPurchase !== false` check (D-13); 10+ unit tests green |
| 2 | Giver receives expiry email when reservation timer lapses; re-reserve link in email initiates new reservation (NOTF-03) | VERIFIED | `releaseReservation.ts` calls `expiryTemplate` + `sendEmail`; URL format `https://giftregistry.app/reservation/${rid}/re-reserve` confirmed in source |
| 3 | Owner opting out receives neither push nor email when a gift is purchased | VERIFIED | `onPurchaseNotification.ts` logs `[NOTF] Skipped for owner {uid} — opted out` and returns early when `notifyOnPurchase === false`; unit test (Test 4 in onPurchaseNotification.test.ts) verifies no FCM, no mail doc |
| 4 | Email transport uses Firestore mail collection (Trigger Email extension) — no SMTP SDK imported | VERIFIED | `send.ts` writes to `mail` collection only; no `@sendgrid/mail` or `nodemailer` import found anywhere under `functions/src/` |
| 5 | Existing-user invite delivers FCM push + email (D-17); non-user invite is email-only (D-18) | VERIFIED | `inviteToRegistry.ts` guarded by `if (isExistingUser && invitedUid)` before calling `sendInvitePush`; non-user path skips FCM entirely |
| 6 | idempotency guard via `purchaseNotificationSentAt` inside a transaction (D-04) | VERIFIED | `onPurchaseNotification.ts` runs `db.runTransaction` to set sentinel before fanout; early-exit when `after.purchaseNotificationSentAt` already present |
| 7 | Stale FCM tokens (UNREGISTERED / INVALID_ARGUMENT) are deleted from subcollection (D-11) | VERIFIED | Both `invitePush.ts` and `onPurchaseNotification.ts` inspect `r.error?.code`, delete stale token docs in a batch |
| 8 | Android: ConfirmPurchaseBanner rendered in RegistryDetailScreen; FCM token registered via GiftRegistryMessagingService | VERIFIED | `RegistryDetailScreen.kt` renders `ConfirmPurchaseBanner` at line 285; `GiftRegistryMessagingService.kt` calls `handler.onNewToken` -> `RegisterFcmTokenUseCase`; `AndroidManifest.xml` declares service + `POST_NOTIFICATIONS` permission |
| 9 | Web: ConfirmPurchaseBanner rendered in RegistryPage; calls `httpsCallable('confirmPurchase')` | VERIFIED | `RegistryPage.tsx` imports and renders `ConfirmPurchaseBanner`; `useConfirmPurchase.ts` creates `httpsCallable(functions, 'confirmPurchase')` inside the hook; toast wired via `useToast` |
| 10 | SettingsScreen exposes email locale read/write (D-14) | VERIFIED | `SettingsViewModel.kt` injects `ObserveEmailLocaleUseCase` + `SetEmailLocaleUseCase`; `EmailLocaleRepositoryImpl.kt` reads/writes `users/{uid}.preferredLocale` |

**Score:** 10/10 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `functions/src/email/send.ts` | `sendEmail` writes to mail collection | VERIFIED | Exports `sendEmail`, writes `{ to, message: { subject, html, text } }` to Firestore `mail` collection |
| `functions/src/email/templates/_shell.ts` | Shared HTML shell helper | VERIFIED | Exports `renderShell`; used by all three templates |
| `functions/src/email/templates/expiry.ts` | `expiryTemplate` with en/ro locales | VERIFIED | Exports `expiryTemplate`, `ExpiryVars`; both locales produce non-empty subject/html/text |
| `functions/src/email/templates/invite.ts` | `inviteTemplate` with en/ro locales | VERIFIED | Exports `inviteTemplate`, `InviteVars` |
| `functions/src/email/templates/purchase.ts` | `purchaseTemplate` with null-name fallback | VERIFIED | Exports `purchaseTemplate`, `PurchaseVars` |
| `functions/src/email/devMailLogger.ts` | Dev-only Firestore listener | VERIFIED | Exports `devMailLogger`; conditionally exported from `index.ts` via `FUNCTIONS_EMULATOR === "true"` guard |
| `functions/src/notifications/invitePush.ts` | `sendInvitePush` with stale-token cleanup + failure logging | VERIFIED | Exports `sendInvitePush`; contains `messaging/registration-token-not-registered` and `notifications_failures` writes |
| `functions/src/reservation/releaseReservation.ts` | Stub replaced with `expiryTemplate` + `sendEmail` | VERIFIED | Imports both; `[STUB]` string absent; re-reserve URL present |
| `functions/src/registry/inviteToRegistry.ts` | Stub replaced with `inviteTemplate` + `sendEmail` + `sendInvitePush` (guarded) | VERIFIED | All three imported; `[STUB]` string absent; `if (isExistingUser && invitedUid)` guard present |
| `functions/src/reservation/confirmPurchase.ts` | `onCall` callable, D-01/D-02 | VERIFIED | Exported from `index.ts`; contains status guard, `failed-precondition` throw, `code === 5` Cloud Task swallow |
| `functions/src/notifications/onPurchaseNotification.ts` | `onDocumentUpdated` trigger, opt-out + idempotency + fanout | VERIFIED | Exported from `index.ts`; contains all required patterns |
| `app/.../FcmTokenRepository.kt` + `FcmTokenRepositoryImpl.kt` | FCM token registration interface + impl | VERIFIED | `FcmTokenRepositoryImpl` writes to `users/{uid}/fcmTokens/{token}` with correct shape; bound in `DataModule` |
| `app/.../EmailLocaleRepository.kt` + `EmailLocaleRepositoryImpl.kt` | preferredLocale read/write | VERIFIED | `EmailLocaleRepositoryImpl` reads/writes `preferredLocale` on `users/{uid}`; bound in `DataModule` |
| `app/.../ConfirmPurchaseUseCase.kt` | Delegates to `ReservationRepository.confirmPurchase` | VERIFIED | Exists; domain-layer boundary clean |
| `app/.../GiftRegistryMessagingService.kt` | FCM service wired to `RegisterFcmTokenUseCase` + `NotificationBus` | VERIFIED | `MessagingHandler` injects both; declared in `AndroidManifest.xml` |
| `app/.../NotificationBus.kt` | SharedFlow for foreground push routing | VERIFIED | Exists in `ui/notifications/` |
| `app/.../ConfirmPurchaseBanner.kt` | Compose component per UI-SPEC Contract 1 | VERIFIED | Exists; rendered in `RegistryDetailScreen` at line 285 |
| `web/src/features/reservation/useConfirmPurchase.ts` | Hook wrapping `httpsCallable('confirmPurchase')` | VERIFIED | `httpsCallable` created inside hook on each call (mirrors `useResolveReservation` pattern) |
| `web/src/features/reservation/ConfirmPurchaseBanner.tsx` | React component per UI-SPEC Contract 1 web variant | VERIFIED | Imports `useConfirmPurchase` + `useToast`; no `font-semibold` used |
| `web/src/pages/RegistryPage.tsx` | Renders `ConfirmPurchaseBanner` when active reservation exists | VERIFIED | Imported and rendered at line 183 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `releaseReservation.ts` | `email/send.ts` + `email/templates/expiry.ts` | Import + call after transaction | WIRED | Both imported; `expiryTemplate(...)` + `sendEmail(...)` called in error-safe try/catch block |
| `inviteToRegistry.ts` | `email/send.ts` + `email/templates/invite.ts` | Stub replacement | WIRED | Both imported; `inviteTemplate(...)` + `sendEmail(...)` replace prior `[STUB]` log |
| `inviteToRegistry.ts` | `notifications/invitePush.ts` | `if (isExistingUser && invitedUid)` guard | WIRED | `sendInvitePush` imported and called only on existing-user path (D-17); non-user path skips (D-18) |
| `index.ts` | `devMailLogger` | `FUNCTIONS_EMULATOR === "true"` conditional require | WIRED | Lines 24-26 in `index.ts` conditionally attach the logger |
| `confirmPurchase.ts` | `CloudTasksClient.deleteTask` | After-transaction cancellation | WIRED | `tasksClient.deleteTask({ name: cloudTaskName })` called post-transaction; NOT_FOUND code 5 swallowed |
| `onPurchaseNotification.ts` | `sendEmail` + `purchaseTemplate` | Email fanout after opt-in check | WIRED | `purchaseTemplate(vars, locale)` then `sendEmail(...)` in try/catch |
| `onPurchaseNotification.ts` | `getMessaging().sendEachForMulticast` | FCM fanout to owner tokens | WIRED | Called with `tokens`, `notification`, `data: { registryId, itemId, type: "purchase" }` |
| `onPurchaseNotification.ts` | `items/{id}.purchaseNotificationSentAt` | Idempotency sentinel inside `runTransaction` | WIRED | `tx.update(itemRef, { purchaseNotificationSentAt: FieldValue.serverTimestamp() })` inside transaction |
| `RegistryDetailScreen.kt` | `ConfirmPurchaseBanner` | Conditional composable render | WIRED | `ConfirmPurchaseBanner(...)` at line 285 |
| `RegistryPage.tsx` | `ConfirmPurchaseBanner` + `useActiveReservation` | Conditional render when `active != null` | WIRED | Rendered at line 183 with `reservationId={active.reservationId}` |
| `useConfirmPurchase.ts` | Firebase `httpsCallable('confirmPurchase')` | Hook-internal callable creation | WIRED | `httpsCallable(functions, "confirmPurchase")` inside `confirm()` callback |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| `onPurchaseNotification.ts` | `notifyOnPurchase` | `registries/{registryId}` Firestore doc | Yes — reads live doc after transaction commits | FLOWING |
| `onPurchaseNotification.ts` | `locale` | `users/{ownerUid}.preferredLocale` Firestore doc | Yes — reads live user doc; falls back to `"en"` when field absent | FLOWING |
| `onPurchaseNotification.ts` | FCM `tokens` | `users/{ownerUid}/fcmTokens` subcollection | Yes — full subcollection get; skips when empty | FLOWING |
| `onPurchaseNotification.ts` | `giverFirstName/giverLastName` | `reservations` query where `itemId == id AND status == purchased` | Yes — Firestore query; graceful null fallback for template | FLOWING |
| `releaseReservation.ts` | `itemName`, `registryName` | Item + registry docs read inside transaction | Yes — reads live item/registry docs; string fallbacks `"your gift"` / `"a registry"` | FLOWING |
| `RegistryPage.tsx` | `active` (active reservation) | `useActiveReservation` hook (established in Phase 5) | Yes — real-time Firestore listener from Phase 5 | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All functions unit tests pass | `cd functions && npm test` | 54 tests passed, 7 suites | PASS |
| TypeScript build clean | `cd functions && npm run build` | Exit 0, no errors | PASS |
| Web tests pass (incl. ConfirmPurchaseBanner) | `cd web && npx vitest run` | 107 tests passed, 21 suites | PASS |
| No `[STUB]` strings in Phase 6 functions files | `grep -rn '\[STUB\]'` across modified files | No output | PASS |
| `@sendgrid/mail` not imported | `grep -rn '@sendgrid' functions/src/` | No results | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| NOTF-01 | 06-02, 06-03, 06-04, 06-05 | Owner receives push notification when a gift is purchased (if opted in) | SATISFIED | `onPurchaseNotification.ts` FCM fanout + `GiftRegistryMessagingService.kt` receive path + `ConfirmPurchaseBanner` on both Android and web trigger the callable |
| NOTF-02 | 06-02, 06-03, 06-04, 06-05 | Owner receives email when a gift is purchased (if opted in) | SATISFIED | `onPurchaseNotification.ts` writes `purchaseTemplate` email to mail collection; `EmailLocaleRepositoryImpl` stores `preferredLocale` for template locale selection |
| NOTF-03 | 06-01 | Giver receives expiration email when reservation timer expires | SATISFIED | `releaseReservation.ts` calls `expiryTemplate` + `sendEmail` with re-reserve URL after transaction commits |

### Anti-Patterns Found

No blockers or warnings found. Scan across all Phase 6 modified files (functions, Android, web) returned:
- Zero `[STUB]` strings in production code paths
- Zero `@sendgrid/mail` or `nodemailer` SDK imports
- Zero `return null` / empty-array stubs in rendering paths
- No `font-semibold` in web `ConfirmPurchaseBanner.tsx` (UI-SPEC typography contract honoured)

### Human Verification Required

#### 1. FCM Push on Real Android Device

**Test:** Install the app on a physical Android device. As owner (logged in), open RegistryDetailScreen. From a second session as giver, call the `confirmPurchase` Cloud Function with a valid active `reservationId` belonging to this registry. Repeat with app backgrounded (device screen off / app not in foreground).

**Expected:** Foreground: Material3 Snackbar appears at top of screen ("Your gift was purchased!" or Romanian variant). Background: push notification appears in device system tray with the correct title and registry name as body.

**Why human:** `GiftRegistryMessagingService.onMessageReceived` and the Android system notification tray require a running device with a real FCM token. Unit tests mock `sendEachForMulticast` and `NotificationBus` but cannot verify the full delivery chain.

#### 2. Email Rendering in Gmail and Outlook

**Test:** With the Firebase Emulator Suite running, trigger an expiry, invite, and purchase email by advancing the emulator clock or calling the relevant functions directly. Inspect the rendered HTML in Gmail and Outlook (desktop + mobile).

**Expected:** 600px centred shell with #6750A4 header band, "Gift Registry" wordmark, white content card, bulletproof CTA button (purple #6750A4 background), and bilingual footer. No layout breaks in Outlook 2016 (table-based layout should handle it). CTA links point to correct `giftregistry.app` URLs.

**Why human:** Email client quirks (Outlook conditional comments, Gmail CSS stripping) cannot be verified from HTML source alone. The Trigger Email extension must be connected to SendGrid and a real send must be observed.

#### 3. Re-reserve Deep Link from Email on Device

**Test:** Receive the expiry email on a device that has the app installed. Tap the "Re-reserve this gift" CTA button (URL: `https://giftregistry.app/reservation/{reservationId}/re-reserve`).

**Expected:** App opens and navigates directly to the re-reserve flow for the correct reservation. If app is not installed, web fallback at that URL renders the re-reserve UI.

**Why human:** Android App Links require a deployed `assetlinks.json` and a signed app. Deep link routing from email to app cannot be simulated in unit tests.

#### 4. Owner Opt-Out End-to-End

**Test:** As registry owner, navigate to SettingsScreen and disable purchase notifications (toggle off). From a second session as giver, complete a purchase against an item in that registry.

**Expected:** No FCM push received on owner device. No document written to the `mail` collection. Firestore `notifications_failures` collection has no new entries for this event.

**Why human:** The opt-out guard is covered by unit tests but the full chain (UI toggle -> Firestore `notifyOnPurchase` write -> trigger read -> skip) requires a live emulator run with observable Firestore state.

#### 5. ConfirmPurchaseBanner Render and Dismiss on Android

**Test:** As giver with an active reservation, open RegistryDetailScreen. Verify the sticky banner appears. Tap "Confirm purchase". Optionally force-expire the reservation and try again.

**Expected:** Banner visible with correct copy per UI-SPEC Contract 1. Tap success: snackbar "Purchase confirmed. Thank you!" and banner unmounts. Tap on expired reservation: snackbar "Could not confirm. Please try again." and banner remains visible.

**Why human:** Compose UI layout, snackbar timing, and banner mount/unmount behaviour require a running emulator or device.

### Gaps Summary

No gaps. All 10 observable truths are verified, all artifacts exist with substantive implementations, all key links are wired, data flows from real Firestore reads, and all automated test suites are green (54 functions tests, 107 web tests). The phase status is `human_needed` — the automated verification is complete, but FCM delivery to real devices, email rendering in mail clients, and deep link routing from email all require physical hardware or a live emulator with the Trigger Email extension connected.

---

_Verified: 2026-04-19T22:32:00Z_
_Verifier: Claude (gsd-verifier)_
