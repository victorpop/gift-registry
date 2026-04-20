---
phase: quick-260420-ozb
plan: 01
type: execute
wave: 1
depends_on: []
autonomous: true
requirements: [NOTIF-INBOX-01, NOTIF-INBOX-02, NOTIF-INBOX-03, NOTIF-INBOX-04, NOTIF-INBOX-05]
files_modified:
  - functions/src/notifications/writeNotification.ts
  - functions/src/registry/inviteToRegistry.ts
  - functions/src/reservation/createReservation.ts
  - functions/src/reservation/releaseReservation.ts
  - functions/src/notifications/onPurchaseNotification.ts
  - firestore.rules
  - app/src/main/java/com/giftregistry/domain/model/Notification.kt
  - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt
  - app/src/main/java/com/giftregistry/domain/usecase/ObserveNotificationsUseCase.kt
  - app/src/main/java/com/giftregistry/domain/usecase/ObserveUnreadNotificationCountUseCase.kt
  - app/src/main/java/com/giftregistry/domain/usecase/MarkNotificationsReadUseCase.kt
  - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt
  - app/src/main/java/com/giftregistry/data/notifications/NotificationDto.kt
  - app/src/main/java/com/giftregistry/di/DataModule.kt
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsInboxBell.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml

must_haves:
  truths:
    - "A signed-in user who receives a registry invite sees a new entry in the notifications inbox"
    - "A registry owner sees inbox entries when someone reserves, purchases, or has a reservation expire on items in a registry they own"
    - "A giver whose reservation expires sees a re-reserve inbox entry on their account (when signed in) separate from the owner-side expiry entry"
    - "The home top bar shows an unread badge with the exact unread count (capped display 9+) that decrements as notifications are opened"
    - "Opening the notifications screen flips all currently-visible unread entries to readAt=serverTimestamp in a single batched write"
    - "Tapping an inbox entry navigates to its payload destination (registry detail for invite/reserve/purchase, registry detail for expiry)"
    - "All notification titles and bodies render in the user's current device locale (Romanian or English) from strings.xml keys — server never writes localized strings"
    - "Client cannot create notification documents — only Admin SDK writes succeed"
  artifacts:
    - path: "functions/src/notifications/writeNotification.ts"
      provides: "Shared Admin-SDK helper writeNotification(uid, type, payload, titleKey, bodyKey) used by all 5 event sources"
      exports: ["writeNotification", "NotificationType"]
    - path: "firestore.rules"
      provides: "match /users/{uid}/notifications/{id} — read/update (readAt only) for self, create denied"
      contains: "match /users/{userId}/notifications/{notificationId}"
    - path: "app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt"
      provides: "observeNotifications(uid), observeUnreadCount(uid), markRead(uid, ids)"
    - path: "app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt"
      provides: "Firestore users/{uid}/notifications subcollection observation + batched readAt update"
    - path: "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt"
      provides: "LazyColumn inbox screen with type-aware leading icons + empty state"
    - path: "app/src/main/java/com/giftregistry/ui/notifications/NotificationsInboxBell.kt"
      provides: "TopAppBar actions-slot composable: BadgedBox + bell IconButton"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Romanian strings for all 5 notification type titles/bodies with positional args"
      contains: "notification_invite_title"
  key_links:
    - from: "functions/src/registry/inviteToRegistry.ts"
      to: "writeNotification"
      via: "server-side call in existing-user branch (invitedUid != null)"
      pattern: "writeNotification\\(invitedUid"
    - from: "functions/src/reservation/createReservation.ts"
      to: "writeNotification"
      via: "post-transaction call writing to registry.ownerId"
      pattern: "writeNotification\\("
    - from: "functions/src/notifications/onPurchaseNotification.ts"
      to: "writeNotification"
      via: "called after sentinel claim for ownerUid"
      pattern: "writeNotification\\(ownerUid"
    - from: "functions/src/reservation/releaseReservation.ts"
      to: "writeNotification"
      via: "called after transaction commit — owner-side expiry + giver-side re-reserve (if giverId present)"
      pattern: "writeNotification\\("
    - from: "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt"
      to: "NotificationsInboxBell"
      via: "TopAppBar actions slot"
      pattern: "NotificationsInboxBell\\("
    - from: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      to: "NotificationsScreen"
      via: "entry<NotificationsKey> registration + bell onClick adds NotificationsKey to back stack"
      pattern: "entry<NotificationsKey>"
---

