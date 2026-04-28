---
phase: quick-260428-iny
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
autonomous: false
requirements:
  - QUICK-260428-INY-01  # Sheet shows exactly 2 rows (New registry, Add an item)
  - QUICK-260428-INY-02  # Add Item form gains a mandatory registry picker when entered from sheet
  - QUICK-260428-INY-03  # Picker is hidden on the Create-Registry → AddItem chained path
  - QUICK-260428-INY-04  # Zero-registry empty state in picker links to CreateRegistryKey

must_haves:
  truths:
    - "Tapping the bottom-nav FAB opens an Add sheet with exactly 2 action rows: 'New registry' (primary, accentSoft) and 'Add an item' (secondary, paperDeep)."
    - "Tapping 'Add an item' on the sheet opens AddItemScreen with a registry picker as the FIRST field; both Save CTAs (Add another / Save) are disabled until a registry is picked."
    - "When the picker is open and the user has zero registries, an inline empty-state shows 'Create a registry first' with a tappable affordance that navigates to CreateRegistryKey."
    - "Tapping 'New registry' on the sheet → saving the new registry routes to AddItemScreen with the picker HIDDEN (registry pre-selected to the freshly-created id)."
    - "All sheet copy + new picker labels resolve through strings.xml in EN and values-ro/strings.xml in RO; no hardcoded user-facing strings."
    - "Sheet preserves prior bug fixes: top-only 22dp corners, scrim ink alpha 0.55, paper container, symmetric dragHandle padding (vertical=12dp), no bottomSheetShadow."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt"
      provides: "Trimmed sheet — 2 ActionRows, single onAddItem callback replaces 3 prior callbacks"
      contains: "onAddItem"
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt"
      provides: "AddItemKey extended: registryId nullable + fromAddSheet flag"
      contains: "fromAddSheet"
    - path: "app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt"
      provides: "Conditional registry picker as first field when fromAddSheet=true; submit-disabled gating; empty-state link"
      contains: "ExposedDropdownMenuBox"
    - path: "app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt"
      provides: "Nullable selectedRegistryId state + setRegistry mutator + registries flow for picker"
      contains: "selectedRegistryId"
    - path: "app/src/main/res/values/strings.xml"
      provides: "EN copy: add_sheet_add_item, add_sheet_add_item_sub, add_item_picker_label, add_item_picker_hint, add_item_no_registries_cta"
      contains: "add_sheet_add_item"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "RO copy mirroring EN keys"
      contains: "add_sheet_add_item"
  key_links:
    - from: "app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt"
      to: "AppNavigation onAddItem lambda"
      via: "onAddItem: () -> Unit callback"
      pattern: "onAddItem"
    - from: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      to: "AddItemKey(registryId = null, fromAddSheet = true)"
      via: "backStack.add on sheet's Add-an-item row"
      pattern: "fromAddSheet\\s*=\\s*true"
    - from: "app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt"
      to: "AddItemViewModel.setRegistry(id)"
      via: "ExposedDropdownMenuBox onSelect"
      pattern: "setRegistry"
    - from: "AddItemViewModel"
      to: "ObserveRegistriesUseCase"
      via: "Hilt-injected use case + currentUser.uid"
      pattern: "observeRegistries"
---

<objective>
Trim the bottom-sheet "Add" action sheet from 4 rows to 2 rows: keep "New registry" (primary) and replace the 3 item-creation rows ("Item from URL", "Browse stores", "Add manually") with a single "Add an item" row that routes to AddItemScreen with a mandatory registry picker as the first field.

Purpose: Reduce decision fatigue in the Add sheet — users picked between 4 paths to ultimately land on the same form anyway. Consolidate the 3 item entry points behind one row, with registry choice as an explicit first step (no auto-default) when entering from the FAB. Preserve the Create-Registry → Add-Items chained flow (picker stays hidden there because the registry was just created).

Output: Trimmed AddActionSheet, extended AddItemKey, conditionally-shown registry picker in AddItemScreen, ViewModel refactor accepting nullable registryId, strings.xml additions/cleanup. App compiles, existing unit tests pass, four user-facing flows verified by human checkpoint.

Concern raised (planner): Removing "Item from URL" and "Browse stores" from the SHEET only — both flows MUST still be reachable elsewhere. Confirmed:
  • URL paste — remains the default tab inside AddItemScreen itself (PasteUrl is `ADD_ITEM_MODE_DEFAULT_ORDINAL`, ordinal 0).
  • Browse stores — remains reachable via the bottom-nav STORES tab (`onStores` in AppNavigation pushes `StoreListKey`) AND via the in-form "Browse stores" tab in AddItemScreen (selectedTab == BrowseStores triggers navigation).
  • Add manually — remains reachable via the in-form "Manual" tab.
