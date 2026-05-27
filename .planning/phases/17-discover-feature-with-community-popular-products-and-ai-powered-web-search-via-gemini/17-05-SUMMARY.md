---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 05
subsystem: android-discover-ui
tags: [android, compose, discover, hilt, firebase-functions, navigation3, bottom-nav, localization]

# Dependency graph
requires:
  - phase: 17-discover-feature
    plan: 01
    provides: Inert slot 2 of GiftMaisonBottomNav (onStores no-op + retained nav_stores_tab); DiscoverKey absent from AppNavKeys; AddItemMode collapsed to 2 entries
  - phase: 17-discover-feature
    plan: 03
    provides: discoverPopular Callable (no payload) + discoverSearch Callable (`{ query }` payload) deployed to europe-west3; D-20 + D-31 response shapes
  - phase: 09-shared-chrome-status-ui
    provides: 5-slot bottom nav with NavItemSlot + FabSlot composables; pill/accent visual tokens; existing onHome/onFab/onLists/onYou callbacks
  - phase: 08-giftmaison-design-foundation
    provides: GiftMaisonColors (paperDeep, accentSoft, accent, accentInk, inkFaint, line), GiftMaisonTypography (bodyL/bodyM/bodyMEmphasis/monoCaps), shapes.pill/radius16

provides:
  - DiscoverProduct domain model + DiscoverRepository interface + DiscoverRepositoryImpl (Hilt-injected, wraps both Callables, returns Result<List<DiscoverProduct>>)
  - DiscoverModule (Hilt @Binds DiscoverRepository <- DiscoverRepositoryImpl; FirebaseFunctions reused from existing AppModule)
  - PopularState + SearchState sealed interfaces (Loading | Loaded | Empty | Error, SearchState also Idle)
  - DiscoverViewModel (@HiltViewModel, popular + search + searchQuery StateFlows; loadPopular auto-fires from init; retrySearch helper)
  - DiscoverScreen Composable (Scaffold + OutlinedTextField with ImeAction.Search + LazyColumn with FROM THE WEB section [hidden when search Idle] + FROM THE COMMUNITY section)
  - DiscoverProductCard (Coil AsyncImage + bodyL title + bodyM desc + Romanian-locale currency price; Card.onClick → Intent.ACTION_VIEW RAW retailerUrl, ActivityNotFoundException → Snackbar)
  - DiscoverShimmerCard (16:9 image stub + 4 text stubs with horizontalGradient sweep 1200ms FastOutSlowInEasing)
  - discover_card_placeholder.xml vector drawable (accentSoft→accent vertical gradient @ 40% alpha + paper gift-box glyph @ 70% alpha; aapt:attr gradient inside vector — minSdk 23 supports it)
  - 11 new string keys per locale (nav_discover_tab + 10 discover_*) in values/strings.xml + values-ro/strings.xml; LocalizationParityTest still green
  - DiscoverKey added to AppNavKeys; GiftMaisonBottomNav slot 2 rewired (NavSlotId.DISCOVER + Icons.Outlined.Search + nav_discover_tab + onDiscover); AppNavigation entry<DiscoverKey> { DiscoverScreen() } registered and slot 2 callback pushes DiscoverKey
  - StyleGuidePreview.kt: DiscoverPreview composable showing all 5 interaction states inline for offline visual review
  - 17 new unit tests (7 repository + 10 ViewModel) — all pass

affects: [17-06-deploy-and-uat]