<objective>
Add a persistent per-user in-app notifications inbox for the 5 notifiable event types (invite received, reservation created on your registry, item purchased on your registry, reservation expired on your registry, re-reserve window opened for you as giver). Server writes go through a single shared Admin-SDK helper; the Android client observes `users/{uid}/notifications`, renders a top-bar bell with unread badge on Home, and shows a standalone `NotificationsScreen` that marks currently-visible items as read in a single batched write.

Purpose: Users need a reliable history of events on their registries and invites — the existing email + FCM paths are ephemeral and users currently have no in-app record after a push is dismissed.

Output:
- Firestore subcollection `users/{uid}/notifications` populated server-side by 5 existing Cloud Function call sites
- New security rules entry: self-read + self-update-readAt-only, no client creates
- Android inbox UI reachable via bell icon on Home, with live unread badge
- Romanian + English strings for all 5 event types
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/quick/260420-ozb-add-persistent-in-app-notifications-inbo/260420-ozb-CONTEXT.md
@CLAUDE.md
@functions/src/registry/inviteToRegistry.ts
@functions/src/reservation/createReservation.ts
@functions/src/reservation/releaseReservation.ts
@functions/src/reservation/confirmPurchase.ts
@functions/src/notifications/onPurchaseNotification.ts
@functions/src/notifications/invitePush.ts
@functions/src/index.ts
@firestore.rules
@app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
@app/src/main/java/com/giftregistry/di/DataModule.kt
@app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
@app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt
@app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt
@app/src/main/java/com/giftregistry/domain/usecase/ObserveRegistriesUseCase.kt

<interfaces>
<!-- Contracts executors need. Extracted from the codebase so no scavenger hunt. -->

From functions/src/notifications/onPurchaseNotification.ts — existing failure-log pattern:
```ts
// logFailure(db, { type, userId, itemId, registryId, error }) — writes to notifications_failures with FieldValue.serverTimestamp()
// Admin SDK bypasses rules; collection is hard-denied for clients.
```

From functions/src/registry/inviteToRegistry.ts — invited user resolution (already in code):
```ts
// After admin.auth().getUserByEmail(email):
//   invitedUid: string | null   // null => non-user invite, email-only, NO notification doc (no account to write to)
//   isExistingUser: boolean     // true => invitedUid set, existing-user branch writes notification
// registryName = registryData.title ?? "a registry"
// ownerName resolved via admin.auth().getUser(request.auth.uid)
```

From functions/src/reservation/createReservation.ts — available fields post-transaction:
```ts
// reservationId, registryId, itemId, giverName, giverId (nullable — null for guests)
// registry ownerId NOT yet loaded in this file — needs an admin.firestore().collection("registries").doc(registryId).get() before writing the owner notification
// itemName NOT loaded either — needs item doc read or pass affiliateUrl/title through from transaction
```

From functions/src/reservation/releaseReservation.ts — available fields post-transaction:
```ts
// emailData (currently only emitted when transaction flipped an expiry) contains:
//   giverEmail, reservationId, itemName, registryName
// Also available on `data` inside tx: giverId (null for guests), registryId, itemId, giverName, giverEmail
// Expiry writes TWO notifications:
//   - owner-side "reservation_expired" to registryData.ownerId
//   - giver-side "re_reserve_window" to giverId (ONLY if giverId != null — guests can't receive inbox entries)
```

From app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt — the callbackFlow + awaitClose pattern MUST be mirrored for NotificationRepositoryImpl:
```kotlin
fun observeX(...): Flow<List<T>> = callbackFlow {
    val listener = firestore.collection(...).addSnapshotListener { snapshot, error ->
        if (error != null) { close(error); return@addSnapshotListener }
        trySend(snapshot?.documents?.mapNotNull { ... } ?: emptyList())
    }
    awaitClose { listener.remove() }
}
```

From app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt — nav integration points:
```kotlin
// backStack: mutableStateListOf<Any>  — add via backStack.add(NotificationsKey)
// entryProvider { entry<NotificationsKey> { NotificationsScreen(...) } }
// HomeKey is the only place the bell should render — per CONTEXT D "entry point is the bell only"
```

