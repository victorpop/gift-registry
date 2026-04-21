# Phase 11: Registry Detail + Create + Add Item Redesign — Research

**Researched:** 2026-04-21
**Domain:** Jetpack Compose re-skin — three owner-facing screens preserving all existing behaviour
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Registry Detail (08)**
- Hero fallback when `registry.imageUrl == null`: gradient placeholder (accentSoft→accent) with occasion glyph centred (Instrument Serif italic, paper colour, 40 sp). No network borrow, no stock photo.
- Parallax MVP: pinned toolbar alpha-fades transparent→opaque (`paper @ 0→1`) as list scrolls. No true parallax hero shift. `LazyListState.firstVisibleItemScrollOffset` tied to `Modifier.graphicsLayer { alpha = computed }`.
- Stats strip (4 stats: items / reserved / given / views): `views` renders as `"0"` with a Phase 10-era todo. `items`, `reserved`, `given` derived from existing `items: Flow<List<Item>>`.
- Filter chips `All · Open · Reserved · Completed`: horizontally scrolling `LazyRow` with inkFaint outlined pills; active = ink-filled paper text. Counts inline. Domain: Open=AVAILABLE, Reserved=RESERVED, Completed=PURCHASED.
- Item row: 58×58 10-radius thumbnail left, content column, right column (StatusChip stacked above 26×26 ⋯ circle).
- Overflow ⋯ button is always visible. Long-press on row removed.
- Share banner taps: copy `https://gift-registry-ro.web.app/r/{registryId}` AND launch system share sheet; snackbar confirms "Link copied".

**Create Registry (09)**
- "Step 1 of 2" flow: Continue CTA → `AddItemKey(registryId = newId)`. New RegistryDetailKey reached after saving an item on step 2 or tapping × close on step 2.
- Skip button: saves draft (blank title → Draft heuristic per Phase 10) and pops to Home. Repository save fires.
- 2×3 occasion tile grid with fixed order: Housewarming ⌂, Wedding ♡, Baby ◐, Birthday ✦, Christmas ❅, Custom +. Selected: `bg=accent, content=accentInk, border=accent 1.5 dp`. Unselected: `bg=paperDeep, content=ink, border=line 1.5 dp`.
- Visibility radio card: paperDeep bg, line border, 12 radius. Map 1:1 to existing `Registry.visibility: String`.
- Bottom CTA: 1 dp line border-top, paper bg, ink pill button.

**Add Item URL (10)**
- Single screen with 3 modes. Paste URL = default.
- Browse Stores tab: immediately pushes `StoreListKey(preSelectedRegistryId = registryId)`.
- Manual mode: URL field hidden; same `AddItemUseCase`.
- PulsingDot with `period = 1_000.milliseconds`. (Param confirmed present in `PulsingDot.kt`.)
- Affiliate confirmation row: shown only when OG fetch succeeded AND `AffiliateUrlTransformer.merchantRules.any { url.contains(it.key) }`.
- Preview card: 14-radius paperDeep, `colors.line` border, 80×80 8-radius thumbnail.
- "✓ auto-filled" green tag next to Title label when OG populated it.
- Info pill: `secondSoft` bg with affiliate info copy.
- Dual CTA: ghost "Add another" (weight 1f, stay on screen + reset form) + primary "Save to registry ✓" (weight 1.5f, pop to RegistryDetail).

**All Screens**
- All new labels in `values/strings.xml` + `values-ro/strings.xml` (~30+ new keys, full table in UI-SPEC.md).
- `StyleGuidePreview.kt` gets 8 new @Preview sections.
- Re-skin only: preserve all existing ViewModels, repositories, use cases, Firestore paths, Cloud Function calls, Navigation3 back stack.

### Claude's Discretion
- Composable file split for each screen (e.g., `RegistryDetailHero` + `StatsStrip` etc.). Single public screen entry points must be identical to current signatures.
- Occasion tile glyphs: if glyph lacks Instrument Serif coverage, fall back to a close unicode equivalent.
- Share URL format: `https://gift-registry-ro.web.app/r/{registryId}` recommended; match if a different shape is used elsewhere.
- Filter chip state: `rememberSaveable` + enum class (All / Open / Reserved / Completed).
- AddItem tab state: `rememberSaveable` + enum (PasteUrl / BrowseStores / Manual), default PasteUrl.

### Deferred Ideas (OUT OF SCOPE)
- `Registry.viewCount` field — deferred to v1.2. Phase 11 renders `0` with a todo comment.
- Occasion theming cascade (THEME-01/02/03) — per-registry palette deferred to v1.2.
- Dark mode.
- Stores browser WebView chrome redesign.
- Manual mode image upload — imageUrl field accepts URL string only.
- Preview card richer error UI — only inline message in scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SCR-08 | Registry detail screen matches handoff (180 px hero with gradient + pinned toolbar, 4-stat strip, share banner pill, filter chips, full-width item rows with 58 px thumbnail + status chip + ⋯) | Hero, StatsStrip, ShareBanner, FilterChipsRow, RegistryItemRow composable specs fully documented in UI-SPEC.md; all token values verified in theme files |
| SCR-09 | Create registry screen matches handoff (Step 1 of 2 app bar with Skip, italic-accent headline, 2×3 occasion tile grid, form fields, visibility radio card, bottom CTA bar) | Existing occasion picker (ExposedDropdownMenuBox) to be replaced; `CreateRegistryScreen.onSaved` callback wiring in AppNavigation.kt identified as sole nav change; `OccasionTileGrid` + `VisibilityRadioCard` composable specs locked in UI-SPEC.md |
| SCR-10 | Add item (paste URL) screen matches handoff (× close app bar, 3-tab segmented, URL field with "Fetching from {domain}" + affiliate confirmation row, preview card, auto-fill tag, optional note, info pill, dual CTA bar) | `SegmentedTabs` confirmed accepts List<String> (not fixed arity); `PulsingDot.period` param confirmed present; `AffiliateUrlTransformer.merchantRules` access path identified; all AddItem composable specs locked in UI-SPEC.md |
</phase_requirements>

