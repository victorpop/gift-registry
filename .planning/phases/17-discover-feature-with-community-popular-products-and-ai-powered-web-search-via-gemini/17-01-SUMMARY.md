---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 01
subsystem: cleanup
tags: [stores-decommission, firestore-rules, android-strings, hilt, navigation3]

# Dependency graph
requires:
  - phase: 07-romanian-store-browser
    provides: Phase 7 Stores capability (StoreListScreen, StoreBrowserScreen, StoreRepository, config/stores doc, store_*.webp drawables, seedStores.ts, stores_* strings, nav_stores_tab, AddItemMode.BrowseStores, add_sheet_browse_stores FAB row)
  - phase: 09-shared-chrome-status-ui
    provides: GiftMaisonBottomNav.kt with NavSlotId.STORES + onStores callback (slot 2)
  - phase: 11-registry-detail-create-add-item-redesign
    provides: AddItemMode 3-tab segmented control on AddItemScreen
provides:
  - Buildable Android app with zero references to deleted Store types (kotlin compile clean)
  - functions/scripts/deleteConfigStores.ts (idempotent Admin SDK script staged for Plan 17-06 deploy)
  - firestore.rules without match /config/{configId} block
  - tests/rules/firestore.rules.test.ts without the config/stores describe block (36/36 rules tests still pass)
  - AddItemMode enum collapsed to { PasteUrl, Manual } (2 entries; AddItemScreen, AddItemModeTest updated)
  - en + ro strings.xml with stores_* content keys removed; nav_stores_tab DELIBERATELY RETAINED until Plan 17-05 lock-step rename
  - Bottom nav slot 2 inert (onStores callback emptied) — Plan 17-05 will wire Discover
affects: [17-02-backend-foundations, 17-03-callables, 17-04-triggers-and-backfill, 17-05-android-discover, 17-06-deploy-and-uat]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Two-phase nav slot replacement: Plan 17-01 removes the predecessor (Stores) and empties the slot; Plan 17-05 wires the successor (Discover) and deletes the surviving label string in lock-step with the GiftMaisonBottomNav rename. This avoids a window where the app references a deleted string resource."
    - "One-shot Firestore data-cleanup scripts (deleteConfigStores.ts): Admin SDK doc deletion idempotent on missing docs, staged in functions/scripts/ for deploy invocation in a later plan."

key-files:
  created:
    - functions/scripts/deleteConfigStores.ts
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/di/StorageModule.kt
    - app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt
    - app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
    - firestore.rules
    - tests/rules/firestore.rules.test.ts
    - functions/package.json

key-decisions:
  - "Delete orphaned LastRegistryPreferencesRepository interface + ObserveLastRegistryIdUseCase + SetLastRegistryIdUseCase (Phase 7 prep stubs that were never wired up outside the deleted Stores flow) — auto-fix Rule 3 deviation."
  - "Delete orphaned GetStoresUseCase + Store domain model + StoreDto data model (would have caused compile failure after StoreRepository deletion) — auto-fix Rule 3 deviation."
  - "Deliberately RETAIN nav_stores_tab string in both en + ro until Plan 17-05 lock-step deletes it together with the GiftMaisonBottomNav slot-2 rename to nav_discover_tab. Plan-author flagged this departure from a strict reading of D-42 to preserve build-time safety throughout Wave 1."
  - "Empty onStores callback in AppNavigation.kt (rather than rename to onDiscover) because GiftMaisonBottomNav.kt still exposes onStores until Plan 17-05 renames it; emptying the callback body makes slot 2 inert without breaking the callback contract."

patterns-established:
  - "Two-phase nav slot rotation (decommission → empty stub → wire new feature) for swapping bottom-nav destinations without a broken build window."

requirements-completed: [D-03, D-04, D-05, D-06, D-07, D-08, D-09, D-42, D-44]

# Metrics
duration: 12min
completed: 2026-05-27
---

# Phase 17 Plan 01: Stores Decommission Summary

