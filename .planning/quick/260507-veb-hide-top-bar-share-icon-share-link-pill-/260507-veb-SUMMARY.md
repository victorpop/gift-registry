---
phase: quick-260507-veb
plan: 01
subsystem: ui-registry-detail
tags:
  - quick
  - ui-gating
  - owner-only-affordances
  - non-owner-ux
  - registry-detail
dependency_graph:
  requires:
    - quick-260507-uzv (RegistryDetailHero.onOverflow nullable-callback convention; RegistryDetailViewModel.isOwner StateFlow already pinned by RegistryDetailViewModelIsOwnerTest)
    - Phase 12 D-13 (RegistryDetailHero.onCoverTap nullable-callback convention; the same isOwner StateFlow seed)
  provides:
    - RegistryDetailHero.onShare nullable-callback convention (third sibling alongside onOverflow + onCoverTap)
    - LazyListScope `if (isOwner) { item(...) }` gating pattern for owner-only list items
  affects:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
tech_stack:
  added: []
  patterns:
    - Nullable-callback owner-gate on Compose owner-only icon buttons (third sibling to onOverflow + onCoverTap; same file, same convention)
    - `if (isOwner) { item(key=..) { ... } }` inside `LazyListScope` lambda for conditional list-item registration (matches the existing `if (filteredItems.isEmpty() && items.isNotEmpty())` block in the same LazyColumn)
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
decisions:
  - Reused existing RegistryDetailViewModel.isOwner StateFlow (Phase 12 D-13 seed, pinned by RegistryDetailViewModelIsOwnerTest in quick-260507-uzv) — no new VM logic, no new auth abstraction, no new tests
  - Mirrored the existing nullable-callback gating convention for the top-bar Share icon — same shape as onOverflow (quick-260507-uzv) and onCoverTap (Phase 12 D-13) so all three owner-only IconButtons in RegistryDetailHero now follow one consistent pattern
  - Used standard Compose `if (isOwner) { item(...) }` inside LazyListScope for ShareBanner + AddItemTopCta — items are conditionally registered at composition time, no key collision possible because non-owners never have an item with those keys
  - Spacer(Modifier.weight(1f)) retained between Back arrow and the (now-conditional) Share IconButton — without it the kebab would slide left next to Back for owners (regression on owner UX)
  - Owner-gating decisions made client-side ONLY for UX alignment with the server's ownership contract; firestore.rules + Cloud Functions still authoritative (Add Item rejects non-owners regardless of UI state)
metrics:
  duration: 1.6min
  completed_date: "2026-05-07"
---

# Quick Task 260507-veb: Hide Top-Bar Share Icon, ShareBanner Pill, and Add Item CTA from Non-Owners on RegistryDetailScreen — Summary

Owner-only client-side gate on three RegistryDetailScreen affordances — top-bar Share IconButton (OpenInNew), ShareBanner "Tap to copy or share" pill, and "+ Add an item" primary CTA — extending quick-260507-uzv's kebab gate via the same `viewModel.isOwner` StateFlow.

## What Was Built

Three coordinated edits across two files; one atomic commit (`1f563db`).

### Edit A — `RegistryDetailHero.kt`

1. Renamed parameter `onShare: () -> Unit` to `onShare: (() -> Unit)? = null` and moved it to AFTER `modifier` so trailing nullable callbacks (`onShare`, `onOverflow`, `onCoverTap`) follow one convention. Final parameter order: `registry, listState, onBack, modifier, onShare, onOverflow, onCoverTap`.
2. Wrapped the OpenInNew IconButton in `if (onShare != null) { IconButton(onClick = onShare) { ... } }`, mirroring the existing `if (onOverflow != null)` block at lines 196-204.
3. Spacer(Modifier.weight(1f)) preserved between Back and Share — keeps the kebab pushed to the far right when Share is hidden for non-owners (no regression on owner UX).
4. KDoc added for `onShare` describing the nullable-gate convention (kept to a single block per parameter; not double-documented across `onOverflow`).

### Edit B — `RegistryDetailScreen.kt`

