---
status: resolved
trigger: "Registry tiles on the Android home screen always show 0 for items / reserved / given counts across all registries, instead of the real values."
created: 2026-05-30T00:00:00Z
updated: 2026-05-30T12:00:00Z
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: CONFIRMED — statsLine() in RegistryCard.kt hardcodes 0 for all three counts. No count data flows through the domain model (Registry has no itemCount/reservedCount/givenCount fields) or the ViewModel. Items live in a subcollection and are never loaded on the list screen. Fix: add RegistryStats data class, load items per registry in RegistryListViewModel via ObserveItemsUseCase, pass counts to the card composable.
test: code trace complete — reading all layers confirmed the issue
expecting: fix to resolve the zeros
next_action: implement fix across RegistryCard, RegistryListViewModel, and RegistryListScreen

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: Each registry tile on the home screen displays the correct counts for items, reserved, and given on that registry.
actual: All three counters (items, reserved, given) display 0 on every tile, on every registry.
errors: None reported.
reproduction: Launch the Android app, sign in, view the home screen showing the list of registries.
started: Never worked — original implementation gap, not a regression.

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: Firestore Registry document has aggregate count fields that aren't being read
  evidence: RegistryDto and Registry domain model have no itemCount/reservedCount/givenCount fields. No such fields in Firestore schema.
  timestamp: 2026-05-30

- hypothesis: Cloud Function maintains counter fields on the registry document
  evidence: No Cloud Function writes count fields to registries/ — functions only write to items subcollection. RegistryRepositoryImpl.toMap() also doesn't include any count fields.
  timestamp: 2026-05-30

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-05-30
  checked: RegistryCard.kt lines 202-213
  found: statsLine() is a private @Composable that hardcodes stringResource(R.string.home_stats_items, 0), stringResource(R.string.home_stats_reserved, 0), stringResource(R.string.home_stats_given, 0). No parameters taken, no data bound. Comment at line 202 says "Phase 10 renders zeros. Follow-up: Firestore doc-level counts or per-card Flow observation."
  implication: The zero is explicit and intentional deferral. Not a bug in data binding — the composable never accepted real data.

- timestamp: 2026-05-30
  checked: Registry domain model, RegistryDto
  found: Neither has itemCount, reservedCount, or givenCount fields. No aggregates are persisted in Firestore.
  implication: Counts must be computed client-side from the items subcollection.

- timestamp: 2026-05-30
  checked: FirestoreDataSource.observeItems — items live at registries/{id}/items subcollection. ItemStatus enum has AVAILABLE, RESERVED, PURCHASED.
  found: items are loaded per-registryId via observeItems(registryId). ItemRepositoryImpl and ObserveItemsUseCase are already wired.
  implication: Fix path is to load items for each registry on the list screen and compute counts client-side.

- timestamp: 2026-05-30
  checked: RegistryListViewModel
  found: Only loads List<Registry> via ObserveRegistriesUseCase. Never loads items. RegistryListUiState.Success holds only registries: List<Registry>.
  implication: ViewModel needs to additionally combine items flows per registry into count maps.

- timestamp: 2026-05-30
  checked: RegistryListScreen
  found: Passes registry object directly to RegistryCardPrimary/Secondary. totalItems on line 141 also hardcoded to 0 with "deferred" comment.
  implication: Screen also needs to pass counts through to card composables.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause: statsLine() in RegistryCard.kt hardcoded 0 for all three counts (items/reserved/given). This was an intentional deferral from Phase 10 — the comment said "per-registry stat aggregation is deferred". No count data existed in the Registry domain model or Firestore registry documents; items live in a subcollection. Nothing in RegistryListViewModel loaded items for the list screen, so counts were never populated.
fix: |
  1. Added RegistryCounts data class to RegistryListViewModel.kt (items/reserved/given Int fields).
  2. Expanded RegistryListUiState.Success to carry a counts: Map<String, RegistryCounts> field.
  3. RegistryListViewModel.init now combines a per-registry ObserveItemsUseCase flow for every registry in the list, computing items=size, reserved=RESERVED count, given=PURCHASED count client-side. Uses kotlinx.coroutines.flow.combine(List<Flow>) so counts stay live with Firestore real-time updates.
  4. RegistryCard.kt: statsLine() now takes a RegistryCounts parameter and passes the real values to string resources (which already had %1$d format parameters).
  5. RegistryCard.kt: RegistryCardPrimary and RegistryCardSecondary each gained a counts: RegistryCounts = RegistryCounts() parameter.
  6. RegistryListScreen.kt: passes counts[registry.id] ?: RegistryCounts() to each card. Also fixed the hardcoded totalItems = 0 in the headline caption to counts.values.sumOf { it.items }.
verification: BUILD SUCCESSFUL and on-device verified against production Firebase. User confirmed all three counters (items, reserved, given) display correct real-time values on every registry tile. Secondary incident during verification: initial installDebug used default use_emulator=true, causing empty registry list (local emulator has different seed data). Re-installed with -Puse_emulator=false to match prior task environment — counts displayed correctly.
files_changed:
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
