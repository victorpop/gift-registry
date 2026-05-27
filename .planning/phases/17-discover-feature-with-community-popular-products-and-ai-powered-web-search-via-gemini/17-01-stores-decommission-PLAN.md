---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/store/list/StoreListScreen.kt
  - app/src/main/java/com/giftregistry/ui/store/list/StoreListUiState.kt
  - app/src/main/java/com/giftregistry/ui/store/list/StoreListViewModel.kt
  - app/src/main/java/com/giftregistry/ui/store/list/StoreLogoResolver.kt
  - app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserScreen.kt
  - app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserViewModel.kt
  - app/src/main/java/com/giftregistry/domain/store/StoreRepository.kt
  - app/src/main/java/com/giftregistry/data/store/StoreRepositoryImpl.kt
  - app/src/main/java/com/giftregistry/di/StoresModule.kt
  - app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt
  - app/src/test/java/com/giftregistry/data/store/StoreRepositoryImplTest.kt
  - app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt
  - app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt
  - app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt
  - app/src/main/res/drawable-nodpi/store_emag.webp
  - app/src/main/res/drawable-nodpi/store_altex.webp
  - app/src/main/res/drawable-nodpi/store_flanco.webp
  - app/src/main/res/drawable-nodpi/store_libris.webp
  - app/src/main/res/drawable-nodpi/store_carturesti.webp
  - app/src/main/res/drawable-nodpi/store_ikea.webp
  - app/src/main/res/drawable-nodpi/store_dedeman.webp
  - app/src/main/res/drawable-nodpi/store_elefant.webp
  - app/src/main/res/drawable-nodpi/store_generic.webp
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
  - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
  - functions/scripts/seedStores.ts
  - functions/data/stores.seed.json
  - functions/package.json
  - firestore.rules
  - tests/rules/firestore.rules.test.ts
autonomous: true
requirements:
  - D-03
  - D-04
  - D-05
  - D-06
  - D-07
  - D-08
  - D-09
  - D-44

must_haves:
  truths:
    - "App compiles after Stores capability is removed (no broken imports, no missing R.string references)"
    - "Bottom nav slot 2 has no live click handler pointing to Stores (cleared in this plan, rewired in 17-05)"
    - "Firestore rules no longer include `match /config/{configId}` and tests no longer reference config/stores"
    - "Stores Cloud Functions seed assets are gone (script + JSON + npm script)"
    - "nav_stores_tab string deliberately RETAINED until 17-05 swaps the GiftMaisonBottomNav reference to nav_discover_tab"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/store/list/StoreListScreen.kt"
      provides: "DELETED — file must not exist"
    - path: "app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserScreen.kt"
      provides: "DELETED — file must not exist"
    - path: "app/src/main/java/com/giftregistry/di/StoresModule.kt"
      provides: "DELETED — file must not exist"
    - path: "app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt"
      provides: "DELETED — file must not exist"
    - path: "functions/scripts/seedStores.ts"
      provides: "DELETED — file must not exist"
    - path: "functions/data/stores.seed.json"
      provides: "DELETED — file must not exist"
    - path: "firestore.rules"
      provides: "Updated rules without config/{configId} block"
      contains: "match /databases/{database}/documents"
    - path: "app/src/main/res/values/strings.xml"
      provides: "stores_*, add_item_tab_browse, add_sheet_browse_stores keys removed; nav_stores_tab RETAINED for 17-05 swap"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "stores_*, add_item_tab_browse, add_sheet_browse_stores keys removed (Romanian); nav_stores_tab RETAINED"
  key_links:
    - from: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      to: "StoreListKey / StoreBrowserKey entry blocks"
      via: "Removal — `entry<StoreListKey>` and `entry<StoreBrowserKey>` deleted; `onStores` callback emptied or pointed to TODO"
      pattern: "no `import com.giftregistry.ui.store` lines remain"
    - from: "app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt"
      to: "AddItemMode tab strip"
      via: "Collapse 3-tab to 2-tab: PasteUrl + Manual only; remove BrowseStores branch + onNavigateToBrowseStores param"
      pattern: "no `AddItemMode.BrowseStores` references"
