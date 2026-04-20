---
phase: quick-260420-ozb
plan: 01
subsystem: notifications
tags: [notifications, inbox, firestore, cloud-functions, android-ui, i18n, firestore-rules]
dependency_graph:
  requires: [functions/notifications, functions/reservation, functions/registry, android/domain, android/data, android/navigation]
  provides: [persistent-in-app-notifications-inbox, unread-badge, mark-as-read-batched]
  affects: [RegistryListScreen (TopAppBar), AppNavigation, DataModule]
tech_stack:
  added: [writeNotification helper (Admin SDK), users/{uid}/notifications subcollection, NotificationRepository, InboxBellViewModel, NotificationsViewModel]
  patterns: [callbackFlow+awaitClose (mirrors FirestoreDataSource), flatMapLatest auth->subcollection, single-batch markRead, @HiltViewModel injection]
key_files:
  created:
    - functions/src/notifications/writeNotification.ts
    - app/src/main/java/com/giftregistry/domain/model/Notification.kt
    - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveNotificationsUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveUnreadNotificationCountUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/MarkNotificationsReadUseCase.kt
    - app/src/main/java/com/giftregistry/data/notifications/NotificationDto.kt
    - app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/ui/notifications/InboxBellViewModel.kt
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsInboxBell.kt
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt
  modified:
    - functions/src/registry/inviteToRegistry.ts
    - functions/src/reservation/createReservation.ts
    - functions/src/reservation/releaseReservation.ts
    - functions/src/notifications/onPurchaseNotification.ts
    - firestore.rules
    - app/src/main/java/com/giftregistry/di/DataModule.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
decisions:
  - "observeUnreadCount uses live snapshot listener on whereEqualTo('readAt', null) — .count() aggregation is one-shot and cannot drive a Flow"
  - "InboxBellViewModel is a separate @HiltViewModel from NotificationsViewModel — avoids heavyweight observation when only the badge count is needed on Home"
  - "releaseReservation emailData struct extended to carry registryId, itemId, giverId, ownerId — captured inside the transaction, passed out for post-commit notification writes"
  - "lint NewApi errors (removeLast) are pre-existing in AppNavigation.kt — verified by stashing and re-running lint before Task 3"
metrics:
  duration: "~8 minutes"
  completed: 2026-04-20
  tasks: 3
  files: 21
---

# Phase quick-260420-ozb Plan 01: Persistent In-App Notifications Inbox Summary

**One-liner:** Firestore-backed per-user notification inbox for 5 event types (invite, reservation created, purchased, expired, re-reserve) with Admin-SDK server writes, client callbackFlow observation, live unread badge, and batched mark-as-read.

## What Shipped

### Server (Task 1)

**`functions/src/notifications/writeNotification.ts`** — shared Admin-SDK helper that writes to `users/{userId}/notifications`. Best-effort: failures log to `notifications_failures` and never rethrow. Exports `writeNotification(params)` and `NotificationType` union.

**5 event sources wired (all after critical-path work):**

| Source | Type | Recipient |
|--------|------|-----------|
| `inviteToRegistry.ts` | `invite` | invitedUid (existing users only; non-user invites remain email-only) |
| `createReservation.ts` | `reservation_created` | registry ownerId (two extra Firestore reads post-transaction) |
| `onPurchaseNotification.ts` | `item_purchased` | registry ownerId (after email send) |
| `releaseReservation.ts` | `reservation_expired` | registry ownerId |
| `releaseReservation.ts` | `re_reserve_window` | giverId (only when giverId != null — guests have no account) |

**`firestore.rules`** — added `match /users/{userId}/notifications/{notificationId}`:
- `allow read`: self only
- `allow create`: `if false` (Admin SDK bypasses rules; no client creates)
- `allow update`: self only AND `request.resource.data.diff(resource.data).affectedKeys().hasOnly(["readAt"])` — prevents field tampering
- `allow delete`: `if false` (v1: no user deletion)

### Android Domain + Data (Task 2)

- `Notification` domain model with `NotificationType` enum (includes `UNKNOWN` for forward-compat)
- `NotificationRepository` interface: `observe`, `observeUnreadCount`, `markRead`
- 3 use cases wrapping the repository (pattern mirrors `ObserveRegistriesUseCase`)
- `NotificationDto` Firestore POJO with `@ServerTimestamp createdAt`
- `NotificationRepositoryImpl`:
  - `observe`: `callbackFlow + awaitClose` pattern (mirrors `FirestoreDataSource`)
  - `observeUnreadCount`: live listener on `whereEqualTo("readAt", null)` — snapshot.size() only, no doc bodies
  - `markRead`: single `batch.commit()` — one Firestore round-trip for all ids