# Tech tracking
tech-stack:
  added: []  # All deps already in the project (Coil 3.4.0, Hilt 2.51.x, Compose BOM, FirebaseFunctions, Material3, Turbine, mockk)
  patterns:
    - "Per-section sealed state machines (PopularState + SearchState) co-located in one UiState file; ViewModel emits independently to two StateFlows so UI sections render unaware of each other's lifecycle."
    - "Lock-step slot rotation completed: Plan 17-01 emptied slot 2 (no-op callback + retained label string) and Plan 17-05 renames the enum/icon/label/callback in one commit alongside deletion of the legacy string. Build remains green across the two-plan window."
    - "Search responses without doc IDs get UUID-synthesised LazyColumn keys at the repository mapping site (not the UI), so the UI never sees a non-unique key."
    - "Raw retailerUrl click-through (Intent.ACTION_VIEW) — affiliate transform deferred to a future Save-to-Registry flow per D-32, keeping Discover a pure browse surface."

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt
    - app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt
    - app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/di/DiscoverModule.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
    - app/src/main/res/drawable/discover_card_placeholder.xml
    - app/src/test/java/com/giftregistry/data/discover/DiscoverRepositoryImplTest.kt
    - app/src/test/java/com/giftregistry/ui/discover/DiscoverViewModelTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "FirebaseFunctions provided by existing AppModule.provideFirebaseFunctions() (pinned to europe-west3), NOT redeclared inside DiscoverModule. The plan flagged this as an either/or; grep showed AppModule is the canonical provider used by Reservation/Notification/Item/Registry repos."
  - "Coil import path is coil3.compose.AsyncImage (Coil 3.4.0 pinned in libs.versions.toml; matches HeroImageOrPlaceholder / ItemPreviewCard / RegistryItemRow precedent)."
  - "Placeholder drawable uses aapt:attr gradient inside vector (minSdk 23 supports this — AAPT2 resolves it at build time; processDebugResources passes cleanly)."
  - "Did not introduce a separate FirebaseFunctions provider — DiscoverModule is @Binds only."
  - "Used accentInk for retry-button content colour (token exists on GiftMaisonColors per GiftMaisonColors.kt:39)."

patterns-established:
  - "Two-section LazyColumn with conditional rendering: the FROM THE WEB section is gated on `search !is SearchState.Idle`. This makes the Idle case render exactly the FROM THE COMMUNITY section, matching D-35."
  - "Repository-layer ID synthesis for Callables that return IDless payloads: pass a `generateMissingIds: Boolean` flag to the shared mapper; popular keeps empty-string IDs (mapping never expected to lack id), search synthesises UUIDs."

requirements-completed: [D-01, D-02, D-32, D-33, D-34, D-35, D-36, D-37, D-38, D-39, D-40, D-41, D-42, D-49, D-50, D-51]

# Metrics
duration: 11min
completed: 2026-05-27
---

# Phase 17 Plan 05: Android Discover Summary

**Full Android Discover surface shipped: 4-layer stack (domain / data / di / ui), bottom-nav slot 2 rewired with lock-step deletion of the legacy Stores label string, 17 new unit tests, all builds and tests green.**

## Performance

- **Duration:** 11 min
- **Started:** 2026-05-27T14:53:27Z
- **Completed:** 2026-05-27T15:04:03Z
- **Tasks:** 4 (each committed atomically)
- **Files created:** 12 (10 Kotlin + 1 XML drawable + 1 strings-block addition × 2 locales)
- **Files modified:** 7 (nav graph + bottom nav + visibility doc + StyleGuidePreview + 2× strings.xml)
- **Tests added:** 17 (7 DiscoverRepositoryImplTest + 10 DiscoverViewModelTest)

## Accomplishments

- **Domain + Data + DI (Task 1):** DiscoverProduct domain model; DiscoverRepository interface; DiscoverRepositoryImpl wrapping both Callables with runCatching → Result; empty-array → Result.success(emptyList()) (NOT failure); UUID synthesis for search responses without doc IDs; DiscoverModule binds the interface to the impl. FirebaseFunctions is reused from the existing AppModule provider (europe-west3) — no per-feature provider needed.
- **ViewModel + UiState + strings (Task 2):** PopularState + SearchState sealed interfaces; DiscoverViewModel exposes popular/search/searchQuery StateFlows + loadPopular/search/onQueryChange/retrySearch handlers; init {} auto-loads popular; trimmed-empty query resets SearchState to Idle without invoking the Callable. 10 new string keys + nav_discover_tab in both en + ro strings.xml; LocalizationParityTest stays green.
- **Compose UI (Task 3):** discover_card_placeholder.xml vector drawable (accentSoft→accent gradient + paper gift-box glyph); DiscoverShimmerCard with horizontalGradient brush sweep; DiscoverProductCard with Coil AsyncImage + Romanian-locale currency formatting + Intent.ACTION_VIEW (RAW retailerUrl, no affiliate transform per D-32) + ActivityNotFoundException → Snackbar (D-33); DiscoverScreen renders Scaffold + OutlinedTextField (ImeAction.Search) + LazyColumn with both sections gated by the SearchState/PopularState state machines.
- **Nav rewire + StyleGuidePreview (Task 4):** DiscoverKey added to AppNavKeys; GiftMaisonBottomNav slot 2 renamed in lock-step (NavSlotId.STORES → DISCOVER, Storefront → Search icon, nav_stores_tab → nav_discover_tab, onStores → onDiscover); AppNavigation entry<DiscoverKey> { DiscoverScreen() } registered; nav_stores_tab deleted from both strings.xml files in lock-step with the rename. StyleGuidePreview.kt: renamed 2 existing call-sites and appended DiscoverPreview composable showing all 5 interaction states (idle, loading, loaded both sections, empty, error).