---

<objective>
Fully decommission the Phase 7 Stores capability before Discover replaces nav slot 2 in plan 17-05. Removes all Stores-related Android code, drawables, string resources (en + ro — EXCEPT `nav_stores_tab`, retained until 17-05), Firebase Cloud Functions seed script, the `config/{configId}` Firestore security rule (and its tests), and the `BrowseStores` tab + `onNavigateToBrowseStores` route from AddItemScreen — the deeper Stores entanglement that survived Phase 11.

Purpose: Clear runway for Discover (D-01..D-09 of CONTEXT.md). No rollback path planned — this is irreversible per user instruction in CONTEXT.md `<deferred>` ("Phase 7 Stores capability — fully decommissioned per user's instruction; no rollback path planned").

**Critical ordering note:** `nav_stores_tab` and the `GiftMaisonBottomNav.kt` `Icons.Outlined.Storefront` + `NavSlotId.STORES` + `onStores` references are DELIBERATELY left untouched in this plan. They will be renamed in lock-step in plan 17-05 (rename nav_stores_tab → nav_discover_tab, swap NavSlotId.STORES → NavSlotId.DISCOVER, etc.). Removing the string here without the simultaneous Compose rename would break compilation.

Output: A buildable Android app with no Stores references in code, drawables, content strings, or nav graph; a Firestore rules file without the `config/{configId}` rule; a `functions/` tree without seedStores.ts; a strings.xml without `add_item_tab_browse` or content-level `stores_*` keys (but `nav_stores_tab` retained). Bottom nav slot 2 will be temporarily inert (`onStores` callback emptied; placeholder no-op until plan 17-05 wires Discover) — this plan does NOT add Discover, only removes Stores.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md
@CLAUDE.md
</context>

<interfaces>
<!-- Files to be deleted contain no exports the rest of the app should still use. -->
<!-- Files to be EDITED keep their existing signatures except for the noted removals. -->

From app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (current):
```kotlin
@Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)  // DELETE
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)  // DELETE
```

From app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt (current — DO NOT modify in this plan):
```kotlin
fun GiftMaisonBottomNav(
    currentKey: Any?,
    onHome: () -> Unit,
    onStores: () -> Unit,   // KEEP — 17-05 renames to onDiscover
    onFab: () -> Unit,
    onLists: () -> Unit,
    onYou: () -> Unit,
    modifier: Modifier = Modifier,
)
```

From app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt (current):
```kotlin
enum class AddItemMode { PasteUrl, BrowseStores, Manual }  // becomes: { PasteUrl, Manual }
const val ADD_ITEM_MODE_DEFAULT_ORDINAL: Int = 0  // KEEP
```

From app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt (current signature):
```kotlin
fun AddItemScreen(
    registryId: String?,
    fromAddSheet: Boolean = false,
    initialUrl: String? = null,
    initialRegistryId: String? = null,
    onBack: () -> Unit,
    onNavigateToBrowseStores: (String) -> Unit = {},   // DELETE this param
    onNavigateToCreateRegistry: () -> Unit = {},
)
```
</interfaces>

<tasks>

