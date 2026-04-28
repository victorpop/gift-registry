---
phase: quick-260428-iny
plan: 01
subsystem: chrome+add-item
tags: [add-sheet, registry-picker, navigation, i18n]
dependency_graph:
  requires:
    - AddActionSheet (Phase 9 CHROME-03 — sheet shape, scrim, drag-handle fixes from quick-260427-nkn / 260427-n67 / 260427-lwz)
    - AddItemScreen (Phase 11 SCR-10 — paste/browse/manual segmented tabs, dual CTA bar)
    - AddItemViewModel (Phase 11 — title/url/notes/price/imageUrl flows, OG fetch, addAnotherMode)
    - hiltViewModelWithNavArgs (Phase 2 — bundleOf(*args) seeds SavedStateHandle)
    - ObserveRegistriesUseCase (Phase 3 — repository.observeRegistries(uid))
    - RegistryListViewModel (Phase 3 — flatMapLatest pattern reused)
  provides:
    - "AddItemKey: nullable registryId + fromAddSheet flag — enables FAB-sheet entry without preselected registry"
    - "AddActionSheet: 2-row trimmed shape + onAddItem callback (single replacement for the 3 deprecated callbacks)"
    - "RegistryPickerField composable (private to AddItemScreen) — empty-state link + populated dropdown branches"
    - "AddItemViewModel.selectedRegistryId / setRegistry / registriesForPicker — picker state reactive to authState"
    - "AddItemDualCtaBar.enabled param — visual gating of save buttons"
  affects:
    - AppNavigation entry<AddItemKey> (now passes fromAddSheet + onNavigateToCreateRegistry)
    - AppNavigation AddActionSheet call (3 callbacks collapsed to 1)
    - strings.xml + values-ro/strings.xml (4 new keys, 6 deprecated keys removed)
tech_stack:
  added: []
  patterns:
    - "Material3 ExposedDropdownMenuBox/ExposedDropdownMenu — first use of ExposedDropdown* in this codebase"
    - "ExposedDropdownMenuAnchorType.PrimaryNotEditable (not MenuAnchorType — that name was renamed in Material3 1.4.0)"
    - "Member-function ExposedDropdownMenu — called inside ExposedDropdownMenuBox lambda (receiver picks up the BoxScope; no qualified import works because it is a scope member)"
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemDualCtaBar.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
decisions:
  - "AddItemDualCtaBar gained an `enabled: Boolean = true` parameter — chosen over the lambda-guard fallback because the visual disable (Button/OutlinedButton already react to disabled=!isSaving && !isFetching && enabled) gives users immediate feedback that the picker must be filled. Default=true keeps every existing call site unchanged."
  - "Reused R.string.add_sheet_no_registry_hint for the picker empty-state CTA (planner explicitly recommended reuse) — avoided introducing add_item_no_registries_cta. Picker label/hint added as new keys (add_item_picker_label, add_item_picker_hint) since no existing key carries the right copy."
  - "AddItemKey.registryId widened to String? rather than introducing a separate AddItemPickerKey — keeps entry<AddItemKey> as the single render call site. fromAddSheet is the disambiguator. Five existing call sites still compile because they all pass concrete non-null Strings."
  - "ExposedDropdownMenu invoked WITHOUT a qualified import because Material3 1.4.0 makes it a member of ExposedDropdownMenuBoxScope. The qualified-import attempt (`androidx.compose.material3.ExposedDropdownMenu(...)`) failed compilation; relying on the receiver scope of the enclosing ExposedDropdownMenuBox is the canonical path."
  - "Type alias rename: Material3 1.4.0 renamed MenuAnchorType to ExposedDropdownMenuAnchorType. The plan referenced the old name; corrected during implementation."
  - "Browse-stores side-trip from the URL/Browse/Manual tabs now no-ops when selectedRegistryId is null — the user must pick a registry before navigating away. Prevents `onNavigateToBrowseStores(\"\")` which would crash StoreBrowser with an empty preSelectedRegistryId."
metrics:
  duration: 7min
  completed: 2026-04-28
---

# Quick task 260428-iny: Trim Add sheet to 2 rows + conditional registry picker — Summary

## One-liner

Trimmed the FAB Add-action sheet from 4 rows to 2 (New registry + Add an item) and gated AddItemScreen behind an explicit registry picker as the first field when entered from the sheet — picker hides on the CreateRegistry → AddItem chained path so the freshly-created registry stays auto-selected.

## What was built

