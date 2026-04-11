---
phase: 04-reservation-system
plan: 04
subsystem: android-reservation-ui
tags: [reservation, compose, navigation3, ui, tdd]
dependency_graph:
  requires: [04-03]
  provides: [reservation-ui, guest-identity-sheet, reserve-button, re-reserve-deep-link]
  affects: [RegistryDetailScreen, AppNavigation, AppNavKeys]
tech_stack:
  added: []
  patterns: [Channel.BUFFERED + receiveAsFlow for one-shot events, LaunchedEffect for reservation event collection, Intent ACTION_VIEW after callable success]
key_files:
  created:
    - app/src/main/java/com/giftregistry/ui/registry/detail/ReservationCountdown.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/GuestIdentitySheet.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
    - app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt
decisions:
  - "ReservationEvent as Channel<BUFFERED> in ViewModel — one-shot events collected in LaunchedEffect(Unit) so Intent fires exactly once per reservation"
  - "Item domain model lacks expiresAt field — countdown deferred, showing only Reserved label; no domain model changes made in this plan"
  - "ReReserveDeepLink NavEntry navigates to HomeKey with Phase 6 TODO — placeholder route exists so email stub URL is well-formed (RES-08 partial)"
metrics:
  duration: 3min
  completed_date: "2026-04-11"
  tasks_completed: 3
  files_modified: 9
---

# Phase 04 Plan 04: Reservation UI Summary

**One-liner:** Reservation-to-purchase UI with Channel-based one-shot events, GuestIdentitySheet, Reserve button, and re-reserve deep link placeholder.

## What Was Built

Full Android UI for the reservation flow:

1. **RegistryDetailViewModel** extended with `ReserveItemUseCase` + `GuestPreferencesRepository` injection. New reservation state: `ReservationEvent` sealed interface with `OpenRetailer`, `ShowGuestSheet`, `ShowConflictError`. One-shot events via `Channel.BUFFERED + receiveAsFlow`. `isReserving` StateFlow guards against duplicate taps.

2. **ReservationCountdown.kt** — display-only countdown composable. Recomputes every second from `expiresAtMs` using `LaunchedEffect` + `delay(1_000L)`. No Firestore writes, no repository references (D-08 enforced).

3. **GuestIdentitySheet.kt** — `ModalBottomSheet` with three `OutlinedTextField`s (firstName, lastName, email). Validates non-blank before calling `onSubmit`.

4. **RegistryDetailScreen** updated: Reserve `Button` on `AVAILABLE` items (disabled while `isReserving`), "Reserved" label on `RESERVED` items, `LaunchedEffect(Unit)` collects reservation events — `Intent(ACTION_VIEW)` fires only after callable returns success (RES-04, D-15), conflict snackbar with localized error.

5. **AppNavKeys** — added `ReReserveDeepLink(reservationId: String)` nav key.

6. **AppNavigation** — added `entry<ReReserveDeepLink>` handler routing to `RegistryListScreen` with Phase 6 TODO log (RES-08 placeholder).

7. **Strings** — 11 `reservation_*` keys in both `values/strings.xml` (English) and `values-ro/strings.xml` (Romanian).

8. **ReserveItemViewModelTest** — replaced stub with 4 MockK-based tests (all GREEN).

## Deviations from Plan

### Auto-fixed Issues

None.

### Plan Notes

- Task 3 (checkpoint:human-verify) was auto-approved per autonomous chain execution mode.
- `Item` domain model does not expose `expiresAt` — per plan spec, only "Reserved" label shown (no countdown on cards). Countdown composable exists and will be wired when `expiresAt` is added to the domain model in a future plan.
- `ReservationCountdown.kt` comment contains the word "Firestore" in a documentation remark explaining the D-08 constraint — this is intentional and not an actual Firestore class reference.

## Known Stubs

- `ReReserveDeepLink` handler navigates to `RegistryListScreen` instead of resolving `reservationId -> registryId`. Phase 6 (email + deep link wiring) will implement the full redirect.
- `ReservationCountdown` composable is created but not used in `ItemCard` because `Item.expiresAt` does not exist yet. Countdown wiring deferred to the plan that adds `expiresAt` to the domain model.

## Self-Check: PASSED

Files verified:
- `ReservationCountdown.kt` exists
- `GuestIdentitySheet.kt` exists
- `RegistryDetailViewModel.kt` contains `ReserveItemUseCase` and `ReservationEvent`
- `AppNavKeys.kt` contains `ReReserveDeepLink`
- Commits: `24c7428` (Task 1), `44090db` (Task 2)