<task type="auto">
  <name>Task 1: Delete Stores Android code, drawables, tests, and seed script</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-03, D-05, D-08)
    - app/src/main/java/com/giftregistry/di/StoresModule.kt (verify what it binds — confirms only Store-related bindings live here before deletion)
    - app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt (verify only-used-by-store-browser claim from D-03)
    - app/src/main/res/drawable-nodpi/ (list store_*.webp files)
    - app/src/test/java/com/giftregistry/data/store/StoreRepositoryImplTest.kt
    - app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt
    - functions/scripts/seedStores.ts
    - functions/package.json (find the "seed:stores" npm script line)
  </read_first>

  <files>
    app/src/main/java/com/giftregistry/ui/store/list/ (entire directory),
    app/src/main/java/com/giftregistry/ui/store/browser/ (entire directory),
    app/src/main/java/com/giftregistry/domain/store/ (entire directory),
    app/src/main/java/com/giftregistry/data/store/ (entire directory),
    app/src/main/java/com/giftregistry/di/StoresModule.kt,
    app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt,
    app/src/test/java/com/giftregistry/ui/store/ (entire directory),
    app/src/test/java/com/giftregistry/data/store/ (entire directory),
    app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt,
    app/src/main/res/drawable-nodpi/store_emag.webp,
    app/src/main/res/drawable-nodpi/store_altex.webp,
    app/src/main/res/drawable-nodpi/store_flanco.webp,
    app/src/main/res/drawable-nodpi/store_libris.webp,
    app/src/main/res/drawable-nodpi/store_carturesti.webp,
    app/src/main/res/drawable-nodpi/store_ikea.webp,
    app/src/main/res/drawable-nodpi/store_dedeman.webp,
    app/src/main/res/drawable-nodpi/store_elefant.webp,
    app/src/main/res/drawable-nodpi/store_generic.webp,
    functions/scripts/seedStores.ts,
    functions/data/stores.seed.json,
    functions/package.json
  </files>

  <action>
    Execute the following deletions via `rm`/`rm -rf` shell commands (or filesystem equivalents). Per CONTEXT.md D-03, D-05, D-08:

    **Android code deletions (D-03 verbatim list):**
    1. `rm -rf app/src/main/java/com/giftregistry/ui/store/` — removes both `list/` (StoreListScreen.kt, StoreListUiState.kt, StoreListViewModel.kt, StoreLogoResolver.kt) and `browser/` (StoreBrowserScreen.kt, StoreBrowserViewModel.kt).
    2. `rm -rf app/src/main/java/com/giftregistry/domain/store/` — removes StoreRepository.kt.
    3. `rm -rf app/src/main/java/com/giftregistry/data/store/` — removes StoreRepositoryImpl.kt.
    4. `rm app/src/main/java/com/giftregistry/di/StoresModule.kt`.
    5. `rm app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt`. Per D-03, this DataStore is used only by the Store Browser (Phase 7 D-10); verify there are no other importers via grep before deleting, and if any non-store importer exists, STOP and surface the conflict.

    **Android test deletions:**
    6. `rm -rf app/src/test/java/com/giftregistry/ui/store/` — removes StoreListViewModelTest, StoreBrowserViewModelTest, etc.
    7. `rm -rf app/src/test/java/com/giftregistry/data/store/` — removes StoreRepositoryImplTest.kt.
    8. `rm app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt`.

    **Drawable deletions (D-05 verbatim list — 9 files):**
    9. `rm app/src/main/res/drawable-nodpi/store_emag.webp app/src/main/res/drawable-nodpi/store_altex.webp app/src/main/res/drawable-nodpi/store_flanco.webp app/src/main/res/drawable-nodpi/store_libris.webp app/src/main/res/drawable-nodpi/store_carturesti.webp app/src/main/res/drawable-nodpi/store_ikea.webp app/src/main/res/drawable-nodpi/store_dedeman.webp app/src/main/res/drawable-nodpi/store_elefant.webp app/src/main/res/drawable-nodpi/store_generic.webp`.

    **Cloud Functions seed deletions (D-08):**
    10. `rm functions/scripts/seedStores.ts`.
    11. `rm functions/data/stores.seed.json`.
    12. Edit `functions/package.json`: remove the line `"seed:stores": "ts-node scripts/seedStores.ts"` (and the preceding comma if it leaves a dangling trailing comma — keep the JSON valid).

    Do NOT modify navigation or strings or AddItemScreen yet — that's Task 2 + Task 3. After this task: `./gradlew app:compileDebugKotlin` will FAIL due to dangling references; that is expected and gets fixed in Task 2. Run `git status` at the end and confirm only files listed above show D (deleted) or M (modified for functions/package.json).
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      test ! -d app/src/main/java/com/giftregistry/ui/store
      test ! -d app/src/main/java/com/giftregistry/domain/store
      test ! -d app/src/main/java/com/giftregistry/data/store
      test ! -f app/src/main/java/com/giftregistry/di/StoresModule.kt
      test ! -f app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt
      test ! -d app/src/test/java/com/giftregistry/ui/store
      test ! -d app/src/test/java/com/giftregistry/data/store
      test ! -f app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt
      for f in emag altex flanco libris carturesti ikea dedeman elefant generic; do
        test ! -f "app/src/main/res/drawable-nodpi/store_$f.webp"
      done
      test ! -f functions/scripts/seedStores.ts
      test ! -f functions/data/stores.seed.json
      ! grep -q "seed:stores" functions/package.json
      echo OK
      '
    </automated>
  </verify>

  <done>
    All 9 store_*.webp drawables deleted; all Stores Android source directories deleted; LastRegistryPreferencesDataStore deleted; StoresModule deleted; functions/scripts/seedStores.ts + functions/data/stores.seed.json deleted; functions/package.json no longer has the seed:stores npm script.
  </done>
