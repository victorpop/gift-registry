---
phase: 11-registry-detail-create-add-item-redesign
plan: 05
subsystem: ui
tags: [kotlin, compose, giftmaison, add-item, scr-10, segmented-tabs, pulsing-dot, affiliate]

# Dependency graph
requires:
  - phase: 11-02
    provides: AddItemMode enum, shouldShowAffiliateRow predicate, AffiliateUrlTransformer.isAffiliateDomain
  - phase: 09-shared-chrome-status-ui
    provides: PulsingDot composable (period param confirmed)
  - phase: 10-onboarding-home-redesign
    provides: SegmentedTabs composable (3-tab capable)
  - phase: 03-registry-item-management
    provides: AddItemViewModel, AddItemUseCase, FetchOgMetadataUseCase, affiliate pipeline

provides:
  - AddItemScreen: SCR-10 re-skinned with x close bar, 3-tab SegmentedTabs, mode-aware body, dual CTA
  - FetchingIndicator: mono-caps fetching label + PulsingDot(period=1_000.ms)
  - AffiliateConfirmationRow: green tick confirmation + Clear ghost button
  - ItemPreviewCard: 14-radius paperDeep card with 80x80 thumbnail + title/price/source
  - AutoFillTag: ok-green pill tag for OG-populated fields
  - InfoPill: secondSoft pill with affiliate info copy + domain placeholder
  - AddItemDualCtaBar: 1:1.5 ghost+primary dual CTA bottom bar
  - onNavigateToBrowseStores wired in AppNavigation entry<AddItemKey>

affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "6 internal sub-composables in ui.item.add package — all consume GiftMaisonTheme tokens exclusively, no raw Color literals"
    - "Dual CTA bar pattern: addAnotherMode flag in Composable controls save-and-reset vs save-and-pop branch in LaunchedEffect(savedItemId)"
    - "BrowseStores tab: LaunchedEffect(selectedTab) triggers navigation + resets tab index before navigating so re-entry shows PasteUrl"
    - "PulsingDot period=1_000.ms at FetchingIndicator call site; Status chips still use 1_400ms default"
    - "giftMaisonFieldColors(): OutlinedTextFieldDefaults.colors private function — same pattern as Plan 04 CreateRegistryScreen"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/item/add/FetchingIndicator.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AffiliateConfirmationRow.kt
    - app/src/main/java/com/giftregistry/ui/item/add/ItemPreviewCard.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AutoFillTag.kt
    - app/src/main/java/com/giftregistry/ui/item/add/InfoPill.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemDualCtaBar.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "addAnotherMode flag in Composable (not ViewModel) separates save+reset from save+pop — keeps ViewModel pure; flag resets to false after LaunchedEffect branch fires"
  - "LaunchedEffect(selectedTab) resets selectedTabIndex before calling onNavigateToBrowseStores — ensures re-entry to AddItemScreen shows PasteUrl tab, not BrowseStores"
  - "item_notes_hint_detail added as a new key (not overriding item_notes_hint) so existing EditItemScreen hint is unchanged"
  - "add_item_close_cd not common_close — avoids clobbering existing common strings; scoped to Add Item screen only"

patterns-established:
  - "Dual CTA save flow: addAnotherMode Boolean in Composable; LaunchedEffect branches on it after savedItemId fires"

requirements-completed: [SCR-10]

# Metrics
duration: 5min
completed: 2026-04-21
---

# Phase 11 Plan 05: SCR-10 Add Item URL Re-skin Summary

**6 new internal composables (FetchingIndicator/AffiliateConfirmationRow/ItemPreviewCard/AutoFillTag/InfoPill/AddItemDualCtaBar) re-skin AddItemScreen with GiftMaison tokens, 3-tab SegmentedTabs, and dual CTA bar while preserving the Phase 3 OG + affiliate pipeline unchanged**

## Performance