From app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt — TopAppBar currently has no `actions` slot; add `actions = { NotificationsInboxBell(onClick = onNavigateToNotifications) }`.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Server — shared writeNotification helper, Firestore rules, and wire all 5 event sources</name>
  <files>
    - functions/src/notifications/writeNotification.ts (new)
    - functions/src/registry/inviteToRegistry.ts (modify)
    - functions/src/reservation/createReservation.ts (modify)
    - functions/src/reservation/releaseReservation.ts (modify)
    - functions/src/notifications/onPurchaseNotification.ts (modify)
    - firestore.rules (modify)
  </files>
  <action>
    **Part A — new shared helper `functions/src/notifications/writeNotification.ts`:**

    Export a `NotificationType` string literal union exactly:
    `"invite" | "reservation_created" | "item_purchased" | "reservation_expired" | "re_reserve_window"`

    Export `writeNotification(params)` where `params` is:
    ```ts
    {
      userId: string;           // required — the recipient (account UID)
      type: NotificationType;
      titleKey: string;         // i18n key (e.g. "notification_invite_title")
      bodyKey: string;          // i18n key (e.g. "notification_invite_body")
      titleFallback: string;    // pre-rendered English fallback for diagnostics
      bodyFallback: string;     // pre-rendered English fallback for diagnostics
      payload: Record<string, unknown>;  // type-specific: registryId, itemId, reservationId, registryName, itemName, actorName, actorUid
    }
    ```

    Implementation:
    - Writes to `users/{userId}/notifications` with `admin.firestore().collection(...).add(...)` (auto-id)
    - Fields written: `type, titleKey, bodyKey, title (=titleFallback), body (=bodyFallback), payload, createdAt: FieldValue.serverTimestamp(), readAt: null`
    - Wrap in try/catch. On error, log to `notifications_failures` with `{ type: "inbox_write", userId, notificationType: type, error, timestamp: FieldValue.serverTimestamp() }` using the same pattern as `onPurchaseNotification.ts` `logFailure`. Never rethrow — notification inbox is best-effort like email/push.

    **Part B — wire the 5 event sources. Each call happens AFTER the critical-path work (transaction commit, email send, etc.) so an inbox-write failure cannot break the main flow:**

    1. `functions/src/registry/inviteToRegistry.ts` — after the existing `sendInvitePush` call, in the `if (isExistingUser && invitedUid)` branch, call:
       ```ts
       await writeNotification({
         userId: invitedUid,
         type: "invite",
         titleKey: "notification_invite_title",
         bodyKey: "notification_invite_body",
         titleFallback: `${ownerName} invited you to "${registryName}"`,
         bodyFallback: `Tap to view ${registryName}`,
         payload: { registryId, registryName, actorName: ownerName, actorUid: request.auth.uid },
       });
       ```
       Non-user invites (invitedUid === null) get NO notification doc — there's no account to write to. Email-only remains the behavior per CONTEXT.

    2. `functions/src/reservation/createReservation.ts` — AFTER the Cloud Tasks block and the final `.update({ cloudTaskName })`. Read the registry owner + item title with two Firestore gets (outside the transaction — rules allow Admin SDK):
       ```ts
       const registrySnap = await db.collection("registries").doc(registryId).get();
       const itemSnap = await db.collection("registries").doc(registryId).collection("items").doc(itemId).get();
       const ownerId = registrySnap.data()?.ownerId as string | undefined;
       const registryName = (registrySnap.data()?.title as string) ?? "your registry";
       const itemName = (itemSnap.data()?.title as string) ?? "a gift";
       if (ownerId) {
         await writeNotification({
           userId: ownerId,
           type: "reservation_created",
           titleKey: "notification_reservation_created_title",
           bodyKey: "notification_reservation_created_body",
           titleFallback: `Someone reserved "${itemName}"`,
           bodyFallback: `${giverName} reserved "${itemName}" on "${registryName}"`,
           payload: { registryId, itemId, reservationId, registryName, itemName, actorName: giverName },
         });
       }
       ```
       Wrap in try/catch and log + swallow — reservation is already created, inbox write is best-effort.

    3. `functions/src/notifications/onPurchaseNotification.ts` — after the existing email send block, call:
       ```ts
       await writeNotification({
         userId: ownerUid,
         type: "item_purchased",
         titleKey: "notification_item_purchased_title",
         bodyKey: "notification_item_purchased_body",
         titleFallback: `"${itemName}" was purchased`,
         bodyFallback: `Someone bought "${itemName}" from "${registryName}"`,
         payload: { registryId, itemId, registryName, itemName, actorName: [giverFirstName, giverLastName].filter(Boolean).join(" ") || null },
       });
       ```

    4. `functions/src/reservation/releaseReservation.ts` — the existing transaction already captures `itemName` and `registryName`. Extend `emailData` (or add a parallel struct) to ALSO carry `registryId, itemId, giverId (nullable)`. AFTER the existing email send block:
       ```ts
       // Owner-side expiry notification — read owner from registry doc
       const registrySnap = await db.collection("registries").doc(registryId).get();
       const ownerId = registrySnap.data()?.ownerId as string | undefined;
       if (ownerId) {
         await writeNotification({
           userId: ownerId,
           type: "reservation_expired",
           titleKey: "notification_reservation_expired_title",
           bodyKey: "notification_reservation_expired_body",
           titleFallback: `Reservation on "${itemName}" expired`,
           bodyFallback: `The 30-minute reservation on "${itemName}" in "${registryName}" expired`,
           payload: { registryId, itemId, reservationId, registryName, itemName },
         });
       }
       // Giver-side re-reserve window — only if signed-in giver (guests have no account)
       if (giverId) {
         await writeNotification({
           userId: giverId,
           type: "re_reserve_window",
           titleKey: "notification_re_reserve_window_title",
           bodyKey: "notification_re_reserve_window_body",
           titleFallback: `"${itemName}" is available again`,
           bodyFallback: `Your reservation expired — "${itemName}" is available to re-reserve in "${registryName}"`,
           payload: { registryId, itemId, reservationId, registryName, itemName },
         });
       }
       ```
       Note: `giverId` is on the reservation doc; capture it inside the existing transaction read and pass out via the same hoisted struct pattern the file already uses for `emailData`.

    **Part C — `firestore.rules`. Add inside the existing `service cloud.firestore > match /databases/{database}/documents` block, near the existing `/users/{userId}/fcmTokens/{tokenId}` rule:**

    ```
    // Persistent in-app notifications inbox — per-user subcollection.
    // Admin SDK writes via writeNotification helper; clients can read their
    // own entries and flip readAt, nothing else.
    match /users/{userId}/notifications/{notificationId} {
      allow read: if isSignedIn() && request.auth.uid == userId;
      allow create: if false;
      // Client updates limited to readAt only — prevents tampering with type/payload.
      allow update: if isSignedIn()
                    && request.auth.uid == userId
                    && request.resource.data.diff(resource.data).affectedKeys()
                         .hasOnly(["readAt"]);
      allow delete: if false;  // v1: no user-initiated deletion
    }
    ```

    Rationale for the `hasOnly(["readAt"])` diff-based guard: prevents a client from rewriting `type`, `payload`, or forging a `createdAt` while still allowing mark-as-read.

    **Do NOT touch:** the FCM path, email templates, `invitePush.ts` logic, or push delivery. These remain as they are per CONTEXT (out of scope).
  </action>
  <verify>
    <automated>cd functions && npm run build && npm test -- --testPathPattern='onPurchaseNotification|releaseReservation|confirmPurchase' 2>&1 | tail -40</automated>

    Additional manual checks (executor runs):
    - `grep -n "writeNotification(" functions/src/**/*.ts` returns the 5 call sites (invite, createReservation, onPurchase, releaseReservation owner-side, releaseReservation giver-side)
    - `grep -n "users/{userId}/notifications" firestore.rules` matches the new block
    - Rules syntax check: `firebase deploy --only firestore:rules --dry-run` (or `firebase firestore:rules:get` if no emulator); emulator compile is the hard gate
  </verify>
  <done>
    - `writeNotification.ts` exports helper + `NotificationType`; failures log to `notifications_failures` and never rethrow
    - All 5 call sites invoke `writeNotification` after critical-path work, each wrapped in try/catch-and-log
    - Non-user invites do NOT write notifications; guest-giver expiries do NOT write the re-reserve entry
    - `firestore.rules` has a `users/{userId}/notifications/{notificationId}` block with self-read, self-update-readAt-only, create:false, delete:false
    - `functions` TypeScript build passes
    - Existing notification tests (onPurchase, releaseReservation, confirmPurchase) still pass
  </done>