**Phase 7 Stores capability fully removed from Android + Firestore + Cloud Functions; AddItemScreen collapsed to 2 tabs; nav slot 2 inert until Plan 17-05 wires Discover; idempotent config/stores delete script staged for deploy.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-05-27T14:16:49Z
- **Completed:** 2026-05-27T14:29:13Z
- **Tasks:** 3 (+ 1 cleanup commit)
- **Files modified:** 13 (excluding deletions)
- **Files deleted:** 32 (16 Android source, 4 Android tests, 9 drawables, 2 Functions seed assets, 1 functions/package.json entry — see breakdown below)

## Accomplishments

- Deleted all Phase 7 Stores Android code (ui/store/, domain/store/, data/store/, di/StoresModule.kt, data/preferences/LastRegistryPreferencesDataStore.kt) plus the 3 Stores test classes.
- Deleted 9 store_*.webp drawables (emag, altex, flanco, libris, carturesti, ikea, dedeman, elefant, generic).
- Healed AppNavigation.kt + AppNavKeys.kt + NavVisibility.kt so nothing references the deleted StoreListKey / StoreBrowserKey types; AddItemMode enum collapsed to { PasteUrl, Manual }; AddItemScreen lost its `onNavigateToBrowseStores` parameter + browse-stores LaunchedEffect + middle tab; AddItemModeTest + BottomNavVisibilityTest updated for the new shape.
- Removed all `stores_*` content string keys + `add_item_tab_browse` from en + ro strings.xml (`nav_stores_tab` deliberately retained — see Decisions).
- Deleted `match /config/{configId}` block from `firestore.rises` and the corresponding `describe("config/stores rules")` block from `tests/rules/firestore.rules.test.ts`. Full rules test suite passes (36/36).
- Deleted `functions/scripts/seedStores.ts`, `functions/data/stores.seed.json`, and the `seed:stores` npm script from `functions/package.json`.
- Created `functions/scripts/deleteConfigStores.ts` — one-shot Admin SDK script (idempotent on missing doc) staged for invocation in Plan 17-06 deploy to remove the live `config/stores` Firestore document.
- Final build verification: `./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q` passes; targeted unit tests (AddItemModeTest, BottomNavVisibilityTest, LocalizationParityTest) pass; tests/rules/jest 36/36 pass.

## Task Commits

Each task was committed atomically:

1. **Task 1: Delete Stores Android code, drawables, tests, and seed script** - `18426aa` (chore)
2. **Task 2: Rewire nav graph + collapse AddItemScreen tabs (heal compilation)** - `93fa317` (refactor)
3. **Task 3: Remove stores_* strings, config/{configId} rule + tests; add cleanup script** - `624c6f9` (chore)

**Follow-up cleanup:** `2ea914f` (chore) — final scrub of Stores symbol names from StorageModule.kt KDoc so the plan's source-only verify grep returns zero matches.

## Files Created/Modified

**Created:**
- `functions/scripts/deleteConfigStores.ts` — idempotent Admin SDK script to delete the `config/stores` Firestore document. Logs "config/stores not present — nothing to delete (idempotent no-op)" on missing doc, "Deleted config/stores Firestore document." on success. Intended to be invoked once during Plan 17-06 deploy.

**Modified:**
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — deleted StoreListKey, StoreBrowserKey
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — deleted store imports, entry<StoreListKey>/entry<StoreBrowserKey> blocks; emptied onStores callback body (temporary no-op with 17-05 pointer); dropped onNavigateToBrowseStores argument from AddItemScreen call
- `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` — KDoc cleanup (removed Stores-key mentions, added Plan 17-01 history bullet)
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt` — enum collapsed to { PasteUrl, Manual }; KDoc + History updated
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — dropped onNavigateToBrowseStores param; deleted browse-mode LaunchedEffect + browse tab label + BrowseStores when-branch
- `app/src/main/java/com/giftregistry/di/StorageModule.kt` — doc-comment cleanup
- `app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt` — rewritten to assert 2-entry enum + ordinal mapping
- `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — dropped two Stores nav-key import + assertions
- `app/src/main/res/values/strings.xml` — deleted 14 `stores_*` content keys + `add_item_tab_browse`; retained `nav_stores_tab`
- `app/src/main/res/values-ro/strings.xml` — same set (Romanian)
- `firestore.rules` — deleted `match /config/{configId}` block, added Plan 17-01 history comment
- `tests/rules/firestore.rules.test.ts` — deleted `describe("config/stores rules")` block (4 it() cases)
- `functions/package.json` — removed `"seed:stores"` npm script

