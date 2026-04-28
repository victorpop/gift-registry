---
phase: quick-260421-moi
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
autonomous: true
requirements:
  - QUICK-260421-MOI

must_haves:
  truths:
    - "On a 360 dp wide phone, all 4 bottom-nav labels ('HOME', 'STORES', 'LISTS', 'YOU') and the FAB caption 'ADD' render on a single line with no horizontal clipping and no ellipsis — each slot occupies an equal 1/5 share of the bar width"
    - "The centre FAB still lifts 22 dp above the bar baseline, still shows the accent-filled circle + paper ring + accent shadow (GiftMaisonFab contract untouched), and its slot is the same width as the 4 nav item slots (no hardcoded 72 dp)"
    - "RegistryListScreen no longer renders its own FloatingActionButton — the screen's Scaffold has no floatingActionButton argument; the 'add' entry point for the owner flow is solely the centre-nav FAB + AddActionSheet wired in AppNavigation (Phase 9 Plan 04)"
    - "Creating a new registry still works end-to-end: Home → tap centre FAB → AddActionSheet → 'New registry' row → CreateRegistryScreen → save → RegistryDetailScreen for the new registry"
    - "No unused imports remain in RegistryListScreen.kt (no FloatingActionButton, no Icons, no filled.Add)"
    - "Full unit suite stays GREEN and :app:assembleDebug still produces a debug APK"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt"
      provides: "5-slot bottom nav with equal weighted slots, maxLines=1/softWrap=false on labels, no hardcoded FAB slot width"
      contains: "Modifier.weight(1f)"
    - path: "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt"
      provides: "Scaffold without floatingActionButton; no Icons / FloatingActionButton / filled.Add imports; no onNavigateToCreate parameter"
      contains: "fun RegistryListScreen"
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      provides: "entry<HomeKey> call site updated — no onNavigateToCreate argument passed (CreateRegistryKey target preserved for AddActionSheet path)"
      contains: "RegistryListScreen("
  key_links:
    - from: "GiftMaisonBottomNav Row children"
      to: "Equal 1/5 width allocation across HOME / STORES / FAB / LISTS / YOU slots"
      via: "Modifier.weight(1f) applied to each NavItemSlot + FabSlot; Arrangement.SpaceAround removed"
      pattern: "Modifier\\.weight\\(1f\\)"
    - from: "Home screen 'add' entry point"
      to: "CreateRegistryKey via AddActionSheet"
      via: "onFab = { showAddSheet = true } → AddActionSheet.onNewRegistry → backStack.add(CreateRegistryKey) (already wired in AppNavigation from 09-04)"
      pattern: "backStack\\.add\\(CreateRegistryKey\\)"
---

<objective>
Fix two post-Phase-9 bottom-nav regressions:

1. **Label truncation** — `GiftMaisonBottomNav.kt` arranges 5 content-sized Columns with `Arrangement.SpaceAround` + a hardcoded `.width(72.dp)` FAB slot. On 360 dp phones, 'STORES' and 'LISTS' (monoCaps 9.5 sp + 1.5 sp letter-spacing) exceed their natural slot width and clip. Fix by giving every slot `Modifier.weight(1f)` so the 5 slots share the bar width evenly, dropping `Arrangement.SpaceAround` (irrelevant with weights), removing the FAB slot's hardcoded 72 dp, and adding `maxLines = 1` + `softWrap = false` on the label Texts as belt-and-braces.

2. **Duplicate floating FAB** — `RegistryListScreen.kt` still renders its legacy `Scaffold(floatingActionButton = { FloatingActionButton(onNavigateToCreate) { Icon(Add) } })` from pre-Phase-9. Phase 9's shared centre-nav FAB + AddActionSheet already owns the "add" entry point, so the legacy FAB is a duplicate overlay. Delete the FAB block, the now-unused imports (`FloatingActionButton`, `Icons`, `filled.Add`), and the now-dead `onNavigateToCreate` parameter + its call site in `AppNavigation.kt`.

Purpose: Restore the handoff-spec 5-slot-equal-width nav bar and eliminate a visually confusing second "+" button on the Home screen that duplicates the centre-FAB entry point.

Output: 3 files modified — `GiftMaisonBottomNav.kt` (layout), `RegistryListScreen.kt` (FAB removal + param drop), `AppNavigation.kt` (call-site update). No file deletions. No tests broken (verified: no test file exists for RegistryListScreen). Pixel contract on GiftMaisonFab, typography, colours, shapes, and the 22 dp FAB lift is untouched.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@.planning/phases/09-shared-chrome-status-ui/09-UI-SPEC.md
@.planning/phases/09-shared-chrome-status-ui/09-04-PLAN.md

