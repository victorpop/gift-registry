---
quick_id: 260530-ncw
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
  - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
  - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
autonomous: false
requirements:
  - NCW-260530-01  # Icon-only layout toggle on Discover, top-right, swaps 1-col / 2-col, swaps icon
  - NCW-260530-02  # Toggle applies to BOTH community + search sections AND to shimmer skeletons
  - NCW-260530-03  # Localized content descriptions (en + ro), default = 1-col, session-local state
must_haves:
  truths:
    - "User sees an icon-only toggle button in the top-right of the Discover screen on first open."
    - "Default state is one-per-row, matching the current Discover visual."
    - "When in one-per-row mode, the toggle shows the 2x2 grid icon (Icons.Outlined.GridView)."
    - "Tapping the toggle in one-per-row switches the screen to two-per-row and the icon becomes the list icon (Icons.Outlined.ViewAgenda)."
    - "Tapping again returns to one-per-row and restores the grid icon."
    - "Both FROM THE COMMUNITY and FROM THE WEB product cards lay out using the same column count."
    - "Loading shimmer placeholders use the same column count as real cards."
    - "The search bar and section headers (FROM THE COMMUNITY / FROM THE WEB) stay full-width in both modes."
    - "Re-launching the app returns to the one-per-row default (no persistence)."
    - "TalkBack reads a localized content description that reflects the action the icon will perform."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt"
      provides: "DiscoverLayoutMode sealed enum (OneColumn / TwoColumns) with columnCount property"
      contains: "DiscoverLayoutMode"
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt"
      provides: "layoutMode StateFlow + toggleLayoutMode() function"
      contains: "toggleLayoutMode"
    - path: "app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt"
      provides: "LazyVerticalGrid + IconButton toggle in top-right; full-span items for search bar + section headers"
      contains: "LazyVerticalGrid"
    - path: "app/src/main/res/values/strings.xml"
      provides: "discover_layout_toggle_to_grid + discover_layout_toggle_to_list (en)"
      contains: "discover_layout_toggle_to_grid"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Same two keys (ro)"
      contains: "discover_layout_toggle_to_grid"
    - path: "app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt"
      provides: "@Preview blocks for 1-col and 2-col Discover layouts so future visual review covers both modes"
      contains: "Discover (2-col)"
  key_links:
    - from: "DiscoverScreen.kt IconButton"
      to: "DiscoverViewModel.toggleLayoutMode()"
      via: "onClick lambda"
      pattern: "viewModel\\.toggleLayoutMode\\(\\)"
    - from: "DiscoverScreen.kt LazyVerticalGrid"
      to: "layoutMode.columnCount"
      via: "GridCells.Fixed(layoutMode.columnCount)"
      pattern: "GridCells\\.Fixed\\("
    - from: "DiscoverScreen.kt search bar + section headers"
      to: "full-width row in LazyVerticalGrid"
      via: "item(span = { GridItemSpan(maxLineSpan) })"
      pattern: "GridItemSpan\\(maxLineSpan\\)"
---

<objective>
Add a session-local layout toggle (1-col ↔ 2-col) to the Discover screen. The
toggle is an icon-only button in the upper-right of the Discover content area
that swaps both the column count and its own icon on tap. Both the FROM THE
COMMUNITY and FROM THE WEB sections — including their shimmer placeholders —
must respect the current mode. Default = 1-col (preserves the just-shipped
Phase 17 look). State lives in the existing `DiscoverViewModel` as a StateFlow
— no persistence across app restarts.

Purpose: Give the user a denser grid view of Discover results so more products
fit on-screen without forcing it as the default.

Output: An icon-only toggle in the top-right of Discover, a `LazyVerticalGrid`
rendering both sections at the selected column count, two localized content-
description strings, and previews of both modes in the style guide.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md

<!-- Source files touched by this plan -->
@app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt
@app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
@app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt
@app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt
@app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml
@app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt

<interfaces>
<!-- Existing contracts the executor will build on. No codebase exploration needed. -->

