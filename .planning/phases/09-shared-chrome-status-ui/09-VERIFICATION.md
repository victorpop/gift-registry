---
phase: 09-shared-chrome-status-ui
verified: 2026-04-21T14:45:00Z
status: human_needed
score: 8/8 automated must-haves verified
human_verification:
  - test: "Sign in and land on Home — bottom nav visible with 5 slots (Home, Stores, +FAB, Lists, You)"
    expected: "bottom nav visible; 5 equal-weight slots with correct icons and monoCaps labels"
    why_human: "Compose rendering cannot be verified without an emulator or physical device"
  - test: "Tap any registry card to reach RegistryDetail — bottom nav still visible, Lists slot highlighted"
    expected: "bottom nav persists; Lists slot shows accentSoft pill + accent icon/label"
    why_human: "Selected-state visual requires on-device inspection"
  - test: "From RegistryDetail, tap menu → Edit registry"
    expected: "bottom nav HIDDEN (CHROME-01 hides on EditRegistryKey)"
    why_human: "Nav-hide behaviour requires on-device confirmation"
  - test: "From Home, tap FAB → sheet → 'New registry' → CreateRegistry"
    expected: "bottom nav HIDDEN on CreateRegistry screen"
    why_human: "Nav-hide behaviour requires on-device confirmation"
  - test: "Tap Stores slot → StoreList"
    expected: "bottom nav HIDDEN (intentional Phase 9 change — CHROME-01 hides on StoreListKey)"
    why_human: "Nav-hide behaviour requires on-device confirmation"
  - test: "Tap 'You' slot → Settings"
    expected: "bottom nav HIDDEN (CHROME-01 hides on SettingsKey)"
    why_human: "Nav-hide behaviour requires on-device confirmation"
  - test: "Back from hidden-nav screen to Home"
    expected: "bottom nav visible again"
    why_human: "Back-stack restoration requires on-device inspection"
  - test: "FAB inspection on Home screen (CHROME-02)"
    expected: "54 dp accent-filled circle, ~4 dp paper ring visible, accent-tinted shadow, lifted ~22 dp above bar baseline, centred between Stores and Lists, mono-caps ADD caption below at bar baseline"
    why_human: "Visual measurements require on-device/emulator inspection; 4-dp paper ring and shadow validated indirectly via quick-260421-moi but full visual requires device"
  - test: "Tap the FAB (CHROME-02)"
    expected: "AddActionSheet slides up with scrim dimming content behind"
    why_human: "Sheet animation requires on-device testing"
  - test: "AddActionSheet layout inspection (CHROME-03)"
    expected: "Top corners rounded 22 dp; drag handle 36x4 dp pill; italic serif title 'What are you adding?'; 4 rows (New registry accentSoft primary, Item from URL / Browse stores / Add manually paperDeep); chevron right on each row"
    why_human: "Layout measurements and visual details require on-device inspection"
  - test: "API 31+ blur behind scrim (CHROME-03)"
    expected: "Background home content subtly blurred behind scrim"
    why_human: "Blur effect requires API 31+ device; Build.VERSION.SDK_INT guard is verified in code but visual effect needs device"
  - test: "Dismiss sheet (swipe down or tap scrim)"
    expected: "Sheet dismisses"
    why_human: "Gesture interaction requires on-device testing"
  - test: "FAB → 'New registry' row navigation"
    expected: "Sheet dismisses, navigates to CreateRegistry form; validated indirectly via quick-260421-moi (RegistryListScreen FAB removed; CreateRegistry flow now exclusively through this sheet path)"
    why_human: "Navigation flow requires on-device confirmation"
  - test: "FAB → 'Browse stores' row navigation"
    expected: "Navigates to StoreList with isPrimary registry id pre-selected (or null if no registries — graceful no-op)"
    why_human: "Navigation routing with registry context requires on-device verification"
  - test: "Reserved chip on reserved item (STAT-01)"
    expected: "Filled accent pill with pulsing dot at ~1.4 s cadence, countdown like '23m' in accent-ink, 'RESERVED' mono-caps label; wait 60+ s — countdown decrements"
    why_human: "Real-time animation and timer behaviour require on-device testing with a live reserved item"
  - test: "Open chip on available item (STAT-03)"
    expected: "Transparent outlined pill with thin line border + 'OPEN' inkFaint label"
    why_human: "Visual chip rendering requires on-device inspection"
  - test: "Given chip on purchased item (STAT-02)"
    expected: "secondSoft filled pill with '✓ GIVEN' in second-colour text"
    why_human: "Visual chip rendering requires on-device inspection"
  - test: "Purchased row visual treatment (STAT-04)"
    expected: "Whole row at ~55% opacity; row STILL VISIBLE. Note: element-level grayscale/checkmark/strikethrough are Phase 11 work per PurchasedRowModifier KDoc — only row-level alpha ships in Phase 9"
    why_human: "Opacity visual treatment requires on-device inspection"
  - test: "Backstack — FAB → 'New registry' → fill → submit"
    expected: "Lands on RegistryDetail for new registry (NOT a new Home above the old one)"
    why_human: "Back-stack correctness requires on-device navigation testing"
  - test: "Back button from new RegistryDetail"
    expected: "Pops to Home; no leaked screens"
    why_human: "Back-stack integrity requires on-device inspection"
  - test: "RO locale switch"
    expected: "Nav labels 'ACASA / MAGAZINE / ADAUGA / LISTE / TU'; chip labels 'REZERVAT / OFERIT / DISPONIBIL'; sheet title 'Ce adaugi?'"
    why_human: "Locale rendering requires on-device locale switch; string keys verified in code (ACASA/ADAUGA encoded as HTML entity in XML which renders correctly on device)"
