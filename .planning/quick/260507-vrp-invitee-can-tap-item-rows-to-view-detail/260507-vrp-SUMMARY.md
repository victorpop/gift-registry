---
phase: quick-260507-vrp
plan: 01
subsystem: registry-detail + item-edit (UI dual-mode)
tags: [android, compose, registry-detail, item-edit, invitee-flow, reservation, ownership-gate]
one_liner: "Invitee taps item row -> EditItemScreen read-only mode + Reserve/Mark-as-purchased actions reusing the existing giver-flow use cases verbatim."
requires:
  - RegistryDetailViewModel.isOwner pattern (Phase 12 D-13 + quick-260507-uzv)
  - ReserveItemUseCase + ConfirmPurchaseUseCase + GuestPreferencesRepository (Phases 4 + 6, unchanged)
  - GuestIdentitySheet composable (Phase 4, reused as-is)
  - EditItemKey nav route already carrying (registryId, itemId) (no nav change)
  - Phase 6 string resources (reservation_reserve_button, reservation_confirm_purchase_cta,
    reservation_confirm_purchase_success/error, reservation_error_unavailable/generic) in EN + RO
provides:
  - Row-level tap entry into EditItemScreen for both owners and invitees
  - Dual-mode EditItemScreen: owner = full edit (unchanged); invitee = read-only + giver actions
  - Owner-gated per-item kebab on RegistryItemRow (showOverflow flag)
  - EditItemViewModel.isOwner StateFlow (line-for-line mirror of RegistryDetailViewModel.isOwner)
  - EditItemViewModel reservation orchestration mirroring RegistryDetailViewModel.performReservation
    + onConfirmPurchase, with two adjustments (raw Int snackbar resId; giverId = currentUser.uid)
affects:
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  - app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
  - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
tech-stack:
  added: []
  patterns:
    - "Row-level Modifier.clickable(onClick = onTap) wraps the entire row and the IconButton inner clickable consumes the kebab tap (Compose hit-testing prefers inner clickable)"
    - "Owner-gated per-item kebab via showOverflow: Boolean = true default + RegistryDetailScreen call site passes showOverflow = isOwner"
    - "Dual-mode UI on EditItemScreen via if (isOwner) { /* full-edit verbatim */ } else { /* read-only + giver actions */ }"
    - "Re-declared private nested ReservationEvent inside EditItemViewModel (NOT promoted to top-level type) to keep the diff localized — same 4-line contract as RegistryDetailViewModel.ReservationEvent, both VMs feed identical UI side effects"
    - "Snackbar payload as raw Int resId (not SnackbarMessage sealed type) — EditItemScreen does not consume FCM push events"
    - "giverId passed to ReserveItemUseCase = authRepository.currentUser?.uid (signed-in invitee has UID; web giver flow always passes null because anonymous)"
    - "itemFlow StateFlow exposes reactive Item? for status-based button gating (separate from existing init-block one-shot lookup that populates form fields)"
    - "Loading-state safe — isOwner defaults false during load (Eagerly + initial-false + .catch{emit(false)}) so owner-only edit affordances never flash for an invitee; Reserve/Mark-as-purchased buttons gated on currentItem?.status which is null while loading -> both buttons disabled"
key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt
    - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
decisions:
  - "Re-declared ReservationEvent privately inside EditItemViewModel rather than promoting to top-level domain type — keeps the diff localized to two files; documented with a comment so future readers know the two contracts must stay aligned"
  - "Snackbar channel uses raw Int resId not the SnackbarMessage sealed type from RegistryDetailViewModel — EditItemScreen does not consume FCM push events, simpler payload keeps the new code surface small"
  - "giverId passed as authRepository.currentUser?.uid on the Android invitee path, not null — signed-in invitees have a UID for analytics; the public web giver flow keeps passing null because anonymous"
  - "EditItemViewModel.itemFlow added as a separate reactive Item? observation channel (not folded into the existing init-block firstOrNull lookup) so status-based button gates update reactively when the Cloud Function flips RESERVED -> PURCHASED"
  - "showOverflow: Boolean = true default on RegistryItemRow keeps the contract permissive for any future caller; the (only) current call site in RegistryDetailScreen explicitly passes showOverflow = isOwner"
  - "onTap is required (no default) on RegistryItemRow so the screen call site is forced to wire navigation explicitly; prevents silent regression where a future copy-paste forgets to pass it"
