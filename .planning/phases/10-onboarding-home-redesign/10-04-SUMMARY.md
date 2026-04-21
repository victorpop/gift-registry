---
phase: 10-onboarding-home-redesign
plan: "04"
subsystem: home-screen
tags: [compose, ui, home, registry-list, theming, navigation]
dependency_graph:
  requires:
    - 10-02  # AvatarButton, SegmentedTabs, TabFilters, primaryRegistryIdOf, Registry.imageUrl
    - 08     # GiftMaisonTheme tokens (colors, typography, shapes, spacing, wordmark)
    - 09     # Phase 9 bottom nav/chrome — LazyColumn clearance
  provides:
    - RegistryListScreen SCR-07 re-skin (HomeTopBar + RegistryCard variants + SegmentedTabs)
    - RegistryListViewModel.currentUser StateFlow
    - HomeTopBar composable
    - RegistryCardPrimary + RegistryCardSecondary composables
    - onNavigateToSettings callback wired through AppNavigation.kt
    - 15 home_*/registry_* string keys (EN + RO)
  affects:
    - AppNavigation.kt HomeKey entry (1-line callback addition)
    - AvatarButton.kt (provisional string ref rewired)
tech_stack:
  added:
    - Coil 3 ColorFilter.colorMatrix (70% brightness for primary card image)
  patterns:
    - inline Row HomeTopBar replacing Material3 TopAppBar
    - RegistryCardPrimary/Secondary dual-variant pattern (primary = maxByOrNull updatedAt)
    - rememberSaveable mutableIntStateOf(0) for tab index (RESEARCH.md Pitfall 3 applied)
    - Long-press DropdownMenu for edit/delete (no ⋯ icon per handoff)
key_files:
  created:
    - app/src/main/java/com/giftregistry/ui/registry/list/HomeTopBar.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
    - app/src/main/java/com/giftregistry/ui/common/AvatarButton.kt
decisions:
  - "onNavigateToNotifications kept as no-op default — inbox still reachable via deep link; bell placement deferred to Phase 11"
  - "MaterialTheme.colorScheme.error retained in delete AlertDialog — approved by CONTEXT.md for destructive-action patterns"
  - "Tab index uses Int (not sealed class) per RESEARCH.md Pitfall 3"
  - "isDraft passes itemCount=0 as placeholder per CONTEXT.md deferred stats aggregation"
metrics:
  duration: "3 minutes"
  completed_date: "2026-04-21"
  tasks: 2
  files: 8
requirements:
  - SCR-07
---

# Phase 10 Plan 04: SCR-07 Home Re-skin Summary

SCR-07 Home screen re-skinned with GiftMaison v1.1 tokens: wordmark + avatar top bar, "Your registries" displayXL headline, SegmentedTabs (Active/Drafts/Past), primary card (ink bg, 70% brightness image) + secondary cards (paperDeep, line border), per-tab empty states, onNavigateToSettings wired to SettingsKey.

## What Was Built

### Task 1: RegistryListViewModel + HomeTopBar + RegistryCard + Strings

**RegistryListViewModel.kt** — added `currentUser: StateFlow<User?>` by collecting `authRepository.authState` with `SharingStarted.Eagerly` so the UI has a value before the first frame. One-line addition that does not disturb the existing `uiState` Flow chain.

**HomeTopBar.kt** — new file. Inline `Row` (not Material3 `TopAppBar`) with `GiftMaisonWordmark()` on the left and `AvatarButton` on the right, separated by a `Spacer(Modifier.weight(1f))`. Padding uses `spacing.edge` and `spacing.gap16`.

**RegistryCard.kt** — new file. Two public composables:
- `RegistryCardPrimary`: `ink` background, `paper` text, Coil 3 `AsyncImage` with `ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) })` for 70% brightness, occasion pill (accent bg), date bottom-right.
- `RegistryCardSecondary`: `paperDeep` background, `line` border at 1.dp, full-brightness image, occasion pill (paperDeep bg / inkSoft text).
Both cards show a 16:9 hero image, occasion pill top-left, optional date bottom-right, registry title in `displayS`, stats line in `monoCaps` with `\u2022` bullet separators.