</task>

<task type="auto">
  <name>Task 2: Rewire nav graph + collapse AddItemScreen tabs (heal compilation)</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-04, D-09, plus Claude's discretion note on AddItemScreen 3-tab control)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (current — line 27-28 have StoreListKey + StoreBrowserKey)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (current — has store imports at line 51-52, onStores callback at 160-165, two entry blocks at 265-294, onNavigateToBrowseStores wire at 303-307)
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt (current — has KDoc reference to StoreListKey/StoreBrowserKey at line 20)
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt (current — line 38 has NavSlotId.STORES enum value, callback `onStores` at line 55; DO NOT modify yet — 17-05 renames to DISCOVER)
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt (current — enum `{ PasteUrl, BrowseStores, Manual }`)
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt (current — references AddItemMode.BrowseStores at lines 125, 235, 258; onNavigateToBrowseStores param at line 63)
    - app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt (current — asserts 3 entries, has BrowseStores assertions at lines 25, 33, 43)
    - app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt (current — imports StoreBrowserKey + StoreListKey at lines 14-15, asserts showsBottomNav at 53, 55)
  </read_first>

  <files>
    app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
    app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt,
    app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt,
    app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt,
    app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt,
    app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt
  </files>

  <action>
    Heal all references to the deleted Store types so `./gradlew compileDebugKotlin compileDebugUnitTestKotlin` passes. Per CONTEXT.md D-04 (delete StoreListKey + StoreBrowserKey from AppNavKeys + AppNavigation + NavVisibility) and D-09 (remove "Browse stores" row from FAB Add-action sheet — already done in earlier quick task, but the AddItemScreen 3-tab control with `BrowseStores` mode is the surviving Stores entry point and must also collapse).

    **1. `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt`** — delete these two lines (current lines 27-28):
    ```kotlin
    @Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)
    @Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)
    ```
    Keep every other key in the file unchanged.

    **2. `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`**:
       - Delete imports at lines 51-52: `import com.giftregistry.ui.store.browser.StoreBrowserScreen` and `import com.giftregistry.ui.store.list.StoreListScreen`.
       - Replace the entire `onStores = { … }` callback body (current lines 160-165) with a temporary no-op so the GiftMaisonBottomNav callback contract still type-checks (plan 17-05 will rename it to `onDiscover` and push DiscoverKey):
         ```kotlin
         onStores = {
             // Plan 17-05 will rewire this to onDiscover -> push DiscoverKey.
             // Temporary no-op so slot 2 is inert during Wave 1.
         },
         ```
       - Delete the entire `entry<StoreListKey> { … }` block (current lines 265-272 inclusive) and the entire `entry<StoreBrowserKey> { … }` block (current lines 274-294 inclusive).
       - In the `entry<AddItemKey> { … }` block (around line 296), delete the `onNavigateToBrowseStores = { regId -> backStack.add(StoreListKey(...)) }` argument (current lines 303-307). The AddItemScreen call should keep `onBack`, `registryId`, `fromAddSheet`, `initialUrl`, `initialRegistryId`, `onNavigateToCreateRegistry`. Verify no other `StoreListKey` / `StoreBrowserKey` references remain in the file.

    **3. `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt`** — in the KDoc at line 20, remove the "StoreListKey, StoreBrowserKey," tokens. The visibility predicate body itself does not reference these keys (it's a hidden-whitelist with only AuthKey, OnboardingKey, ReReserveDeepLink) so only the doc comment needs editing. Final line 20-ish should read:
    ```kotlin
     * NotificationsKey, CreateRegistryKey, EditRegistryKey, AddItemKey, EditItemKey,
     * and any future post-auth key by default).
    ```
    Update the History bullet at line ~25 from "Settings, Notifications, Stores, and all forms keep the nav bar" to "Settings, Notifications, and all forms keep the nav bar" (drop the word "Stores").

    **4. `app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt`** — replace the enum line:
    ```kotlin
    enum class AddItemMode { PasteUrl, BrowseStores, Manual }
    ```
    with:
    ```kotlin
    enum class AddItemMode { PasteUrl, Manual }
    ```
    Keep `ADD_ITEM_MODE_DEFAULT_ORDINAL = 0` and the KDoc, but in the KDoc replace "3-mode segmented control on the Add Item screen (Paste URL / Browse stores / Manual)" with "2-mode segmented control on the Add Item screen (Paste URL / Manual)" and replace "PasteUrl (default), BrowseStores, Manual" with "PasteUrl (default), Manual".

    **5. `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt`**:
       - Remove the `onNavigateToBrowseStores: (String) -> Unit = {},` parameter from the `AddItemScreen` function signature (current line 63).
       - Remove the `if (selectedTab == AddItemMode.BrowseStores) { … onNavigateToBrowseStores(target) … }` LaunchedEffect block (current lines 125-130).
       - Remove the `stringResource(R.string.add_item_tab_browse),` line from the SegmentedTabs labels list (current line 235). The list should be `listOf(stringResource(R.string.add_item_tab_paste_url), stringResource(R.string.add_item_tab_manual))` (or equivalent — read the current call site and remove only the browse entry).
       - Remove the `AddItemMode.BrowseStores -> { … }` branch from the `when (selectedTab)` body (current line 258). Keep only `AddItemMode.PasteUrl -> PasteUrlModeContent(...)` and `AddItemMode.Manual -> ManualModeContent(...)`.

    **6. `app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt`** — rewrite to assert the 2-entry enum:
       - Replace assertion lines that check `entries[1] == AddItemMode.BrowseStores` and `entries[2] == AddItemMode.Manual` with `entries[1] == AddItemMode.Manual` and `entries.size == 2`.
       - Remove the ordinal-1 BrowseStores assertion entirely. Update any ordinal-2 Manual assertion to ordinal-1 Manual.
       - Update KDoc references from "3 modes" to "2 modes" and remove "BrowseStores" mentions.

    **7. `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt`** — delete the imports `import com.giftregistry.ui.navigation.StoreBrowserKey` and `import com.giftregistry.ui.navigation.StoreListKey` (current lines 14-15), and delete the two assertions `assertTrue(StoreListKey(preSelectedRegistryId = null).showsBottomNav())` (line 53) and `assertTrue(StoreBrowserKey(storeId = "s1", registryId = null).showsBottomNav())` (line 55). Keep the rest of the test file unchanged.

    After all edits: `./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin` should succeed (the bottom-nav callback `onStores` still exists on GiftMaisonBottomNav — 17-05 renames it).
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      ! grep -q "StoreListKey\|StoreBrowserKey" app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
      ! grep -q "com.giftregistry.ui.store" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      ! grep -q "StoreListKey\|StoreBrowserKey" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      ! grep -q "onNavigateToBrowseStores" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
      ! grep -q "StoreListKey\|StoreBrowserKey" app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
      ! grep -q "BrowseStores" app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt
      ! grep -q "BrowseStores\|onNavigateToBrowseStores\|add_item_tab_browse" app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
      ! grep -q "BrowseStores" app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt
      ! grep -q "StoreListKey\|StoreBrowserKey" app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt
      ./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q
      echo OK
      '
    </automated>
  </verify>

  <done>
    `./gradlew compileDebugKotlin compileDebugUnitTestKotlin` passes cleanly with zero `Unresolved reference` errors. AddItemMode enum has exactly 2 entries (`PasteUrl`, `Manual`). AddItemScreen has no `onNavigateToBrowseStores` parameter and no `BrowseStores` mode branch. AppNavKeys, AppNavigation, NavVisibility have zero references to StoreListKey/StoreBrowserKey. BottomNavVisibilityTest and AddItemModeTest no longer reference deleted symbols.
  </done>
</task>

<task type="auto">
  <name>Task 3: Remove stores_* content strings, config/{configId} rule + rules tests, and config/stores Firestore doc cleanup script (RETAIN nav_stores_tab)</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-06, D-07, D-42, D-44)
    - app/src/main/res/values/strings.xml (find all `stores_*` keys at lines 155-174, `nav_stores_tab` at 244 — DO NOT DELETE 244, `add_item_tab_browse` at 315; check the comment block at line 252 mentioning add_sheet_browse_stores)
    - app/src/main/res/values-ro/strings.xml (find all matching Romanian keys at lines 156-175, 245 — DO NOT DELETE 245, 312)
    - firestore.rules (find `match /config/{configId}` block at lines 115-118)
    - tests/rules/firestore.rules.test.ts (find `describe("config/stores rules"` block at lines 410-439)
    - app/src/test/java/com/giftregistry/LocalizationParityTest.kt (verify en + ro parity test will pass after key removals)
  </read_first>

  <files>
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml,
    firestore.rules,
    tests/rules/firestore.rules.test.ts,
    functions/scripts/deleteConfigStores.ts
  </files>

  <action>
    Remove Stores-related content string resources (BUT retain `nav_stores_tab`), the `config/{configId}` Firestore security rule + its test block, and add a one-shot cleanup script for the live `config/stores` Firestore doc. Per CONTEXT.md D-06, D-07, D-42, D-44.

    **Critical:** Although D-42 in CONTEXT.md says to delete `nav_stores_tab` in the same commit as Stores decommission, this plan DELIBERATELY retains `nav_stores_tab` because `GiftMaisonBottomNav.kt` still calls `stringResource(R.string.nav_stores_tab)` until plan 17-05 swaps slot 2 to Discover. Plan 17-05 will delete `nav_stores_tab` simultaneously with the GiftMaisonBottomNav rename so the app remains build-clean throughout. This is a documented departure from the strict reading of D-42 in service of build-time safety.

    **1. `app/src/main/res/values/strings.xml`** — delete these string keys (use exact name match; some may share a line; preserve XML validity):
       - `stores_fab_label` (line ~155)
       - `stores_browse_label` (line ~156)
       - `stores_create_registry_label` (line ~157)
       - `stores_list_title` (line ~160)
       - `stores_list_error_heading` (line ~161)
       - `stores_list_error_body` (line ~162)
       - `stores_retry` (line ~163)
       - `stores_logo_content_description` (line ~164)
       - `stores_webview_close` (line ~167)
       - `stores_webview_error_heading` (line ~168)
       - `stores_webview_error_body` (line ~169)
       - `stores_add_to_list_cta` (line ~170)
       - `stores_external_link_blocked` (line ~171)
       - `stores_registry_picker_label` (line ~174)
       - `add_item_tab_browse` (line ~315)

       **DO NOT DELETE:** `nav_stores_tab` (line ~244). Plan 17-05 will rename it to `nav_discover_tab` simultaneously with the GiftMaisonBottomNav slot-2 rename.

       Also delete the legacy comment at line ~252 that mentions `add_sheet_item_url`, `add_sheet_browse_stores` (if the comment references no-longer-existing keys, prune the obsolete tokens from the comment text but keep the rest of the comment).

       Final grep `grep -c "stores_\|add_item_tab_browse\|add_sheet_browse_stores" app/src/main/res/values/strings.xml` MUST return 0 (nav_stores_tab retained, not counted in this grep — note the absence of `nav_stores_tab` in the pattern).

    **2. `app/src/main/res/values-ro/strings.xml`** — same key set as step 1 minus `nav_stores_tab`. Delete exactly these keys: `stores_fab_label`, `stores_browse_label`, `stores_create_registry_label`, `stores_list_title`, `stores_list_error_heading`, `stores_list_error_body`, `stores_retry`, `stores_logo_content_description`, `stores_webview_close`, `stores_webview_error_heading`, `stores_webview_error_body`, `stores_add_to_list_cta`, `stores_external_link_blocked`, `stores_registry_picker_label`, `add_item_tab_browse`. Retain `nav_stores_tab`.

       Final grep `grep -c "stores_\|add_item_tab_browse\|add_sheet_browse_stores" app/src/main/res/values-ro/strings.xml` MUST return 0.

    **3. `firestore.rules`** — delete the entire block:
    ```
        // Phase 7 (STORE-01 / D-21): config collection — world-readable so the
        // Store Browser can load the store list even for unauthenticated
        // onboarding states. No client writes — admin only via seed script
        // (Admin SDK bypasses rules).
        match /config/{configId} {
          allow read: if true;
          allow write: if false;
        }
    ```
    (Approximately lines 110-118 — verify by reading the file first.) The closing `}` of `match /databases/{database}/documents { … }` MUST be preserved. After edit, the file should still start with `rules_version = '2';` and have one fewer `match` block.

    **4. `tests/rules/firestore.rules.test.ts`** — delete the entire `describe("config/stores rules", () => { … })` block (current lines 409-439, including the section header comment `// ─────…` above and the closing `});`). Do not touch any other `describe()` block.

    **5. Create `functions/scripts/deleteConfigStores.ts`** — one-shot Admin SDK script to delete the live `config/stores` Firestore document. Mirror the (now-deleted) `seedStores.ts` pattern (Admin SDK init, `main()`, exit codes). Content:
    ```typescript
    /**
     * Phase 17 D-07 one-shot cleanup: deletes the live config/stores Firestore
     * document left behind by the decommissioned Phase 7 Stores capability.
     *
     * Idempotent — re-running on a missing doc is a no-op (Firestore delete()
     * succeeds on non-existent docs).
     *
     * Run via: `cd functions && npx ts-node scripts/deleteConfigStores.ts`
     * Requires application-default credentials (`gcloud auth application-default
     * login`) when targeting prod; emulator users can run inside
     * `firebase emulators:exec`.
     */
    import * as admin from "firebase-admin";

    if (admin.apps.length === 0) {
      admin.initializeApp();
    }

    async function main(): Promise<void> {
      const ref = admin.firestore().collection("config").doc("stores");
      const snap = await ref.get();
      if (!snap.exists) {
        console.log("config/stores not present — nothing to delete (idempotent no-op).");
        return;
      }
      await ref.delete();
      console.log("Deleted config/stores Firestore document.");
    }

    main()
      .then(() => process.exit(0))
      .catch((err) => {
        console.error("deleteConfigStores failed:", err);
        process.exit(1);
      });
    ```
    This script will be invoked from plan 17-06 during deploy; it is NOT run automatically in this plan. Its existence is the deliverable.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # Content stores_* keys gone; nav_stores_tab RETAINED for plan 17-05 swap
      test "$(grep -c "stores_\|add_item_tab_browse\|add_sheet_browse_stores" app/src/main/res/values/strings.xml)" -eq 0
      test "$(grep -c "stores_\|add_item_tab_browse\|add_sheet_browse_stores" app/src/main/res/values-ro/strings.xml)" -eq 0
      # nav_stores_tab MUST still exist (will be replaced by plan 17-05)
      grep -q "name=\"nav_stores_tab\"" app/src/main/res/values/strings.xml
      grep -q "name=\"nav_stores_tab\"" app/src/main/res/values-ro/strings.xml
      ! grep -q "match /config/{configId}" firestore.rules
      ! grep -q "config/stores rules" tests/rules/firestore.rules.test.ts
      ! grep -q "config.*stores" tests/rules/firestore.rules.test.ts
      test -f functions/scripts/deleteConfigStores.ts
      grep -q "config/stores not present" functions/scripts/deleteConfigStores.ts
      grep -q "admin.firestore().collection(\"config\").doc(\"stores\")" functions/scripts/deleteConfigStores.ts
      # Confirm Android still builds (regression guard for missing-resource references)
      ./gradlew app:compileDebugKotlin -q
      # Confirm rules tests still pass without the config/stores describe block
      cd tests/rules && npm test 2>&1 | tail -20
      echo OK
      '
    </automated>
  </verify>

  <done>
    `grep -c "stores_\|add_item_tab_browse" app/src/main/res/values/strings.xml` returns 0 (content stores keys gone). `nav_stores_tab` retained in both en + ro (will be deleted in plan 17-05 lock-step with GiftMaisonBottomNav rewire). `firestore.rules` has no `match /config/{configId}` block. `tests/rules/firestore.rules.test.ts` has no `describe("config/stores rules")` block. `functions/scripts/deleteConfigStores.ts` exists with idempotent Admin SDK delete logic. Existing rules test suite passes (config/stores cases removed, all other rule tests pass). Android compiles.
  </done>