## Task Commits

1. **Task 1: Domain + Data + DI layer + repository tests** — `2c1da05` (feat) — TDD: RED test compile-fail → implementation → GREEN
2. **Task 2: ViewModel + UiState + strings (en+ro) + VM tests** — `fc98e56` (feat) — TDD: RED test compile-fail → implementation → GREEN
3. **Task 3: Drawable + DiscoverProductCard + DiscoverShimmer + DiscoverScreen** — `96448bc` (feat) — compile + processResources verified
4. **Task 4: Nav rewire (slot 2 → Discover) + DiscoverPreview** — `03c0ced` (refactor) — all targeted tests + grep verifications pass

## Files Created/Modified

**Created (12):**
- `app/src/main/java/com/giftregistry/domain/discover/DiscoverProduct.kt` — flat data class matching Callable response shape (id, title, description, imageUrl, price Double, currency, retailerUrl)
- `app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt` — `suspend getPopular(): Result<List<DiscoverProduct>>` + `suspend search(query): Result<List<DiscoverProduct>>`
- `app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt` — Hilt-injected, wraps both Callables; shared `mapResponseToProducts(generateMissingIds)` handles popular vs search ID convention
- `app/src/main/java/com/giftregistry/di/DiscoverModule.kt` — `@Binds DiscoverRepository -> DiscoverRepositoryImpl` (FirebaseFunctions reused from AppModule)
- `app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt` — PopularState (Loading/Loaded/Empty/Error) + SearchState (Idle/Loading/Loaded/Empty/Error) sealed interfaces
- `app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt` — @HiltViewModel; loadPopular fires in init; search trims query, short-circuits blank to Idle; retrySearch re-fires last query
- `app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt` — Card.onClick → Intent.ACTION_VIEW on raw retailerUrl; ActivityNotFoundException → Snackbar; Romanian-locale currency formatting; Coil AsyncImage with placeholder/error → discover_card_placeholder
- `app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt` — DiscoverShimmerCard with horizontalGradient brush sweep (paperDeep→line→paperDeep), 1200ms FastOutSlowInEasing infinite repeat
- `app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt` — Scaffold + OutlinedTextField (ImeAction.Search) + LazyColumn with conditional FROM THE WEB section (hidden when search Idle) and always-on FROM THE COMMUNITY section
- `app/src/main/res/drawable/discover_card_placeholder.xml` — 160×90 vector with aapt:attr linear gradient + paper-tinted gift-box glyph
- `app/src/test/java/com/giftregistry/data/discover/DiscoverRepositoryImplTest.kt` — 7 cases (popular success/empty/failure/missing-id; search success+payload-capture/empty/failure)
- `app/src/test/java/com/giftregistry/ui/discover/DiscoverViewModelTest.kt` — 10 cases (init triggers loadPopular; Loaded/Empty/Error transitions; blank-query short-circuits to Idle; search trims; retrySearch re-fires; onQueryChange does NOT auto-fire)

