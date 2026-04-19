---
phase: 06-notifications-email-flows
plan: "03"
subsystem: android-data-layer
tags: [kotlin, firebase, repository, hilt, fcm, firestore, tdd]
dependency_graph:
  requires:
    - 06-00 (firebase-messaging dep, Phase 6 string keys)
  provides:
    - ReservationRepository.confirmPurchase (callable wrapper)
    - FcmTokenRepository + FcmTokenRepositoryImpl (users/{uid}/fcmTokens/{token})
    - EmailLocaleRepository + EmailLocaleRepositoryImpl (users/{uid}.preferredLocale)
    - ConfirmPurchaseUseCase, RegisterFcmTokenUseCase, ObserveEmailLocaleUseCase, SetEmailLocaleUseCase
    - DataModule @Binds for FcmTokenRepository + EmailLocaleRepository
  affects:
    - Plan 06-04 (ViewModels can @Inject all four new use cases)
    - Plan 06-05 (MessagingService calls RegisterFcmTokenUseCase)
tech_stack:
  added: []
  patterns:
    - runCatching wrapping all Firebase await() calls in repository impls
    - callbackFlow + awaitClose for Firestore snapshot listener (observeEmailLocale)
    - Silent no-op for anonymous path (FCM token registration): currentUser?.uid ?: return@runCatching Unit
    - relaxed = true for final Firebase classes (HttpsCallableResult, DocumentSnapshot) in MockK tests
    - ALLOWED_LOCALES set pattern for locale validation at data layer
key_files:
  created:
    - app/src/main/java/com/giftregistry/domain/fcm/FcmTokenRepository.kt
    - app/src/main/java/com/giftregistry/data/fcm/FcmTokenRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt
    - app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ConfirmPurchaseUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/RegisterFcmTokenUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt
    - app/src/test/java/com/giftregistry/data/reservation/ReservationRepositoryConfirmPurchaseTest.kt
    - app/src/test/java/com/giftregistry/data/fcm/FcmTokenRepositoryImplTest.kt
    - app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt
    - app/src/test/java/com/giftregistry/domain/usecase/ConfirmPurchaseUseCaseTest.kt
  modified:
    - app/src/main/java/com/giftregistry/domain/reservation/ReservationRepository.kt (added confirmPurchase)
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt (added confirmPurchase impl)
    - app/src/main/java/com/giftregistry/di/DataModule.kt (added 2 @Binds + 4 imports)
    - app/src/test/java/com/giftregistry/domain/usecase/ReserveItemUseCaseTest.kt (updated FakeReservationRepository for new interface method)
decisions:
  - relaxed=true required for MockK mocking of final Firebase classes (HttpsCallableResult, DocumentSnapshot) on Java 25: Byte Buddy inline instrumentation does not support Java 25 bytecode; relaxed avoids the transform
  - FcmTokenRepositoryImpl uses return@runCatching Unit for early exits (no-auth, blank token) — keeps all paths within the runCatching scope rather than returning Result directly from nested lambdas
  - EmailLocaleRepositoryImpl.observeEmailLocale returns flowOf(null) (not callbackFlow) when user is not signed in — avoids registering a Firestore listener that would immediately fail
  - ALLOWED_LOCALES as private top-level val in EmailLocaleRepositoryImpl for sharing between observeEmailLocale, getEmailLocale, and setEmailLocale
metrics:
  duration: ~4min
  completed_date: "2026-04-19T19:05:15Z"
  tasks_completed: 3
  files_modified: 14
---

# Phase 6 Plan 03: Android Domain + Data Layer Primitives for Notifications

Three new repository contracts (confirmPurchase callable, FCM token lifecycle, email locale preference), four new use cases, and two Hilt bindings — all zero Firebase imports in domain, runCatching at data layer, 19 unit tests green.

## What Was Built

### Task 1: confirmPurchase Callable Wrapper