<interfaces>
<!-- Current (broken) bottom nav Row — lines 70-106 of GiftMaisonBottomNav.kt -->

From app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt (current):
```kotlin
Row(
    modifier = modifier
        .fillMaxWidth()
        .background(colors.paper)
        .border(width = 1.dp, color = colors.line)
        .navigationBarsPadding()
        .padding(top = 4.dp, bottom = 6.dp)
        .height(56.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceAround,   // <-- REMOVE (irrelevant with weights)
) {
    NavItemSlot(icon = Icons.Outlined.Home,       labelRes = R.string.nav_home_tab,   ...)
    NavItemSlot(icon = Icons.Outlined.Storefront, labelRes = R.string.nav_stores_tab, ...)
    FabSlot(onClick = onFab)
    NavItemSlot(icon = Icons.AutoMirrored.Outlined.List, labelRes = R.string.nav_lists_tab, ...)
    NavItemSlot(icon = Icons.Outlined.Person,     labelRes = R.string.nav_you_tab,    ...)
}

// NavItemSlot (lines 114-158) — takes no Modifier parameter today; must be extended:
@Composable
private fun NavItemSlot(icon: ImageVector, labelRes: Int, isSelected: Boolean, onClick: () -> Unit) { ... }

// FabSlot (lines 161-184) — has hardcoded .width(72.dp); must accept Modifier + drop width:
@Composable
private fun FabSlot(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)          // <-- REMOVE
            .fillMaxHeight(),
        ...
    ) {
        GiftMaisonFab(onClick = onClick, modifier = Modifier.offset(y = (-22).dp))
        Text(text = stringResource(R.string.nav_fab_add), style = typography.monoCaps, color = colors.inkFaint)
    }
}
```

<!-- Current RegistryListScreen legacy FAB — lines 84-101 -->

From app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt (current):
```kotlin
@Composable
fun RegistryListScreen(
    onNavigateToCreate: () -> Unit,              // <-- DELETE parameter
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    viewModel: RegistryListViewModel = hiltViewModel()
) {
    ...
    Scaffold(
        topBar = { TopAppBar(...) },
        floatingActionButton = {                  // <-- DELETE whole argument
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.registry_create_button))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues -> ... }
}

// Imports to DELETE after FAB removal (grep-confirmed: Icons/filled.Add used ONLY at the FAB):
//   line 16:  import androidx.compose.material.icons.Icons
//   line 17:  import androidx.compose.material.icons.filled.Add
//   line 30:  import androidx.compose.material3.FloatingActionButton
```

<!-- AppNavigation entry<HomeKey> call site — line 192-199 -->

From app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (current):
```kotlin
entry<HomeKey> {
    RegistryListScreen(
        onNavigateToCreate = { backStack.add(CreateRegistryKey) },   // <-- DELETE line
        onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
        onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
        onNavigateToNotifications = { backStack.add(NotificationsKey) },
    )
}
```

Note: `CreateRegistryKey` itself must stay — it is the target of the AddActionSheet's 'New registry' row (line 342 of AppNavigation.kt: `onNewRegistry = { showAddSheet = false; backStack.add(CreateRegistryKey) }`). Also `entry<CreateRegistryKey>` at line 201 must stay untouched. Only the dead `onNavigateToCreate` argument is removed.

<!-- Grep-verified: no other callers of RegistryListScreen's onNavigateToCreate -->

```bash
$ grep -rn "onNavigateToCreate\|RegistryListScreen" app/src/main/java --include="*.kt"
app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt:49:import com.giftregistry.ui.registry.list.RegistryListScreen
app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt:193:    RegistryListScreen(
app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt:194:    onNavigateToCreate = { backStack.add(CreateRegistryKey) },
app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt:65:    onNavigateToCreate: () -> Unit,
```

