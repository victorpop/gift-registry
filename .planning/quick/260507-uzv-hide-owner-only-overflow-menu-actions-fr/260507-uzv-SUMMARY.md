---
phase: quick-260507-uzv
plan: 01
subsystem: ui-registry-detail
type: ui-fix
tags: [auth, ownership, ui, overflow-menu, registry-detail]
requires:
  - RegistryDetailViewModel.isOwner (StateFlow<Boolean>) — already exposed since Phase 12 D-13
  - AuthRepository.authState (Flow<User?>) — Phase 02
  - ObserveRegistryUseCase (Flow<Registry?>) — Phase 03
provides:
  - UI gate hiding kebab IconButton + DropdownMenu from non-owners
  - Pinned isOwner contract via RegistryDetailViewModelIsOwnerTest (4 cases)
affects:
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
tech-stack:
  added: []
  patterns:
    - Nullable callback as the gate (mirrors existing onCoverTap pattern in the same file)
    - Defence-in-depth: trigger nulled out + entire DropdownMenu Box removed from composition
key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
decisions:
  - Reused the existing viewModel.isOwner StateFlow rather than introducing a new auth abstraction — single source of truth, mirrors server-side check at inviteToRegistry.ts:50
  - Used the established onCoverTap nullable-callback convention (same file, D-13) for the kebab gate instead of a separate showOverflow Boolean flag
  - Defence-in-depth: gated BOTH the trigger (onOverflow = null at call site) AND the DropdownMenu Box (wrapped in if (isOwner)) so the menu region is excluded from composition for non-owners
  - Top-bar Share icon, hero share-pill, ShareBanner row left untouched per spec ("Out of scope: top-bar Share")
  - D-13 cover-photo tap target (onCoverTap) untouched — it was already owner-gated and remains so
metrics:
  duration: 3m 23s
  commits: 2
  files_changed: 3
  tasks_completed: 2
  test_count: 4
  completed: 2026-05-07
---

# Quick Task 260507-uzv: Hide owner-only overflow menu actions Summary

Owner-only overflow region (Edit / Share / Invite / Delete + the kebab icon that opens it) on `RegistryDetailScreen` is now hidden from non-owners by gating the existing `RegistryDetailViewModel.isOwner` StateFlow at the UI layer.

## What changed

**Before:** Any signed-in viewer of a registry — owner or invitee — saw the three-dot kebab in the top-right of the hero toolbar; tapping it opened a DropdownMenu with Edit Registry, Share Registry, Invite People, and Delete. The server already rejected non-owner attempts (Cloud Function `inviteToRegistry` line 50, Firestore rules for Edit/Delete) but the client UI was misleading.

**After:** Non-owners see no kebab and no overflow menu. Owners see the unchanged Edit / Share / Invite / Delete menu.

## Implementation

Pure client-side UI gate that mirrors the canonical server-side `registryData.ownerId !== request.auth.uid` ownership check.

### Pattern reused: nullable callback

Same convention already established in `RegistryDetailHero.kt` for the D-13 cover-photo tap target — pass `null` for guests, the affordance disappears entirely:

```kotlin
// RegistryDetailHero.kt
onOverflow: (() -> Unit)? = null,    // <-- new (was: () -> Unit)
onCoverTap: (() -> Unit)? = null,    // <-- existing, unchanged
```

```kotlin
// Inside the toolbar Row
if (onOverflow != null) {
    IconButton(onClick = onOverflow) { Icon(Icons.Default.MoreVert, ...) }
}
```

### Call site (RegistryDetailScreen.kt)

```kotlin
RegistryDetailHero(
    ...
    onOverflow = if (isOwner) ({ overflowMenuExpanded = true }) else null,
    onCoverTap = if (isOwner) ({ pickerSheetOpen = true }) else null,   // unchanged
)
```

### Defence in depth

Even though the trigger is null for non-owners (so `overflowMenuExpanded` cannot be flipped true), the entire `DropdownMenu` Box is also wrapped in `if (isOwner) { ... }` so the menu region is removed from the composition for non-owners:

```kotlin
if (isOwner) {
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 8.dp)) {
        DropdownMenu(expanded = overflowMenuExpanded, ...) {
            // Edit / Share / Invite / Delete items unchanged
        }
    }
}
```

### Why no VM changes

`RegistryDetailViewModel.isOwner` already existed (lines 182-189), introduced in Phase 12 for the D-13 cover-photo tap target. It is correct as-is:

```kotlin
val isOwner: StateFlow<Boolean> = combine(registry, authRepository.authState) { reg, user ->
    reg != null && user != null && reg.ownerId == user.uid
}
    .catch { emit(false) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

`SharingStarted.Eagerly` + initial `false` + `.catch { emit(false) }` means non-owner UI is the safe default until the first emission resolves — no flash of owner-only items during load.

## Tests

**4 new contract tests** in `RegistryDetailViewModelIsOwnerTest.kt` pin the existing StateFlow behaviour the UI gate now consumes:

| Case                                          | Expected         |
| --------------------------------------------- | ---------------- |
| `registry.ownerId == authState.uid`           | `isOwner = true` |
| `registry.ownerId != authState.uid` (invitee) | `isOwner = false` |
| `registry == null` (loading / not-found)      | `isOwner = false` |
| `authState == null` (signed-out)              | `isOwner = false` |

```
testsuite name="RegistryDetailViewModelIsOwnerTest" tests=4 failures=0 errors=0 time=0.729
```

Full app test suite (`:app:testDebugUnitTest`) — 54 suites, all green. No regressions in `RegistryDetailViewModelConfirmPurchaseTest`, `HeroToolbarAlphaTest`, etc.

## Observable behaviour delta

| Viewer                                                                | Kebab icon | Edit / Share / Invite / Delete menu | Top-bar Share icon | Hero share-pill | Cover-photo tap |
| --------------------------------------------------------------------- | ---------- | ----------------------------------- | ------------------ | --------------- | --------------- |
| Owner (uid == ownerId)                                                | Visible    | Visible on tap                      | Visible            | Visible         | Enabled         |
| Invitee / non-owner (uid != ownerId) — **the bug being fixed**        | **Hidden** | **Hidden**                          | Visible            | Visible         | Disabled        |
| Loading (registry == null)                                            | Hidden     | Hidden                              | Visible            | n/a             | Disabled        |
| Signed-out (authState == null)                                        | Hidden     | Hidden                              | Visible            | n/a             | Disabled        |

## Verification

**Automated** (run and green):

```bash
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.RegistryDetailViewModelIsOwnerTest" --rerun-tasks  # 4/4 GREEN
./gradlew :app:compileDebugKotlin                                                                                                  # BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest                                                                                                   # 54 suites, 0 failures, 0 errors
grep -rn "RegistryDetailHero(" app/src/ | grep -v "internal fun"   # 3 hits, all use named args -> reorder safe
```

**Manual device verification** (deferred to user, not blocking — same convention as previous quick tasks):

1. Sign in as a registry owner -> open RegistryDetailScreen -> kebab visible top-right -> tap -> see Edit / Share / Invite / Delete. Unchanged.
2. Sign in as a different user invited to that registry -> kebab gone -> no overflow menu accessible. Top-bar Share icon (OpenInNew) and hero share-pill remain visible. Cover-photo tap target on hero is also gone (existing D-13 behaviour, unchanged).
3. Edit / Delete / Invite via Cloud Functions still rejected server-side for non-owners (defence in depth — already in place, just confirming nothing was relaxed).

## What was NOT touched

- `RegistryDetailViewModel.kt` — VM contract was already correct
- `AuthRepository.kt` — surface unchanged
- `firestore.rules`, `storage.rules` — server-side ownership checks unchanged
- `functions/src/registry/inviteToRegistry.ts` — Cloud Function ownership check unchanged
- `strings.xml` (any locale) — no labels added or removed
- Top-bar Share icon (OpenInNew) on the hero toolbar
- Hero share-pill / `ShareBanner` row
- D-13 cover-photo tap target (`onCoverTap`)
- ConfirmPurchaseBanner / SnackbarHost
- StyleGuidePreview hero previews — they pass `onOverflow = {}` (a non-null lambda) so the kebab stays in the @Preview, which is the correct behaviour for a style-guide reference

## Deviations from Plan

None — plan executed exactly as written. Both tasks completed in the planned order with the exact files / signatures / patterns specified in the plan's `<interfaces>` and `<action>` blocks.

## Commits

| Task | Hash    | Message                                                                  |
| ---- | ------- | ------------------------------------------------------------------------ |
| 1    | a332866 | test(quick-260507-uzv-01): pin RegistryDetailViewModel.isOwner contract  |
| 2    | ed1f180 | feat(quick-260507-uzv-02): hide owner-only overflow actions from non-owners |

## Outstanding Follow-up

Manual device verification (Task 2 done criteria — non-blocking):
- Owner walkthrough on bug-reproducing account: confirm kebab + Edit / Share / Invite / Delete still work
- Invitee walkthrough: confirm kebab disappears entirely on invited registry, top-bar Share + hero share-pill still present

## Self-Check: PASSED

**Files verified to exist:**
- `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt` — FOUND
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — FOUND (modified)
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — FOUND (modified)

**Commits verified:**
- a332866 (Task 1) — FOUND
- ed1f180 (Task 2) — FOUND

**Stub scan:** No stubs introduced. The two production-code edits remove UI surface for non-owners; they do not introduce new components with unwired data sources, hardcoded empty values, or "coming soon" placeholders. The 4 new unit tests are real, behaviour-asserting tests (not no-op stubs).
