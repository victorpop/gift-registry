# Phase 6: Notifications + Email Flows — Research

**Researched:** 2026-04-19
**Domain:** Firebase Cloud Messaging (Android), Firestore triggers (v2), Firebase Trigger Email extension, Cloud Tasks cancellation, email template authoring
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Purchase Detection Mechanism**
- D-01: Items transition to `status: "purchased"` via an explicit giver-facing CTA "I completed the purchase" shown on the giver's active reservation (persistent banner/card on registry screen, both Android and web)
- D-02: On purchase confirmation: reservation doc → `status: "purchased"`, the scheduled Cloud Task for that reservation is cancelled (via stored `cloudTaskName`), and the item doc → `status: "purchased"` — atomic in a single transaction
- D-03: If the reservation 30-min timer lapses before confirmation, existing `releaseReservation` flow runs unchanged: item → `available`, reservation → `released`, expiry email sent
- D-04: NOTF-01/02 fanout fires from a Firestore `onDocumentUpdated` trigger on `items/{id}` when `status` transitions from `reserved` to `purchased`; guarded by a `purchaseNotificationSentAt` field to ensure idempotency on retries

**Email Provider & Delivery**
- D-05: Use the **Firebase Trigger Email** extension (`firebase-ext-trigger-email`) — write to Firestore `mail` collection; extension fans out via configured SMTP
- D-06: SMTP transport: **SendGrid SMTP relay**; extension config references `SMTP_URI` secret — no SDK import in Cloud Functions code
- D-07: Templates: inline in `functions/src/email/templates/{invite,expiry,purchase}.ts` as typed TS functions `(vars, locale) => { subject, html, text }`; en + ro keys co-located per template
- D-08: Emulator/dev behavior: the Trigger Email extension is not installed in the emulator — functions write to the `mail` collection unchanged, but a dev-only listener logs the rendered `subject + html + text` to console for inspection. No real emails sent in dev

**FCM Push Notifications**
- D-09: FCM tokens stored in **`users/{uid}/fcmTokens/{tokenId}`** subcollection, doc shape: `{ token, platform: "android"|"web", createdAt, lastSeenAt }`; doc id = token itself (natural dedup)
- D-10: Multi-device: fanout sends to every token in the subcollection
- D-11: Token lifecycle: Android `FirebaseMessagingService` subclass registers on `onNewToken`; additionally an app-start post-auth hook ensures token is refreshed if the subcollection is missing the current device token; send path catches `UNREGISTERED` / `INVALID_ARGUMENT` errors and deletes stale tokens
- D-12: Payload shape: **`notification + data`** — `notification.title` / `notification.body` use the owner's locale; `data.registryId` / `data.itemId` / `data.type` drive deep link handling on tap

**Fanout, Opt-out & Localization**
- D-13: Opt-out is **server-side only** — notification trigger reads `registries/{id}.notifyOnPurchase` before sending push or email; if false, skip both and log `[NOTF] Skipped for owner {uid} — opted out`
- D-14: Owner `preferredLocale` field on `users/{uid}` (seeded from device locale at registration, overrideable via existing settings screen); notification trigger reads this to pick en/ro template. Default `en` if missing
- D-15: Failures (SMTP errors, FCM errors) are logged to `notifications_failures` collection for ops visibility — shape: `{ type, userId, itemId, error, timestamp }`. No automatic retry queue in v1

**Invite Email Wiring**
- D-16: The existing `inviteToRegistry` Cloud Function's stub log is replaced by writing to the `mail` collection via a new helper `functions/src/email/send.ts` that calls into the invite template
- D-17: Existing-user invite (REG-06): same email goes out, *plus* an in-app notification is delivered via FCM to any token on the invited user's account
- D-18: Non-user invite (REG-07): email-only; registry link in email opens existing web fallback flow unchanged

**Expiry Email Wiring**
- D-19: Existing `releaseReservation` Cloud Function's stub block is replaced by writing to the `mail` collection using the expiry template; the existing re-reserve deep link URL format is preserved unchanged

**Localization Keys**
- D-20: New strings added under `notifications_*` (Android `strings.xml` + web `i18n/*.json`) plus email template strings co-located with the TS templates; all follow the feature-namespaced key convention from Phase 1 (D-01)

**Testing Strategy**
- D-21: Cloud Functions unit tests for the notification trigger (opt-in, opt-out, idempotency guard), token-cleanup logic (stale token deletion), and each email template (en/ro rendering with fixture vars)
- D-22: Firestore security rules updated + tested to deny client writes to `mail`, `users/{uid}/fcmTokens/*` (except own), and `notifications_failures` (admin-only) — extends existing `tests/rules` setup from Phase 1
- D-23: Android instrumentation not required for FCM service itself (framework-tested); unit test covers the token registration repository

### Claude's Discretion

No explicit discretion areas specified in CONTEXT.md. All key implementation decisions are locked in D-01 through D-23.

### Deferred Ideas (OUT OF SCOPE)

- Web push notifications (FCM for web) — requires VAPID keys + service worker + reCAPTCHA Enterprise
- Owner-initiated "mark purchased" for items bought offline/without reservation
- Auto-infer purchase from reservation non-release (rejected in D-03)
- Automatic retry queue for failed emails/pushes
- Dashboard for "my active reservations" on giver side
- Bilingual emails (send both languages in one message)
- SMS notifications
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| NOTF-01 | Owner receives push notification when a gift is purchased (if opted in) | FCM Admin SDK sendEachForMulticast, onDocumentUpdated trigger, token subcollection, idempotency guard via purchaseNotificationSentAt |
| NOTF-02 | Owner receives email when a gift is purchased (if opted in) | Firebase Trigger Email extension `mail` collection write, purchase template, opt-in gate via notifyOnPurchase field |
| NOTF-03 | Giver receives expiration email when reservation timer expires | Replace releaseReservation stub with mail collection write using expiry template; this also completes RES-07/RES-08 |
</phase_requirements>

