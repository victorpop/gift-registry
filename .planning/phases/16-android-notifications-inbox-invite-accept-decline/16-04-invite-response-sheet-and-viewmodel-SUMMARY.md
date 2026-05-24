---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 04
subsystem: android
tags: [kotlin, compose, jetpack-hilt, material3, modal-bottom-sheet, alert-dialog, turbine, state-machine, invite-flow]

# Dependency graph
requires:
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 01
    provides: Wave 0 RED tests NotificationCardBranchingTest + InviteResponseViewModelTest + LocalizationParityTest that this plan flips GREEN; locked the predicate signature + state machine contract
  - phase: 16-android-notifications-inbox-invite-accept-decline
    plan: 03
    provides: NotificationRepository.acceptInvite/declineInvite already wired against httpsCallable; NotificationType extended with 3 new INVITE_ACCEPTED_* / DECLINED enums; placeholder when-branches in NotificationsScreen left untouched for Plan 16-05
  - phase: 12-registry-cover-photo-themed-placeholder
    provides: HeroImageOrPlaceholder composable reused verbatim — payload-driven hero render (coverUrl + occasion) with zero registry-doc read on the client (D-10)
  - phase: 08-giftmaison-design-foundation
    provides: GiftMaisonTheme.colors / typography / spacing / shapes composition local accessors used throughout the sheet
provides:
  - InviteResponseViewModel — Hilt-injected sheet-scoped state machine (Idle → Submitting → AcceptedSuccess / DeclinedSuccess / Error(action) with retry()/reset()) (D-07)
  - InviteResponseSheet — Material3 ModalBottomSheet with HeroImageOrPlaceholder + Accept/Decline CTAs + inline warn-banner + Retry on Error + DeclineConfirmDialog (D-01, D-03, D-07)
  - shouldOpenInviteSheet(notification) — pure-Kotlin top-level predicate routing tap-time sheet vs legacy navigate (D-11)
  - NotificationsViewModel extension — inviteSheetState: StateFlow<Notification?> + openInviteSheet/dismissInviteSheet (host-in-screen sheet pattern)
  - NotificationsScreen extension — LazyColumn onClick branches on shouldOpenInviteSheet; hosts InviteResponseSheet outside Scaffold so scrim covers bottom nav
  - 11 stub R.string.invite_sheet_* keys (en + ro) to keep LocalizationParityTest green; Plan 16-05 overwrites with locked copy
affects:
  - 16-05 (inbox reskin + strings) — must overwrite the 11 stub invite_sheet_* keys with locked English + Romanian copy from UI-SPEC; also replaces the 3 placeholder when-branches in NotificationsScreen for the new INVITE_ACCEPTED_* / DECLINED enum types (Plan 16-03 placeholders)
  - 16-06 (deploy + UAT) — verifies the sheet flow end-to-end on a physical device against the live europe-west3 callables deployed by Plan 16-02

# Tech tracking
tech-stack:
  added: []  # No new libraries — Material3 ModalBottomSheet + AlertDialog + Hilt + Turbine all already on classpath
  patterns:
    - "Host-in-screen sheet pattern: parent screen owns a StateFlow<Notification?> for sheet visibility; tap-time predicate decides between sheet vs navigate. Avoids leaking sheet state into a navigation key (no new NavKey, no back-stack pollution). Future invite-style sheets should follow this pattern."
    - "State-machine ViewModel with action-tagged Error: sealed interface State { Idle | Submitting(action) | Error(action, messageKey) | AcceptedSuccess | DeclinedSuccess }. The (action, messageKey) split lets the parent composable render localized copy AT render time via stringResource() rather than baking strings into the ViewModel — clean separation, easier locale switching, no Android Resources dependency in the VM."
    - "Pure-Kotlin top-level branching predicate co-located with its primary consumer composable: shouldOpenInviteSheet lives in InviteResponseSheet.kt alongside the sheet it gates. Easy to JVM-unit-test (no Android dependency), easy to discover at the call site."
    - "Stub-string + LocalizationParityTest pattern: when a plan creates UI that references R.string keys owned by a downstream plan, ship stub keys to BOTH locales (en + ro) with English placeholder copy. Keeps LocalizationParityTest green and unblocks build; downstream plan overwrites with locked copy. Avoids cross-plan compile-blocks."
    - "DisposableEffect cleanup for sheet-scoped VM error state: if (state is Error) viewModel.reset() in onDispose, so reopening the sheet starts fresh in Idle instead of replaying the prior error. Belt-and-suspenders for the rare mid-error swipe-dismiss."

