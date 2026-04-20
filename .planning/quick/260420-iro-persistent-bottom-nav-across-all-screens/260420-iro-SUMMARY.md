---
phase: quick-260420-iro
plan: 01
subsystem: navigation
tags: [navigation, bottom-nav, fab, registry-detail, ux]
requires: []
provides:
  - showsBottomNav (inverted bottom-nav visibility gate)
  - Persistent NavigationBar on all feature screens
  - Plain RegistryDetail FAB (single-tap to Add Item)
affects:
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
tech-stack:
  added: []
  patterns:
    - Inverted-sense visibility gate — small hide-list instead of enumerated show-list
    - Direct-navigation FAB (no DropdownMenu wrapping) — eliminates touch-through bug risk
key-files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
decisions:
  - "isTopLevelDestination helper replaced with showsBottomNav (inverted sense) — enumerates the 3 exceptions rather than the show-list; new feature screens automatically inherit the bottom nav without changing the gate"
  - "RegistryDetail FAB flattened to a single-tap FloatingActionButton → onNavigateToAddItem; Browse stores row removed (now reachable via bottom-nav tab)"
  - "Tab-click behavior on deep screens preserved unchanged: clicking Home/Create/Stores/Preferences clears the back stack and pushes the target (acceptable per explicit user constraint)"
  - "NavigationBarItem.selected checks left unchanged — on deep screens (RegistryDetail, AddItem, etc.) none of the 4 keys match, so no tab is highlighted, which is the desired UX"
metrics:
  duration: "1min"
  tasks: 1
  files: 2
  completed: 2026-04-20
---

# Quick Task 260420-iro: Persistent Bottom Nav Across All Screens Summary

Bottom navigation now persists across every feature screen except the 3 pre-auth / deep-link-resolver exceptions, and the RegistryDetail FAB is a single-tap shortcut straight to Add Item — matching the simplified RegistryListScreen FAB pattern from quick task 260420-hua.

## What Was Built

### Persistent bottom navigation

`AppNavigation.kt` previously gated the `NavigationBar` with an enumerated top-level destination check (`HomeKey`, `CreateRegistryKey`, `StoreListKey`, `SettingsKey`). The gate is now inverted — `showsBottomNav()` returns `true` for every nav key except the 3 screens where a bottom nav would break the UX:

- `AuthKey` — pre-auth, no destinations yet available
- `OnboardingKey` — pre-auth carousel
- `ReReserveDeepLink` — full-screen CircularProgressIndicator resolver

All 9 other destinations (Home, CreateRegistry, EditRegistry, RegistryDetail, AddItem, EditItem, StoreList, StoreBrowser, Settings) now show the bottom nav. On deep screens no tab is highlighted (none of the 4 `selected = currentKey is …` checks match), which is the intended "persistent but unhighlighted" state.

### Simplified RegistryDetail FAB

Replaced the `Box { FloatingActionButton { … } + DropdownMenu { Add Item, Browse stores } }` expand-menu pattern with a plain `FloatingActionButton(onClick = onNavigateToAddItem)`. Single-tap goes directly to Add Item — no intermediate menu, no touch-through bug risk. `Browse stores` remains reachable via the bottom-nav Stores tab.

## Exact Diff Summary

| File | Lines added | Lines removed | Net |
|------|-------------|---------------|-----|
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` | +6 | −3 | +3 |
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` | +6 | −26 | −20 |
| **Total** | **+12** | **−29** | **−17** |

### `AppNavigation.kt` changes
1. Replaced the `isTopLevelDestination()` extension (2 lines, line 54-55) with a commented-up `showsBottomNav()` extension (6 lines) that returns `true` for every key except `AuthKey`, `OnboardingKey`, `ReReserveDeepLink`.
2. Updated the call site `val showBottomBar = currentKey.isTopLevelDestination()` → `currentKey.showsBottomNav()` (line 119).
3. Removed `onNavigateToBrowseStores = { backStack.add(StoreListKey(preSelectedRegistryId = key.registryId)) },` from the `RegistryDetailScreen` call site inside `entry<RegistryDetailKey>`.

### `RegistryDetailScreen.kt` changes
1. Dropped the `onNavigateToBrowseStores: () -> Unit = {},` parameter from the `RegistryDetailScreen` signature. All other parameters (`onNavigateToRegistry`, etc.) preserved.
2. Replaced the 26-line expand-menu FAB block (`Box { FAB + DropdownMenu { Add Item + Browse stores } }` plus `var addMenuExpanded by remember { mutableStateOf(false) }`) with a 6-line plain `FloatingActionButton(onClick = onNavigateToAddItem)`.
3. Deleted the now-unused `import androidx.compose.material.icons.filled.ShoppingBag` (the top-bar overflow menu uses Edit / Delete / PersonAdd / Share — those imports stay).

### Imports removed from `RegistryDetailScreen.kt`
- `androidx.compose.material.icons.filled.ShoppingBag` (was referenced only by the removed Browse-stores menu row)

No other imports became unused. `DropdownMenu`, `DropdownMenuItem`, `Box`, `Icons.Default.Add`, `Icons.Default.Edit`, `Icons.Default.Delete`, `Icons.Default.PersonAdd`, `Icons.Default.Share`, and the `mutableStateOf` / `remember` pair all remain in use by the top-bar overflow menu and per-item menu.

## Confirmation of Full Removal

- `grep -rn "isTopLevelDestination" app/src/main/java/com/giftregistry/ui/` → **0 matches** (fully removed)
- `grep -rn "onNavigateToBrowseStores" app/src/main/java/com/giftregistry/ui/` → **0 matches** (removed from both signature and call site)
- `grep -n "showsBottomNav" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → **2 matches** (declaration + call site)
- `grep -n "addMenuExpanded" app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` → **0 matches**
- `grep -n "FloatingActionButton(onClick = onNavigateToAddItem)" app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` → **1 match** (line 254)

## Build Verification

```
$ ./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 16s
41 actionable tasks: 16 executed, 25 up-to-date
```

Cold build succeeded. Incremental rebuild also clean (`BUILD SUCCESSFUL in 870ms`, `:app:assembleDebug UP-TO-DATE`).

## Screens Affected

### Bottom nav now VISIBLE on (9 screens):
1. Home (RegistryList) — was visible before
2. CreateRegistry — was visible before
3. EditRegistry — **NEW (was hidden)**
4. RegistryDetail — **NEW (was hidden)**
5. AddItem — **NEW (was hidden)**
6. EditItem — **NEW (was hidden)**
7. StoreList — was visible before
8. StoreBrowser — **NEW (was hidden)**
9. Settings — was visible before

### Bottom nav HIDDEN on (3 screens, unchanged from before):
1. Auth (AuthKey) — sign-in flow
2. Onboarding (OnboardingKey) — 3-slide carousel
3. ReReserveDeepLink — full-screen resolver spinner

## Deviations from Plan

None — plan executed exactly as written.

## Commits

| Hash    | Message                                                             |
| ------- | ------------------------------------------------------------------- |
| 3f28013 | feat(quick-260420-iro-01): persistent bottom nav + simplify RegistryDetail FAB |

## Self-Check: PASSED

- FOUND: `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` (modified, contains `showsBottomNav`)
- FOUND: `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` (modified, contains `FloatingActionButton(onClick = onNavigateToAddItem)`)
- FOUND: commit `3f28013` in git log
- Build: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
- All grep verify criteria pass (5/5)
