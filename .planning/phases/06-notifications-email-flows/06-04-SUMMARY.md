---
phase: 06-notifications-email-flows
plan: "04"
subsystem: android-ui-layer
tags: [kotlin, compose, fcm, notifications, hilt, material3, tdd]
dependency_graph:
  requires:
    - 06-00 (firebase-messaging dep, Phase 6 string keys)
    - 06-03 (ConfirmPurchaseUseCase, RegisterFcmTokenUseCase, ObserveEmailLocaleUseCase, SetEmailLocaleUseCase)
  provides:
    - NotificationBus (@Singleton SharedFlow<PurchasePush> replay=0)
    - MessagingHandler (plain class, unit-testable FCM logic)
    - GiftRegistryMessagingService (@AndroidEntryPoint, delegates to MessagingHandler)
    - ConfirmPurchaseBanner (Compose composable, UI-SPEC Contract 1)
    - RegistryDetailViewModel confirm-purchase flow (confirmingPurchase, onConfirmPurchase, snackbarMessages)
    - SettingsViewModel emailLocale StateFlow + onEmailLocaleChange
    - SettingsScreen Email Language row + dialog (UI-SPEC Contract 5)
    - POST_NOTIFICATIONS deferred permission in MainActivity
  affects:
    - RegistryDetailScreen (banner visible when hasActiveReservation, snackbars wired)
    - SettingsScreen (new Email language row)
    - AndroidManifest.xml (GiftRegistryMessagingService declared, default_notification_channel_id meta-data)
tech_stack:
  added: []
  patterns:
    - MessagingHandler pattern: plain class extracted from Service for unit-testability (replaces direct FirebaseMessagingService instantiation in tests)
    - SnackbarMessage sealed interface: Resource(resId) for confirm-purchase + Push(registryName, registryId) for FCM foreground
    - NotificationBus re-emitted via RegistryDetailViewModel.snackbarMessages to keep Compose UI unaware of singleton bus
    - hasActiveReservation derived from combine(items, observeGuestIdentity) — no new DataStore key needed
    - registerForActivityResult at Activity level (before setContent) for POST_NOTIFICATIONS launcher
key_files:
  created:
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationBus.kt
    - app/src/main/java/com/giftregistry/fcm/MessagingHandler.kt
    - app/src/main/java/com/giftregistry/fcm/GiftRegistryMessagingService.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/ConfirmPurchaseBanner.kt
    - app/src/test/java/com/giftregistry/fcm/GiftRegistryMessagingServiceTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelConfirmPurchaseTest.kt
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt
  modified:
    - app/src/main/AndroidManifest.xml (POST_NOTIFICATIONS + GiftRegistryMessagingService + default_notification_channel_id)
    - app/src/main/java/com/giftregistry/MainActivity.kt (POST_NOTIFICATIONS deferred request)
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt (SnackbarMessage, confirmingPurchase, onConfirmPurchase, NotificationBus, hasActiveReservation)
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt (ConfirmPurchaseBanner, snackbar collectors)
    - app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt (emailLocale, onEmailLocaleChange)
    - app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt (Email language ListItem + dialog)
    - app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt (updated for new VM constructor)
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt (updated for new VM constructor)
decisions:
  - MessagingHandler extracted as plain class (not Service) to enable unit testing without Android Service context — GiftRegistryMessagingService delegates to it
  - "@AndroidEntryPoint(FirebaseMessagingService::class) + extends Hilt_GiftRegistryMessagingService" pattern required by this project's KSP setup (same as MainActivity uses AppCompatActivity::class)
  - NotificationBus re-emitted via RegistryDetailViewModel rather than collecting directly in Compose — keeps UI layer clean of singleton bus
  - hasActiveReservation derived from items + guestIdentity combine rather than a new DataStore reservation ID — avoids new infrastructure for this plan
  - registerForActivityResult called at Activity level (before setContent) because Compose launcher must be registered before activity starts
  - SnackbarMessage.Resource uses resId: Int; context.getString() called at collection site in Screen (not in ViewModel)
metrics:
  duration: ~12min
  completed_date: "2026-04-19T19:27:00Z"
  tasks_completed: 3
  files_modified: 15
---

# Phase 6 Plan 04: Android UI Surface — Notifications, ConfirmPurchaseBanner, Email Language Picker

FCM integration end-to-end (token registration + foreground message routing), ConfirmPurchaseBanner composable matching UI-SPEC Contract 1, confirm-purchase success/error snackbars (Contract 2), foreground FCM push snackbar with "View" action (Contract 3), and Email Language row in Settings (Contract 5). 67 unit tests total, all green.

## What Was Built

### Task 1: NotificationBus + GiftRegistryMessagingService + Manifest + POST_NOTIFICATIONS