key-files:
  created:
    - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt — 94 LoC — Hilt VM, sealed-interface State machine, accept/decline/retry/reset"
    - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt — 286 LoC — ModalBottomSheet + shouldOpenInviteSheet top-level predicate + DeclineConfirmDialog (private)"
  modified:
    - "app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt — +14 LoC — inviteSheetState StateFlow + openInviteSheet/dismissInviteSheet methods"
    - "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt — +24 LoC — tap-branching via shouldOpenInviteSheet + sheet host outside Scaffold"
    - "app/src/main/res/values/strings.xml — +12 lines — 11 stub invite_sheet_* keys (Plan 16-05 overwrites)"
    - "app/src/main/res/values-ro/strings.xml — +12 lines — same 11 keys in RO (English placeholder copy; Plan 16-05 translates)"
    - ".planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md — appended pre-existing AuthViewModelTest 2/12 Turbine failure as deferred (verified at HEAD 7e0d1a9 BEFORE Task 3)"

key-decisions:
  - "Reused the existing onNavigateToRegistry callback for D-05 post-accept auto-nav — NotificationsScreen passes onAcceptSuccess = { rid -> dismissInviteSheet(); onNavigateToRegistry(rid) }, and AppNavigation already wires that to backStack.add(RegistryDetailKey(rid)). No new AppNavigation prop needed; saves a parameter and keeps the screen's API surface minimal."
  - "Sheet hosted OUTSIDE the Scaffold (after the Scaffold's closing brace) so the ModalBottomSheet scrim covers the bottom nav. If the sheet were inside the Scaffold's content slot, the bottom nav would render above the scrim. This matches Phase 12+ chrome contract — sheets ALWAYS hoist above the Scaffold."
  - "Did NOT edit AppNavigation.kt — verified the existing NotificationsScreen call already passes onNavigateToRegistry = { rid -> backStack.add(RegistryDetailKey(rid)) }. The Plan 16-04 sheet's onAcceptSuccess reuses this same callback verbatim. Zero-edit Task 3 sub-step."
  - "Did NOT edit NavVisibility.kt — verified NotificationsKey is in the hidden-list exclusion (post-auth keys all show bottom nav by default). The plan's defensive-verification step was a no-op for this codebase."
  - "Error state carries messageKey: String (not @StringRes Int) — the VM has no Android Resources dependency; the parent composable resolves the key to a R.string via a when-mapping at render time. Keeps the VM pure-JVM-testable (no Robolectric needed) and lets the parent swap copy without touching the VM."
  - "Stub-strings in BOTH locales with English placeholder copy — Plan 16-05 owns the locked en + ro copy. Shipping stubs in both locales keeps LocalizationParityTest green (key parity, not value parity, is the contract) and unblocks the build today. The 11 keys are: invite_sheet_default_actor, _default_registry, _title_template, _accept_cta, _decline_cta, _decline_confirm_title, _decline_confirm_cancel, _decline_confirm_decline, _error_accept, _error_decline, _error_retry."

patterns-established:
  - "Host-in-screen sheet pattern (vs sheet-as-NavKey) — parent ViewModel owns a StateFlow for sheet visibility; tap-time pure-Kotlin predicate decides sheet vs navigate. Avoids back-stack pollution; predicate is JVM-unit-testable."
  - "Action-tagged Error state in sealed-interface VM — State.Error(action: Action, messageKey: String) lets the UI know WHICH action failed (for the right CTA pulse animation, the right retry semantics) and gets localized copy at render time without baking strings into the VM."
  - "Stub-string + LocalizationParityTest pattern — when a UI plan ships before its strings plan, stub the keys in BOTH locales with placeholder copy. LocalizationParityTest verifies key parity (not copy parity), so this is sufficient to keep the build + tests green while preserving the locked-copy plan's ownership."