From app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt:
```kotlin
sealed interface PopularState {
    data object Loading : PopularState
    data class Loaded(val products: List<DiscoverProduct>) : PopularState
    data object Empty : PopularState
    data class Error(val message: String) : PopularState
}
sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Loaded(val products: List<DiscoverProduct>) : SearchState
    data object Empty : SearchState
    data class Error(val message: String) : SearchState
}
```

From app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt:
```kotlin
val popular: StateFlow<PopularState>
val search: StateFlow<SearchState>
val searchQuery: StateFlow<String>
fun loadPopular()
fun onQueryChange(query: String)
fun search(query: String)
fun retrySearch()
```

From app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt:
```kotlin
@Composable
fun DiscoverProductCard(
    product: DiscoverProduct,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
)
```
NOTE: Title is `maxLines = 2`, description is `maxLines = 2`, retailerName is `maxLines = 1`.
Image uses `aspectRatio(16f / 9f)`. Card uses `fillMaxWidth()` — already adapts to its parent
grid cell width, so no layout fork is needed; the only risk is text overflow at narrow widths.

From app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt:
```kotlin
@Composable
fun DiscoverShimmerCard(modifier: Modifier = Modifier)
```

From app/build.gradle.kts (line 69):
```
implementation("androidx.compose.material:material-icons-extended")
```
=> `Icons.Outlined.GridView` and `Icons.Outlined.ViewAgenda` are AVAILABLE on the classpath.
Use them directly — do NOT create custom vector drawables.

Material 3 icon-button import pattern (already used elsewhere in the codebase):
```kotlin
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
```

Existing string keys to follow as a naming-style template:
```
discover_section_web, discover_section_community, discover_empty_search,
discover_empty_popular, discover_error_load, discover_error_search,
discover_retry, discover_no_browser_toast
```
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: State + localized strings — DiscoverLayoutMode, ViewModel toggle, en+ro content descriptions</name>
  <files>
    app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt,
    app/src/main/res/values/strings.xml,
    app/src/main/res/values-ro/strings.xml
  </files>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt (current sealed interfaces)
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt (current StateFlow patterns)
    - app/src/main/res/values/strings.xml (existing `discover_*` keys near line 232-240)
    - app/src/main/res/values-ro/strings.xml (existing `discover_*` keys near line 233-241)
  </read_first>
  <action>
    1) In `DiscoverUiState.kt`, ADD (do not modify the existing `PopularState` / `SearchState` interfaces) the following sealed enum:

       ```kotlin
       /**
        * 260530-ncw — session-local layout mode for the Discover screen.
        * Default = OneColumn (preserves the Phase 17 visual). TwoColumns is
        * user-toggled via the icon button in the top-right; not persisted.
        */
       sealed interface DiscoverLayoutMode {
           val columnCount: Int
           data object OneColumn : DiscoverLayoutMode { override val columnCount: Int = 1 }
           data object TwoColumns : DiscoverLayoutMode { override val columnCount: Int = 2 }
       }
       ```

    2) In `DiscoverViewModel.kt`, ADD (alongside `_popular`, `_search`, `_searchQuery`):

       ```kotlin
       private val _layoutMode = MutableStateFlow<DiscoverLayoutMode>(DiscoverLayoutMode.OneColumn)
       val layoutMode: StateFlow<DiscoverLayoutMode> = _layoutMode.asStateFlow()

       fun toggleLayoutMode() {
           _layoutMode.value = when (_layoutMode.value) {
               DiscoverLayoutMode.OneColumn -> DiscoverLayoutMode.TwoColumns
               DiscoverLayoutMode.TwoColumns -> DiscoverLayoutMode.OneColumn
           }
       }
       ```

       Do NOT touch `init {}` or the existing load/search/retry methods.

    3) In `app/src/main/res/values/strings.xml`, ADD next to the other `discover_*` keys (after line 240, before the closing of the relevant group — keep them grouped with the other `discover_*` strings):

       ```xml
       <string name="discover_layout_toggle_to_grid">Switch to two-column grid</string>
       <string name="discover_layout_toggle_to_list">Switch to single-column list</string>
       ```

       Semantics: the string describes the ACTION the icon will perform (TalkBack convention), not the current state. So when the screen is in `OneColumn` and the GridView icon is shown, the contentDescription is `discover_layout_toggle_to_grid` ("Switch to two-column grid"). When in `TwoColumns` showing the ViewAgenda icon, the contentDescription is `discover_layout_toggle_to_list`.

    4) In `app/src/main/res/values-ro/strings.xml`, ADD the matching Romanian translations next to the other `discover_*` keys:

       ```xml
       <string name="discover_layout_toggle_to_grid">Comută la grilă cu două coloane</string>
       <string name="discover_layout_toggle_to_list">Comută la listă cu o singură coloană</string>
       ```

       (Use proper Romanian diacritics — `ă`, `ă` — exactly as shown. The file uses numeric XML entities for diacritics in some places; either entity form or the literal character is fine — match whichever the surrounding lines use.)

    Do NOT touch `DiscoverShimmer.kt`, `DiscoverScreen.kt`, `DiscoverProductCard.kt`, or `DiscoverViewModelTest.kt` in this task.
  </action>
  <verify>
    <automated>grep -n "DiscoverLayoutMode" app/src/main/java/com/giftregistry/ui/discover/DiscoverUiState.kt &amp;&amp; grep -n "toggleLayoutMode\|_layoutMode" app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt &amp;&amp; grep -n "discover_layout_toggle_to_grid\|discover_layout_toggle_to_list" app/src/main/res/values/strings.xml &amp;&amp; grep -n "discover_layout_toggle_to_grid\|discover_layout_toggle_to_list" app/src/main/res/values-ro/strings.xml</automated>
  </verify>
  <done>
    - `DiscoverUiState.kt` exports a `DiscoverLayoutMode` sealed interface with `OneColumn` (columnCount=1) and `TwoColumns` (columnCount=2).
    - `DiscoverViewModel` exposes `layoutMode: StateFlow&lt;DiscoverLayoutMode&gt;` defaulting to `OneColumn` and a `toggleLayoutMode()` function that flips between the two.
    - Both `values/strings.xml` and `values-ro/strings.xml` contain `discover_layout_toggle_to_grid` and `discover_layout_toggle_to_list` keys.
    - `DiscoverViewModelTest.kt` is untouched and the existing test class still compiles (it does not reference `layoutMode`, so adding a new StateFlow + function is non-breaking).
  </done>
