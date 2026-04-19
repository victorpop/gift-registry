# Phase 6: Notifications + Email Flows - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Owners receive timely purchase notifications (push + email, only if opted in) and all three email flows — purchase notification, reservation expiry (with re-reserve link), and private-registry invite — are fully operational end-to-end. Covers NOTF-01, NOTF-02, NOTF-03 and completes the invite email that Phase 3 stubbed.

To make NOTF-01/02 firable, this phase also introduces a giver-facing **purchase confirmation** path: the item transitions to `purchased` via explicit user action, which is the event notifications hang off. Out of scope: owner-initiated "mark purchased"; auto-infer purchase from non-release; retailer return detection.

</domain>

<decisions>
## Implementation Decisions

### Purchase Detection Mechanism
- **D-01:** Items transition to `status: "purchased"` via an explicit giver-facing CTA "I completed the purchase" shown on the giver's active reservation (persistent banner/card on registry screen, both Android and web)
- **D-02:** On purchase confirmation: reservation doc → `status: "purchased"`, the scheduled Cloud Task for that reservation is cancelled (via stored `cloudTaskName`), and the item doc → `status: "purchased"` — atomic in a single transaction
- **D-03:** If the reservation 30-min timer lapses before confirmation, existing `releaseReservation` flow runs unchanged: item → `available`, reservation → `released`, expiry email sent
- **D-04:** NOTF-01/02 fanout fires from a Firestore `onDocumentUpdated` trigger on `items/{id}` when `status` transitions from `reserved` to `purchased`; guarded by a `purchaseNotificationSentAt` field to ensure idempotency on retries

### Email Provider & Delivery
- **D-05:** Use the **Firebase Trigger Email** extension (`firebase-ext-trigger-email`) — write to Firestore `mail` collection; extension fans out via configured SMTP
- **D-06:** SMTP transport: **SendGrid SMTP relay** (per CLAUDE.md preference for Romanian deliverability); the extension config references `SMTP_URI` secret — no SDK import in Cloud Functions code
- **D-07:** Templates: inline in `functions/src/email/templates/{invite,expiry,purchase}.ts` as typed TS functions `(vars, locale) => { subject, html, text }`; en + ro keys co-located per template
- **D-08:** Emulator/dev behavior: the Trigger Email extension is not installed in the emulator — functions write to the `mail` collection unchanged, but a dev-only listener logs the rendered `subject + html + text` to console for inspection. No real emails sent in dev

### FCM Push Notifications
- **D-09:** FCM tokens stored in **`users/{uid}/fcmTokens/{tokenId}`** subcollection, doc shape: `{ token, platform: "android"|"web", createdAt, lastSeenAt }`; doc id = token itself (natural dedup)
- **D-10:** Multi-device: fanout sends to every token in the subcollection
- **D-11:** Token lifecycle: Android `FirebaseMessagingService` subclass registers on `onNewToken`; additionally an app-start post-auth hook ensures token is refreshed if the subcollection is missing the current device token; send path catches `UNREGISTERED` / `INVALID_ARGUMENT` errors and deletes stale tokens
- **D-12:** Payload shape: **`notification + data`** — `notification.title` / `notification.body` use the owner's locale; `data.registryId` / `data.itemId` / `data.type` drive deep link handling on tap

### Fanout, Opt-out & Localization
- **D-13:** Opt-out is **server-side only** — notification trigger reads `registries/{id}.notifyOnPurchase` before sending push or email; if false, skip both and log `[NOTF] Skipped for owner {uid} — opted out`
- **D-14:** Owner `preferredLocale` field on `users/{uid}` (seeded from device locale at registration, overrideable via existing settings screen); notification trigger reads this to pick en/ro template. Default `en` if missing
- **D-15:** Failures (SMTP errors, FCM errors) are logged to `notifications_failures` collection for ops visibility — shape: `{ type, userId, itemId, error, timestamp }`. No automatic retry queue in v1 — Trigger Email extension and FCM have their own retry semantics

### Invite Email Wiring (completes Phase 3 stub)
- **D-16:** The existing `inviteToRegistry` Cloud Function's `[STUB] Invite email would be sent` log is replaced by writing to the `mail` collection via a new helper `functions/src/email/send.ts` that calls into the invite template
- **D-17:** Existing-user invite (REG-06): same email goes out, *plus* an in-app notification is delivered via FCM to any token on the invited user's account
- **D-18:** Non-user invite (REG-07): email-only; registry link in email opens existing web fallback flow unchanged

### Expiry Email Wiring (completes Phase 4 stub)
- **D-19:** Existing `releaseReservation` Cloud Function's stub (`stubEmailData` block) is replaced by writing to the `mail` collection using the expiry template; the existing re-reserve deep link URL format is preserved unchanged