No flow is orphaned by this change.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md

@app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
@app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml

<interfaces>
<!-- Key contracts the executor needs. Extracted from codebase. -->

From app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (CURRENT):
```kotlin
@Serializable data class AddItemKey(
    val registryId: String,                 // currently REQUIRED — must become nullable
    val initialUrl: String? = null,
    val initialRegistryId: String? = null,
)
@Serializable data object CreateRegistryKey
```

From app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt:
```kotlin
val registryId: String = savedStateHandle["registryId"] ?: ""
val initialUrl: String = savedStateHandle["initialUrl"] ?: ""
val initialRegistryId: String = savedStateHandle["initialRegistryId"] ?: ""
fun onSave() { ... addItem(registryId, item) ... }   // uses registryId field
```

From app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt:
```kotlin
sealed interface RegistryListUiState {
    data object Loading : RegistryListUiState
    data class Success(val registries: List<Registry>) : RegistryListUiState
    data class Error(val message: String) : RegistryListUiState
}
// Uses ObserveRegistriesUseCase(uid) to expose StateFlow<RegistryListUiState>
```

From com.giftregistry.domain.usecase.ObserveRegistriesUseCase (used by RegistryListViewModel):
```kotlin
operator fun invoke(uid: String): Flow<List<Registry>>   // shape inferred from RegistryListViewModel.kt:51-52
```

From com.giftregistry.domain.model.Registry: has `id: String`, `title: String` (used elsewhere via `registry.title` in RegistryListScreen — confirm in source).

AddActionSheet CURRENT signature:
```kotlin
fun AddActionSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onNewRegistry: () -> Unit,
    onItemFromUrl: () -> Unit,        // REMOVE
    onBrowseStores: () -> Unit,       // REMOVE
    onAddManually: () -> Unit,        // REMOVE
)
// → New signature must add: onAddItem: () -> Unit
```

AppNavigation entry<AddItemKey> CURRENT (lines 275-287):
```kotlin
entry<AddItemKey> { key ->
    AddItemScreen(
        registryId = key.registryId,                  // currently String, must accept null
        initialUrl = key.initialUrl,
        initialRegistryId = key.initialRegistryId,
        onBack = { backStack.removeLast() },
        onNavigateToBrowseStores = { regId -> backStack.add(StoreListKey(preSelectedRegistryId = regId)) },
    )
}
```

AppNavigation CreateRegistryKey onSaved (line 204-207) — KEEP UNCHANGED in semantics:
```kotlin
onSaved = { registryId ->
    backStack.removeLast()
    backStack.add(AddItemKey(registryId = registryId))   // → must become AddItemKey(registryId = registryId, fromAddSheet = false) after key change
}
```

Strings to ADD (EN + RO):
- add_sheet_add_item            "Add an item" / "Adaug&#259; un produs"
- add_sheet_add_item_sub        "Add a gift to one of your registries" / "Adaug&#259; un cadou &#238;ntr-una din listele tale"
- add_item_picker_label         "Choose a registry" / "Alege o list&#259;"
- add_item_picker_hint          "Select which registry this gift goes to" / "Selecteaz&#259; lista pentru acest cadou"
- add_item_no_registries_cta    "Create a registry first" / "Creeaz&#259; mai &#238;nt&#226;i o list&#259;"
  (NOTE: `add_sheet_no_registry_hint` already has identical EN copy "Create a registry first" but is consumed nowhere in src/main — executor MUST grep before reusing vs. adding a new key. Prefer reuse to keep the strings file lean. If reusing, do not add `add_item_no_registries_cta`.)

Strings to REMOVE (EN + RO) — verified ZERO consumers in app/src/main/java outside AddActionSheet.kt itself:
- add_sheet_item_url, add_sheet_item_url_sub
- add_sheet_browse_stores, add_sheet_browse_stores_sub
- add_sheet_add_manually, add_sheet_add_manually_sub
(Phase 9 PLAN docs reference these as documentation — that's fine, we're not editing planning docs. No production-code consumers.)

Existing reusable string `add_sheet_no_registry_hint` (EN: "Create a registry first" / RO: "Creeaz&#259; mai &#238;nt&#226;i o list&#259;") — currently UNUSED in production code. Reuse for the empty-state text instead of adding a duplicate.

Existing reusable strings `add_sheet_lists_empty_hint` ("No registries yet") and `add_sheet_lists_empty_cta` ("Create your first registry") — also unused; executor MAY use these together for the inline empty-state pair (a heading + a tappable CTA). Decision deferred to executor — pick the single cleanest copy variant; do not introduce parallel new keys for the same idea.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Trim sheet to 2 rows + extend AddItemKey + reroute sheet's Add-an-item lambda</name>
  <files>
    app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt,
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml
  </files>
  <action>
