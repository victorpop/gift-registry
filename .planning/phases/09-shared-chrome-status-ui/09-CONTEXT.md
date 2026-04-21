# Phase 9: Shared Chrome + Status UI - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase ships the cross-cutting owner-facing chrome and status surfaces that screens 06–10 will assemble from:

1. **Bottom nav** — replaces the current 4-tab `NavigationBar` in `AppNavigation.kt` with a GiftMaison-styled 5-slot nav (`Home · Stores · +FAB · Lists · You`), visible only on Home + RegistryDetail (hidden on Auth, Onboarding, Create, AddItem, EditItem, ReReserve).
2. **Centre FAB** — 54 px accent circle lifted 22 px above the bar with accent shadow + paper ring.
3. **Add-action bottom sheet** — modal sheet over scrim-blurred home with 4 actions (New registry / Item from URL / Browse stores / Add manually).
4. **Status chips + purchased treatment** — reusable composables for Reserved (filled-accent + pulsing dot + "Nm" countdown), Given (secondSoft "✓ given"), Open (outlined inkFaint), plus a `Modifier.purchasedVisualTreatment()` for given-item rows (55 % opacity + grayscale + strikethrough).

Out of scope (belongs to Phases 10 / 11): redesigning the screens themselves — Phase 9 only ships the shared components, and the existing screens (Home, RegistryDetail, Settings, StoreList) keep their current layouts until their respective phases consume the new chrome.

</domain>

<decisions>
## Implementation Decisions

### Navigation restructure
- **"Lists" tab behaviour** — Routes to the user's `isPrimary` (most-recently-active) registry detail via `RegistryDetailKey`. If the user has no registries, show an empty-state helper that suggests creating one (not a crash, not a silent no-op). The `isPrimary` selection rule itself is a Phase 10 concern; in Phase 9, use a simple "first registry in the owner's list, ordered by `updatedAt desc`" resolver.
- **"You" tab destination** — Routes to the existing `SettingsKey` (Preferences screen). No new profile screen — out of v1.1 scope.
- **Nav visibility rule** — Follow the handoff exactly: visible only on `HomeKey` + `RegistryDetailKey`. Hidden on `AuthKey`, `OnboardingKey`, `CreateRegistryKey`, `EditRegistryKey`, `AddItemKey`, `EditItemKey`, `StoreListKey`, `StoreBrowserKey`, `SettingsKey`, `NotificationsKey`, `ReReserveDeepLink`. Deviates from the current "persistent-everywhere" model shipped in quick tasks 260420-hua / 260420-iro.
- **Cutover strategy** — Full replacement. Delete the current `NavigationBar { NavigationBarItem(...) x4 }` block in `AppNavigation.kt` and replace with a new `GiftMaisonBottomNav` composable sourced from `ui/common/chrome/`. No feature flag, no parallel nav.

### FAB Add-action bottom sheet
- **"Item from URL" with no registry context** — Open a lightweight registry picker (reuse existing `RegistryListViewModel`-observed list) that lets the owner pick which registry to add to, then push `AddItemKey(registryId)`. If the owner has zero registries, disable the row and show inline helper "Create a registry first". Same behaviour for "Browse stores" and "Add manually" when no registry exists.
- **"Add manually" destination** — Route to the existing `AddItemKey(registryId, initialUrl = "")` — `AddItemScreen` already supports manual entry when the URL is blank. No new screen.
- **FAB availability** — Exactly where nav is visible (Home + RegistryDetail). On RegistryDetail the sheet pre-selects the currently-viewed registry for URL/manual/browse-stores actions. On Home the picker opens as described above.
- **"ADD" mono-caps label** — Include per handoff: mono 9.5 caps centred under the FAB at the bar's baseline, `inkFaint` colour.

### Status UI implementation
- **Component location** — New package `app/src/main/java/com/giftregistry/ui/common/status/`:
  - `StatusChip.kt` — `ReservedChip`, `GivenChip`, `OpenChip` composables + a `StatusChip` wrapper.
  - `PulsingDot.kt` — reusable pulsing-dot composable.
  - `PurchasedRowModifier.kt` — `Modifier.purchasedVisualTreatment()` extension.
- **Pulsing dot implementation** — Single `PulsingDot(color: Color, size: Dp = 4.dp, period: Duration = 1_400.ms)` composable using `rememberInfiniteTransition(label)` + `animateFloat` on alpha (1f↔0.5f) and scale (1f↔0.85f), `tween(durationMillis = 700, easing = FastOutSlowInEasing)` with `RepeatMode.Reverse` — matches handoff CSS `pulse` keyframe exactly. Used by Reserved chip (1_400.ms period) and by the "Fetching from…" URL field in Phase 11 (1_000.ms period — reuse same composable).
- **Countdown "Nm" source** — Per-composable `LaunchedEffect` + `delay(60_000)` loop. Takes `expiresAt: Instant` from the `Item` domain model, derives `minutesLeft = max(0, (expiresAt - now) / 60.seconds)`, re-renders once per minute. No ViewModel changes, no ticker StateFlow. When `minutesLeft == 0`, chip shows "<1m" until the item flips back to Open via the existing Firestore listener.
- **Purchased row treatment** — `Modifier.purchasedVisualTreatment()` extension that applies:
  1. `alpha(0.55f)` to the whole row.
  2. For the item image: `ColorMatrix.setToSaturation(0f)` (grayscale) + a `Color(0x662A2420)` (ink at ~40 % alpha) overlay behind a centred `Icons.Default.Check` in paper colour.
  3. For the title: `TextDecoration.LineThrough`.