**NotificationBus** (`ui/notifications/NotificationBus.kt`):
- `@Singleton class NotificationBus`: `MutableSharedFlow<PurchasePush>(replay = 0)` — mirrors `ReservationDeepLinkBus` pattern
- `data class PurchasePush(registryId: String, registryName: String)` — FCM data payload shape
- `suspend fun emit(event: PurchasePush)` public API

**MessagingHandler** (`fcm/MessagingHandler.kt`):
- Plain `@Singleton` class injected with `RegisterFcmTokenUseCase` + `NotificationBus`
- `onNewToken(token, scope)` — fires in Service's CoroutineScope
- `onMessageReceived(remoteMessage, scope)` — guards `data["type"] != "purchase"` and empty registryId
- Internal `suspend` overloads for unit tests (no scope parameter)

**GiftRegistryMessagingService** (`fcm/GiftRegistryMessagingService.kt`):
- `@AndroidEntryPoint(FirebaseMessagingService::class)` extending `Hilt_GiftRegistryMessagingService`
- Injects `MessagingHandler`; delegates `onNewToken` and `onMessageReceived`; cancels `scope` in `onDestroy`

**AndroidManifest.xml** additions:
- `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
- `<service android:name=".fcm.GiftRegistryMessagingService" android:exported="false">` with `MESSAGING_EVENT` intent-filter
- `<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" android:value="purchase_notifications" />`

**MainActivity.kt**: `registerForActivityResult(RequestPermission)` called before `setContent`; `LaunchedEffect(Unit)` checks API level ≥ 33 and requests `POST_NOTIFICATIONS` if not granted.

**Tests (4):** onNewToken token registration, purchase message emits PurchasePush, non-purchase type ignored, missing registryId ignored.

### Task 2: ConfirmPurchaseBanner + RegistryDetailViewModel + RegistryDetailScreen snackbar wiring

**ConfirmPurchaseBanner** (`ui/registry/detail/ConfirmPurchaseBanner.kt`):
- Composable signature: `(isConfirming: Boolean, onConfirm: () -> Unit, modifier: Modifier)`
- `Card(containerColor = surfaceVariant, elevation = 2.dp)`, `defaultMinSize(minHeight = 64.dp)`
- `Icons.Default.ShoppingCart` (24dp), `tint = MaterialTheme.colorScheme.primary`
- `Text(labelLarge)` heading from `R.string.reservation_confirm_purchase_heading`
- `Button(heightIn(min = 48.dp))`: idle → cta string; loading → 20dp `CircularProgressIndicator` + loading string, `enabled = false`

**RegistryDetailViewModel** additions:
```kotlin
sealed interface SnackbarMessage {
    data class Resource(val resId: Int) : SnackbarMessage
    data class Push(val registryName: String, val registryId: String) : SnackbarMessage
}
```
- `snackbarMessages: SharedFlow<SnackbarMessage>` (replay=0, extraBufferCapacity=1)
- `confirmingPurchase: StateFlow<Boolean>` — true during use case call
- `hasActiveReservation: StateFlow<Boolean>` — derived from `combine(items, observeGuestIdentity)`
- `onConfirmPurchase(reservationId)` — sets confirming=true, calls use case, emits Resource message, resets to false
- `init {}` collects `NotificationBus.events` and re-emits as `SnackbarMessage.Push`

**RegistryDetailScreen** wiring:
- Collects `viewModel.snackbarMessages` in `LaunchedEffect(Unit)`; Resource → `showSnackbar(Short)`; Push → `showSnackbar(Long)` with `notifications_purchase_snackbar` + `notifications_purchase_snackbar_action` ("View"); `ActionPerformed` navigates to registry if different from current
- `ConfirmPurchaseBanner` rendered as `item(key = "confirm-purchase-banner")` when `hasActiveReservation`
- New `onNavigateToRegistry: (String) -> Unit = {}` parameter (backward compatible default)

**Tests (6):** success emits confirm_purchase_success resource, failure emits confirm_purchase_error, confirmingPurchase false after success, confirmingPurchase false after failure, NotificationBus push emits Push message, use case called with correct reservationId.

### Task 3: Email Language row in SettingsScreen + SettingsViewModel

**SettingsViewModel** additions:
- `observeEmailLocaleUseCase: ObserveEmailLocaleUseCase` + `setEmailLocaleUseCase: SetEmailLocaleUseCase` injected
- `val emailLocale: StateFlow<String?> = observeEmailLocaleUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, null)`
- `fun onEmailLocaleChange(locale: String)` — fire-and-forget coroutine launch