</task>

<task type="auto">
  <name>Task 2: DiscoverScreen — LazyVerticalGrid with conditional columnCount + top-right icon toggle</name>
  <files>
    app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt,
    app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt
  </files>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt (the current LazyColumn implementation — full file)
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt (existing Card uses fillMaxWidth — already adapts to grid cell width)
    - app/src/main/java/com/giftregistry/ui/discover/DiscoverShimmer.kt (existing shimmer also uses fillMaxWidth)
  </read_first>
  <action>
    Refactor `DiscoverScreen.kt` to use `LazyVerticalGrid` for both sections and add the icon-only toggle button.

    1) ADD imports:
       ```kotlin
       import androidx.compose.foundation.layout.Row
       import androidx.compose.foundation.lazy.grid.GridCells
       import androidx.compose.foundation.lazy.grid.GridItemSpan
       import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
       import androidx.compose.foundation.lazy.grid.items
       import androidx.compose.material.icons.outlined.GridView
       import androidx.compose.material.icons.outlined.ViewAgenda
       import androidx.compose.material3.IconButton
       ```
       REMOVE imports that are no longer used (`androidx.compose.foundation.lazy.LazyColumn`, `androidx.compose.foundation.lazy.items`) — keep `Arrangement`, `PaddingValues`, etc.

    2) Inside `DiscoverScreen` composable, ADD a collected state:
       ```kotlin
       val layoutMode by viewModel.layoutMode.collectAsStateWithLifecycle()
       ```
       Put it right next to the other `by viewModel.X.collectAsStateWithLifecycle()` lines.

    3) ADD the toggle row ABOVE the search bar and BELOW the existing 20.dp top spacer. The row holds only the IconButton, right-aligned, with 16.dp horizontal padding (matches the search bar's horizontal padding so the icon visually aligns with the right edge of the content):

       ```kotlin
       Row(
           modifier = Modifier
               .fillMaxWidth()
               .padding(horizontal = 8.dp),
           horizontalArrangement = Arrangement.End,
       ) {
           val isOneColumn = layoutMode == DiscoverLayoutMode.OneColumn
           IconButton(onClick = { viewModel.toggleLayoutMode() }) {
               Icon(
                   imageVector = if (isOneColumn) Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,
                   contentDescription = stringResource(
                       if (isOneColumn) R.string.discover_layout_toggle_to_grid
                       else R.string.discover_layout_toggle_to_list,
                   ),
                   tint = colors.ink,
               )
           }
       }
       ```

       Use 8.dp horizontal padding on the Row (not 16.dp) — Material `IconButton` has a built-in 48.dp touch target with the icon centered inside ~12.dp inset, so 8.dp Row padding lands the visible icon at roughly the same right-edge position as the search field's 16.dp inset.

    4) REPLACE the `LazyColumn { … }` block with `LazyVerticalGrid` using `GridCells.Fixed(layoutMode.columnCount)`. The search bar AND every section header AND every empty/error/divider state must span all columns; only product cards and shimmer cards use a single cell.

       Skeleton:
       ```kotlin
       LazyVerticalGrid(
           columns = GridCells.Fixed(layoutMode.columnCount),
           contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
           verticalArrangement = Arrangement.spacedBy(10.dp),
           horizontalArrangement = Arrangement.spacedBy(10.dp),
           modifier = Modifier.fillMaxWidth(),
       ) {
           // FROM THE WEB section — full-width header + (full-width state OR per-cell cards).
           if (search !is SearchState.Idle) {
               item(span = { GridItemSpan(maxLineSpan) }) {
                   SectionHeader(stringResource(R.string.discover_section_web))
               }
               when (val state = search) {
                   SearchState.Loading -> items(3, span = { GridItemSpan(1) }) { DiscoverShimmerCard() }
                   is SearchState.Loaded -> items(
                       items = state.products,
                       key = { it.id },
                       span = { GridItemSpan(1) },
                   ) { product ->
                       DiscoverProductCard(product = product, snackbarHostState = snackbarHostState)
                   }
                   SearchState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                       EmptyStateText(stringResource(R.string.discover_empty_search))
                   }
                   is SearchState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                       InlineErrorState(
                           message = stringResource(R.string.discover_error_search),
                           onRetry = { viewModel.retrySearch() },
                       )
                   }
                   SearchState.Idle -> Unit
               }
               item(span = { GridItemSpan(maxLineSpan) }) {
                   HorizontalDivider(
                       color = colors.line,
                       thickness = 1.dp,
                       modifier = Modifier.padding(vertical = 12.dp),
                   )
               }
           }

           // FROM THE COMMUNITY section — always visible.
           item(span = { GridItemSpan(maxLineSpan) }) {
               SectionHeader(stringResource(R.string.discover_section_community))
           }
           when (val state = popular) {
               PopularState.Loading -> items(3, span = { GridItemSpan(1) }) { DiscoverShimmerCard() }
               is PopularState.Loaded -> items(
                   items = state.products,
                   key = { it.id },
                   span = { GridItemSpan(1) },
               ) { product ->
                   DiscoverProductCard(product = product, snackbarHostState = snackbarHostState)
               }
               PopularState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                   EmptyStateText(stringResource(R.string.discover_empty_popular))
               }
               is PopularState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                   InlineErrorState(
                       message = stringResource(R.string.discover_error_load),
                       onRetry = { viewModel.loadPopular() },
                   )
               }
           }
       }
       ```

       Notes on the LazyVerticalGrid API:
       - `items(count: Int, span: ((LazyGridItemSpanScope) -> GridItemSpan)?, itemContent: ...)` overload is what's used for shimmer (count = 3).
       - `items(items: List<T>, key: ((T) -> Any)?, span: ...)` overload is what's used for products.
       - `GridItemSpan(maxLineSpan)` makes the item span ALL columns (so it works for both 1-col AND 2-col modes — `maxLineSpan` is the current column count). `GridItemSpan(1)` is the default for product cells.

    5) `DiscoverProductCard.kt` — VERIFY at narrow width:
       The card already uses `fillMaxWidth()` so it will shrink to the grid cell width. Existing constraints:
       - Title: `maxLines = 2, overflow = TextOverflow.Ellipsis` — OK at half-width (2 lines still readable).
       - Description: `maxLines = 2, overflow = TextOverflow.Ellipsis` — OK.
       - Price: 1 line, NumberFormat ron — OK (short strings).
       - RetailerName: `maxLines = 1, overflow = TextOverflow.Ellipsis` — OK.
       - Image: `aspectRatio(16f / 9f)` on `fillMaxWidth()` — scales naturally.

       NO CODE CHANGE EXPECTED for `DiscoverProductCard.kt`. Re-read the file after Task 2 compiles and visually scan: if the Compose preview in Task 3 shows text being clipped or the price line wrapping awkwardly in 2-col mode, ONLY THEN reduce title/description `maxLines` from 2 → 1 — but check first; the existing values almost certainly work. If you DO change it, leave a brief comment `// 260530-ncw: tightened maxLines for 2-col fit` so future readers know why.

    The screen must compile, the 1-col mode must visually match the pre-change Phase 17 look (same horizontal padding, same vertical spacing between cards, same divider, same section headers), and the toggle must visibly swap both the column count AND its icon.
  </action>
  <verify>
    <automated>grep -n "LazyVerticalGrid" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; grep -n "GridCells.Fixed(layoutMode.columnCount)" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; grep -n "GridItemSpan(maxLineSpan)" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; grep -n "Icons.Outlined.GridView\|Icons.Outlined.ViewAgenda" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; grep -n "toggleLayoutMode" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; ! grep -n "LazyColumn" app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --console=plain -q</automated>
  </verify>
  <done>
    - `DiscoverScreen.kt` no longer uses `LazyColumn`; it uses `LazyVerticalGrid` with `GridCells.Fixed(layoutMode.columnCount)`.
    - Search bar is OUTSIDE the grid (still a sibling `OutlinedTextField` in the parent `Column`, full-width).
    - Section headers, dividers, empty states, and error states all use `item(span = { GridItemSpan(maxLineSpan) })` to span all columns.
    - Product cards and shimmer cards use `GridItemSpan(1)`.
    - An `IconButton` placed in a right-aligned `Row` above the search bar shows `Icons.Outlined.GridView` when `layoutMode == OneColumn` and `Icons.Outlined.ViewAgenda` when `TwoColumns`; its `contentDescription` is the matching localized string.
    - `:app:compileDebugKotlin` succeeds.
  </done>