### Claude's Discretion
- Exact naming of the new package (`ui/common/chrome/` for nav/FAB/sheet and `ui/common/status/` for chips) is Claude's choice — pick one and stay consistent.
- Animation durations inside the Add-sheet (scrim fade-in, sheet slide-up) follow Material 3 defaults unless the handoff specifies otherwise (it doesn't, beyond the `pulse` keyframe).
- Nav icon choice: handoff says "stroked, 1.6 stroke, linecap round". Claude may use Material Symbols (home, storefront, list, person) or inline `ImageVector` paths — prefer Material Symbols for maintenance.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phase 8 + earlier)
- `GiftMaisonColors` (`app/src/main/java/com/giftregistry/ui/theme/GiftMaisonColors.kt`) — full Housewarming palette as a `CompositionLocal`.
- `GiftMaisonTypography` — 10 TextStyle roles including `monoCaps9_5` and `monoCaps11` needed for nav labels, status chip text, "ADD" caption, and countdown.
- `GiftMaisonShapes` — 7 radii (8/10/12/14/16/22/999) — the FAB uses 999, nav-item selection pill uses 999, bottom sheet uses 22.
- `GiftMaisonShadows` — FAB shadow (`0 8 20 {accent}55`) and bottom-sheet shadow (`0 -10 40 rgba(0,0,0,0.15)`) already defined as `Modifier` extensions.
- `GiftMaisonWordmark` composable — used on top bars, not directly needed for Phase 9 but exists.
- `StyleGuidePreview` (debug harness) — Phase 9 should append preview sections for the new chrome/status components to keep the style guide complete.

### Established Patterns
- **Navigation**: Navigation3 with `backStack: MutableList<Any>` in `AppNavigation.kt`. Nav entries are singleton `data object`/`data class` keys declared in the same file. All screen-level navigation is driven by mutating `backStack`.
- **State**: Hilt `@HiltViewModel` + `StateFlow` + `collectAsStateWithLifecycle()`. Firestore `callbackFlow { addSnapshotListener { ... } }` pattern in repositories.
- **String resources**: Every user-visible label lives in `strings.xml` + `values-ro/strings.xml` (I18N-02). Phase 9 must add keys: `nav_home_tab`, `nav_stores_tab`, `nav_lists_tab`, `nav_you_tab`, `add_sheet_title`, `add_sheet_new_registry`, `add_sheet_item_url`, `add_sheet_browse_stores`, `add_sheet_add_manually`, `status_chip_open`, `status_chip_given`, `status_chip_countdown_template` ("%dm"), plus helpers.
- **Existing status rendering**: `RegistryDetailScreen.kt` currently renders status inline (lines ~400–450). The new shared `StatusChip` composables must fully replace that inline code; Phase 9 ships the chip components AND re-points RegistryDetailScreen to them so CHROME / STAT requirements are exercised end-to-end.
- **Observation pattern for Lists-tab routing**: `RegistryListViewModel` already exposes a StateFlow of the user's registries. Lists-tab routing uses that flow's first entry as the `isPrimary` registry in Phase 9 — Phase 10 refines this with the real `isPrimary` field once the Home redesign exposes it.

### Integration Points
- **`AppNavigation.kt`** (373 lines) — the single integration point. The `Scaffold(bottomBar = { ... })` block and the `showsBottomNav()` extension are both replaced; the entry-provider map stays untouched.
- **`RegistryDetailScreen.kt`** (626 lines) — inline status chip code deleted; imports from `ui/common/status/` added.
- **`strings.xml` + `values-ro/strings.xml`** — new nav / sheet / status keys added in both locales.
- **`StyleGuidePreview`** (from Phase 8) — appended with new chrome/status preview sections for on-device verification.

</code_context>

<specifics>
## Specific Ideas

- **Handoff is the visual contract.** Every pixel spec in `design_handoff/design_handoff_android_owner_flow/README.md` (§ Shared chrome, § Status chips, § Bottom sheet) is a must-match requirement. The `pulse` keyframe, the `0 8 20 {accent}55` FAB shadow, the 4 px paper ring around the FAB, the 22-radius sheet corners, and the `{ink}55` scrim colour are all locked.
- **Purchased item stays visible.** Per handoff: "deliberate trust pattern — givers who later view the public page also see given items (still visible, not hidden) so nobody duplicates." Must not be filtered out of the list.
- **Countdown is display-only on owner screens.** Owners cannot manually reserve/release on behalf of givers (handoff § Interactions). The Nm label is informational; no tap handler.
- **FAB is the only entry point to the Add sheet.** Direct nav buttons to Create / Add / Browse / Manual are removed from the old tab bar and moved into the sheet. The only persistent direct-route is Home · Stores · Lists · You.

</specifics>

<deferred>
## Deferred Ideas

- **isPrimary pinning UI** — the handoff's "most recent" rule is used in Phase 9 for Lists-tab routing, but the Home card's dark-primary treatment and any explicit pin control is Phase 10 work (SCR-07).
- **Stores-tab / You-tab visual redesign** — the current `StoreListScreen` and `SettingsScreen` keep their Phase 7 / v1.0 layouts. Redesigning those is out of v1.1 scope entirely (handoff § Out of scope).
- **Occasion theming cascade** — Phase 9 chips use the Housewarming accent / secondSoft / second tokens directly. THEME-01..03 (per-registry occasion cascade) is deferred to v1.2.
- **Scrim blur on older Android** — `Modifier.blur(1.dp)` requires API 31+. On lower APIs, fall back to a plain `{ink}55` scrim without blur. Implement the fallback but note it in the SUMMARY.
- **Registry picker polish** — Phase 9 ships a minimal "pick a registry to add to" bottom-sheet list when Home-FAB actions fire with no registry context. Full picker UX (search, recents, empty-state CTA) is follow-up work.

</deferred>
