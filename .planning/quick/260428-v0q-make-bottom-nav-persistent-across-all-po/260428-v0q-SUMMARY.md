---
phase: quick-260428-v0q
plan: 01
subsystem: ui
tags: [navigation, compose, bottom-nav, predicate, kotlin]

requires:
  - phase: 09-shared-chrome-status-ui
    provides: "Any?.showsBottomNav() predicate (visible-whitelist HomeKey + RegistryDetailKey only)"
provides:
  - "Inverted Any?.showsBottomNav() to a 4-case hidden-whitelist (null, AuthKey, OnboardingKey, ReReserveDeepLink)"
  - "Bottom nav now persistent on every authenticated destination — Settings, Notifications, Stores, all forms"
  - "BottomNavVisibilityTest pinning contract — 14 tests (4 hidden, 10 visible)"
affects: [Settings, Notifications, StoreList, StoreBrowser, CreateRegistry, EditRegistry, AddItem, EditItem]

tech-stack:
  added: []
  patterns:
    - "Hidden-whitelist navigation predicate — fewer arms, default-true safer for new keys"

key-files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
    - app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt

key-decisions:
  - "Hidden-whitelist over visible-whitelist: any future post-auth nav key shows the bar by default; only the 4 well-known pre-auth/transitional keys must opt out explicitly"
  - "AppNavigation.kt:128 left untouched — call site `currentKey.showsBottomNav()` already invokes the predicate with nullable receiver; semantics swap is fully encapsulated in NavVisibility.kt"
  - "GiftMaisonBottomNav.kt:43 KDoc cross-reference left untouched — comment reads accurately ('Visibility predicate Any?.showsBottomNav() lives in NavVisibility.kt'); no 'HomeKey + RegistryDetailKey only' wording present so no edit needed"

patterns-established:
  - "TDD ship-together for predicate flips: write the new pinning tests + flip the impl in a single commit so the build is green at every step (no transient RED on main)"

requirements-completed:
  - QUICK-260428-V0Q-NAV

duration: ~2min (executor)
completed: 2026-04-28
---

# quick-260428-v0q: Persistent Bottom Nav Summary

**`Any?.showsBottomNav()` inverted from a 2-key visible-whitelist (HomeKey + RegistryDetailKey) to a 4-case hidden-whitelist (null, AuthKey, OnboardingKey, ReReserveDeepLink) — bottom nav is now persistent on every authenticated destination, fixing the user's reported bug where tapping YOU (Settings) hid the chrome.**

## Performance

- **Duration:** ~2 min (executor wall-clock; build+test ~12 s)
- **Started:** 2026-04-28T19:24:54Z
- **Completed:** 2026-04-28T19:26:35Z
- **Tasks executed:** 1 of 2 (Task 2 is `checkpoint:human-verify` — outstanding for the user)
- **Files modified:** 2

## Accomplishments

- Predicate flip: `NavVisibility.kt` now describes a hidden-whitelist contract; KDoc rewritten; HomeKey + RegistryDetailKey imports removed; AuthKey + OnboardingKey + ReReserveDeepLink imports added.
- Test contract pinned: `BottomNavVisibilityTest` covers all 14 cases (4 hidden + 10 visible — every nav key in `AppNavKeys.kt` plus the null receiver). 8 keys flipped from `assertFalse` → `assertTrue` (Settings, Notifications, CreateRegistry, EditRegistry, AddItem, EditItem, StoreList, StoreBrowser).
- Verified zero collateral: full `./gradlew :app:testDebugUnitTest` green; no other test pinned the old contract.
- Surgical scope: `AppNavigation.kt:128` (consumer call site), `AppNavKeys.kt`, and `GiftMaisonBottomNav.kt` not touched. No string resource, no dependency, no version bump.

## Predicate Diff (before / after)

**Before** (`NavVisibility.kt`, Plan 09-02 visible-whitelist):

```kotlin
import com.giftregistry.ui.navigation.HomeKey
import com.giftregistry.ui.navigation.RegistryDetailKey

fun Any?.showsBottomNav(): Boolean = when (this) {
    is HomeKey           -> true
    is RegistryDetailKey -> true
    else                 -> false
}
```

**After** (quick-260428-v0q hidden-whitelist):

```kotlin
import com.giftregistry.ui.navigation.AuthKey
import com.giftregistry.ui.navigation.OnboardingKey
import com.giftregistry.ui.navigation.ReReserveDeepLink

fun Any?.showsBottomNav(): Boolean = when (this) {
    null                 -> false
    is AuthKey           -> false
    is OnboardingKey     -> false
    is ReReserveDeepLink -> false
    else                 -> true
}
```

Notes:
- `null` arm listed first so the literal-match precedes the `is X` smart-casts. Required because the default arm flipped from `false` → `true`; without an explicit `null -> false`, a null receiver would now return `true`.
- Default-true makes any future post-auth nav key show the bar by default — safer for ongoing UI work than a visible-whitelist that requires an explicit add.

## Test Flip Tally

`BottomNavVisibilityTest.kt` — 14 cases total covering every nav key in `AppNavKeys.kt` plus the null receiver:

