---
phase: quick-260507-veb
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
autonomous: true
requirements:
  - QUICK-260507-VEB: Hide three more owner-only affordances from non-owners on RegistryDetailScreen — (1) top-bar Share icon (OpenInNew) in the hero toolbar, (2) "Tap to copy or share" ShareBanner pill, (3) "+ Add an item" primary CTA. All three render only when `viewModel.isOwner == true`. Reuse the existing `isOwner` StateFlow (already collected at RegistryDetailScreen.kt:86 from quick-260507-uzv).

must_haves:
  truths:
    - "Owner viewing their own registry sees the top-bar Share icon (OpenInNew), the ShareBanner pill, AND the '+ Add an item' primary CTA — unchanged behaviour."
    - "Non-owner (signed-in user whose uid != registry.ownerId, OR an invitee opening someone else's registry) sees NONE of the three: no Share icon in the top toolbar, no ShareBanner pill, no '+ Add an item' button."
    - "Loading / signed-out state (registry == null OR user == null → isOwner == false) sees NO owner-only affordances — no flash of owner UI during load (matches existing isOwner=false default from .catch+initial=false in RegistryDetailViewModel.kt:182-189)."
    - "Items list (SALTSJÖBADEN, Ibric, etc.), filter chips (All / Open / Reserved / Completed), metric counters (Items / Reserved / Given / Views), and item-row reserve/buy actions all REMAIN visible to invitees — only the three named affordances are gated."
    - "Existing owner-only kebab + DropdownMenu (quick-260507-uzv) and D-13 cover-photo tap target — UNCHANGED, no regression."
    - "ConfirmPurchaseBanner (giver flow) — UNCHANGED, still rendered for any user with an active reservation regardless of ownership."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt"
      provides: "Top-bar Share IconButton conditional on non-null onShare callback (mirrors existing onOverflow nullable pattern from quick-260507-uzv in same file)"
      contains: "if (onShare != null)"
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      provides: "isOwner-gated call sites: onShare callback null when !isOwner, ShareBanner item wrapped in `if (isOwner)`, AddItemTopCta item wrapped in `if (isOwner)`"
      contains: "if (isOwner)"
  key_links:
    - from: "RegistryDetailScreen.RegistryDetailHero call site (~line 196-208)"
      to: "RegistryDetailHero.onShare IconButton (RegistryDetailHero.kt:186-192)"
      via: "nullable onShare callback"
      pattern: "onShare = if \\(isOwner\\)"
    - from: "RegistryDetailScreen LazyColumn share item (~line 214-219)"
      to: "ShareBanner composable (ShareBanner.kt:38)"
      via: "if (isOwner) { item(key = \"share\") { ShareBanner(...) } }"
      pattern: "if \\(isOwner\\) \\{[^}]*ShareBanner"
    - from: "RegistryDetailScreen LazyColumn add-item-cta item (~line 231-233)"
      to: "AddItemTopCta composable (RegistryDetailScreen.kt:440)"
      via: "if (isOwner) { item(key = \"add-item-cta\") { AddItemTopCta(...) } }"
      pattern: "if \\(isOwner\\) \\{[^}]*AddItemTopCta"
---

<objective>
Hide three additional owner-only affordances from non-owners on RegistryDetailScreen:
1. **Top-bar Share icon** — the `OpenInNew` IconButton at top-right of the 180 dp hero (RegistryDetailHero.kt:186-192).
2. **ShareBanner row** — the "Tap to copy or share" pill displaying `gift-registry-ro.web.app/r/...` (RegistryDetailScreen.kt:214-219).
3. **"+ Add an item" primary button** — the accent-fill `AddItemTopCta` above the items list (RegistryDetailScreen.kt:231-233).

Purpose: Following quick-260507-uzv (which hid the kebab/overflow menu from non-owners), three more owner-only affordances remain visible to invitees and other non-owners. They are useless to non-owners (the share link is the same one they already received; the Add Item flow writes via Cloud Function that rejects non-owners). Hiding them aligns the client UI with the server's ownership contract (`registryData.ownerId !== request.auth.uid`).

Output: Three owner-only affordances disappear for non-owners. Owner experience unchanged. Items list, filter chips, metric counters, item-row reserve/buy actions, ConfirmPurchaseBanner, top-bar back button — all unchanged.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@.planning/STATE.md
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt
@.planning/quick/260507-uzv-hide-owner-only-overflow-menu-actions-fr/260507-uzv-PLAN.md

