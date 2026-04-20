---
phase: quick-260420-iro
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
autonomous: true
requirements: [QUICK-260420-IRO]

must_haves:
  truths:
    - "Bottom nav is visible on Home, Create, Edit, Detail, AddItem, EditItem, StoreList, StoreBrowser, Settings."
    - "Bottom nav is hidden on AuthKey, OnboardingKey, and ReReserveDeepLink (these are the only 3 exceptions)."
    - "On deep screens (RegistryDetail, AddItem, EditItem, EditRegistry, StoreBrowser), NO NavigationBarItem is marked selected."
    - "RegistryDetail FAB navigates directly to the Add Item screen on a single tap — no intermediate menu."
    - "The 'Browse stores' menu row is removed from RegistryDetail (Browse stores remains reachable via the bottom nav)."
    - "`./gradlew :app:assembleDebug` succeeds after the changes."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      provides: "Inverted bottom-nav visibility gate (`showsBottomNav()` replacing `isTopLevelDestination()` for the Scaffold guard)"
      contains: "showsBottomNav"
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      provides: "Plain FloatingActionButton that calls onNavigateToAddItem directly — no DropdownMenu wrapping the FAB"
      contains: "FloatingActionButton(onClick = onNavigateToAddItem)"
  key_links:
    - from: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      to: "Scaffold.bottomBar visibility decision"
      via: "showsBottomNav() gate on currentKey"
      pattern: "showsBottomNav\\(\\)"
    - from: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      to: "RegistryDetailScreen call site"
      via: "onNavigateToBrowseStores parameter removal"
      pattern: "RegistryDetailScreen\\("
    - from: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      to: "Add Item screen navigation"
      via: "FAB onClick → onNavigateToAddItem()"
      pattern: "FloatingActionButton\\(onClick = onNavigateToAddItem"
---

<objective>
Make the bottom navigation bar persistent across (almost) all screens in the app, and simplify the RegistryDetail FAB so it navigates directly to "Add Item" (no expand menu, no touch-through bug risk).

Purpose: After quick task 260420-hua added the bottom nav, it only shows on 4 top-level destinations. The user wants it visible on every feature screen for consistent navigation. Separately, the RegistryDetail FAB still uses the expand-menu pattern that was removed from RegistryListScreen in 260420-hua (touch-through bug risk + redundancy — Browse stores is now a bottom-nav tab).

Output:
- `AppNavigation.kt`: `isTopLevelDestination()` helper replaced with `showsBottomNav()` (inverted sense — return `true` for all keys except `AuthKey`, `OnboardingKey`, `ReReserveDeepLink`). `RegistryDetailScreen` call site updated to drop `onNavigateToBrowseStores` argument.
- `RegistryDetailScreen.kt`: Expand-menu FAB replaced with a plain `FloatingActionButton` that calls `onNavigateToAddItem()`. Unused `onNavigateToBrowseStores` parameter removed.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md

@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt

<interfaces>
<!-- Nav keys that exist today (AppNavKeys.kt) — used by the visibility gate. -->
<!-- The 3 keys that MUST hide the bottom nav: AuthKey, OnboardingKey, ReReserveDeepLink. -->
<!-- All other keys MUST show the bottom nav. -->

From app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt:
```kotlin
@Serializable data object AuthKey
@Serializable data object OnboardingKey
@Serializable data object HomeKey
@Serializable data object SettingsKey
@Serializable data object CreateRegistryKey
@Serializable data class RegistryDetailKey(val registryId: String)
@Serializable data class AddItemKey(
    val registryId: String,
    val initialUrl: String? = null,
    val initialRegistryId: String? = null,
)
@Serializable data class EditItemKey(val registryId: String, val itemId: String)
@Serializable data class EditRegistryKey(val registryId: String)
@Serializable data class ReReserveDeepLink(val reservationId: String)
@Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)
```

Today's helper in AppNavigation.kt (to be replaced):
```kotlin
private fun Any?.isTopLevelDestination(): Boolean =
    this is HomeKey || this is CreateRegistryKey || this is StoreListKey || this is SettingsKey
```

