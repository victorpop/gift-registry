# Phase 9: Shared Chrome + Status UI — Research

**Researched:** 2026-04-21
**Domain:** Jetpack Compose UI — bottom navigation, ModalBottomSheet, infinite animations, image color treatment, Navigation3 integration
**Confidence:** HIGH (all critical claims verified against official docs or live codebase inspection)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Navigation restructure**
- "Lists" tab routes to first registry in `RegistryListViewModel.uiState` (ordered by `updatedAt desc`) as `isPrimary` resolver in Phase 9. Zero-registry case: show empty-state composable — no crash.
- "You" tab routes to existing `SettingsKey` — no new profile screen.
- Nav visible only on `HomeKey` and `RegistryDetailKey`. Full list of hidden keys: `AuthKey`, `OnboardingKey`, `CreateRegistryKey`, `EditRegistryKey`, `AddItemKey`, `EditItemKey`, `StoreListKey`, `StoreBrowserKey`, `SettingsKey`, `NotificationsKey`, `ReReserveDeepLink`.
- Full replacement of current `NavigationBar { NavigationBarItem(...) x4 }` block. No feature flag, no parallel nav.

**FAB Add-action bottom sheet**
- "Item from URL" / "Browse stores" / "Add manually" from Home open a registry picker (reuse `RegistryListViewModel`). Zero-registry case: rows disabled with inline hint `add_sheet_no_registry_hint`.
- "Add manually" routes to existing `AddItemKey(registryId, initialUrl = "")`.
- FAB visible exactly where nav is visible (Home + RegistryDetail).
- FAB slot shows mono-caps "ADD" caption (inkFaint) at bar baseline.
- On RegistryDetail the sheet pre-selects the currently-viewed registry.

**Status UI implementation**
- Component packages: `ui/common/chrome/` for nav/FAB/sheet; `ui/common/status/` for chips.
- `PulsingDot` uses `rememberInfiniteTransition(label)` + `animateFloat` on alpha (1f↔0.5f) and scale (1f↔0.85f), `tween(period/2, FastOutSlowInEasing)`, `RepeatMode.Reverse`. Period param: 1400 ms for Reserved chip, 1000 ms for Phase 11 "Fetching" field.
- Countdown: `LaunchedEffect(expiresAt)` + `delay(60_000L)` loop. No ViewModel ticker.
- `purchasedVisualTreatment()` modifier applies `alpha(0.55f)` row-level; image grayscale+tint and title strikethrough applied at element level by caller.

### Claude's Discretion
- Exact package naming within `ui/common/chrome/` and `ui/common/status/` is Claude's choice.
- Add-sheet animation timing follows Material 3 defaults (handoff does not specify beyond pulse keyframe).
- Nav icons: Material Symbols (Outlined variant) preferred for maintenance over inline ImageVector paths.

### Deferred Ideas (OUT OF SCOPE)
- `isPrimary` pinning UI — Phase 10 concern.
- Stores-tab / You-tab visual redesign — out of v1.1 scope.
- Occasion theming cascade — v1.2 (THEME-01..03).
- Scrim blur on older Android is a known fallback (implement + comment) but full blur library is not in scope.
- Registry picker polish (search, recents, empty-state CTA) — Phase 9 ships minimal picker only.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CHROME-01 | Bottom nav shows 5 slots (Home · Stores · +FAB · Lists · You) with stroked icons and mono caps labels; selected state uses accentSoft pill + accent stroke; hidden on screens 06, 09, 10 | AppNavigation.kt integration pattern documented; `showsBottomNav()` replacement strategy defined; existing nav keys catalogued |
| CHROME-02 | Centre FAB is a 54 px accent circle lifted 22 px above the bar with accent shadow and 4 px paper ring; tapping opens the Add action bottom sheet | FAB lift pattern (Box + offset) verified; `Modifier.fabShadow` already ships from Phase 8; `Modifier.border(4.dp, paper, CircleShape)` approach confirmed |
| CHROME-03 | Add action bottom sheet (22-radius top, drag handle, scrim over blurred home) offers 4 actions: New registry, Item from URL, Browse stores, Add manually | `ModalBottomSheet` M3 API verified — `scrimColor`, `shape`, `dragHandle`, `sheetState` params documented; blur fallback strategy documented |
| STAT-01 | Reserved chip uses filled accent pill with pulsing 4 px dot (1.4 s interval) and "Nm" countdown updated once per minute | `rememberInfiniteTransition` + `animateFloat` API confirmed; countdown `LaunchedEffect` pattern verified; `expiresAt: Long?` available on domain `Item` model |
| STAT-02 | Given chip uses secondSoft fill with "✓ given" label | Straightforward; all tokens available in `GiftMaisonColors` |
| STAT-03 | Open chip uses outlined pill with line border and inkFaint text | Straightforward; `Modifier.border(1.dp, colors.line, pill)` pattern confirmed |
| STAT-04 | Purchased item row renders at 55 % opacity with grayscale image, ink tint, centred ✓, and strikethrough title — remains visible to viewers per handoff trust pattern | `ColorMatrix.setToSaturation(0f)` + `ColorFilter.colorMatrix()` on `AsyncImage` confirmed; `TextDecoration.LineThrough` available; `Modifier.alpha()` pattern documented |
</phase_requirements>