Goal: sheet renders exactly 2 ActionRows; tapping "Add an item" pushes AddItemKey with registryId=null and fromAddSheet=true; existing CreateRegistry → AddItem chain still pushes AddItemKey with the freshly-created registryId and fromAddSheet=false (default).

1. **Edit `AppNavKeys.kt`** — change AddItemKey:
   ```kotlin
   @Serializable data class AddItemKey(
       val registryId: String? = null,           // was non-null required → now nullable
       val initialUrl: String? = null,
       val initialRegistryId: String? = null,
       val fromAddSheet: Boolean = false,        // NEW — true only from the FAB sheet's Add-an-item row
   )
   ```
   Rationale (vs. introducing AddItemPickerKey): one nav key keeps the entry<AddItemKey> block single-source-of-truth and avoids duplicating the AddItemScreen render call. The `fromAddSheet` flag is a tiny addition; making `registryId` nullable is the only structural change. Existing call sites in StoreBrowserScreen and CreateRegistryScreen onSaved still compile because they pass a concrete registryId; defaults cover the new flag.

2. **Edit `AddActionSheet.kt`** — replace the 3 secondary rows with one:
   - Drop callbacks: `onItemFromUrl`, `onBrowseStores`, `onAddManually` (all 3 parameters).
   - Add callback: `onAddItem: () -> Unit`.
   - Drop the 3 ActionRow calls for Item-from-URL, Browse-stores, Add-manually.
   - Add ONE ActionRow after the New-registry primary row:
     ```kotlin
     ActionRow(
         icon = Icons.Outlined.AddCircleOutline,         // import androidx.compose.material.icons.outlined.AddCircleOutline
         headingRes = R.string.add_sheet_add_item,
         subtitleRes = R.string.add_sheet_add_item_sub,
         isPrimary = false,
         onClick = onAddItem,
     )
     ```
   - Drop now-unused icon imports: `EditNote`, `Link`, `Storefront`. KEEP `AddHome` (primary row), `KeyboardArrowRight` (chevron), and add `AddCircleOutline`.
   - Update the KDoc header: change the row list from "4 action rows" to "2 action rows" and rename items 2-4 to a single item 2 ("Add an item").
   - DO NOT touch: `RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)`, `containerColor = colors.paper`, `scrimColor = colors.ink.copy(alpha = 0.55f)`, the dragHandle Box `padding(vertical = 12.dp)`, the Column padding block — these are the just-shipped fixes that MUST NOT regress.

3. **Edit `AppNavigation.kt`** at the AddActionSheet call (current lines ~330-360):
   - Remove the `onItemFromUrl`, `onBrowseStores`, `onAddManually` lambdas entirely.
   - Add a single `onAddItem` lambda:
     ```kotlin
     onAddItem = {
         showAddSheet = false
         backStack.add(AddItemKey(registryId = null, fromAddSheet = true))
     }
     ```
   - Keep `onNewRegistry` exactly as-is (still pushes `CreateRegistryKey`).
   - The `sheetContextRegistryId` derivedState is no longer consumed by the sheet — DELETE the `val sheetContextRegistryId: String?` declaration (currently lines 132-135) since its only consumers were the three removed lambdas. Verify with grep before deleting; if any other consumer surfaces, leave it.
   - In the `entry<AddItemKey> { key -> ... }` block, AddItemScreen's `registryId` parameter currently expects non-null `String`. After the nav-key change, `key.registryId` is `String?`. We are reshaping `AddItemScreen` in Task 2 to accept `String?`. Update the call site here in Task 1 ONLY if the signature change has compiled in Task 2 — otherwise this task ends with a known-broken `AddItemScreen(registryId = key.registryId, ...)` line. Order Task 2 to land in the same commit/build as Task 1 (executor can do all edits before running `./gradlew compileDebugKotlin`).
   - Pass through new args explicitly:
     ```kotlin
     entry<AddItemKey> { key ->
         AddItemScreen(
             registryId = key.registryId,                  // now String?
             fromAddSheet = key.fromAddSheet,              // NEW
             initialUrl = key.initialUrl,
             initialRegistryId = key.initialRegistryId,
             onBack = { backStack.removeLast() },
             onNavigateToBrowseStores = { regId -> backStack.add(StoreListKey(preSelectedRegistryId = regId)) },
             onNavigateToCreateRegistry = { backStack.add(CreateRegistryKey) },   // NEW — empty-state link
         )
     }
     ```
   - The `entry<CreateRegistryKey>` `onSaved` lambda (line 204-207) keeps its current call: `backStack.add(AddItemKey(registryId = registryId))`. This now resolves with `fromAddSheet = false` by default — picker hidden — which is exactly what we want. DO NOT add `fromAddSheet = false` explicitly; rely on the default to keep the diff minimal.