metrics:
  duration: "~9 min"
  tasks: 3
  files: 6
  tests: 9
  completed_date: "2026-05-07"
---

# Quick Task 260507-vrp: Invitees can tap item rows to view detail and reserve/mark-as-purchased

## One-liner

Invitee taps item row on RegistryDetailScreen -> EditItemScreen opens in read-only mode with Reserve + Mark-as-purchased buttons; per-item kebab now owner-only; both giver actions reuse the EXACT SAME use cases the giver flow already uses on RegistryDetailScreen (zero new reserve/purchase code paths).

## Pattern

Three coordinated UI/VM edits delivered in three commits each (RED -> GREEN for the two TDD tasks):

1. **Row-level tap + owner-gated kebab on RegistryItemRow.** New required `onTap: () -> Unit` parameter wires the row's outer `Row` modifier with `Modifier.clickable(onClick = onTap)` (placed between `.fillMaxWidth()` and `.drawBehind{}` so the divider line still draws on top of the ripple bounds). New `showOverflow: Boolean = true` parameter wraps the kebab `Box { IconButton + DropdownMenu }` in `if (showOverflow) { ... }`; the `Spacer + StatusChip` stays outside the gate (invitees still see status). RegistryDetailScreen call site passes `onTap = { onNavigateToEditItem(item.id) }` (same callback as `onEdit`) and `showOverflow = isOwner` (reuses the pre-existing `isOwner` StateFlow already collected at line 86).

2. **EditItemViewModel.isOwner via combine + EditItemScreen dual-mode.** EditItemViewModel grows 5 new constructor params (`AuthRepository`, `ObserveRegistryUseCase`, `ReserveItemUseCase`, `ConfirmPurchaseUseCase`, `GuestPreferencesRepository`); all are already provided in the existing Hilt graph used by RegistryDetailViewModel so no module changes. `isOwner` derived as `combine(registry, authRepository.authState) { reg, user -> reg != null && user != null && reg.ownerId == user.uid }.catch { emit(false) }.stateIn(viewModelScope, Eagerly, false)` — line-for-line mirror of RegistryDetailViewModel.isOwner so both surfaces use the same predicate as the server. EditItemScreen Column body now branches on `isOwner`: owner branch is the existing form verbatim (URL+Fetch row, OG indicators, Save Button); invitee branch renders the same 5 fields with `enabled = false` and skips Save / Fetch / error / OG indicators.

3. **EditItemViewModel reservation orchestration + EditItemScreen action buttons.** Private nested `sealed interface ReservationEvent { OpenRetailer / ShowGuestSheet / ShowConflictError }` (re-declared rather than promoted to a top-level type — the two VMs feed identical UI side effects so the 4-line duplication is cheaper than touching every test that names the type). `onReserveClicked` -> `getGuestIdentity()` check -> sheet on null OR `performReservation`; `performReservation` calls `ReserveItemUseCase(registryId, itemId, guest, giverId = authRepository.currentUser?.uid)`, persists `activeReservationId` on success, emits `OpenRetailer(affiliateUrl)`; emits `ShowConflictError(code)` on failure. `onConfirmPurchase` -> `confirmingPurchase = true`, calls `ConfirmPurchaseUseCase`, emits `R.string` resId snackbar, clears `activeReservationId` on success. EditItemScreen wires `SnackbarHost` slot, two `LaunchedEffect(Unit)` collectors (one for reservationEvents -> `Intent.ACTION_VIEW` / sheet open / snackbar; one for snackbarMessages -> snackbar + `onBack()` on success resId), Reserve + Mark-as-purchased Buttons with status gates (`canReserve = item.status == AVAILABLE && !isReserving`; `canConfirmPurchase = item.status == RESERVED && activeReservationId != null && !confirmingPurchase`), and `GuestIdentitySheet` rendered when `showGuestSheet` is true.

## Test count + names (9 new GREEN tests)