---

## Summary

Phase 11 is a **visual re-skin** of three screens: RegistryDetailScreen (~430 lines as-shipped), CreateRegistryScreen (~313 lines), and AddItemScreen (~216 lines). All existing ViewModels, repositories, use cases, and Firestore paths are preserved verbatim. The scope is Compose-layer changes only, with exactly two logic changes: (1) `AppNavigation.kt` wires `CreateRegistryKey.onSaved` to `AddItemKey` instead of `RegistryDetailKey`, and (2) `AddItemScreen` gains an `onSkip` callback path and a `onNavigateToBrowseStores` route. No new backend, no new domain models, no new nav keys.

All design tokens (colours, typography, spacing, shapes, shadows) are confirmed present in Phase 8–10 theme files. The `SegmentedTabs` composable is confirmed to accept a `List<String>` so 3-tab Add Item reuse is drop-in. The `PulsingDot.period: Duration` parameter is confirmed present at the default of 1400 ms; Phase 11 passes 1000 ms at its call site, no source change required to PulsingDot.kt. `AffiliateUrlTransformer.merchantRules` is a private `Map<String, (String) -> String>`; the affiliate row visibility condition `merchantRules.any { url.contains(it.key) }` must be exposed via a new internal helper or the existing `AffiliateUrlTransformer.transform(url).wasTransformed` return value post-OG-fetch.

Wave 0 RED tests cover six pure-Kotlin helpers: toolbar alpha computation, stats derivation, filter chip count aggregation, affiliate domain matching, occasion glyph mapping, and the AppNavigation wiring path.

**Primary recommendation:** Build each screen as a thin shell that wires existing VM StateFlows into the new composable sub-components listed in UI-SPEC.md § Component Inventory. Write Wave 0 RED tests for the six pure-Kotlin helpers first; implement composables in Wave 1; verify via StyleGuidePreview in Wave 2.

---

## Standard Stack

### Core (all already in project — no new dependencies)

| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| Jetpack Compose BOM | 2026.03.00 | UI framework | Already declared in `libs.versions.toml` |
| Coil | 3.4.0 | Image loading (hero, thumbnails, preview card) | Already used in RegistryDetailScreen + AddItemScreen |
| Material3 | via Compose BOM | M3 DropdownMenu, OutlinedTextField, etc. | Existing usage patterns preserved |
| Navigation3 | 1.0.1 | Back stack management | `AppNavigation.kt` — no new keys |
| Kotlin Coroutines / Flow | 1.9.x | StateFlow collection via `collectAsStateWithLifecycle` | Pattern established in all existing screens |
| GiftMaison theme | Phase 8 | All colour / typography / spacing / shape tokens | Fully verified — see Token Verification section below |

**Installation:** No new packages. `merchantRules` access may need a package-internal accessor if it stays `private` — see Pitfall 3.

---

## Architecture Patterns

### Pattern 1: Box over LazyColumn for hero + pinned toolbar (RegistryDetail)

The UI-SPEC calls for a `Box` (not `Scaffold`) as the outer container for RegistryDetail, so the hero image and pinned toolbar can be layered as absolute-position children over the scrolling `LazyColumn`.

```kotlin
// Source: 11-UI-SPEC.md § Screen 08 Layout
Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item { RegistryDetailHero(registry = registry, listState = listState) }
        item { StatsStrip(items = items) }
        item { ShareBanner(registryId = registryId) }
        item { FilterChipsRow(items = items, active = activeFilter, onSelect = { activeFilter = it }) }
        items(filteredItems, key = { it.id }) { item -> RegistryItemRow(item = item, ...) }
        item { Spacer(Modifier.height(90.dp)) } // nav bar padding
    }
    // Pinned toolbar is a sibling in the Box — NOT inside LazyColumn header
    RegistryDetailToolbar(
        toolbarAlpha = toolbarAlpha,
        onBack = onBack,
        onShare = onShare,
        onOverflow = { overflowMenuExpanded = true }
    )
}
```

**Toolbar alpha derivation:**
```kotlin
val toolbarAlpha by remember {
    derivedStateOf {
        (listState.firstVisibleItemScrollOffset / with(density) { 120.dp.toPx() })
            .coerceIn(0f, 1f)
    }
}
```
Note: `120.dp.toPx()` must be called inside a density context. The `density` value must be captured from `LocalDensity.current` outside the `derivedStateOf` lambda (lambdas passed to `derivedStateOf` must not read composition locals — capture the value before).

**Key insight:** `listState.firstVisibleItemScrollOffset` resets to 0 when the first item scrolls out of view (i.e., `firstVisibleItemIndex >= 1`). Add a guard:
```kotlin
val toolbarAlpha by remember {
    derivedStateOf {
        if (listState.firstVisibleItemIndex >= 1) 1f
        else (listState.firstVisibleItemScrollOffset / heroThresholdPx).coerceIn(0f, 1f)
    }
}
```
This ensures the toolbar stays fully opaque once the hero item is no longer visible.

### Pattern 2: Pure-Kotlin stats derivation (no new ViewModel query)

Stats are computed in the screen composable from the existing `items: List<Item>` state:

```kotlin
// Source: 11-UI-SPEC.md § Stats Strip
val itemCount = items.size
val reservedCount = items.count { it.status == ItemStatus.RESERVED }
val givenCount = items.count { it.status == ItemStatus.PURCHASED }
// viewCount = 0 // TODO v1.2: Registry.viewCount
```

These can be computed inside the composable or via a pure-Kotlin helper function — making them unit-testable without Compose.

### Pattern 3: AffiliateUrlTransformer.merchantRules for affiliate row visibility

`merchantRules` in `AffiliateUrlTransformer` is currently `private`. The affiliate confirmation row visibility condition is: "OG fetch succeeded AND domain matches any merchant rule."

Two approaches, in order of preference:

**Option A (preferred):** Use the already-public `AffiliateUrlTransformer.transform(url).wasTransformed` — this is called during item add anyway; the ViewModel can track whether the transformed result was applied. However, for the *UI* visibility check before saving, the composable needs to know if the current URL matches a merchant.

**Option B:** Add an internal `fun isAffiliateDomain(url: String): Boolean` to `AffiliateUrlTransformer`:
```kotlin
fun isAffiliateDomain(url: String): Boolean {
    val domain = extractDomain(url) ?: return false
    return merchantRules.keys.any { domain.endsWith(it) }
}
```
This is a one-line addition to the object, backward-compatible, and testable.

**Option C:** Expose the domain check through `AddItemViewModel` as a derived StateFlow:
```kotlin
val isAffiliateDomain: StateFlow<Boolean> = url.map { u ->
    AffiliateUrlTransformer.transform(u).wasTransformed
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
```
Limitation: `transform()` does a real URL parse which may have slightly different semantics than a pure-domain check.

**Recommendation:** Option B (add `isAffiliateDomain()` to `AffiliateUrlTransformer`) — cleanest separation, pure Kotlin, directly testable.

### Pattern 4: SegmentedTabs reuse on AddItem

`SegmentedTabs` at `ui/registry/list/SegmentedTabs.kt` accepts `tabs: List<String>` — confirmed not hardcoded to 3 labels. It uses `forEachIndexed` over the list. Drop-in reuse:

```kotlin
// Source: SegmentedTabs.kt (read 2026-04-21)
SegmentedTabs(
    tabs = listOf(
        stringResource(R.string.add_item_tab_url),
        stringResource(R.string.add_item_tab_browse),
        stringResource(R.string.add_item_tab_manual),
    ),
    selectedIndex = selectedTab.ordinal,
    onTabSelected = { selectedTab = AddItemTab.entries[it] },
)
```

Note: `AddItemTab.values()` is deprecated in Kotlin 1.9+. Use `AddItemTab.entries` instead.

### Pattern 5: Italic-accent headline (Create Registry)

The "What's the **occasion?**" headline in CreateRegistryScreen is NOT a single `Text`. Implement as two sibling `Text` composables in a `Column`:

```kotlin
// Source: 11-UI-SPEC.md § Headline
Column(modifier = Modifier.padding(horizontal = GiftMaisonTheme.spacing.edge)) {
    Text(
        text = stringResource(R.string.registry_create_headline_prefix), // "What's the"
        style = GiftMaisonTheme.typography.displayS,
        color = GiftMaisonTheme.colors.ink,
    )
    Text(
        text = stringResource(R.string.registry_create_headline_accent), // "occasion?"
        style = GiftMaisonTheme.typography.displayS.copy(fontStyle = FontStyle.Italic),
        color = GiftMaisonTheme.colors.accent,
    )
}
```

An `AnnotatedString` with `SpanStyle` for the italic-accent portion is a valid alternative and produces a single-line layout if both parts appear on one line. The two-Text approach matches the UI-SPEC and handles multi-line naturally.

### Pattern 6: Share intent + clipboard

```kotlin
// ClipboardManager + system share from a @Composable
val clipboardManager = LocalClipboardManager.current
val context = LocalContext.current

fun onShareTap() {
    clipboardManager.setText(AnnotatedString(shareUrl))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareUrl)
    }
    context.startActivity(Intent.createChooser(intent, null))
    // Show snackbar separately via LaunchedEffect or coroutine scope
}
```

### Pattern 7: Item row bottom border via `drawBehind`

```kotlin
Modifier.drawBehind {
    if (!isLast) {
        val strokeWidth = 1.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height - strokeWidth / 2),
            end = Offset(size.width, size.height - strokeWidth / 2),
            strokeWidth = strokeWidth,
        )
    }
}
```

### Pattern 8: Custom radio indicator (Visibility card)

Material3 `RadioButton` uses M3 tinting which may not match the design token `colors.accent`. Use a hand-drawn custom radio indicator instead:

```kotlin
// Source: 11-UI-SPEC.md § Visibility Radio Card
Box(
    modifier = Modifier
        .size(18.dp)
        .border(width = 2.dp, color = radioColor, shape = CircleShape),
    contentAlignment = Alignment.Center
) {
    if (isSelected) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(GiftMaisonTheme.colors.accent, CircleShape)
        )
    }
}
```
This avoids M3 RadioButton's ripple and tinting and exactly matches the handoff spec.

### Pattern 9: AppNavigation wiring change (two callbacks)

Current `entry<CreateRegistryKey>`:
```kotlin
entry<CreateRegistryKey> {
    CreateRegistryScreen(
        onBack = { backStack.removeLast() },
        onSaved = { registryId ->
            backStack.removeLast()
            backStack.add(RegistryDetailKey(registryId))  // CURRENT — Phase 11 changes this
        }
    )
}
```