4. **Edit `app/src/main/res/values/strings.xml`** (after line 247, before `error_loading_registries`):
   - ADD:
     ```xml
     <string name="add_sheet_add_item">Add an item</string>
     <string name="add_sheet_add_item_sub">Add a gift to one of your registries</string>
     <string name="add_item_picker_label">Choose a registry</string>
     <string name="add_item_picker_hint">Select which registry this gift goes to</string>
     ```
   - DELETE the 6 strings: `add_sheet_item_url`, `add_sheet_item_url_sub`, `add_sheet_browse_stores`, `add_sheet_browse_stores_sub`, `add_sheet_add_manually`, `add_sheet_add_manually_sub`.
   - Before deleting, run `grep -rn 'add_sheet_item_url\|add_sheet_browse_stores\|add_sheet_add_manually' app/src/main/java app/src/main/res` and confirm zero matches outside AddActionSheet.kt (which is being edited in step 2). If unexpected hits, surface and HALT.
   - DO NOT delete `add_sheet_no_registry_hint` (will be reused for the picker empty-state in Task 2), `add_sheet_lists_empty_hint`, or `add_sheet_lists_empty_cta` — Task 2 may reuse one or both.

5. **Edit `app/src/main/res/values-ro/strings.xml`** mirror — ADD:
   ```xml
   <string name="add_sheet_add_item">Adaug&#259; un produs</string>
   <string name="add_sheet_add_item_sub">Adaug&#259; un cadou &#238;ntr-una din listele tale</string>
   <string name="add_item_picker_label">Alege o list&#259;</string>
   <string name="add_item_picker_hint">Selecteaz&#259; lista pentru acest cadou</string>
   ```
   DELETE the 6 RO mirrors of the keys removed from values/strings.xml.

Constraints honoured:
- Locale: every new label is in BOTH strings.xml + values-ro/strings.xml — no hardcoded strings (CLAUDE.md I18N rule).
- Bug-fix preservation: zero changes to sheet shape, scrim alpha, drag-handle padding, containerColor, OR `bottomSheetShadow` (which is already absent — confirm not reintroduced).
- Compile-once flow: Task 1 leaves AddItemScreen call site referencing `fromAddSheet = key.fromAddSheet` and `onNavigateToCreateRegistry = ...` — these only exist after Task 2 lands. Both tasks must compile together. Run `./gradlew :app:compileDebugKotlin` ONLY after Task 2 finishes.

Per CLAUDE.md: GSD workflow honoured (this plan is the entry point); all UI labels stay in resource files.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin</automated>
    Also: `grep -rn 'add_sheet_item_url\|add_sheet_browse_stores\|add_sheet_add_manually' app/src/main` returns NO matches in any .kt or .xml file (only allowed in .planning/ docs).
    Also: `grep -n 'onItemFromUrl\|onBrowseStores\|onAddManually' app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` returns NO matches.
    Also: `grep -n 'fromAddSheet' app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` returns the field declaration.
  </verify>
  <done>
    - AddActionSheet.kt has exactly 2 ActionRow calls and a single new `onAddItem` callback parameter; the 3 deprecated callbacks are gone.
    - AppNavKeys.kt: AddItemKey has nullable registryId + fromAddSheet field with default false.
    - AppNavigation.kt: AddActionSheet call has only onDismiss/onNewRegistry/onAddItem; entry<AddItemKey> passes fromAddSheet and onNavigateToCreateRegistry through.
    - strings.xml + values-ro/strings.xml: 4 new keys present, 6 deprecated keys removed (both files), `add_sheet_no_registry_hint` retained.
    - `./gradlew :app:compileDebugKotlin` succeeds (depends on Task 2 already applied).
  </done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: Conditional registry picker as first field + ViewModel refactor for nullable registryId</name>
  <files>
    app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt,
    app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
  </files>
  <action>
Goal: When `fromAddSheet=true`, render a Material3 `ExposedDropdownMenuBox` registry picker as the FIRST field in the form (above the SegmentedTabs), force the user to pick before any save CTA enables, and show an inline "Create a registry first" affordance + tappable link to `CreateRegistryKey` when the registry list is empty. When `fromAddSheet=false` (the existing CreateRegistry → AddItem chain or any other path with concrete registryId), the picker is HIDDEN and behaviour matches today.