---

# Phase 9: Shared Chrome + Status UI Verification Report

**Phase Goal:** The bottom nav / centre FAB / Add-action bottom sheet, and the Reserved/Given/Open/Purchased status treatments are shipped as shared UI the owner screens can assemble from.

**Verified:** 2026-04-21T14:45:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | `GiftMaisonBottomNav` composable exists with 5 slots and `showsBottomNav` predicate | VERIFIED | `NavVisibility.kt` + `GiftMaisonBottomNav.kt` both exist; `showsBottomNav()` returns true only for `HomeKey`/`RegistryDetailKey` |
| 2 | `GiftMaisonFab` renders 54 dp accent circle with correct modifier chain order (fabShadow → border → background) | VERIFIED | `GiftMaisonFab.kt` lines 45–47 confirm exact chain: `.fabShadow(tint=colors.accent)` → `.border(4.dp, colors.paper, CircleShape)` → `.background(colors.accent, CircleShape)` |
| 3 | `AddActionSheet` renders 4 action rows with asymmetric top-22 dp corners and ink scrim | VERIFIED | `AddActionSheet.kt` uses `RoundedCornerShape(topStart=22.dp, topEnd=22.dp)` (not `shapes.radius22`) and `colors.ink.copy(alpha=0.55f)` scrim |
| 4 | `StatusChip` dispatches correctly: AVAILABLE→OpenChip, RESERVED→ReservedChip, PURCHASED→GivenChip | VERIFIED | `StatusChip.kt` `statusChipTypeOf()` function + `StatusChipDispatcherTest` 4/4 GREEN |
| 5 | `PulsingDot` animates with 1400 ms period, alpha 1f↔0.5f, scale 1f↔0.85f | VERIFIED | `PulsingDot.kt` public consts + `PulsingDotTest` 6/6 GREEN |
| 6 | `Modifier.purchasedVisualTreatment()` applies `alpha(0.55f)` at row level | VERIFIED | `PurchasedRowModifier.kt` + `PurchasedRowModifierTest` 3/3 GREEN |
| 7 | `AppNavigation.kt` wired: old 4-tab NavigationBar replaced by `GiftMaisonBottomNav` + `AddActionSheet` | VERIFIED | `AppNavigation.kt` imports and calls `GiftMaisonBottomNav` + `AddActionSheet`; no `NavigationBar {` or `NavigationBarItem(` remaining |
| 8 | `RegistryDetailScreen.kt` uses shared `StatusChip(item.status, item.expiresAt)` — inline `ItemStatusChip` and `ReservationCountdown` block deleted | VERIFIED | `RegistryDetailScreen.kt` line 526 calls `StatusChip`; no `private fun ItemStatusChip` or `ReservationCountdown` references; `ReservationCountdown.kt` file deleted |

