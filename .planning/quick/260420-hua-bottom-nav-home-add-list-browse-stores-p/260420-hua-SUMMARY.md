---
phase: quick/260420-hua-bottom-nav
plan: 01
subsystem: android-ui-navigation
tags: [navigation, material3, bottom-nav, fab, registry-list, bugfix]
requires:
  - "Navigation3 1.0.1 backStack"
  - "Compose Material3 NavigationBar + NavigationBarItem"
provides:
  - "Global Material 3 bottom navigation bar (4 tabs) on top-level destinations"
  - "Direct-create FAB on RegistryListScreen (no expand menu)"
affects:
  - "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
  - "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt"
  - "app/src/main/res/values/strings.xml"
  - "app/src/main/res/values-ro/strings.xml"
tech-stack:
  added: []
  patterns:
    - "isTopLevelDestination() helper gates Scaffold.bottomBar visibility on nav-key class match"
    - "Tab click = backStack.clear() + backStack.add(TargetKey) — no-op if already on tab root"
    - "NavDisplay wrapped in Scaffold { bottomBar = ... } with Modifier.padding(innerPadding)"
key-files:
  created: []
  modified:
    - "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
    - "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt"
    - "app/src/main/res/values/strings.xml"
    - "app/src/main/res/values-ro/strings.xml"
decisions:
  - "Reused existing R.string.stores_browse_label for Browse-stores tab — no duplicate key"
  - "NavigationBarItem default alwaysShowLabel=true retained (labels always visible + supply a11y text per M3 spec)"
  - "Icon contentDescription=null on NavigationBarItem icons — visible text label is the accessibility source of truth"
  - "Orphaned strings stores_fab_label + stores_create_registry_label left in place — out of scope for this bug fix (no code references left)"
metrics:
  duration: ~6min
  completed: 2026-04-20
---

# Quick Task 260420-hua: Bottom Nav + Simplified FAB Summary

**One-liner:** Replaced the buggy expand-menu FAB on RegistryListScreen with a global Material 3 bottom navigation bar (Home / Add list / Browse stores / Preferences) and made the FAB a direct-create shortcut, eliminating the "tap FAB jumps to existing list" bug caused by AnimatedVisibility menu items overlapping registry cards.

## Requirements Satisfied

- **NAV-BOTTOM-01** — Persistent Material 3 bottom navigation on top-level destinations (HomeKey / CreateRegistryKey / StoreListKey / SettingsKey).
- **NAV-BOTTOM-02** — Bottom bar hidden on deeper destinations (RegistryDetail, AddItem, EditItem, EditRegistry, StoreBrowser, ReReserveDeepLink, OnboardingKey, AuthKey) and during AuthUiState.Loading.

## Tasks Executed

| Task | Name                                               | Commit  | Files                                                                                 |
| ---- | -------------------------------------------------- | ------- | ------------------------------------------------------------------------------------- |
| 1    | Wire M3 bottom nav in AppNavigation + nav strings  | 63c608e | AppNavigation.kt, values/strings.xml, values-ro/strings.xml                           |
| 2    | Strip expand-menu FAB + Settings action            | 8d72726 | RegistryListScreen.kt                                                                 |

## Implementation Notes

### AppNavigation.kt

- New imports: `Icons.Default.{Home, Add, Settings, ShoppingBag}`, `NavigationBar`, `NavigationBarItem`, `Scaffold`, `Icon`, `Text`, `stringResource`, `padding`, `R`.
- New file-private helper `Any?.isTopLevelDestination()` — `true` when current key is `HomeKey`, `CreateRegistryKey`, `StoreListKey`, or `SettingsKey`.
- The Loading early-return at line 104 is untouched; the Scaffold wrapping only affects the authenticated/unauthenticated rendering path below it.
- Selection highlighting compares `backStack.lastOrNull()` via `is`-checks. Any `StoreListKey` instance (with or without `preSelectedRegistryId`) highlights the Browse-stores tab — matches intended UX.
- Tab `onClick` is a no-op if the current key is already that tab's class — prevents spurious back-stack resets on accidental double-taps.
- `NavDisplay` receives `modifier = Modifier.padding(innerPadding)` so bottom-bar height is reserved even for inner screens that compose their own Scaffold (`CreateRegistryScreen`, `SettingsScreen`, `StoreListScreen`).

