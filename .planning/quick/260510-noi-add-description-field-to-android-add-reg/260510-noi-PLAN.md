---
phase: quick-260510-noi
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt
  - app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt
autonomous: false
requirements:
  - QUICK-NOI-01

must_haves:
  truths:
    - "Owner sees a multi-line 'Description' field on the Add/Edit registry form"
    - "Description is optional — empty submission still creates the registry"
    - "Typed description (≤500 chars) persists to Firestore registries/{id}.description"
    - "Edit mode loads existing description into the field on screen open"
    - "Description label and hint render in English (values/) and Romanian (values-ro/)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt"
      provides: "Description OutlinedTextField bound to viewModel.description"
      contains: "viewModel.description"
    - path: "app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt"
      provides: "Pin: description round-trips through onSave; empty-string maps to null"
  key_links:
    - from: "CreateRegistryScreen.kt OutlinedTextField"
      to: "CreateRegistryViewModel.description: MutableStateFlow<String>"
      via: "value=description, onValueChange={ viewModel.description.value = it.take(500) }"
      pattern: "viewModel\\.description\\.value"
    - from: "CreateRegistryViewModel.onSave (existing)"
      to: "Registry.description (existing)"
      via: "description = description.value.ifBlank { null }"
      pattern: "description = description\\.value\\.ifBlank"
---

<objective>
Wire the existing `description` field through the Android Add/Edit Registry form's UI. All backend, mapping, domain, repository, ViewModel, and i18n string scaffolding is already in place from prior phases — this task adds only the missing OutlinedTextField rendering plus a regression test pinning the round-trip.

Purpose: Owner can capture a short note about the occasion when creating or editing a registry. Currently the field is silently dropped because no UI surfaces it.

Output: Visible multi-line description input on `CreateRegistryScreen`, persisted to `registries/{id}.description`, with an automated test pinning the empty→null and non-empty→string contracts.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@/Users/victorpop/ai-projects/gift-registry/CLAUDE.md
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/domain/model/Registry.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/data/model/RegistryDto.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values/strings.xml
@/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values-ro/strings.xml
@/Users/victorpop/ai-projects/gift-registry/app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelEventTimeTest.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt

<investigation_findings>
Pre-flight scan confirms the following backend wiring ALREADY EXISTS:

1. `Registry` domain model (domain/model/Registry.kt:11) — `description: String? = null` ✓
2. `RegistryDto` (data/model/RegistryDto.kt:11) — `description: String? = null` ✓
3. `RegistryRepositoryImpl.toDomain` (data/registry/RegistryRepositoryImpl.kt:89) — propagates description ✓
4. `RegistryRepositoryImpl.toMap` (line 111) — writes "description" to Firestore on create ✓
5. `RegistryRepositoryImpl.toUpdateMap` (line 123) — writes "description" on update ✓
6. `CreateRegistryViewModel.description: MutableStateFlow<String>` (line 57) — exists ✓
7. `CreateRegistryViewModel` edit-mode hydration (line 105) — `description.value = registry.description ?: ""` ✓
8. `CreateRegistryViewModel.onSave` (line 199) — `description = description.value.ifBlank { null }` ✓
9. i18n strings — `registry_description_label` and `registry_description_hint` exist in BOTH `values/strings.xml` (lines 77-78) and `values-ro/strings.xml` (lines 77-78) ✓
10. Web fallback — `web/src/lib/firestore-mapping.ts` already maps description, `web/src/features/registry/RegistryHeader.tsx:60` already renders it ✓ (out of scope for this task; already covered)

The ONLY gap: `CreateRegistryScreen.kt` does not render an OutlinedTextField for description. The field is read by the VM and round-tripped to Firestore but invisible to the user.

This is a UI-only wiring fix. No domain, data, mapper, or string-resource changes needed.
</investigation_findings>