**ReservationRepository interface** extended with:
```kotlin
suspend fun confirmPurchase(reservationId: String): Result<Unit>
```

**ReservationRepositoryImpl** implementation:
```kotlin
override suspend fun confirmPurchase(reservationId: String): Result<Unit> = runCatching {
    functions.getHttpsCallable("confirmPurchase")
        .call(mapOf("reservationId" to reservationId))
        .await()
    Unit
}
```

**ConfirmPurchaseUseCase** — thin delegate in domain layer with `@Inject constructor`.

Tests: 3 repo tests (success, failure propagation, payload capture) + 2 use case tests (delegation, failure propagation) = 5 tests.

### Task 2: FCM Token Repository

**FcmTokenRepository interface** (domain):
- `suspend fun registerToken(token: String): Result<Unit>`
- `suspend fun deleteToken(token: String): Result<Unit>`

**FcmTokenRepositoryImpl** (data):
- Writes `users/{uid}/fcmTokens/{token}` doc shape: `{ token, platform: "android", createdAt: serverTimestamp(), lastSeenAt: serverTimestamp() }` with `SetOptions.merge()`
- Silent no-op (`return@runCatching Unit`) when `currentUser == null` or token is blank

**RegisterFcmTokenUseCase** delegates `registerToken`.

Tests: 6 tests — write path + map shape verification, no-auth no-op, blank token no-op, exception wrapping, delete no-op, delete success.

### Task 3: EmailLocale Repository + DataModule Bindings

**EmailLocaleRepository interface** (domain):
- `fun observeEmailLocale(): Flow<String?>`
- `suspend fun getEmailLocale(): String?`
- `suspend fun setEmailLocale(locale: String): Result<Unit>`

**EmailLocaleRepositoryImpl** (data):
- `observeEmailLocale()`: `flowOf(null)` when not signed in; `callbackFlow { addSnapshotListener }` when signed in, normalizes to `ALLOWED_LOCALES = setOf("en", "ro")` or null
- `getEmailLocale()`: one-shot `get().await()` with same normalization
- `setEmailLocale("fr")`: `require(locale in ALLOWED_LOCALES)` → `IllegalArgumentException` wrapped by `runCatching`; not-signed-in → `error()` → `IllegalStateException` wrapped

**ObserveEmailLocaleUseCase** and **SetEmailLocaleUseCase** delegates.

**DataModule** additions:
```kotlin
@Binds @Singleton abstract fun bindFcmTokenRepository(impl: FcmTokenRepositoryImpl): FcmTokenRepository
@Binds @Singleton abstract fun bindEmailLocaleRepository(impl: EmailLocaleRepositoryImpl): EmailLocaleRepository
```

Tests: 8 tests — en read, ro read, null field, not-signed-in, unsupported locale, write success, invalid locale, not-signed-in write.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] MockK inline instrumentation fails on Java 25**
- **Found during:** Task 1 (test run)
- **Issue:** MockK's Byte Buddy version does not support Java 25 (class file version 69). `every { result.data }` on `HttpsCallableResult` failed with `MockKException: Missing mocked calls inside every { ... } block` because the inline transformer could not process the class
- **Fix:** Removed `every { result.data }` stubs (confirmPurchase impl ignores `.data` entirely — correct) and used `relaxed = true` for `HttpsCallableResult` and `DocumentSnapshot` mocks throughout. No behavior change; implementation was already correct
- **Files modified:** ReservationRepositoryConfirmPurchaseTest.kt, EmailLocaleRepositoryImplTest.kt

## Known Stubs

None — this plan implements repository/use case contracts only. No UI or ViewModel stubs.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | f0339a8 | feat(06-03): extend ReservationRepository with confirmPurchase + use case |
| 2 | 4fbb18d | feat(06-03): create FcmTokenRepository + RegisterFcmTokenUseCase |
| 3 | 09bdb18 | feat(06-03): create EmailLocaleRepository + use cases + DataModule bindings |

## Self-Check: PASSED