No test files under `app/src/test/java/com/giftregistry/ui/registry/list/` or `app/src/androidTest/java/com/giftregistry/ui/registry/list/` — grep/find confirmed. No test updates required.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: GiftMaisonBottomNav — weighted equal-width slots + label overflow guards</name>
  <files>
    app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
  </files>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt (full 184 lines — understand current Row + NavItemSlot + FabSlot shape)
    - .planning/phases/09-shared-chrome-status-ui/09-UI-SPEC.md § "Shared chrome → Bottom nav" (confirms 5-slot equal-width contract)
    - CLAUDE.md § "Kotlin 2.3.x / Jetpack Compose BOM 2026.03.00" (import surface for RowScope + Modifier.weight lives under androidx.compose.foundation.layout)
  </read_first>
  <action>
    Apply the following surgical edits to `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt`. Each slot must take a `Modifier` parameter so the caller can pass `Modifier.weight(1f)` from within the `Row`. Preserve every other behaviour: `GiftMaisonBottomNav(currentKey, onHome, onStores, onFab, onLists, onYou, modifier)` signature unchanged; selected-state logic (`HomeKey` → HOME, `RegistryDetailKey` → LISTS) unchanged; 22 dp FAB lift, accent colours, pill shape, monoCaps typography, 44 dp touch target — all unchanged.

    **Edit 1 — Remove the unused `width` import** (line 18):
    ```kotlin
    // DELETE:
    import androidx.compose.foundation.layout.width
    ```
    (After this edit `.width(72.dp)` on FabSlot is also gone, so no usages remain.)

    **Edit 2 — Update the Row in `GiftMaisonBottomNav`** (current lines 70-106). Two changes:
    1. Remove `horizontalArrangement = Arrangement.SpaceAround` — weights make it meaningless.
    2. Pass `Modifier.weight(1f)` to every child (4 NavItemSlots + 1 FabSlot).

    Resulting Row body:
    ```kotlin
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .border(width = 1.dp, color = colors.line)
            .navigationBarsPadding()
            .padding(top = 4.dp, bottom = 6.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItemSlot(
            icon = Icons.Outlined.Home,
            labelRes = R.string.nav_home_tab,
            isSelected = selected == NavSlotId.HOME,
            onClick = onHome,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.Outlined.Storefront,
            labelRes = R.string.nav_stores_tab,
            isSelected = selected == NavSlotId.STORES,
            onClick = onStores,
            modifier = Modifier.weight(1f),
        )
        FabSlot(
            onClick = onFab,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.AutoMirrored.Outlined.List,
            labelRes = R.string.nav_lists_tab,
            isSelected = selected == NavSlotId.LISTS,
            onClick = onLists,
            modifier = Modifier.weight(1f),
        )
        NavItemSlot(
            icon = Icons.Outlined.Person,
            labelRes = R.string.nav_you_tab,
            isSelected = selected == NavSlotId.YOU,
            onClick = onYou,
            modifier = Modifier.weight(1f),
        )
    }
    ```
    Note: `Arrangement` is still imported but no longer referenced. DELETE the now-unused import at line 6:
    ```kotlin
    // DELETE:
    import androidx.compose.foundation.layout.Arrangement
    ```
    (Verify first: `grep -n "Arrangement\." app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` should return zero hits after the edits above. The `verticalArrangement = Arrangement.Center` inside NavItemSlot and FabSlot still uses `Arrangement`, so DO NOT delete the import yet — re-check after Edit 3 and Edit 4. Actually both slots use `Arrangement.Center` → KEEP the `Arrangement` import.)

    **Edit 3 — Update `NavItemSlot` signature + apply weight-friendly layout**:
    Current NavItemSlot (lines 114-158) has no `modifier` parameter and applies its own `padding(horizontal = 6.dp)` on the Column. With weights, the Column receives an exact width share; extra horizontal padding shrinks the usable label width. Keep a small visual gap between slots by trimming to 4 dp, and add `maxLines = 1` + `softWrap = false` on the label Text.

    New NavItemSlot:
    ```kotlin
    @Composable
    private fun NavItemSlot(
        icon: ImageVector,
        labelRes: Int,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val shapes = GiftMaisonTheme.shapes
        val iconTint = if (isSelected) colors.accent else colors.inkFaint
        val labelTint = if (isSelected) colors.accent else colors.inkFaint
        val pillBg: Color = if (isSelected) colors.accentSoft else Color.Transparent

        Column(
            modifier = modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp),  // trimmed 6 -> 4 so weighted slot fits the widest label
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(shapes.pill)
                    .background(pillBg, shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(labelRes),
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(labelRes),
                style = typography.monoCaps.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else typography.monoCaps.fontWeight,
                ),
                color = labelTint,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
    ```
    Critical: the outer Column's `modifier = modifier` line places the caller-supplied weight FIRST so `.clickable` and `.padding` both respect the weighted width.

    **Edit 4 — Update `FabSlot` signature + drop hardcoded width**:
    Current FabSlot (lines 161-184) applies `.width(72.dp)` which steals extra horizontal space. With a weighted share equal to the 4 nav slots, drop the width entirely and accept a `modifier` parameter.

    New FabSlot:
    ```kotlin
    @Composable
    private fun FabSlot(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        Column(
            modifier = modifier
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 22 dp lift per handoff — hardcoded absolute value, not a spacing token.
            GiftMaisonFab(
                onClick = onClick,
                modifier = Modifier.offset(y = (-22).dp),
            )
            Text(
                text = stringResource(R.string.nav_fab_add),
                style = typography.monoCaps,
                color = colors.inkFaint,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
    ```

    **Critical constraints:**
    - DO NOT change the public `GiftMaisonBottomNav(...)` signature. `currentKey`, `onHome`, `onStores`, `onFab`, `onLists`, `onYou`, `modifier` all stay.
    - DO NOT change the selected-state mapping (`HomeKey → HOME`, `RegistryDetailKey → LISTS`, else → none).
    - DO NOT touch typography values, colour tokens, shapes, the 44 dp touch-target Box, the 22 dp FAB offset, the Row height (56 dp), the top/bottom padding (4 dp / 6 dp), the border (1 dp, `colors.line`), or the `navigationBarsPadding()` — pixel contract is intact.
    - DO NOT modify `GiftMaisonFab.kt` — it's consumed correctly.
    - DO NOT touch `NavVisibility.kt` / `showsBottomNav()` — orthogonal concern.
    - KEEP the `Arrangement` import (still used by `verticalArrangement = Arrangement.Center` inside NavItemSlot and FabSlot).
    - DELETE the `width` import (no remaining references after FabSlot edit).
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.chrome.*"</automated>
  </verify>
  <done>
    - `grep -c "Modifier.weight(1f)" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 5 (one per slot)
    - `grep -c "Arrangement.SpaceAround" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 0 (removed)
    - `grep -c ".width(72.dp)" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 0 (removed)
    - `grep -c "import androidx.compose.foundation.layout.width" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 0 (unused import removed)
    - `grep -c "maxLines = 1" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 2 (NavItemSlot label + FabSlot "ADD")
    - `grep -c "softWrap = false" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 2
    - `grep -c "padding(horizontal = 4.dp)" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` → 1 (trimmed from 6 dp)
    - `./gradlew :app:compileDebugKotlin` succeeds
    - BottomNavVisibilityTest still GREEN (signature + selected-state logic unchanged)
  </done>