requirements-completed:
  - D-01
  - D-03
  - D-05
  - D-07
  - D-11

# Metrics
duration: 6min
completed: 2026-05-24
---

# Phase 16 Plan 04: Invite Response Sheet and ViewModel Summary

**Material3 ModalBottomSheet hosting the registry hero + Accept/Decline CTAs, payload-only rendering (zero registry-doc read), wired into the inbox via a pure-Kotlin tap-branching predicate — flipping the last 2 Wave 0 Android RED tests (NotificationCardBranchingTest + InviteResponseViewModelTest) GREEN, with the full 4-test Wave 0 Android suite now passing together (NotificationCardBranchingTest 9/9, InviteResponseViewModelTest 5/5, NotificationTypeFromWireTest 10/10, NotificationRepositoryImplAcceptDeclineTest 4/4) + LocalizationParityTest 1/1.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-05-24T17:39:43Z
- **Completed:** 2026-05-24T17:45:27Z
- **Tasks:** 3
- **Files created:** 2 (InviteResponseViewModel.kt, InviteResponseSheet.kt)
- **Files modified:** 4 (NotificationsViewModel.kt, NotificationsScreen.kt, values/strings.xml, values-ro/strings.xml) + 1 deferred-items.md append

## Accomplishments

- **InviteResponseViewModel (D-07):** Hilt-injected sheet-scoped state machine. `sealed interface State { Idle | Submitting(action) | Error(action, messageKey) | AcceptedSuccess | DeclinedSuccess }`. `accept(rid)` / `decline(rid)` launch the corresponding `NotificationRepository` callable in `viewModelScope`, wrapping `.fold { onSuccess → terminal | onFailure → Error }`. `retry()` re-runs the last attempted action (cached via private `lastRegistryId` + `lastAction`). `reset()` returns to Idle (called from `DisposableEffect` on sheet dispose mid-error).
- **InviteResponseSheet (D-01):** Material3 ModalBottomSheet. Renders payload-driven hero via `HeroImageOrPlaceholder` (180dp tall, `glyphSize = 40.sp` matching Phase 12 hero pixel contract). Sheet body: title template `"{actorName} invited you to"` in `displayS`, registry name in `displayL accent` (italic from InstrumentSerif), optional event-date metadata in `monoCaps` (only when `payload.eventDateMs` parseable). Primary CTA = filled `Button(accent/accentInk, height=48dp)` with mid-flight `CircularProgressIndicator(16dp)`. Secondary CTA = `OutlinedButton(border=ink@40%, height=48dp)` with same spinner pattern. Both CTAs `enabled = !isLoading`. D-07: `confirmValueChange = { state !is Submitting }` blocks swipe-dismiss in flight + `onDismissRequest = { if (!isLoading) onDismiss() }` blocks scrim-tap. D-07: AnimatedVisibility-style warn-banner (verbatim AuthScreen pattern — `Box(clip(radius12).background(warn @ 15%).padding(12dp))`) with `TextButton(retry)` on Error state.
- **shouldOpenInviteSheet predicate (D-11):** Pure-Kotlin top-level fun (no Android dependency, JVM-testable). Returns `true` iff `notification.type == INVITE && notification.payload["pendingEntryKey"] != null`. Co-located with the sheet it gates for discoverability.
- **DeclineConfirmDialog (D-03):** Private `@Composable` using Material3 `AlertDialog`. Title template `"Decline invite to \"{registryName}\"?"` in `bodyMEmphasis ink`. Confirm = `TextButton(text=accent)`. Dismiss = `TextButton(text=default)`. Tapping Decline on the sheet sets `showDeclineDialog = rememberSaveable mutableStateOf` true; Confirm runs `viewModel.decline(rid)`; Cancel just dismisses the dialog.
- **NotificationsViewModel extension:** Added `inviteSheetState: StateFlow<Notification?>` (host-in-screen sheet pattern) + `openInviteSheet(notification)` + `dismissInviteSheet()`.
- **NotificationsScreen wiring:** `LazyColumn` items `onClick` now branches via `if (shouldOpenInviteSheet(n)) viewModel.openInviteSheet(n) else onNavigateToRegistry(...)`. Sheet hosted OUTSIDE the Scaffold (so its scrim covers the bottom nav). `onAcceptSuccess = { rid -> dismissInviteSheet(); onNavigateToRegistry(rid) }` — D-05 auto-nav to RegistryDetailKey on accept success, reusing the existing `onNavigateToRegistry` callback verbatim (no new AppNavigation prop needed).
- **Stub strings (11 keys × 2 locales):** Added to both `values/strings.xml` + `values-ro/strings.xml` with English placeholder copy. Plan 16-05 will overwrite with the locked en + ro copy. LocalizationParityTest passes (key parity preserved).