Phase 11 target:
```kotlin
entry<CreateRegistryKey> {
    CreateRegistryScreen(
        onBack = { backStack.removeLast() },
        onSaved = { registryId ->
            backStack.removeLast()
            backStack.add(AddItemKey(registryId = registryId))  // Step 1→2 flow
        },
        onSkip = { _ ->
            backStack.removeLast()
            // no push — Skip pops to Home (whatever is below CreateRegistryKey on stack)
        }
    )
}
```

The `CreateRegistryScreen` composable must gain an `onSkip: (String) -> Unit` parameter. The ViewModel fires `savedRegistryId` both for Continue and Skip — the screen routes differently based on which callback should be called. Simplest: the composable exposes two separate "exit" paths — the ViewModel fires the same save, but the `onSaved` vs `onSkip` callbacks are separate triggers at the composable level (Skip triggers `onSkip`, the Continue CTA triggers `onSaved`).

Implementation in the VM layer: `viewModel.onSave()` fires for both; to distinguish, the composable can pass a `skipMode: Boolean` flag to a single-save action or the ViewModel can expose two methods `onSave()` and `onSkip()` that internally behave identically.

**Recommendation:** Distinguish at ViewModel level — `onSkipAndSave()` sets a flag before calling save; `LaunchedEffect(savedRegistryId)` checks the flag to choose the callback. Or simpler: two separate `savedRegistryId` state flows (`savedRegistryId` for Continue, `skippedRegistryId` for Skip).

### Recommended File Structure (new composables)

```
app/src/main/java/com/giftregistry/
  ui/registry/detail/
    RegistryDetailScreen.kt       # existing — shell + LazyColumn wiring
    RegistryDetailHero.kt         # NEW: hero Box + gradient overlay + pinned toolbar
    StatsStrip.kt                 # NEW: 4-stat horizontal row
    ShareBanner.kt                # NEW: accentSoft pill, clipboard + share intent
    FilterChipsRow.kt             # NEW: LazyRow with 4 ink-fill/outlined chips
    RegistryItemRow.kt            # NEW: 58×58 thumb + content + StatusChip + ⋯
  ui/registry/create/
    CreateRegistryScreen.kt       # existing — shell + form wiring
    OccasionTileGrid.kt           # NEW: 2×3 grid with animateColorAsState
    VisibilityRadioCard.kt        # NEW: custom radio indicator
  ui/item/add/
    AddItemScreen.kt              # existing — shell + tab switching
    FetchingIndicator.kt          # NEW: monoCaps + PulsingDot(1_000.ms)
    AffiliateConfirmationRow.kt   # NEW: ok-green text + Clear button
    ItemPreviewCard.kt            # NEW: 14-radius preview card
    AutoFillTag.kt                # NEW: ok-tinted pill tag
    InfoPill.kt                   # NEW: secondSoft row with ℹ + affiliate copy
    AddItemDualCtaBar.kt          # NEW: 1:1.5 flex ghost+primary bar
  ui/theme/preview/
    StyleGuidePreview.kt          # existing — append 8 new @Preview sections
```

### Anti-Patterns to Avoid

- **Using M3 TopAppBar for CreateRegistry/AddItem custom bars:** The UI-SPEC explicitly says "NOT Material3 TopAppBar" for both. Use a custom `Row`.
- **Using M3 RadioButton for visibility:** Tinting is M3-controlled. Use the custom `Box`+`Box` pattern above.
- **Passing density-dependent values into `derivedStateOf` lambdas:** `120.dp.toPx()` inside `derivedStateOf` will read `LocalDensity` from a non-composable context and crash at runtime. Capture outside: `val heroThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }`.
- **Keeping `firstVisibleItemScrollOffset` alone for toolbar alpha:** When `firstVisibleItemIndex >= 1`, the offset resets to 0. Add the `>= 1 → 1f` guard.
- **Using `AnnotatedString` with `SpanStyle` for multi-line italic-accent headline:** Fine for single-line, but multi-line wrapping behaves differently. Two separate `Text` composables is safer.
- **Using `Modifier.blur()` for the hero blur pill:** `Modifier.blur()` on API < 31 is a no-op or crashes. The blur pill is implemented as a semi-transparent `Box` overlay (not a true blur) per UI-SPEC: "implement as `Modifier.background(color = Color.Black.copy(alpha = 0.25f), shape = shapes.pill)`".
- **`AddItemTab.values()`:** Deprecated in Kotlin 1.9+. Use `AddItemTab.entries`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Segmented 3-tab control | Custom tab container | Existing `SegmentedTabs.kt` (Phase 10) | Already implements pill-track + animateColorAsState 200 ms; accepts List<String> |
| Status chip on item rows | New chip composable | Existing `StatusChip(status, expiresAt)` (Phase 9) | Already handles AVAILABLE/RESERVED/PURCHASED with PulsingDot wiring |
| Pulsing dot on fetching indicator | New animation | Existing `PulsingDot(color, size, period)` (Phase 9) | `period: Duration` param confirmed present; pass `1_000.milliseconds` |
| Purchased item visual dimming | Custom modifier | `Modifier.purchasedVisualTreatment()` (Phase 9) | Already applies 55% opacity + grayscale |
| Gradient overlay on hero | Manual drawWithContent | `Brush.verticalGradient(colorStops)` via `Modifier.background(brush)` | Compose-native, single call |
| System clipboard + share | Android ClipData + Intent directly | `LocalClipboardManager.current` + `Intent.ACTION_SEND` chooser | Standard patterns; no custom wrapper needed |
| Occasion glyph → string lookup | Ad-hoc when/if chain | Small `occasionGlyph(occasion: String): String` pure-Kotlin function | Centralisable, testable, reusable in both CreateRegistry tile grid and RegistryDetail hero placeholder |