---

## Summary

Phase 9 ships six Compose composables and two string resource files. The main technical challenges are (1) integrating the new bottom nav into the existing `AppNavigation.kt` `Scaffold(bottomBar=...)` block cleanly, (2) implementing the FAB lift geometry without fighting Material 3 NavigationBar layout, and (3) driving the Reserved chip's pulse animation and countdown from within the composable without a ViewModel.

All Phase 8 design primitives (`GiftMaisonColors`, `GiftMaisonTypography`, `GiftMaisonShapes`, `GiftMaisonSpacing`, `GiftMaisonShadows`) are confirmed present in the codebase. The `ItemStatus` domain enum has values `AVAILABLE`, `RESERVED`, `PURCHASED` — **not** `OPEN`/`GIVEN`. Phase 9 must introduce a mapping layer or Phase 9 uses `AVAILABLE` = Open, `RESERVED` = Reserved, `PURCHASED` = Given+Purchased combined at the display layer; the CONTEXT.md decided chips map to `ItemStatus.AVAILABLE→OpenChip`, `ItemStatus.RESERVED→ReservedChip`, `ItemStatus.PURCHASED→GivenChip`. The `item.expiresAt` field is `Long?` (epoch millis, not `Instant`) — conversion required at the chip boundary.

The existing nav is a 4-tab persistent `NavigationBar` (from quick tasks 260420-hua/260420-iro). Phase 9 fully replaces it. No `RegistryListViewModel` structural changes are required — the `uiState: StateFlow<RegistryListUiState>` already exposes the list the Lists-tab resolver needs.

**Primary recommendation:** Keep `AppNavigation.kt` as the single integration point — add `GiftMaisonBottomNav` and `AddActionSheet` there, replace `showsBottomNav()`, and inject `RegistryListViewModel` at the `AppNavigation` level using `hiltViewModel()`. Status chip composables live in `ui/common/status/` and are the only change to `RegistryDetailScreen.kt`.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose BOM | 2026.03.00 (project-pinned) | All Compose APIs | Already in project; pins all Compose library versions |
| Material 3 (`material3`) | via BOM | `ModalBottomSheet`, `NavigationBar`, `FloatingActionButton` | Project standard; already in use |
| Kotlin Coroutines | 1.9.x (project-pinned) | `LaunchedEffect` countdown loop | Already in project |
| Coil 3 (`coil-compose`) | 3.4.0 (project-pinned) | `AsyncImage` with `colorFilter` for grayscale | Already in project |
| Material Icons Extended | via BOM | `Icons.Default.Check`, `Icons.AutoMirrored.Filled.ChevronRight` | Already in project |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.ui:ui-graphics` | via BOM | `ColorMatrix`, `ColorFilter` | Purchased row grayscale treatment |
| `kotlinx.serialization` | via project | Nav key serialization | Already in `AppNavKeys.kt` |

### No new dependencies required
All libraries needed for Phase 9 are already declared in `app/build.gradle.kts`. No `build.gradle.kts` changes needed.

---

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/com/giftregistry/ui/
├── common/
│   ├── chrome/
│   │   ├── GiftMaisonBottomNav.kt     # 5-slot nav bar composable
│   │   ├── GiftMaisonFab.kt           # 54dp accent circle + paper ring
│   │   └── AddActionSheet.kt          # ModalBottomSheet with 4 rows
│   └── status/
│       ├── StatusChip.kt              # ReservedChip / GivenChip / OpenChip / StatusChip dispatcher
│       ├── PulsingDot.kt              # Reusable infinite-animation dot
│       └── PurchasedRowModifier.kt    # Modifier.purchasedVisualTreatment() extension
├── navigation/
│   └── AppNavigation.kt               # MODIFIED: replace nav block, inject RegistryListViewModel
└── registry/
    └── detail/
        └── RegistryDetailScreen.kt    # MODIFIED: replace ItemStatusChip with StatusChip import
```

### Pattern 1: Bottom Nav Integration in Scaffold