**String keys** — 15 new keys added to `values/strings.xml` (EN) and `values-ro/strings.xml` (RO):
`home_headline`, `home_stats_caption`, `home_stats_items`, `home_stats_reserved`, `home_stats_given`, `home_tab_active`, `home_tab_drafts`, `home_tab_past`, `home_empty_active`, `home_empty_drafts`, `home_empty_past`, `home_avatar_content_desc`, `registry_card_menu_edit`, `registry_card_menu_delete`, `registry_list_retry`.

**AvatarButton.kt** — provisional string ref rewired from `auth_settings_title` to `home_avatar_content_desc` (Plan 02 note resolved).

### Task 2: RegistryListScreen Rewrite + AppNavigation Wire-up

**RegistryListScreen.kt** — full rewrite (292 → 236 lines, net -56):
- Signature: 5 params including new `onNavigateToSettings: () -> Unit = {}` (default no-op for backward compat)
- `HomeTopBar` replaces `TopAppBar` — inline Row, no `topBar = { ... }` Scaffold slot needed
- `rememberSaveable { mutableIntStateOf(0) }` for tab index (RESEARCH.md Pitfall 3)
- `remember(registries) { primaryRegistryIdOf(registries) }` for primary card selection
- `remember { startOfTodayMs() }` captured once at composition (stable, recomputed only on re-entrance)
- `remember(registries, selectedTabIndex, todayMs) { ... }` for filtered list derivation
- Per-tab empty state: single `bodyM inkFaint` `Text`, no CTA, no illustration (v1.1 scope)
- LazyColumn `contentPadding.bottom = 100.dp` for Phase 9 bottom nav + FAB clearance
- Long-press `DropdownMenu` retained for edit/delete per RESEARCH.md open question 1 resolution
- Delete `AlertDialog` with `registryToDelete` state preserved from Phase 3
- `NotificationsInboxBell` import and reference removed

**AppNavigation.kt** — 1-line addition: `onNavigateToSettings = { backStack.add(SettingsKey) }` in the `entry<HomeKey>` block.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

**Stats line (RegistryCard.kt, `statsLine()` function):** Renders `"0 items • 0 reserved • 0 given"` for all cards. Per CONTEXT.md: "per-registry stat aggregation deferred — Phase 10 renders zeros." This is intentional and documented. Data is correct at 0; it does not mislead, but is incomplete. Follow-up: Firestore doc-level counts or per-card Flow observation once a stats pipeline ships.

**Draft tab filter (`isDraft(itemCount = 0)`):** Because itemCount is hardcoded to 0, all registries with non-blank titles appear as drafts in the Drafts tab. This is the CONTEXT.md-approved stop-gap until per-registry item counts arrive.

## Follow-ups Recorded

1. **Per-registry stats aggregation** — itemCount / reservedCount / givenCount rendered as zeros today per CONTEXT.md deferred. Needs Firestore doc-level counter fields or per-card Flow observation.
2. **Notifications bell placement** — deferred to Phase 11 per CONTEXT.md. Inbox remains reachable via deep link route; `onNavigateToNotifications` parameter kept as no-op default.
3. **Old string key cleanup** — `registry_list_title`, `registry_list_empty`, `auth_or_email_divider`, `common_retry` (superseded by `registry_list_retry`) are now unused but not deleted. Cleanup pass after confirming no transitive references.
4. **Registry.imageUrl Firestore backfill** — pre-Phase-10 registry documents lack `imageUrl`; cards render `paperDeep` placeholder (AsyncImage with null model). Acceptable MVP behavior.
5. **AvatarButton.kt Kdoc** — comment still references "auth_settings_title provisionally" from Phase 02; doc cleanup is a cosmetic follow-up.

## Verification Results

- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (all 29 unit tests pass, including Phase 10 Wave 0 tests)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (debug APK produced)

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | 0d83e8a | feat(10-04): Task 1 — currentUser StateFlow, HomeTopBar, RegistryCard, 15 home_/registry_ strings |
| 2 | ff376f2 | feat(10-04): Wave 1 — SCR-07 Home re-skin (HomeTopBar + RegistryCard + SegmentedTabs) + onNavigateToSettings |

## Self-Check: PASSED