---

## Token Verification

All theme tokens confirmed present in source (read 2026-04-21):

| Token Access | File | Status |
|---|---|---|
| `GiftMaisonTheme.colors.paper` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.paperDeep` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.ink` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.inkSoft` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.inkFaint` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.line` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.accent` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.accentInk` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.accentSoft` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.second` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.secondSoft` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.colors.ok` | `GiftMaisonColors.kt` | CONFIRMED |
| `GiftMaisonTheme.shapes.radius8..pill` | `GiftMaisonShapes.kt` | CONFIRMED (8/10/12/14/16/22/pill all present) |
| `GiftMaisonTheme.spacing.edge..gap20` | `GiftMaisonSpacing.kt` | CONFIRMED (edge/edgeWide/gap4-20 all present) |
| `PulsingDot(color, size, period)` | `PulsingDot.kt` | CONFIRMED — `period: Duration` is a named param with default `PULSING_DOT_DEFAULT_PERIOD_MS.milliseconds` (1400 ms) |
| `SegmentedTabs(tabs: List<String>, selectedIndex, onTabSelected)` | `SegmentedTabs.kt` | CONFIRMED — uses `forEachIndexed`, accepts any list length |
| `AffiliateUrlTransformer.transform(url)` | `AffiliateUrlTransformer.kt` | CONFIRMED — returns `TransformResult.wasTransformed: Boolean` and `merchantName: String?` |
| `AffiliateUrlTransformer.merchantRules` | `AffiliateUrlTransformer.kt` | CONFIRMED private — contains only `"emag.ro"` key currently; needs `isAffiliateDomain()` accessor |
| `Registry.imageUrl: String?` | `Registry.kt` | CONFIRMED nullable — Phase 10 addition |
| `Item.expiresAt: Long?` | `Item.kt` | CONFIRMED nullable |
| `StatusChip(status, expiresAt)` | Phase 9 — existing | confirmed imported in RegistryDetailScreen.kt |

---

## Common Pitfalls

### Pitfall 1: `firstVisibleItemScrollOffset` resets when hero item scrolls off-screen
**What goes wrong:** Hero item (index 0) scrolls fully off screen → `firstVisibleItemIndex` becomes 1 → `firstVisibleItemScrollOffset` resets to 0 → toolbar alpha snaps back to 0 (transparent) even though list is scrolled down.
**Why it happens:** `firstVisibleItemScrollOffset` is always relative to the first visible item, not the overall scroll position.
**How to avoid:** Guard with `if (listState.firstVisibleItemIndex >= 1) return 1f` before applying the offset calculation.
**Warning signs:** Toolbar flashes transparent after scrolling past the hero.

### Pitfall 2: Density access inside `derivedStateOf`
**What goes wrong:** `derivedStateOf { (listState.firstVisibleItemScrollOffset / 120.dp.toPx()).coerceIn(0f, 1f) }` — `120.dp.toPx()` reads `LocalDensity` inside a non-composable lambda.
**Why it happens:** `derivedStateOf` runs outside the composition; composition locals are not accessible.
**How to avoid:** `val heroThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }` before `remember { derivedStateOf { ... } }`, then reference `heroThresholdPx` in the lambda.

### Pitfall 3: `AffiliateUrlTransformer.merchantRules` is private
**What goes wrong:** A screen composable or ViewModel cannot read `merchantRules` keys directly to implement the affiliate row visibility condition.
**How to avoid:** Add `fun isAffiliateDomain(url: String): Boolean` as an internal/public method to `AffiliateUrlTransformer` — a one-line pure-Kotlin addition. Alternatively, expose visibility state via `AddItemViewModel.isAffiliateDomain: StateFlow<Boolean>` derived from the current `url` StateFlow using `transform()`.

### Pitfall 4: `Box` vs `Scaffold` choice for RegistryDetail
**What goes wrong:** Using `Scaffold` for RegistryDetail with a hero image — the `topBar` slot adds static height before the content area, breaking the "hero is part of the scroll content" design.
**Why it happens:** `Scaffold(topBar)` occupies fixed vertical space; you cannot scroll content under it.
**How to avoid:** Use `Box` as the outer container per UI-SPEC. The GiftMaisonBottomNav is wired from AppNavigation's outer Scaffold — not from RegistryDetailScreen's Scaffold — so removing Scaffold from RegistryDetailScreen does not remove the bottom nav. The existing `RegistryDetailScreen` currently uses `Scaffold` — Phase 11 replaces this with a `Box`.

### Pitfall 5: FAB removal from RegistryDetailScreen
**What goes wrong:** Current `RegistryDetailScreen.kt` has a `floatingActionButton = { FloatingActionButton(...) }` inside its `Scaffold`. If Phase 11 removes the `Scaffold`, the FAB is also removed.
**What to do:** The GiftMaison FAB (Phase 9 AddActionSheet trigger) is wired from AppNavigation's outer Scaffold, not from individual screens. The per-screen FAB in the current RegistryDetailScreen is a legacy artefact from before Phase 9. Phase 11 should remove it as part of the re-skin — the global FAB in AppNavigation handles add-item navigation.

### Pitfall 6: `occasionOptions` list in CreateRegistryScreen uses old string keys
**What goes wrong:** The current `CreateRegistryScreen.kt` (lines 83–91) builds `occasionOptions` from string resources: `registry_occasion_wedding`, `registry_occasion_baby_shower`, `registry_occasion_anniversary`, `registry_occasion_christmas`, `registry_occasion_birthday`, `registry_occasion_custom`. Phase 11 maps occasion to the tile grid with different UI labels: Housewarming / Wedding / Baby / Birthday / Christmas / Custom.
**Action:** Phase 11 adds new string keys (`registry_occasion_housewarming`, `registry_occasion_baby` — note: "Baby" not "Baby shower", "Birthday" not renamed). The `viewModel.occasion.value` can continue storing the display string; the ViewModel doesn't validate against an enum. Confirm that `CreateRegistryViewModel.occasion` is a `MutableStateFlow<String>` (it is — confirmed in CreateRegistryScreen.kt line 68).

### Pitfall 7: `drawBehind` + recomposition performance on LazyColumn
**What goes wrong:** Drawing a bottom border via `drawBehind` on each `RegistryItemRow` runs on every recomposition.
**How to avoid:** The `drawBehind` lambda captures `isLast: Boolean` and `lineColor: Color` from the composable scope. Ensure both are stable (neither changes on each frame). `isLast` is derived from `index == items.lastIndex` in the LazyColumn block — stable per item. `lineColor` is from `GiftMaisonTheme.colors.line` — stable (CompositionLocal is immutable per GiftMaisonColors Immutable annotation). No action needed; this pattern is correct.

### Pitfall 8: `DropdownMenu` placement in the always-visible ⋯ overflow button
**What goes wrong:** `DropdownMenu` must be placed inside a `Box` that is the anchor, and the `Box` must be the parent of both the `IconButton` and the `DropdownMenu`. If placed incorrectly, the dropdown appears at the wrong position.
**How to avoid:** Use the same `Box { IconButton { ... } DropdownMenu(expanded = ...) }` pattern already present in the current `RegistryDetailScreen.kt` `ItemCard` composable (lines 542–583). Phase 11 preserves this structure inside `RegistryItemRow`.

---

## Code Examples

### Hero gradient overlay (exact spec from UI-SPEC.md)

```kotlin
// Source: 11-UI-SPEC.md § Color, confirmed in 11-CONTEXT.md
val inkTop = Color(0xFF2A2420).copy(alpha = 0.27f)    // ink@0x44
val inkBottom = Color(0xFF2A2420).copy(alpha = 0.67f) // ink@0xAA
val heroGradient = Brush.verticalGradient(
    0f to inkTop,
    0.4f to Color.Transparent,
    1f to inkBottom,
)
// Applied as:
Box(modifier = Modifier.fillMaxSize().background(brush = heroGradient))
```

### Hero placeholder (no imageUrl)

```kotlin
// Source: 11-UI-SPEC.md § Hero
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .background(
            brush = Brush.verticalGradient(
                0f to GiftMaisonTheme.colors.accentSoft,
                1f to GiftMaisonTheme.colors.accent,
            )
        ),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = occasionGlyph(registry.occasion), // e.g. "⌂" for Housewarming
        style = GiftMaisonTheme.typography.displayXL.copy(
            fontSize = 40.sp,
            fontStyle = FontStyle.Italic,
        ),
        color = GiftMaisonTheme.colors.paper,
    )
}
```

### Occasion glyph pure-Kotlin helper (unit-testable)

```kotlin
// Suggested location: ui/registry/create/OccasionTileGrid.kt or a shared ui/util/OccasionGlyph.kt
fun occasionGlyph(occasion: String): String = when {
    occasion.equals("Housewarming", ignoreCase = true) -> "⌂"
    occasion.equals("Wedding", ignoreCase = true)      -> "♡"
    occasion.equals("Baby", ignoreCase = true)         -> "◐"
    occasion.equals("Birthday", ignoreCase = true)     -> "✦"
    occasion.equals("Christmas", ignoreCase = true)    -> "❅"
    else                                               -> "+"
}
```

### Filter chip state enum

```kotlin
// rememberSaveable requires a Saver when the state is a non-primitive.
// Use a simple Int-backed approach per STATE.md Phase 10 decision log:
// "Tab index uses Int via rememberSaveable mutableIntStateOf(0), not sealed class"
// However, for readability an enum is preferable here if a custom Saver is provided.
// Alternative: just use mutableIntStateOf + a mapping function.
enum class RegistryFilterChip { All, Open, Reserved, Completed }