</task>

</tasks>

<verification>
After all three tasks:

1. `./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin` succeeds (no broken imports, no missing R.string references; `R.string.nav_stores_tab` still resolves).
2. `./gradlew app:testDebugUnitTest --tests com.giftregistry.ui.item.add.AddItemModeTest --tests com.giftregistry.ui.common.chrome.BottomNavVisibilityTest --tests com.giftregistry.LocalizationParityTest` succeeds.
3. `cd tests/rules && npm test` succeeds (rules tests still pass — config/stores describe block removed).
4. `grep -r "StoreListKey\|StoreBrowserKey\|StoreRepository\|StoresModule\|LastRegistryPreferencesDataStore\|seedStores\|BrowseStores" app/src functions/src functions/scripts 2>/dev/null` returns zero matches (case-sensitive, scoped to source dirs only — not node_modules, not .planning/, not tests/, not git history).
5. The Discover stub (plan 17-05) can now safely use slot 2 of GiftMaisonBottomNav and will delete the retained `nav_stores_tab` in lock-step with the slot rename.
</verification>

<success_criteria>
- All 9 store_*.webp drawables removed from `app/src/main/res/drawable-nodpi/`.
- Stores Android packages deleted: `ui/store/`, `domain/store/`, `data/store/`, `di/StoresModule.kt`, `data/preferences/LastRegistryPreferencesDataStore.kt`, and their tests.
- AddItemMode is now `{ PasteUrl, Manual }` (2 entries) and AddItemScreen has no `BrowseStores` branch or `onNavigateToBrowseStores` parameter.
- AppNavKeys.kt, AppNavigation.kt, NavVisibility.kt have zero references to StoreListKey/StoreBrowserKey.
- strings.xml + values-ro/strings.xml have zero `stores_*` or `add_item_tab_browse` keys; `nav_stores_tab` RETAINED for plan 17-05 swap.
- firestore.rules no longer contains `match /config/{configId}`.
- tests/rules/firestore.rules.test.ts no longer contains the `config/stores rules` describe block.
- `functions/scripts/deleteConfigStores.ts` exists (Admin SDK doc deletion script).
- `functions/scripts/seedStores.ts`, `functions/data/stores.seed.json`, and the `seed:stores` npm script are deleted.
- Android builds cleanly; rules tests pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-01-SUMMARY.md` documenting:
- File deletions count (Android source files, drawables, tests, Functions seed script).
- The AddItemScreen tab-collapse decision (3 → 2) and rationale (Stores fully decommissioned).
- The deliberate retention of `nav_stores_tab` (with rationale: deleted in plan 17-05 lock-step with GiftMaisonBottomNav rename).
- Confirmation that `deleteConfigStores.ts` is staged for execution in plan 17-06 (deploy plan).
- Any unexpected reference to `LastRegistryPreferencesDataStore` outside the store browser flow that was discovered (should be none per D-03, but document if so).
</output>