<interfaces>
<!-- Key contracts the executor needs. Do NOT re-explore — these are the exact symbols in play. -->

`RegistryDetailViewModel.isOwner` ALREADY exists (RegistryDetailViewModel.kt:182-189) and is ALREADY collected in RegistryDetailScreen at line 86:
```kotlin
val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
```
**Reuse it.** Defaults to `false` while loading (`SharingStarted.Eagerly` + initial `false` + `.catch { emit(false) }`) — no flash of owner-only UI. Already in use at the cover-photo callsite (line 206) and the overflow callsite (line 203) from quick-260507-uzv.

`RegistryDetailHero` current signature (RegistryDetailHero.kt:60-82, AFTER quick-260507-uzv):
```kotlin
internal fun RegistryDetailHero(
    registry: Registry?,
    listState: LazyListState,
    onBack: () -> Unit,
    onShare: () -> Unit,                  // <-- becomes onShare: (() -> Unit)? = null in this plan
    modifier: Modifier = Modifier,
    onOverflow: (() -> Unit)? = null,     // <-- already nullable from quick-260507-uzv
    onCoverTap: (() -> Unit)? = null,     // <-- already nullable from Phase 12 D-13
)
```
The kebab IconButton at lines 196-204 is already wrapped in `if (onOverflow != null) { ... }` — mirror exactly the same pattern for `onShare`.

`ShareBanner` signature (ShareBanner.kt:38-42):
```kotlin
internal fun ShareBanner(
    registryId: String,
    onShared: () -> Unit,
    modifier: Modifier = Modifier,
)
```
Single call site at RegistryDetailScreen.kt:214-219 inside `item(key = "share") { ShareBanner(...) }`.

`AddItemTopCta` is a private composable in RegistryDetailScreen.kt:439-470. Single call site at line 231-233 inside `item(key = "add-item-cta") { AddItemTopCta(onClick = onNavigateToAddItem) }`.