1. `RegistryDetailHero` call site: `onShare = if (isOwner) onShareTap else null` (matches the sibling `onOverflow` and `onCoverTap` conditionals on the two lines below).
2. ShareBanner LazyColumn item wrapped in `if (isOwner) { item(key = "share") { ShareBanner(...) } }`.
3. AddItemTopCta LazyColumn item wrapped in `if (isOwner) { item(key = "add-item-cta") { AddItemTopCta(onClick = onNavigateToAddItem) } }`.

The same `viewModel.isOwner` StateFlow is now consumed by **four** call sites in `RegistryDetailScreen`:
- `onShare` (this task)
- `onOverflow` (quick-260507-uzv)
- `onCoverTap` (Phase 12 D-13)
- `pickerSheetOpen` guard (Phase 12 D-13)

…and the dropdown Box (`if (isOwner) { Box(...) { DropdownMenu(...) } }`, quick-260507-uzv) — five total isOwner consumers, all gating owner-only affordances.

## Pattern Reused

Two pre-existing project patterns; zero new conventions introduced:

1. **Nullable-callback owner-gate** — `RegistryDetailHero.onCoverTap` (Phase 12 D-13) introduced this for the cover-photo tap target. `RegistryDetailHero.onOverflow` (quick-260507-uzv) extended it to the kebab IconButton. This task extends it to the third top-bar IconButton (Share / OpenInNew). All three sibling owner-only icons in this file now follow one shape: `(() -> Unit)? = null` parameter + `if (callback != null)` wrap at the IconButton call site + `if (isOwner) ... else null` at the screen call site.
2. **Conditional LazyListScope item** — the same LazyColumn already contains `if (filteredItems.isEmpty() && items.isNotEmpty()) { item(...) { ... } }` and `else if (items.isEmpty()) { item(...) { ... } }` at lines 246-255. Wrapping `item(key = "share") { ... }` and `item(key = "add-item-cta") { ... }` in `if (isOwner) { ... }` is the standard Compose pattern — non-owners simply never register those keys.

## Owner / Non-Owner Behavioural Delta

| Affordance | Owner (`registry.ownerId == user.uid`) | Non-owner (signed-in invitee, OR `registry == null` / `user == null` during load) |
|---|---|---|
| Top-bar Back arrow | Visible | Visible |
| Top-bar Share icon (OpenInNew) | Visible — taps fire share intent + clipboard copy + snackbar | **Hidden** (IconButton not in composition) |
| Top-bar kebab (MoreVert) | Visible — opens Edit / Share / Invite / Delete menu | Hidden (quick-260507-uzv) |
| ShareBanner pill ("Tap to copy or share — gift-registry-ro.web.app/r/...") | Visible | **Hidden** (LazyColumn item not registered) |
| StatsStrip (Items / Reserved / Given / Views counters) | Visible | Visible |
| FilterChipsRow (All / Open / Reserved / Completed) | Visible | Visible |
| "+ Add an item" primary CTA (accent fill) | Visible — taps open AddItemKey | **Hidden** (LazyColumn item not registered) |
| Items list (RegistryItemRow) — reserve/buy actions | Visible — owner sees but typically uses the giver flow only on registries they don't own | Visible — invitees use this to reserve and buy |
| ConfirmPurchaseBanner (giver flow with active reservation) | Visible if VM reports active reservation | Visible if VM reports active reservation (unchanged) |
| Cover-photo hero tap target (D-13) | Tappable | Not tappable (clickable disabled) |

## Files Modified

| File | Lines changed | Purpose |
|---|---|---|
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` | onShare parameter (signature reorder + KDoc) + IconButton wrap | Top-bar Share icon now nullable, owner-only at composition |
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` | 3 call-site edits (onShare conditional, ShareBanner if-wrap, AddItemTopCta if-wrap) | Routes `isOwner` to the three new gates |

Both files in one atomic commit: `1f563db`.

## Verification

**Automated (executed during plan):**

```
./gradlew :app:compileDebugKotlin -q     # GREEN
./gradlew :app:testDebugUnitTest -q      # GREEN (all existing tests still pass)
grep -rn "RegistryDetailHero(" app/src/ | grep -v "internal fun" | grep -v "//"
```

