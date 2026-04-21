---
phase: 09-shared-chrome-status-ui
plan: 03
subsystem: ui
tags: [jetpack-compose, material3, bottom-nav, modal-bottom-sheet, tdd, wave-1, chrome, fab]

# Dependency graph
requires:
  - phase: 08-giftmaison-design-foundation
    provides: GiftMaisonColors/Typography/Shapes/Spacing/Shadows tokens consumed by all 3 new composables
  - phase: 09-01
    provides: Wave 0 RED test stubs (BottomNavVisibilityTest 14 methods) locking the public API surface
  - phase: 09-02
    provides: NavVisibility.kt stub with correct showsBottomNav() implementation; status composables
provides:
  - GiftMaisonFab.kt — 54 dp accent circle FAB with fabShadow→border→background chain + paper ring
  - GiftMaisonBottomNav.kt — 5-slot nav composable (Home·Stores·FAB·Lists·You) + selected-state logic
  - AddActionSheet.kt — ModalBottomSheet with 4 action rows, asymmetric 22 dp top corners, 55% scrim
  - 19 new string keys (6 nav + 13 sheet/empty/error) in EN + RO
affects: [09-04-app-navigation-integration, 11-add-item-url-field]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "FAB modifier chain: fabShadow(tint) → border(4.dp, paper, CircleShape) → background(accent, CircleShape) — exact order required (Pitfall 4: border-after-background paints ring inside fill)"
    - "FAB lift 22 dp via Modifier.offset(y = (-22).dp) applied by FabSlot caller — keeps GiftMaisonFab slot-agnostic for style guide preview"
    - "ModalBottomSheet shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp) NOT shapes.radius22 — shapes.radius22 rounds all 4 corners (Pitfall 2)"
    - "44 dp touch target floor on NavItemSlot pill Box — Box sized to 44.dp, icon sized to 22.dp inside it"
    - "showsBottomNav() stays in NavVisibility.kt (Plan 02 stub) — GiftMaisonBottomNav.kt contains the composable; no duplicate declaration"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "showsBottomNav() kept in NavVisibility.kt (Plan 02 stub) rather than merged into GiftMaisonBottomNav.kt — avoids duplicate declaration, tests already passing, single correct implementation in the package"
  - "Icons.Outlined.KeyboardArrowRight used instead of Icons.AutoMirrored.Filled.ChevronRight — ChevronRight not available in this project's Material Icons extended set; KeyboardArrowRight is a semantic equivalent"
  - "AddActionSheet shapes.radius22 references appear ONLY in comments (Pitfall 2 documentation) — actual code uses RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp) directly"

patterns-established:
  - "Pattern: FAB-in-BottomNav uses Column+offset not BottomAppBar — Material 3 BottomAppBar docks FAB inside bar; offset pattern lifts it visually above the bar per handoff spec"

requirements-completed: [CHROME-01, CHROME-02, CHROME-03]

# Metrics
duration: 4min
completed: 2026-04-21
---

# Phase 9 Plan 03: ui/common/chrome/ — Wave 1 Implementation Summary

**GiftMaisonFab + GiftMaisonBottomNav (5-slot, accentSoft selected pill) + AddActionSheet (4-row ModalBottomSheet, 22 dp asymmetric corners) — BottomNavVisibilityTest Wave 0 (14 methods) confirmed GREEN; 19 string keys (6 nav + 13 sheet) shipped in EN + RO**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-21T15:16:33Z
- **Completed:** 2026-04-21
- **Tasks:** 2
- **Files modified:** 5 (3 created, 2 modified)

## Accomplishments