</task>

<task type="auto">
  <name>Task 2: Android domain + data layer — Notification model, repository, use cases, DI binding</name>
  <files>
    - app/src/main/java/com/giftregistry/domain/model/Notification.kt (new)
    - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt (new)
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveNotificationsUseCase.kt (new)
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveUnreadNotificationCountUseCase.kt (new)
    - app/src/main/java/com/giftregistry/domain/usecase/MarkNotificationsReadUseCase.kt (new)
    - app/src/main/java/com/giftregistry/data/notifications/NotificationDto.kt (new)
    - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt (new)
    - app/src/main/java/com/giftregistry/di/DataModule.kt (modify — add @Binds)
  </files>
  <action>
    **`domain/model/Notification.kt`** — pure Kotlin model (zero Firebase imports, per Phase 02 convention):
    ```kotlin
    package com.giftregistry.domain.model

    enum class NotificationType {
        INVITE,
        RESERVATION_CREATED,
        ITEM_PURCHASED,
        RESERVATION_EXPIRED,
        RE_RESERVE_WINDOW,
        UNKNOWN;  // forward-compat: any future server type reads as UNKNOWN

        companion object {
            fun fromWire(raw: String?): NotificationType = when (raw) {
                "invite" -> INVITE
                "reservation_created" -> RESERVATION_CREATED
                "item_purchased" -> ITEM_PURCHASED
                "reservation_expired" -> RESERVATION_EXPIRED
                "re_reserve_window" -> RE_RESERVE_WINDOW
                else -> UNKNOWN
            }
        }
    }

    data class Notification(
        val id: String,
        val type: NotificationType,
        val titleKey: String,
        val bodyKey: String,
        val titleFallback: String,
        val bodyFallback: String,
        val payload: Map<String, String?>,   // flattened string-only view; all server values coerced to String? client-side
        val createdAtMs: Long,
        val readAtMs: Long?,
    )
    ```

    **`data/notifications/NotificationDto.kt`** — Firestore POJO:
    ```kotlin
    package com.giftregistry.data.notifications

    import com.google.firebase.firestore.ServerTimestamp
    import java.util.Date

    data class NotificationDto(
        val id: String = "",
        val type: String = "",
        val titleKey: String = "",
        val bodyKey: String = "",
        val title: String = "",
        val body: String = "",
        val payload: Map<String, Any?> = emptyMap(),
        @ServerTimestamp val createdAt: Date? = null,
        val readAt: Date? = null,
    )
    ```
    Use `doc.toObject(NotificationDto::class.java)?.copy(id = doc.id)` in the data source. For `payload`, flatten to `Map<String, String?>` in the DTO→domain mapper (stringify non-string values with `value?.toString()`).

    **`domain/notifications/NotificationRepository.kt`**:
    ```kotlin
    package com.giftregistry.domain.notifications

    import com.giftregistry.domain.model.Notification
    import kotlinx.coroutines.flow.Flow

    interface NotificationRepository {
        fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
        fun observeUnreadCount(uid: String): Flow<Int>
        suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>
    }
    ```

    **`data/notifications/NotificationRepositoryImpl.kt`** — mirrors `FirestoreDataSource.observeInvitedRegistries` pattern exactly (callbackFlow + addSnapshotListener + awaitClose + listener.remove):
    - `observe(uid, limit)` — listens on `users/{uid}/notifications` with `.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)`. Map each `NotificationDto` to `Notification` via a private extension.
    - `observeUnreadCount(uid)` — listens on `users/{uid}/notifications.whereEqualTo("readAt", null)`. In the snapshot callback emit `snapshot?.size() ?: 0` (ignore full docs — metadata-only is cheaper). Do NOT use `.count()` aggregation — it's a one-shot call, not a listener; we need a Flow for the live badge.
    - `markRead(uid, ids)` — `firestore.runBatch { batch -> ids.forEach { id -> batch.update(firestore.collection("users").document(uid).collection("notifications").document(id), "readAt", FieldValue.serverTimestamp()) } }.await()`. Wrap in `runCatching` to return `Result<Unit>` (matches `RegistryRepositoryImpl` convention).

    Inject `FirebaseFirestore` directly (not `FirestoreDataSource`) — this is a distinct subcollection and the existing data source is registry-scoped. This mirrors how `FcmTokenRepositoryImpl` is separate.

    **Use cases** — trivial wrappers, follow `ObserveRegistriesUseCase` pattern exactly:
    - `ObserveNotificationsUseCase(private val repo: NotificationRepository)` — `operator fun invoke(uid, limit=50)`
    - `ObserveUnreadNotificationCountUseCase(private val repo: NotificationRepository)` — `operator fun invoke(uid)`
    - `MarkNotificationsReadUseCase(private val repo: NotificationRepository)` — `suspend operator fun invoke(uid, ids)`

    **`di/DataModule.kt`** — add:
    ```kotlin
    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    ```
    Add import for both types at the top of the file.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin 2>&1 | tail -30</automated>

    Also verify:
    - `grep -n "NotificationRepository" app/src/main/java/com/giftregistry/di/DataModule.kt` matches the new @Binds
    - `grep -rn "class NotificationRepositoryImpl" app/src/main/java/com/giftregistry/` finds exactly one definition
  </verify>
  <done>
    - `:app:compileDebugKotlin` succeeds
    - DataModule binds `NotificationRepository` to `NotificationRepositoryImpl` with `@Singleton`
    - Repository uses `callbackFlow`/`awaitClose` pattern (no leaked listeners) — same shape as `FirestoreDataSource.observeInvitedRegistries`
    - Domain layer has zero Firebase imports (verified by grepping `domain/notifications/` and `domain/model/Notification.kt`)
    - `markRead` is a single batched write (not per-id), matching CONTEXT decision
  </done>