Single call site for `RegistryDetailHero(...)` is at RegistryDetailScreen.kt:196-208 — uses named arguments throughout, so reordering the `onShare` parameter is safe.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Gate top-bar Share icon (nullable callback) in RegistryDetailHero, and gate ShareBanner + AddItemTopCta call sites on isOwner in RegistryDetailScreen</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  </files>
  <action>
    Three coordinated edits across two files. All gating reuses the existing `viewModel.isOwner` StateFlow already collected at RegistryDetailScreen.kt:86 (no new VM logic, no new tests, no string changes — `RegistryDetailViewModelIsOwnerTest` from quick-260507-uzv already pins the contract).

    **Edit A — `RegistryDetailHero.kt` (top-bar Share icon, mirror the kebab nullable-callback pattern from quick-260507-uzv):**

    1. Change the `onShare` parameter on line 65 from:
       ```kotlin
       onShare: () -> Unit,
       ```
       to:
       ```kotlin
       onShare: (() -> Unit)? = null,
       ```
       Move it to AFTER `modifier` to match the established trailing-nullable convention used by `onOverflow` and `onCoverTap`. Final parameter order:
       ```
       registry, listState, onBack, modifier, onShare, onOverflow, onCoverTap
       ```
       The single call site at RegistryDetailScreen.kt:200 uses named args (`onShare = onShareTap`), so the reorder is safe.

    2. Wrap the Share IconButton (lines 186-192) in a null-check, mirroring the existing `if (onOverflow != null)` block at lines 196-204:
       ```kotlin
       if (onShare != null) {
           IconButton(onClick = onShare) {
               Icon(
                   imageVector = Icons.Default.OpenInNew,
                   contentDescription = stringResource(R.string.registry_detail_share_button_desc),
                   tint = if (toolbarAlpha > 0.5f) colors.ink else colors.paper,
               )
           }
       }
       ```
       The `Spacer(Modifier.weight(1f))` between Back and Share IconButton MUST stay — it pushes the kebab to the far right when the Share icon is hidden. Without the Spacer the kebab would slide left next to the back arrow for owners (regression on the owner UX).

       Update the KDoc comment block on `onOverflow` (lines 67-73) to also mention `onShare` follows the same nullable-gate convention, OR add a brief KDoc on the new `onShare` nullable parameter. Pick whichever keeps the doc concise; do not double-document.

    **Edit B — `RegistryDetailScreen.kt` (three changes, all gated on `isOwner` already in scope at line 86):**

    1. At the `RegistryDetailHero` call site (lines 196-208), change `onShare = onShareTap` to:
       ```kotlin
       onShare = if (isOwner) onShareTap else null,
       ```
       This matches the exact pattern used two lines below for `onOverflow` (line 203) and `onCoverTap` (line 206). Three sibling owner-gated callbacks, three identical conditionals.

    2. Wrap the ShareBanner LazyColumn item (lines 214-219) in `if (isOwner) { ... }`:
       ```kotlin
       if (isOwner) {
           item(key = "share") {
               ShareBanner(
                   registryId = registryId,
                   onShared = { scope.launch { snackbarHostState.showSnackbar(linkCopiedMsg) } },
               )
           }
       }
       ```
       NOTE: `if` blocks are valid inside a `LazyListScope` lambda — they conditionally register items at composition time. This is the standard Compose pattern; the existing `if (filteredItems.isEmpty() && items.isNotEmpty())` block at lines 246-250 in this same `LazyColumn` confirms the convention.

    3. Wrap the AddItemTopCta LazyColumn item (lines 231-233) in `if (isOwner) { ... }`:
       ```kotlin
       if (isOwner) {
           item(key = "add-item-cta") {
               AddItemTopCta(onClick = onNavigateToAddItem)
           }
       }
       ```
       Same Compose pattern as the ShareBanner gate above. Item key `"add-item-cta"` is unique only when this item is registered, which is fine because LazyColumn re-evaluates the lambda on recomposition; non-owners simply never have an item with that key.

    **Constraints honoured:**
    - **Reuse existing `isOwner` StateFlow** — no new VM logic, no new auth abstraction. Already pinned by `RegistryDetailViewModelIsOwnerTest` (quick-260507-uzv).
    - **No new tests required.** The contract for `isOwner` (match→true, mismatch→false, null-registry→false, null-user→false) is already pinned. This plan only adds three new call-site `if (isOwner)` consumers, each of which is trivially equivalent to the kebab + cover-tap consumers already wired and tested via the same flow.
    - **No string resource changes.** Pure UI gating — no labels added, no labels removed.
    - **No security rule / Cloud Function changes.** The server already enforces ownership for create/edit/delete via Cloud Functions and Firestore rules. The Add Item flow writes via Cloud Function that rejects non-owners; the share link is just a URL anyone with access could compute. This is a UI-only correctness gate.
    - **Out of scope (DO NOT touch):**
      * Items list rendering (still visible to invitees)
      * FilterChipsRow (still visible to invitees)
      * StatsStrip / metric counters (still visible to invitees)
      * RegistryItemRow reserve/buy actions (giver flow — invitees use these)
      * ConfirmPurchaseBanner (giver flow — invitees use this)
      * Top-bar back button (everyone can navigate back)
      * RegistryDetailViewModel (no changes)
      * AuthRepository, firestore.rules, storage.rules, any Cloud Function, any strings.xml
      * D-13 cover-photo tap (already gated, untouched)
      * Kebab + DropdownMenu (already gated by quick-260507-uzv, untouched)
    - **Loading state safe.** `isOwner` defaults to `false` so the three affordances are HIDDEN by default during load — no flash of owner-only UI. This is the same safe-default behaviour used by quick-260507-uzv for the kebab.
    - **Owner regression sanity.** Edit A.2 keeps the `Spacer(Modifier.weight(1f))` between Back and Share — owner UX is unchanged (Back at left, Share at right, kebab to the right of Share). Verifying via grep that no other file passes `onShare` positionally is part of <verify> below.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest -q && grep -rn "RegistryDetailHero(" app/src/ | grep -v "internal fun" | grep -v "//"</automated>
  </verify>
  <done>
    - `:app:compileDebugKotlin` succeeds (parameter signature reorder on `onShare` does not break any callers — only call site at RegistryDetailScreen.kt uses named args).
    - All existing app unit tests still pass (no regression on `RegistryDetailViewModelIsOwnerTest`, `RegistryDetailViewModelConfirmPurchaseTest`, `HeroToolbarAlphaTest`, etc.).
    - The grep step returns exactly ONE call site (`RegistryDetailScreen.kt`), confirming the parameter reorder is safe.
    - `RegistryDetailHero.onShare: (() -> Unit)? = null` and is wrapped in `if (onShare != null)` — kebab pattern mirrored exactly.
    - `RegistryDetailScreen.kt` passes `onShare = if (isOwner) onShareTap else null`, wraps the `ShareBanner` LazyColumn item in `if (isOwner)`, wraps the `AddItemTopCta` LazyColumn item in `if (isOwner)`.
    - Manual visual contract (deferred to user): Owner sees top-bar Share icon, ShareBanner pill, "+ Add an item" CTA — unchanged. Non-owner / invitee sees NONE of those three. Items list, filter chips, metric counters, reserve/buy actions, kebab-already-hidden — UNCHANGED.
  </done>