The current `AppNavigation.kt` `Scaffold(bottomBar = { if (showBottomBar) NavigationBar {...} })` pattern is the correct integration point. Replace the content of the `if (showBottomBar)` block with `GiftMaisonBottomNav(...)`.

The visibility predicate replaces the current `showsBottomNav()` extension:

```kotlin
// Source: codebase inspection + CONTEXT.md decision
private val NAV_VISIBLE_KEYS: Set<KClass<*>> = setOf(
    HomeKey::class,
    RegistryDetailKey::class,
)

private fun Any?.showsBottomNav(): Boolean = this?.let { it::class in NAV_VISIBLE_KEYS } ?: false
```

The `RegistryListViewModel` must be injected at `AppNavigation` scope (not inside the `entry<HomeKey>` lambda) so the Lists-tab resolver can access it at the nav bar level:

```kotlin
@Composable
fun AppNavigation(deepLinkRegistryId: String? = null) {
    // Existing ViewModels
    val registryListViewModel: RegistryListViewModel = hiltViewModel()
    val registryListState by registryListViewModel.uiState.collectAsStateWithLifecycle()
    // ... rest of function
}
```

The Lists-tab resolver (Phase 9 version):

```kotlin
// Source: CONTEXT.md decision + RegistryListViewModel.kt inspection
val primaryRegistryId: String? = when (val state = registryListState) {
    is RegistryListUiState.Success ->
        state.registries.maxByOrNull { it.updatedAt }?.id
    else -> null
}
```

**Critical:** `RegistryListViewModel` exposes `uiState: StateFlow<RegistryListUiState>` where `RegistryListUiState.Success(registries: List<Registry>)`. `Registry.updatedAt: Long` is present in the domain model. Use `maxByOrNull { it.updatedAt }` — not `firstOrNull()` since the list order from Firestore is not guaranteed to be `updatedAt desc` on the client.

### Pattern 2: FAB Lift Geometry

Material 3 `NavigationBar` is a fixed-height container. To lift the FAB 22 dp above the bar, use `Box` + `offset(y = (-22).dp)` inside the FAB slot column. This is Option (b) from the research prompt — it is the pattern that fits the handoff exactly without custom Layout.

```kotlin
// Source: handoff README.md "Centre FAB: top: -22 relative to its slot"
// ui/common/chrome/GiftMaisonBottomNav.kt
Box(
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    contentAlignment = Alignment.Center
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GiftMaisonFab(
            onClick = onFab,
            modifier = Modifier.offset(y = (-22).dp)  // Lift: hardcoded per handoff
        )
        Text(
            text = stringResource(R.string.nav_fab_add),
            style = GiftMaisonTheme.typography.monoCaps,
            color = GiftMaisonTheme.colors.inkFaint,
        )
    }
}
```

The `GiftMaisonFab` itself:

```kotlin
// Source: UI-SPEC.md GiftMaisonFab anatomy + GiftMaisonShadows.kt
Box(
    modifier = modifier
        .size(54.dp)
        .fabShadow(tint = GiftMaisonTheme.colors.accent)
        .border(4.dp, GiftMaisonTheme.colors.paper, CircleShape)
        .background(GiftMaisonTheme.colors.accent, CircleShape)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        tint = GiftMaisonTheme.colors.accentInk,
        modifier = Modifier.size(24.dp)
    )
}
```

Note: `border` must be applied BEFORE `background` in the modifier chain for the paper ring to render around (not inside) the accent circle. The shadow renders behind the border.

### Pattern 3: ModalBottomSheet for Add-Action Sheet

Material 3 `ModalBottomSheet` signature (confirmed via web search + Material 3 docs):

```kotlin
// Source: Material 3 documentation, @ExperimentalMaterial3Api
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),  // shapes.radius22 top only
    containerColor = GiftMaisonTheme.colors.paper,
    scrimColor = GiftMaisonTheme.colors.ink.copy(alpha = 0.55f),
    dragHandle = {
        Box(
            modifier = Modifier
                .padding(top = GiftMaisonTheme.spacing.edgeWide)
                .size(width = 36.dp, height = 4.dp)
                .background(GiftMaisonTheme.colors.line, CircleShape)
        )
    },
    modifier = Modifier.bottomSheetShadow(),
) {
    // Sheet content
}
```

**Blur fallback for content behind sheet:**

```kotlin
// Source: Android docs — Modifier.blur requires API 31+
// Source: CONTEXT.md deferred items
val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    Modifier.blur(radius = 1.dp)
} else {
    Modifier // No blur on API < 31 — plain scrim only
}
```