<interfaces>
From CreateRegistryViewModel.kt:
```kotlin
class CreateRegistryViewModel @Inject constructor(...) : ViewModel() {
    val title = MutableStateFlow("")
    val description = MutableStateFlow("")          // ← already exists, needs UI binding
    val eventLocation = MutableStateFlow("")
    // ... existing onSave() already handles description.value.ifBlank { null }
}
```

From values/strings.xml (English) and values-ro/strings.xml (Romanian) — already present:
```xml
<string name="registry_description_label">Description</string>          <!-- en: -->
<string name="registry_description_hint">Optional details for your guests</string>
<string name="registry_description_label">Descriere</string>             <!-- ro: -->
<string name="registry_description_hint">Detalii optionale pentru invitati</string>
```

Multi-line OutlinedTextField pattern (precedent: AddItemScreen.kt:362-372):
```kotlin
OutlinedTextField(
    value = notes,
    onValueChange = { viewModel.notes.value = it },
    label = { Text(stringResource(R.string.item_notes_label)) },
    placeholder = { Text(stringResource(R.string.item_notes_hint_detail)) },
    shape = shapes.radius12,
    minLines = 2,
    maxLines = 4,
    modifier = Modifier.fillMaxWidth(),
    colors = giftMaisonFieldColors(),
)
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add Description OutlinedTextField + pin round-trip test</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt,
    app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt
  </files>
  <behavior>
    - Test 1 (RED first): Setting `viewModel.description.value = "Some note"` then calling `onSave()` produces a `Registry` whose `description == "Some note"` passed to `CreateRegistryUseCase`.
    - Test 2: Empty description (`""`) on save produces `Registry.description == null` (the existing `ifBlank { null }` contract — must not regress).
    - Test 3: Edit-mode hydration: when `observeRegistryUseCase` returns a Registry with `description = "Existing"`, after init the VM's `description.value == "Existing"`.

    UI behavior (manual verify in Task 2 of <verification> below):
    - Description field renders BELOW the Place (eventLocation) field, inside the same form-fields Column block.
    - Multi-line: `minLines = 3, maxLines = 5` (per brief: "3-5 lines visible by default"; sized larger than the AddItemScreen 2/4 precedent because the brief calls for a roomier multi-line input).
    - Optional: empty submission must succeed (no validation gate). Existing `onSave()` already enforces `title.length` 3..50 only — DO NOT add description length validation that blocks save.
    - 500-char cap applied at input via `onValueChange = { viewModel.description.value = it.take(500) }`. This is a soft client cap; the Firestore string field has no schema limit.
    - Label: `R.string.registry_description_label` (already exists in en + ro).
    - Placeholder: `R.string.registry_description_hint` (already exists in en + ro).
    - Use the existing `giftMaisonFieldColors()` helper (defined at the bottom of CreateRegistryScreen.kt) — same colors as Title/Date/Location fields.
    - Use `shape = shapes.radius12` (matches sibling fields).
    - Do NOT pass `singleLine = true` — multi-line requires the default `singleLine = false`.
  </behavior>
  <action>
    **Step 1 — Write the failing test (RED).** Create `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt`. Mirror the 8-arg constructor + SavedStateHandle setup from `CreateRegistryViewModelEventTimeTest.kt` (already loaded in context). Use mockk for all dependencies. Three test methods:

    ```kotlin
    @Test
    fun `onSave passes typed description through to CreateRegistryUseCase`() = runTest {
        val captured = slot<Registry>()
        val createUseCase = mockk<CreateRegistryUseCase> {
            coEvery { invoke(capture(captured)) } returns Result.success("new-id")
        }
        val vm = buildViewModel(createRegistryUseCase = createUseCase)
        vm.title.value = "My Wedding"
        vm.description.value = "Sunday brunch at Grand Hotel"
        vm.onSave()
        advanceUntilIdle()
        assertEquals("Sunday brunch at Grand Hotel", captured.captured.description)
    }

    @Test
    fun `onSave maps blank description to null`() = runTest { /* assert captured.description == null */ }

    @Test
    fun `edit mode hydrates description from observed registry`() = runTest {
        val observed = mockk<ObserveRegistryUseCase> {
            every { invoke("reg-1") } returns flowOf(
                Registry(id = "reg-1", ownerId = "uid-1", title = "T", occasion = "wedding", description = "Existing note")
            )
        }
        val vm = buildViewModel(observeRegistryUseCase = observed, registryId = "reg-1")
        advanceUntilIdle()
        assertEquals("Existing note", vm.description.value)
    }
    ```

    Run: `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.create.CreateRegistryViewModelDescriptionTest"`. Test 1 and Test 3 should FAIL or compile-fail (the field is unbound in UI but VM logic already exists — Tests 1, 2 should actually PASS immediately because the VM wiring is complete; Test 3 should also PASS since edit-mode hydration is already wired). NOTE: Because the VM-side wiring is already complete from earlier phases, all three tests will likely go GREEN immediately. That is correct — these tests are regression pins, locking the existing contract so future refactors cannot silently break it. If any test fails, fix the VM (do not weaken the test).

    **Step 2 — Add the UI field (GREEN for the user-visible behavior).** Edit `CreateRegistryScreen.kt`. Find the form-fields Column block that currently contains Title → Date/Time row → Place (eventLocation). Append a new OutlinedTextField AFTER the Place field, BEFORE the closing `}` of that Column:

    ```kotlin
    // Description field (quick-260510-noi)
    val description by viewModel.description.collectAsStateWithLifecycle()
    OutlinedTextField(
        value = description,
        onValueChange = { viewModel.description.value = it.take(500) },
        label = { Text(stringResource(R.string.registry_description_label)) },
        placeholder = { Text(stringResource(R.string.registry_description_hint)) },
        shape = shapes.radius12,
        minLines = 3,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        colors = giftMaisonFieldColors(),
    )
    ```

    Place the `val description by viewModel.description.collectAsStateWithLifecycle()` declaration near the other `val ... by viewModel.X.collectAsStateWithLifecycle()` declarations at the top of the composable (around line 109-117 alongside `title`, `occasion`, `eventLocation`, etc.) for consistency — NOT inline in the Column.

    Do NOT add new imports for OutlinedTextField, stringResource, Modifier — already imported. Do NOT change the existing `singleLine = true` on the Title or Place fields. Do NOT add length validation that blocks save (description must remain optional).

    **Step 3 — Verify build + tests pass.** See <verify> below.

    **Why no DTO/mapper/strings work:** Pre-flight scan (see <investigation_findings> in context) confirmed Registry.description, RegistryDto.description, toDomain/toMap/toUpdateMap, ViewModel field + onSave wiring, and BOTH en + ro string resources all already exist. The web fallback (`web/src/lib/firestore-mapping.ts` and `RegistryHeader.tsx`) already reads and renders description — no web work needed.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.create.CreateRegistryViewModelDescriptionTest" :app:compileDebugKotlin</automated>
  </verify>
  <done>
    - `CreateRegistryViewModelDescriptionTest` passes all 3 tests (round-trip non-empty, blank→null, edit-mode hydration).
    - `:app:compileDebugKotlin` succeeds (proves the new OutlinedTextField + collectAsStateWithLifecycle binding compile against the existing imports in CreateRegistryScreen.kt).
    - `CreateRegistryScreen.kt` contains a multi-line OutlinedTextField bound to `viewModel.description` placed after the eventLocation field.
    - No changes to RegistryDto, Registry, RegistryRepositoryImpl, or strings.xml files (the existing wiring already handles the field).
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Manual verification — description field renders, persists, and localizes</name>
  <files>app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt</files>
  <action>
    Human verifies on a real device/emulator that Task 1's UI binding behaves as specified — see <how-to-verify> for the exact steps. No code changes in this task; this is a gate that confirms the field renders, persists, hydrates on edit, and localizes correctly before the quick task is closed.
  </action>
  <what-built>
    Description input added to the Android Add Registry form. Multi-line text area (3-5 lines visible) labeled "Description" / "Descriere" with placeholder "Optional details for your guests" / "Detalii optionale pentru invitati". Persists to `registries/{id}.description` in Firestore. Empty submission is valid.
  </what-built>
  <how-to-verify>
    1. Run the app on a device/emulator: `./gradlew :app:installDebug` then launch.
    2. Sign in (any auth method).
    3. Tap the FAB / "Create registry" entry point to open the Add Registry form.
    4. Verify a "Description" multi-line field appears below the "Event Location" / "Place" field.
       - Field shows ~3 lines of vertical space by default (minLines=3).
       - Placeholder text "Optional details for your guests" visible when empty.
    5. Type a description (e.g., "Sunday brunch reception, smart casual dress code").
    6. Fill in title + occasion (required for Save). Tap "Create registry" CTA.
    7. After redirect to Registry detail, navigate back into Edit mode (pencil/edit icon).
    8. Confirm the typed description is loaded into the field (edit-mode hydration works).
    9. Switch device language to Romanian (Settings → Languages → Romanian) or via in-app locale switcher if exposed.
    10. Re-open Add/Edit Registry. Confirm the field label reads "Descriere" and placeholder reads "Detalii optionale pentru invitati".
    11. Optional: open Firebase Console → Firestore → `registries/{id}` and confirm the `description` field contains the typed string. Empty submission stores the field as missing/null (do NOT submit blank as a separate test if validation logic seems to require it — title still requires 3..50 chars; empty description must not block save).

    Edge cases worth touching:
    - Type >500 chars (e.g., paste a long block) — input should stop at 500 (soft cap via `it.take(500)`).
    - Submit with description empty + valid title + occasion — should succeed (description is optional).
  </how-to-verify>
  <verify>Manual on-device check per <how-to-verify> steps. User responds with approval or issue list.</verify>
  <done>User has typed "approved" after confirming: (a) description field visible below Place, (b) typed text persists to Firestore, (c) edit-mode hydrates, (d) Romanian label "Descriere" renders when locale is RO, (e) 500-char soft cap holds, (f) empty description does not block save.</done>
  <resume-signal>Type "approved" or describe issues</resume-signal>