**EditItemViewModelIsOwnerTest** (4 tests, mirror RegistryDetailViewModelIsOwnerTest verbatim):
1. `isOwner is true when registry ownerId matches auth uid`
2. `isOwner is false when registry ownerId differs from auth uid`
3. `isOwner is false when registry is null`
4. `isOwner is false when authState is null`

**EditItemViewModelReservationTest** (5 tests, all using turbine.test for the Channel/SharedFlow):
1. `onReserveClicked success persists activeReservationId and emits OpenRetailer` — verifies giverId = "user-2" passed AND `setActiveReservationId("res-1")` called AND `OpenRetailer("https://aff.example/abc")` event emitted.
2. `onReserveClicked with no guest identity emits ShowGuestSheet and skips reserve` — verifies ReserveItemUseCase NOT invoked when getGuestIdentity returns null.
3. `onReserveClicked failure emits ShowConflictError and does not persist reservation id` — verifies `setActiveReservationId` NOT called on failure path.
4. `onConfirmPurchase success emits success resId and clears activeReservationId` — verifies `R.string.reservation_confirm_purchase_success` emitted AND `setActiveReservationId(null)` called.
5. `onConfirmPurchase failure emits error resId and does not clear activeReservationId` — verifies `R.string.reservation_confirm_purchase_error` emitted AND `setActiveReservationId(null)` NOT called.

## Owner / invitee observable behaviour delta on EditItemScreen

| Surface                              | Owner (today + post-vrp, IDENTICAL) | Invitee (pre-vrp)         | Invitee (post-vrp, NEW)                                                                                    |
| ------------------------------------ | ----------------------------------- | ------------------------- | ---------------------------------------------------------------------------------------------------------- |
| Tap row on RegistryDetailScreen      | Opens EditItemScreen full-edit      | No-op (unclickable)       | Opens EditItemScreen read-only                                                                             |
| Per-item kebab on RegistryItemRow    | Visible (Edit / Delete entries)     | Visible (server-rejected) | **Hidden**                                                                                                 |
| EditItemScreen URL + Fetch row       | Editable, Fetch enabled             | n/a                       | Read-only OutlinedTextField (no Fetch button)                                                              |
| EditItemScreen 5 form fields         | Editable                            | n/a                       | `enabled = false` (Material3 greys them)                                                                   |
| EditItemScreen Save button           | Visible + functional                | n/a                       | **Absent**                                                                                                 |
| EditItemScreen Reserve button        | Absent                              | n/a                       | **Visible**, enabled when `item.status == AVAILABLE && !isReserving`                                       |
| EditItemScreen Mark-as-purchased btn | Absent                              | n/a                       | **Visible**, enabled when `item.status == RESERVED && activeReservationId != null && !confirmingPurchase`  |
| GuestIdentitySheet                   | n/a                                 | n/a                       | Shown on Reserve tap when `getGuestIdentity()` returns null; persists GuestUser then auto-fires reservation |
| Snackbars                            | n/a (no Scaffold snackbarHost)      | n/a                       | Conflict / unavailable / generic-error on reserve fail; success / error on confirm-purchase                 |

## Confirmation: zero touches to security-critical surfaces