The blur is applied to the composable that serves as the background content, not to the sheet itself. Since `AppNavigation.kt` wraps `NavDisplay` inside `Scaffold`, the blur must be applied as a `Modifier` on the `Box` or content area when `showAddSheet` is true. Practically, apply it via a wrapper around `NavDisplay`.

### Pattern 4: PulsingDot with rememberInfiniteTransition

```kotlin
// Source: Android Developers official animation docs + handoff CSS pulse keyframe
@Composable
fun PulsingDot(
    color: Color,
    size: Dp = 4.dp,
    period: Duration = 1_400.milliseconds,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (period.inWholeMilliseconds / 2).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingDotAlpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (period.inWholeMilliseconds / 2).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingDotScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(color, CircleShape)
    )
}
```

`infiniteRepeatable` wraps the `tween` spec. The period is split in half because `RepeatMode.Reverse` covers the full cycle in two halves (forward + reverse = 1 period). `kotlin.time.Duration` is used; import `kotlin.time.Duration.Companion.milliseconds`.

### Pattern 5: Countdown LaunchedEffect

```kotlin
// Source: CONTEXT.md decision + handoff "updates once per minute"
@Composable
fun ReservedChip(expiresAt: Long?, modifier: Modifier = Modifier) {
    var minutesLeft by remember(expiresAt) {
        mutableIntStateOf(computeMinutesLeft(expiresAt))
    }

    LaunchedEffect(expiresAt) {
        while (true) {
            delay(60_000L)
            minutesLeft = computeMinutesLeft(expiresAt)
        }
    }
    // render chip with minutesLeft
}

private fun computeMinutesLeft(expiresAt: Long?): Int {
    if (expiresAt == null) return 0
    return max(0, ((expiresAt - System.currentTimeMillis()) / 60_000L).toInt())
}
```

`expiresAt` on the `Item` domain model is `Long?` (epoch millis). The UI-SPEC uses `Instant` but the domain model uses `Long`. The chip must convert: `expiresAt: Long?` parameter, not `Instant?`. The `LaunchedEffect` key is `expiresAt` — if the item transitions back to OPEN (expiresAt becomes null), the effect is cancelled automatically.

**`rememberSaveable` not needed for countdown state.** Config changes will restart the ticker but the minutes calculation is computed fresh from `expiresAt` on restart — no meaningful state loss. `remember(expiresAt)` is sufficient.

### Pattern 6: Purchased Row Treatment

`purchasedVisualTreatment()` is a **row-level alpha Modifier only**. Image and title treatment are applied at element level by the caller:

```kotlin
// Source: UI-SPEC.md PurchasedRowModifier + Compose graphics API
fun Modifier.purchasedVisualTreatment(): Modifier = this.alpha(0.55f)

// At the call site (RegistryDetailScreen after Phase 9):
val rowModifier = if (item.status == ItemStatus.PURCHASED) {
    Modifier.purchasedVisualTreatment()
} else Modifier

// On the AsyncImage:
val imageColorFilter = if (item.status == ItemStatus.PURCHASED) {
    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
} else null

AsyncImage(
    model = item.imageUrl,
    colorFilter = imageColorFilter,
    // ...
)
// Ink tint overlay and checkmark are a Box overlay on the image:
if (item.status == ItemStatus.PURCHASED) {
    Box(
        modifier = Modifier.matchParentSize().background(GiftMaisonTheme.colors.ink.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Check, tint = GiftMaisonTheme.colors.paper, modifier = Modifier.size(20.dp))
    }
}

// On the title Text:
val titleDecoration = if (item.status == ItemStatus.PURCHASED) TextDecoration.LineThrough else null
Text(text = item.title, textDecoration = titleDecoration)
```

### Pattern 7: ItemStatus Enum — Domain vs Display Mapping

**Critical finding:** The domain `ItemStatus` enum has values `AVAILABLE`, `RESERVED`, `PURCHASED` — not `OPEN`/`GIVEN`. The handoff and UI-SPEC use `Open`/`Reserved`/`Given`/`Purchased`. The mapping for Phase 9:

| Domain `ItemStatus` | Chip displayed |
|---------------------|----------------|
| `AVAILABLE` | `OpenChip` |
| `RESERVED` | `ReservedChip` |
| `PURCHASED` | `GivenChip` (handoff says "given" = purchased confirmed by giver) |

The `StatusChip` dispatcher:

```kotlin
// Source: domain model ItemStatus.kt + UI-SPEC StatusChip dispatcher
@Composable
fun StatusChip(
    status: ItemStatus,
    expiresAt: Long?,
    modifier: Modifier = Modifier,
) = when (status) {
    ItemStatus.AVAILABLE  -> OpenChip(modifier)
    ItemStatus.RESERVED   -> ReservedChip(expiresAt, modifier)
    ItemStatus.PURCHASED  -> GivenChip(modifier)
}
```