**Sheet (CHROME-03 trim):**
The 3 secondary rows ("Item from URL", "Browse stores", "Add manually") collapsed into a single "Add an item" row. All 3 destinations are still reachable from inside `AddItemScreen` itself (the URL/Browse-stores/Manual segmented tab strip) and Browse-stores is also reachable via the bottom-nav `STORES` tab — no flow orphaned. The sheet now reads:

1. **New registry** — primary row, accentSoft background, +home icon (unchanged copy)
2. **Add an item** — secondary row, paperDeep background, plus-circle icon (new copy)

**Bug-fix preservation:** Top-only 22 dp corners, scrim ink alpha 0.55, paper container, symmetric drag-handle padding (`vertical = 12.dp`), and the absence of `bottomSheetShadow` — all preserved exactly. The just-shipped fixes from quick-260427-nkn (drop FAB lift / no protrusion) / 260427-n67 (FAB optical alignment) / 260427-lwz (bottom-nav clipping) are not regressed because none of those code paths were touched.

**Nav-key migration:**
```kotlin
@Serializable data class AddItemKey(
    val registryId: String? = null,           // was: String (required) -> nullable
    val initialUrl: String? = null,
    val initialRegistryId: String? = null,
    val fromAddSheet: Boolean = false,        // NEW
)
```
The widening is non-breaking — every existing call site (`CreateRegistry.onSaved → AddItemKey(registryId)`, `StoreBrowser.onAddToList → AddItemKey(registryId, initialUrl, initialRegistryId)`, `RegistryDetail.onNavigateToAddItem → AddItemKey(key.registryId)`) keeps working because they all pass concrete non-null `String` values. The `fromAddSheet` default of `false` keeps the picker hidden on these paths.

**AddItemScreen registry picker:**
A new private `RegistryPickerField` composable rendered as the FIRST child of the form column when `fromAddSheet == true`:

- **Populated branch** (`registries.isNotEmpty()`) — Material3 `ExposedDropdownMenuBox` with an `OutlinedTextField` anchor (read-only, label/placeholder via stringResource) and a dropdown listing each registry by `title`. Tapping a row calls `viewModel.setRegistry(id)`, which flips `selectedRegistryId` and enables the bottom Save CTAs.
- **Empty branch** (`registries.isEmpty()`) — inline "Create a registry first" affordance (label + tappable row with right-chevron) that calls `onNavigateToCreateRegistry`, routing the user to `CreateRegistryKey`.

When `fromAddSheet == false` the entire `if` block is bypassed — no spacer, no extra padding, no behavioural change for the `CreateRegistry → AddItem` chained path.

**ViewModel reshape:**
- Injects `AuthRepository` + `ObserveRegistriesUseCase`, mirroring `RegistryListViewModel`'s `flatMapLatest`/`stateIn` pattern.
- `selectedRegistryId: StateFlow<String?>` — initialised from `savedStateHandle["registryId"]` when non-blank (existing paths), otherwise `null` (FAB sheet path).
- `setRegistry(id)` — picker mutator.
- `registriesForPicker: StateFlow<List<Registry>>` — drives the dropdown; empty list flips into the empty-state branch.
- `onSave()` — now reads `_selectedRegistryId.value` (with a defensive blank-guard) instead of the immutable `registryId` field.

**Save CTA gating:**
`AddItemDualCtaBar` gained an `enabled: Boolean = true` parameter. When `fromAddSheet=true && selectedRegistryId == null`, `canSubmit = false` and the dual CTAs render visually disabled (Material3 `Button.enabled = false`) AND the lambda guards short-circuit defensively. Default=`true` keeps every existing call site unchanged.

## Why it was built

Reduce decision fatigue: the previous 4-row sheet asked users to pick between "Item from URL", "Browse stores", and "Add manually" — but those 3 paths all landed on the same `AddItemScreen` form. Consolidating them behind one row + an explicit registry picker as the first form field replaces a 4-way choice with 2 clear options ("Make a new list" / "Add to a list") and forces the registry choice to be conscious instead of implicit (the previous sheet auto-defaulted to the most-recently-updated registry, which is invisible to the user).

## Strings touched

**Added (EN + RO):**

| Key                       | EN                                            | RO                                                   |
| ------------------------- | --------------------------------------------- | ---------------------------------------------------- |
| `add_sheet_add_item`      | Add an item                                   | Adaugă un produs                                      |
| `add_sheet_add_item_sub`  | Add a gift to one of your registries          | Adaugă un cadou într-una din listele tale              |
| `add_item_picker_label`   | Choose a registry                             | Alege o listă                                         |
| `add_item_picker_hint`    | Select which registry this gift goes to       | Selectează lista pentru acest cadou                   |

