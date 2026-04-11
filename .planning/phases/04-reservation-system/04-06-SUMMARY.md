---
phase: 04-reservation-system
plan: 06
subsystem: reservation
tags: [firebase-functions, kotlin, navigation3, hilt, sharedflow, deep-link, onCall]

requires:
  - phase: 04-reservation-system
    provides: createReservation callable, ReservationRepository.reserve, ReReserveDeepLink nav key, reservations hard-deny Firestore rule

provides:
  - resolveReservation Cloud Function (Admin SDK reads reservations/{id}, returns registryId/itemId/status)
  - ReservationLookup domain model
  - ReservationRepository.resolve() interface + impl via getHttpsCallable("resolveReservation")
  - ResolveReservationUseCase
  - ReservationDeepLinkBus @Singleton SharedFlow bridges nav layer to RegistryDetailViewModel
  - ReReserveResolverViewModel + ReReserveResolver composable in AppNavigation
  - RegistryDetailViewModel.init collects deepLinkBus and routes to onReserveClicked(itemId)

affects: [05-sharing-giver-access, 06-notifications-email, phase-04-verification]

tech-stack:
  added: []
  patterns:
    - "SharedFlow(replay=1) bus pattern for decoupled deep-link-to-screen auto-trigger"
    - "Resolver composable pattern: spinner composable in nav entry that resolves async then navigates"
    - "ReReserveResolverViewModel guards duplicate resolve() calls with Loading/Resolved state check"

key-files:
  created:
    - functions/src/reservation/resolveReservation.ts
    - app/src/main/java/com/giftregistry/domain/model/ReservationLookup.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ResolveReservationUseCase.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/ReservationDeepLinkBus.kt
  modified:
    - functions/src/index.ts
    - app/src/main/java/com/giftregistry/domain/reservation/ReservationRepository.kt
    - app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt

key-decisions:
  - "ReservationDeepLinkBus SharedFlow(replay=1) chosen over adding autoReserveItemId to RegistryDetailKey — avoids Navigation3 @Serializable default-value complications and keeps the nav key clean"
  - "resolveReservation Cloud Function has no auth guard — giver arrives from email link, may be unauthenticated (guest flow); function returns only opaque navigation IDs, no PII"
  - "deepLinkBus.request() called before setting State.Resolved in ViewModel — ensures replay buffer is populated before RegistryDetailViewModel subscribes via cold-start"
  - "RegistryDetailViewModel.init collect reuses onReserveClicked(itemId) — re-reserve goes through full createReservation transaction (D-11/Pitfall 3), no shortcut"

patterns-established:
  - "Resolver composable pattern: nav entry renders spinner + LaunchedEffect, resolves async, calls onResolved/onError callbacks, caller replaces backstack"
  - "SharedFlow bus for deep-link auto-trigger: singleton @Inject bus with replay=1 decouples producer (nav resolver) from consumer (destination ViewModel)"

requirements-completed: [RES-08]

duration: 4min
completed: 2026-04-11
---

# Phase 04 Plan 06: Re-reserve Deep Link Resolution Summary

**resolveReservation Cloud Function + ReservationDeepLinkBus completes RES-08: tapping re-reserve email link opens the correct registry item and auto-triggers the reserve flow**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-11T16:57:51Z
- **Completed:** 2026-04-11T17:02:27Z
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments
- New `resolveReservation` onCall Cloud Function reads `reservations/{id}` via Admin SDK (bypasses Firestore hard-deny) and returns `{registryId, itemId, status}` — no PII leaked
- New `ReservationDeepLinkBus` @Singleton SharedFlow(replay=1) bridges the nav resolver to `RegistryDetailViewModel` without modifying Navigation3 nav keys
- `AppNavigation.entry<ReReserveDeepLink>` TODO stub replaced with `ReReserveResolver` composable; on success navigates to correct registry + auto-triggers `onReserveClicked(itemId)` via the bus; on error falls back to HomeKey with graceful degradation
- `firestore.rules` reservations hard-deny preserved — client still cannot read reservations directly

## Task Commits

1. **Task 1: Add resolveReservation Cloud Function + export** - `6709a84` (feat)
2. **Task 2: Add domain + data layer support for resolve** - `d353139` (feat)
3. **Task 3: Wire re-reserve deep link through AppNavigation + RegistryDetailViewModel** - `57aded5` (feat)

## Files Created/Modified
- `functions/src/reservation/resolveReservation.ts` - onCall Cloud Function reading reservation doc via Admin SDK
- `functions/src/index.ts` - added `export { resolveReservation }`
- `app/.../domain/model/ReservationLookup.kt` - domain model `{registryId, itemId, status}`
- `app/.../domain/reservation/ReservationRepository.kt` - added `suspend fun resolve(reservationId)` to interface
- `app/.../data/reservation/ReservationRepositoryImpl.kt` - `resolve()` impl via `getHttpsCallable("resolveReservation")`
- `app/.../domain/usecase/ResolveReservationUseCase.kt` - thin use case, @Inject constructor
- `app/.../ui/registry/detail/ReservationDeepLinkBus.kt` - @Singleton SharedFlow bus for auto-reserve requests
- `app/.../ui/navigation/AppNavigation.kt` - ReReserveResolver composable + ReReserveResolverViewModel, replaced TODO entry
- `app/.../ui/registry/detail/RegistryDetailViewModel.kt` - init block collects deepLinkBus, calls onReserveClicked(req.itemId)

## Decisions Made
- `ReservationDeepLinkBus` SharedFlow(replay=1) chosen over adding `autoReserveItemId: String?` to `RegistryDetailKey` — avoids Navigation3 @Serializable default-value complications and keeps the nav key stable
- No auth guard on `resolveReservation` Cloud Function — giver arrives from email without account; function returns only opaque IDs (registryId, itemId), no PII
- `deepLinkBus.request()` fires before `_state.value = State.Resolved` in `ReReserveResolverViewModel.resolve()` — ensures replay buffer is populated before `RegistryDetailViewModel` subscribes on cold-start navigation
- Re-reserve reuses `onReserveClicked(itemId)` unchanged — goes through full `createReservation` transaction (D-11/Pitfall 3); item unavailability surfaces as `reservation_error_unavailable` snackbar

## Deviations from Plan

None — plan executed exactly as written. The plan itself included the revised approach (SharedFlow bus instead of nav key parameter) inline, so no runtime deviation was needed.

## Issues Encountered
None.

## User Setup Required
None — no external service configuration required. The new Cloud Function will be deployed alongside existing functions on next `firebase deploy`.

## Known Stubs
None — RES-08 is now fully satisfied. The `ReReserveDeepLink` entry no longer has a TODO stub. All paths (success, not-found, malformed, network error) are handled.

## Next Phase Readiness
- RES-08 gap 2 from 04-VERIFICATION.md is closed; Truth #8 is now verifiable end-to-end
- Phase 05 (sharing/giver access) can build on the established `resolveReservation` callable pattern
- Phase 06 (notifications/email) can use the `reservationId` in email deep links knowing the resolver flow is complete
- No blockers

---
*Phase: 04-reservation-system*
*Completed: 2026-04-11*