No domain model changes are required. The `Item.expiresAt: Long?` field already exists and was added in Phase 4.

### Pattern 8: RegistryListViewModel Injection for Lists-Tab Routing

`RegistryListViewModel` is a `@HiltViewModel` already used inside `entry<HomeKey>`. To reuse it at `AppNavigation` scope for Lists-tab routing, call `hiltViewModel<RegistryListViewModel>()` directly inside `AppNavigation`. Navigation3 uses Activity-scoped `ViewModelStoreOwner` for `hiltViewModel()` outside entry lambdas — **both calls share the same ViewModel instance** since the key is the class name by default. This is correct behaviour: no circular dependency, no duplicate Firebase listeners.

```kotlin
// Source: Phase 2 decision log — hiltViewModel() uses Activity ViewModelStoreOwner in Nav3
@Composable
fun AppNavigation(...) {
    val registryListViewModel: RegistryListViewModel = hiltViewModel()
    // This shares the same VM instance as the one inside entry<HomeKey>
}
```

### Anti-Patterns to Avoid

- **Adding `ModalBottomSheet` inside an `entry<HomeKey>` lambda.** The sheet must be hoisted above `NavDisplay` so the scrim covers the nav bar. Keep `showAddSheet` state and `AddActionSheet` in `AppNavigation`, outside the entry provider.
- **Using `NavigationBar { FloatingActionButton }` (BottomAppBar pattern).** The Material 3 `BottomAppBar` with FAB is designed for a different layout — the FAB docks into the bar, not above it. Use a plain `Row` / `Box` layout inside the `Scaffold.bottomBar` slot instead.
- **Applying `Modifier.blur()` without the API 31 guard.** On API < 31 (minSdk=23 for this project), the call is silently ignored per official docs, but wrapping in the version check makes intent explicit and documents the fallback.
- **Hardcoding string values** — all user-visible strings must go into `strings.xml` + `values-ro/strings.xml`. The existing nav strings (`nav_home`, `nav_preferences`) must be kept for backward compat but Phase 9 introduces the new keys that replace them in the new composable.
- **Re-declaring Phase 8 tokens.** `GiftMaisonColors`, `GiftMaisonTypography`, `GiftMaisonShapes`, `GiftMaisonSpacing` are accessed via `GiftMaisonTheme.*` composition locals. Never hardcode hex values or dp values that have named tokens.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Bottom sheet modal with scrim | Custom `Popup` + `BackHandler` + manual z-ordering | `ModalBottomSheet` (M3) | Handles accessibility, back gesture, drag-to-dismiss, scrim, keyboard insets |
| Infinite animation | Manual `coroutineScope.launch` with `animate*AsState` loops | `rememberInfiniteTransition` + `animateFloat` | Purpose-built for infinite loops; cancels on composition exit automatically |
| Image grayscale | Custom shader or Bitmap manipulation | `ColorMatrix.setToSaturation(0f)` + `ColorFilter.colorMatrix()` on `AsyncImage` | GPU-accelerated, no Bitmap allocation, single line |
| Nav key set membership | `is` checks across all keys | `Set<KClass<*>>` lookup | O(1), type-safe, no risk of missing a new key as routes are added |

---

## Common Pitfalls

### Pitfall 1: `shapes.pill` is `CircleShape` — not `RoundedCornerShape(999.dp)`
**What goes wrong:** Using `RoundedCornerShape(999.dp)` appears visually identical but fails on very tall containers where 999 dp < half the height, causing non-circular corners.
**How to avoid:** Use `GiftMaisonShapes.pill` which is `CircleShape` — adapts to any height. Already defined in Phase 8 `GiftMaisonShapes.kt`.

### Pitfall 2: `ModalBottomSheet` shape requires explicit top-corner rounding only
**What goes wrong:** `shape = shapes.radius22` applies 22 dp to all four corners, giving rounded bottom corners that clip off-screen weirdly.
**How to avoid:** Use `RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)` directly — not `shapes.radius22`. This is an exception to the "use shape tokens" rule, justified by the asymmetric geometry.

### Pitfall 3: `ModalBottomSheet` requires `@ExperimentalMaterial3Api`
**What goes wrong:** Compile error without the `@OptIn(ExperimentalMaterial3Api::class)` annotation on the containing composable or the file.
**How to avoid:** `AddActionSheet.kt` and any composable file hosting a `ModalBottomSheet` needs `@OptIn(ExperimentalMaterial3Api::class)`. The existing `AppNavigation.kt` already has `@OptIn(ExperimentalMaterial3Api::class)` on line 62 — adding `AddActionSheet` to `AppNavigation.kt` state management does not require a new annotation.