| # | Case | Before | After | Change |
|---|------|--------|-------|--------|
| 1 | `nullKey_hidesNav` | assertFalse | assertFalse | unchanged |
| 2 | `authKey_hidesNav` | assertFalse | assertFalse | unchanged |
| 3 | `onboardingKey_hidesNav` | assertFalse | assertFalse | unchanged |
| 4 | `reReserveDeepLink_hidesNav` | assertFalse | assertFalse | unchanged |
| 5 | `homeKey_showsNav` | assertTrue | assertTrue | unchanged |
| 6 | `registryDetailKey_showsNav` | assertTrue | assertTrue | unchanged |
| 7 | `settingsKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** (the user's reported bug) |
| 8 | `notificationsKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 9 | `createRegistryKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 10 | `editRegistryKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 11 | `addItemKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 12 | `editItemKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 13 | `storeListKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |
| 14 | `storeBrowserKey_showsNav` (was `_hidesNav`) | assertFalse | assertTrue | **FLIPPED** |

Result: 8 flipped, 6 unchanged. JUnit XML confirms 14 tests, 0 failures, 0 errors.

> **Note on plan miscount:** the plan's `<behavior>` block summarised "13 cases (4 false, 9 true)" but explicitly enumerated 10 visible cases (Home, RegistryDetail, Settings, Notifications, CreateRegistry, EditRegistry, AddItem, EditItem, StoreList, StoreBrowser). The implementation matches the explicit enumeration verbatim and the file-artifact spec in the action body, yielding 14 tests total. No deviation — the planner's tally line undercounted by one.

## Task Commits

1. **Task 1: Invert showsBottomNav() predicate + flip BottomNavVisibilityTest assertions** — `a486ca5` (fix)
2. **Task 2: Human verifies on device across 8 scripted scenarios** — OUTSTANDING (`checkpoint:human-verify`, no commit)

## Files Created/Modified

- `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` — predicate body inverted, KDoc rewritten, imports updated.
- `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — 14-case pinning contract for the new hidden-whitelist.

## Decisions Made

- **Ship tests + impl in one commit** (TDD-collapsed): the plan explicitly requested it ("tests and impl ship together so the build is green at every step"). Avoids transient RED on main between commits and prevents a follow-up commit racing the impl.
- **Leave `GiftMaisonBottomNav.kt:43` KDoc alone:** confirmed via Grep — the comment reads "Visibility predicate `Any?.showsBottomNav()` lives in NavVisibility.kt" with no "HomeKey + RegistryDetailKey only" wording. Per plan instruction "if it says ... update the comment, otherwise leave it alone."
- **Order `null` first in the `when {}`:** Kotlin matches `null` cleanly when listed as a literal arm; safer than relying on smart-cast nullability semantics ahead of `is X` checks.

## Deviations from Plan

None — plan executed exactly as written.

