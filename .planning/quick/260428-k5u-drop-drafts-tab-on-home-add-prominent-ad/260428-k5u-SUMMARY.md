---
phase: quick-260428-k5u
plan: 01
subsystem: ui-registry-home + ui-registry-detail
tags: [home, segmented-tabs, registry-detail, cta, localization, drop-drafts]
provides:
  - "2-tab Home segmented control (ACTIVE | PAST) — DRAFTS dropped"
  - "Primary accent-filled 'Add an item' CTA above items list on Registry Detail"
  - "registry_detail_add_item_cta string in EN + RO"
requires:
  - "quick-260428-iny: AddItemKey(registryId, fromAddSheet=false) → registry picker hidden contract"
  - "quick-260427-lwz/n67/nkn: bottom nav layout (preserved, untouched)"
  - "quick-260427-gxu/lnq: auth headline + serif fonts (preserved, untouched)"
affects:
  - "Home (RegistryListScreen) tab control + filter logic"
  - "Registry Detail (RegistryDetailScreen) LazyColumn item ordering"
  - "Style guide preview (SegmentedTabsPreview)"
removed:
  - "Registry.isDraft(itemCount: Int) helper in TabFilters.kt"
  - "DraftHeuristicTest.kt (5 @Test methods)"
  - "home_tab_drafts + home_empty_drafts in EN + RO"
  - "DRAFTS preview row in StyleGuidePreview SegmentedTabsPreview"
key-files:
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt (-5/+1)"
    - "app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt (-13/+0)"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt (+55/-3)"
    - "app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt (-3/+0)"
    - "app/src/main/res/values/strings.xml (-2/+3)"
    - "app/src/main/res/values-ro/strings.xml (-2/+3)"
  deleted:
    - "app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt"
decisions:
  - "DRAFTS tab removed wholesale (heuristic + tests + strings + preview) per CLAUDE.md no-backwards-compat-hacks guidance — no flag/feature gate"
  - "rememberSaveable selectedTabIndex left unmigrated: a saved Int=1 (DRAFTS) post-upgrade now maps to PAST — acceptable migration behaviour for a quick fix per plan"
  - "AddItemTopCta defined as file-private composable in RegistryDetailScreen.kt — no project-wide PrimaryButton/AccentButton helper exists; one-off file scope is appropriate"
  - "CTA placement: between FilterChipsRow and items list (LazyColumn item key='add-item-cta') — keeps filter chips as visual section header for items, CTA scrolls naturally with rest of screen"
  - "Romanian translation matches add_sheet_add_item byte-for-byte: 'Adaugă un produs' — keeps tone consistent between bottom-nav add sheet and per-registry CTA"
metrics:
  duration: 5min
  tasks_completed: 2 (of 3 — Task 3 is human-verify checkpoint, see Outstanding section)
  files_changed: 6 modified + 1 deleted
  completed: 2026-04-28
---

# Quick Task 260428-k5u: Drop DRAFTS Tab on Home + Add Prominent "Add an Item" CTA on Registry Detail Summary

Two-line product simplification: Home segmented control reduced from 3 tabs (ACTIVE | DRAFTS | PAST) to 2 tabs (ACTIVE | PAST) — the DRAFTS tab had no clear product behaviour ("client-only derivation until v1.2 ships a real Registry.status field" per CONTEXT.md). Registry Detail now exposes a primary accent-filled "Add an item" CTA between FilterChipsRow and the items list, wired through the existing-but-unused `onNavigateToAddItem` callback so the registry picker stays hidden (quick-260428-iny contract: `fromAddSheet=false`).

## What Shipped

### Task 1: Drop DRAFTS Tab — Commit `4d7ea61`

`refactor(quick-260428-k5u-01): drop DRAFTS tab — Home now ACTIVE | PAST`

**RegistryListScreen.kt** — `tabs` list literal collapsed from 3 entries to 2; the `selectedTabIndex when()` block dropped its `1 -> registries.filter { it.isDraft(itemCount = 0) }` branch (now `1 -> isPast`); the `emptyKey when()` block dropped the `home_empty_drafts` branch (now `1 -> home_empty_past` with `else` retained for exhaustiveness). `selectedTabIndex` initial value stays at 0 (ACTIVE remains the default). `rememberSaveable` left unmigrated — a previously-saved index 1 (DRAFTS) now resolves to PAST, acceptable migration for a quick fix with no DataStore-level state to clear.