### Pitfall 4: `Modifier.border` render order vs `Modifier.background`
**What goes wrong:** Applying `border` after `background` renders the border inside the background, making the paper ring invisible against the accent fill.
**How to avoid:** Chain `shadow → border → background` in that order. The border clips to the shape boundary and renders between the shadow and fill.

### Pitfall 5: `delay(60_000L)` countdown starts immediately vs. after first minute
**What goes wrong:** If the chip renders at second 0 of a minute boundary, the first delay fires at second 60, causing the displayed minutes to lag by up to 59 seconds at first render.
**How to avoid:** Compute `minutesLeft` eagerly in `remember(expiresAt)` before the loop starts. The `LaunchedEffect` tick only updates the value; initial value is computed synchronously. This is already accounted for in the pattern above.

### Pitfall 6: ItemStatus enum mismatch — domain vs display
**What goes wrong:** Writing `when (status) { ItemStatus.GIVEN -> ... }` — `GIVEN` does not exist in the enum; it is `PURCHASED`. Compilation fails.
**How to avoid:** The domain enum is `AVAILABLE / RESERVED / PURCHASED`. Map in `StatusChip` dispatcher as shown in Pattern 7.

### Pitfall 7: FAB touch target inflation pushes the nav bar down
**What goes wrong:** If the FAB `Box` is placed inside a `Row` with `fillMaxHeight()` and the offset causes its click area to extend below the nav bar bottom edge, it may conflict with system gesture area.
**How to avoid:** Apply `WindowInsets.navigationBars` padding to the `Scaffold.bottomBar` container so the nav bar itself accounts for the gesture area. The FAB offset pushes the FAB visually above the bar, not below.

### Pitfall 8: `hiltViewModel()` outside entry lambda — Activity vs Entry scope
**What goes wrong:** Calling `hiltViewModel<RegistryListViewModel>()` inside an `entry<HomeKey>` lambda AND again in `AppNavigation` scope might create two ViewModel instances if Navigation3 uses per-entry `ViewModelStoreOwner`.
**Why it's safe here:** Per Phase 2 decision log: "hiltViewModel() uses Activity ViewModelStoreOwner" in Navigation3 1.0.1. Both call sites resolve to the same Activity-scoped ViewModel store, so the same instance is returned.

---

## Code Examples

### Full PulsingDot composable
```kotlin
// Source: handoff README.md CSS pulse keyframe + official InfiniteTransition docs
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PulsingDot(
    color: Color,
    size: Dp = 4.dp,
    period: Duration = 1_400.milliseconds,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
    val halfPeriodMs = (period.inWholeMilliseconds / 2).toInt()
    val animSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = halfPeriodMs, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.5f, animationSpec = animSpec, label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.85f, animationSpec = animSpec, label = "scale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .background(color, CircleShape)
    )
}
```

### StatusChip dispatcher with domain ItemStatus
```kotlin
// Source: domain model inspection + UI-SPEC StatusChip
@Composable
fun StatusChip(
    status: ItemStatus,
    expiresAt: Long?,   // epoch millis, nullable (Item.expiresAt)
    modifier: Modifier = Modifier,
) = when (status) {
    ItemStatus.AVAILABLE -> OpenChip(modifier)
    ItemStatus.RESERVED  -> ReservedChip(expiresAt, modifier)
    ItemStatus.PURCHASED -> GivenChip(modifier)
}
```

### Nav visibility predicate (replaces showsBottomNav())
```kotlin
// Source: AppNavKeys.kt inspection + CONTEXT.md nav visibility rule
private val NAV_VISIBLE_KEYS: Set<KClass<*>> = setOf(
    HomeKey::class,
    RegistryDetailKey::class,
)
private fun Any?.showsBottomNav(): Boolean =
    this?.let { it::class in NAV_VISIBLE_KEYS } ?: false
```

### Lists-tab isPrimary resolver (Phase 9 version)
```kotlin
// Source: RegistryListViewModel.kt + CONTEXT.md "first registry by updatedAt desc"
val primaryRegistryId: String? = when (val s = registryListState) {
    is RegistryListUiState.Success -> s.registries.maxByOrNull { it.updatedAt }?.id
    else -> null
}
```

---

## State of the Art