</task>

</tasks>

<verification>
**Automated** (must pass before marking the quick task complete):

```bash
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*" -q
./gradlew :app:compileDebugKotlin -q
```

**Owner-side regression sanity check** (run after the build is green):

```bash
# Confirm no other file passes onShare positionally — the parameter reorder must be safe.
grep -rn "RegistryDetailHero(" app/src/ | grep -v "internal fun"
```
Expected: a single hit at `RegistryDetailScreen.kt` using named arguments.

**Manual device verification** (deferred to user; not blocking):
1. Sign in as a Registry's owner → open RegistryDetailScreen → top-bar Share icon (OpenInNew), ShareBanner pill ("Tap to copy or share — gift-registry-ro.web.app/r/..."), and "+ Add an item" primary button all visible. Tap Share → share intent fires. Tap pill → link copied + chooser. Tap Add an item → AddItemKey opens. Owner UX unchanged.
2. Sign in as a different user who has been invited to that registry → open RegistryDetailScreen → top-bar Share icon GONE, ShareBanner pill GONE, "+ Add an item" button GONE. Items list (e.g. SALTSJÖBADEN, Ibric) still visible. Filter chips (All / Open / Reserved / Completed) still visible. Metric counters (Items / Reserved / Given / Views) still visible. Tap an open item row → reserve flow still works (giver flow, intentionally not gated).
3. Sign-out / loading state → no flash of owner-only affordances on the brief moment between screen open and isOwner emission resolving.
4. Confirm previously-fixed quick-260507-uzv is unaffected: kebab still hidden for invitees, still visible for owners; D-13 cover-photo tap still owner-only.
</verification>

<success_criteria>
- `RegistryDetailHero.onShare` is nullable; top-bar `IconButton(OpenInNew)` only renders when non-null.
- `RegistryDetailScreen` passes `onShare = if (isOwner) onShareTap else null` and wraps `ShareBanner` + `AddItemTopCta` LazyColumn items in `if (isOwner) { ... }`.
- Zero changes to: `RegistryDetailViewModel.kt`, `AuthRepository.kt`, `ShareBanner.kt`, `firestore.rules`, `storage.rules`, any Cloud Function, any `strings.xml`.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` both green.
- Items list, filter chips, metric counters, reserve/buy actions, ConfirmPurchaseBanner, top-bar back button, kebab (quick-260507-uzv), D-13 cover-photo tap — all unchanged.
- Non-owner sees: items list + filters + counters + reserve flow ONLY. No share affordances, no add affordances, no edit affordances.
</success_criteria>

<output>
After completion, create `.planning/quick/260507-veb-hide-top-bar-share-icon-share-link-pill-/260507-veb-SUMMARY.md` summarising:
- Final files modified (2: RegistryDetailHero.kt, RegistryDetailScreen.kt)
- The pattern reused (existing `viewModel.isOwner` StateFlow from quick-260507-uzv; mirrored existing nullable-callback convention for the top-bar Share icon; standard Compose `if` inside `LazyListScope` lambda for ShareBanner + AddItemTopCta)
- Owner / non-owner observable behaviour delta for each of the three affordances
- Confirmation that no security rules, Cloud Functions, string resources, ViewModel logic, or new tests were touched (the existing `RegistryDetailViewModelIsOwnerTest` already pins the gating contract)
- Outstanding follow-up: device verification (three affordances present for owner, absent for invitee) — combine with the still-pending quick-260507-uzv Task 2 device verification
</output>