</task>

<task type="auto">
  <name>Task 2: RegistryListScreen — remove legacy FAB + unused param; update AppNavigation call site</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
  </files>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt (full 304 lines — understand Scaffold shape and confirm Icons.* has no other callers)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt lines 190-210 (entry<HomeKey> block + entry<CreateRegistryKey> block)
    - .planning/phases/09-shared-chrome-status-ui/09-04-PLAN.md (confirms AddActionSheet.onNewRegistry already wires to backStack.add(CreateRegistryKey) — the owner-flow add entry point)
  </read_first>
  <action>
    **Edit A — `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt`:**

    1. **Delete the parameter** `onNavigateToCreate: () -> Unit,` from the `RegistryListScreen(...)` signature (currently line 65). Keep the three remaining parameters (`onNavigateToDetail`, `onNavigateToEdit`, `onNavigateToNotifications`).

    2. **Delete the entire `floatingActionButton = { ... }` argument** from the `Scaffold(...)` call (currently lines 93-100). That means removing:
    ```kotlin
    floatingActionButton = {
        FloatingActionButton(onClick = onNavigateToCreate) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.registry_create_button)
            )
        }
    },
    ```
    The resulting Scaffold should look like:
    ```kotlin
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.registry_list_title)) },
                actions = {
                    NotificationsInboxBell(onClick = onNavigateToNotifications)
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ...
    }
    ```
    Note: the `Icon` composable reference on line 94-98 refers to `androidx.compose.material3.Icon` (already imported at line 31). That import STAYS — it is still used by `RegistryCard` for visibility / menu icons (verify: `grep -n "Icon(" RegistryListScreen.kt` shows usages inside `RegistryCard` at the visibility lock/globe and the MoreVert menu). DO NOT delete the `material3.Icon` import.

    3. **Delete the now-unused imports** (verify via grep before each deletion):
       - Line 30: `import androidx.compose.material3.FloatingActionButton` — only use was the deleted FAB. DELETE.
       - Line 17: `import androidx.compose.material.icons.filled.Add` — only use was `Icons.Default.Add` in the deleted FAB. DELETE.
       - Line 16: `import androidx.compose.material.icons.Icons` — **grep-verify first**: `grep -n "\\bIcons\\." app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt`. Expected callers remaining (in `RegistryCard`): `Icons.Default.Lock`, `Icons.Default.Public`, `Icons.Default.MoreVert`, `Icons.Default.Edit`, `Icons.Default.Delete`. These all still need the base `Icons` import. CONCLUSION: **KEEP `import androidx.compose.material.icons.Icons`**. (The `Icons.Default.Add` use is gone, but `Icons.Default.Lock/Public/MoreVert/Edit/Delete` remain.)

       Summary of import edits for RegistryListScreen.kt:
       - DELETE `import androidx.compose.material.icons.filled.Add` (line 17)
       - DELETE `import androidx.compose.material3.FloatingActionButton` (line 30)
       - KEEP `import androidx.compose.material.icons.Icons` (line 16) — still used by Lock/Public/MoreVert/Edit/Delete
       - KEEP `import androidx.compose.material3.Icon` (line 31) — still used in RegistryCard

    4. **DO NOT delete `R.string.registry_create_button`** from `strings.xml` — leave it for now (potential cleanup todo for a later quick task; keeps this diff minimal and prevents breaking any other consumer).

    **Edit B — `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`:**

    In the `entry<HomeKey> { RegistryListScreen(...) }` block (currently lines 192-199), DELETE the line:
    ```kotlin
    onNavigateToCreate = { backStack.add(CreateRegistryKey) },
    ```

    The resulting block should be:
    ```kotlin
    entry<HomeKey> {
        RegistryListScreen(
            onNavigateToDetail = { registryId -> backStack.add(RegistryDetailKey(registryId)) },
            onNavigateToEdit = { registryId -> backStack.add(EditRegistryKey(registryId)) },
            onNavigateToNotifications = { backStack.add(NotificationsKey) },
        )
    }
    ```

    **Critical constraints:**
    - DO NOT delete `CreateRegistryKey` — it is still the navigation target of `AddActionSheet.onNewRegistry` (line ~342 of AppNavigation.kt: `onNewRegistry = { showAddSheet = false; backStack.add(CreateRegistryKey) }`).
    - DO NOT delete the `entry<CreateRegistryKey> { CreateRegistryScreen(...) }` block at line 201 — it is the screen the AddActionSheet "New registry" row routes to.
    - DO NOT touch the other three `onNavigate*` lambdas (`onNavigateToDetail`, `onNavigateToEdit`, `onNavigateToNotifications`) — they all have real callers.
    - Other screens' FABs (EditItemScreen, AddItemScreen, etc.) are out of scope — only the duplicate on RegistryListScreen is removed.
    - `GiftMaisonFab.kt` is untouched.
    - No test-file updates required — grep-confirmed no tests exist under `app/src/test/java/com/giftregistry/ui/registry/list/` or `app/src/androidTest/java/com/giftregistry/ui/registry/list/`.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug</automated>
  </verify>
  <done>
    - `grep -c "floatingActionButton" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 0
    - `grep -c "FloatingActionButton" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 0 (both the composable use and the import are gone)
    - `grep -c "onNavigateToCreate" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 0 (parameter deleted)
    - `grep -c "onNavigateToCreate" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → 0 (call site argument deleted)
    - `grep -c "import androidx.compose.material.icons.filled.Add" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 0
    - `grep -c "import androidx.compose.material3.FloatingActionButton" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 0
    - `grep -c "import androidx.compose.material.icons.Icons" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → 1 (KEPT — still used by Lock/Public/MoreVert/Edit/Delete)
    - `grep -c "Icons.Default.Lock\\|Icons.Default.Public\\|Icons.Default.MoreVert\\|Icons.Default.Edit\\|Icons.Default.Delete" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` → ≥ 5 (all RegistryCard usages intact)
    - `grep -c "backStack.add(CreateRegistryKey)" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → ≥ 2 (AddActionSheet.onNewRegistry line ~342 AND the `startActivity(deepLinkRegistryId...)` path — both preserved)
    - `grep -c "entry<CreateRegistryKey>" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` → 1 (preserved)
    - `./gradlew :app:compileDebugKotlin` succeeds
    - `./gradlew :app:testDebugUnitTest` — full unit suite GREEN
    - `./gradlew :app:assembleDebug` produces a debug APK
  </done>