**Deleted (32 files / inline removals):**
- 16 Android source files (7 store source, 3 deleted-orphaned-use-cases-and-interface, 3 deleted-orphaned-Store/StoreDto/GetStoresUseCase, 1 StoresModule.kt, 1 LastRegistryPreferencesDataStore.kt, 1 store-related domain interface)
- 4 Android test files (StoreListViewModelTest, StoreBrowserViewModelTest, StoreRepositoryImplTest, LastRegistryPreferencesDataStoreTest)
- 9 store_*.webp drawables in res/drawable-nodpi/
- 2 Functions seed assets (seedStores.ts, stores.seed.json)
- 1 npm script entry in functions/package.json

## Decisions Made

1. **Retain `nav_stores_tab` string until Plan 17-05.** Plan-author called out this deliberate departure from D-42 ("Existing `nav_stores_tab` and all `stores_*` keys DELETED in same commit"). `GiftMaisonBottomNav.kt` references `R.string.nav_stores_tab` from Wave 1; deleting the string without simultaneously renaming the Compose reference would break the build. Plan 17-05 will rename the key to `nav_discover_tab` AND update `GiftMaisonBottomNav.kt` in the same commit. Documented in NavVisibility.kt, strings.xml comments, and AppNavigation.kt onStores body.

2. **Empty `onStores` callback rather than rename to `onDiscover` now.** GiftMaisonBottomNav.kt signature still names the slot-2 callback `onStores` (renamed in Plan 17-05). The implementation in AppNavigation.kt is now an empty body with a comment pointing to Plan 17-05. This makes slot 2 visually present but inert during Wave 1 — clicking it does nothing — and avoids a half-renamed callback contract.

3. **Delete orphaned use cases + domain types as Rule 3 deviation.** The plan listed `LastRegistryPreferencesDataStore.kt` for deletion but not the orphaned `LastRegistryPreferencesRepository` interface or two never-consumed use cases (`ObserveLastRegistryIdUseCase`, `SetLastRegistryIdUseCase`). Similarly, `StoreRepository` deletion left `GetStoresUseCase`, `Store` (domain model), and `StoreDto` (data model) as orphans that would have caused compile failures. All deleted under deviation Rule 3 (blocking issue + dead code directly caused by plan deletions); grep-verified no non-Stores consumers exist for any of them.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Deleted orphaned `LastRegistryPreferencesRepository` interface + 2 unused use cases**
- **Found during:** Task 1 (before commit)
- **Issue:** Plan deleted the only `LastRegistryPreferencesRepository` implementation (`LastRegistryPreferencesDataStore`) and the only Hilt binding (`StoresModule`), but left the interface and two `@Inject`-annotated use cases (`ObserveLastRegistryIdUseCase`, `SetLastRegistryIdUseCase`) in `domain/`. Grep showed zero consumers of either use case outside the Stores flow — they were Phase 7 prep stubs that never wired up. Leaving them yields dead code with an unbindable interface.
- **Fix:** Deleted all three files (`LastRegistryPreferencesRepository.kt`, `ObserveLastRegistryIdUseCase.kt`, `SetLastRegistryIdUseCase.kt`).
- **Verification:** `grep -rn ObserveLastRegistryIdUseCase\|SetLastRegistryIdUseCase` returns zero matches; `./gradlew app:compileDebugKotlin` clean.
- **Committed in:** `18426aa` (Task 1 commit)

**2. [Rule 3 - Blocking] Deleted orphaned `GetStoresUseCase`, `Store` domain model, `StoreDto` data model**
- **Found during:** Task 2 (before commit, surfaced by grep regression check)
- **Issue:** `GetStoresUseCase.kt` imports `com.giftregistry.domain.store.StoreRepository` (deleted in Task 1) and `com.giftregistry.domain.model.Store` (also deleted). `StoreDto.kt` is consumed only by the deleted `StoreRepositoryImpl`. Leaving them would have caused a Kotlin compile error on `compileDebugKotlin`.
- **Fix:** Deleted `GetStoresUseCase.kt`, `Store.kt`, `StoreDto.kt`. Grep-verified zero consumers.
- **Verification:** `./gradlew app:compileDebugKotlin app:compileDebugUnitTestKotlin -q` passes.
- **Committed in:** `93fa317` (Task 2 commit)