Today's Scaffold body wires `innerPadding` into the NavDisplay modifier:
```kotlin
NavDisplay(
    ...,
    modifier = Modifier.padding(innerPadding),
    ...
)
```
This already prevents double-padding because the NavDisplay body (each screen's inner Scaffold) receives already-inset bounds. The inner Scaffolds' own `paddingValues` then apply on top of those bounds for top/horizontal insets, which is correct. No layout contract change is required — just keep the existing `Modifier.padding(innerPadding)` line unchanged.

Today's RegistryDetail FAB (to be replaced):
```kotlin
floatingActionButton = {
    var addMenuExpanded by remember { mutableStateOf(false) }
    Box {
        FloatingActionButton(onClick = { addMenuExpanded = true }) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.item_add_title))
        }
        DropdownMenu(
            expanded = addMenuExpanded,
            onDismissRequest = { addMenuExpanded = false },
        ) {
            DropdownMenuItem(... onClick = { addMenuExpanded = false; onNavigateToAddItem() })
            DropdownMenuItem(... onClick = { addMenuExpanded = false; onNavigateToBrowseStores() })
        }
    }
}
```

Today's RegistryDetailScreen signature:
```kotlin
fun RegistryDetailScreen(
    registryId: String,
    onBack: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToEditItem: (String) -> Unit,
    onNavigateToEditRegistry: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateToBrowseStores: () -> Unit = {},   // <-- REMOVE this parameter
    onNavigateToRegistry: (String) -> Unit = {},
    viewModel: ...
)
```

No tests reference `onNavigateToBrowseStores` or `isTopLevelDestination` (verified via grep — only source files reference them).
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Invert bottom-nav gate + simplify RegistryDetail FAB</name>
  <files>
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  </files>
  <action>
Make two coordinated edits. Preserve the existing `NavigationBar` + `NavigationBarItem` implementation and the existing tab-selection semantics — do NOT rewrite them.

**Edit A — `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`:**

1. Replace the helper at lines 54-55:
   ```kotlin
   private fun Any?.isTopLevelDestination(): Boolean =
       this is HomeKey || this is CreateRegistryKey || this is StoreListKey || this is SettingsKey
   ```
   with an inverted-sense helper:
   ```kotlin
   // Bottom nav is hidden only where it would break UX:
   //   - AuthKey/OnboardingKey: pre-auth flows, no destinations available
   //   - ReReserveDeepLink: full-screen CircularProgressIndicator resolver
   // All other keys SHOW the bottom nav (persistent across the app).
   private fun Any?.showsBottomNav(): Boolean =
       this !is AuthKey && this !is OnboardingKey && this !is ReReserveDeepLink
   ```

2. Update the call site at line 115:
   ```kotlin
   val showBottomBar = currentKey.isTopLevelDestination()
   ```
   to:
   ```kotlin
   val showBottomBar = currentKey.showsBottomNav()
   ```

3. Leave the `NavigationBarItem.selected = currentKey is HomeKey` (and the 3 other `is CreateRegistryKey` / `is StoreListKey` / `is SettingsKey` checks) UNCHANGED. This is correct: on deep screens (RegistryDetail, AddItem, etc.) none of the 4 `is` checks match, so `selected = false` on every item — the bottom nav correctly shows no tab as selected on deep screens. This is the intended behavior per constraints.

4. Leave the tab-click stack handler UNCHANGED (`backStack.clear(); backStack.add(TargetKey)`). On a deep screen, tapping Home/Create/Stores/Preferences clears and pushes the target — acceptable per user's explicit request.

5. Leave the NavDisplay `modifier = Modifier.padding(innerPadding)` line UNCHANGED. Inner Scaffolds on Create/Edit/Detail/AddItem/EditItem/StoreBrowser/Settings screens already render inside the outer Scaffold's `innerPadding`; nested Scaffolds are supported in Material 3 and the current wiring prevents double-padding of the bottom inset. Do not add `WindowInsets(0.dp)` or remove any inset handling.

6. Remove the `onNavigateToBrowseStores = { backStack.add(StoreListKey(preSelectedRegistryId = key.registryId)) },` argument at line 215 (inside `entry<RegistryDetailKey>`), since the parameter is being dropped from `RegistryDetailScreen`. Also remove the unused `com.giftregistry.ui.store.list.StoreListScreen` import? — NO: `StoreListScreen` is still used by `entry<StoreListKey>`. Only the single `onNavigateToBrowseStores = ...` line goes.

**Edit B — `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt`:**

1. Remove the `onNavigateToBrowseStores: () -> Unit = {},` parameter from the `RegistryDetailScreen` signature (around line 86). Keep every other parameter (including `onNavigateToRegistry`) unchanged.

2. Replace the `floatingActionButton = { ... }` block (lines 255-280 in the current file) with a plain FAB:
   ```kotlin
   floatingActionButton = {
       FloatingActionButton(onClick = onNavigateToAddItem) {
           Icon(
               imageVector = Icons.Default.Add,
               contentDescription = stringResource(R.string.item_add_title),
           )
       }
   },
   ```
   This removes:
   - the surrounding `Box { ... }`
   - the `var addMenuExpanded by remember { mutableStateOf(false) }` state
   - the `DropdownMenu` block with both `DropdownMenuItem`s (Add Item + Browse stores)
   No intermediate expand menu; single-tap goes straight to Add Item. This matches the simplified RegistryListScreen FAB pattern from 260420-hua and eliminates the touch-through bug risk.

3. Clean up now-unused imports in `RegistryDetailScreen.kt`. Specifically remove these imports IF AND ONLY IF they have no other use in the file after Edit B.2 (the top bar's overflow menu at lines 197-250 still uses `DropdownMenu`, `DropdownMenuItem`, `Icons.Default.Edit`, `Icons.Default.Delete`, `Icons.Default.PersonAdd`, `Icons.Default.Share`, so those imports STAY):
   - `androidx.compose.material.icons.filled.ShoppingBag` — used only by the removed Browse-stores menu row. Grep the file after edits; if no remaining reference, delete the import.
   No other imports become unused.

4. Do NOT touch the ViewModel, the snackbar/reservation flow, the ItemCard composable, any string resources, or the top-bar overflow menu. Out of scope.

5. Leave `onNavigateToRegistry: (String) -> Unit = {}` in the signature — it's still used by the FCM push Snackbar handler at line 164. Only `onNavigateToBrowseStores` is removed.

**Order of edits is flexible — both files can be edited in either order; they compile together. No intermediate commit between them.**

String cleanup: `R.string.stores_browse_label` is still referenced by the bottom-nav label in AppNavigation.kt, so the string stays. No string resource changes.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>
Additional automated checks (grep-based — fast, run after assembleDebug succeeds):
- `grep -n "isTopLevelDestination" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → no matches
- `grep -n "showsBottomNav" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → 2 matches (declaration + call site)
- `grep -n "onNavigateToBrowseStores" app/src/main/java/com/giftregistry/ui/` → no matches (both source files clean)
- `grep -n "addMenuExpanded" app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` → no matches
- `grep -n "FloatingActionButton(onClick = onNavigateToAddItem)" app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` → 1 match
  </verify>
  <done>
- `./gradlew :app:assembleDebug` exits 0.
- `isTopLevelDestination` is fully removed; `showsBottomNav` returns `true` for every key except `AuthKey`, `OnboardingKey`, `ReReserveDeepLink`.
- `RegistryDetailScreen` exposes a plain FAB whose `onClick` is `onNavigateToAddItem` — no DropdownMenu wraps the FAB.
- `onNavigateToBrowseStores` is gone from both the Screen signature and the AppNavigation call site.
- Imports in `RegistryDetailScreen.kt` that were only used by the removed Browse-stores row (notably `ShoppingBag`) are deleted; all other imports and top-bar behavior are untouched.
  </done>
</task>

</tasks>

<verification>
Manual spot-check after install (optional, not gating):
- Launch the app, sign in (or continue from signed-in state). Bottom nav visible on Home.
- Tap a registry → RegistryDetail opens. Bottom nav STILL visible, no tab highlighted as selected.
- Tap the `+` FAB on RegistryDetail → goes directly to AddItem screen (no menu). Bottom nav visible on AddItem.
- Back out to Home. Tap "Add list" (Create) → CreateRegistry opens with bottom nav visible.
- Tap "Browse stores" tab from RegistryDetail → jumps to StoreList (backstack cleared). Expected per constraints.
- Sign out. Onboarding or Auth screen shows WITHOUT bottom nav.
- Trigger a re-reserve deep link (optional; if infra available) → CircularProgressIndicator screen WITHOUT bottom nav.
</verification>

<success_criteria>
- `./gradlew :app:assembleDebug` succeeds.
- Bottom nav shows on 9 screens (Home, Create, Edit, Detail, AddItem, EditItem, StoreList, StoreBrowser, Settings) and hides on exactly 3 (Auth, Onboarding, ReReserveDeepLink).
- RegistryDetail FAB is a single-tap shortcut to Add Item — no DropdownMenu, no `Browse stores` menu row.
- No test failures introduced (no tests reference the removed symbols; verified via grep).
</success_criteria>

<output>
After completion, create `.planning/quick/260420-iro-persistent-bottom-nav-across-all-screens/260420-iro-SUMMARY.md` recording:
- Exact diff summary (lines changed per file)
- Confirmation that `isTopLevelDestination` and `onNavigateToBrowseStores` are fully removed
- Confirmation that `./gradlew :app:assembleDebug` passed
- Any imports removed from `RegistryDetailScreen.kt`
</output>