**Modified (7):**
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — added `@Serializable data object DiscoverKey` after NotificationsKey
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — added DiscoverScreen import; renamed onStores callback to onDiscover and rewrote body to push DiscoverKey (guarded against duplicate top-of-stack); added `entry<DiscoverKey> { DiscoverScreen() }` after HomeKey entry
- `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` — Storefront import → Search; NavSlotId.STORES → DISCOVER; callback parameter onStores → onDiscover; slot-2 NavItemSlot rewired to new icon/label/callback; `selected` mapping adds DiscoverKey → NavSlotId.DISCOVER; KDoc updated with Plan 17-01 + 17-05 history
- `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` — KDoc visible-on list now includes DiscoverKey; history bullet for Plan 17-05 appended
- `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` — renamed 2 existing `onStores = {}` call-sites to `onDiscover = {}`; added imports (SnackbarHostState, DiscoverProduct, DiscoverProductCard, DiscoverShimmerCard, PaddingValues, remember); appended Phase 17 DiscoverPreview composable showing 5 interaction states
- `app/src/main/res/values/strings.xml` — deleted `nav_stores_tab` (English); added 11 new keys (nav_discover_tab + 10 discover_*) in a Phase 17 comment block
- `app/src/main/res/values-ro/strings.xml` — deleted `nav_stores_tab` (Romanian); added 11 new keys with Romanian translations using the file's existing `&#NNNN;` Unicode-escape convention for diacritics (ă=259, Ă=258, î=238, Î=206, ș=537)

## Decisions Made

1. **FirebaseFunctions reused from AppModule, not redeclared in DiscoverModule.** The plan documented both options; grep revealed `AppModule.provideFirebaseFunctions()` at line 36–44 is the canonical app-wide provider already used by ReservationRepositoryImpl, NotificationRepositoryImpl, ItemRepositoryImpl, and RegistryRepositoryImpl. DiscoverModule contains only the `@Binds DiscoverRepository -> DiscoverRepositoryImpl` mapping. The region `europe-west3` is pinned in AppModule with the matching emulator hook.

2. **Coil import path: `coil3.compose.AsyncImage`.** Cross-checked against HeroImageOrPlaceholder.kt, ItemPreviewCard.kt, EditItemScreen.kt, RegistryItemRow.kt, PresetThumbnail.kt — all use `coil3.compose.AsyncImage`. `gradle/libs.versions.toml` pins `coil = "3.4.0"` and `coil-compose = io.coil-kt.coil3:coil-compose`. This is the Coil 3 Compose-native namespace.

3. **Placeholder drawable uses `aapt:attr` gradient inside vector.** minSdk 23 supports AAPT2's build-time gradient inlining inside `<vector>`. `./gradlew app:processDebugResources` passes cleanly — no fallback to flat-fill was needed.

4. **Card.onClick used directly (not Material3 experimental opt-in).** The Compose BOM 2026.03.00 pinned in this project surfaces `Card(onClick: () -> Unit)` as a stable API; no `@OptIn(ExperimentalMaterial3Api::class)` annotation was required and `compileDebugKotlin` passed without one.

5. **LazyColumn contentPadding used start/end/bottom instead of horizontal/bottom.** Compose's PaddingValues factory does not accept `horizontal` and `bottom` together (compile-fail caught and fixed inline during Task 3).

6. **Romanian diacritics use mixed `&#NNNN;` decimal entities.** The existing values-ro/strings.xml uses several conventions (`&#0258;`, `&#0259;`, `&#537;`, `ă`); I picked decimal `&#NNNN;` for consistency with the immediately-adjacent nav strings block (line 224–230) where `&#0196;` / `&#0258;` / `&#0259;` are used.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] LazyColumn `contentPadding = PaddingValues(horizontal = 16.dp, bottom = 24.dp)` does not compile**
- **Found during:** Task 3 first compile attempt
- **Issue:** `PaddingValues` factory function does not accept `horizontal` + `bottom` simultaneously; Kotlin compiler resolved the call to the (start, top, end, bottom) overload which has no `horizontal` parameter.
- **Fix:** Rewrote as `PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)`. Visual contract identical.
- **Committed in:** `96448bc` (Task 3 commit — caught + fixed before commit).