- `DataModule`: `@Binds @Singleton NotificationRepository -> NotificationRepositoryImpl`
- Zero Firebase imports in domain layer (verified)

### Android UI (Task 3)

- **`InboxBellViewModel`**: flatMapLatest auth state → `observeUnreadCount`, emits 0 when unauthenticated
- **`NotificationsInboxBell`**: `BadgedBox + Icon + IconButton` — badge hidden when count == 0, "9+" when count > 9
- **`NotificationsViewModel`**: flatMapLatest auth → `observeNotifications`, `UiState` sealed interface (Loading/Unauthenticated/Empty/Loaded)
- **`NotificationsScreen`**:
  - `LazyColumn` of `Card` items with type-aware leading icons
  - Read items visually muted (`onSurfaceVariant` text color)
  - `LaunchedEffect(uiState)` + 500ms delay → `markVisibleRead(unreadIds)` — single batch per visible-set change
  - Empty state with localized `notifications_empty` string
  - Tap → `onNavigateToRegistry(payload.registryId)`
- **`AppNavKeys`**: added `@Serializable data object NotificationsKey`
- **`AppNavigation`**: `entry<NotificationsKey>` + HomeKey wired with `onNavigateToNotifications`
- **`RegistryListScreen`**: `onNavigateToNotifications` param + `NotificationsInboxBell` in `TopAppBar.actions`
- **13 string keys** added to both `values/strings.xml` (EN) and `values-ro/strings.xml` (RO)

## Known Follow-Ups

- **FCM parity**: FCM push payload currently uses hardcoded English strings. A follow-up could use the same i18n key approach (server sends key, client resolves) for push notifications.
- **Notification preferences / opt-out**: No per-type or global opt-out in v1. Future work.
- **Item-level deep links**: Tapping any notification navigates to registry detail. Item-level scrolling (`payload.itemId`) is ignored in v1 — no item anchor route exists yet.
- **Auto-delete TTL**: v1 has no TTL. A Cloud Scheduler + Firestore batch delete (90-day TTL) is a follow-up.
- **Web fallback parity**: Inbox is Android-only per CONTEXT. Web would need a separate React component.
- **`lintDebug` NewApi errors**: Pre-existing `removeLast()` call in `AppNavigation.kt` and other files causes lint to fail on `NewApi` (requires API 35). This is out of scope for this task — tracked as a pre-existing codebase issue.

## Manual QA Seed Instructions

To manually verify the inbox without triggering real events:

1. In Firebase Console → Firestore → `users/{your-uid}/notifications` → Add document:
   ```json
   {
     "type": "invite",
     "titleKey": "notification_invite_title",
     "bodyKey": "notification_invite_body",
     "title": "Test User invited you to \"Seed List\"",
     "body": "Tap to view Seed List",
     "payload": {
       "actorName": "Test User",
       "registryName": "Seed List",
       "registryId": "some-valid-registry-id",
       "actorUid": "some-uid"
     },
     "createdAt": "<server timestamp>",
     "readAt": null
   }
   ```
2. Open the app → Home → bell shows "1" badge.
3. Tap bell → NotificationsScreen opens → entry visible with mail icon.
4. After ~500ms → Firestore console refresh → `readAt` field populated → back to Home → badge disappears.
5. Switch device locale to Romanian → reopen screen → title/body switch to Romanian.

## Deviations from Plan

None — plan executed exactly as written. The lint NewApi pre-existing errors were confirmed pre-existing by reverting Task 3 changes and re-running lint (same count).

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `functions/src/notifications/writeNotification.ts` exists | FOUND |
| `domain/model/Notification.kt` exists | FOUND |
| `data/notifications/NotificationRepositoryImpl.kt` exists | FOUND |
| `ui/notifications/NotificationsScreen.kt` exists | FOUND |
| `ui/notifications/NotificationsInboxBell.kt` exists | FOUND |
| Commit 04d007d (Task 1) | FOUND |
| Commit 7ee937b (Task 2) | FOUND |
| Commit cbc8b9a (Task 3) | FOUND |
| `:app:compileDebugKotlin` | BUILD SUCCESSFUL |
