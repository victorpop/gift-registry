---
phase: quick/260420-hua-bottom-nav
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
autonomous: true
requirements:
  - NAV-BOTTOM-01
  - NAV-BOTTOM-02

must_haves:
  truths:
    - "From the home screen the user sees a Material 3 bottom navigation bar with 4 tabs: Home, Add list, Browse stores, Preferences."
    - "Tapping each tab routes to the corresponding top-level destination (HomeKey / CreateRegistryKey / StoreListKey / SettingsKey) and clears the back stack so the tab becomes the new root."
    - "Exactly one tab is visually selected at a time, matching the current top-level destination via backStack.lastOrNull()::class."
    - "The bottom bar is hidden on all non-top-level destinations (RegistryDetailKey, AddItemKey, EditItemKey, EditRegistryKey, StoreBrowserKey, ReReserveDeepLink, OnboardingKey, AuthKey) and during AuthUiState.Loading."
    - "The FAB on RegistryListScreen is a plain FloatingActionButton that navigates directly to CreateRegistryKey on tap — no expand menu, no backdrop, no animated Close icon; the 'tap FAB jumps to existing list' bug no longer reproduces."
    - "The Settings IconButton is removed from the RegistryListScreen TopAppBar (Preferences tab replaces it)."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      provides: "Scaffold wrapping NavDisplay with conditional bottomBar; NavigationBar + 4 NavigationBarItem composables; isTopLevelDestination() helper; tab-click back-stack reset logic."
      contains: "NavigationBar"
    - path: "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt"
      provides: "Simplified FAB (direct-create) and TopAppBar without Settings action; signature no longer takes onNavigateToSettings or onNavigateToBrowseStores."
      contains: "FloatingActionButton"
    - path: "app/src/main/res/values/strings.xml"
      provides: "English strings nav_home, nav_add_list, nav_preferences."
      contains: "nav_home"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Romanian strings nav_home, nav_add_list, nav_preferences."
      contains: "nav_home"
  key_links:
    - from: "AppNavigation.kt Scaffold.bottomBar"
      to: "backStack.lastOrNull()"
      via: "isTopLevelDestination() guard"
      pattern: "isTopLevelDestination"
    - from: "NavigationBarItem.onClick"
      to: "backStack.clear() + backStack.add(TargetKey)"
      via: "lambda per tab"
      pattern: "backStack\\.clear\\(\\)"
    - from: "RegistryListScreen FAB"
      to: "onNavigateToCreate"
      via: "direct invocation — no menuExpanded state"
      pattern: "FloatingActionButton"
---

<objective>
Replace the buggy expand-menu FAB on RegistryListScreen with a global Material 3 bottom navigation bar (4 tabs: Home, Add list, Browse stores, Preferences) and make the FAB a direct-create shortcut.

Purpose:
- Fix the reproducible bug where tapping an expanded FAB menu item lands on an existing registry detail instead of the intended destination. Root cause: the AnimatedVisibility menu items render outside the Scaffold `floatingActionButton` slot's measured bounds, so taps fall through to the registry card beneath. Replacing the whole construct with a top-level NavigationBar + a direct-create FAB removes the overlap class entirely.
- Align entry-point IA with Home / Twitter / IG patterns where primary navigation is always visible on top-level screens.
- Reclaim the TopAppBar actions slot (remove Settings IconButton — now redundant with the Preferences tab).

Output: Working bottom-nav across HomeKey / CreateRegistryKey / StoreListKey / SettingsKey; hidden on all deeper destinations and during auth loading; simplified FAB; localized nav labels (en + ro).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml

<interfaces>
<!-- Nav keys (from AppNavKeys.kt). All are @Serializable. -->
data object AuthKey
data object OnboardingKey
data object HomeKey
data object SettingsKey
data object CreateRegistryKey
data class RegistryDetailKey(registryId: String)
data class AddItemKey(registryId: String, initialUrl: String? = null, initialRegistryId: String? = null)
data class EditItemKey(registryId: String, itemId: String)
data class EditRegistryKey(registryId: String)
data class ReReserveDeepLink(reservationId: String)
data class StoreListKey(preSelectedRegistryId: String? = null)
data class StoreBrowserKey(storeId: String, registryId: String?)

<!-- Top-level destinations (bottom nav visible only on these four) -->
TOP_LEVEL = { HomeKey, CreateRegistryKey, StoreListKey, SettingsKey }