</task>

<task type="auto">
  <name>Task 3: StyleGuidePreview — render 1-col and 2-col Discover previews side-by-side</name>
  <files>app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt</files>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt lines 880-1050 (existing `DiscoverPreview` block)
  </read_first>
  <action>
    Keep the existing `@Preview` "Phase 17 — Discover (5 states)" unchanged (it documents the original 1-col layout — leave it as a regression reference). ADD a NEW `@Preview` block AFTER the existing `DiscoverPreview` function that renders the new 2-col grid layout using `LazyVerticalGrid` + `GridCells.Fixed(2)`.

    The new preview should mirror the existing 1-col preview's `discoverPreviewProducts` fixture (reuse the file-level `private val discoverPreviewProducts = listOf(...)` — do NOT redeclare it). The preview must show:
    - Section header "FROM THE COMMUNITY" spanning both columns
    - 4 product cards rendered as 2 rows of 2 (use `discoverPreviewProducts + discoverPreviewProducts` to get 4 cards from the existing 2-item fixture)
    - Section header "FROM THE WEB" spanning both columns
    - 2 product cards rendered as 1 row of 2
    - 2 shimmer cards in a row to verify shimmer also tiles correctly

    Required block:
    ```kotlin
    @Preview(
        name = "Phase 17 — Discover (2-col)",
        showBackground = true,
        backgroundColor = 0xFFF7F2E9,
        widthDp = 390,
        heightDp = 1200,
    )
    @Composable
    private fun DiscoverTwoColumnPreview() {
        GiftRegistryTheme {
            val colors = GiftMaisonTheme.colors
            val typography = GiftMaisonTheme.typography
            val snackbar = remember { SnackbarHostState() }
            val fourProducts = discoverPreviewProducts + discoverPreviewProducts
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.paper),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "FROM THE COMMUNITY",
                            style = typography.monoCaps,
                            color = colors.inkFaint,
                        )
                    }
                    items(items = fourProducts, key = { "${it.id}-${fourProducts.indexOf(it)}" }) { product ->
                        DiscoverProductCard(product = product, snackbarHostState = snackbar)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "FROM THE WEB",
                            style = typography.monoCaps,
                            color = colors.inkFaint,
                        )
                    }
                    items(items = discoverPreviewProducts, key = { it.id }) { product ->
                        DiscoverProductCard(product = product, snackbarHostState = snackbar)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "FROM THE WEB — shimmer",
                            style = typography.monoCaps,
                            color = colors.inkFaint,
                        )
                    }
                    items(2) { DiscoverShimmerCard() }
                }
            }
        }
    }
    ```

    The required imports for `GridCells`, `GridItemSpan`, `LazyVerticalGrid`, and `items` (grid overload) are ALREADY present in this file (lines 20-23). The required imports for `DiscoverProductCard` and `DiscoverShimmerCard` (lines 56-57) are also already present. You may need to add `import androidx.compose.foundation.lazy.grid.items` if only `itemsIndexed` is currently imported — verify by searching for `lazy.grid.items` in the file.
  </action>
  <verify>
    <automated>grep -n "Phase 17 — Discover (2-col)\|DiscoverTwoColumnPreview" app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt &amp;&amp; grep -n "GridCells.Fixed(2)" app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --console=plain -q</automated>
  </verify>
  <done>
    - A new `@Preview` named "Phase 17 — Discover (2-col)" is defined in `StyleGuidePreview.kt`.
    - The preview renders `LazyVerticalGrid(GridCells.Fixed(2))` with full-width section headers (via `GridItemSpan(maxLineSpan)`) and per-cell product/shimmer cards.
    - The existing 1-col `DiscoverPreview` is unchanged.
    - `:app:compileDebugKotlin` succeeds.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <what-built>
    Session-local layout toggle on the Discover screen: top-right icon button
    swaps the LazyVerticalGrid between `GridCells.Fixed(1)` and
    `GridCells.Fixed(2)`. The icon swaps between `Icons.Outlined.GridView`
    (shown in 1-col, action = "go to grid") and `Icons.Outlined.ViewAgenda`
    (shown in 2-col, action = "go to list"). Both FROM THE COMMUNITY and
    FROM THE WEB sections — including their shimmer skeletons — respect the
    column count. Section headers and the search bar stay full-width.
    State is held in `DiscoverViewModel.layoutMode` (StateFlow); default
    is `OneColumn`; not persisted across app restarts.
  </what-built>
  <how-to-verify>
    1) Run the app on an emulator: `./gradlew :app:installDebug` then launch.
       Sign in if needed and navigate to the Discover bottom-nav tab.
    2) Confirm DEFAULT state on first open is one card per row (matches the
       pre-change Phase 17 look). Top-right shows a 2x2 grid icon.
    3) Tap the icon. Confirm:
       - Cards reflow to 2 per row.
       - Both FROM THE COMMUNITY cards AND any visible state (loading / loaded)
         move to 2-col.
       - The icon changes to a "half-square over full-square" (ViewAgenda)
         shape.
    4) Submit a search query (IME Search). When the FROM THE WEB section
       appears, confirm its cards also lay out at 2-col while the section
       header "FROM THE WEB" still spans the full width.
    5) During the brief shimmer load (or by triggering a retry that re-enters
       Loading), confirm the shimmer placeholders also tile at 2-col.
    6) Tap the icon again — back to 1-col, icon back to 2x2 grid.
    7) Long-press / TalkBack: enable TalkBack and confirm the button announces
       a localized content description ("Switch to two-column grid" in en,
       "Comută la grilă cu două coloane" in ro after switching device locale).
       Also confirm the announcement changes after a tap.
    8) Force-close the app and relaunch — Discover should open in 1-col
       (default; not persisted). PASS criterion: default restored.
    9) Visual check: in 2-col mode, no DiscoverProductCard text is clipped in
       a visually jarring way (one ellipsis on long titles is fine). Image
       aspect ratio still 16:9, price + retailerName both still legible.
   10) Open the Compose preview file `StyleGuidePreview.kt` in Android Studio
       and confirm BOTH the original "Discover (5 states)" preview and the
       new "Discover (2-col)" preview render.
  </how-to-verify>
  <resume-signal>Type "approved" or describe issues (e.g. "text clipped in 2-col" or "icon misaligned with search bar")</resume-signal>