**TabFilters.kt** — `Registry.isDraft(itemCount: Int)` extension function deleted entirely (KDoc + body). Top-level KDoc updated to drop the "Active / Drafts / Past" wording → "Active / Past", drop the `Draft = …` locked-decision line, and drop the `DraftHeuristicTest` cross-reference. `startOfTodayMs`, `Registry.isActive`, `Registry.isPast`, and `primaryRegistryIdOf` all unchanged.

**strings.xml (EN + RO)** — `home_tab_drafts` and `home_empty_drafts` removed in both locales. `home_tab_active`, `home_tab_past`, `home_empty_active`, `home_empty_past` preserved.

**DraftHeuristicTest.kt** — entire file deleted (5 @Test methods: `blankTitle_isDraft`, `whitespaceTitle_isDraft`, `emptyItems_isDraft`, `bothPresent_isNotDraft`, `blankTitleAndEmptyItems_isDraft`). The two surviving predicate test files (`TabFilterPredicateTest`, `IsPrimarySelectionTest`) remain untouched and pass.

**StyleGuidePreview.kt** — `SegmentedTabsPreview` `tabs` literal updated from `("ACTIVE", "DRAFTS", "PAST")` to `("ACTIVE", "PAST")`. The third `SegmentedTabs(... selectedIndex = 2 ...)` row deleted (would IndexOutOfBounds at preview-render time on a 2-entry list). Preview annotation name updated to "Segmented tabs — both selected states".

**CreateRegistryScreen.kt:139 comment** — left untouched per plan (descriptive only, no `isDraft()` call).

### Task 2: "Add an Item" CTA on Registry Detail — Commit `0cfb715` (see Deviation note below)

**values/strings.xml (EN)** — added `<string name="registry_detail_add_item_cta">Add an item</string>` with comment anchor `<!-- quick-260428-k5u: Registry Detail "Add an item" CTA -->`.

**values-ro/strings.xml (RO)** — added `<string name="registry_detail_add_item_cta">Adaugă un produs</string>` (matches `add_sheet_add_item` Romanian translation byte-for-byte).