1. **Refactor `AddItemViewModel.kt`**:
   - Inject `AuthRepository` and `ObserveRegistriesUseCase` (same pattern as RegistryListViewModel — see @app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt:31-57). DI graph already provides both — confirm by inspecting RegistryListViewModel imports and mirroring exactly.
   - Replace the immutable `val registryId: String = savedStateHandle["registryId"] ?: ""` with a reactive state:
     ```kotlin
     // Hold the savedStateHandle initial value (may be empty when fromAddSheet=true)
     private val initialRegistryIdFromKey: String =
         savedStateHandle["registryId"] ?: ""

     private val _selectedRegistryId = MutableStateFlow(
         if (initialRegistryIdFromKey.isBlank()) null else initialRegistryIdFromKey
     )
     val selectedRegistryId: StateFlow<String?> = _selectedRegistryId.asStateFlow()

     fun setRegistry(id: String) { _selectedRegistryId.value = id }
     ```
   - Add `val fromAddSheet: Boolean = savedStateHandle["fromAddSheet"] ?: false` (Task 3 of `hiltViewModelWithNavArgs` already supports passing booleans via `bundleOf`).
   - Expose a list of the user's registries for the picker:
     ```kotlin
     val registriesForPicker: StateFlow<List<Registry>> =
         authRepository.authState.flatMapLatest { user ->
             if (user == null) flowOf(emptyList())
             else observeRegistries(user.uid)
         }
         .catch { emit(emptyList()) }
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
     ```
     Use the SAME `flatMapLatest` + `@OptIn(ExperimentalCoroutinesApi::class)` pattern as RegistryListViewModel.
   - Add `val isFormReady: StateFlow<Boolean>` — true when `_selectedRegistryId.value != null` (CTAs gate on this when fromAddSheet=true; bypass gate when fromAddSheet=false to preserve current behaviour for the chained path).
   - Update `onSave()` — replace `addItem(registryId, item)` with:
     ```kotlin
     val targetRegistryId = _selectedRegistryId.value
     if (targetRegistryId.isNullOrBlank()) {
         _error.value = "Please choose a registry first"   // defensive — UI gates the CTA
         return
     }
     // ... existing item construction ...
     addItem(targetRegistryId, item)
     ```
   - Remove the unused `// TODO(D-10 follow-up): When multi-registry picker ships ...` comment block (this IS that follow-up).
   - Existing flows (`url`, `title`, `imageUrl`, `price`, `notes`, `isFetchingOg`, `ogFetchFailed`, `ogFetchSucceeded`, `isAffiliateDomain`, `isSaving`, `error`, `savedItemId`) UNCHANGED.