</task>

</tasks>

<verification>
End-state verification after both tasks:

1. Layout sanity — no clipping and no hardcoded width:
   ```bash
   grep -c "Modifier.weight(1f)" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt   # expect 5
   grep -c ".width(72.dp)"        app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt   # expect 0
   grep -c "Arrangement.SpaceAround" app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt # expect 0
   grep -c "maxLines = 1"         app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt   # expect 2
   ```

2. Duplicate FAB eliminated:
   ```bash
   grep -c "floatingActionButton" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt   # expect 0
   grep -c "FloatingActionButton" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt   # expect 0
   grep -c "onNavigateToCreate"   app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt   # expect 0
   grep -c "onNavigateToCreate"   app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt            # expect 0
   ```

3. "Add" entry point still routed to CreateRegistryScreen via AddActionSheet:
   ```bash
   # AddActionSheet wiring (from Phase 9 Plan 04) must still exist:
   grep "onNewRegistry" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt                     # matches
   grep "backStack.add(CreateRegistryKey)" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt  # matches ≥ 1
   grep "entry<CreateRegistryKey>" app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt         # matches
   ```

4. Full regression clean:
   ```bash
   ./gradlew :app:compileDebugKotlin    # expect success
   ./gradlew :app:testDebugUnitTest     # expect all GREEN (no test file changes required)
   ./gradlew :app:assembleDebug         # expect debug APK
   ```