**Removed (EN + RO) — verified zero production consumers:**

- `add_sheet_item_url`, `add_sheet_item_url_sub`
- `add_sheet_browse_stores`, `add_sheet_browse_stores_sub`
- `add_sheet_add_manually`, `add_sheet_add_manually_sub`

**Retained for reuse:**

- `add_sheet_no_registry_hint` — used by the picker's empty-state CTA (planner-recommended reuse).
- `add_sheet_lists_empty_hint`, `add_sheet_lists_empty_cta` — kept untouched (Lists-tab empty-state copy still in scope).

CLAUDE.md localisation rule honoured: every new label/copy resolves through `stringResource(R.string.*)`. No hardcoded user-facing strings.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Build error] `Modifier.padding(horizontal=, top=)` mixed-keyword call**
- **Found during:** Task 2 first compile attempt
- **Issue:** Compose's `Modifier.padding` overloads do not allow mixing `horizontal=` with `top=` in a single call.
- **Fix:** Switched to explicit `start = / end = / top =` keywords on the picker's outer Box.
- **File:** `AddItemScreen.kt` line 203
- **Commit:** 5752adf

**2. [Rule 3 - Build error] `MenuAnchorType` rename in Material3 1.4.0**
- **Found during:** Task 2 first compile attempt
- **Issue:** Plan referenced `MenuAnchorType.PrimaryNotEditable`. Material3 1.4.0 renamed the type to `ExposedDropdownMenuAnchorType.PrimaryNotEditable` (the legacy name is gone).
- **Fix:** Updated import + usage to `ExposedDropdownMenuAnchorType`.
- **File:** `AddItemScreen.kt` import block + RegistryPickerField body
- **Commit:** 5752adf

**3. [Rule 3 - Build error] `ExposedDropdownMenu` is a scope member, not a top-level function**
- **Found during:** Task 2 first compile attempt
- **Issue:** Material3 1.4.0 declares `ExposedDropdownMenu` as a member function of `ExposedDropdownMenuBoxScope`. The plan suggested calling `androidx.compose.material3.ExposedDropdownMenu(...)` qualified — that path doesn't exist.
- **Fix:** Removed the qualified prefix; called `ExposedDropdownMenu(...)` directly inside the `ExposedDropdownMenuBox { ... }` lambda so the receiver scope is in effect.
- **File:** `AddItemScreen.kt` RegistryPickerField populated-branch
- **Commit:** 5752adf

### Choices the plan deferred to executor

**1. AddItemDualCtaBar gating: `enabled` param, not lambda-only guard.**
The plan presented two options: wrap onClick lambdas with a `canSubmit` check (lighter touch) OR add an `enabled: Boolean` parameter to the dual CTA bar (visually clearer). I chose the param path — added `enabled: Boolean = true` to `AddItemDualCtaBar`, then plumbed both `enabled = canSubmit` and the lambda guards. The default value keeps every existing call site (currently only `AddItemScreen` itself, but defensive for future callers) unchanged. The lambda guards remain as a defensive backstop.

**2. Reused `add_sheet_no_registry_hint`, did not add `add_item_no_registries_cta`.**
The planner explicitly noted reuse was preferred to keep strings.xml lean — the EN copy "Create a registry first" already exists and matches the empty-state copy exactly. The RO mirror "Creează mai întâi o listă" is identical to the recommended translation. No duplicate key needed.

**3. Browse-stores side-trip skipped when `selectedRegistryId == null`.**
The pre-existing `LaunchedEffect(selectedTab)` in AddItemScreen unconditionally called `onNavigateToBrowseStores(registryId)`. After the registryId widening to `String?`, an unguarded call would either pass `""` (legacy) or crash. Added a `target == null` short-circuit so the user must pick a registry before the Browse-stores tab can navigate. Tracked here as a deviation Rule 1 — the previous behaviour passed an empty `registryId` to `StoreListKey(preSelectedRegistryId = "")`, which downstream code would have to handle defensively. The new behaviour is strictly safer.

## Authentication gates

None — this task is pure UI/state plumbing.

## Tests

**Unit tests:** All pass.

```
./gradlew :app:testDebugUnitTest  →  BUILD SUCCESSFUL
```