---

## Summary

Phase 6 wires together three email flows and one push notification channel by extending existing Cloud Functions stubs. The work divides cleanly into four tracks: (1) a new `confirmPurchase` callable that closes the reservation loop and becomes the trigger event for notifications, (2) a Firestore `onDocumentUpdated` trigger that reads the owner's notification preferences and fans out to FCM + email, (3) replacement of two existing stubs (in `releaseReservation` and `inviteToRegistry`) with `mail` collection writes, and (4) Android FCM integration via a new `FirebaseMessagingService` subclass plus a `NotificationEventBus` for foreground delivery.

All email is sent via the Firebase Trigger Email extension — functions never import a mail SDK directly. FCM is sent via `firebase-admin` `messaging().sendEachForMulticast()`, which handles multi-device fanout and returns per-token success/failure results enabling stale-token cleanup. Cloud Tasks cancellation uses `CloudTasksClient.deleteTask()` on the `cloudTaskName` already stored in the reservation doc — this client is already imported in `createReservation.ts`.

The phase introduces two new Firebase dependencies on the Android side: `firebase-messaging` (for FCM token registration and foreground message handling) and no additional web dependencies beyond what Phase 5 already installed.

**Primary recommendation:** Build in the order: (1) `confirmPurchase` callable + transaction logic, (2) email template helpers + `sendEmail` abstraction, (3) stub replacements in `releaseReservation` and `inviteToRegistry`, (4) `onPurchaseNotification` Firestore trigger, (5) Android FCM service + token lifecycle, (6) Android UI (confirm-purchase banner, foreground snackbar, email-language settings row), (7) web UI (confirm-purchase banner), (8) security rules + all tests.

---

## Standard Stack

### Core (new additions for this phase)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `firebase-messaging` (Android) | via Firebase BoM 34.11.0 | FCM token registration + foreground message handling | Firebase-native; included in BoM; `FirebaseMessagingService` is the only supported way to intercept FCM on Android |
| `firebase-admin` messaging | 13.7.0 (already in functions/package.json) | Server-side FCM send via Admin SDK | `messaging().sendEachForMulticast()` is the current multi-device API; replaces deprecated `sendMulticast()` |
| `@google-cloud/tasks` | 6.2.1 (already in functions/package.json) | Task cancellation via `deleteTask()` | Already imported in `createReservation.ts`; `CloudTasksClient.deleteTask()` exists on v6 |
| Firebase Trigger Email extension | extension install (not npm) | Email delivery via `mail` Firestore collection | Decouples email from function code; handles SMTP retries; SMTP_URI is a secret param, not code |

### Supporting (existing libraries reused)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `firebase-functions/v2/firestore` | 7.2.3 | `onDocumentUpdated` trigger for item status changes | D-04: fires when items/{itemId} transitions to purchased |
| `firebase-functions/v2/https` | 7.2.3 | `onCall` for `confirmPurchase` callable | Same pattern as `createReservation` |
| `kotlinx-coroutines` + `callbackFlow` | 1.9.0 | Android FCM token refresh flow and event bus | Established pattern from Phase 2 (Auth state bridging) |
| `SharedFlow` / `StateFlow` | bundled | `NotificationEventBus` singleton for foreground FCM events | Same pattern as `ReservationDeepLinkBus` from Phase 4 |
| Material3 `Snackbar` + `Card` | via Compose BOM 2026.03.00 | Confirm-purchase banner and foreground notification display | Consistent with existing UI; no new components |
| Radix UI Toast | existing in web | Confirm-purchase success toast on web | Already present from Phase 5 |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Trigger Email extension | Nodemailer/SendGrid SDK in Functions | Extension handles SMTP retries and connection pooling; SDK would require managing SMTP connection state inside a stateless function |
| `sendEachForMulticast` | `send()` in a loop | `sendEachForMulticast` is a single HTTP call returning BatchResponse; loop makes N calls |
| Firestore `onDocumentUpdated` trigger | `onDocumentWritten` | `onDocumentUpdated` only fires on updates (not create/delete), which is the correct semantic for status transitions |

### Installation (Android — new dependency only)

```kotlin
// In app/build.gradle.kts — add to existing Firebase dependencies:
implementation("com.google.firebase:firebase-messaging")
// No version needed — covered by Firebase BoM 34.11.0
```

```toml
# libs.versions.toml — no new version entry needed (BoM manages it)
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
```

**No new npm packages are needed** — `firebase-admin` (13.7.0) and `@google-cloud/tasks` (6.2.1) are already in `functions/package.json`. The Trigger Email extension is installed via `firebase ext:install firebase/firestore-send-email`, not npm.

---

## Architecture Patterns

### Recommended Project Structure (new directories only)