**SettingsScreen** additions:
- `ListItem` below app language row: `headlineContent = settings_email_language_label`, `supportingContent` = "English"/"Romanian" based on emailLocale (null defaults to English per D-14), `leadingContent = Icons.Default.Email (24dp)`, `Modifier.heightIn(min = 48.dp).clickable { emailLocaleDialogOpen = true }`
- `AlertDialog` with `RadioButton` list (en/ro) mirrors existing app-language dialog; confirm writes via `onEmailLocaleChange`; uses same `settings_language_confirm` + `common_cancel` string keys

**Tests (3):** emailLocale emits null then ro on flow update, onEmailLocaleChange calls use case with new value, swallows failure without throwing.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] @AndroidEntryPoint without base class arg rejected by KSP**
- **Found during:** Task 1
- **Issue:** Project uses `@AndroidEntryPoint(BaseClass::class)` + `Hilt_ClassName` pattern (established in Phase 2 for KSP support). Using `@AndroidEntryPoint` alone on a Service caused `[Hilt] Expected @AndroidEntryPoint to have a value` KSP error
- **Fix:** Used `@AndroidEntryPoint(FirebaseMessagingService::class)` and `class GiftRegistryMessagingService : Hilt_GiftRegistryMessagingService()`
- **Files modified:** GiftRegistryMessagingService.kt

**2. [Rule 3 - Blocking] FirebaseMessagingService subclass not unit-testable directly**
- **Found during:** Task 1 TDD RED — plan's NOTE itself flagged this possibility
- **Issue:** `FirebaseMessagingService` extends Android `Service` which requires framework context; direct instantiation in JVM unit tests would fail
- **Fix:** Extracted all FCM logic into `MessagingHandler` (plain `@Singleton` class); tests target `MessagingHandler` directly; `GiftRegistryMessagingService` delegates to it. Behavior identical; tests pass without Android framework
- **Files modified:** `MessagingHandler.kt` created, `GiftRegistryMessagingService.kt` updated, `GiftRegistryMessagingServiceTest.kt` updated to use handler

**3. [Rule 3 - Blocking] registerForActivityResult must be called before setContent**
- **Found during:** Task 1 — plan suggested `rememberLauncherForActivityResult` inside Compose, but that API requires Activity-level registration before start lifecycle
- **Fix:** Used `Activity.registerForActivityResult` in `onCreate` before `setContent`, captured launcher in a val, referenced it in `LaunchedEffect(Unit)` inside `setContent`
- **Files modified:** MainActivity.kt

**4. [Rule 2 - Missing] hasActiveReservation not in plan's VM additions**
- **Found during:** Task 2 — `ConfirmPurchaseBanner` visibility requires a condition; no `activeReservation` field existed in ViewModel or DataStore
- **Fix:** Added `hasActiveReservation: StateFlow<Boolean>` as `combine(items, observeGuestIdentity)` — guest exists AND any item RESERVED. No new infrastructure
- **Files modified:** RegistryDetailViewModel.kt

**5. [Rule 1 - Bug] Existing tests broke due to new constructor params**
- **Found during:** Tasks 2 and 3
- **Issue:** `ReserveItemViewModelTest` and `SettingsViewModelTest` called constructors without the new Phase 6 params
- **Fix:** Added `mockk(relaxed = true)` for new params in both existing test files
- **Files modified:** ReserveItemViewModelTest.kt, SettingsViewModelTest.kt

## Known Stubs

None — all banner/snackbar behavior is wired to real use cases from Plan 06-03. FCM foreground push delivery requires a real device (manual UAT item carried forward from VALIDATION.md).

## Manual UAT Items (carried forward from 06-VALIDATION.md)

- On-device FCM delivery: owner receives foreground push → "A gift was purchased from X!" snackbar appears with "View" action
- On-device FCM delivery: app backgrounded → OS system tray notification renders via default_notification_channel_id
- ConfirmPurchaseBanner: giver flow end-to-end (reservation active → banner visible → tap confirm → success snackbar → banner disappears)
- POST_NOTIFICATIONS: first launch on API 33+ device triggers system permission dialog

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| TDD RED T1 | ce3ab4b | test(06-04): add failing tests for MessagingHandler/NotificationBus |
| 1 | d78eb21 | feat(06-04): NotificationBus + MessagingHandler + GiftRegistryMessagingService + manifest + POST_NOTIFICATIONS |
| TDD RED T2 | 75343ad | test(06-04): add failing tests for RegistryDetailViewModel confirm purchase |
| 2 | 7d2e5d2 | feat(06-04): ConfirmPurchaseBanner + RegistryDetailViewModel + snackbar wiring |
| TDD RED T3 | 4c62767 | test(06-04): add failing tests for SettingsViewModel email locale |
| 3 | 544fe7b | feat(06-04): Email language row in SettingsScreen + SettingsViewModel emailLocale wiring |

## Self-Check: PASSED

All 8 created/modified files verified present. All 3 task commits verified in git log. 67 unit tests green, `assembleDebug` successful.