- **Duration:** ~5 min
- **Started:** 2026-04-21T21:21:45Z
- **Completed:** 2026-04-21T21:25:56Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- 6 new `internal` composables in `ui/item/add/` — all use GiftMaison tokens exclusively, zero raw Color literals
- AddItemViewModel extended with `ogFetchSucceeded` + `isAffiliateDomain` derived StateFlows, `onClearUrl()` + `onResetForm()` helpers
- AddItemScreen fully re-skinned: x close top bar, 3-tab SegmentedTabs, PasteUrl/Manual mode bodies, AddItemDualCtaBar bottom bar
- AppNavigation `entry<AddItemKey>` wired with `onNavigateToBrowseStores -> StoreListKey(preSelectedRegistryId)` 
- 15 new string keys (add_item_* / item_url_label / item_notes_hint_detail / item_og_fetch_failed_inline) in EN + RO
- Phase 3 affiliate pipeline (ItemRepositoryImpl), AddItemUseCase, FetchOgMetadataUseCase UNTOUCHED
- `./gradlew :app:assembleDebug` BUILD SUCCESSFUL + full unit suite GREEN

## Task Commits

1. **Task 1: 6 sub-composables + ViewModel extension + string keys** - `19bd519` (feat)
2. **Task 2: AddItemScreen rewrite + AppNavigation wire** - `9978986` (feat)

**Plan metadata:** _(docs commit follows)_

## Files Created/Modified
- `app/src/main/java/com/giftregistry/ui/item/add/FetchingIndicator.kt` — Mono-caps fetching row + PulsingDot(period=1_000.ms)
- `app/src/main/java/com/giftregistry/ui/item/add/AffiliateConfirmationRow.kt` — Green tick confirmed + Clear ghost button
- `app/src/main/java/com/giftregistry/ui/item/add/ItemPreviewCard.kt` — 14-radius paperDeep card with 80x80 AsyncImage
- `app/src/main/java/com/giftregistry/ui/item/add/AutoFillTag.kt` — ok-green pill "auto-filled" tag
- `app/src/main/java/com/giftregistry/ui/item/add/InfoPill.kt` — secondSoft pill with affiliate info + domain param
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemDualCtaBar.kt` — 1:1.5 ghost+primary dual CTA
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — Full rewrite: x close bar, 3-tab, mode body, dual CTA
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` — ogFetchSucceeded, isAffiliateDomain, onClearUrl, onResetForm
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — entry<AddItemKey> + onNavigateToBrowseStores
- `app/src/main/res/values/strings.xml` — 15 new Phase 11 SCR-10 EN keys
- `app/src/main/res/values-ro/strings.xml` — 15 new Phase 11 SCR-10 RO keys

## Decisions Made
- `addAnotherMode` flag in Composable (not ViewModel) — keeps VM pure; only Composable knows which CTA was tapped
- `LaunchedEffect(selectedTab)` resets `selectedTabIndex` to PasteUrl before calling `onNavigateToBrowseStores` — ensures re-entry to AddItemScreen always starts on the URL tab
- `add_item_close_cd` scoped key (not `common_close`) — avoids any collision with existing common string definitions
- `item_notes_hint_detail` as new key alongside existing `item_notes_hint` — preserves EditItemScreen hint unchanged

## Deviations from Plan

None — plan executed exactly as written. The plan specified adding `item_url_label` and `item_notes_hint_detail` as contingent additions ("if missing, add"); both were confirmed absent and added as the 14th and 15th new keys.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None — all composables wire to real ViewModel StateFlows; OG fetch and affiliate pipeline unchanged from Phase 3.

## Next Phase Readiness

Phase 11 is now complete: Plans 01-05 all delivered. The three owner-facing screens (RegistryDetail, CreateRegistry, AddItem) are fully re-skinned to GiftMaison design. The Store Browser tab in AddItemScreen navigates to Phase 7's StoreListScreen with registry pre-selected.

---
*Phase: 11-registry-detail-create-add-item-redesign*
*Completed: 2026-04-21*