```
functions/src/
├── email/
│   ├── send.ts              # sendEmail(template, vars, locale) — writes to mail collection
│   └── templates/
│       ├── invite.ts        # (vars, locale) => { subject, html, text }
│       ├── expiry.ts        # (vars, locale) => { subject, html, text }
│       └── purchase.ts      # (vars, locale) => { subject, html, text }
├── notifications/
│   └── onPurchaseNotification.ts  # onDocumentUpdated trigger + FCM fanout
└── reservation/
    └── confirmPurchase.ts   # new onCall callable

app/src/main/java/com/giftregistry/
├── data/
│   ├── fcm/
│   │   └── FcmTokenRepositoryImpl.kt
│   └── reservation/
│       └── ReservationRepositoryImpl.kt   # add confirmPurchase()
├── domain/
│   └── reservation/
│       └── ReservationRepository.kt      # add confirmPurchase() interface
├── service/
│   └── GiftRegistryMessagingService.kt   # FirebaseMessagingService subclass
└── util/
    └── NotificationEventBus.kt           # SharedFlow singleton for foreground FCM
```

### Pattern 1: Firestore onDocumentUpdated Trigger with Idempotency Guard

**What:** Trigger fires on any update to `items/{itemId}`. Guard checks that status changed `reserved → purchased` and `purchaseNotificationSentAt` is not already set. Then writes `purchaseNotificationSentAt` to the item doc before performing the fanout — this guarantees at-most-once delivery even if Cloud Functions retries the event.

**When to use:** Any trigger that performs side effects (emails, push) that must not repeat on retry.

```typescript
// Source: firebase-functions v2 type declarations (verified in node_modules)
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { Change, QueryDocumentSnapshot } from "firebase-functions/v2/firestore";

export const onPurchaseNotification = onDocumentUpdated(
  { document: "registries/{registryId}/items/{itemId}", region: REGION },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    // Guard: only fire on reserved → purchased transition
    if (before.status !== "reserved" || after.status !== "purchased") return;

    // Idempotency: bail if notification already sent
    if (after.purchaseNotificationSentAt) {
      console.info(`[NOTF] Already sent for item ${event.params.itemId}; skipping`);
      return;
    }

    const db = admin.firestore();
    const itemRef = event.data!.after.ref;

    // Claim the notification slot atomically
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(itemRef);
      if (snap.data()?.purchaseNotificationSentAt) return; // lost the race
      tx.update(itemRef, { purchaseNotificationSentAt: FieldValue.serverTimestamp() });
    });

    // Read registry for notifyOnPurchase and ownerId
    const registryRef = db.collection("registries").doc(event.params.registryId);
    const [registrySnap, ownerSnap] = await Promise.all([
      registryRef.get(),
      // owner uid lookup from registry
    ]);
    // ... opt-out check, locale read, FCM fanout, mail write
  }
);
```

**Confidence:** HIGH — verified against `firebase-functions/lib/v2/providers/firestore.d.ts` in the installed package.

### Pattern 2: FCM Multi-Device Fanout with Stale Token Cleanup

**What:** Read all tokens from `users/{uid}/fcmTokens` subcollection, call `sendEachForMulticast`, collect failures, and delete any token that returned `UNREGISTERED` or `INVALID_ARGUMENT`.

**When to use:** Any server-side push notification send.

```typescript
// Source: firebase-admin 13.7.0 — verified sendEachForMulticast exists in messaging-api.d.ts
import { getMessaging } from "firebase-admin/messaging";

const tokenDocs = await db.collection(`users/${ownerId}/fcmTokens`).get();
const tokens = tokenDocs.docs.map((d) => d.data().token as string).filter(Boolean);
if (tokens.length === 0) return;

const response = await getMessaging().sendEachForMulticast({
  tokens,
  notification: { title, body },
  data: { registryId, itemId, type: "purchase" },
  android: { priority: "normal" },
});

// Stale token cleanup
const staleTokens: string[] = [];
response.responses.forEach((r, idx) => {
  if (!r.success) {
    const code = r.error?.code;
    if (code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token") {
      staleTokens.push(tokens[idx]);
    }
  }
});
if (staleTokens.length > 0) {
  const batch = db.batch();
  staleTokens.forEach((t) =>
    batch.delete(db.doc(`users/${ownerId}/fcmTokens/${t}`))
  );
  await batch.commit();
}
```

**Confidence:** HIGH — `sendEachForMulticast` confirmed in `firebase-admin/lib/messaging/messaging-api.d.ts`.

### Pattern 3: mail Collection Write (Firebase Trigger Email)

**What:** Write a document to the `mail` Firestore collection. The Trigger Email extension picks it up and sends via the configured SMTP relay. The function never imports a mail SDK.

**When to use:** All email sends in this project (invite, expiry, purchase notification).

```typescript
// Source: Firebase Trigger Email extension docs + D-05 decision
// functions/src/email/send.ts
import * as admin from "firebase-admin";

export async function sendEmail(params: {
  to: string;
  subject: string;
  html: string;
  text: string;
}): Promise<void> {
  const db = admin.firestore();
  await db.collection("mail").add({
    to: params.to,
    message: {
      subject: params.subject,
      html: params.html,
      text: params.text,
    },
  });
}
```

**Dev behavior (D-08):** In emulator, the extension is not installed. A dev-only Firestore `onDocumentCreated` listener on `mail` (exported only when `process.env.FUNCTIONS_EMULATOR === "true"`) logs the rendered mail document to console without sending.

**Confidence:** HIGH — mail collection write is the extension's documented interface.

### Pattern 4: Android FirebaseMessagingService Subclass

**What:** Extends `FirebaseMessagingService` to handle token refresh (`onNewToken`) and foreground message display (`onMessageReceived`). Foreground messages are not auto-displayed by FCM on Android — the app must handle them.

**When to use:** All Android FCM integration.