| Old Approach (in codebase) | New Approach (Phase 9) | When Changed | Impact |
|----------------------------|------------------------|--------------|--------|
| 4-slot persistent `NavigationBar` (quick task 260420-hua/iro) | 5-slot `GiftMaisonBottomNav` visible only on Home + RegistryDetail | Phase 9 | Full replacement; old `showsBottomNav()` deleted |
| Inline `ItemStatusChip` in `RegistryDetailScreen.kt` (Material 3 `SuggestionChip`) | Shared `StatusChip` composable from `ui/common/status/` | Phase 9 | RegistryDetailScreen imports from common; inline code deleted |
| Direct `CreateRegistryKey` nav from FAB on home | `AddActionSheet` with 4 actions (FAB is only entry point) | Phase 9 | Creates registry via sheet; old direct-create FAB behaviour removed |

---

## Environment Availability

Step 2.6: SKIPPED — Phase 9 is purely Android UI code/composables changes with no external tool dependencies beyond the project's existing build toolchain.

---

## Validation Architecture

nyquist_validation is enabled (config.json `workflow.nyquist_validation: true`).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 + MockK + Turbine (unit tests); Compose UI test (`createComposeRule`) for instrumented |
| Config file | none declared — JUnit 4 is detected automatically by Android Gradle |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |
| Compose UI test run | `./gradlew :app:connectedDebugAndroidTest` (requires device/emulator) |