<!-- Existing FAB code to DELETE from RegistryListScreen.kt lines 111-164 -->
<!-- Replace with plain FloatingActionButton { onNavigateToCreate() } -->

<!-- Existing M3 imports already in project:
  androidx.compose.material3.Scaffold
  androidx.compose.material3.FloatingActionButton (currently ExtendedFloatingActionButton — switch to FloatingActionButton)
  androidx.compose.material.icons.filled.{Home, Add, ShoppingBag, Settings}
-->

<!-- Existing reusable string (do NOT recreate): -->
R.string.stores_browse_label = "Browse stores" / "Răsfoiește magazine"

<!-- New strings to add (in both values/ and values-ro/): -->
R.string.nav_home              = "Home"        / "Acasă"
R.string.nav_add_list          = "Add list"    / "Adaugă listă"
R.string.nav_preferences       = "Preferences" / "Preferințe"
</interfaces>

<constraints>
- Material 3 only: `androidx.compose.material3.NavigationBar` + `NavigationBarItem`. NOT BottomAppBar, NOT a custom Row.
- NavigationBarItem requires: `selected: Boolean`, `onClick`, `icon: @Composable`, `label: @Composable?`. Pass `contentDescription = null` on each Icon — the `label` composable supplies accessibility text per M3 spec.
- Only one tab selected at any time. Compare `backStack.lastOrNull()` via `is`-checks (data object / data class).
- Tab-click semantics: `backStack.clear(); backStack.add(TargetKey)`. Do NOT push-on-top.
- Bottom bar hidden when `!isTopLevelDestination(backStack.lastOrNull())`. Hide during `AuthUiState.Loading` too (the early-return for Loading is untouched; Scaffold only wraps NavDisplay, which runs after the Loading return).
- Preserve onboarding and auth: `OnboardingKey` / `AuthKey` are NOT top-level. Bottom bar is hidden there.
- Nested Scaffold is allowed and expected: `CreateRegistryScreen`, `SettingsScreen`, `StoreListScreen` all have their own inner Scaffold and top bar. The outer bottomBar lives on the root Scaffold in AppNavigation.
- Do not touch ViewModels or domain/data layers.
- Do not add new icon libraries; `Icons.Default.Home/Add/ShoppingBag/Settings` are already available.
</constraints>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Wire Material 3 bottom nav in AppNavigation + add nav strings</name>
  <files>
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml
  </files>
  <action>
Add three new strings to both `strings.xml` files (place them in a new `<!-- Bottom navigation -->` section near the top, after `<!-- Common / shared -->`):

values/strings.xml:
```xml
<!-- Bottom navigation -->
<string name="nav_home">Home</string>
<string name="nav_add_list">Add list</string>
<string name="nav_preferences">Preferences</string>
```

values-ro/strings.xml (Romanian — use XML numeric entities for diacritics to match the file's existing style, e.g. `Acas&#259;`, `Adaug&#259; list&#259;`, `Preferin&#539;e`):
```xml
<!-- Navigare de jos -->
<string name="nav_home">Acas&#259;</string>
<string name="nav_add_list">Adaug&#259; list&#259;</string>
<string name="nav_preferences">Preferin&#539;e</string>
```

DO NOT add a new string for "Browse stores" — reuse the existing `R.string.stores_browse_label` (both locales are already correct).

In `AppNavigation.kt`:

1) Add these imports (merge with existing ones; keep alphabetical order within each group):
```kotlin
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.giftregistry.R
```

2) Add a top-level private helper (outside `AppNavigation`, above or below it — file-private is fine):
```kotlin
private fun Any?.isTopLevelDestination(): Boolean =
    this is HomeKey || this is CreateRegistryKey || this is StoreListKey || this is SettingsKey
```
(Note: `StoreListKey` is a data class with a nullable arg. Using `is StoreListKey` matches any instance — correct. Selection highlighting will treat any StoreListKey in the stack as the Browse-stores tab, which is the desired UX.)

3) Wrap the existing `NavDisplay(...)` call in a `Scaffold`. The Loading early-return at line 88-96 stays as-is; only the `NavDisplay` below it becomes the Scaffold content:

```kotlin
val currentKey = backStack.lastOrNull()
val showBottomBar = currentKey.isTopLevelDestination()

Scaffold(
    bottomBar = {
        if (showBottomBar) {
            NavigationBar {
                NavigationBarItem(
                    selected = currentKey is HomeKey,
                    onClick = {
                        if (currentKey !is HomeKey) {
                            backStack.clear()
                            backStack.add(HomeKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = currentKey is CreateRegistryKey,
                    onClick = {
                        if (currentKey !is CreateRegistryKey) {
                            backStack.clear()
                            backStack.add(CreateRegistryKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_add_list)) },
                )
                NavigationBarItem(
                    selected = currentKey is StoreListKey,
                    onClick = {
                        if (currentKey !is StoreListKey) {
                            backStack.clear()
                            backStack.add(StoreListKey(preSelectedRegistryId = null))
                        }
                    },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                    label = { Text(stringResource(R.string.stores_browse_label)) },
                )
                NavigationBarItem(
                    selected = currentKey is SettingsKey,
                    onClick = {
                        if (currentKey !is SettingsKey) {
                            backStack.clear()
                            backStack.add(SettingsKey)
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_preferences)) },
                )
            }
        }
    }
) { innerPadding ->
    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLast() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        modifier = Modifier.padding(innerPadding),
        entryProvider = entryProvider { /* unchanged entries */ }
    )
}
```

Required additional imports for the above: `androidx.compose.foundation.layout.padding`.

4) Update the `HomeKey` entry (currently lines 107-115): drop the `onNavigateToSettings` and `onNavigateToBrowseStores` arguments (Task 2 removes them from the composable signature). The call becomes:
```kotlin
entry<HomeKey> {
    RegistryListScreen(
        onNavigateToCreate = { backStack.add(CreateRegistryKey) },
        onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
        onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
    )
}
```

Notes / gotchas:
- `currentKey` is read from the snapshot-backed `mutableStateListOf`, so tab selection recomposes correctly on back-stack mutations.
- Do NOT use `NavigationBarItem(alwaysShowLabel = false)`; leave the default (true) so labels always show. M3 spec says visible labels count as the a11y text.
- Do NOT pass `contentDescription` on the Icons (null is correct per M3 guidance when a visible text label exists on the same item).
- The `Modifier.padding(innerPadding)` on NavDisplay is how the bottom-bar height is reserved — inner screens that have their own Scaffold will respect this padding since NavDisplay hosts them.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>
  </verify>
  <done>
    - `./gradlew :app:assembleDebug` succeeds with zero new warnings attributable to this task.
    - `R.string.nav_home`, `R.string.nav_add_list`, `R.string.nav_preferences` exist in both `values/strings.xml` and `values-ro/strings.xml`.
    - `AppNavigation.kt` contains `NavigationBar`, exactly 4 `NavigationBarItem` calls, an `isTopLevelDestination()` helper, and wraps `NavDisplay` in a `Scaffold { bottomBar = ... }`.
    - The `HomeKey` entry no longer passes `onNavigateToSettings` or `onNavigateToBrowseStores`.
  </done>
</task>

<task type="auto">
  <name>Task 2: Strip expand-menu FAB + Settings action from RegistryListScreen</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
  </files>
  <action>
Simplify `RegistryListScreen` to match the new navigation model:

1) Update the composable signature — remove `onNavigateToSettings` and `onNavigateToBrowseStores` (no test files reference them; grep confirmed):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryListScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: RegistryListViewModel = hiltViewModel()
) { ... }
```

2) Remove the `actions = { IconButton(onClick = onNavigateToSettings) { ... } }` block from the `TopAppBar` (lines 101-108). The TopAppBar keeps only its title:
```kotlin
TopAppBar(
    title = { Text(stringResource(R.string.registry_list_title)) }
)
```

3) Replace the entire `floatingActionButton = { ... }` block (lines 111-164 — the custom Box/Column/AnimatedVisibility/ExtendedFloatingActionButton construct) with a plain Material 3 FloatingActionButton that directly invokes `onNavigateToCreate`:
```kotlin
floatingActionButton = {
    FloatingActionButton(onClick = onNavigateToCreate) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.registry_create_button)
        )
    }
}
```

Notes:
- Use `R.string.registry_create_button` ("Create Registry" / "Creeaza registru") as the a11y contentDescription. This is a pre-existing string — do not create a new one.
- The Add icon import already exists (`androidx.compose.material.icons.filled.Add`).

4) Delete the now-unused private `FabMenuRow` composable (currently lines ~365-381) — it has no remaining callers.

5) Remove now-unused imports. These become dead after the changes above:
```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable                   // only used by backdrop Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement         // only if no other usage remains
import androidx.compose.foundation.layout.width               // only used by FabMenuRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
```
KEEP (still used elsewhere in the file): `Icons.Default.Add`, `Icons.Default.Edit`, `Icons.Default.Delete`, `Icons.Default.Lock`, `Icons.Default.Public`, `Icons.Default.MoreVert`, `Arrangement` (used by LazyColumn + RegistryCard Row), `padding`, `height`, `size`, `Spacer`, `Column`, `Row`.

ADD (new usage):
```kotlin
import androidx.compose.material3.FloatingActionButton
```

Before removing each import above, verify no other usage remains in the file (a final read-through or the compiler's "unused import" warning will flag any miss). When in doubt, leave an import — AGP will warn but not fail.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>
  </verify>
  <done>
    - `./gradlew :app:assembleDebug` succeeds.
    - `RegistryListScreen`'s parameter list no longer contains `onNavigateToSettings` or `onNavigateToBrowseStores`.
    - The `TopAppBar` block has no `actions = { ... }` lambda.
    - `floatingActionButton` contains only a `FloatingActionButton { Icon(Icons.Default.Add, ...) }` — zero references to `AnimatedVisibility`, `menuExpanded`, `ExtendedFloatingActionButton`, `FabMenuRow`, `FilledTonalButton`, or `Icons.Default.Close` remain in the file.
    - Private `FabMenuRow` function is deleted.
    - Manual sanity (owner does this after build): launch app, open Home, tap FAB once → lands on Create Registry (not on an existing registry card). Bottom nav shows 4 tabs; Home tab is selected. Tap "Browse stores" tab → lands on Store list, Browse-stores tab selected. Tap "Preferences" tab → Settings screen, Preferences tab selected. Open a registry → bottom nav disappears.
  </done>
</task>

</tasks>

<verification>
Build verification (mandatory, covered by each task's `<automated>`):
- `./gradlew :app:assembleDebug` succeeds after both tasks.

Functional verification (owner, post-build):
1. Launch debug APK on emulator / device.
2. On the registry list (Home):
   - Bottom nav visible with 4 labeled tabs; Home is highlighted.
   - TopAppBar shows only the "My Registries" title — no Settings gear.
   - FAB is a plain circular "+" button; tapping it goes straight to Create Registry (no menu, no backdrop, no flash of overlapping buttons).
3. Tap the "Add list" tab → Create Registry screen opens; Add-list tab highlighted. Press system back → returns to Home (CreateRegistry inner back button also works).
4. Tap the "Browse stores" tab → Store list screen; Browse-stores tab highlighted.
5. Tap "Preferences" tab → Settings screen; Preferences tab highlighted.
6. Open a registry (card tap) → bottom nav disappears.
7. Open a store → WebView screen → bottom nav disappears.
8. Sign out → Auth screen → bottom nav disappears.
9. Fresh install (onboarding path) → onboarding carousel → bottom nav disappears.
10. Switch UI language to Romanian from Preferences → nav labels become "Acasă", "Adaugă listă", "Răsfoiește magazine", "Preferințe".

Regression checks:
- "Tap FAB → jumps into existing list" bug no longer reproduces.
- Deep link to a registry from email still lands on RegistryDetail with correct back behavior.
- Re-reservation deep link still resolves and lands on Home + RegistryDetail; no bottom nav flicker on Detail.
</verification>

<success_criteria>
- Build: `./gradlew :app:assembleDebug` passes.
- Exactly one Material 3 `NavigationBar` in the app, rendered from `AppNavigation.kt`'s root Scaffold, visible only on top-level destinations and hidden during auth loading.
- Four `NavigationBarItem`s with labels "Home", "Add list", "Browse stores", "Preferences" (or the Romanian equivalents), icons Home / Add / ShoppingBag / Settings, and exactly one selected at a time based on `backStack.lastOrNull()` class match.
- Tab clicks clear and re-root the back stack (do not stack).
- `RegistryListScreen` FAB is a direct-create shortcut — no expand-menu state in the file.
- Settings gear IconButton is removed from `RegistryListScreen` TopAppBar.
- New strings `nav_home`, `nav_add_list`, `nav_preferences` exist in both `values/strings.xml` and `values-ro/strings.xml`; `stores_browse_label` is reused (not duplicated).
</success_criteria>

<output>
After completion, no SUMMARY file is required (this is a `/gsd:quick` task). The quick-task directory at `.planning/quick/260420-hua-bottom-nav-home-add-list-browse-stores-p/` will contain this PLAN.md plus any execution artifacts the quick orchestrator drops.
</output>
