---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 03
subsystem: android
tags: [kotlin, hilt, firebase-functions, notifications, invite-flow, tdd]

# Dependency graph
requires:
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 01
    provides: RED tests NotificationTypeFromWireTest + NotificationRepositoryImplAcceptDeclineTest that this plan flips GREEN once Plan 16-04 also ships
  - phase: 06-notifications-email-flows
    provides: NotificationType enum + NotificationRepository interface + NotificationRepositoryImpl scaffolding that this plan extends with 3 new enum values + 2 new suspend methods + FirebaseFunctions dependency
  - phase: 04-reservation-system
    provides: ReservationRepositoryImpl.confirmPurchase httpsCallable + runCatching pattern reused verbatim for accept/declineInvite
provides:
  - NotificationType enum extended with 3 new values (INVITE_ACCEPTED_SELF, INVITE_ACCEPTED, INVITE_DECLINED) + fromWire mappings for the 3 new server wire strings (D-25)
  - NotificationRepository interface extended with suspend acceptInvite(registryId) + declineInvite(registryId), both returning Result<Unit> (D-27)
  - NotificationRepositoryImpl now takes FirebaseFunctions via constructor injection (Hilt) and exposes httpsCallable wrappers for "acceptInvite" + "declineInvite" callables (D-27)
  - Notification data class signature preserved verbatim — the 4 new optional payload keys (pendingEntryKey, occasion, coverUrl, eventDateMs) flow transparently through the existing Map<String, String?> payload (D-26 — no breaking change)
affects:
  - 16-04 (invite response sheet + viewmodel) — can now inject NotificationRepository with acceptInvite/declineInvite into InviteResponseViewModel
  - 16-05 (inbox reskin + strings) — must replace the placeholder when-branches in NotificationsScreen.kt with proper icons + localized titles for the 3 new types
  - Any future caller of NotificationRepositoryImpl — must now provide FirebaseFunctions to the constructor (Hilt handles this automatically via AppModule.provideFirebaseFunctions)

# Tech tracking
tech-stack:
  added: []  # No new libraries — reuses FirebaseFunctions (already on classpath), kotlinx-coroutines tasks.await (already imported), Hilt @Inject (already used)
  patterns:
    - "Cross-plan exhaustive-when invariant: when adding enum values to a domain type consumed by UI when-blocks across plan boundaries, the enum-extending plan MUST add placeholder branches to all consumer when-blocks (Rule 3 blocking fix). The downstream UI/string plan then replaces placeholders with proper UI."
    - "Httpscallable wrapper: verbatim shape of ReservationRepositoryImpl.confirmPurchase (functions.getHttpsCallable(name).call(payload).await() wrapped in runCatching) is the standard for all client-side onCall wrappers — keeps error semantics uniform across the domain layer."

key-files:
  created: []  # All edits — no new files
  modified:
    - "app/src/main/java/com/giftregistry/domain/model/Notification.kt — enum extended with 3 new values + 3 new fromWire mappings; data class signature preserved"
    - "app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt — interface extended with 2 new suspend methods + KDoc explaining failure modes"
    - "app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt — constructor + FirebaseFunctions import + 2 new method implementations using verbatim ReservationRepositoryImpl.confirmPurchase pattern"
    - "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt — Rule 3 blocking fix: added placeholder when-branches for 3 new enum values across 3 exhaustive when blocks (toIcon, localizedTitle, localizedBody). Plan 16-05 replaces with proper UI."

key-decisions:
  - "Did NOT create a new Hilt module — verified AppModule.provideFirebaseFunctions (lines 34-44) already provides @Singleton FirebaseFunctions bound to europe-west3. Adding it as a @Inject constructor param on NotificationRepositoryImpl is enough; Hilt resolves it from the existing module automatically."
  - "Used UNKNOWN-style fallback for the 3 new types in NotificationsScreen.kt (Notifications icon + titleFallback/bodyFallback strings). This is intentionally minimal — Plan 16-05 (inbox reskin + strings) will replace these placeholders with proper Material icons and localized strings from values/strings.xml + values-ro/strings.xml. Keeping the change scoped to enum exhaustiveness ONLY avoids pre-empting Plan 16-04 (sheet + viewmodel) or 16-05 (reskin)."
  - "Preserved Notification data class signature verbatim (D-26). The 4 new payload keys (pendingEntryKey, occasion, coverUrl, eventDateMs) live INSIDE the existing Map<String, String?> payload — no new top-level constructor params, no breaking change for existing callers. The DTO-to-domain mapper already coerces all server payload values to String? via mapValues, so any new server-side payload key flows through without code change."