- `AddItemModeTest` — pure-Kotlin, no VM instantiation; unaffected by the VM constructor signature change.
- `AffiliateRowVisibilityTest` — pure-Kotlin predicate test; unaffected.
- `BottomNavVisibilityTest` — calls `AddItemKey("r1")`. Positional `String` still binds to the (now-nullable) first parameter; test passes without modification.
- `AddItemViewModelTest` — none exists yet (the VM is currently un-tested at the unit level; addition is out of scope and non-blocking).

**Build:**

```
./gradlew :app:compileDebugKotlin  →  BUILD SUCCESSFUL
```

Only deprecation warnings surfaced are pre-existing (`Icons.Outlined.KeyboardArrowRight` per STATE.md decision — kept for parity with `AddActionSheet.kt`'s pre-existing usage).

## Outstanding — Task 3 (checkpoint:human-verify)

Task 3 of the plan is a `checkpoint:human-verify` step. Per the executor constraints, this was NOT blocked on; it is documented here for the user to walk through on a device/emulator.

```bash
./gradlew :app:installDebug
```

Then exercise the **4 user-facing flows** (full transcript in `260428-iny-PLAN.md` `<how-to-verify>`):

### Flow 1 — Sheet has exactly 2 rows
1. Open the app, sign in, land on Home.
2. Tap the centre FAB (ADD) in the bottom nav.
3. **Expected:** Bottom sheet opens with title "What are you adding?" above EXACTLY TWO rows:
   - **New registry** / Start a list for any occasion (accentSoft / pink, +home icon)
   - **Add an item** / Add a gift to one of your registries (paperDeep / light grey, plus-circle icon)
4. Visual regression checks:
   - Top corners are 22 dp rounded; bottom corners flush
   - Drag handle has equal padding above and below (symmetric — quick-260427-nkn fix preserved)
   - No shadow / elevation halo above the sheet (no `bottomSheetShadow`)
   - Scrim covers the bottom nav (sheet hoisted above Scaffold's NavDisplay)

### Flow 2 — "Add an item" with multiple registries
**Pre-condition:** account has ≥ 2 registries.
1. Tap FAB → "Add an item".
2. **Expected:** AddItemScreen opens. The TOP-MOST field (above the URL/Browse-stores/Manual tab strip) is a **"Choose a registry"** dropdown. The dropdown is empty (no registry pre-selected).
3. Both bottom CTAs ("Add another" and "Save to registry ✓") are DISABLED / visibly greyed.
4. Tap the dropdown → menu opens listing all your registry titles.
5. Pick one → dropdown collapses showing the chosen title.
6. Both CTAs are now ENABLED. Type a URL or switch to Manual tab, fill a title, tap Save → form saves and exits.

### Flow 3 — "Add an item" with zero registries
**Pre-condition:** account has ZERO registries (delete all via Lists tab → registry settings, or use a fresh account).
1. Tap FAB → "Add an item".
2. **Expected:** AddItemScreen opens. Where the dropdown would be, you see a **"Choose a registry"** label above an inline tappable row reading **"Create a registry first"** (accent-coloured) with a right-chevron.
3. The Save CTAs are disabled.
4. Tap the "Create a registry first" row.
5. **Expected:** Create Registry screen opens (the same one reached from "New registry" on the sheet).
6. Save a new registry → Flow 4 covers what happens next.

### Flow 4 — "New registry" → save → AddItemScreen with picker HIDDEN
1. Tap FAB → "New registry".
2. Fill in a title, save.
3. **Expected:** AddItemScreen opens. The registry picker is **NOT** rendered (no `Choose a registry` label, no dropdown). The form looks exactly as it did before this change — URL tab default, segmented tabs at the very top.
4. Add an item, Save → goes back to wherever Save normally exits.

### Optional — Romanian locale spot-check
Switch device locale to Romanian (Settings → System → Languages, or via the in-app Settings → Limbă). Repeat Flows 1 and 2:
- Sheet rows: **Listă nouă** / **Adaugă un produs**
- Picker label: **Alege o listă**
- Empty state: **Creează mai întâi o listă**

### Resume signal

Type **"approved"** to mark this quick task fully complete, OR describe which flow failed (1, 2, 3, or 4) and what was observed.

## Self-Check: PASSED

Files verified to exist:
- `app/src/main/java/com/giftregistry/ui/common/chrome/AddActionSheet.kt`
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt`
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt`
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt`
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemDualCtaBar.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ro/strings.xml`

Commit verified: `5752adf` (`feat(quick-260428-iny): trim Add sheet to 2 rows + conditional registry picker`).