</task>

<task type="auto">
  <name>Task 3: Android UI — inbox bell on Home, NotificationsScreen, navigation wiring, Romanian + English strings</name>
  <files>
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsInboxBell.kt (new)
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt (new)
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt (new)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (modify — add NotificationsKey)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (modify — wire entry + HomeKey callback)
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt (modify — add TopAppBar actions slot with bell)
    - app/src/main/res/values/strings.xml (modify — add notification_* keys)
    - app/src/main/res/values-ro/strings.xml (modify — Romanian counterparts)
  </files>
  <action>
    **`ui/notifications/NotificationsInboxBell.kt`** — composable that renders in `TopAppBar.actions` on Home:
    - Inject its own `NotificationsViewModel` via `hiltViewModel()` (keyed so it's the same instance the screen uses — or simpler: use a separate `BellViewModel` that only observes `unreadCount`). For v1 simplicity: a dedicated small `@HiltViewModel InboxBellViewModel @Inject constructor(observeAuthStateUseCase, observeUnreadNotificationCountUseCase)` exposing `StateFlow<Int>` of unread count, flat-mapped on auth state (emit 0 when unauthenticated).
    - UI: `IconButton(onClick = onClick)` wrapping `BadgedBox(badge = { if (count > 0) Badge { Text(if (count > 9) "9+" else count.toString()) } }) { Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notifications_bell_cd)) }`.
    - Expose only: `@Composable fun NotificationsInboxBell(onClick: () -> Unit)`.

    **`ui/notifications/NotificationsViewModel.kt`**:
    - `@HiltViewModel` injected: `ObserveAuthStateUseCase`, `ObserveNotificationsUseCase`, `MarkNotificationsReadUseCase`.
    - Exposes `StateFlow<UiState>` where `UiState = Loading | Empty | Loaded(list: List<Notification>) | Unauthenticated`.
    - On init: `flatMapLatest` on auth state → `observeNotifications(uid)` → map to UiState. `stateIn(viewModelScope, WhileSubscribed(5000), Loading)`.
    - `fun markVisibleRead(ids: List<String>)` — filters out already-read ids client-side, calls `markNotificationsReadUseCase(uid, unreadIds)` in a `viewModelScope.launch`. Debounce not needed for v1 — ScreenEffect calls it once per visible-set change with a 500ms delay.

    **`ui/notifications/NotificationsScreen.kt`**:
    - `@OptIn(ExperimentalMaterial3Api::class) @Composable fun NotificationsScreen(onBack: () -> Unit, onNavigateToRegistry: (registryId: String) -> Unit)`.
    - `Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.notifications_screen_title)) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) })`.
    - `LazyColumn` of `Card` items.
    - Each row shows a leading icon chosen by `NotificationType`:
      | Type | Icon |
      |---|---|
      | INVITE | `Icons.Filled.MailOutline` |
      | RESERVATION_CREATED | `Icons.Filled.Bookmark` |
      | ITEM_PURCHASED | `Icons.Filled.CheckCircle` |
      | RESERVATION_EXPIRED | `Icons.Filled.Schedule` |
      | RE_RESERVE_WINDOW | `Icons.Filled.Refresh` |
      | UNKNOWN | `Icons.Filled.Notifications` |
    - Row renders localized title via `stringResource` with positional args drawn from `payload` (e.g. `stringResource(R.string.notification_invite_title, notification.payload["actorName"] ?: "Someone", notification.payload["registryName"] ?: "a registry")`). If `titleKey` is unknown (e.g. UNKNOWN type), fall back to `notification.titleFallback`.
    - Visual muting for read items: `MaterialTheme.colorScheme.onSurfaceVariant` for text color when `readAtMs != null`, full `onSurface` otherwise.
    - Empty state: centered `Text(stringResource(R.string.notifications_empty))`.
    - Tap behavior: `onClick = { notification.payload["registryId"]?.let { onNavigateToRegistry(it) } }` — per CONTEXT, all 5 types navigate to registry detail; item-level detail route doesn't exist, so payload.itemId is ignored in v1.
    - Batched mark-as-read: `LaunchedEffect(uiState)` collects unread ids currently in `Loaded` state, `delay(500)`, then calls `viewModel.markVisibleRead(unreadIds)`. The key is the list content — if it changes (e.g., new notification lands while screen is open), the effect restarts and marks the new entries too after the delay.

    **`ui/navigation/AppNavKeys.kt`** — append:
    ```kotlin
    @Serializable data object NotificationsKey
    ```

    **`ui/navigation/AppNavigation.kt`** — two edits:
    1. The `showsBottomNav()` predicate stays as-is (NotificationsKey shows the bottom nav — user may switch tabs while on it).
    2. Add to `RegistryListScreen(...)` call inside `entry<HomeKey>`: a new callback `onNavigateToNotifications = { backStack.add(NotificationsKey) }`.
    3. Register the entry:
       ```kotlin
       entry<NotificationsKey> {
           NotificationsScreen(
               onBack = { backStack.removeLast() },
               onNavigateToRegistry = { registryId ->
                   backStack.add(RegistryDetailKey(registryId))
               },
           )
       }
       ```

    **`ui/registry/list/RegistryListScreen.kt`** — two edits:
    1. Add parameter `onNavigateToNotifications: () -> Unit` to the `RegistryListScreen` function.
    2. Change the `TopAppBar(...)` to include an `actions` slot:
       ```kotlin
       TopAppBar(
           title = { Text(stringResource(R.string.registry_list_title)) },
           actions = {
               NotificationsInboxBell(onClick = onNavigateToNotifications)
           }
       )
       ```

    **`res/values/strings.xml`** — add inside the existing `<resources>`:
    ```xml
    <!-- Notifications inbox (quick-task 260420-ozb) -->
    <string name="notifications_bell_cd">Notifications</string>
    <string name="notifications_screen_title">Notifications</string>
    <string name="notifications_empty">No notifications yet.</string>
    <string name="notification_invite_title">%1$s invited you to \"%2$s\"</string>
    <string name="notification_invite_body">Tap to view the registry</string>
    <string name="notification_reservation_created_title">Someone reserved \"%1$s\"</string>
    <string name="notification_reservation_created_body">%1$s reserved \"%2$s\" on \"%3$s\"</string>
    <string name="notification_item_purchased_title">\"%1$s\" was purchased</string>
    <string name="notification_item_purchased_body">Someone bought \"%1$s\" from \"%2$s\"</string>
    <string name="notification_reservation_expired_title">Reservation on \"%1$s\" expired</string>
    <string name="notification_reservation_expired_body">The 30-minute reservation on \"%1$s\" in \"%2$s\" expired</string>
    <string name="notification_re_reserve_window_title">\"%1$s\" is available again</string>
    <string name="notification_re_reserve_window_body">Your reservation expired — \"%1$s\" can be re-reserved in \"%2$s\"</string>
    ```

    **`res/values-ro/strings.xml`** — add Romanian counterparts (same keys, translated bodies). Use Romanian diacritics where natural; the file already uses unicode escapes for some entries:
    ```xml
    <string name="notifications_bell_cd">Notificări</string>
    <string name="notifications_screen_title">Notificări</string>
    <string name="notifications_empty">Nicio notificare încă.</string>
    <string name="notification_invite_title">%1$s te-a invitat la \"%2$s\"</string>
    <string name="notification_invite_body">Apasă pentru a vedea lista</string>
    <string name="notification_reservation_created_title">Cineva a rezervat \"%1$s\"</string>
    <string name="notification_reservation_created_body">%1$s a rezervat \"%2$s\" pe \"%3$s\"</string>
    <string name="notification_item_purchased_title">\"%1$s\" a fost cumpărat</string>
    <string name="notification_item_purchased_body">Cineva a cumpărat \"%1$s\" din \"%2$s\"</string>
    <string name="notification_reservation_expired_title">Rezervarea pe \"%1$s\" a expirat</string>
    <string name="notification_reservation_expired_body">Rezervarea de 30 de minute pe \"%1$s\" din \"%2$s\" a expirat</string>
    <string name="notification_re_reserve_window_title">\"%1$s\" este disponibil din nou</string>
    <string name="notification_re_reserve_window_body">Rezervarea ta a expirat — \"%1$s\" poate fi re-rezervat în \"%2$s\"</string>
    ```

    CONSTRAINT: zero hardcoded UI strings in Kotlin files per CLAUDE.md. Every user-visible label above MUST come from `stringResource(...)`.

    **Icon imports reminder:** `import androidx.compose.material.icons.filled.Notifications`, `MailOutline`, `Bookmark`, `CheckCircle`, `Schedule`, `Refresh`. If any icon is not in the default extended set, swap to the closest match (the Phase 07 work demonstrated `ShoppingBag` usage) — the exact icon is not load-bearing.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:lintDebug 2>&1 | tail -40</automated>

    Additional manual checks:
    - Build and run emulator: bell renders on Home top bar, taps open NotificationsScreen
    - With the app pointed at a Firestore project that has seed notification docs in `users/{uid}/notifications`, the list renders and badge count matches. Quick seed: Firebase console → add a doc under `users/{your-uid}/notifications` with `{ type: "invite", titleKey: "notification_invite_title", payload: { actorName: "Test", registryName: "Seed list", registryId: "some-registry-id" }, createdAt: serverTimestamp, readAt: null }`.
    - Open the screen → within ~500ms the seed doc's `readAt` is populated (verified in Firebase console refresh).
    - Switch device locale to Romanian → titles and bodies render in Romanian.
  </verify>
  <done>
    - `:app:compileDebugKotlin` and `:app:lintDebug` pass
    - Bell icon visible on Home top bar only (not on Auth/Onboarding — Home is the only place `RegistryListScreen` renders)
    - Badge shows exact unread count capped at "9+"
    - Tapping bell opens `NotificationsScreen` via Navigation3 `entry<NotificationsKey>`
    - List renders all 5 type variants with distinct leading icons
    - Empty state shows localized "No notifications yet."
    - Tapping a row navigates to `RegistryDetailKey(payload.registryId)`
    - Opening the screen marks all currently-visible unread entries as read in a single batched write (verified by watching `readAt` flip in Firestore)
    - Every new UI string exists in both `values/strings.xml` and `values-ro/strings.xml` with identical keys
    - No hardcoded user-visible text in Kotlin files (grep `"[A-Z][a-z]+ [a-z]+"` in new .kt files — all strings should be `stringResource` calls or fallbacks read from domain model)
  </done>
</task>

</tasks>

<verification>

**End-to-end smoke (after all 3 tasks):**

1. **Invite flow:** Signed-in user A invites user B (already registered) → within a few seconds a new doc appears at `users/{B.uid}/notifications/{auto}` with `type: "invite"`, `readAt: null`. B's Home shows the bell with a "1" badge. B taps bell → NotificationsScreen opens → seed entry visible with mail icon → after ~500ms `readAt` in Firestore is set → back out to Home → badge disappears.

2. **Reservation flow:** User A (owner) creates a registry with one item. Guest/giver C reserves the item. Within seconds A has a new `reservation_created` notification with correct payload. A taps → lands on Registry Detail.

3. **Purchase flow:** C confirms purchase via the existing flow → A receives an `item_purchased` notification on top of the existing email.

4. **Expiry flow (owner-side):** C reserves but doesn't purchase → 30 min later `releaseReservation` fires → A receives a `reservation_expired` notification. If C was signed in (giverId populated), C ALSO receives a `re_reserve_window` notification. If C was a guest (giverId null), only A receives the notification.

5. **Security:** With Android app + emulator or prod project, attempt from a client:
   - `firestore.collection("users/{otherUid}/notifications").addSnapshotListener { }` → PERMISSION_DENIED
   - Client direct `add(...)` to own subcollection → PERMISSION_DENIED (create blocked)
   - Client `update(notification, {"type": "forged"})` → PERMISSION_DENIED (hasOnly guard)
   - Client `update(notification, {"readAt": serverTimestamp})` on own doc → succeeds

6. **Locale:** Switch device locale RO → reopen NotificationsScreen → all titles/bodies switch to Romanian. Server payload never contains localized strings — verified by reading one doc in Firebase console and seeing only i18n keys + payload map.

</verification>

<success_criteria>

- All 5 event sources write a notification doc via the shared `writeNotification` helper; a failure to write inbox entries never propagates to the critical-path operation (invite, reservation create, purchase, expiry)
- `firestore.rules` locks down creation (client:deny, admin-only) and scopes reads/updates to the owning user with `readAt`-only update diff
- Android client observes the inbox subcollection via `callbackFlow` with proper `awaitClose` cleanup, matching existing patterns
- Home top bar shows the bell with a live unread badge capped at `9+`
- Opening NotificationsScreen marks currently-visible unread items as read in a SINGLE batched write (verified by Firestore write log / snapshot latency)
- Every user-visible string has en + ro entries; the server never writes localized titles — only i18n keys + positional payload values
- Tapping a notification navigates to the registry detail for its `payload.registryId`
- `:functions` npm build passes and existing tests still pass
- `:app:compileDebugKotlin` + `:app:lintDebug` pass
- No hardcoded UI strings in any new Kotlin file

</success_criteria>

<output>
After completion, create `.planning/quick/260420-ozb-add-persistent-in-app-notifications-inbo/260420-ozb-SUMMARY.md` summarizing:
- What shipped (5 server writes + rules + inbox UI)
- Files touched and which patterns were followed (callbackFlow + Admin SDK writer)
- Known follow-ups (FCM parity opt, preferences/opt-out, item-level deep links, auto-delete TTL, web fallback)
- Seed instructions for manual QA (how to add a test notification to Firestore console for verification)
</output>