- **firestore.rules** — untouched
- **storage.rules** — untouched
- **Cloud Functions** (functions/src/**) — untouched (createReservation + confirmPurchase already accept the calls; Phase 6 D-decision: confirmPurchase has no auth guard because guest givers must be able to confirm purchase)
- **GuestPreferencesRepository** + impl — untouched (reused as-is)
- **ReservationRepository** + impl — untouched
- **ReserveItemUseCase** — untouched (signature already supports the optional `giverId` param)
- **ConfirmPurchaseUseCase** — untouched
- **AuthRepository** — untouched
- **NavKeys** (`AppNavKeys.kt`) — untouched (`EditItemKey` already carried `(registryId, itemId)`)
- **AppNavigation.kt** — untouched (`entry<EditItemKey>` already wires `EditItemScreen`)
- **strings.xml** (EN + RO) — untouched (`git diff` returns empty); reuses 6 pre-existing keys: `reservation_reserve_button`, `reservation_confirm_purchase_cta`, `reservation_confirm_purchase_success`, `reservation_confirm_purchase_error`, `reservation_error_unavailable`, `reservation_error_generic`

## Verification ledger

| Check                                                                                | Result                                          |
| ------------------------------------------------------------------------------------ | ----------------------------------------------- |
| `./gradlew :app:compileDebugKotlin -q`                                               | GREEN                                           |
| `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.edit.*"`         | 9/9 GREEN (4 isOwner + 5 reservation)           |
| `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*"`   | GREEN (no regression on isOwner / confirm tests) |
| `./gradlew :app:testDebugUnitTest` (full suite)                                      | GREEN                                           |
| `./gradlew :app:assembleDebug -q` (Hilt graph sanity)                                | GREEN                                           |
| `git diff app/src/main/res/values/strings.xml app/src/main/res/values-ro/strings.xml` | empty (no string changes)                       |

## Deviations from Plan

None — the plan executed exactly as written. The plan's recommended approach for every fork in the road (re-declaring `ReservationEvent` privately rather than promoting; raw Int snackbar resId rather than `SnackbarMessage` sealed type; turbine for the events Channel; passing `giverId = currentUser.uid` rather than null) was followed verbatim.

The plan called out that `User.displayName` and `User.email` may be null for some Firebase auth providers and recommended always going through `getGuestIdentity()` regardless of whether the caller is signed-in (so the `GuestIdentitySheet` collects a usable name + email once per session and persists in DataStore). That recommendation was honoured — `onReserveClicked` always calls `guestPreferencesRepository.getGuestIdentity()` first, mirroring RegistryDetailViewModel.performReservation.

## Outstanding follow-up

- **Device verification on a real owner + invitee account pair.** Combine with the still-pending Task 2 device passes from `quick-260507-uzv` and `quick-260507-veb`. The unified device session should walk these scenarios:
  1. Owner taps row -> EditItemScreen opens in full-edit mode, Save button visible, kebab Edit / Delete still entry-points.
  2. Owner kebab tap -> Edit / Share / Invite / Delete still appear (quick-260507-uzv contract).
  3. Owner top-bar Share + ShareBanner pill + Add-an-item CTA all visible (quick-260507-veb contract).
  4. Switch to invitee account (registry shared via invite). Invitee opens registry: per-item kebab gone on every row, top-bar Share gone, ShareBanner gone, "+ Add an item" CTA gone (quick-260507-veb contract).
  5. Invitee taps any item row -> EditItemScreen opens in read-only mode (fields greyed, no Save).
  6. Invitee taps Reserve -> GuestIdentitySheet appears (or skips if guest identity already cached); fill name/email/submit -> affiliate URL opens in browser via `Intent.ACTION_VIEW`.
  7. Return to app -> EditItemScreen now shows status RESERVED -> Reserve disabled, Mark-as-purchased ENABLED.
  8. Tap Mark-as-purchased -> success snackbar -> screen pops back to RegistryDetailScreen.
  9. Refresh / reopen -> status now PURCHASED -> both buttons disabled.
  10. Edge: invitee tries to reserve an already-RESERVED item via direct nav -> Reserve disabled by `canReserve = currentItem?.status == AVAILABLE` gate.

## Self-Check: PASSED

**Files exist:**
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt` FOUND
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` FOUND
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt` FOUND
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt` FOUND
- `/Users/victorpop/ai-projects/gift-registry/app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt` FOUND
- `/Users/victorpop/ai-projects/gift-registry/app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt` FOUND

**Commits exist (in chronological order on `main`):**
- `a06622d` feat(quick-260507-vrp-01): wire item-row tap and gate per-item kebab on ownership FOUND
- `7c74722` test(quick-260507-vrp-02): RED — pin EditItemViewModel.isOwner contract FOUND
- `6aeae70` feat(quick-260507-vrp-02): GREEN — EditItemScreen dual-mode (read-only invitee branch) FOUND
- `125455b` test(quick-260507-vrp-03): RED — pin invitee Reserve + Mark-as-purchased contract FOUND
- `3c4b663` feat(quick-260507-vrp-03): GREEN — invitee Reserve + Mark-as-purchased on EditItemScreen FOUND

No stubs detected in modified files (Compose `placeholder = ...` matches are the standard OutlinedTextField hint API, not stub markers; the pre-existing `RegistryItemRow.kt:137 "v1.2 TODO"` for reserver/giver sub-line is documented and out of scope).