</task>

</tasks>

<verification>
- `./gradlew :app:compileDebugKotlin` succeeds.
- `./gradlew :app:lintDebug` produces no NEW Kotlin/Compose warnings on the modified files.
- `./gradlew :app:testDebugUnitTest --tests "*DiscoverViewModelTest*"` still passes (the existing test does not reference `layoutMode`, so adding a new StateFlow + function must not break it).
- Manual verification per the checkpoint above.
</verification>

<success_criteria>
- Toggle icon appears in the upper-right of Discover on first open.
- Default mode = 1-col (visually identical to the pre-change Phase 17 surface).
- Tapping the icon swaps both the column count AND the icon glyph.
- Both sections (FROM THE COMMUNITY and FROM THE WEB) AND their shimmer
  placeholders respect the chosen column count.
- Search bar and section headers remain full-width in both modes.
- TalkBack announces a localized content description in en AND in ro.
- App restart returns to 1-col default (no DataStore writes added).
- `compileDebugKotlin` is green; existing `DiscoverViewModelTest` still passes.
</success_criteria>

<output>
After completion, append a short note to `.planning/STATE.md` under the Phase 17
section: "Discover layout toggle (quick 260530-ncw) shipped — session-local,
2x2 / ViewAgenda icons, en+ro descriptions." No phase summary file required —
this is a quick.
</output>