## Task Commits

Each task was committed atomically:

1. **Task 1: Create InviteResponseViewModel state machine** — `8475bb1` (feat)
2. **Task 2: Create InviteResponseSheet + shouldOpenInviteSheet predicate + stub strings** — `7e0d1a9` (feat)
3. **Task 3: Wire tap-branching + InviteResponseSheet host in NotificationsScreen** — `2043178` (feat)

**Plan metadata:** _(pending — final commit after STATE.md / ROADMAP.md / REQUIREMENTS.md updates)_

## Files Created/Modified

### Created (2)
- `app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt` (94 LoC) — `@HiltViewModel` D-07 state machine
- `app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt` (286 LoC) — D-01 ModalBottomSheet + D-11 `shouldOpenInviteSheet` top-level predicate + private `DeclineConfirmDialog` (D-03)

### Modified (4)
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt` — added `inviteSheetState` StateFlow + `openInviteSheet`/`dismissInviteSheet`
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` — tap-branching via `shouldOpenInviteSheet` + sheet host outside Scaffold (D-05 auto-nav via existing `onNavigateToRegistry`)
- `app/src/main/res/values/strings.xml` — 11 stub `invite_sheet_*` keys (en)
- `app/src/main/res/values-ro/strings.xml` — 11 stub `invite_sheet_*` keys (RO; English placeholder copy; Plan 16-05 translates)

### Appended (1)
- `.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md` — pre-existing AuthViewModelTest 2/12 Turbine failure logged as deferred (verified at HEAD `7e0d1a9` BEFORE Task 3 edits — same 2 failures; unrelated to Plan 16-04 scope)

## Decisions Made

- **Reused `onNavigateToRegistry` for D-05 auto-nav** — `onAcceptSuccess = { rid -> dismissInviteSheet(); onNavigateToRegistry(rid) }`. AppNavigation already wires `onNavigateToRegistry` to `backStack.add(RegistryDetailKey(rid))`. Zero new prop on `NotificationsScreen`; clean minimal API.
- **Sheet hosted OUTSIDE the Scaffold** — ModalBottomSheet scrim must cover the bottom nav (chrome contract since Phase 12). Hosted after the Scaffold's closing brace.
- **AppNavigation.kt — no edit needed** — verified existing wiring is correct as-is.
- **NavVisibility.kt — no edit needed** — `NotificationsKey` already shows bottom nav (hidden-list exclusion covers only `AuthKey, OnboardingKey, ReReserveDeepLink`).
- **`State.Error(action, messageKey: String)` not `@StringRes Int`** — keeps VM pure-JVM-testable. Parent composable resolves `messageKey` to `R.string.invite_sheet_error_{accept,decline}` at render time.
- **Stub strings in BOTH locales with English placeholders** — LocalizationParityTest verifies key parity (not copy parity); shipping stubs in both keeps build green and preserves Plan 16-05's copy-ownership.
- **`shouldOpenInviteSheet` is a TOP-LEVEL fun, not an extension on `Notification`** — matches Wave 0 test's import `import com.giftregistry.ui.notifications.shouldOpenInviteSheet` and `shouldOpenInviteSheet(notif)` call site exactly.

## Deviations from Plan

