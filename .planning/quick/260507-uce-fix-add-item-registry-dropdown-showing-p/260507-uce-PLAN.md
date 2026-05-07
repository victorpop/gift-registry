---
phase: quick-260507-uce
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
  - app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelPickerFilterTest.kt
autonomous: false
requirements:
  - quick-260507-uce-bug
must_haves:
  truths:
    - "Tapping bottom-nav '+' opens AddItemScreen and the Choose-a-registry dropdown lists ONLY active registries (eventDateMs == null OR eventDateMs >= startOfTodayMs)"
    - "Past registries (eventDateMs < startOfTodayMs, e.g., 'test', 'A&V Wedding', 'Secret Santa') are absent from the dropdown"
    - "Empty-state branch still fires when the user has zero ACTIVE registries (even if past registries exist), surfacing the inline 'Create a registry first' affordance"
    - "All existing AddItemScreen entry paths (CreateRegistry chain, Store Browser deep link, RegistryDetail FAB) are unaffected — picker stays hidden, save still works against any registryId"
    - "The Active definition matches RegistryListScreen's Active tab exactly — no new convention introduced"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt"
      provides: "registriesForPicker filtered through Registry.isActive(todayMs) before stateIn"
      contains: "isActive("
    - path: "app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelPickerFilterTest.kt"
      provides: "Unit test pinning the active-only filter contract"
      contains: "registriesForPicker"
  key_links:
    - from: "AddItemViewModel.registriesForPicker"
      to: "com.giftregistry.ui.registry.list.TabFilters.isActive"
      via: "extension import + map { it.filter { r -> r.isActive(todayMs) } }"
      pattern: "isActive\\("
---

<objective>
Fix the "Choose a registry" dropdown in AddItemScreen so it only shows active registries (matching the Lists screen Active-tab definition), instead of every registry the owner has ever created.

Purpose: Past/expired registries (e.g., "test", "A&V Wedding", "Secret Santa") shouldn't be valid targets when adding a new gift. The Lists screen already established the active/past convention via `Registry.isActive(todayMs)` in `TabFilters.kt` — reuse it; do NOT invent a new "active" predicate.

Output: `AddItemViewModel.registriesForPicker` filters out past registries before exposing the StateFlow to AddItemScreen's RegistryPickerField.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
@app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
@app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
@app/src/main/java/com/giftregistry/domain/model/Registry.kt
@app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt

<interfaces>
<!-- Contracts the executor must use directly. NO codebase exploration required. -->

From app/src/main/java/com/giftregistry/domain/model/Registry.kt:
```kotlin
data class Registry(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val occasion: String = "",
    val visibility: String = "public",
    val eventDateMs: Long? = null,           // <-- the active/past discriminator
    val eventLocation: String? = null,
    val description: String? = null,
    val locale: String = "en",
    val notificationsEnabled: Boolean = true,
    val invitedUsers: Map<String, Boolean> = emptyMap(),
    val imageUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
```

From app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt
(this file is the single source of truth for the active/past convention — DO NOT redefine):
```kotlin
package com.giftregistry.ui.registry.list

// Returns midnight (device tz) of the day containing `now`. Default arg = System.currentTimeMillis().
fun startOfTodayMs(now: Long = System.currentTimeMillis()): Long

// Active = eventDateMs == null || eventDateMs >= todayMs    (inclusive lower bound)
fun Registry.isActive(todayMs: Long): Boolean

// Past = eventDateMs != null && eventDateMs < todayMs       (strict)
fun Registry.isPast(todayMs: Long): Boolean
```

Current bug location — `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` lines 71-78
(returns the FULL unfiltered list — this is what we change):
```kotlin
val registriesForPicker: StateFlow<List<Registry>> =
    authRepository.authState
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else observeRegistries(user.uid)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```

How AddItemScreen consumes the StateFlow
(`app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` line 91 + RegistryPickerField at line 454):
```kotlin
val registriesForPicker by viewModel.registriesForPicker.collectAsStateWithLifecycle()
// ...
RegistryPickerField(
    selectedRegistryId = selectedRegistryId,
    registries = registriesForPicker,                 // consumed verbatim — no UI-side filter
    onSelect = { viewModel.setRegistry(it) },
    onCreateRegistry = onNavigateToCreateRegistry,
)
// RegistryPickerField branches on registries.isEmpty() for the "Create a registry first" affordance.
```

Reference precedent — `RegistryListScreen.kt` lines 122-129 (how Lists tab applies the same filter):
```kotlin
val todayMs = remember { startOfTodayMs() }
val filtered = remember(registries, selectedTabIndex, todayMs) {
    when (selectedTabIndex) {
        0 -> registries.filter { it.isActive(todayMs) }   // Active tab — REUSE THIS PREDICATE
        1 -> registries.filter { it.isPast(todayMs) }
        else -> registries
    }
}
```