## PLANNING COMPLETE

**Plan path:** `/Users/victorpop/ai-projects/gift-registry/.planning/quick/260530-ncw-add-a-layout-toggle-icon-button-to-the-d/260530-ncw-PLAN.md`

**Task count:** 3 auto tasks + 1 human-verify checkpoint (autonomous: false).

**Wave:** 1 (single plan, no inter-plan deps; tasks within run sequentially since they share `DiscoverScreen.kt` / preview file ownership).

**Key design choices baked in:**
- Single rendering path via `LazyVerticalGrid` with `GridCells.Fixed(columnCount)` (1 or 2) — no LazyColumn/LazyVerticalGrid swap; section headers + search bar use `GridItemSpan(maxLineSpan)` to span all columns.
- Material Icons Extended IS on the classpath (`app/build.gradle.kts:69`), so `Icons.Outlined.GridView` + `Icons.Outlined.ViewAgenda` are used directly — no custom vector drawables needed (constraint hint accommodated, but verified unnecessary).
- `DiscoverProductCard.kt` is NOT forked — it already uses `fillMaxWidth()` + bounded `maxLines`, so it adapts naturally to half-width cells. The task instructs the executor to verify visually in Task 2 and tighten `maxLines` only if needed.
- State lives in the existing `DiscoverViewModel` as a `StateFlow<DiscoverLayoutMode>` (sealed enum with `columnCount` property); `init {}` and existing methods untouched, so `DiscoverViewModelTest` stays green.
- Two new localized keys (`discover_layout_toggle_to_grid`, `discover_layout_toggle_to_list`) in both `values/` and `values-ro/`; semantics describe the action (TalkBack convention).