</task>

</tasks>

<verification>
Phase-level checks:
- [ ] `:app:testDebugUnitTest` for `CreateRegistryViewModelDescriptionTest` passes (3/3 tests).
- [ ] `:app:compileDebugKotlin` succeeds (no UI binding errors).
- [ ] Manual verify checkpoint approved by user (description visible, persists, edit-mode hydrates, ro localization renders).
- [ ] No changes outside the two files declared in `files_modified`.
- [ ] No regressions: `CreateRegistryViewModelCoverTest` and `CreateRegistryViewModelEventTimeTest` still pass.
</verification>

<success_criteria>
- Owner can type an optional multi-line description (≤500 chars) on the Add/Edit Registry form.
- Description persists to `registries/{id}.description` in Firestore.
- Edit mode hydrates the existing description into the field.
- Both English (`values/strings.xml`) and Romanian (`values-ro/strings.xml`) labels render — using the pre-existing `registry_description_label` and `registry_description_hint` resources.
- Empty description does not block save (optional field).
- Web fallback is already covered by existing `firestore-mapping.ts` + `RegistryHeader.tsx` — no web changes needed (verified during investigation).
- Regression test pins the empty→null mapping and the edit-mode hydration contract.
</success_criteria>

<output>
After completion, create `.planning/quick/260510-noi-add-description-field-to-android-add-reg/260510-noi-SUMMARY.md` summarizing:
- Files changed (2: CreateRegistryScreen.kt + new test file)
- Why no DTO/mapper/strings work was needed (pre-existing wiring from prior phases)
- The UI binding pattern used (collectAsStateWithLifecycle + take(500) soft cap)
- Test pin contracts established
- Web fallback status (already covered by existing code, verified via grep)
</output>