```kotlin
// AndroidManifest.xml additions (inside <application>):
// <service android:name=".service.GiftRegistryMessagingService"
//          android:exported="false">
//   <intent-filter>
//     <action android:name="com.google.firebase.MESSAGING_EVENT" />
//   </intent-filter>
// </service>
// <meta-data android:name="com.google.firebase.messaging.default_notification_channel_id"
//            android:value="purchase_notifications" />

class GiftRegistryMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var notificationEventBus: NotificationEventBus

    override fun onNewToken(token: String) {
        // Post to coroutine scope — Service doesn't have a CoroutineScope by default
        ServiceScope.launch { fcmTokenRepository.registerToken(token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"]
        if (type == "purchase") {
            // App is in foreground — emit to bus; background auto-renders via system tray
            ServiceScope.launch {
                notificationEventBus.emit(
                    NotificationEvent.Purchase(
                        registryId = remoteMessage.data["registryId"] ?: "",
                        registryName = remoteMessage.notification?.body ?: ""
                    )
                )
            }
        }
    }
}
```

**Hilt injection into Service:** Use `@AndroidEntryPoint` on the Service class and declare it as an `@AndroidEntryPoint` — Hilt supports Services directly. However, `FirebaseMessagingService` is initialized by the Firebase SDK, not by the app component. Solution: use `EntryPointAccessors` or a Hilt `ServiceComponent` pattern, OR inject via `(application as GiftRegistryApp).appComponent` for simplicity. The established project pattern for `GiftRegistryApp` uses Hilt's standard `@HiltAndroidApp`, so `@AndroidEntryPoint` on the service is the correct approach.

**Confidence:** HIGH — FirebaseMessagingService integration is well-documented; Hilt @AndroidEntryPoint on Service is standard.

### Pattern 5: NotificationEventBus (Foreground FCM)

**What:** Singleton `SharedFlow` (no replay) for foreground FCM events. The active screen collects it in a `LaunchedEffect`. This is identical in structure to `ReservationDeepLinkBus` already in the project.

```kotlin
// util/NotificationEventBus.kt
@Singleton
class NotificationEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<NotificationEvent>(replay = 0)
    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    suspend fun emit(event: NotificationEvent) = _events.emit(event)
}

sealed interface NotificationEvent {
    data class Purchase(val registryId: String, val registryName: String) : NotificationEvent
}
```

**Confidence:** HIGH — mirrors `ReservationDeepLinkBus` which is established and working.

### Pattern 6: confirmPurchase Callable + Cloud Task Cancellation

**What:** `onCall` callable that runs a Firestore transaction to transition both the reservation and item to `purchased`, then cancels the Cloud Task for that reservation using `cloudTaskName`.