### Localization Keys
- **D-20:** New strings added under `notifications_*` (Android `strings.xml` + web `i18n/*.json`) plus email template strings co-located with the TS templates; all follow the feature-namespaced key convention from Phase 1 (D-01)

### Testing Strategy
- **D-21:** Cloud Functions unit tests for the notification trigger (opt-in, opt-out, idempotency guard), token-cleanup logic (stale token deletion), and each email template (en/ro rendering with fixture vars)
- **D-22:** Firestore security rules updated + tested to deny client writes to `mail`, `users/{uid}/fcmTokens/*` (except own), and `notifications_failures` (admin-only) — extends existing `tests/rules` setup from Phase 1
- **D-23:** Android instrumentation not required for FCM service itself (framework-tested); unit test covers the token registration repository

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `functions/src/registry/inviteToRegistry.ts` — callable on `europe-west3`, stub email log at line 92 — direct replacement target
- `functions/src/reservation/releaseReservation.ts` — task-triggered handler, `stubEmailData` block lines 27-67 — direct replacement target
- `functions/src/reservation/createReservation.ts` — stores `cloudTaskName` pattern on reservation doc; purchase confirmation needs to cancel this task (same CloudTasksClient import already in package.json via `@google-cloud/tasks`)
- `app/src/main/.../LanguagePreferencesDataStore.kt` — existing locale persistence pattern; user's `preferredLocale` field should mirror this
- Existing `firebase.json` emulator config has Functions + Firestore — mail-collection dev listener can ride on same emulator bringup
- `tests/rules/` — security rules test harness exists; extend for mail/fcmTokens/notifications_failures

### Established Patterns
- Cloud Functions v2 (`firebase-functions/v2`) on `europe-west3`; onCall + HttpsError pattern; stub email logging convention
- Callbackflow + awaitClose for Firebase Android observers (auth, Firestore)
- Feature-namespaced string keys (`app_`, `auth_`, `registry_`, `reservation_`); Phase 6 adds `notifications_*`
- Guard-transaction pattern from Phase 4 `releaseReservation` — same idempotency shape applies to the notification trigger's `purchaseNotificationSentAt` guard
- runCatching wraps Firebase suspend calls in Android repositories — purchase confirmation repo follows same pattern
- Zero Firebase imports in domain layer (from Phase 2) — new `ConfirmPurchaseUseCase` interface lives in domain, impl in data

### Integration Points
- **Android:** New FCM `MessagingService` registered in `AndroidManifest.xml`; reuse existing Hilt app component; token repository injected into Auth session observer so tokens refresh on login
- **Web fallback:** Web push deferred (FCM web support exists but adds reCAPTCHA Enterprise + service worker — out of scope); web shows the same "confirm purchase" CTA for web-reserved items
- **Cloud Functions:** New directory `functions/src/email/` and `functions/src/notifications/`; `index.ts` exports the new trigger + confirmPurchase callable
- **Firestore schema additions:** `users/{uid}.preferredLocale`, `users/{uid}/fcmTokens/*`, `items/{id}.purchaseNotificationSentAt`, `notifications_failures` collection, `mail` collection (extension-managed)
- **Security rules:** extend `firestore.rules` with new collection paths; add to tests

</code_context>

<specifics>
## Specific Ideas

- Purchase confirmation CTA label: "I completed the purchase" (en) / "Am finalizat achiziția" (ro) — final copy per localization pass
- Push notification copy (en): "Your gift was purchased! 🎁" / "Someone bought an item from {registryName}" — placeholder; final copy during execution
- Re-reserve deep link format already defined in Phase 4 (D-12): `https://giftregistry.app/reservation/{reservationId}/re-reserve` — reuse unchanged
- SMTP_URI is a secret, not a config value — referenced from Trigger Email extension installation params
- Keep the stub-replace diffs small: `inviteToRegistry.ts` and `releaseReservation.ts` should each become a few-line delta that calls `sendEmail(template, vars, locale)`

</specifics>

<deferred>
## Deferred Ideas

- Web push notifications (FCM for web) — requires VAPID keys + service worker + reCAPTCHA Enterprise; defer to a later milestone
- Owner-initiated "mark purchased" for items bought offline/without reservation — v2 polish
- Auto-infer purchase from reservation non-release — rejected in D-03 (explicit confirmation only)
- Automatic retry queue for failed emails/pushes — relying on extension + FCM retry in v1 (D-15)
- Dashboard for "my active reservations" on giver side — still deferred from Phase 4
- Bilingual emails (send both languages in one message) — rejected in favour of locale picker (D-14)
- SMS notifications — out of scope per PROJECT.md (affiliate-only revenue; SMS cost model doesn't fit)

</deferred>