**Score:** 8/8 automated truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` | 5-slot nav + showsBottomNav in GiftMaisonBottomNav.kt | VERIFIED | 197 lines; `showsBottomNav` moved to NavVisibility.kt (same package, intentional architectural split documented in KDoc) |
| `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` | `showsBottomNav(Any?): Boolean` predicate | VERIFIED | 21 lines; top-level extension in `com.giftregistry.ui.common.chrome` package — imported correctly by BottomNavVisibilityTest |
| `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt` | 54 dp FAB with paper ring + shadow | VERIFIED | 62 lines; modifier chain order correct (fabShadow → border → background) |
| `app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt` | ModalBottomSheet with 4 action rows | VERIFIED | 192 lines; `@OptIn(ExperimentalMaterial3Api::class)`; asymmetric corners; 4 ActionRow calls; `bottomSheetShadow()` applied |
| `app/src/main/java/com/giftregistry/ui/common/status/PulsingDot.kt` | Infinite-animation dot + 5 public consts | VERIFIED | 83 lines; all 5 consts present (`PULSING_DOT_DEFAULT_PERIOD_MS`, `ALPHA_START/END`, `SCALE_START/END`); `rememberInfiniteTransition` + `RepeatMode.Reverse` + `FastOutSlowInEasing` |
| `app/src/main/java/com/giftregistry/ui/common/status/StatusChip.kt` | Dispatcher + 3 chip composables + computeMinutesLeft | VERIFIED | 151 lines; `StatusChipType` enum + `statusChipTypeOf()` + `computeMinutesLeft()` + 4 `@Composable` functions; `LaunchedEffect(expiresAt) { delay(60_000L) }` countdown; no hardcoded colour literals |
| `app/src/main/java/com/giftregistry/ui/common/status/PurchasedRowModifier.kt` | Row-alpha modifier + public alpha const | VERIFIED | 34 lines; `PURCHASED_ROW_ALPHA = 0.55f`; `Modifier.purchasedVisualTreatment()` delegates to `alpha(PURCHASED_ROW_ALPHA)`; KDoc documents caller responsibility for image/title treatments |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` | Wired with GiftMaisonBottomNav + AddActionSheet | VERIFIED | Imports `GiftMaisonBottomNav`, `AddActionSheet`, `showsBottomNav`; old local `showsBottomNav` predicate deleted; old `NavigationBar {}` deleted; `RegistryListViewModel` injected at nav scope; blur guard `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`; `AddActionSheet` hoisted outside Scaffold |
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` | `StatusChip(item.status, item.expiresAt)` at item row | VERIFIED | Line 72: `import com.giftregistry.ui.common.status.StatusChip`; line 526: `StatusChip(status=item.status, expiresAt=item.expiresAt)`; no `ItemStatusChip`; no `ReservationCountdown`; `SuggestionChip` import retained (still used in `RegistryInfoSection` for occasion chip — correct) |
| `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` | 4 new @Preview composables | VERIFIED | 9 total `@Preview` annotations (+4 vs Phase 8's 5); `GiftMaisonBottomNav` (2 states), `StatusChipsPreview`, `PulsingDotPreview` all present |
| `app/src/main/res/values/strings.xml` | Phase 9 EN string keys | VERIFIED | 24 Phase 9 keys: 5 `status_chip_*` + 6 nav + 13 sheet/error; `RESERVED`, `ADD`, `HOME`, `LISTS`, `YOU`, `What are you adding?` |
| `app/src/main/res/values-ro/strings.xml` | Phase 9 RO string keys | VERIFIED | 24 Phase 9 keys: `REZERVAT`, `OFERIT`, `DISPONIBIL`, `ACASA` (entity-encoded), `ADAUGA` (entity-encoded), `LISTE`, `TU`, `Ce adaugi?` |
| `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` | 14 @Test methods, all GREEN | VERIFIED | 14/14 pass |
| `app/src/test/java/com/giftregistry/ui/common/status/ReservedChipTest.kt` | 6 @Test methods, all GREEN | VERIFIED | 6/6 pass |
| `app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt` | 6 @Test methods, all GREEN | VERIFIED | 6/6 pass |
| `app/src/test/java/com/giftregistry/ui/common/status/StatusChipDispatcherTest.kt` | 4 @Test methods, all GREEN | VERIFIED | 4/4 pass |
| `app/src/test/java/com/giftregistry/ui/common/status/PurchasedRowModifierTest.kt` | 3 @Test methods, all GREEN | VERIFIED | 3/3 pass |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `BottomNavVisibilityTest` | `NavVisibility.kt` showsBottomNav | import `com.giftregistry.ui.common.chrome.showsBottomNav` | VERIFIED | Test imports from the chrome package; `NavVisibility.kt` exports `fun Any?.showsBottomNav(): Boolean` |
| `GiftMaisonBottomNav` FAB slot | `GiftMaisonFab` composable | `GiftMaisonFab(onClick=onClick, modifier=Modifier.offset(y=(-22).dp))` | VERIFIED | Line 184–187 of `GiftMaisonBottomNav.kt`; 22 dp lift applied at slot, not inside FAB |
| `AddActionSheet` | `ModalBottomSheet` | `@OptIn(ExperimentalMaterial3Api::class)` + `RoundedCornerShape(topStart=22.dp, topEnd=22.dp)` | VERIFIED | `AddActionSheet.kt` line 48 OptIn, line 69 shape |
| `AppNavigation.kt` bottomBar | `GiftMaisonBottomNav` + `AddActionSheet` | `import com.giftregistry.ui.common.chrome.*` + direct composable calls | VERIFIED | Lines 28–29 imports; lines 140, 319 call sites |
| `AppNavigation.kt` | `showsBottomNav` predicate | `import com.giftregistry.ui.common.chrome.showsBottomNav` | VERIFIED | Line 29 import; line 128 `currentKey.showsBottomNav()`; old local predicate deleted |
| `RegistryDetailScreen.kt` item row | `StatusChip` | `import com.giftregistry.ui.common.status.StatusChip` + `StatusChip(status=item.status, expiresAt=item.expiresAt)` | VERIFIED | Line 72 import; line 526 call |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `StatusChip` → `ReservedChip` | `minutesLeft` | `computeMinutesLeft(expiresAt)` + `LaunchedEffect(expiresAt) { delay(60_000L) }` | Yes — `expiresAt` comes from `item.expiresAt: Long?` (Firestore epoch millis) | FLOWING |
| `GiftMaisonBottomNav` | `selected` slot | `when(currentKey) { is HomeKey → HOME; is RegistryDetailKey → LISTS }` | Yes — `currentKey = backStack.lastOrNull()` is live nav state | FLOWING |
| `AddActionSheet` | `sheetContextRegistryId` | `when(currentKey) { is RegistryDetailKey → key.registryId; else → primaryRegistryId }` where `primaryRegistryId` comes from `RegistryListViewModel.uiState` (Firestore) | Yes — ViewModel reads from Firestore | FLOWING |
| `AppNavigation` isPrimary resolver | `primaryRegistryId` | `RegistryListUiState.Success.registries.maxByOrNull { it.updatedAt }?.id` | Yes — registries from Firestore via RegistryListViewModel | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All 33 Wave 0/1 unit tests pass | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.*"` | 33 tests, 0 failures (BottomNavVisibilityTest 14, PulsingDotTest 6, PurchasedRowModifierTest 3, ReservedChipTest 6, StatusChipDispatcherTest 4) | PASS |
| `compileDebugKotlin` clean | `./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL | PASS |
| `ReservationCountdown.kt` deleted | `test ! -f app/src/main/java/com/giftregistry/ui/registry/detail/ReservationCountdown.kt` | File absent (confirmed by `ls` of directory) | PASS |
| No orphan `ReservationCountdown` references | `grep -rn "ReservationCountdown" app/src/main --include="*.kt"` | 0 matches | PASS |
| `NavigationBar {` removed from AppNavigation | grep for `NavigationBar {` | 0 matches | PASS |
| Old local `showsBottomNav` removed | grep for `private fun Any?.showsBottomNav` | 0 matches | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|------------|-------------|-------------|--------|---------|
| CHROME-01 | 09-01, 09-03, 09-04 | Bottom nav 5 slots, showsBottomNav predicate, visible only on Home+RegistryDetail | SATISFIED | `NavVisibility.kt` predicate; `BottomNavVisibilityTest` 14/14 GREEN; `GiftMaisonBottomNav` wired in `AppNavigation.kt` |
| CHROME-02 | 09-03, 09-04 | 54 dp FAB, paper ring, accent shadow, 22 dp lift, ADD caption | SATISFIED (code); human UAT pending | `GiftMaisonFab.kt`: 54.dp size, `fabShadow(accent)`, `border(4.dp, paper)`, `background(accent)`; `FabSlot` applies `offset(y=(-22).dp)` lift + `nav_fab_add` caption |
| CHROME-03 | 09-03, 09-04 | Add-action sheet, 22 dp top corners, scrim, 4 action rows | SATISFIED (code); human UAT pending | `AddActionSheet.kt`: `RoundedCornerShape(topStart=22.dp, topEnd=22.dp)`, `ink.copy(alpha=0.55f)` scrim, 4 `ActionRow()` calls in correct order; hoisted above Scaffold so scrim covers nav bar |
| STAT-01 | 09-01, 09-02 | Reserved chip: accent fill, pulsing dot 1.4 s, countdown per minute | SATISFIED (code); human UAT pending | `ReservedChip` + `PulsingDot` (1400 ms) + `computeMinutesLeft` + `LaunchedEffect(60_000L delay)`; all test classes GREEN |
| STAT-02 | 09-01, 09-02 | Given chip: secondSoft fill, "✓ GIVEN" label | SATISFIED (code); human UAT pending | `GivenChip` in `StatusChip.kt`; `domain PURCHASED → StatusChipType.GIVEN` mapping pinned by `StatusChipDispatcherTest` |
| STAT-03 | 09-01, 09-02 | Open chip: outlined pill, inkFaint text | SATISFIED (code); human UAT pending | `OpenChip` in `StatusChip.kt`; `domain AVAILABLE → StatusChipType.OPEN` |
| STAT-04 | 09-01, 09-02 | Purchased row 55% opacity, row remains visible | SATISFIED (code); Phase 11 for element-level treatments; human UAT pending | `PurchasedRowModifier.kt`: `PURCHASED_ROW_ALPHA = 0.55f`; `Modifier.purchasedVisualTreatment()` applies `alpha(0.55f)`; KDoc documents Phase 11 caller responsibility for image grayscale + title strikethrough |

All 7 requirements are accounted for. No orphaned requirements detected.

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `AppNavigation.kt` line 160 | `backStack.add(RegistryDetailKey(registryId = primaryRegistryId))` — `primaryRegistryId` is `String?` but `RegistryDetailKey.registryId` is `String`; smart cast through `hasRegistries != null` check | Info | No runtime risk — `hasRegistries = primaryRegistryId != null` guard is correct logically, but the Kotlin type system does not narrow `String?` to `String` across the `else if (hasRegistries)` branch without a smart cast. Compiles successfully, indicating either Kotlin infers the narrow or there is an implicit `!!` in the IR. This is a minor code-smell worth a follow-up cleanup (`primaryRegistryId?.let { backStack.add(RegistryDetailKey(it)) }`) but does not block Phase 9 goal |
| `RegistryDetailScreen.kt` lines 45–46 | `SuggestionChip` + `SuggestionChipDefaults` imports retained | Info | Not a stub anti-pattern — these imports are still used in `RegistryInfoSection` (occasion chip at line 416). Plan 04's acceptance criteria said to delete them only if unused, and they are used. Correct |
| `AddActionSheet.kt` | Uses `Icons.Outlined.KeyboardArrowRight` instead of `Icons.AutoMirrored.Filled.ChevronRight` as specified in PLAN | Info | Minor icon variant deviation. Both are Material Icons chevron-right variants. Visual appearance is essentially identical; no functional impact. |

No blocker anti-patterns found.

### Human Verification Required

The 21-item on-device UAT has been deferred by the user to unblock Phase 10/11. The full checklist is in `/Users/victorpop/ai-projects/gift-registry/.planning/phases/09-shared-chrome-status-ui/09-HUMAN-UAT.md`.

**Follow-up context:** Two quick tasks have already shipped user-approved fixes that transitively validate some UAT items:
- `quick-260421-moi`: Nav slot weighting fixed to equal `Modifier.weight(1f)` on all 5 slots; duplicate FAB on RegistryListScreen removed. Transitively validates UAT item 8 (FAB layout) and item 13 (New registry flow exclusively via AddActionSheet).
- `quick-260421-lwi`: Typography em→sp correction in Phase 8 font sizing. Transitively validates text rendering quality across all new UI.

UAT items 8, 13, and 18 are marked `partial` in `09-HUMAN-UAT.md` as indirectly validated.

**Summary of 21 UAT items by category:**
1. Nav visibility (items 1–7): on-device nav show/hide on each of the 13 key types
2. FAB inspection (item 8): 54 dp, paper ring, shadow, lift, caption
3. FAB tap + sheet (items 9–14): slide-up, layout, blur, dismiss, navigation routing
4. Status chips (items 15–17): Reserved pulsing+countdown, Open outlined, Given secondSoft
5. Purchased row (item 18): 55% opacity (element-level Phase 11)
6. Backstack (items 19–20): no leaked screens
7. Locale (item 21): RO string rendering

### Gaps Summary

No automated gaps. All 8 observable truths verified. All 7 requirements satisfied in code. The only pending items are on-device visual checks that require a human with an emulator or physical device (the 21-check UAT listed above).

The `RegistryDetailKey(registryId = primaryRegistryId)` null-safety smell at line 160 of `AppNavigation.kt` compiles without error and is protected by the `hasRegistries` guard at runtime. It is a candidate for a clean-up task but not a gap.

---

_Verified: 2026-04-21T14:45:00Z_
_Verifier: Claude (gsd-verifier)_