### RegistryListScreen.kt

- Composable signature trimmed from 5 parameters to 3 (dropped `onNavigateToSettings` and `onNavigateToBrowseStores`).
- `TopAppBar` now has no `actions` block — Preferences tab in bottom nav replaces the Settings gear.
- `floatingActionButton` replaced with a plain `FloatingActionButton(onClick = onNavigateToCreate) { Icon(Icons.Default.Add, contentDescription = R.string.registry_create_button) }`. Zero `AnimatedVisibility`, `menuExpanded`, `ExtendedFloatingActionButton`, `FabMenuRow`, `FilledTonalButton`, or `Icons.Default.Close` references remain.
- Private `FabMenuRow` composable deleted (no remaining callers).
- Dead imports pruned: `AnimatedContent`, `AnimatedVisibility`, `expandVertically`, `fadeIn`, `fadeOut`, `shrinkVertically`, `clickable`, `MutableInteractionSource`, `width`, `Close`, `Settings`, `ShoppingBag`, `ExtendedFloatingActionButton`, `FilledTonalButton`.

### Strings

- Added to `values/strings.xml`:
  - `nav_home` = "Home"
  - `nav_add_list` = "Add list"
  - `nav_preferences` = "Preferences"
- Added to `values-ro/strings.xml` (using XML numeric entities per existing file convention):
  - `nav_home` = "Acas&#259;"
  - `nav_add_list` = "Adaug&#259; list&#259;"
  - `nav_preferences` = "Preferin&#539;e"
- Reused existing `stores_browse_label` for the Browse-stores tab — no new key created.

## Deviations from Plan

None — plan executed exactly as written.

The Task 1 `./gradlew :app:assembleDebug` done criterion is only met once Task 2 lands (HomeKey entry drops two args that Task 2 removes from the composable signature). This is a known plan ordering coupling; the final build after Task 2 succeeds cleanly. Both tasks were committed atomically, with the final state of the working tree verifying green build.

## Known Stubs / Out-of-Scope

- `stores_fab_label` ("New" / "Nou") and `stores_create_registry_label` ("Create registry" / "Creează o listă") are now orphaned in `strings.xml` (en + ro) — no code references remain. Not removed per scope boundary (this plan's objective is the bottom-nav migration, not string cleanup). Safe to remove in a future cleanup pass.

## Verification

- **Build:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL in 13s, no new warnings attributable to this change.
- **Code audit (grep):**
  - `onNavigateToSettings` / `onNavigateToBrowseStores` in production code → zero references (except `RegistryDetailScreen.kt`'s unrelated `onNavigateToBrowseStores` param which has its own independent dispatch at AppNavigation:215).
  - `FabMenuRow`, `ExtendedFloatingActionButton`, `FilledTonalButton`, `Icons.Default.Close`, `AnimatedVisibility` in `RegistryListScreen.kt` → zero references.
  - `NavigationBar` in `AppNavigation.kt` → 1 instance, wrapped in `if (showBottomBar) { ... }`.
  - `NavigationBarItem` in `AppNavigation.kt` → exactly 4 instances.
- **Manual (owner follow-up):** Launch debug APK; verify:
  1. Home shows bottom nav with 4 labeled tabs (Home highlighted); FAB tap goes directly to Create Registry.
  2. Tab clicks route correctly and clear the back stack (each tab becomes the new root).
  3. Bottom nav disappears on RegistryDetail, Add/Edit Item, Store Browser WebView, Auth, Onboarding.
  4. Romanian locale shows nav labels as "Acasă / Adaugă listă / Răsfoiește magazine / Preferințe".

## Self-Check: PASSED

- Files modified exist on disk:
  - `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — FOUND
  - `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` — FOUND
  - `app/src/main/res/values/strings.xml` — FOUND
  - `app/src/main/res/values-ro/strings.xml` — FOUND
- Commits exist in git log:
  - `63c608e` — FOUND (Task 1)
  - `8d72726` — FOUND (Task 2)