**Note:** No `androidTest` directory exists in the codebase currently. All existing tests are in `app/src/test/` (JUnit 4 unit tests). Compose UI tests (`createComposeRule`) require `app/src/androidTest/` and `androidx.compose.ui:ui-test-junit4` + `ui-test-manifest` dependencies not yet in `build.gradle.kts`.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHROME-01 | `showsBottomNav()` returns true only for `HomeKey`/`RegistryDetailKey`; false for all 10 hidden keys | unit | `./gradlew :app:testDebugUnitTest --tests "*.BottomNavVisibilityTest"` | ❌ Wave 0 |
| CHROME-01 | `GiftMaisonBottomNav` renders 5 slots; selected slot highlights correctly | Compose UI test | `./gradlew :app:connectedDebugAndroidTest --tests "*.GiftMaisonBottomNavTest"` | ❌ Wave 0 |
| CHROME-02 | `GiftMaisonFab` offset is 22 dp above bar baseline; border applies before background | Compose UI test (layout assertion) | `./gradlew :app:connectedDebugAndroidTest --tests "*.GiftMaisonFabTest"` | ❌ Wave 0 |
| CHROME-03 | `AddActionSheet` renders 4 rows in correct order; scrim color is `ink.copy(0.55f)` | Compose UI test | `./gradlew :app:connectedDebugAndroidTest --tests "*.AddActionSheetTest"` | ❌ Wave 0 |
| STAT-01 | `ReservedChip` shows countdown; `computeMinutesLeft` returns 0 when expired, positive when active | unit | `./gradlew :app:testDebugUnitTest --tests "*.ReservedChipTest"` | ❌ Wave 0 |
| STAT-01 | `PulsingDot` animates at 1400 ms period (verify `animationSpec` parameters) | unit | `./gradlew :app:testDebugUnitTest --tests "*.PulsingDotTest"` | ❌ Wave 0 |
| STAT-02 | `GivenChip` uses `secondSoft` background, `second` text color | Compose UI test | `./gradlew :app:connectedDebugAndroidTest --tests "*.StatusChipTest"` | ❌ Wave 0 |
| STAT-03 | `OpenChip` uses transparent background with `line` border | Compose UI test | `./gradlew :app:connectedDebugAndroidTest --tests "*.StatusChipTest"` | ❌ Wave 0 |
| STAT-04 | `purchasedVisualTreatment()` applies `alpha(0.55f)` to row modifier | unit (Modifier inspection) | `./gradlew :app:testDebugUnitTest --tests "*.PurchasedRowModifierTest"` | ❌ Wave 0 |
| STAT-04 | `StatusChip(PURCHASED, ...)` dispatches to `GivenChip` | unit | included in `*.StatusChipTest` above | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.*"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full unit suite green + visual verification of StyleGuidePreview sections in Android Studio before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — covers CHROME-01 visibility predicate (pure Kotlin, no Android)
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/ReservedChipTest.kt` — covers STAT-01 countdown logic (`computeMinutesLeft`)
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt` — covers STAT-01 animation spec parameters
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/StatusChipDispatcherTest.kt` — covers STAT-01/02/03 dispatcher routing
- [ ] `app/src/test/java/com/giftregistry/ui/common/status/PurchasedRowModifierTest.kt` — covers STAT-04
- [ ] Compose UI tests for CHROME-02 / CHROME-03 / STAT-02 / STAT-03: require `androidTest` directory + `ui-test-junit4` dependency addition to `build.gradle.kts`:
  ```kotlin
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  ```
  These tests are Wave 0 stubs (RED) that become GREEN in Wave 2.

**Pragmatic note:** Given project history (Phase 8 used unit tests only in `app/src/test/`), Wave 0 should prioritise the pure-Kotlin unit tests. The Compose instrumented tests are desirable but require device/emulator. The planner should decide whether to defer instrumented tests to a later wave or include them as RED stubs in Wave 0.

---

## Open Questions

1. **`ItemStatusChip` replacement scope in `RegistryDetailScreen.kt`**
   - What we know: Lines ~601–625 contain `ItemStatusChip` private composable using `SuggestionChip` with Material 3 defaults. Lines ~540–552 render a separate `reservation_reserved_label` + `ReservationCountdown` block.
   - What's unclear: The Phase 9 plan must delete both the `ItemStatusChip` composable AND the inline reserved label/countdown block, replacing both with a single `StatusChip(item.status, item.expiresAt)` call.
   - Recommendation: The plan should explicitly list both deletions so the executor does not leave a ghost `ReservationCountdown` composable.

2. **`nav_home` / `nav_preferences` / `stores_browse_label` string keys — keep or remove?**
   - What we know: `nav_home`, `nav_add_list`, `nav_preferences` exist in `strings.xml` / `values-ro/strings.xml` (used by the old 4-tab nav). Phase 9 adds new keys like `nav_home_tab`, `nav_you_tab`.
   - What's unclear: Whether to delete old keys (`nav_home`, `nav_add_list`) or leave them as dead strings.
   - Recommendation: Keep old keys — removing them would require verifying no other code references them, creating risk. The planner can add a cleanup task at end of phase.

3. **`RegistryDetailKey` selected-slot mapping for Lists tab**
   - What we know: UI-SPEC says `RegistryDetailKey → Lists slot selected`. The Lists tab navigates to `RegistryDetailKey(isPrimaryId)`.
   - What's unclear: When the user is already ON RegistryDetail but for a non-primary registry (e.g., navigated there from a notification), the Lists tab should still show as selected, but tapping it again should no-op (user is already on a RegistryDetailKey destination).
   - Recommendation: Selected = `currentKey is RegistryDetailKey` (any RegistryDetailKey, not just the primary one). On-tap behaviour: `if (currentKey is RegistryDetailKey) { /* no-op */ } else { backStack.add(RegistryDetailKey(primaryId)) }`.

---

## Sources

### Primary (HIGH confidence)
- Codebase direct inspection — `AppNavigation.kt` (373 lines), `AppNavKeys.kt`, `RegistryDetailScreen.kt` (626 lines), `RegistryListViewModel.kt`, `Item.kt`, `ItemStatus.kt`, `Registry.kt`, `GiftMaisonShapes.kt`, `GiftMaisonSpacing.kt`, `GiftMaisonColors.kt`, `GiftMaisonShadows.kt`, `Theme.kt`, `build.gradle.kts`, `libs.versions.toml`
- `design_handoff/design_handoff_android_owner_flow/README.md` — pixel specs, animation keyframe, shadow specs
- `09-CONTEXT.md` — locked decisions
- `09-UI-SPEC.md` — component signatures, string keys, spacing tokens

### Secondary (MEDIUM confidence)
- [Android Developers — Value-based animations](https://developer.android.com/develop/ui/compose/animation/value-based) — `rememberInfiniteTransition`, `animateFloat`, `RepeatMode.Reverse`, `tween` API
- [Android Developers — Customize an image](https://developer.android.com/develop/ui/compose/graphics/images/customize) — `ColorMatrix.setToSaturation`, `ColorFilter.colorMatrix`
- [Material 3 ModalBottomSheet API](https://composables.com/material3/modalbottomsheet) — `scrimColor`, `sheetState`, `dragHandle`, `shape` parameters; `@ExperimentalMaterial3Api` annotation
- Web search (verified): `Modifier.blur()` requires `Build.VERSION_CODES.S` (API 31+); silently ignored on older versions

### Tertiary (LOW confidence)
- None — all critical claims sourced from HIGH or MEDIUM sources.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in project; no new dependencies needed
- Architecture: HIGH — existing code inspected directly; patterns derived from live codebase
- API signatures: HIGH — verified against official Android docs or live source
- Pitfalls: HIGH — derived from code inspection + documented Phase 2/4/8 decisions
- Domain model mapping: HIGH — `ItemStatus.kt` read directly; `AVAILABLE/RESERVED/PURCHASED` confirmed

**Research date:** 2026-04-21
**Valid until:** 2026-05-21 (stable — Compose BOM and Material 3 APIs are stable for these features)