patterns-established:
  - "Wave 2 enum-extension cross-plan blocker: adding an enum value to a domain type used by a UI when-block in another plan's territory creates a compile-blocker that the enum-extending plan MUST fix (Rule 3). Standard remediation: add minimal placeholder branches and document that the consumer-plan's UI work will replace them. Future enum extensions should follow this pattern."
  - "Httpscallable wrapper SOP: copy ReservationRepositoryImpl.confirmPurchase verbatim — `getHttpsCallable(name).call(payload).await()` wrapped in `runCatching` returning `Result<Unit>` (or typed Result on need). Uniform error semantics make UI ViewModels simpler."

requirements-completed:
  - D-25
  - D-26
  - D-27

# Metrics
duration: 3min
completed: 2026-05-24
---

# Phase 16 Plan 03: Android Domain + Data Layer Summary

**Domain enum + repository interface + impl extension for invite accept/decline — 3 new NotificationType values, 2 new NotificationRepository suspend methods, FirebaseFunctions injected into NotificationRepositoryImpl via existing Hilt provider, plus a Rule 3 placeholder fix in NotificationsScreen.kt for cross-plan when-exhaustiveness.**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-05-24T17:30:11Z
- **Completed:** 2026-05-24T17:33:19Z
- **Tasks:** 1 (TDD-style — RED already shipped by Plan 16-01; this is the GREEN production code)
- **Files modified:** 4

## Accomplishments

- Extended `NotificationType` enum with `INVITE_ACCEPTED_SELF`, `INVITE_ACCEPTED`, `INVITE_DECLINED` values + matching `fromWire` mappings for the 3 new server wire strings (D-25). UNKNOWN remains the last sentinel; forward-compat preserved.
- Extended `NotificationRepository` interface with `suspend fun acceptInvite(registryId: String): Result<Unit>` and `suspend fun declineInvite(registryId: String): Result<Unit>` + KDoc enumerating all failure modes (D-27).
- Extended `NotificationRepositoryImpl` constructor with a second `FirebaseFunctions` param (Hilt-injected from existing `AppModule.provideFirebaseFunctions`) + implemented both methods using the verbatim httpsCallable shape from `ReservationRepositoryImpl.confirmPurchase`.
- Preserved `Notification` data class signature verbatim — the 4 new optional payload keys (pendingEntryKey, occasion, coverUrl, eventDateMs) flow transparently through the existing `Map<String, String?>` payload field. No breaking change for any existing caller (D-26).
- Rule 3 blocking fix: added placeholder when-branches for the 3 new enum values across the 3 exhaustive when blocks in `NotificationsScreen.kt` (`toIcon`, `localizedTitle`, `localizedBody`) so the production code still compiles. Plan 16-05 (inbox reskin + strings) will replace these placeholders with proper Material icons + localized strings.

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend NotificationType enum + NotificationRepository interface + NotificationRepositoryImpl** — `b6a7967` (feat)

**Plan metadata:** _(pending — final commit after STATE.md / ROADMAP.md updates)_

## Files Created/Modified

### Modified (4)