**Key insight on Cloud Tasks cancellation:** `CloudTasksClient.deleteTask({ name: cloudTaskName })` is the correct API. If the task has already fired (or doesn't exist), the call throws a NOT_FOUND error — this must be caught and treated as a no-op, not an error.

```typescript
// Verified: CloudTasksClient.deleteTask() exists on @google-cloud/tasks 6.2.1
try {
  await tasksClient.deleteTask({ name: cloudTaskName });
} catch (err: unknown) {
  const code = (err as { code?: number }).code;
  if (code === 5) { // NOT_FOUND (gRPC status 5) — task already fired or doesn't exist
    console.info(`[confirmPurchase] Cloud Task ${cloudTaskName} already gone; ignoring`);
  } else {
    throw err;
  }
}
```

**Confidence:** HIGH — `deleteTask` verified in `CloudTasksClient` type declarations; NOT_FOUND (gRPC 5) is the standard error for absent tasks.

### Pattern 7: Email Templates as Typed TypeScript Functions

**What:** Each template is a function `(vars: TemplateVars, locale: "en" | "ro") => { subject: string; html: string; text: string }`. Locale switch is a simple if/else inside the function. Both locales are co-located in one file.

```typescript
// functions/src/email/templates/expiry.ts
interface ExpiryVars {
  itemName: string;
  registryName: string;
  reReserveUrl: string;
}

export function expiryTemplate(vars: ExpiryVars, locale: "en" | "ro"): {
  subject: string; html: string; text: string
} {
  if (locale === "ro") {
    return {
      subject: `Rezervarea ta pentru "${vars.itemName}" a expirat`,
      html: renderExpiryHtml({ ...vars, locale: "ro" }),
      text: `Rezervarea ta pentru ${vars.itemName} a expirat. Re-rezervă: ${vars.reReserveUrl}`,
    };
  }
  return {
    subject: `Your reservation for "${vars.itemName}" has expired`,
    html: renderExpiryHtml({ ...vars, locale: "en" }),
    text: `Your reservation for ${vars.itemName} has expired. Re-reserve: ${vars.reReserveUrl}`,
  };
}
```

**Confidence:** HIGH — this is pure TypeScript, no library required.

### Anti-Patterns to Avoid

- **Importing Nodemailer or SendGrid SDK in Cloud Functions:** D-05 locks the Trigger Email extension as the mail path. Functions only write to the `mail` collection.
- **Sending FCM inside `db.runTransaction()`:** FCM send is a side effect that cannot be rolled back. Always send AFTER the transaction commits, just as Cloud Tasks is enqueued after-transaction in `createReservation.ts`.
- **Writing `purchaseNotificationSentAt` outside a transaction:** Without a transaction, two concurrent function retries can both read the field as absent and both send. The guard transaction is mandatory.
- **Using `sendMulticast()` instead of `sendEachForMulticast()`:** `sendMulticast` is deprecated in firebase-admin v12+. Use `sendEachForMulticast`.
- **Registering FCM token on every app start without dedup check:** The token subcollection uses the token value as the doc ID, providing natural dedup — a write to an existing doc is an update (updates `lastSeenAt`), not a duplicate.
- **Catching Cloud Tasks NOT_FOUND as a fatal error in `confirmPurchase`:** If the giver clicks confirm just as the 30-min timer fires, the task is already deleted by `releaseReservation`. The NOT_FOUND error must be swallowed.
- **Storing notification channel ID as a hardcoded string:** Use `getString(R.string.notifications_channel_purchase_name)` for the display name, but the **channel ID** (`purchase_notifications`) must be a compile-time string constant — notification channels are identified by ID, not by the localized name.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| SMTP delivery + retries | Custom SMTP client in Cloud Function | Firebase Trigger Email extension | Extension handles connection pooling, retry on transient SMTP errors, and keeps SMTP credentials out of function code |
| Multi-token FCM send | Loop calling `send()` per token | `sendEachForMulticast()` | Single API call; BatchResponse gives per-token results for stale-token cleanup |
| Email HTML structure | Custom HTML builder | Template TS functions with string interpolation | Phase scope is 3 simple templates; a template engine (Handlebars, Mustache) adds a dependency for zero benefit at this scale |
| Token dedup | Unique constraint logic | Use token value as Firestore doc ID | Firestore doc ID is naturally unique; writing the same token twice is idempotent |
| Foreground notification UI (Android) | Custom notification overlay | `SnackbarHostState` + `NotificationEventBus` | M3 Snackbar is the standard ephemeral message pattern; avoid reinventing toast-like overlays |

---

## Common Pitfalls

### Pitfall 1: FCM Notification Not Shown in Foreground
**What goes wrong:** When the app is in the foreground and a push arrives with both `notification` and `data` payloads, Android does NOT auto-display the notification. The `onMessageReceived()` callback fires instead. If the app does not explicitly handle it, the notification is silently dropped.
**Why it happens:** FCM only auto-renders notifications (via system tray) when the app is in background/killed. Foreground delivery is the app's responsibility.
**How to avoid:** `onMessageReceived` emits to `NotificationEventBus`; the active screen collects and shows a Snackbar (per D-12 / UI-SPEC component 3).
**Warning signs:** Push visible in system tray when app is backgrounded but nothing shown when foregrounded — confirms foreground path is missing.

### Pitfall 2: Trigger Email Extension Not Available in Emulator
**What goes wrong:** Writing to the `mail` collection in the emulator does nothing — the extension is not running. Email flows appear to work (no error) but nothing sends.
**Why it happens:** Firebase extensions only run in production or against a real Firebase project with extension installed. The local emulator has no extension host.
**How to avoid:** Per D-08, add a dev-only `onDocumentCreated("mail/{docId}")` function (exported only when `FUNCTIONS_EMULATOR === "true"`) that logs the rendered template to console. This makes email output visible in emulator without any real send.
**Warning signs:** `mail` collection accumulates documents in emulator UI but no console output — dev listener is missing.

### Pitfall 3: onDocumentUpdated Trigger Fires on purchaseNotificationSentAt Write
**What goes wrong:** Writing `purchaseNotificationSentAt` to the item doc triggers `onDocumentUpdated` again, potentially causing infinite recursion.
**Why it happens:** The trigger listens to all updates on `registries/{registryId}/items/{itemId}`. Writing the sentinel field is itself an update.
**How to avoid:** The guard checks `after.status !== "purchased"` AND `after.purchaseNotificationSentAt` before doing any work. The second invocation sees `purchaseNotificationSentAt` set and returns immediately. This is the established idempotency pattern (same as `releaseReservation` guard).
**Warning signs:** `notifications_failures` collection grows rapidly with duplicate entries for the same item.

### Pitfall 4: Cloud Task Already Deleted When confirmPurchase Fires
**What goes wrong:** The 30-minute timer fires concurrently with the giver's "confirm purchase" tap. `releaseReservation` runs first, deleting the task and transitioning status to `released`. Then `confirmPurchase` tries to transition `released → purchased` but the guard transaction finds `status !== "active"` and aborts. The giver sees an error.
**Why it happens:** The giver and the timer race. This is an expected edge case, not a bug.
**How to avoid:** `confirmPurchase` transaction must check `status === "active"` before transitioning. If not active (already expired), throw `HttpsError("failed-precondition", "RESERVATION_EXPIRED")`. The Android/web client maps this error code to a user-friendly message.
**Warning signs:** Occasional `confirmPurchase` errors in production for reservations near expiry.

### Pitfall 5: FCM Token Registration Without POST_NOTIFICATIONS Permission (Android 13+)
**What goes wrong:** On Android 13+ (API 33+), FCM notifications require the `POST_NOTIFICATIONS` runtime permission. Without it, tokens are registered but no notifications appear in the system tray when the app is backgrounded.
**Why it happens:** Android 13 introduced `Manifest.permission.POST_NOTIFICATIONS` as a runtime permission.
**How to avoid:** Request `POST_NOTIFICATIONS` permission at a contextually appropriate moment (e.g., after user opts into notifications). The `GiftRegistryMessagingService` still registers tokens regardless — permission only affects system-tray display. For this phase, since `notifyOnPurchase` toggle already exists in Phase 3, request permission when the user enables it.
**Warning signs:** Tokens registered successfully (visible in Firestore), FCM send API returns success, but no notifications appear on physical device running Android 13+.

### Pitfall 6: preferredLocale Field Missing for Existing Users
**What goes wrong:** The notification trigger reads `users/{uid}.preferredLocale` for the email/push locale, but existing users (created in Phase 2) don't have this field. The trigger defaults to `"en"` per D-14, which is correct behavior. However, the Settings screen's "Email language" row reads the same field — if null, it must display a sensible default.
**Why it happens:** Phase 2 pre-dates the `preferredLocale` field.
**How to avoid:** The Settings screen reads `preferredLocale` with a null-safe fallback to the device locale from `LanguagePreferencesDataStore`. When the user first opens the settings row, if `preferredLocale` is null, display the current app language as the default value. Writing through the picker writes `preferredLocale` to Firestore.

### Pitfall 7: items/{id} Path Mismatch in Trigger Document Pattern
**What goes wrong:** The trigger document pattern must match the actual Firestore path. Items are stored at `registries/{registryId}/items/{itemId}` — not at a top-level `items` collection.
**Why it happens:** Easy to misread the schema when writing the trigger path.
**How to avoid:** Use `document: "registries/{registryId}/items/{itemId}"` with `opts` form so `event.params.registryId` and `event.params.itemId` are both available in the handler.

---

## Code Examples

### confirmPurchase callable — transaction core

```typescript
// Source: pattern mirrors createReservation.ts in this codebase
// functions/src/reservation/confirmPurchase.ts (outline)
await db.runTransaction(async (tx) => {
  const reservationSnap = await tx.get(reservationRef);
  if (!reservationSnap.exists) throw new HttpsError("not-found", "RESERVATION_NOT_FOUND");
  const reservation = reservationSnap.data()!;

  if (reservation.status !== "active") {
    throw new HttpsError("failed-precondition", "RESERVATION_EXPIRED");
  }

  const itemRef = db
    .collection("registries").doc(reservation.registryId as string)
    .collection("items").doc(reservation.itemId as string);

  tx.update(reservationRef, { status: "purchased" });
  tx.update(itemRef, { status: "purchased" });
});

// Cancel Cloud Task AFTER transaction (never inside)
const cloudTaskName = reservationData.cloudTaskName as string;
if (cloudTaskName) {
  try {
    await tasksClient.deleteTask({ name: cloudTaskName });
  } catch (err: unknown) {
    if ((err as { code?: number }).code !== 5) throw err; // 5 = NOT_FOUND
  }
}
```

### releaseReservation stub replacement (D-19)

```typescript
// Replace the entire stubEmailData block in releaseReservation.ts with:
if (stubEmailData) {
  const { giverEmail, itemName, registryName, reservationId: rid } = stubEmailData;
  // Fetch giver locale if stored — default "en" for giver (not an owner)
  const reReserveUrl = `https://giftregistry.app/reservation/${rid}/re-reserve`;
  const { subject, html, text } = expiryTemplate(
    { itemName, registryName, reReserveUrl },
    "en" // givers don't have preferredLocale — Phase 6 uses "en" as default
  );
  await sendEmail({ to: giverEmail, subject, html, text });
}
// Note: stubEmailData needs to capture itemName and registryName too — read from item/registry docs inside transaction
```

### Android FcmTokenRepositoryImpl.registerToken

```kotlin
// data/fcm/FcmTokenRepositoryImpl.kt
// Pattern: mirrors existing AuthRepositoryImpl runCatching + Firebase suspend call
suspend fun registerToken(token: String): Result<Unit> = runCatching {
  val uid = firebaseAuth.currentUser?.uid ?: return@runCatching
  val tokenDocRef = firestore
    .collection("users").document(uid)
    .collection("fcmTokens").document(token)
  tokenDocRef.set(
    mapOf(
      "token" to token,
      "platform" to "android",
      "createdAt" to FieldValue.serverTimestamp(),
      "lastSeenAt" to FieldValue.serverTimestamp(),
    ),
    SetOptions.merge() // update lastSeenAt if token already exists
  ).await()
}
```

### Firestore Security Rules — new collections

```javascript
// Additions to firestore.rules:

// mail — write denied to all clients (Admin SDK only via extension)
match /mail/{docId} {
  allow read, write: if false;
}

// notifications_failures — admin-only (read/write: if false for all clients)
match /notifications_failures/{docId} {
  allow read, write: if false;
}

// FCM tokens — owner reads/writes own tokens only; no other user reads them
match /users/{userId}/fcmTokens/{tokenId} {
  allow read, write: if isSignedIn() && request.auth.uid == userId;
  allow delete: if isSignedIn() && request.auth.uid == userId;
}
```

### Web — confirmPurchase callable pattern

```typescript
// web/src/features/reservation/useConfirmPurchase.ts
// Pattern: mirrors useReservation hook from Phase 5
import { httpsCallable } from "firebase/functions"
import { functions } from "../../firebase"

const confirmPurchaseFn = httpsCallable<
  { reservationId: string },
  { success: boolean }
>(functions, "confirmPurchase")

export function useConfirmPurchase() {
  const [isPending, setIsPending] = useState(false)
  const confirm = async (reservationId: string) => {
    setIsPending(true)
    try {
      await confirmPurchaseFn({ reservationId })
    } finally {
      setIsPending(false)
    }
  }
  return { confirm, isPending }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `sendMulticast()` (firebase-admin) | `sendEachForMulticast()` | firebase-admin v12 | Old method deprecated; new returns BatchResponse with per-token results for cleanup |
| Cloud Functions 1st gen Firestore triggers | `onDocumentUpdated` from `firebase-functions/v2/firestore` | Firebase Functions v2 | 2nd gen has better cold-start, concurrency control, and regional config |
| Nodemailer directly in functions | Firebase Trigger Email extension | Firebase extension ecosystem | Extension handles SMTP pooling, secrets management, retry logic without function code changes |

**Deprecated/outdated in this context:**
- `admin.messaging().sendMulticast()`: deprecated in favour of `sendEachForMulticast()`.
- Firebase KTX modules (`firebase-messaging-ktx`): removed from BoM 34.0.0; use `firebase-messaging` main module.
- Cloud Functions 1st gen `functions.firestore.document().onUpdate()`: replaced by `onDocumentUpdated` from `firebase-functions/v2/firestore`.

---

## Open Questions

1. **Firebase Trigger Email extension installation state**
   - What we know: The extension must be installed in the Firebase project via `firebase ext:install firebase/firestore-send-email`. The SMTP_URI secret must reference SendGrid SMTP relay credentials.
   - What's unclear: Whether the extension is already installed in this Firebase project. If not, the plan must include an extension-install step as Wave 0 work (done once, not per-deployment).
   - Recommendation: Plan includes a Wave 0 task to verify/install the extension and configure `SMTP_URI`. This is a human step, not automated.

2. **giverLocale in expiry email**
   - What we know: D-14 specifies that the owner's `preferredLocale` is used for purchase notification emails. The expiry email goes to the giver, who may not have a `preferredLocale` field.
   - What's unclear: Should the expiry email use the giver's locale (if authenticated) or default to `"en"`?
   - Recommendation: Per D-19, the expiry email replaces the existing stub in `releaseReservation`. The reservation doc stores `giverEmail` but not a locale. Default to `"en"` in v1 — same pragmatic approach as D-14's "Default `en` if missing."

3. **`POST_NOTIFICATIONS` permission UX timing**
   - What we know: Android 13+ requires runtime permission for system-tray notifications. The `notifyOnPurchase` toggle is already in the Settings screen (Phase 3).
   - What's unclear: Should requesting `POST_NOTIFICATIONS` be wired to the toggle, or done at app start?
   - Recommendation: Request `POST_NOTIFICATIONS` when the user enables `notifyOnPurchase` if not already granted. This is the most contextually appropriate moment and avoids a cold permission request at app launch.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Firebase Trigger Email extension | D-05 email delivery | Unknown (must verify) | n/a — extension install | None — plan must include install step |
| firebase-messaging (Android) | FCM token registration | Not yet in build.gradle | via BoM 34.11.0 | None — add dependency |
| Cloud Tasks API (gcloud) | Task cancellation | ✓ | @google-cloud/tasks 6.2.1 | — |
| firebase-admin messaging | FCM server send | ✓ | 13.7.0 (in functions/package.json) | — |
| SendGrid SMTP relay | Email delivery | Unknown (external account) | n/a | None — human setup required |

**Missing dependencies with no fallback:**
- Firebase Trigger Email extension: must be installed in Firebase project before email flows work in production.
- SendGrid SMTP credentials: `SMTP_URI` secret must be configured in extension params. This is a one-time human setup.

**Missing dependencies with fallback:**
- `firebase-messaging` Android dependency: straightforward one-line addition to `build.gradle.kts`. No fallback needed — just needs to be added.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework (Android) | JUnit 4 + MockK + Turbine (existing) |
| Framework (Cloud Functions) | Jest + ts-jest (no existing function tests — Wave 0 gap) |
| Framework (Firestore rules) | @firebase/rules-unit-testing + Jest (existing in `tests/rules/`) |
| Config file (Android) | none — standard AGP test runner |
| Config file (Functions) | `functions/jest.config.js` — Wave 0 gap |
| Quick run (Android) | `./gradlew :app:testDebugUnitTest` |
| Full suite (Android) | `./gradlew :app:testDebugUnitTest` |
| Quick run (Functions) | `cd functions && npm test` |
| Full suite (rules) | `cd tests/rules && npm test` (requires Firestore emulator running) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| NOTF-01 | Purchase push sent to owner when opted in | unit (Cloud Functions) | `cd functions && npm test -- --testPathPattern=onPurchaseNotification` | ❌ Wave 0 |
| NOTF-01 | Push NOT sent when opted out | unit (Cloud Functions) | same | ❌ Wave 0 |
| NOTF-01 | Idempotency: second invocation is no-op | unit (Cloud Functions) | same | ❌ Wave 0 |
| NOTF-01 | Stale token deleted on UNREGISTERED | unit (Cloud Functions) | same | ❌ Wave 0 |
| NOTF-02 | Purchase email written to mail collection when opted in | unit (Cloud Functions) | same | ❌ Wave 0 |
| NOTF-02 | Email NOT written when opted out | unit (Cloud Functions) | same | ❌ Wave 0 |
| NOTF-03 | Expiry email written to mail collection on reservation release | unit (Cloud Functions) | `cd functions && npm test -- --testPathPattern=releaseReservation` | ❌ Wave 0 |
| NOTF-03 | Expiry template renders correct en/ro content | unit (Cloud Functions) | `cd functions && npm test -- --testPathPattern=expiryTemplate` | ❌ Wave 0 |
| REG-06 | Invite email written to mail collection for existing user | unit (Cloud Functions) | `cd functions && npm test -- --testPathPattern=inviteToRegistry` | ❌ Wave 0 |
| REG-07 | Invite email written for non-user | unit (Cloud Functions) | same | ❌ Wave 0 |
| D-22 | mail collection: client write denied | rules test | `cd tests/rules && npm test` | ❌ Wave 0 |
| D-22 | notifications_failures: client write denied | rules test | same | ❌ Wave 0 |
| D-22 | fcmTokens: owner can write own tokens | rules test | same | ❌ Wave 0 |
| D-22 | fcmTokens: other user cannot read/write | rules test | same | ❌ Wave 0 |
| D-23 | FcmTokenRepository.registerToken saves correct shape | unit (Android) | `./gradlew :app:testDebugUnitTest --tests "*.FcmTokenRepositoryTest"` | ❌ Wave 0 |
| D-23 | ConfirmPurchaseUseCase delegates to repository | unit (Android) | `./gradlew :app:testDebugUnitTest --tests "*.ConfirmPurchaseUseCaseTest"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest` (Android) or `cd functions && npm test` (Functions)
- **Per wave merge:** All three suites: Android unit, Functions unit, Firestore rules
- **Phase gate:** All three suites green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `functions/jest.config.js` — Jest configuration for Cloud Functions unit tests
- [ ] `functions/src/__tests__/onPurchaseNotification.test.ts` — covers NOTF-01, NOTF-02 (opt-in, opt-out, idempotency, stale token)
- [ ] `functions/src/__tests__/releaseReservation.test.ts` — update existing (no file exists) to cover NOTF-03 expiry email write
- [ ] `functions/src/__tests__/inviteToRegistry.test.ts` — covers REG-06, REG-07 email write
- [ ] `functions/src/__tests__/emailTemplates.test.ts` — covers en/ro rendering for all 3 templates
- [ ] `tests/rules/firestore.rules.test.ts` — extend existing file with mail, notifications_failures, fcmTokens rule tests (file exists, add new describe blocks)
- [ ] `app/src/test/java/com/giftregistry/data/fcm/FcmTokenRepositoryTest.kt` — covers D-23
- [ ] `app/src/test/java/com/giftregistry/domain/usecase/ConfirmPurchaseUseCaseTest.kt` — covers confirmPurchase domain contract

---

## Project Constraints (from CLAUDE.md)

| Constraint | Implication for Phase 6 |
|------------|------------------------|
| Kotlin 2.3.x, no `kotlin-android` plugin (AGP 9 built-in) | `GiftRegistryMessagingService` and `FcmTokenRepositoryImpl` must not introduce any incompatible annotations; KSP 2.3.6 is the annotation processor |
| Firebase BoM 34.11.0, no KTX modules | Add `firebase-messaging` (not `firebase-messaging-ktx`); Kotlin APIs are in the main module |
| No other persistence layer — Firebase only | FCM tokens, mail docs, and failure logs all go in Firestore; no local DB |
| Cloud Functions v2 on Node.js 22 | `onDocumentUpdated` import from `firebase-functions/v2/firestore`, not v1 |
| All UI strings externalized | New strings go in `res/values/strings.xml` + `res/values-ro/strings.xml`; web strings in `web/src/i18n/en.json` + `ro.json`; no hardcoded strings in Kotlin or TSX |
| Guest access without account creation | `confirmPurchase` callable must accept a `reservationId` from a guest session; no auth required (giver may be anonymous) |
| Localization: Romanian + English | All 3 email templates must produce both `en` and `ro` variants; FCM payload uses owner's `preferredLocale` |
| KSP over KAPT (established in Phase 2) | `@AndroidEntryPoint` on `GiftRegistryMessagingService` uses KSP-compiled Hilt component |

---

## Sources

### Primary (HIGH confidence)
- `functions/node_modules/firebase-functions/lib/v2/providers/firestore.d.ts` — verified `onDocumentUpdated` signature with `Change<QueryDocumentSnapshot>` and `event.params`
- `functions/node_modules/firebase-admin/lib/messaging/messaging-api.d.ts` — verified `sendEachForMulticast` exists in firebase-admin 13.7.0
- `functions/node_modules/@google-cloud/tasks/package.json` (v6.2.1) + `CloudTasksClient.deleteTask` — verified method exists
- `functions/package.json` — confirmed firebase-admin 13.7.0 and @google-cloud/tasks 6.2.1 already present
- `gradle/libs.versions.toml` — confirmed Firebase BoM 34.11.0, Hilt 2.59.2, KSP 2.3.6 (KSP, not KAPT)
- `app/build.gradle.kts` — confirmed `firebase-messaging` is NOT yet declared; must be added
- Existing codebase (`createReservation.ts`, `releaseReservation.ts`, `inviteToRegistry.ts`) — confirmed stub locations for D-16, D-19
- `tests/rules/package.json` — confirmed rules test infrastructure exists with @firebase/rules-unit-testing

### Secondary (MEDIUM confidence)
- Firebase Trigger Email extension documentation (extension interface: write to `mail` collection; extension handles SMTP) — consistent with D-05 decision
- Firebase Cloud Messaging Android documentation — `FirebaseMessagingService.onMessageReceived` for foreground; system tray auto-render for background

### Tertiary (LOW confidence)
- None — all critical claims are verified against the installed packages or existing codebase.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified against installed node_modules and build.gradle.kts
- Architecture: HIGH — all patterns mirror established codebase conventions (createReservation, ReservationDeepLinkBus, callbackFlow, runCatching)
- Pitfalls: HIGH — derived from FCM Android documented behavior and codebase-specific patterns
- Email templates: HIGH — pure TypeScript, no library dependency

**Research date:** 2026-04-19
**Valid until:** 2026-05-19 (stable Firebase ecosystem; firebase-functions v2 API is stable)