Existing test pattern — `app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt`
(plain JUnit + assertTrue/assertFalse, no Robolectric, no MockK). Mirror this style.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Filter registriesForPicker to active registries only</name>
  <files>app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt, app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelPickerFilterTest.kt</files>
  <behavior>
    Test contract — AddItemViewModelPickerFilterTest:
    - Test 1: `registriesForPicker_excludesPastRegistries` — given a Flow of 3 registries (one with eventDateMs in the past, one in the future, one with eventDateMs == null), the filter retains only the future + null-dated registries. Past registry id MUST NOT appear in the resulting list.
    - Test 2: `registriesForPicker_includesNullEventDate` — registry with eventDateMs == null is treated as Active (matches `Registry.isActive` semantics).
    - Test 3: `registriesForPicker_includesTodayBoundary` — registry whose eventDateMs equals startOfTodayMs is Active (inclusive boundary, matches Lists screen behaviour).
    - Test 4: `registriesForPicker_emptyWhenAllPast` — when every registry returned by ObserveRegistriesUseCase is past, the picker StateFlow emits an empty list (drives the empty-state "Create a registry first" affordance in RegistryPickerField).

    Test technique: do NOT instantiate AddItemViewModel directly (constructor pulls SavedStateHandle, AuthRepository, several use cases — too much wiring for a unit test). Instead, exercise the SAME filter expression as a pure function over a `List<Registry>` and a fixed `todayMs`. The production code in Task 1's implementation must use that exact predicate so this test pins the contract. Concretely:
      - In the test file declare a local helper `fun activeRegistriesFor(list: List<Registry>, todayMs: Long): List<Registry> = list.filter { it.isActive(todayMs) }` — this mirrors the production filter.
      - Assert per case using fixed `todayMs = 5_000L` and Registry instances with eventDateMs ∈ {null, 1_000L, 5_000L, 10_000L}.
    This matches the existing TabFilterPredicateTest style (plain JUnit, no Android framework, no MockK) so the test runs in the standard `testDebugUnitTest` task with zero new dependencies.
  </behavior>
  <action>
    1. Write `app/src/test/java/com/giftregistry/ui/item/add/AddItemViewModelPickerFilterTest.kt` first (RED). Use plain JUnit 4 (`org.junit.Test`, `assertTrue`, `assertFalse`, `assertEquals`). Import `com.giftregistry.domain.model.Registry` and `com.giftregistry.ui.registry.list.isActive`. Mirror the structure of `TabFilterPredicateTest.kt` for consistency.

    2. Modify `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt`:
       - Add import: `import com.giftregistry.ui.registry.list.isActive`
       - Add import: `import com.giftregistry.ui.registry.list.startOfTodayMs`
       - Change the `registriesForPicker` chain to apply the active filter BEFORE `.catch { ... }.stateIn(...)`. Pattern:
         ```kotlin
         val registriesForPicker: StateFlow<List<Registry>> =
             authRepository.authState
                 .flatMapLatest { user ->
                     if (user == null) flowOf(emptyList())
                     else observeRegistries(user.uid)
                 }
                 .map { registries ->
                     val todayMs = startOfTodayMs()    // captured per emission so Flow updates if user keeps the screen open across midnight
                     registries.filter { it.isActive(todayMs) }
                 }
                 .catch { emit(emptyList()) }
                 .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
         ```
       - Compute `todayMs` INSIDE the `map { }` block (per emission), not once outside it — so a screen kept open past midnight re-evaluates "active" naturally on the next Firestore emission. This is consistent with how `RegistryListScreen` recomputes via `remember(registries, ...)`.
       - DO NOT add a new "isActive" definition or duplicate `startOfTodayMs` — import the existing extensions from `com.giftregistry.ui.registry.list`.
       - DO NOT touch any other field (selectedRegistryId, onSave, etc.).

    3. Run unit tests for the add-item module: `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.add.AddItemViewModelPickerFilterTest"` (GREEN).

    Why this layer (ViewModel, not repository): The repository's `observeRegistries(ownerId)` is shared with `RegistryListViewModel` which needs the FULL list (Active + Past tabs). Filtering at the repository layer would break the Past tab. Filtering at the picker's ViewModel layer is the minimal change — the picker is the only consumer that wants active-only semantics.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry &amp;&amp; ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.add.AddItemViewModelPickerFilterTest" --tests "com.giftregistry.ui.item.add.AddItemModeTest" --tests "com.giftregistry.ui.item.add.AffiliateRowVisibilityTest" --tests "com.giftregistry.ui.registry.list.TabFilterPredicateTest"</automated>
  </verify>
  <done>
    - AddItemViewModelPickerFilterTest: 4/4 tests pass.
    - Existing AddItemModeTest, AffiliateRowVisibilityTest, TabFilterPredicateTest: still pass (no regressions).
    - `AddItemViewModel.kt` imports `isActive` and `startOfTodayMs` from `com.giftregistry.ui.registry.list` — no duplicate predicate.
    - `registriesForPicker` chain shape: `flatMapLatest { ... } → map { filter isActive } → catch → stateIn` (filter applied BEFORE catch so error fallback still emits an empty list).
    - `app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt` is unchanged.
    - `RegistryListScreen.kt` / `RegistryListViewModel.kt` are unchanged (Lists screen still sees all registries; its tab predicate handles Active vs Past locally).
    - No new strings.xml keys added (no UI copy change — the empty-state path already exists for the "no registries at all" case and now also covers "all past registries").
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Device verification — picker shows active registries only</name>
  <files>(no source changes — manual device verification only)</files>
  <action>
    Human-verification checkpoint. Builds + installs the debug APK and walks the operator through the failing reproduction path from the bug report so we can confirm the fix landed end-to-end. No code edits in this task.
  </action>
  <what-built>
    `AddItemViewModel.registriesForPicker` now filters its emitted list through `Registry.isActive(startOfTodayMs())`, reusing the predicate that powers the Lists screen Active tab. Past registries no longer appear in the bottom-nav '+' add-item dropdown.
  </what-built>
  <how-to-verify>
    1. Build + install on the test device:
       ```
       cd /Users/victorpop/ai-projects/gift-registry &amp;&amp; ./gradlew :app:installDebug
       ```
    2. Open the app, sign in as the user whose registries include the past entries reported in the bug ("test", "A&V Wedding", "Secret Santa") — the same account that exhibited the original bug.
    3. From the Home screen, tap the bottom-nav '+' (ADD) button → choose "Add an item" → AddItemScreen opens with the picker as the first field.
    4. Tap the "Choose a registry" dropdown.
    5. Verify the dropdown list:
       - SHOWS only registries whose eventDateMs is null OR ≥ today's midnight (i.e., the same set that appears in Lists → Active tab).
       - DOES NOT show "test", "A&V Wedding", "Secret Santa" (or any registry whose event date is before today).
    6. Cross-check by switching to Lists tab — the Active tab there should list exactly the same registries as the picker. If they diverge, the fix is wrong.
    7. Edge case — pick one of the visible (active) registries and complete a Save. Verify the item is added correctly (no regression on the save path).
    8. Edge case — if you have an account with NO active registries (only past ones), open Add an item via the FAB sheet and verify the picker shows the empty-state "Create a registry first" link (instead of listing past registries).
    9. Regression check — open Add Item via the RegistryDetail FAB and via the Store Browser "Add to list" path. The picker should NOT render in those flows (they pass a concrete registryId; `fromAddSheet=false`). Save should still work end-to-end.
  </how-to-verify>
  <verify>Device walkthrough per how-to-verify steps 1-9 above.</verify>
  <done>Operator confirms picker excludes past registries on the bug-reproducing account AND Lists Active tab matches dropdown contents AND no regressions on the three non-FAB-sheet entry paths.</done>
  <resume-signal>Type "approved" if verified, or describe what's still wrong.</resume-signal>