The grep step returned 3 hits — 1 in `RegistryDetailScreen.kt` (named args throughout: safe under reorder) and 2 in `StyleGuidePreview.kt` (also named args: `onShare = {}, onOverflow = {}` — safe under reorder; build confirmed). Parameter reorder of `onShare` to AFTER `modifier` did not break any caller.

**Why no new tests:**

The `isOwner` contract (match→true, mismatch→false, null-registry→false, null-user→false, plus the `Eagerly + initial=false + .catch{emit(false)}` safe-default-during-load behaviour) is already pinned by `RegistryDetailViewModelIsOwnerTest` from quick-260507-uzv. This plan only adds three new call-site `if (isOwner)` consumers, each of which is trivially equivalent to the kebab + cover-tap consumers already wired and exercised through the same StateFlow. Adding bespoke UI tests for each new call site would duplicate that coverage without improving signal.

**Manual device verification (deferred, combined with quick-260507-uzv Task 2):**

1. Sign in as a registry's owner → open RegistryDetailScreen → top-bar Share icon (OpenInNew), ShareBanner pill ("Tap to copy or share — gift-registry-ro.web.app/r/..."), AND "+ Add an item" primary button all visible. Tap Share → share intent fires + clipboard + snackbar. Tap pill → link copied + chooser. Tap "Add an item" → AddItemKey opens with picker hidden.
2. Sign in as a different user who has been invited to that registry → open RegistryDetailScreen → top-bar Share icon GONE, ShareBanner pill GONE, "+ Add an item" button GONE. Items list (e.g. SALTSJÖBADEN, Ibric) still visible. FilterChipsRow (All / Open / Reserved / Completed) still visible. StatsStrip (Items / Reserved / Given / Views) still visible. Tap an open item row → reserve flow still works (giver flow, intentionally not gated).
3. Sign-out / loading state → no flash of owner-only affordances on the brief moment between screen open and isOwner emission resolving (safe-default-false from `Eagerly + initial=false + .catch{emit(false)}`).
4. Confirm previously-fixed quick-260507-uzv is unaffected: kebab still hidden for invitees, still visible for owners; D-13 cover-photo tap still owner-only; ConfirmPurchaseBanner still rendered for any user with an active reservation regardless of ownership.

## Out of Scope (Explicitly Untouched)

- `RegistryDetailViewModel.kt` — no changes (existing isOwner StateFlow reused as-is)
- `AuthRepository.kt`, `firestore.rules`, `storage.rules` — no changes
- Any Cloud Function — no changes (Cloud Functions still authoritative for ownership; e.g. Add Item rejects non-owners regardless of UI state)
- Any `strings.xml` resource — no changes (pure UI gating, no new labels added or removed)
- `ShareBanner.kt` — no changes (only the call-site is conditionally registered)
- `AddItemTopCta` (private composable in `RegistryDetailScreen.kt`) — no changes (only the call-site wrapper is conditional)
- `StatsStrip`, `FilterChipsRow`, `RegistryItemRow`, `ConfirmPurchaseBanner`, top-bar Back button — all unchanged
- `RegistryDetailViewModelIsOwnerTest` — no changes (already pins the contract)

## Deviations from Plan

None — plan executed exactly as written. The single grep verification returned 3 hits instead of the planned 1, but the additional 2 hits (in `StyleGuidePreview.kt`) also use named arguments and the build confirmed they compile cleanly. The "exactly ONE call site" expectation in the plan's <done> block was an oversight that the build itself caught more rigorously.

## Outstanding Follow-Up

- **Device verification** (deferred): combine with the still-pending quick-260507-uzv Task 2 device verification. Three new affordances to verify present-for-owner / absent-for-invitee on top of the kebab + DropdownMenu items already covered by uzv's checklist.

## Self-Check: PASSED

- File `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — FOUND (modified)
- File `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — FOUND (modified)
- Commit `1f563db` — FOUND in `git log`
- `:app:compileDebugKotlin` — GREEN
- `:app:testDebugUnitTest` — GREEN (no regression on RegistryDetailViewModelIsOwnerTest, RegistryDetailViewModelConfirmPurchaseTest, HeroToolbarAlphaTest, etc.)