**RegistryDetailScreen.kt** —
- New imports: `androidx.compose.foundation.clickable`, `Arrangement`, `Row`, `fillMaxWidth`, `width`, `Icons.Outlined.Add`, `androidx.compose.ui.draw.clip`.
- Stale `onNavigateToAddItem` doc comment ("retained for future inline 'add first item' affordance") replaced with `// quick-260428-k5u: wired to the in-screen "Add an item" CTA above the items list.`
- New `LazyColumn item(key = "add-item-cta")` block inserted immediately after `item(key = "filter")` and immediately before the Phase-6 confirm-purchase banner block. Calls `AddItemTopCta(onClick = onNavigateToAddItem)`.
- New file-private `AddItemTopCta` composable appended at the end of the file (after `RegistryDetailScreen`'s closing brace). Full-width Row with `fillMaxWidth`, edge padding (`spacing.edge` 16 dp horizontal, `spacing.gap8` 8 dp vertical), `clip(shapes.radius14)`, `background(colors.accent)`, `clickable(onClick)`, inner padding (`gap16` horizontal, `gap14` vertical), `Arrangement.Center`. Renders `Icons.Outlined.Add` + 8 dp Spacer + `Text(typography.bodyMEmphasis, color = colors.accentInk)`.
- AppNavigation.kt UNTOUCHED (verified via `git diff` — empty). The existing `entry<RegistryDetailKey>` already supplies `onNavigateToAddItem = { backStack.add(AddItemKey(key.registryId)) }` with `fromAddSheet=false` default → registry picker hidden per quick-260428-iny contract.

## Verification Run

```
$ ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.list.*" -q
BUILD SUCCESSFUL
```

Full-suite gate (per `<verification>` whole-plan target):

```
$ ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest -q
:app:compileDebugKotlin   → SUCCESS (zero unresolved-reference / overload errors)
:app:testDebugUnitTest    → 299 tests run, 5 failed (all in CreateRegistryViewModelCoverTest), 2 skipped
```

The 5 failures are **out-of-scope Phase 12 Plan 04 RED tests** (`CreateRegistryViewModelCoverTest` — created in commit `1e72362` `test(12-01): Wave 0 RED tests + stubs for VM/repo/storage/processor`). Per Phase 12 Plan 12-01 decision pinned in STATE.md: *"Plan 04 must satisfy via VM impl, NEVER edit the test"* — these tests are RED on purpose and flip GREEN when Phase 12 Plan 04 ships. They are not caused by k5u changes (no overlap with `RegistryListScreen`, `TabFilters`, `RegistryDetailScreen`, or any k5u-touched file). See `<deviation_rules>` SCOPE BOUNDARY: pre-existing failures in unrelated files are out of scope.

### No-regression grep gates (all pass)

```
$ grep -rn "home_tab_drafts\|home_empty_drafts" app/src
(no hits)

$ grep -rn "isDraft" app/src/main app/src/test
app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt:139:
    // Phase 10's isDraft(itemCount == 0) heuristic still classifies
(1 hit — the historical comment plan explicitly told us to leave alone)

$ grep -c "registry_detail_add_item_cta" \
    app/src/main/res/values/strings.xml \
    app/src/main/res/values-ro/strings.xml \
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
1
1
1
(3 total — exactly the expected EN + RO + composable consumer)

$ grep -rn "DRAFTS" app/src/main
(no hits)
```

### Preservation checks (no regressions to prior shipped fixes)

- **Bottom nav (260427-lwz/n67/nkn):** `GiftMaisonBottomNav.kt` UNTOUCHED — 5-slot equal-width layout, FabSlot wrap-content scaffold, no FAB lift, all preserved.
- **Add sheet → AddItem picker (260428-iny):** `AddItemKey(val registryId: String?, val fromAddSheet: Boolean = false)` contract preserved end-to-end. `AppNavigation.kt` `entry<RegistryDetailKey>` block UNCHANGED — its existing `onNavigateToAddItem = { backStack.add(AddItemKey(key.registryId)) }` (i.e. `fromAddSheet` defaults to false) is the contract our new CTA consumes; bottom-nav-sheet path still passes `fromAddSheet=true` to render the registry picker.
- **Sheet render (260427-* prior):** `AddSheetContent.kt` UNTOUCHED.
- **Auth headline (260427-gxu) + serif fonts (260427-lnq):** `AuthHeadline.kt`, `InstrumentSerifFamily.kt` UNTOUCHED.
- **Phase 6 confirm-purchase banner:** still present, still positioned AFTER the new `add-item-cta` item in the LazyColumn — gated on `hasActiveReservation && reservationId != null` exactly as before.
- **Phase 11 toolbar alpha pinning, item delete dialog, reservation flow, GuestIdentitySheet, overflow DropdownMenu, SnackbarHost overlay:** all UNTOUCHED.

## Deviations from Plan

### 1. [Rule 3 — Blocking issue/cleanup, NOT auto-fixed] Task 2 commit boundary contaminated by parallel Phase-12 agent

- **Found during:** Task 2 staging step (`git status --short` showed unexpected staged files from Phase 12 storage work).
- **Issue:** A second agent was concurrently executing Phase 12 Plan 02 storage work in the same repo. Between my `git add` of Task 2 files and the moment I attempted `git commit`, that parallel agent's `git commit` went out and absorbed my staged Task 2 changes (`RegistryDetailScreen.kt`, `values/strings.xml`, `values-ro/strings.xml`) into its own commit `0cfb715` (`feat(12-02): wire StorageDataSource + StorageRepositoryImpl + CoverImageProcessorImpl + StorageModule (D-04 / D-05 / D-06 / D-07)`).
- **Verification:** `git show 0cfb715 -- app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` confirms ALL Task 2 changes (imports, comment swap, LazyColumn item insert, `AddItemTopCta` composable) shipped verbatim. `git show 0cfb715 -- app/src/main/res/values/strings.xml` and `... values-ro/strings.xml` confirm the new `registry_detail_add_item_cta` keys shipped in both locales.
- **Resolution chosen:** Did NOT attempt to surgically rewrite `0cfb715` (would require destructive `git reset --hard` + force-rewrite of two parallel agents' work; user did not request force-rewrite, and `<git-safety-protocol>` forbids it without explicit user instruction). Task 2 is verified complete on disk and in `main` branch; the audit trail is documented here.
- **Files modified:** RegistryDetailScreen.kt, values/strings.xml, values-ro/strings.xml — all in commit `0cfb715`.
- **Effective Task 2 commit hash:** `0cfb715` (parallel agent's commit message; my Task 2 changes are the last 3 file-pairs in its diff).

No other deviations. Plan executed as written.

## Outstanding — Task 3 (Human Verification, Blocking)

Per plan, Task 3 is a `checkpoint:human-verify` gate. **All automation is complete; the verification environment is ready for emulator install.**

### How to verify

```
cd /Users/victorpop/ai-projects/gift-registry
./gradlew :app:installDebug -Puse_emulator
```

Then walk through these four checks (verbatim from PLAN section `<how-to-verify>`):

**Check 1 — Home tabs (DRAFTS gone)**
1. Launch app, sign in (or use existing session).
2. Land on Home / "Your registries" screen.
3. Confirm segmented tab control shows EXACTLY TWO chips: **ACTIVE** and **PAST**. DRAFTS chip absent.
4. Confirm ACTIVE selected by default (filled bg / active indicator on leftmost chip).
5. Tap PAST — list switches to past-events view (or "No past registries" empty state).
6. Tap ACTIVE — list returns to active registries.
7. Layout sanity: no broken/clipped chip area; tab row centered with proper edge padding (16 dp).

**Check 2 — Registry Detail CTA visible and styled correctly**
1. From Home, tap any registry card to open Registry Detail.
2. Visual order from top: Hero → Stats strip → Share banner → Filter chips row → **NEW "Add an item" CTA** → items list.
3. CTA is full-width within 16 dp page edge padding; accent yellow/amber background (matches FAB); outlined "+" icon left + "Add an item" label.
4. Tap target feels >= 48 dp tall.

**Check 3 — CTA navigates correctly with picker hidden**
1. Tap the "Add an item" CTA on Registry Detail.
2. AddItemScreen opens.
3. Registry-picker dropdown (ExposedDropdownMenuBox) at the TOP of the form must be ABSENT — only "Paste URL" / form fields visible (confirms `fromAddSheet=false` was passed correctly).
4. Form ready for input; submitting an item adds it to the registry you came from.
5. Back-navigate; registry's items list reflects the addition.

**Check 4 — Romanian locale spot-check**
1. Settings → switch app language to Romanian.
2. Home: tabs read **ACTIVE** + **TRECUTE**.
3. Open any registry → CTA reads **"Adaugă un produs"** (lowercase except first letter, diacritic on `ă`).
4. Tap CTA → AddItemScreen opens; picker hidden; form labels Romanian.

**Regression checks (do not skip):**
- Bottom nav (HOME / STORES / ADD / LISTS / YOU) — all 5 labels visible, no truncation, FAB flush.
- Bottom nav ADD button → sheet opens with exactly 2 rows (New registry + Add an item). Tap "Add an item" — registry picker dropdown IS visible at top of AddItemScreen (confirms `fromAddSheet=true` path still works).
- Auth screen headline: line 1 ink, line 2 fully accent including period. Wordmark renders in serif font immediately.

**Resume signal:** User types "approved" or describes any visual/functional issues observed.

## Self-Check: PASSED

Verified the following claims after writing this SUMMARY:

- File `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` exists and contains the 2-tab list (no `home_tab_drafts` reference).
- File `app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt` exists and no longer contains `isDraft`.
- File `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` exists and contains `AddItemTopCta` composable + `key = "add-item-cta"` LazyColumn item.
- File `app/src/main/res/values/strings.xml` contains `registry_detail_add_item_cta` and no longer contains `home_tab_drafts` or `home_empty_drafts`.
- File `app/src/main/res/values-ro/strings.xml` contains `registry_detail_add_item_cta` and no longer contains `home_tab_drafts` or `home_empty_drafts`.
- File `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` contains the 2-entry `tabs = listOf("ACTIVE", "PAST")`.
- File `app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt` is gone from disk.
- Commit `4d7ea61` exists in `git log --oneline --all` (Task 1).
- Commit `0cfb715` exists in `git log --oneline --all` (Task 2 absorbed by parallel agent — see Deviation 1).