**None — plan executed exactly as written.**

The only minor variances from the plan text are clarifying notes, not deviations:

- The plan said "find the NotificationsKey case... VERIFY (no edit if already correct)" — verification confirmed the existing wiring is correct, so no AppNavigation.kt edit was made. This was an expected outcome per the plan's "no edit if already correct" instruction.
- The plan said `NavVisibility.kt` may not have `NotificationsKey` in its hidden-list; this verification step found that the actual predicate is inverted (hidden-list exclusion of `AuthKey, OnboardingKey, ReReserveDeepLink`), so `NotificationsKey` is correctly covered as a post-auth key. No edit needed. This was an expected outcome.
- Plan Task 2 noted that NotificationCardBranchingTest expected "6 tests pass"; actual test file has 9 cases (3 INVITE variants + 3 new INVITE_ACCEPTED_*/DECLINED + 3 other types). All 9 pass.

## Issues Encountered

- **Pre-existing AuthViewModelTest 2/12 Turbine timing failure** — discovered when running the full `:app:testDebugUnitTest` suite to scope regressions during Task 3 verification. Stash-verified at HEAD `7e0d1a9` (post-Task-2, pre-Task-3): same 2 failures with the same `Expected no events but found Item(Unauthenticated)` Turbine error. Unrelated to Plan 16-04 — affects `ui/auth/AuthViewModelTest.kt`, which is outside Plan 16-04 scope (Plan 16-04 touches only `ui/notifications/*` + theme reads + resource strings; AuthViewModel.kt is untouched). Logged to `deferred-items.md` per executor scope-boundary rule. All 5 in-scope test suites (NotificationCardBranchingTest, InviteResponseViewModelTest, NotificationTypeFromWireTest, NotificationRepositoryImplAcceptDeclineTest, LocalizationParityTest) are GREEN.

## Test Results

| Suite | Tests | Status | Notes |
|---|---|---|---|
| `InviteResponseViewModelTest` | 5 | ✅ PASS | Plan 16-01 RED flipped GREEN by Task 1 |
| `NotificationCardBranchingTest` | 9 | ✅ PASS | Plan 16-01 RED flipped GREEN by Task 2 |
| `NotificationTypeFromWireTest` | 10 | ✅ PASS | Plan 16-01 RED — was already green after Plan 16-03 but needed the test source set to compile; now confirmed green |
| `NotificationRepositoryImplAcceptDeclineTest` | 4 | ✅ PASS | Plan 16-01 RED — same as above; now confirmed green |
| `LocalizationParityTest` | 1 | ✅ PASS | Key parity preserved with stub strings in both locales |
| **All 5 in-scope target suites** | **29** | ✅ **29/29 GREEN** | — |
| All `com.giftregistry.ui.notifications.*` tests | 14 | ✅ PASS | No regressions in NotificationsScreen / NotificationsViewModel suite |
| Full `:app:testDebugUnitTest` | — | 2 pre-existing failures in `AuthViewModelTest` (out of scope; logged to deferred-items.md) | All 4 Plan 16-01 Android RED tests flip GREEN simultaneously |

**Build verification:** `./gradlew :app:compileDebugKotlin` exits 0 after every task. `./gradlew :app:compileDebugUnitTestKotlin` (full test source set compile) exits 0 — Plan 16-03's "test source set blocked until 16-04 ships" gate is now resolved.

## Acceptance Criteria Verification

Per the plan's per-task acceptance lists:

**Task 1 (InviteResponseViewModel):**
- ✅ File exists; contains `@HiltViewModel`, `sealed interface State`, `data object Idle`, `data class Submitting(val action: Action)`, `data class Error(val action: Action`, `data object AcceptedSuccess`, `data object DeclinedSuccess`, `enum class Action { Accept, Decline }`, `fun accept(registryId: String)`, `fun decline(registryId: String)`, `fun retry()`, `notificationRepository.acceptInvite`, `notificationRepository.declineInvite`
- ✅ `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.InviteResponseViewModelTest"` exits 0 (5 tests pass)