</task>

</tasks>

<verification>
- Unit tests: `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.add.*" --tests "com.giftregistry.ui.registry.list.TabFilterPredicateTest"` → green.
- Compile check: `./gradlew :app:assembleDebug` → green (no broken imports, no other call sites of `registriesForPicker` to update).
- Manual verification per Task 2 confirms the dropdown contents match the Lists Active tab.
</verification>

<success_criteria>
- The "Choose a registry" dropdown in AddItemScreen lists only active registries (eventDateMs == null OR eventDateMs >= startOfTodayMs).
- Definition of "active" matches RegistryListScreen — same `Registry.isActive(todayMs)` extension is the only source of truth.
- Empty-state path still works: zero active registries → inline "Create a registry first" affordance fires (even if past registries exist).
- No regressions on CreateRegistry → AddItem chain, Store Browser → AddItem chain, or RegistryDetail FAB → AddItem chain (picker stays hidden, save flow unchanged).
- New file count: 1 test file (`AddItemViewModelPickerFilterTest.kt`).
- Modified file count: 1 (`AddItemViewModel.kt` — two new imports + filter step in `registriesForPicker`).
- Zero changes to: TabFilters.kt, RegistryListScreen.kt, RegistryListViewModel.kt, AddItemScreen.kt, RegistryPickerField, RegistryRepository, ObserveRegistriesUseCase, strings.xml, navigation keys.
</success_criteria>

<output>
After completion, create `.planning/quick/260507-uce-fix-add-item-registry-dropdown-showing-p/260507-uce-SUMMARY.md` summarising:
- Root cause one-liner (registriesForPicker bypassed the same active filter the Lists screen uses).
- The two-line fix (import + map filter).
- Verification status (unit + device).
- Any follow-ups (none expected — this is a localized bug fix).
</output>