2. **Edit `AddItemScreen.kt`**:
   - Update signature:
     ```kotlin
     fun AddItemScreen(
         registryId: String?,                              // was String — now nullable
         fromAddSheet: Boolean = false,                    // NEW
         initialUrl: String? = null,
         initialRegistryId: String? = null,
         onBack: () -> Unit,
         onNavigateToBrowseStores: (String) -> Unit = {},
         onNavigateToCreateRegistry: () -> Unit = {},      // NEW — empty-state link target
         viewModel: AddItemViewModel = hiltViewModelWithNavArgs(
             key = registryId ?: "no-registry-yet",        // stable key for the no-registry case
             "registryId" to (registryId ?: ""),
             "fromAddSheet" to fromAddSheet,
             "initialUrl" to (initialUrl ?: ""),
             "initialRegistryId" to (initialRegistryId ?: ""),
         )
     )
     ```
   - Collect new VM flows:
     ```kotlin
     val selectedRegistryId by viewModel.selectedRegistryId.collectAsStateWithLifecycle()
     val registriesForPicker by viewModel.registriesForPicker.collectAsStateWithLifecycle()
     ```
   - **Add picker as FIRST field (above SegmentedTabs)** — only when `fromAddSheet == true`:
     - Inside the `Column { ... }` content body (currently the `// 3-tab segmented control` Box is the first child, lines 164-181), insert a new conditional block at the very top:
       ```kotlin
       if (fromAddSheet) {
           Box(
               modifier = Modifier.padding(
                   horizontal = spacing.edge,
                   top = spacing.gap12,
               )
           ) {
               RegistryPickerField(
                   selectedRegistryId = selectedRegistryId,
                   registries = registriesForPicker,
                   onSelect = { viewModel.setRegistry(it) },
                   onCreateRegistry = onNavigateToCreateRegistry,
               )
           }
       }
       ```
   - **Implement `RegistryPickerField` as a private @Composable** at the bottom of AddItemScreen.kt (alongside `PasteUrlModeContent`, `ManualModeContent`, `giftMaisonFieldColors`):
     ```kotlin
     @OptIn(ExperimentalMaterial3Api::class)
     @Composable
     private fun RegistryPickerField(
         selectedRegistryId: String?,
         registries: List<Registry>,
         onSelect: (String) -> Unit,
         onCreateRegistry: () -> Unit,
     ) {
         val colors = GiftMaisonTheme.colors
         val typography = GiftMaisonTheme.typography
         val spacing = GiftMaisonTheme.spacing
         val shapes = GiftMaisonTheme.shapes

         // Empty-state branch: no registries yet → inline "Create a registry first" link.
         if (registries.isEmpty()) {
             Column(verticalArrangement = Arrangement.spacedBy(spacing.gap8)) {
                 Text(
                     text = stringResource(R.string.add_item_picker_label),
                     style = typography.bodyS,
                     color = colors.inkSoft,
                 )
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .background(colors.paperDeep, shapes.radius12)
                         .clickable(onClick = onCreateRegistry)
                         .padding(horizontal = 12.dp, vertical = 12.dp),
                     verticalAlignment = Alignment.CenterVertically,
                 ) {
                     Text(
                         text = stringResource(R.string.add_sheet_no_registry_hint),
                         style = typography.bodyMEmphasis,
                         color = colors.accent,
                         modifier = Modifier.weight(1f),
                     )
                     Icon(
                         imageVector = Icons.Outlined.KeyboardArrowRight,
                         contentDescription = null,
                         tint = colors.accent,
                         modifier = Modifier.size(18.dp),
                     )
                 }
             }
             return
         }

         // Populated branch: ExposedDropdownMenuBox with Material3.
         var expanded by remember { mutableStateOf(false) }
         val selected = registries.firstOrNull { it.id == selectedRegistryId }

         ExposedDropdownMenuBox(
             expanded = expanded,
             onExpandedChange = { expanded = !expanded },
         ) {
             OutlinedTextField(
                 value = selected?.title ?: "",
                 onValueChange = { /* read-only */ },
                 readOnly = true,
                 label = { Text(stringResource(R.string.add_item_picker_label)) },
                 placeholder = { Text(stringResource(R.string.add_item_picker_hint)) },
                 trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                 shape = shapes.radius12,
                 modifier = Modifier
                     .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                     .fillMaxWidth(),
                 colors = giftMaisonFieldColors(),
             )
             ExposedDropdownMenu(
                 expanded = expanded,
                 onDismissRequest = { expanded = false },
             ) {
                 registries.forEach { registry ->
                     DropdownMenuItem(
                         text = { Text(registry.title) },
                         onClick = {
                             onSelect(registry.id)
                             expanded = false
                         },
                     )
                 }
             }
         }
     }
     ```
     Imports needed (add at top of file): `androidx.compose.material3.DropdownMenuItem`, `androidx.compose.material3.ExposedDropdownMenuBox`, `androidx.compose.material3.ExposedDropdownMenu`, `androidx.compose.material3.ExposedDropdownMenuDefaults`, `androidx.compose.material3.MenuAnchorType`, `androidx.compose.material3.ExperimentalMaterial3Api`, `androidx.compose.foundation.background`, `androidx.compose.foundation.clickable`, `androidx.compose.foundation.layout.Column` (already present), `androidx.compose.foundation.shape` (via theme), `androidx.compose.material.icons.Icons`, `androidx.compose.material.icons.outlined.KeyboardArrowRight`, `androidx.compose.material3.DropdownMenuItem`, `com.giftregistry.domain.model.Registry`.

   - **Gate the bottom-bar Save CTAs** — currently `AddItemDualCtaBar(isSaving=..., isFetching=..., onAddAnother=..., onSaveAndExit=...)`. Add a new `enabled` parameter (or wrap the existing onClicks with a guard):
     - Simplest path: wrap the two onClick lambdas:
       ```kotlin
       val canSubmit = !fromAddSheet || selectedRegistryId != null
       AddItemDualCtaBar(
           isSaving = isSaving,
           isFetching = isFetchingOg,
           onAddAnother = {
               if (!canSubmit) return@AddItemDualCtaBar
               addAnotherMode = true
               viewModel.onSave()
           },
           onSaveAndExit = {
               if (!canSubmit) return@AddItemDualCtaBar
               addAnotherMode = false
               viewModel.onSave()
           },
       )
       ```
     - Preferred (visually clearer): inspect `AddItemDualCtaBar` (likely co-located in this file or the `add` package) and add an `enabled: Boolean` parameter that disables both buttons when false. Executor: read the existing component before deciding; whichever path requires fewer touch-points wins. The visual gating is preferred for accessibility but the lambda-guard fallback is acceptable if the dual CTA bar component lives elsewhere and editing it bloats scope.
   - **Behaviour preservation when `fromAddSheet == false`**:
     - The picker block is wrapped in `if (fromAddSheet) { ... }` so it does NOT render — no visual change, no extra Spacer.
     - `canSubmit` short-circuits to `true` — Save buttons enabled as today.
     - `viewModel.onSave()` uses `_selectedRegistryId.value`, which is initialised from `initialRegistryIdFromKey` (the key's `registryId`) when non-blank — same registryId routed to AddItemUseCase as before.

Constraints honoured:
- Localization: all picker labels resolve via stringResource — `add_item_picker_label`, `add_item_picker_hint`, `add_sheet_no_registry_hint`. No hardcoded strings.
- Bug-fix preservation: this task does NOT touch AddActionSheet's shape/scrim/drag-handle. Untouched files = untouched behaviour.
- Existing tests: `AddItemModeTest.kt` and `AffiliateRowVisibilityTest.kt` test pure logic (selected tab default + affiliate row visibility) — neither references the VM constructor signature directly. They should remain green; if they instantiate AddItemViewModel directly they may need an updated constructor — executor must run `./gradlew :app:testDebugUnitTest` and fix any breaks (target: at most extend the test's fake/mock setup to provide AuthRepository + ObserveRegistriesUseCase stubs).
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest</automated>
    Both must succeed. Specifically: `AddItemModeTest`, `AffiliateRowVisibilityTest`, and any other tests in `app/src/test/java/com/giftregistry/ui/item/add/` continue to pass.
    Also: `grep -n 'fromAddSheet\|selectedRegistryId\|registriesForPicker' app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` returns matches in BOTH files.
  </verify>
  <done>
    - AddItemViewModel has nullable `selectedRegistryId: StateFlow<String?>`, `setRegistry(id)` mutator, `registriesForPicker: StateFlow<List<Registry>>`, and `fromAddSheet` saved-state field.
    - AddItemScreen renders `RegistryPickerField` ONLY when `fromAddSheet == true`, as the first child of the form Column above SegmentedTabs.
    - When `registriesForPicker` is empty, the picker shows `add_sheet_no_registry_hint` text + chevron, tapping calls `onNavigateToCreateRegistry`.
    - When populated, ExposedDropdownMenu lists each registry by `title`; tapping calls `viewModel.setRegistry(id)`.
    - Save CTAs gated: when `fromAddSheet=true && selectedRegistryId == null`, both onAddAnother and onSaveAndExit no-op (or are visibly disabled).
    - Build (`compileDebugKotlin`) and unit tests (`testDebugUnitTest`) both pass.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Human-verify the 4 user-facing flows on a device/emulator</name>
  <what-built>
    Trimmed Add sheet (2 rows), conditional registry picker as first field of AddItemScreen, registry-picker empty-state link to Create-Registry, preserved CreateRegistry → AddItem chained path with hidden picker.
  </what-built>
  <files>app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt, app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt</files>
  <action>Install the debug build and walk through the 4 user-facing flows described in <how-to-verify> below. Do not modify any source code in this task — Tasks 1 and 2 have already implemented the change. This task is the human gate that catches anything compile-time tests cannot: visual regressions in the sheet, picker UX, and the chained Create-Registry → AddItem path. If any flow fails, describe the failure in the resume-signal so a follow-up plan can be authored.</action>
  <verify>
    <automated>./gradlew :app:installDebug</automated>
    Manual flows below — see <how-to-verify> for exact steps. ALL FOUR flows must pass.
  </verify>
  <how-to-verify>
    Install the debug build on an emulator or device:
    ```
    ./gradlew :app:installDebug
    ```
    Then exercise these 4 flows. ALL FOUR must pass.

    **Flow 1 — Sheet has exactly 2 rows.**
    1. Open the app, sign in, land on Home.
    2. Tap the centre FAB (ADD) in the bottom nav.
    3. Expected: Bottom sheet slides up. Title reads "What are you adding?". Below the title there are EXACTLY TWO action rows:
       a. "New registry" / "Start a list for any occasion" — accentSoft (pink/rose) background, +home icon.
       b. "Add an item" / "Add a gift to one of your registries" — paperDeep (light grey) background, plus-circle icon.
       NO "Item from URL", NO "Browse stores", NO "Add manually" rows. The sheet is shorter than before.
       Visual regressions check: top corners are rounded (22dp); bottom corners flush; drag handle is centred with even padding above and below; scrim under the sheet is dimmed-ink (you should see the home content behind it darkened).

    **Flow 2 — "Add an item" → form opens with picker as first field, multiple registries.**
    Pre-condition: account has at least 2 registries. (Create extras via "New registry" if needed before starting this flow.)
    1. From Home, tap FAB → "Add an item" row.
    2. Expected: AddItemScreen opens. The TOP-MOST field (above the URL/Browse-stores/Manual tab strip) is a "Choose a registry" dropdown. The dropdown is empty (no registry pre-selected).
    3. Both bottom CTAs ("Add another" and "Save") are DISABLED / unresponsive. Tapping them does nothing OR they appear visibly greyed.
    4. Tap the dropdown. A menu opens listing all your registry titles.
    5. Pick one. The dropdown collapses showing the chosen title.
    6. Both CTAs are now ENABLED. Type a URL or switch to Manual tab, fill a title, tap Save → form saves and exits to wherever Save normally goes (Home or back stack).

    **Flow 3 — "Add an item" → zero registries → empty-state link.**
    Pre-condition: account has ZERO registries. (If you have any, delete them via the Lists tab → registry settings, OR test on a fresh account.)
    1. From Home (the empty-state Home), tap FAB → "Add an item".
    2. Expected: AddItemScreen opens. Where the dropdown would be, you instead see the label "Choose a registry" above an inline tappable row that reads "Create a registry first" (accent-coloured) with a right-chevron.
    3. The Save CTAs are disabled.
    4. Tap the "Create a registry first" row.
    5. Expected: The Create Registry screen opens (the same one reached from "New registry" on the sheet).
    6. Save a new registry. Expected: you are routed to AddItemScreen WITH the new registry pre-selected (Flow 4 covers this). The picker should NOT show again — see Flow 4.

    **Flow 4 — "New registry" → save → AddItemScreen with picker HIDDEN.**
    1. From Home, tap FAB → "New registry".
    2. Fill in a title and any required fields, save.
    3. Expected: AddItemScreen opens. The registry picker is NOT rendered. The form looks exactly as it did before this change (URL tab default, segmented tabs at top).
    4. Add an item, Save → goes back to wherever Save normally exits.

    **Bug-fix preservation (visual quick checks during Flow 1):**
    - The sheet's drag handle has padding above it equal to padding below it (the just-shipped symmetric fix).
    - There is no shadow / elevation halo above the sheet (the just-shipped no-`bottomSheetShadow` fix).
    - The bottom nav is fully covered by the scrim (not visible behind sheet).

    Localization spot-check (optional but valuable):
    - Switch device locale to Romanian (Settings → System → Languages or via app settings).
    - Repeat Flow 1: sheet rows should read "List&#259; nou&#259;" / "Adaug&#259; un produs"; subtitles should be in Romanian.
    - Repeat Flow 2: dropdown label should read "Alege o list&#259;"; empty-state should read "Creeaz&#259; mai &#238;nt&#226;i o list&#259;".
  </how-to-verify>
  <resume-signal>
    Type "approved" to mark this quick task complete, OR describe which flow failed (1, 2, 3, or 4) and what you saw.
  </resume-signal>
  <done>All 4 flows pass; no visual regressions in the sheet's just-shipped fixes; locale toggle (if checked) renders translated copy.</done>
</task>

</tasks>

<verification>
- Build: `./gradlew :app:compileDebugKotlin` succeeds.
- Unit tests: `./gradlew :app:testDebugUnitTest` succeeds — at minimum AddItemModeTest, AffiliateRowVisibilityTest, BottomNavVisibilityTest stay green.
- Lint/strings: zero hardcoded user-facing strings introduced; every new label resolves via R.string.*.
- Strings cleanup: 6 deprecated keys removed from BOTH values/strings.xml and values-ro/strings.xml; 4 new keys added to BOTH.
- Nav-key migration: every existing call site of AddItemKey(...) still compiles (defaults cover the new fromAddSheet field; nullable registryId widens but existing callers pass non-null Strings).
- Human verification: 4 flows in Task 3 all pass.
</verification>

<success_criteria>
- The bottom-sheet shows exactly 2 action rows (New registry + Add an item) — verified visually.
- Tapping "Add an item" with multiple registries opens AddItemScreen with the registry picker as first field; Save is disabled until a pick is made; choosing a registry enables Save.
- Tapping "Add an item" with zero registries shows an inline "Create a registry first" link that navigates to CreateRegistryKey.
- The CreateRegistry → AddItemKey chain (after saving a new registry) opens AddItemScreen WITHOUT the picker visible (registry pre-selected to the freshly-created id).
- Sheet visual fixes (corners, scrim alpha, drag-handle padding, no shadow) preserved — no regression.
- Localization preserved — both EN and RO have all new keys; old keys removed from both.
</success_criteria>

<output>
After completion, create `.planning/quick/260428-iny-trim-add-sheet-to-2-options-new-registry/260428-iny-SUMMARY.md` capturing: files changed, the AddItemKey nav-key migration (nullable registryId + fromAddSheet flag), strings added/removed (with the deprecated set explicitly listed), the picker-gating pattern, and any deviations the executor took (e.g. AddItemDualCtaBar enabled-param vs. lambda-guard choice, choice between reusing add_sheet_no_registry_hint vs. adding a new key).
</output>
