---
phase: quick-260421-moi
plan: 01
subsystem: android-ui-chrome
tags: [bottom-nav, layout, fab, registry-list, navigation]
dependency_graph:
  requires: []
  provides: [equal-width-5-slot-bottom-nav, deduplicated-add-entry-point]
  affects: [GiftMaisonBottomNav, RegistryListScreen, AppNavigation]
tech_stack:
  added: []
  patterns: [Modifier.weight for equal-width Row slots, softWrap=false for single-line nav labels]
key_files:
  modified:
    - app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
decisions:
  - "Modifier.weight(1f) on all 5 bottom-nav slots gives equal width shares without needing hardcoded dp values"
  - "softWrap=false + maxLines=1 on labels as belt-and-braces guard against any remaining truncation"
  - "NavItemSlot horizontal padding trimmed 6dp -> 4dp to preserve visual gap while fitting within weighted slot"
  - "Icons base import kept in RegistryListScreen — still used by Lock/Public/MoreVert/Edit/Delete in RegistryCard"
  - "R.string.registry_create_button deferred for cleanup — kept to keep diff minimal, no other consumer"
metrics:
  duration: ~5 min
  completed_date: "2026-04-21"
  tasks_completed: 2
  files_modified: 3
---

# Quick Task 260421-moi: Fix Bottom Nav Label Truncation + Remove Duplicate FAB — Summary

**One-liner:** Equal-width weighted bottom-nav slots (5x `Modifier.weight(1f)`) with `softWrap=false` label guards; legacy `floatingActionButton` Scaffold arg removed from `RegistryListScreen` to eliminate duplicate "+" overlay on Home screen.

## Tasks Completed

| # | Name | Commit | Files Changed |
|---|------|--------|---------------|
| 1 | GiftMaisonBottomNav — weighted slots + label overflow guards | c1a1f65 | GiftMaisonBottomNav.kt |
| 2 | RegistryListScreen — remove legacy FAB; update AppNavigation call site | 6139b9c | RegistryListScreen.kt, AppNavigation.kt |

## Changes Per File

### GiftMaisonBottomNav.kt

| Change | Lines affected | Notes |
|--------|---------------|-------|
| Removed `import androidx.compose.foundation.layout.width` | line 18 | No remaining usages after FabSlot edit |
| Removed `horizontalArrangement = Arrangement.SpaceAround` from Row | line 79 | Irrelevant with weights; Arrangement import kept (still used in verticalArrangement) |
| Added `modifier = Modifier.weight(1f)` to all 5 slot calls | lines 81-105 | HOME, STORES, FAB, LISTS, YOU each get equal 1/5 share |
| Added `modifier: Modifier = Modifier` param to `NavItemSlot` | line 119 | Caller-supplied weight placed first in Column modifier chain |
| Trimmed horizontal padding 6dp → 4dp in NavItemSlot Column | line 131 | Preserves visual slot gap while fitting widest label in weighted slot |
| Added `maxLines = 1`, `softWrap = false` to NavItemSlot label Text | lines 156-157 | Belt-and-braces overflow guard |
| Added `modifier: Modifier = Modifier` param to `FabSlot` | line 163 | Removes hardcoded width; accepts weight from Row |
| Removed `.width(72.dp)` from FabSlot Column | line 167 | Width now governed by Modifier.weight(1f) from caller |
| Added `maxLines = 1`, `softWrap = false` to FabSlot "ADD" Text | lines 182-183 | Consistent with NavItemSlot label treatment |

### RegistryListScreen.kt

| Change | Notes |
|--------|-------|
| Deleted `import androidx.compose.material.icons.filled.Add` | Only use was the deleted FAB |
| Deleted `import androidx.compose.material3.FloatingActionButton` | Only use was the deleted FAB |
| Deleted `onNavigateToCreate: () -> Unit` parameter | Dead after AddActionSheet owns the "add" entry point |
| Deleted `floatingActionButton = { FloatingActionButton(...) { Icon(...) } }` Scaffold argument | Phase-9 shared centre-nav FAB owns this entry point |
| Kept `import androidx.compose.material.icons.Icons` | Still used by Lock/Public/MoreVert/Edit/Delete in RegistryCard |
| Kept `import androidx.compose.material3.Icon` | Still used throughout RegistryCard |

### AppNavigation.kt

| Change | Notes |
|--------|-------|
| Deleted `onNavigateToCreate = { backStack.add(CreateRegistryKey) }` from `entry<HomeKey>` block | Dead argument — RegistryListScreen no longer accepts this param |
| `CreateRegistryKey` nav key preserved | AddActionSheet.onNewRegistry → backStack.add(CreateRegistryKey) wiring untouched (~line 342) |
| `entry<CreateRegistryKey> { CreateRegistryScreen(...) }` preserved | Screen that AddActionSheet "New registry" routes to |

## Grep-Verified Acceptance

```
grep -c "Modifier.weight(1f)" GiftMaisonBottomNav.kt   → 5  (PASS)
grep -c "Arrangement.SpaceAround" GiftMaisonBottomNav.kt → 0  (PASS)
grep -c "width(72.dp)" GiftMaisonBottomNav.kt           → 0  (PASS)
grep -c "maxLines = 1" GiftMaisonBottomNav.kt           → 2  (PASS)
grep -c "floatingActionButton" RegistryListScreen.kt    → 0  (PASS)
grep -c "FloatingActionButton" RegistryListScreen.kt    → 0  (PASS)
grep -c "onNavigateToCreate" RegistryListScreen.kt      → 0  (PASS)
grep -c "onNavigateToCreate" AppNavigation.kt           → 0  (PASS)
grep "backStack.add(CreateRegistryKey)" AppNavigation.kt → 1 match (PASS)
grep "entry<CreateRegistryKey>" AppNavigation.kt        → 1 match (PASS)
```

## Build Results

- `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- `:app:testDebugUnitTest` — BUILD SUCCESSFUL (all GREEN)
- `:app:assembleDebug` — BUILD SUCCESSFUL (debug APK produced)

Pre-existing warnings (pre-date this task, out of scope):
- `Icons.Outlined.KeyboardArrowRight` deprecated in AddActionSheet.kt
- `Icons.Filled.FormatListBulleted` deprecated in OnboardingScreen.kt
- `menuAnchor()` deprecated in CreateRegistryScreen.kt

## Deviations from Plan

None — plan executed exactly as written.

## Known Follow-Up Items

- `R.string.registry_create_button` is now unreferenced in RegistryListScreen — candidate for string resource cleanup in a future quick task. Kept in this diff to maintain minimal scope. No compile error since Android string resources are not checked for unused references at compile time.

## Self-Check: PASSED

- GiftMaisonBottomNav.kt: exists and contains 5x `Modifier.weight(1f)`, 2x `maxLines = 1`, 0x `Arrangement.SpaceAround`, 0x `.width(72.dp)`
- RegistryListScreen.kt: exists, no `floatingActionButton`, no `FloatingActionButton`, no `onNavigateToCreate`
- AppNavigation.kt: exists, no `onNavigateToCreate`, `CreateRegistryKey` preserved
- Commits c1a1f65 and 6139b9c confirmed in git log