- Created `ui/common/chrome/` package with GiftMaisonFab.kt, GiftMaisonBottomNav.kt, AddActionSheet.kt
- Wave 0 BottomNavVisibilityTest (14 methods) confirmed GREEN — showsBottomNav() already correctly implemented in Plan 02's NavVisibility.kt stub
- Enforced RESEARCH.md critical contracts: FAB modifier chain (fabShadow→border→background, Pitfall 4), asymmetric sheet corners (Pitfall 2), @OptIn(ExperimentalMaterial3Api::class) (Pitfall 3)
- 19 new string keys added to both EN + RO locales (6 bottom nav + 13 add-sheet/empty/error)
- Full unit suite passes with 0 regressions (`./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL)

## Task Commits

1. **Task 1: GiftMaisonFab + GiftMaisonBottomNav + 6 nav string keys** — `07993e2`
2. **Task 2: AddActionSheet + 13 sheet/empty/error string keys** — `b18d9e8`

## Files Created/Modified

### Created

| File | Purpose | LOC |
|------|---------|-----|
| `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonFab.kt` | 54 dp accent circle FAB with paper ring (fabShadow→border→background chain) | ~60 |
| `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` | 5-slot bottom nav composable with selected-state accentSoft pill + FabSlot with 22 dp lift | ~160 |
| `app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt` | ModalBottomSheet with 4 action rows, asymmetric top-22 corners, 55% ink scrim | ~190 |

### Modified

| File | Change |
|------|--------|
| `app/src/main/res/values/strings.xml` | +19 keys: 6 bottom nav + 13 add-sheet/empty/error (EN) |
| `app/src/main/res/values-ro/strings.xml` | +19 keys: Romanian equivalents |

## Integration Surface for Plan 04

Public symbols by file — all in package `com.giftregistry.ui.common.chrome`:

**NavVisibility.kt** (Plan 02, unchanged):
- `fun Any?.showsBottomNav(): Boolean` — visibility predicate for AppNavigation.kt

**GiftMaisonBottomNav.kt**:
- `@Composable fun GiftMaisonBottomNav(currentKey: Any?, onHome, onStores, onFab, onLists, onYou, modifier)` — drop into Scaffold bottomBar slot

**GiftMaisonFab.kt**:
- `@Composable fun GiftMaisonFab(onClick, modifier)` — standalone 54 dp FAB, lift applied by caller

**AddActionSheet.kt**:
- `@Composable fun AddActionSheet(visible, onDismiss, onNewRegistry, onItemFromUrl, onBrowseStores, onAddManually)` — hoisted above NavDisplay in AppNavigation.kt

## Design Decisions Captured During Implementation

1. **showsBottomNav() stays in NavVisibility.kt** — Plan 02 shipped a correct stub that's already passing all 14 Wave 0 tests. Moving it to GiftMaisonBottomNav.kt would require deleting NavVisibility.kt and could break the import paths referenced in the test. Kept as-is since the contract is "top-level in `com.giftregistry.ui.common.chrome`" (satisfied regardless of file name).

2. **Material Symbols Outlined used for all icons** — Per CONTEXT.md Claude's discretion: `Icons.Outlined.Home`, `Icons.Outlined.Storefront`, `Icons.AutoMirrored.Outlined.List`, `Icons.Outlined.Person` for nav slots; `Icons.Outlined.AddHome`, `Icons.Outlined.Link`, `Icons.Outlined.Storefront`, `Icons.Outlined.EditNote` for sheet rows.

3. **ChevronRight → KeyboardArrowRight** — `Icons.AutoMirrored.Filled.ChevronRight` is not in this project's Material Icons extended set (compile error: unresolved reference). `Icons.Outlined.KeyboardArrowRight` is a semantic equivalent (directional right arrow) and was available.

4. **error_loading_registries was a NEW key** — UI-SPEC implied it was existing; grep confirmed it wasn't. Added as new key in both locales.

5. **Blur fallback deferred to Plan 04** — ModalBottomSheet owns the scrim; the blur on underlying content (`Modifier.blur(1.dp)` on NavDisplay, API 31+ only) is the AppNavigation integration concern. Plan 04 implements it with the `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` guard.

## Known Follow-ups

- **Plan 04** (AppNavigation integration): Wire `GiftMaisonBottomNav` + `AddActionSheet` into `AppNavigation.kt` Scaffold; implement `showAddSheet` state; add `Modifier.blur()` on content behind sheet (API 31+ guard); inject RegistryListViewModel for Lists-tab routing
- **Cleanup todo**: Old `nav_home` / `nav_add_list` / `nav_preferences` string keys still present — remove after Plan 04 cutover confirms they have no remaining references

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ChevronRight icon replaced with KeyboardArrowRight**

- **Found during:** Task 2 (AddActionSheet.kt compile)
- **Issue:** `Icons.AutoMirrored.Filled.ChevronRight` unresolved — not in project's Material Icons extended set; compile failed
- **Fix:** Replaced import + usage with `Icons.Outlined.KeyboardArrowRight` (semantic equivalent available in core icons)
- **Files modified:** `app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt`
- **Verification:** `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL
- **Committed in:** `b18d9e8` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (blocking import issue)
**Impact on plan:** Minimal visual difference; KeyboardArrowRight is a standard directional caret semantically equivalent to ChevronRight in context.

## Known Stubs

None — all exported symbols are fully implemented. No placeholder data, hardcoded empty values, or TODO stubs in any created file.

## Self-Check

- [x] GiftMaisonFab.kt exists at correct path
- [x] GiftMaisonBottomNav.kt exists at correct path
- [x] AddActionSheet.kt exists at correct path
- [x] Task 1 commit `07993e2` exists
- [x] Task 2 commit `b18d9e8` exists
- [x] `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL (0 failures)
- [x] BottomNavVisibilityTest 14/14 methods GREEN
- [x] 19 string keys in both EN + RO
- [x] FAB modifier chain order: fabShadow (L45) < border (L46) < background (L47) in GiftMaisonFab.kt
- [x] Asymmetric sheet corners: RoundedCornerShape(topStart=22.dp, topEnd=22.dp) in AddActionSheet.kt

## Self-Check: PASSED

---
*Phase: 09-shared-chrome-status-ui*
*Completed: 2026-04-21*