**2. [Rule 3 - Verify-grep regression] Plan-17-05 history comments in GiftMaisonBottomNav.kt KDoc tripped the orphan-symbol grep**
- **Found during:** Task 4 verification grep pass
- **Issue:** My initial GiftMaisonBottomNav KDoc said "NavSlotId.STORES -> DISCOVER, Icons.Outlined.Storefront -> Search, nav_stores_tab -> nav_discover_tab, onStores -> onDiscover" — literal symbol mentions in the history block. The plan's verification grep `! grep -q "NavSlotId.STORES\|onStores\|Icons.Outlined.Storefront\|nav_stores_tab" ...` flags these as orphan references even though they're documentation of the rename.
- **Fix:** Reworded the history bullet to "enum entry, icon, label string, and callback all renamed to the Discover variant; legacy label string deleted from values + values-ro in lock-step with this rename." Semantically identical, no literal symbol matches.
- **Committed in:** `03c0ced` (Task 4 commit).

**3. [Rule 3 - Verify-grep regression] Lock-step deletion comment in strings.xml tripped the orphan grep**
- **Found during:** Task 4 verification grep pass (plan step 4: `grep -E "...|nav_stores_tab|R.string.nav_stores_tab" app/src/main` MUST return zero lines)
- **Issue:** I left a `<!-- Plan 17-05: nav_stores_tab deleted in lock-step ... -->` comment in both strings.xml files documenting the deletion. The literal text "nav_stores_tab" still matched the orphan grep even though the actual `<string name="...">` line was gone.
- **Fix:** Removed the comments — the rename is documented in the SUMMARY and in GiftMaisonBottomNav.kt KDoc; the strings.xml does not need an in-line gravestone.
- **Committed in:** `03c0ced` (Task 4 commit — alongside the KDoc reword above).

---

**Total deviations:** 3 auto-fixed (1 Rule 3 compile-error, 2 Rule 3 verify-grep regressions — all caused by my initial implementation choices, none introduced new behaviour)
**Impact on plan:** Zero scope creep. All three fixes preserved the visual/behavioural contract — just adjusted source text to match the plan's literal verify-grep expectations.

## Issues Encountered

- **PaddingValues factory has overload restrictions** — `horizontal` + `bottom` cannot be combined. Use `start`/`end`/`bottom` instead. Fixed inline during Task 3.
- **Plan verify-greps are literal-substring greps, not semantic refs** — KDoc + XML comments that mention deleted symbol names trip the orphan check. Reworded both to satisfy the literal grep without losing documentation intent.

## User Setup Required

None. On-device visual verification + smoke test (tap each interaction state, confirm IDP redirect on card tap, verify shimmer animation, confirm Romanian translation toggle) happens in **plan 17-06** as part of UAT.

## Known Stubs

None — every part of the Discover surface is wired and functional. The retailer-URL click-through uses the RAW URL (no affiliate transform) per D-32; this is the intentional v1 behaviour, not a stub.

## Next Phase Readiness

- **Plan 17-06 (deploy + UAT):** Ready. The Android Discover surface compiles cleanly, all targeted unit tests pass, and the StyleGuidePreview includes a 5-state DiscoverPreview for visual review before on-device UAT. Plan 17-06 should:
  - Build a release APK and confirm the bottom-nav slot 2 shows Search icon + DISCOVER label
  - Tap slot 2, confirm DiscoverScreen renders FROM THE COMMUNITY shimmer → loaded
  - Submit a search query, confirm FROM THE WEB section appears with shimmer → loaded
  - Tap a product card, confirm the browser opens at the raw retailer URL
  - Toggle Romanian locale, confirm DESCOPERĂ label and Romanian copy on all states
  - Run the targeted unit-test suite as a CI gate before deploy

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Plan: 05 of 6*
*Completed: 2026-05-27*

## Self-Check: PASSED

- 12 created files: all FOUND on disk
- 4 task commits: all FOUND in git history (2c1da05, fc98e56, 96448bc, 03c0ced)
- All targeted unit tests pass (LocalizationParityTest, BottomNavVisibilityTest, DiscoverViewModelTest, DiscoverRepositoryImplTest)
- app:compileDebugKotlin + app:compileDebugUnitTestKotlin both clean
- Plan verification greps (steps 3 + 4) pass: every expected ref present, zero orphan refs