(The 14-vs-13 case-count observation above is a planner tally error in commentary, not an implementation deviation. The action body's file artifact + explicit enumeration in `<behavior>` were followed verbatim.)

## Out-of-Scope Concerns Surfaced (NOT auto-fixed — for follow-up)

The planner flagged five concerns under `<concerns>` in the PLAN. Per the planner's intent and the executor constraint ("must NOT be auto-fixed"), they are recorded here verbatim for the human to evaluate during Task 2 verification. If any are confirmed as a usability problem on device, promote them to a follow-up quick-task or to STATE.md "Pending Todos".

1. **AddItemScreen — dual-CTA stacking with nav bar**
   - File: `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt:173-189`
   - Concern: screen owns its own `Scaffold.bottomBar = { AddItemDualCtaBar(...) }`. With the outer `AppNavigation` Scaffold also rendering a bottomBar (the persistent nav), the user will now see TWO stacked bottom bars: dual-CTA on top, persistent nav below.
   - Why it's not a regression: `AppNavigation.kt:179` propagates `innerPadding` correctly via `Modifier.padding(innerPadding)` on the NavDisplay; tap targets work; no overlap.
   - UX call-out: density. Follow-up could re-style `AddItemDualCtaBar` (inline above scroll content) or hide the persistent nav on AddItemKey only — neither is in scope here.
   - Preserved from quick-260428-iny: dual-CTA gating (`Save` disabled when `fromAddSheet=true && selectedRegistryId == null`) untouched.

2. **CreateRegistryScreen — Continue CTA stacking with nav bar**
   - File: `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt:253-293`
   - Same pattern as AddItem; "Continue · add items →" pill button now sits above the persistent nav.
   - Same UX density call-out, same out-of-scope conclusion.

3. **StoreBrowserScreen — Add-to-list CTA stacking with nav bar**
   - File: `app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserScreen.kt:102-130`
   - Surface/Box-wrapped Add-to-list Button in `Scaffold.bottomBar` now stacks above the persistent nav. `WindowInsets.navigationBars` padding inside the Surface is fine.
   - UX call-out: WebView content area loses ~64-72 dp to the persistent nav. The user explicitly asked for the nav visible on Stores per constraints, so do NOT preemptively narrow.

4. **EditItemScreen, EditRegistryScreen, NotificationsScreen, StoreListScreen, SettingsScreen**
   - All use `Scaffold(...) { paddingValues -> ... }` and either lack a `bottomBar` (Settings, Notifications, StoreList) or share patterns with the screens above (EditRegistry inherits from CreateRegistry concern; EditItem mirrors AddItem patterns).
   - Inner-padding propagation via `paddingValues` consumes the outer Scaffold's bottom inset correctly. No `Alignment.BottomCenter` / `fillMaxSize`-without-paddingValues pattern detected. No content-under-nav clipping anticipated.

5. **KDoc cross-reference at `GiftMaisonBottomNav.kt:43`**
   - Confirmed accurate as-is — comment names neither the old nor the new contract specifically, just points to NavVisibility.kt. No edit applied.

## Issues Encountered

None. Single Gradle run was green on first try; full unit-test suite remained green. No build warnings introduced.

## User Setup Required

None — no external service configuration touched.

## Outstanding for Task 2 (`checkpoint:human-verify`)

**Build and install the debug APK on a physical device or emulator:**

```
cd /Users/victorpop/ai-projects/gift-registry
./gradlew :app:installDebug
```

Sign in with an existing account (or create one). Walk through these 8 scenarios — Scenario 1 is the user-reported bug:

| # | Scenario | Steps | Expected | Pass / Fail |
|---|----------|-------|----------|------------|
| 1 | **SETTINGS (the reported bug)** | From Home, tap YOU on bottom nav. | Settings opens AND bottom nav remains visible. | _to verify_ |
| 2 | **NOTIFICATIONS** | From Home, tap inbox bell (top-right of registry list). | Notifications screen opens AND bottom nav remains visible. | _to verify_ |
| 3 | **STORES** | From Home, tap STORES on bottom nav. Then tap any store card. | StoreList opens with nav visible; StoreBrowser (WebView) opens with nav visible. The "Add to list" CTA bar sits ABOVE the persistent nav (two stacked bars; CTA on top, nav on bottom). | _to verify_ |
| 4 | **ADD ITEM (form, from Detail)** | From Home, tap any registry to open Detail. Tap "Add an item" entry (or use FAB → Add an item). | AddItem form opens AND bottom nav remains visible. Dual-CTA bar ("Save & add another" / "Save & exit") sits ABOVE the persistent nav. Both CTAs still work and screen pops correctly. | _to verify_ |
| 5 | **CREATE REGISTRY (form, from FAB)** | From Home, tap "+" FAB → "New registry". | CreateRegistry form opens AND bottom nav remains visible. "Continue · add items →" CTA sits ABOVE the persistent nav. | _to verify_ |
| 6 | **AUTH (regression — MUST hide)** | From Settings, tap Sign Out. | Auth screen opens AND bottom nav is HIDDEN. (If onboarding is unseen, lands on Onboarding instead — also HIDDEN.) | _to verify_ |
| 7 | **ONBOARDING (regression — MUST hide)** | Clear app data (Settings → Apps → Gift Registry → Storage → Clear data). Cold-start. | Onboarding carousel opens AND bottom nav is HIDDEN across all 3 slides. | _to verify_ |
| 8 | **RE-RESERVE DEEP LINK (regression — MUST hide during resolve)** | Cold-start a deep link to a re-reserve URL (or simulate by manually pushing ReReserveDeepLink onto the back stack). | Loading spinner (ReReserveDeepLink screen) shows AND bottom nav is HIDDEN. After auto-routing to RegistryDetail, bottom nav appears. | _to verify_ |

**Pass criteria:** scenarios 1-5 show nav, scenarios 6-8 hide nav, AND the dual-CTA bar in AddItem (Scenario 4) + the Continue CTA in CreateRegistry (Scenario 5) + the Add-to-list CTA in StoreBrowser (Scenario 3) all still WORK and SUBMIT correctly without the persistent nav clipping their tap targets.

**Resume signal:** Type "approved" if all 8 scenarios behave as expected, or describe which scenario failed and on which screen the nav misbehaved.

If any of concerns 1-3 above (CTA stacking density) is confirmed as a usability problem during this verification, promote the relevant entry to STATE.md "Pending Todos" as a follow-up quick-task.

## Self-Check: PASSED

- File `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt`: FOUND, contains the new hidden-whitelist body (4 false arms + `else -> true`).
- File `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt`: FOUND, 14 `@Test` methods (4 `assertFalse`, 10 `assertTrue`).
- Commit `a486ca5`: FOUND (`fix(quick-260428-v0q): invert showsBottomNav() to hidden-whitelist ...`).
- `./gradlew :app:testDebugUnitTest`: BUILD SUCCESSFUL, 0 failures.
- `BottomNavVisibilityTest` JUnit XML: 14 testcases, 0 skipped, 0 failures, 0 errors.
- `AppNavigation.kt`, `AppNavKeys.kt`, `GiftMaisonBottomNav.kt`: confirmed UNTOUCHED via `git diff --cached --stat` showing only the two task files staged.

---
*Phase: quick-260428-v0q*
*Completed: 2026-04-28*
