# Phase 11: Registry Detail + Create + Add Item Redesign - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Pixel-accurate GiftMaison re-skin of the three remaining owner-facing screens, preserving all existing behaviour (items query, reservation state, Create-registry repository path, Open Graph fetch, affiliate-tagging):

1. **Screen 08 — Registry detail** (`RegistryDetailScreen.kt`, ~626 lines post Phase 9 StatusChip rewire): 180 px hero with gradient + pinned toolbar, 4-stat strip, `accentSoft` share banner pill, horizontally scrolling filter chips (All · Open · Reserved · Completed), full-width item rows with 58 px thumbnail + shared Phase 9 StatusChip + always-visible overflow ⋯ button.

2. **Screen 09 — Create registry** (`CreateRegistryScreen.kt`): "Step 1 of 2" app bar with Skip, italic-accent "What's the **occasion?**" headline, 2×3 occasion tile grid (selected: accent bg + accentInk text + 1.5 px accent border; unselected: paperDeep bg + ink text + 1.5 px line border), name/date/time/place fields, visibility radio card (paperDeep bg + line border, accent radio), bottom CTA bar ("Continue · add items →" pushes AddItemKey for the new registry).

3. **Screen 10 — Add item (paste URL)** (`AddItemScreen.kt`): × close app bar, 3-tab segmented control (Paste URL / Browse stores / Manual) inside a pill track, URL field with mono-caps "⌕ Fetching from {domain}" + pulsing dot + affiliate confirmation row, 14-radius paperDeep preview card, auto-fill green tag, optional note field, secondSoft info pill, dual CTA bar (ghost "Add another" + primary "Save to registry ✓").

Out of scope: any new backend/data model changes (imageUrl already added Phase 10; views count deferred; draft status deferred). Phase 9 chrome (nav, FAB, sheet) is already wired at AppNavigation — not touched here.

</domain>

<decisions>
## Implementation Decisions

### Registry Detail (08)

- **Hero fallback when `registry.imageUrl == null`** — render a gradient placeholder (`accentSoft → accent` vertical fade) with the registry's occasion glyph centered (Instrument Serif italic, paper colour, 40 sp). No network, no first-item borrow, no stock photo. Populated real hero uses Coil 3 `AsyncImage` with a darkened overlay for text legibility.
- **Parallax treatment** — MVP: pinned toolbar fading from transparent to opaque (`paper @ alpha 0 → 1`) as list scrolls. No true parallax hero shift. Use `Modifier.graphicsLayer { alpha = computed }` tied to `LazyListState.firstVisibleItemScrollOffset`.
- **Stats strip (4 stats: items / reserved / given / views)** — `views` renders as `0` with a Phase 10-era todo to introduce `Registry.viewCount` in v1.2 (tracked as out-of-scope here). `items`, `reserved`, `given` derive from the existing `items: Flow<List<Item>>` — no new query.
- **Filter chips — `All · Open · Reserved · Completed`** — horizontally scrolling `LazyRow` on inkFaint outlined pills; active chip = ink-filled, paper text. Counts rendered inline (`All 12`, `Open 9`, `Reserved 2`, `Completed 1`). Map: `Open = ItemStatus.AVAILABLE`, `Reserved = ItemStatus.RESERVED`, `Completed = ItemStatus.PURCHASED` (domain mapping consistent with Phase 9 StatusChip dispatcher).
- **Item row layout** — 58×58 10-radius thumbnail left, column (title bodyM W500 single-line ellipsis + "price RON · retailer" body/mono row + optional reserver/giver sub-line in accent/second), right column (shared `StatusChip` stacked above 26×26 ⋯ circle with 1 dp line border), 1 dp `colors.line` bottom border except last.
- **Overflow ⋯ button** — **always visible** per handoff (not long-press). Tapping opens the existing DropdownMenu (Edit / Delete) that currently surfaces from long-press on the card. Long-press on the row is removed as a secondary entry point to reduce dual-gesture ambiguity.
- **Share banner** — accentSoft 10-radius pill, 26 px accent square with ↗ glyph, URL mono accent W600, helper "Tap to copy or share" body inkSoft. Tap copies the public shareable URL (format `https://gift-registry-ro.web.app/r/{registryId}`) AND launches the system share sheet; a snackbar confirms "Link copied". Wire to existing `/r/{id}` URL building in the web fallback.

### Create Registry (09)