// Simpler approach (per Phase 10 pattern):
var activeFilterIndex by rememberSaveable { mutableIntStateOf(0) }
val activeFilter = RegistryFilterChip.entries[activeFilterIndex]
```

### Affiliate domain check (recommended helper)

```kotlin
// Proposed addition to AffiliateUrlTransformer.kt
fun isAffiliateDomain(url: String): Boolean {
    val domain = extractDomain(url) ?: return false
    return merchantRules.keys.any { domain.endsWith(it) }
}
```

### Toolbar alpha derivation (safe pattern)

```kotlin
val density = LocalDensity.current
val heroThresholdPx = remember(density) { with(density) { 120.dp.toPx() } }
val listState = rememberLazyListState()

val toolbarAlpha by remember(listState) {
    derivedStateOf {
        if (listState.firstVisibleItemIndex >= 1) 1f
        else (listState.firstVisibleItemScrollOffset / heroThresholdPx).coerceIn(0f, 1f)
    }
}
```

---

## Runtime State Inventory

This section is not applicable — Phase 11 is a visual re-skin with no renames, no rebranding, no migration of stored data keys, and no changes to Firestore collection names or document fields.

---

## Environment Availability

Phase 11 is purely Compose-layer changes. No external tools, services, runtimes, or CLIs beyond the existing Android build toolchain are required.

**SKIPPED** — no external dependencies identified.

---

## Validation Architecture

`workflow.nyquist_validation` is `true` in `.planning/config.json` — this section is required.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (project-standard, confirmed by existing test files) |
| Config file | None — standard Android test source set |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*" --tests "com.giftregistry.ui.registry.create.*" --tests "com.giftregistry.ui.item.add.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SCR-08 | Stats derivation (itemCount, reservedCount, givenCount) from item list | unit | `./gradlew :app:testDebugUnitTest --tests "*.StatsDerivationTest"` | ❌ Wave 0 |
| SCR-08 | Filter chip count aggregation (All/Open/Reserved/Completed counts) | unit | `./gradlew :app:testDebugUnitTest --tests "*.FilterChipCountTest"` | ❌ Wave 0 |
| SCR-08 | Toolbar alpha derivation: 0f at offset 0, 1f at offset >= 120dp, 1f when firstVisibleItemIndex >= 1 | unit | `./gradlew :app:testDebugUnitTest --tests "*.ToolbarAlphaTest"` | ❌ Wave 0 |
| SCR-08 | Occasion glyph mapping (6 occasions → 6 glyphs, unknown → "+") | unit | `./gradlew :app:testDebugUnitTest --tests "*.OccasionGlyphTest"` | ❌ Wave 0 |
| SCR-09 | OccasionTileGrid: selected tile has accent bg + accentInk text; unselected has paperDeep bg + ink text | visual | StyleGuidePreview @Preview | manual |
| SCR-09 | Occasion tile grid renders correct glyph per occasion (same glyph map as hero placeholder) | unit | covered by OccasionGlyphTest above | ❌ Wave 0 |
| SCR-10 | Affiliate domain matching: emag.ro URLs return true, unknown URLs return false | unit | `./gradlew :app:testDebugUnitTest --tests "*.AffiliateDomainTest"` | ❌ Wave 0 |
| SCR-10 | AddItem tab state: BrowseStores triggers navigation, resets to PasteUrl | unit (logic only) | `./gradlew :app:testDebugUnitTest --tests "*.AddItemTabBehaviorTest"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*" --tests "com.giftregistry.ui.registry.create.*" --tests "com.giftregistry.ui.item.add.*"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/StatsDerivationTest.kt` — covers SCR-08 stats (itemCount/reservedCount/givenCount pure-Kotlin functions)
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/FilterChipCountTest.kt` — covers SCR-08 filter chip counts for All/Open/Reserved/Completed
- [ ] `app/src/test/java/com/giftregistry/ui/registry/detail/ToolbarAlphaTest.kt` — covers SCR-08 toolbar alpha computation including the `firstVisibleItemIndex >= 1 → 1f` guard
- [ ] `app/src/test/java/com/giftregistry/ui/registry/create/OccasionGlyphTest.kt` — covers SCR-08 hero placeholder + SCR-09 tile grid (same glyph map)
- [ ] `app/src/test/java/com/giftregistry/ui/item/add/AffiliateDomainTest.kt` — covers SCR-10 `isAffiliateDomain()` (emag.ro matches, unknown domain does not)
- [ ] `app/src/test/java/com/giftregistry/ui/item/add/AddItemTabBehaviorTest.kt` — covers SCR-10 tab state logic (BrowseStores triggers nav + resets to PasteUrl)

All Wave 0 tests cover pure-Kotlin logic extracted from composables (following Phase 8–10 pattern where complex logic is in top-level functions or small helpers, not inline in @Composable lambdas). No Compose UI testing framework needed.

---

## State of the Art

| Old Approach (existing screens) | Phase 11 Approach | Impact |
|---|---|---|
| `Scaffold(topBar = { TopAppBar(...) })` in RegistryDetailScreen | `Box` outer container, manual inset handling | Enables hero to be part of scroll content; pinned toolbar overlaid via `zIndex(1f)` |
| `ExposedDropdownMenuBox` for occasion selection (CreateRegistry) | `OccasionTileGrid` — 2×3 tap-to-select grid | Visual parity with handoff; no dropdown overlay |
| Standard `RadioButton` for visibility (CreateRegistry) | Custom `Box`+`Box` radio indicator | Exact token colour match; no M3 ripple override needed |
| Single save-and-navigate callback `onSaved` (CreateRegistry) | `onSaved` (Continue) + `onSkip` (Skip) two separate callbacks | Implements Step 1→2 flow cleanly |
| Single CTA "Save" button (AddItem) | Dual CTA bar: ghost "Add another" + primary "Save to registry ✓" | Enables multi-item add session from one screen |
| `CircularProgressIndicator` for OG fetch progress (AddItem) | `FetchingIndicator`: monoCaps text + `PulsingDot(1_000.ms)` | On-brand visual; re-uses existing Phase 9 composable |
| Long-press to show item overflow menu (RegistryDetail) | Always-visible 26×26 ⋯ button with `DropdownMenu` | Discoverable; no dual-gesture ambiguity |
| FAB in RegistryDetailScreen's own Scaffold | FAB removed from screen — global Phase 9 GiftMaisonBottomNav FAB handles navigation | Single FAB source of truth; Phase 9 chrome not duplicated |

---

## Open Questions

1. **`occasionOptions` string key migration in CreateRegistryViewModel**
   - What we know: `CreateRegistryViewModel.occasion` is a `MutableStateFlow<String>` storing the display string. Current values set from `registry_occasion_wedding`, `registry_occasion_baby_shower`, `registry_occasion_anniversary`, `registry_occasion_christmas`, `registry_occasion_birthday`, `registry_occasion_custom`.
   - What's unclear: Phase 11 introduces `registry_occasion_housewarming` (new, was "Anniversary" in old UI) and `registry_occasion_baby` (shortened from "Baby shower"). If existing Firestore registry documents have `occasion = "Baby shower"`, the glyph map won't match.
   - Recommendation: The `occasionGlyph()` function should use case-insensitive prefix matching or contain the legacy strings as additional cases. Since occasions are display strings stored in Firestore, add legacy aliases (`"Baby shower"` → ◐, `"Anniversary"` → ⌂ or "+" since no Housewarming existed) to the glyph map for backward compat. Document clearly as a tech debt comment.

2. **`common_close` string key — does it exist?**
   - What we know: The × close button on AddItem requires `contentDescription = stringResource(R.string.common_close)`. The UI-SPEC says "add if missing."
   - What's unclear: Whether `common_close` exists in `strings.xml`.
   - Recommendation: Plan 01 (Wave 0 + strings setup) should add `common_close` to both locales if missing, alongside the 30+ new Phase 11 keys.

3. **`onNavigateToRegistry` callback on RegistryDetailScreen**
   - What we know: The current `RegistryDetailScreen` has `onNavigateToRegistry: (String) -> Unit = {}` as a parameter with a default no-op, used for FCM push snackbar action navigation.
   - What's unclear: Phase 11 re-skin must preserve this parameter. The new composable signature must retain this parameter.
   - Recommendation: Keep unchanged. The parameter signature is already optional (defaults to no-op) and wired correctly in AppNavigation.

---

## Project Constraints (from CLAUDE.md)

- **Tech stack**: Kotlin only. Firebase as the only persistence layer. No SQLite.
- **No KTX modules**: Use main Firebase modules (`firebase-auth`, `firebase-firestore`) not `-ktx` variants.
- **Localization**: ALL new UI labels must go in `res/values/strings.xml` (English) AND `res/values-ro/strings.xml` (Romanian). Never hardcode strings.
- **No LiveData**: Use Kotlin StateFlow + `collectAsStateWithLifecycle()`.
- **No Room / SQLite**: All shared state is Firestore.
- **Compose only**: No XML layouts, no Fragment navigation.
- **Navigation3 1.0.1**: No mixing with Navigation Compose 2.x.
- **GSD workflow enforcement**: All file changes must go through a GSD workflow entry point.
- **Hilt with KSP** (not KAPT): Phase 02 decision — `ksp` for annotation processing.
- **AGP 9.x**: No separate `kotlin-android` plugin.
- **Firebase BoM 34.11.0**: No KTX sub-module imports.
- **Coil 3**: Use `coil3.compose.AsyncImage`, not Coil 2 APIs.
- **Material3 only**: M3 components throughout (M2 is out of scope).
- **No hardcoded dp literals in new composables**: Use `GiftMaisonTheme.spacing.*` tokens for all spacing on new composables (exception: the 90 dp nav padding bottom item in LazyColumn, which is a device layout concern, not a design token).

---

## Sources

### Primary (HIGH confidence)

- `app/src/main/java/com/giftregistry/ui/common/status/PulsingDot.kt` — `period: Duration` parameter confirmed present; `PULSING_DOT_DEFAULT_PERIOD_MS = 1_400L`
- `app/src/main/java/com/giftregistry/ui/registry/list/SegmentedTabs.kt` — confirmed accepts `List<String>`, uses `forEachIndexed`
- `app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt` — `merchantRules` is `private`, contains only `"emag.ro"` key; `transform()` returns `TransformResult.wasTransformed`
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonColors.kt` — all 13 colour tokens confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShapes.kt` — all 7 shape tokens confirmed (radius8..pill)
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonSpacing.kt` — all 10 spacing tokens confirmed (edge/edgeWide/gap4-20)
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — `CreateRegistryKey.onSaved` wiring confirmed; `AddItemKey` signature confirmed
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — all nav keys confirmed; no new keys needed for Phase 11
- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` — `imageUrl: String?` confirmed (Phase 10 addition)
- `app/src/main/java/com/giftregistry/domain/model/Item.kt` — `expiresAt: Long?`, `status: ItemStatus` confirmed
- `.planning/phases/11-registry-detail-create-add-item-redesign/11-UI-SPEC.md` — full design contract (approved 2026-04-21)
- `.planning/phases/11-registry-detail-create-add-item-redesign/11-CONTEXT.md` — 18 locked decisions (ready for planning)
- `.planning/config.json` — `nyquist_validation: true` confirmed
- `CLAUDE.md` — tech stack constraints verified

### Secondary (MEDIUM confidence)

- Phase 09 decision log (STATE.md): "pure-Kotlin unit tests only; Compose UI test scaffolding deferred; unit tests cover logic" — confirms Phase 11 test strategy follows same pattern
- Phase 10 decision log (STATE.md): "Tab index uses Int via rememberSaveable mutableIntStateOf(0), not sealed class" — informs AddItemTab state approach; however using enum with mutableIntStateOf is equally valid

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries confirmed in-project; no new dependencies
- Architecture patterns: HIGH — patterns confirmed from reading actual source files; Pitfall guards derived from Compose docs knowledge
- Pitfalls: HIGH — sourced from direct code inspection (RegistryDetailScreen.kt, AppNavigation.kt, PulsingDot.kt)
- Test strategy: HIGH — follows established Phase 8–10 pure-Kotlin unit test pattern confirmed in STATE.md

**Research date:** 2026-04-21
**Valid until:** 2026-05-21 (stable internal codebase; no external API dependencies)