5. Manual smoke (not automated — optional): install the debug APK, land on Home, confirm (a) no floating FAB in the bottom-right, (b) all 5 bottom-nav labels render single-line without clipping, (c) centre FAB still lifts above the bar, (d) tapping the centre FAB opens AddActionSheet and "New registry" navigates to CreateRegistryScreen and save returns to RegistryDetail.
</verification>

<success_criteria>
- GiftMaisonBottomNav.kt: all 5 slots weighted 1f, `Arrangement.SpaceAround` removed, FabSlot's hardcoded `.width(72.dp)` removed, `width` import deleted, `maxLines=1` + `softWrap=false` on NavItemSlot label and FabSlot "ADD" text, NavItemSlot padding trimmed to 4 dp horizontal
- Public `GiftMaisonBottomNav(currentKey, onHome, onStores, onFab, onLists, onYou, modifier)` signature unchanged
- Pixel contract preserved: 22 dp FAB lift, 44 dp touch-target Box, 56 dp Row height, 4 dp top / 6 dp bottom Row padding, 1 dp `colors.line` border, `colors.paper` background, all monoCaps typography, all colour tokens
- RegistryListScreen.kt: no `floatingActionButton` argument, no `FloatingActionButton` or `filled.Add` imports, no `onNavigateToCreate` parameter, `Icons` base import kept (still used by Lock/Public/MoreVert/Edit/Delete), `material3.Icon` import kept
- AppNavigation.kt: `entry<HomeKey>` no longer passes `onNavigateToCreate`; `CreateRegistryKey` nav key and its `entry<CreateRegistryKey>` block preserved; `AddActionSheet.onNewRegistry → backStack.add(CreateRegistryKey)` wiring untouched
- Full unit suite GREEN; `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` all succeed
- Commit message: `fix(quick-260421-moi): weight bottom-nav slots + remove duplicate RegistryList FAB`
- Known follow-up (record in SUMMARY): `R.string.registry_create_button` is now unreferenced in RegistryListScreen — candidate for string cleanup in a later quick task (kept for this diff to stay minimal)
</success_criteria>

<output>
After completion, create `.planning/quick/260421-moi-fix-bottom-nav-label-truncation-weight-s/260421-moi-SUMMARY.md` documenting:
- Files modified (3): GiftMaisonBottomNav.kt, RegistryListScreen.kt, AppNavigation.kt
- Exact line changes (added / removed) per file
- Grep-verified acceptance: the 4 top-level verification greps in `<verification>` all return the expected counts
- Compile + unit test + assembleDebug results
- Manual smoke result (if user verified on device): nav labels no longer clip on 360 dp, centre FAB still lifted correctly, legacy floating FAB gone from Home, "add registry" flow still works via AddActionSheet → "New registry"
- Known follow-up: unused `R.string.registry_create_button` string resource — defer cleanup to a future quick task
- Any deviations from the plan with rationale
</output>