- **"Step 1 of 2" flow** — literal 2-step. Continue CTA changes from current `onSaved → RegistryDetailKey(newId)` to `onSaved → AddItemKey(registryId = newId, initialRegistryId = newId)` per handoff. The new RegistryDetailKey is only reached after the user saves an item on step 2 or taps the × close on step 2. Step 2 (Add item) already renders the same "2 of 2" variant or similar via the AddItemScreen's existing app bar — do NOT hardcode a step counter on Add item; just land on it after Create.
- **"Skip" button behaviour** — creates a draft registry (title intentionally blank → classified Draft by Phase 10's `isDraft(title.isBlank() OR itemCount == 0)` heuristic) and pops to Home. The repository save still fires; no special draft flag.
- **2×3 occasion tile grid** — 6 tiles in fixed order matching the handoff glyph map:
  1. Housewarming — ⌂ (house glyph)
  2. Wedding — ♡ (heart)
  3. Baby — ◐ (half-moon)
  4. Birthday — ✦ (four-pointed star)
  5. Christmas — ❅ (snowflake)
  6. Custom — + (plus)
  Each tile: 14-radius, 14/16 padding, 6-gap column, 10-gap row. Selected: `bg = accent`, `content = accentInk`, `border = accent 1.5 dp`. Unselected: `bg = paperDeep`, `content = ink`, `border = line 1.5 dp`. Glyph at top (Instrument Serif italic, 22 sp, accent), label body 13 W500 below.
- **Visibility radio card** — paperDeep bg, line border, 12 radius, 12 padding. Mono-caps "VISIBILITY" label. Two radio rows (Public / Private). Selected radio = 18 px circle with 2 px accent border containing an 8 px accent dot. Map 1:1 to existing `Registry.visibility: String` ("public" / "private"); no 3rd mode added.
- **Bottom CTA bar** — `1 dp colors.line` border-top, `colors.paper` bg, 12/20 padding. Primary pill "Continue · add items →" full-width, `colors.ink` background, `colors.paper` content.

### Add Item URL (10)

- **Single screen with 3 modes** — one `AddItemScreen` file; 3-tab segmented switches modes.
  - **Paste URL mode (default)** — URL field visible, OG fetch pipeline active.
  - **Browse stores mode** — tap closes AddItem and pushes `StoreListKey(preSelectedRegistryId = registryId)`. Reuses Phase 7 StoreListScreen with the registry pre-selected so the store browser's "Add to list" CTA round-trips back to AddItemKey with the chosen URL.
  - **Manual mode** — URL field hidden; form fields (title, price, image URL, note) exposed for direct entry. Persists via the same `AddItemUseCase` (no new repository method).
- **"Fetching from {domain}" pulsing dot** — reuse Phase 9 `PulsingDot` composable with a new `period = 1_000.ms` argument. Confirm `PulsingDot` exposes a `period: Duration` param; if not, the Phase 11 plan adds it (backward-compatible overload) and Phase 9 call sites remain on the 1_400 ms default.
- **Affiliate confirmation row** — shown only when OG fetch succeeds AND the URL domain matches `AffiliateUrlTransformer.merchantRules` (the Phase 3 list: emag.ro + any added merchants). Row: mono `colors.ok` green "✓ Affiliate tag applied invisibly" + ghost "Clear" TextButton right that resets the URL field and collapses the row. Omitted entirely for non-matching URLs — no misleading "tag applied" for unknown merchants.
- **Preview card** — 14-radius paperDeep bg, `colors.line` border, 80×80 8-radius thumbnail left (Coil 3 AsyncImage from OG image URL), title body 14 W500 single-line, price body 13 W500 + "RON" mono tiny right, source line "emag.ro · via Open Graph" (monoCaps inkFaint) below.
- **"✓ auto-filled" green tag** next to the Title field label when OG metadata populated it — small `colors.ok` tag with check glyph + "auto-filled" body 11 W500.
- **Info pill** — `secondSoft` bg, 10 radius, `ℹ` glyph + caption "We recognized emag.ro and will add our affiliate tag. No impact on your guests." (body 12 W400, `colors.second`). Only shown when affiliate row is shown.
- **Dual CTA bar** — ghost "Add another" (flex 1) + primary "Save to registry ✓" (flex 1.5). "Add another" = save via AddItemUseCase + reset form for next item (stay on AddItem). "Save to registry" = save + pop to RegistryDetail.

### Claude's Discretion

- Exact composable file split for each screen (e.g., `RegistryDetailHero` + `StatsStrip` + `ShareBanner` + `FilterChipsRow` + `ItemRow` composables) is Claude's call. Keep single public screen entry points identical to current signatures.
- Occasion tile glyphs — chars above render in Instrument Serif italic. If any glyph doesn't have serif coverage, fall back to a close unicode equivalent (e.g., `🏠` fallback for `⌂` if serif italic fails at sp sizes).
- Share URL format — `https://gift-registry-ro.web.app/r/{registryId}` is the recommended default. If a different URL shape is already used elsewhere in the web fallback or email flows, match that.
- Filter chip state — `rememberSaveable` + enum class (All / Open / Reserved / Completed).
- Tab state on AddItem — `rememberSaveable` + enum (PasteUrl / BrowseStores / Manual), default PasteUrl.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets (Phase 8–10 shipped)
- `GiftMaisonTheme.colors / typography / shapes / spacing / shadows` — all tokens available.
- `GiftMaisonWordmark` — used in top bars.
- `GiftMaisonBottomNav` + `AddActionSheet` + `StatusChip` + `PulsingDot` + `Modifier.purchasedVisualTreatment()` — Phase 9 primitives consumed unchanged.
- `AvatarButton` + `SegmentedTabs` + `GoogleBanner` + `ConcentricRings` + `FocusedFieldCaret` + `TabFilters` + `AvatarInitials` + `AuthHeadline` — Phase 10 primitives; SegmentedTabs and FocusedFieldCaret relevant to Phase 11 (3-tab control + focused-input indicators).
- `Registry.imageUrl: String?` — available (Phase 10 addition). `viewCount` deferred.
- `AffiliateUrlTransformer.merchantRules` — Phase 3 merchant registry used to decide whether the affiliate confirmation row shows.

### Established Patterns
- **Navigation3**: `backStack: MutableList<Any>` in `AppNavigation.kt`; CreateRegistryKey, EditRegistryKey, AddItemKey, EditItemKey, RegistryDetailKey already exist. Phase 11 rewires `CreateRegistryScreen.onSaved` callback (parameter-level change in AppNavigation.kt entry<CreateRegistryKey>) from `RegistryDetailKey(newId)` to `AddItemKey(newId)` — minor wiring change.
- **Shared StatusChip**: `com.giftregistry.ui.common.status.StatusChip(status, expiresAt)` — RegistryDetail item rows use this as-is.
- **String resources (I18N-02)** — all new labels live in `values/strings.xml` + `values-ro/strings.xml`. Phase 11 adds ~30+ new keys covering: filter chips, share banner, stat labels, occasion tile labels, step indicator, URL field fetching prompt, affiliate confirmation, preview card labels, info pill, dual CTA labels, etc.
- **Coil 3 AsyncImage** — used for all network images (hero, thumbnails, preview card). Existing usage in RegistryListScreen.
- **DropdownMenu overflow** — existing item row overflow / card overflow uses M3 DropdownMenu. Phase 11 reuses it hanging off the always-visible ⋯ button instead of long-press.

### Integration Points
- **`RegistryDetailScreen.kt`** (~626 lines post-Phase-9) — scaffold + LazyColumn of item rows + header. Replace visuals but preserve Items StateFlow, delete flow, DropdownMenu contents, reservation status dispatch.
- **`CreateRegistryScreen.kt`** — replace the current occasion picker (dropdown/list) with the 2×3 tile grid composable. Rewire `onSaved` in AppNavigation.kt to push AddItemKey.
- **`AddItemScreen.kt`** — replace the Scaffold with the × close app bar + segmented tabs host. Add mode-aware content; wire Browse stores tab to push StoreListKey.
- **`AppNavigation.kt`** — update `entry<CreateRegistryKey>.onSaved` callback only. No new entry keys.
- **`strings.xml` + `values-ro/strings.xml`** — ~30+ new keys in both locales.
- **`StyleGuidePreview.kt`** — append Phase 11 preview sections: HeroPlaceholder, StatsStrip, FilterChipRow (4 states), ShareBanner, OccasionTileGrid (selected + unselected), AddItemSegmentedTabs, PreviewCard.
- **`PulsingDot.kt`** — add `period: Duration = PULSING_DOT_DEFAULT_PERIOD_MS.milliseconds` parameter if not already present. Phase 9 call sites (Reserved chip) pass no arg → default 1400. Phase 11 call site (fetching indicator) passes `period = 1_000.milliseconds`.

</code_context>

<specifics>
## Specific Ideas

- **Design handoff is the contract.** Every pixel in `design_handoff/design_handoff_android_owner_flow/README.md` § 08 Registry detail, § 09 Create registry, § 10 Add item is a must-match. Specific values locked: 180 px hero height, 58×58 thumbnail, 10/12/14/16/22 radii assignments, `0 8 20 {accent}55` FAB shadow (already shipped), gradient overlay `ink44 → transparent 40 % → inkAA` on hero.
- **Re-skin, not rebuild.** Preserve all existing ViewModels, repositories, use cases, Firestore paths, Cloud Function calls, Navigation3 back stack. Only Compose visuals change except: `CreateRegistryScreen.onSaved` rewiring and `PulsingDot` optional period arg.
- **Trust-signal copy is non-negotiable.** Affiliate confirmation row: "We recognized {domain} and will add our affiliate tag. No impact on your guests." — preserve this exact copy intent per handoff.
- **Shared URL format** — `https://gift-registry-ro.web.app/r/{registryId}` — if a different URL shape is used elsewhere, defer to that.

</specifics>

<deferred>
## Deferred Ideas

- **`Registry.viewCount` field** — needed for real Views stat on RegistryDetail. Deferred to v1.2. Phase 11 renders `0` with a todo.
- **Occasion theming cascade** (THEME-01/02/03) — per-registry occasion theme still deferred to v1.2. Phase 11 uses Housewarming palette only.
- **Dark mode** — deferred per Phase 8 Theme.kt.
- **Stores browser WebView chrome redesign** — deferred per v1.1 scope. Phase 11's "Browse stores" tab just routes to the existing StoreListScreen.
- **Manual mode image upload** — Manual tab's imageUrl field accepts a URL string; image upload / camera capture deferred.
- **Preview card error state** — handoff mentions "We couldn't read that page — fill in the details below" when scrape fails. Included in Phase 11 scope as a single inline message; richer error UI deferred.

</deferred>