**Task 2 (InviteResponseSheet + predicate + strings):**
- ✅ File `InviteResponseSheet.kt` exists; contains `fun shouldOpenInviteSheet(notification: Notification): Boolean`, `notification.type == NotificationType.INVITE`, `notification.payload["pendingEntryKey"] != null`, `fun InviteResponseSheet(`, `ModalBottomSheet(`, `rememberModalBottomSheetState`, `confirmValueChange = { state !is InviteResponseViewModel.State.Submitting }`, `HeroImageOrPlaceholder(`, `glyphSize = 40.sp`, `AlertDialog(`, `viewModel.accept(registryId)`, `viewModel.decline(registryId)`, `viewModel.retry()`, `colors.warn.copy(alpha = 0.15f)`, `onAcceptSuccess(registryId)`, `private fun DeclineConfirmDialog`
- ✅ `app/src/main/res/values/strings.xml` contains `invite_sheet_accept_cta`, `invite_sheet_decline_confirm_title`, `invite_sheet_error_retry`
- ✅ `app/src/main/res/values-ro/strings.xml` contains `invite_sheet_accept_cta` (key parity verified)
- ✅ `./gradlew :app:compileDebugKotlin` exits 0
- ✅ `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.NotificationCardBranchingTest"` exits 0 (9 tests pass)
- ✅ `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest"` exits 0 (1 test pass)

**Task 3 (Wire NotificationsScreen + ViewModel + AppNav verify):**
- ✅ `NotificationsViewModel.kt` contains `_inviteSheetState`, `fun openInviteSheet(notification: Notification)`, `fun dismissInviteSheet()`, `val inviteSheetState: StateFlow<Notification?>`
- ✅ `NotificationsScreen.kt` contains `shouldOpenInviteSheet(notification)`, `viewModel.openInviteSheet(notification)`, `InviteResponseSheet(`, `viewModel.dismissInviteSheet()`, `onAcceptSuccess = { rid ->`
- ✅ `AppNavigation.kt` contains `NotificationsScreen(` (verified — no edit needed)
- ✅ `./gradlew :app:compileDebugKotlin` exits 0
- ✅ `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*"` exits 0 (all 14 notification tests pass)

## Self-Check: PASSED

Files verified to exist:
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt`
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt`
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt` (modified)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` (modified)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values/strings.xml` (modified)
- `/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values-ro/strings.xml` (modified)
- `/Users/victorpop/ai-projects/gift-registry/.planning/phases/16-android-notifications-inbox-invite-accept-decline/deferred-items.md` (appended)

Commits verified to exist:
- `8475bb1` (Task 1 — InviteResponseViewModel)
- `7e0d1a9` (Task 2 — InviteResponseSheet + predicate + stub strings)
- `2043178` (Task 3 — wire NotificationsScreen + ViewModel)

## User Setup Required

None — no external service configuration required.

## Next Phase / Plan Readiness

- **Plan 16-05 (inbox reskin + strings) — Wave 4 ready to start.** It needs to:
  - Overwrite the 11 stub `invite_sheet_*` keys in both `values/strings.xml` and `values-ro/strings.xml` with the locked English + Romanian copy from UI-SPEC §Copy.
  - Add the 6 string keys for the 3 new notification types (`notification_invite_accepted_self_title/body`, `notification_invite_accepted_title/body`, `notification_invite_declined_title/body`) in both locales.
  - Replace the 9 placeholder when-branches in `NotificationsScreen.kt` (`toIcon`, `localizedTitle`, `localizedBody` × 3 new enum types each) with proper Material icons + `stringResource` lookups.
  - Apply the inbox reskin (card styling per UI-SPEC).
- **Plan 16-06 (deploy + UAT) — Wave 5 ready after Plan 16-05.** Will verify the full flow end-to-end on a physical device against the live europe-west3 callables.
- **No blockers.** All 4 Wave 0 Android RED tests now GREEN together; test source set compiles; main code compiles; in-scope tests all pass; pre-existing AuthViewModelTest failure logged as deferred (out of scope).

---
*Phase: 16-android-notifications-inbox-invite-accept-decline*
*Completed: 2026-05-24*