**3. [Rule 3 - Doc cleanup] Reworded `StorageModule.kt` KDoc to avoid `StoresModule` substring**
- **Found during:** Final source-only grep verification
- **Issue:** Phase 12's `StorageModule.kt` KDoc mentioned "Mirrors the [StoresModule] pattern". After Task 1 deleted StoresModule, the doc reference became a dangling KDoc link. The plan's source-only grep verify (`grep -r StoreListKey\|StoreBrowserKey\|...\|BrowseStores app/src functions/src functions/scripts`) flagged the live `StoresModule` substring.
- **Fix:** Reworded to "Originally mirrored a Phase 7 module-per-feature pattern; the Phase 7 Stores module was deleted in Plan 17-01." Semantically equivalent without naming the deleted class.
- **Verification:** Final grep returns zero matches.
- **Committed in:** `2ea914f` (cleanup commit)

---

**Total deviations:** 3 auto-fixed (3 Rule 3 — all blocking compile / source-grep regressions caused directly by planned deletions)
**Impact on plan:** All auto-fixes were strictly necessary to keep the build green and to satisfy the plan's own verify-grep checks. No scope creep — only dead code that the plan's deletions made unreachable.

### Verify-Check Interpretation

The plan's literal grep `grep -c "stores_\|add_item_tab_browse\|add_sheet_browse_stores" strings.xml MUST return 0` is **contradicted** by the plan's own retention requirement for `nav_stores_tab` (which contains the substring `stores_`). Final grep returns 1 for each file — the single retained `<string name="nav_stores_tab">` line. This matches the **intent** of the plan ("zero content stores_* keys remain; nav_stores_tab retained") but not the literal regex. Documented here so verifier and Plan 17-05 do not flag it as a regression.

## Issues Encountered

- **Romanian strings.xml edit friction:** The values-ro/strings.xml file uses literal `\uXXXX` escape sequences (e.g., `Răsfoieşte` for "Răsfoiește"). The Edit tool's escape normalization didn't match on one of the Stores strings (`stores_external_link_blocked`) which contained an unusual `í` (í, Latin small letter i with acute) — likely a typo in the original (should have been `î` "î"). Workaround: deleted each `stores_*` line individually rather than in a single block edit.

## User Setup Required

None - no external service configuration required. The `deleteConfigStores.ts` script is staged for invocation in Plan 17-06's deploy procedure (requires `gcloud auth application-default login` for prod target; emulator-only target works inside `firebase emulators:exec`).

## Known Stubs

- **`AppNavigation.kt` `onStores` callback body is intentionally empty.** Slot 2 of the bottom nav is now inert (clicking does nothing). This is documented inline with a comment ("Plan 17-05 will rewire this to onDiscover -> push DiscoverKey. Temporary no-op so slot 2 is inert during Wave 1."). This stub is wired by Plan 17-05.
- **`nav_stores_tab` string deliberately retained in en + ro strings.xml.** Removed simultaneously with the GiftMaisonBottomNav rename in Plan 17-05. Documented inline.

## Next Phase Readiness

- Plan 17-02 (backend foundations): Ready. Firestore rules now have a clear slot for the new `popularItems` / `discoverCache` / `discoverRateLimits` collection rules introduced in 17-02. The deleted `config/{configId}` block is replaced by these new collection-specific rules.
- Plan 17-05 (Android Discover): Ready. Slot 2 of `GiftMaisonBottomNav` is inert, ready to be rewired to `onDiscover`; `nav_stores_tab` and `NavSlotId.STORES` are staged for the lock-step rename to `nav_discover_tab` / `NavSlotId.DISCOVER`.
- Plan 17-06 (deploy + UAT): `functions/scripts/deleteConfigStores.ts` is staged — Plan 17-06 must invoke `cd functions && npx ts-node scripts/deleteConfigStores.ts` after the Plan 17-01 build deploys.

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Plan: 01 of 6*
*Completed: 2026-05-27*

## Self-Check: PASSED