- `app/src/main/java/com/giftregistry/domain/model/Notification.kt` — `NotificationType` enum + `fromWire` extended (D-25). Data class signature preserved (D-26).
- `app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt` — interface extended with 2 new suspend methods (D-27).
- `app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt` — `FirebaseFunctions` import + constructor param + 2 new method implementations (D-27).
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` — Rule 3 blocking fix: placeholder branches for the 3 new enum values in 3 exhaustive when blocks. Plan 16-05 replaces.

## Decisions Made

- **Reused existing Hilt provider for FirebaseFunctions** — `AppModule.provideFirebaseFunctions` (lines 34-44) already provides `@Singleton FirebaseFunctions` bound to `europe-west3`. No new Hilt module created; constructor `@Inject` is sufficient.
- **UNKNOWN-style fallback for the 3 new types in NotificationsScreen.kt** — placeholder branches return the generic `Notifications` icon and the server-provided `titleFallback`/`bodyFallback` strings. Plan 16-05 owns the proper Material icons + localized strings; keeping this plan's change minimal avoids pre-empting that work.
- **D-26 payload shape preserved by design** — Notification's `payload: Map<String, String?>` already accepts arbitrary string keys. The DTO mapper coerces every server payload value to `String?` via `mapValues`, so new keys (pendingEntryKey, occasion, coverUrl, eventDateMs) arriving from the server flow through without a domain change. This is the canonical extension point for adding payload fields without bumping the data class signature.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Extended exhaustive when blocks in NotificationsScreen.kt**
- **Found during:** Task 1 verification (`./gradlew :app:compileDebugKotlin`)
- **Issue:** Adding 3 new enum values to `NotificationType` broke 3 pre-existing exhaustive when blocks in `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` (`toIcon` line 190, `localizedTitle` line 209, `localizedBody` line 243). Build failed with `'when' expression must be exhaustive. Add the 'INVITE_ACCEPTED_SELF', 'INVITE_ACCEPTED', 'INVITE_DECLINED' branches or an 'else' branch.`
- **Fix:** Added minimal placeholder branches for all 3 new types across all 3 when-blocks. `toIcon` returns `Icons.Filled.Notifications` (same as UNKNOWN); `localizedTitle` returns `titleFallback`; `localizedBody` returns `bodyFallback`. Each branch is commented to flag that Plan 16-05 (inbox reskin + strings) will replace with proper UI.
- **Files modified:** `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt`
- **Verification:** `./gradlew :app:compileDebugKotlin` passes (`BUILD SUCCESSFUL in 713ms`).
- **Committed in:** `b6a7967` (Task 1 commit)
- **Scope note:** This edit is in `app/src/main/java/com/giftregistry/ui/notifications/` which is outside the parallel_execution constraint of `app/src/main/java/com/giftregistry/domain/* and data/*`. However, it's the minimum necessary to keep the production code compiling after my enum extension; the alternative would have been to add `else -> ...` branches (worse — destroys exhaustiveness as a type-safety guarantee for future additions). Per Rule 3 scope-boundary clause, fixing direct consequences of my own changes is in scope. Plan 16-02 (parallel) does not touch this file (it's backend/functions territory) — no conflict.

---

**Total deviations:** 1 auto-fixed (1 blocking).
**Impact on plan:** The fix was the minimum necessary to keep production code compiling after my enum extension. No scope creep — the placeholder branches are explicitly marked for Plan 16-05 to replace with proper UI. No new files, no new dependencies.

## Issues Encountered

- **Test source set compilation blocked by Plan 16-04 RED tests** — `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" --tests "com.giftregistry.data.notifications.NotificationRepositoryImplAcceptDeclineTest"` still fails to BUILD because `app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt` and `NotificationCardBranchingTest.kt` reference symbols (`InviteResponseViewModel`, `shouldOpenInviteSheet`) that Plan 16-04 owns. Gradle compiles the entire test source set as a unit, so test runs against my target classes are blocked until Plan 16-04 also ships.
  - **This is the expected Wave 0 RED state per the SUMMARY of Plan 16-01** (line 191): "LocalizationParityTest will run as soon as the Android test source set compiles (after Plan 16-03/04 ship the missing production symbols)."
  - **Verified via filter:** Running `./gradlew :app:compileDebugUnitTestKotlin 2>&1 | grep "^e: " | grep -v -E "(InviteResponseViewModelTest|NotificationCardBranchingTest)"` returns ZERO errors. All remaining compile errors are confined to the 2 Plan 16-04 test files. My target tests (NotificationTypeFromWireTest, NotificationRepositoryImplAcceptDeclineTest) compile cleanly against the new production code. When Plan 16-04 ships, all 4 will go GREEN together.
  - **Not blocking phase progress.** Per the Wave 0 / Nyquist contract, Wave 2 and Wave 3 plans cooperatively flip the entire test source set GREEN. Phase verifier in Plan 16-06 (deploy + UAT) will run the full suite after all production code is in.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **Plan 16-04 (invite response sheet + viewmodel) ready to start.** It can:
  - Inject `NotificationRepository` into `InviteResponseViewModel` and call `repo.acceptInvite(registryId)` / `repo.declineInvite(registryId)`.
  - Create the `InviteResponseViewModel` state-machine that the Plan 16-01 `InviteResponseViewModelTest` is waiting for.
  - Create the `shouldOpenInviteSheet` branching predicate (function or extension on `Notification`) that `NotificationCardBranchingTest` is waiting for. With the 3 new enum values now defined in domain, the predicate can branch on `type == NotificationType.INVITE && payload["pendingEntryKey"] != null` (sheet path) vs. existing `INVITE` without key (legacy navigate path) vs. all other types (no-op for sheet).
  - Once Plan 16-04 ships, all 4 Wave 0 Android RED tests for these symbols (mine + 16-04's) flip GREEN simultaneously.

- **Plan 16-05 (inbox reskin + strings) — placeholder branches in NotificationsScreen.kt awaiting replacement.** 9 placeholder lines (3 in each of toIcon, localizedTitle, localizedBody) must be replaced with proper Material icons + `stringResource` lookups against the new strings.xml keys for INVITE_ACCEPTED_SELF / INVITE_ACCEPTED / INVITE_DECLINED.

- **No blockers** for Plan 16-04. Production-code work is the only remaining piece.

## Self-Check: PASSED

Files verified to exist:
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/domain/model/Notification.kt` (modified — 3 new enum values + 3 new fromWire mappings)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt` (modified — 2 new suspend methods)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt` (modified — FirebaseFunctions injected + 2 new method impls)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` (modified — Rule 3 placeholder branches)

Commit verified to exist:
- `b6a7967` — `feat(16-03): extend NotificationType + NotificationRepository for invite accept/decline`

Acceptance-criteria grep verification (production-code contracts):
- `Notification.kt` contains `INVITE_ACCEPTED_SELF,` — PASS
- `Notification.kt` contains `INVITE_ACCEPTED,` — PASS
- `Notification.kt` contains `INVITE_DECLINED,` — PASS
- `Notification.kt` contains `"invite_accepted_self" -> INVITE_ACCEPTED_SELF` — PASS
- `Notification.kt` contains `"invite_accepted" -> INVITE_ACCEPTED` — PASS
- `Notification.kt` contains `"invite_declined" -> INVITE_DECLINED` — PASS
- `Notification.kt` contains `UNKNOWN;` (sentinel last) — PASS
- `Notification.kt` contains `val payload: Map<String, String?>` (D-26 preserved) — PASS
- `NotificationRepository.kt` contains `suspend fun acceptInvite(registryId: String): Result<Unit>` — PASS
- `NotificationRepository.kt` contains `suspend fun declineInvite(registryId: String): Result<Unit>` — PASS
- `NotificationRepositoryImpl.kt` contains `import com.google.firebase.functions.FirebaseFunctions` — PASS
- `NotificationRepositoryImpl.kt` contains `private val functions: FirebaseFunctions` — PASS
- `NotificationRepositoryImpl.kt` contains `getHttpsCallable("acceptInvite")` — PASS
- `NotificationRepositoryImpl.kt` contains `getHttpsCallable("declineInvite")` — PASS
- `NotificationRepositoryImpl.kt` contains `mapOf("registryId" to registryId)` — PASS

Build verification:
- `./gradlew :app:compileDebugKotlin` — `BUILD SUCCESSFUL`
- `./gradlew :app:compileDebugUnitTestKotlin 2>&1 | grep "^e: " | grep -v -E "(InviteResponseViewModelTest|NotificationCardBranchingTest)"` — empty (all errors confined to Plan 16-04 test files; my target tests compile cleanly)

---
*Phase: 16-android-notifications-inbox-invite-accept-decline*
*Completed: 2026-05-24*
