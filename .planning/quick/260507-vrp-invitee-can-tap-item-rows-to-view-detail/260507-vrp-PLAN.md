---
phase: quick-260507-vrp
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  - app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
  - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
  - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt
  - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt
autonomous: true
requirements:
  - QUICK-260507-VRP-A: Tapping an item row on RegistryDetailScreen navigates to EditItemScreen for both owners and invitees (same destination, same EditItemKey nav route).
  - QUICK-260507-VRP-B: Per-item kebab (three-dot) overflow on RegistryItemRow is hidden for non-owners; owners retain Edit / Delete entries.
  - QUICK-260507-VRP-C: EditItemScreen renders dual-mode based on registry ownership — owner sees today's full-edit form (unchanged); invitee sees read-only field display PLUS Reserve and Mark-as-purchased actions wired to the SAME use cases the giver flow already uses on RegistryDetailScreen (reuse, no new use case, no new repository method).

must_haves:
  truths:
    - "Owner taps an item row → EditItemScreen opens in full-edit mode (form fields editable, Save button visible, Delete reachable from per-item kebab on the row). Identical to today's behaviour."
    - "Invitee (signed-in user, uid != registry.ownerId) taps an item row → EditItemScreen opens in read-only mode: form fields are non-editable, no Save button, no Delete action. Two new action buttons are visible: Reserve and Mark as purchased."
    - "Per-item kebab (⋯) on RegistryItemRow is rendered for owners only; invitees see no kebab on any row."
    - "Invitee taps Reserve on EditItemScreen → reuses ReserveItemUseCase. If guest identity exists in DataStore (or can be derived from auth profile), reservation proceeds and the affiliate URL opens via Intent.ACTION_VIEW; otherwise the existing GuestIdentitySheet appears. activeReservationId is persisted via GuestPreferencesRepository.setActiveReservationId — same pathway as RegistryDetailViewModel.performReservation."
    - "Invitee taps Mark as purchased on EditItemScreen → reuses ConfirmPurchaseUseCase with the persisted activeReservationId. On success a snackbar fires and the screen pops back. On failure a snackbar fires and the screen stays open. No new Cloud Function endpoint, no new repository method."
    - "Reserve button is enabled only when item.status == AVAILABLE (matches the existing giver-flow gating on RegistryDetailScreen — `reservation_reserve_button` already controlled by status). Mark as purchased is enabled only when item.status == RESERVED AND the active reservationId in DataStore corresponds to that reservation (mirrors ConfirmPurchaseBanner's `hasActiveReservation && activeReservationId != null` gate)."
    - "EditItemViewModel.isOwner is unit-tested with the SAME 4 cases as RegistryDetailViewModelIsOwnerTest: match→true, mismatch→false, null registry→false, null user→false. Default while loading is false (Eagerly + .catch { emit(false) }) so owner-only edit affordances never flash for an invitee."
    - "Loading-state safety: while ownership is being resolved, EditItemScreen renders neither owner-mode nor invitee-mode action buttons (existing CircularProgressIndicator covers this). Once isOwner emits, the correct mode renders."
    - "Zero changes to: firestore.rules, storage.rules, Cloud Functions (functions/src/**), GuestPreferencesRepository, ReservationRepository / Impl, ReserveItemUseCase, ConfirmPurchaseUseCase, AuthRepository, NavKeys, AppNavigation entry signature."
    - "Romanian + English strings parity preserved (no new keys; reusing reservation_reserve_button + reservation_confirm_purchase_cta + reservation_confirm_purchase_success / error + reservation_error_unavailable / generic which all exist in BOTH locales since Phase 6)."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt"
      provides: "Row-level Modifier.clickable wrapper plus owner-gated kebab via showOverflow flag."
      contains: "onTap: () -> Unit"
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      provides: "Wires onTap = { onNavigateToEditItem(item.id) } and showOverflow = isOwner at the RegistryItemRow call site (line ~278)."
      contains: "showOverflow = isOwner"
    - path: "app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt"
      provides: "isOwner StateFlow (mirrors RegistryDetailViewModel.isOwner contract); onReserveClicked / onConfirmPurchase / onGuestIdentitySubmitted methods reusing existing use cases; ReservationEvent channel for OpenRetailer / ShowGuestSheet / ShowConflictError; activeReservationId StateFlow for the Mark-as-purchased gate; observeRegistry to derive ownerId for the registry that this item belongs to."
      contains: "val isOwner: StateFlow<Boolean>"
    - path: "app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt"
      provides: "Dual-mode UI: owner mode unchanged; invitee mode disables form fields and shows Reserve + Mark-as-purchased action buttons gated by item.status; LaunchedEffect collects ReservationEvent and triggers Intent.ACTION_VIEW / GuestIdentitySheet / snackbar exactly like RegistryDetailScreen."
      contains: "if (isOwner) { /* full-edit mode */ } else { /* read-only + giver actions */ }"
    - path: "app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt"
      provides: "4 unit tests pinning EditItemViewModel.isOwner — match→true, mismatch→false, null registry→false, null user→false."
      contains: "class EditItemViewModelIsOwnerTest"
    - path: "app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt"
      provides: "Reservation success-path tests for the invitee actions: Reserve (with guest already present) → ReserveItemUseCase invoked once → OpenRetailer event emitted → activeReservationId persisted; Mark-as-purchased → ConfirmPurchaseUseCase invoked → success snackbar emitted → activeReservationId cleared."
      contains: "class EditItemViewModelReservationTest"
  key_links:
    - from: "RegistryDetailScreen RegistryItemRow call site (RegistryDetailScreen.kt:278-283)"
      to: "EditItemKey route (AppNavigation.kt:288 entry<EditItemKey>)"
      via: "onTap = { onNavigateToEditItem(item.id) } — same callback already used by the kebab Edit menu item"
      pattern: "onTap = \\{ onNavigateToEditItem"
    - from: "RegistryItemRow outer Row (RegistryItemRow.kt:77)"
      to: "Modifier.clickable invoking onTap"
      via: "Compose Modifier.clickable composed onto the existing fillMaxWidth().drawBehind {...}.padding(...) modifier chain"
      pattern: "\\.clickable\\(onClick = onTap\\)"
    - from: "RegistryItemRow right-column Box (RegistryItemRow.kt:142-196)"
      to: "showOverflow: Boolean parameter"
      via: "if (showOverflow) { Box { IconButton(...) DropdownMenu(...) } }"
      pattern: "if \\(showOverflow\\)"
    - from: "EditItemViewModel.isOwner"
      to: "AuthRepository.authState + ObserveRegistryUseCase(registryId)"
      via: "combine { reg, user -> reg != null && user != null && reg.ownerId == user.uid } .catch { emit(false) } .stateIn(Eagerly, false) — IDENTICAL to RegistryDetailViewModel.isOwner:182-189"
      pattern: "reg.ownerId == user.uid"
    - from: "EditItemViewModel.onReserveClicked()"
      to: "ReserveItemUseCase(registryId, itemId, guest, giverId)"
      via: "Mirrors RegistryDetailViewModel.performReservation:289-307 line-for-line, including: GuestPreferencesRepository.getGuestIdentity() check, GuestIdentitySheet trigger, ReservationEvent.OpenRetailer / ShowConflictError emission, activeReservationId persistence on success."
      pattern: "reserveItemUseCase\\(registryId, itemId, guest"
    - from: "EditItemViewModel.onConfirmPurchase()"
      to: "ConfirmPurchaseUseCase(reservationId)"
      via: "Mirrors RegistryDetailViewModel.onConfirmPurchase:315-328 — confirmingPurchase StateFlow, success/error snackbar via SnackbarMessage.Resource, clears activeReservationId on success."
      pattern: "confirmPurchaseUseCase\\(reservationId\\)"
    - from: "EditItemScreen LaunchedEffect (new)"
      to: "EditItemViewModel.reservationEvents Flow"
      via: "Same ReservationEvent.OpenRetailer→Intent.ACTION_VIEW / ShowGuestSheet→sheet open / ShowConflictError→snackbar pattern as RegistryDetailScreen.kt:137-155"
      pattern: "viewModel\\.reservationEvents\\.collect"
---

<objective>
Wire item-row tap on RegistryDetailScreen to navigate to EditItemScreen for both owners and invitees, gate the per-item kebab on ownership, AND adapt EditItemScreen to be dual-mode based on the registry's ownership.

Owner: full edit mode unchanged (form fields editable, Save button, Delete via kebab).
Invitee: read-only field display PLUS Reserve + Mark-as-purchased actions reusing the SAME use cases the existing giver flow uses on RegistryDetailScreen. NO new reserve / purchase code paths — pure plumbing.

Purpose: Today, an invitee tapping an item row on RegistryDetailScreen does nothing — there's no clickable affordance. The kebab they see exposes Edit / Delete which they cannot perform (server rejects). After this change, invitees can tap a row to see item detail AND perform the two giver actions (Reserve, Mark as purchased) that the Android app currently lacks (those actions exist only on the public web giver flow today and via the email re-reserve deep link).

Output: A single tap entry-point per role:
- Owner tap on row → EditItemScreen full-edit mode (unchanged from today).
- Invitee tap on row → EditItemScreen read-only mode + Reserve + Mark-as-purchased buttons gated by item.status.
- Per-item kebab visible only to owners.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@.planning/STATE.md
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
@app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
@app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/GuestIdentitySheet.kt
@app/src/main/java/com/giftregistry/ui/registry/detail/ConfirmPurchaseBanner.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt

<interfaces>
<!-- Key contracts the executor needs. Do NOT re-explore — these are the exact symbols in play. -->

`EditItemKey` nav route already carries `registryId` AND `itemId` — no nav signature change needed:
```kotlin
// AppNavKeys.kt:24
@Serializable data class EditItemKey(val registryId: String, val itemId: String)

// AppNavigation.kt:288 — entry already wires both args:
entry<EditItemKey> { key ->
    EditItemScreen(
        registryId = key.registryId,
        itemId = key.itemId,
        onBack = { backStack.removeLast() }
    )
}

// RegistryDetailScreen.kt:67 — already-existing nav callback:
onNavigateToEditItem: (String) -> Unit  // captured at call site as { itemId -> backStack.add(EditItemKey(key.registryId, itemId)) }
```

`RegistryItemRow` current signature (RegistryItemRow.kt:60-66):
```kotlin
@Composable
internal fun RegistryItemRow(
    item: Item,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
)
```
ADD: `onTap: () -> Unit` (required) AND `showOverflow: Boolean = true`. Wrap outer Row in `Modifier.clickable(onClick = onTap)`. Wrap right-column kebab Box (lines 142-196) in `if (showOverflow) { ... }`.

Already-collected `isOwner` on RegistryDetailScreen (line 86) — reuse for the call-site `showOverflow = isOwner` flag.

`RegistryDetailViewModel.isOwner` canonical pattern (RegistryDetailViewModel.kt:182-189) — replicate verbatim in EditItemViewModel:
```kotlin
val isOwner: StateFlow<Boolean> = combine(
    registry,                    // here in EditItemVM: observeRegistryUseCase(registryId).catch{ emit(null) }.stateIn(Eagerly, null)
    authRepository.authState,    // Flow<User?>
) { reg, user ->
    reg != null && user != null && reg.ownerId == user.uid
}
    .catch { emit(false) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```

`ReserveItemUseCase` signature (already exists, reuse as-is):
```kotlin
suspend operator fun invoke(
    registryId: String,
    itemId: String,
    giver: GuestUser,
    giverId: String? = null,
): Result<ReservationResult>
// ReservationResult = (reservationId: String, affiliateUrl: String)
```

`ConfirmPurchaseUseCase` signature (already exists, reuse as-is):
```kotlin
suspend operator fun invoke(reservationId: String): Result<Unit>
```

`GuestPreferencesRepository` surface (existing, reuse):
- `suspend fun getGuestIdentity(): GuestUser?`
- `suspend fun saveGuestIdentity(guest: GuestUser)`
- `fun observeGuestIdentity(): Flow<GuestUser?>`
- `suspend fun setActiveReservationId(id: String?)`
- `fun observeActiveReservationId(): Flow<String?>`

`User` (Firebase wrapper):
```kotlin
data class User(val uid: String, val email: String?, val displayName: String?, val isAnonymous: Boolean)
```

`GuestUser` (reservation payload — required non-null on ReserveItemUseCase):
```kotlin
data class GuestUser(val firstName: String, val lastName: String, val email: String)
```
**Important:** `User.displayName` and `User.email` may be null for some Firebase auth providers. The reserve flow MUST handle that case by surfacing the existing `GuestIdentitySheet` exactly like RegistryDetailViewModel.performReservation does today. Do NOT silently substitute placeholders. The simplest approach (recommended): always go through the existing `guestPreferencesRepository.getGuestIdentity()` check first, regardless of whether the caller is signed-in, and trigger the sheet on null. This matches the giver-flow today and keeps a single code path. The signed-in invitee fills the sheet once; the GuestUser is then persisted in DataStore for subsequent reserves in the same session.

`Item.status` (`ItemStatus` enum): `AVAILABLE`, `RESERVED`, `PURCHASED`. Existing reserve gating on RegistryDetailScreen disables the action when status != AVAILABLE; mark-as-purchased is shown only when status == RESERVED AND `activeReservationId != null`.

`ReservationEvent` sealed interface (currently nested inside `RegistryDetailViewModel` — RegistryDetailViewModel.kt:232-236). EditItemViewModel needs the SAME three event types. Two options:
1. **Recommended (smaller diff):** Re-declare a private nested `sealed interface ReservationEvent { ... }` inside `EditItemViewModel` with the same three subtypes. The two VMs do not share state; duplicating the 4-line declaration is cheaper than promoting it to a top-level type and updating imports across the codebase. Document the duplication with a comment pointing at RegistryDetailViewModel.ReservationEvent so future readers know the contracts must stay aligned.
2. (NOT recommended for this quick task) Promote `ReservationEvent` to a top-level domain type and have both VMs import it. This touches RegistryDetailViewModel.kt + RegistryDetailScreen.kt LaunchedEffect import + any test that names `RegistryDetailViewModel.ReservationEvent.*`.

Pick option 1.

`Intent.ACTION_VIEW(affiliateUrl.toUri())` snippet for OpenRetailer event (RegistryDetailScreen.kt:140-145). EditItemScreen LaunchedEffect collects `viewModel.reservationEvents` and dispatches the same Intent — duplicate the 5-line block, do not factor out.

`SnackbarHost` + `SnackbarHostState` pattern. EditItemScreen currently uses `Scaffold` (line 73-86) without a snackbarHost slot. Add `snackbarHost = { SnackbarHost(snackbarHostState) }` to the existing Scaffold; reuse `R.string.reservation_confirm_purchase_success` / `R.string.reservation_confirm_purchase_error` / `R.string.reservation_error_unavailable` / `R.string.reservation_error_generic` (all already in `values/` and `values-ro/`).

`hiltViewModelWithNavArgs` factory pattern (already used in EditItemScreen.kt:51-55) — supports passing nav args through SavedStateHandle. EditItemViewModel will need three new injected dependencies: `AuthRepository`, `ObserveRegistryUseCase`, `ReserveItemUseCase`, `ConfirmPurchaseUseCase`, `GuestPreferencesRepository`. Hilt handles this automatically via @Inject constructor — no module changes needed (these are all already provided for RegistryDetailViewModel).

Test pattern (mirror `RegistryDetailViewModelIsOwnerTest.kt` verbatim — same package convention, MockK relaxed, MainDispatcherRule, advanceUntilIdle). The shared `vm()` helper wires every constructor param.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Wire item-row tap → EditItemScreen + gate per-item kebab on ownership (Change A + Change B from CONTEXT)</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  </files>
  <action>
    Two coordinated edits — purely additive at the call site.

    **Edit A — `RegistryItemRow.kt`:** Add row-level tap callback + nullable-ish overflow gate.

    1. Add two new parameters to `RegistryItemRow` (alongside the existing ones at lines 60-66):
       ```kotlin
       @Composable
       internal fun RegistryItemRow(
           item: Item,
           isLast: Boolean,
           onTap: () -> Unit,
           onEdit: () -> Unit,
           onDelete: () -> Unit,
           showOverflow: Boolean = true,
           modifier: Modifier = Modifier,
       )
       ```
       Note `onTap` is REQUIRED (no default) so the screen call site is forced to wire navigation. `showOverflow` defaults to `true` to preserve any other callers' current behaviour (there are none today, but the default keeps the contract permissive).

    2. Add `Modifier.clickable(onClick = onTap)` to the outer Row's modifier chain (line 77-90). Place it AFTER `.fillMaxWidth()` and BEFORE `.drawBehind { ... }` so the divider line still draws on top of the ripple bounds. Final chain:
       ```kotlin
       Row(
           modifier = modifier
               .fillMaxWidth()
               .clickable(onClick = onTap)
               .drawBehind { ... }
               .padding(horizontal = spacing.edge, vertical = spacing.gap12),
           verticalAlignment = Alignment.Top,
       ) { ... }
       ```
       Add the import `import androidx.compose.foundation.clickable` (already imported at top of file? confirm — if not, add it).

    3. Wrap the entire right-column kebab Box (lines 142-196 — the `Box { IconButton(...) DropdownMenu(...) }`) in `if (showOverflow) { ... }`. The Spacer above (line 141) and the StatusChip (line 140) stay outside the gate — only the IconButton + DropdownMenu disappear for non-owners. Do NOT touch StatusChip; non-owners still need to see status.

    **Edit B — `RegistryDetailScreen.kt`:** Wire the new params at the call site.

    At lines 274-284 (the `itemsIndexed` block invoking `RegistryItemRow`), pass:
    ```kotlin
    itemsIndexed(
        items = filteredItems,
        key = { _, item -> item.id },
    ) { idx, item ->
        RegistryItemRow(
            item = item,
            isLast = idx == filteredItems.lastIndex,
            onTap = { onNavigateToEditItem(item.id) },
            onEdit = { onNavigateToEditItem(item.id) },
            onDelete = { itemToDelete = item },
            showOverflow = isOwner,
        )
    }
    ```
    `onTap` and `onEdit` deliberately call the same callback — that's the spec ("This applies to BOTH owner and invitee — same destination"). The kebab Edit entry remains for owners as a redundant entry point; tapping the row is the primary entry point for everyone.

    `isOwner` is already collected at line 86 (`val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()`). No new VM work for this task.

    Constraints honoured:
    - No changes to `RegistryDetailViewModel.kt`, `AuthRepository`, `firestore.rules`, `storage.rules`, Cloud Functions, strings.xml, or `AppNavigation.kt`.
    - Reuses the existing `onNavigateToEditItem` callback already wired through `AppNavigation.kt:224` — same EditItemKey route the kebab Edit entry uses today.
    - Loading-state safe: `isOwner` defaults to `false` (Eagerly initial) so during the brief load before the first emission, the kebab is hidden — same safe-default convention as quick-260507-uzv. No flash of owner-only items.
    - Rest-of-row content (thumbnail, title, price, status chip) untouched. The clickable Modifier wraps the entire row; tapping any non-kebab area of the row triggers `onTap`; tapping the kebab still consumes the click via the IconButton (Compose's hit-testing prefers the inner clickable).
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest -q</automated>
  </verify>
  <done>
    - `:app:compileDebugKotlin` green (new required `onTap` parameter forces the call site to compile against the new signature; no other call sites of RegistryItemRow exist in the codebase, so this is safe).
    - All existing `:app:testDebugUnitTest` pass; no regression on `RegistryDetailViewModelIsOwnerTest`, `HeroToolbarAlphaTest`, or any other detail-screen test.
    - At runtime: tapping anywhere on a non-kebab area of an item row navigates to EditItemScreen via the existing `onNavigateToEditItem` callback; the per-item kebab is rendered for `isOwner = true` and absent for `isOwner = false`.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add EditItemViewModel.isOwner + read-only EditItemScreen for invitees (Change C — read-only half, no reserve/purchase yet)</name>
  <files>
    app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
    app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
    app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt
  </files>
  <behavior>
    - Test 1: `isOwner_isTrue_whenRegistryOwnerIdMatchesAuthUid` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authState returns flowOf(User(uid="user-1")), expect `isOwner.value == true` after advanceUntilIdle.
    - Test 2: `isOwner_isFalse_whenRegistryOwnerIdDiffersFromAuthUid` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authState returns flowOf(User(uid="user-2")), expect `isOwner.value == false`. (Invitee case — the bug being fixed.)
    - Test 3: `isOwner_isFalse_whenRegistryIsNull` — observeRegistry emits null, authState returns flowOf(User(uid="user-1")), expect `isOwner.value == false`. (No flash of owner-only edit affordances during load.)
    - Test 4: `isOwner_isFalse_whenAuthStateIsNull` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authState returns flowOf(null), expect `isOwner.value == false`. (Signed-out edge case.)
  </behavior>
  <action>
    Three coordinated edits.

    **Edit A — `EditItemViewModel.kt`:** Inject `AuthRepository` + `ObserveRegistryUseCase` and expose `isOwner: StateFlow<Boolean>`.

    1. Add new constructor params (after the existing `fetchOgMetadata` at line 23):
       ```kotlin
       private val authRepository: AuthRepository,
       private val observeRegistry: ObserveRegistryUseCase,
       ```

    2. Inside the class body (place it AFTER the form-field MutableStateFlow declarations to keep the file's reading order intact), add:
       ```kotlin
       /**
        * quick-260507-vrp — true when the signed-in user owns the registry that
        * this item belongs to. Drives the dual-mode UI on EditItemScreen: owner
        * mode = full edit (Save / Delete reachable from per-item kebab on the
        * Detail row); invitee mode = read-only fields + Reserve / Mark-as-purchased
        * actions. Mirrors RegistryDetailViewModel.isOwner (RegistryDetailViewModel.kt:182-189)
        * line-for-line so both surfaces use the same ownership predicate as the
        * server (functions/src/registry/inviteToRegistry.ts:50).
        */
       val registry: StateFlow<Registry?> = observeRegistry(registryId)
           .catch { emit(null) }
           .stateIn(viewModelScope, SharingStarted.Eagerly, null)

       val isOwner: StateFlow<Boolean> = combine(
           registry,
           authRepository.authState,
       ) { reg, user ->
           reg != null && user != null && reg.ownerId == user.uid
       }
           .catch { emit(false) }
           .stateIn(viewModelScope, SharingStarted.Eagerly, false)
       ```
       Add the missing imports at the top of the file:
       ```kotlin
       import com.giftregistry.domain.auth.AuthRepository
       import com.giftregistry.domain.model.Registry
       import com.giftregistry.domain.usecase.ObserveRegistryUseCase
       import kotlinx.coroutines.flow.SharingStarted
       import kotlinx.coroutines.flow.catch
       import kotlinx.coroutines.flow.combine
       import kotlinx.coroutines.flow.stateIn
       ```
       Do NOT remove or modify any existing fields, init block, or methods. The existing `init { ... }` that loads the item from `observeItems(registryId).firstOrNull()` stays as-is — `_isLoading` continues to gate the form rendering.

    **Edit B — `EditItemScreen.kt`:** Branch the form on `isOwner` to render a read-only invitee view. NO reserve/purchase actions in this task — those land in Task 3 to keep the diff reviewable.

    1. Collect the new state at the top of the composable (after line 67):
       ```kotlin
       val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
       ```

    2. Inside the existing `Column` (lines 100-226), wrap the form fields and Save button in `if (isOwner) { ... } else { ... }`. The owner branch is THE ENTIRE existing form body verbatim — no behavioural change. The invitee branch renders the same fields with `enabled = false` on each `OutlinedTextField` AND OMITS the Save button. Skeleton:
       ```kotlin
       if (isOwner) {
           // EXISTING owner-mode body — verbatim copy of lines 108-225 as-is.
           // Do not refactor; keeping it as a literal copy is the smallest diff.
       } else {
           // Invitee read-only mode (NO Save, NO Delete, NO URL "Fetch" button).
           Spacer(modifier = Modifier.height(8.dp))
           OutlinedTextField(
               value = title,
               onValueChange = {},
               label = { Text(stringResource(R.string.item_title_label)) },
               modifier = Modifier.fillMaxWidth(),
               singleLine = true,
               enabled = false,
           )
           OutlinedTextField(
               value = price,
               onValueChange = {},
               label = { Text(stringResource(R.string.item_price_label)) },
               modifier = Modifier.fillMaxWidth(),
               singleLine = true,
               enabled = false,
           )
           if (imageUrl.isNotBlank()) {
               // SAME AsyncImage block as owner mode — readable preview is fine.
               val previewFallback = rememberVectorPainter(Icons.Default.Image)
               AsyncImage(
                   model = imageUrl,
                   contentDescription = stringResource(R.string.item_image_content_desc),
                   modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)),
                   contentScale = ContentScale.Fit,
                   placeholder = previewFallback,
                   error = previewFallback,
                   fallback = previewFallback,
               )
           }
           OutlinedTextField(
               value = imageUrl,
               onValueChange = {},
               label = { Text(stringResource(R.string.item_image_label)) },
               modifier = Modifier.fillMaxWidth(),
               singleLine = true,
               enabled = false,
           )
           OutlinedTextField(
               value = notes,
               onValueChange = {},
               label = { Text(stringResource(R.string.item_notes_label)) },
               modifier = Modifier.fillMaxWidth(),
               minLines = 2,
               maxLines = 4,
               enabled = false,
           )
           // URL field also rendered read-only (no Fetch button, no row arrangement).
           OutlinedTextField(
               value = url,
               onValueChange = {},
               label = { Text(stringResource(R.string.item_add_url_label)) },
               modifier = Modifier.fillMaxWidth(),
               singleLine = true,
               enabled = false,
           )
           Spacer(modifier = Modifier.height(16.dp))
           // Task 3 will append Reserve + Mark-as-purchased action buttons here.
       }
       ```
       Skip the `isFetchingOg` indicator, the `ogFetchFailed` error text, and the `error` text in invitee mode — those are owner-side concerns (only `onSave` and `onFetchMetadata` produce them, and invitees call neither).

    **Edit C — `EditItemViewModelIsOwnerTest.kt`:** Create the file mirroring `RegistryDetailViewModelIsOwnerTest.kt` exactly (same fixtures, same MockK setup, same MainDispatcherRule + ExperimentalCoroutinesApi).

    Skeleton:
    ```kotlin
    package com.giftregistry.ui.item.edit

    import androidx.lifecycle.SavedStateHandle
    import com.giftregistry.MainDispatcherRule
    import com.giftregistry.domain.auth.AuthRepository
    import com.giftregistry.domain.model.Registry
    import com.giftregistry.domain.model.User
    import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
    import com.giftregistry.domain.usecase.ObserveItemsUseCase
    import com.giftregistry.domain.usecase.ObserveRegistryUseCase
    import com.giftregistry.domain.usecase.UpdateItemUseCase
    import io.mockk.every
    import io.mockk.mockk
    import kotlinx.coroutines.ExperimentalCoroutinesApi
    import kotlinx.coroutines.flow.flowOf
    import kotlinx.coroutines.test.advanceUntilIdle
    import kotlinx.coroutines.test.runTest
    import org.junit.Assert.assertFalse
    import org.junit.Assert.assertTrue
    import org.junit.Rule
    import org.junit.Test

    @OptIn(ExperimentalCoroutinesApi::class)
    class EditItemViewModelIsOwnerTest {
        @get:Rule val mainDispatcherRule = MainDispatcherRule()

        private val observeRegistry: ObserveRegistryUseCase = mockk(relaxed = true)
        private val observeItems: ObserveItemsUseCase = mockk(relaxed = true)
        private val authRepository: AuthRepository = mockk(relaxed = true)

        private fun fakeRegistry(ownerId: String) = Registry(
            id = "reg-1",
            ownerId = ownerId,
            title = "Test",
            occasion = "Birthday",
            // Fill any remaining required Registry fields with minimal valid defaults
            // — consult the Registry data class. Mirror the helper from RegistryDetailViewModelIsOwnerTest:51-56.
        )

        private fun fakeUser(uid: String) = User(uid = uid, email = null, displayName = null, isAnonymous = false)

        private fun vm(): EditItemViewModel {
            val ssh = SavedStateHandle(mapOf("registryId" to "reg-1", "itemId" to "item-1"))
            return EditItemViewModel(
                savedStateHandle = ssh,
                updateItem = mockk<UpdateItemUseCase>(relaxed = true),
                observeItems = observeItems,
                fetchOgMetadata = mockk<FetchOgMetadataUseCase>(relaxed = true),
                authRepository = authRepository,
                observeRegistry = observeRegistry,
            )
        }

        @Test
        fun `isOwner is true when registry ownerId matches auth uid`() = runTest {
            every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
            every { observeItems("reg-1") } returns flowOf(emptyList())
            every { authRepository.authState } returns flowOf(fakeUser(uid = "user-1"))
            val viewModel = vm()
            advanceUntilIdle()
            assertTrue(viewModel.isOwner.value)
        }

        // + 3 more tests covering: ownerId mismatch → false, null registry → false, null authState → false.
    }
    ```

    IMPORTANT for the executor:
    - Inspect the `Registry` data class for required fields (the `RegistryDetailViewModelIsOwnerTest` helper at line 51-56 gives the minimum set: `id, ownerId, title, occasion`). Mirror exactly.
    - The VM's existing init block calls `observeItems(registryId).firstOrNull()` which we mock with `flowOf(emptyList())` so `firstOrNull()` returns an empty list, the item lookup yields null, the form fields stay empty, and `_isLoading` flips false. None of this affects the `isOwner` contract under test.
    - Use `SharingStarted.Eagerly` so `advanceUntilIdle()` is sufficient — no `turbine.test {}` needed.
    - Do NOT create instrumented (Robolectric) tests — pure JVM unit tests only, matching the project's existing pattern.
  </behavior>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.giftregistry.ui.item.edit.EditItemViewModelIsOwnerTest" -q</automated>
  </verify>
  <done>
    - All 4 tests in `EditItemViewModelIsOwnerTest` GREEN.
    - `:app:compileDebugKotlin` green; injecting two new constructor params into EditItemViewModel does not break Hilt graph (both deps are already provided for RegistryDetailViewModel).
    - `:app:testDebugUnitTest` green for the full suite (no regression).
    - Manually: signing in as the registry owner and tapping a row → owner sees today's full-edit form with Save button. Signing in as an invitee and tapping a row → invitee sees the same fields read-only (`enabled = false` greys them out via Material3 default behaviour) with NO Save button. Reserve / Mark-as-purchased buttons NOT yet wired in this task — they land in Task 3.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Wire Reserve + Mark-as-purchased actions on EditItemScreen invitee mode (Change C — giver actions half)</name>
  <files>
    app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
    app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
    app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt
  </files>
  <behavior>
    - Test 1 (`reserve_success_emitsOpenRetailerAndPersistsActiveReservationId`): Given guestPreferencesRepository.getGuestIdentity() returns a non-null GuestUser AND reserveItemUseCase returns Result.success(ReservationResult(reservationId="res-1", affiliateUrl="https://aff/...")), when onReserveClicked("item-1") is invoked, then: (a) reserveItemUseCase is called exactly once with (registryId, "item-1", guest, giverId=user.uid OR null), (b) guestPreferencesRepository.setActiveReservationId("res-1") is called exactly once, (c) reservationEvents emits a single ReservationEvent.OpenRetailer("https://aff/..."). Use turbine.test on the Flow OR collect into a CompletableDeferred — match the existing test pattern in `RegistryDetailViewModelConfirmPurchaseTest` if present.
    - Test 2 (`reserve_noGuestIdentity_emitsShowGuestSheet`): Given guestPreferencesRepository.getGuestIdentity() returns null, when onReserveClicked("item-1") is invoked, then reservationEvents emits ReservationEvent.ShowGuestSheet AND reserveItemUseCase is NOT called yet (verify { reserveItemUseCase wasNot Called }).
    - Test 3 (`reserve_failure_emitsShowConflictError`): Given guest is non-null AND reserveItemUseCase returns Result.failure(Exception("ITEM_UNAVAILABLE")), when onReserveClicked("item-1"), then reservationEvents emits ReservationEvent.ShowConflictError(code="ITEM_UNAVAILABLE") AND setActiveReservationId is NOT called.
    - Test 4 (`confirmPurchase_success_emitsSnackbarSuccessAndClearsActiveReservationId`): Given confirmPurchaseUseCase returns Result.success(Unit) AND activeReservationId initial value is "res-1", when onConfirmPurchase("res-1") is invoked, then: (a) confirmPurchaseUseCase is called once with "res-1", (b) snackbarMessages emits Resource(R.string.reservation_confirm_purchase_success), (c) guestPreferencesRepository.setActiveReservationId(null) is called once.
    - Test 5 (`confirmPurchase_failure_emitsSnackbarError`): Given confirmPurchaseUseCase returns Result.failure(...), when onConfirmPurchase("res-1"), then snackbarMessages emits Resource(R.string.reservation_confirm_purchase_error) AND setActiveReservationId(null) is NOT called.
  </behavior>
  <action>
    Three coordinated edits — replicate the giver-flow plumbing from RegistryDetailViewModel into EditItemViewModel and wire the buttons in EditItemScreen.

    **Edit A — `EditItemViewModel.kt`:** Add reservation orchestration plumbing.

    1. Inject the remaining dependencies:
       ```kotlin
       private val reserveItemUseCase: ReserveItemUseCase,
       private val confirmPurchaseUseCase: ConfirmPurchaseUseCase,
       private val guestPreferencesRepository: GuestPreferencesRepository,
       ```

    2. Declare the SAME nested ReservationEvent contract as RegistryDetailViewModel (RegistryDetailViewModel.kt:232-236) — see <interfaces> note above. Add a `// quick-260507-vrp — mirrors RegistryDetailViewModel.ReservationEvent; both VMs feed identical UI side effects` comment for future readers:
       ```kotlin
       sealed interface ReservationEvent {
           data class OpenRetailer(val affiliateUrl: String) : ReservationEvent
           data object ShowGuestSheet : ReservationEvent
           data class ShowConflictError(val code: String) : ReservationEvent
       }

       private val _reservationEvents = Channel<ReservationEvent>(Channel.BUFFERED)
       val reservationEvents: Flow<ReservationEvent> = _reservationEvents.receiveAsFlow()

       private val _isReserving = MutableStateFlow(false)
       val isReserving: StateFlow<Boolean> = _isReserving.asStateFlow()

       private var pendingReserveItemId: String? = null
       ```

    3. Add the `activeReservationId` StateFlow + `_snackbarMessages` SharedFlow + `_confirmingPurchase` StateFlow (mirror RegistryDetailViewModel.kt:74-79 + 171-174). Use SnackbarMessage.Resource only — no Push subtype needed here since EditItemScreen does not collect FCM pushes:
       ```kotlin
       private val _snackbarMessages = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 1)
       val snackbarMessages: SharedFlow<Int> = _snackbarMessages.asSharedFlow()

       private val _confirmingPurchase = MutableStateFlow(false)
       val confirmingPurchase: StateFlow<Boolean> = _confirmingPurchase.asStateFlow()

       val activeReservationId: StateFlow<String?> = guestPreferencesRepository
           .observeActiveReservationId()
           .catch { emit(null) }
           .stateIn(viewModelScope, SharingStarted.Eagerly, null)
       ```
       Note: simpler payload (`Int` resId) instead of the `SnackbarMessage` sealed interface — EditItemScreen only handles success/error snackbars, no FCM push. Keeps the diff small.

    4. Add `onReserveClicked(itemId)`, `onGuestIdentitySubmitted(guest)`, `performReservation(itemId, guest)`, `onConfirmPurchase(reservationId)` methods. **Copy verbatim from `RegistryDetailViewModel.kt:267-307` and `:315-328`**, with two adjustments:
       - `_snackbarMessages.emit(...)` takes the resId Int directly, not `SnackbarMessage.Resource(resId)`.
       - On reserve success, also pass `giverId = authRepository.currentUser?.uid` so the server-side reservation record has the signed-in user's UID for analytics (the public web giver-flow today passes null because anonymous; on Android invitee path we have a UID, so pass it). Matches the existing optional-param contract on `ReserveItemUseCase`.

    Add the imports:
    ```kotlin
    import com.giftregistry.R
    import com.giftregistry.domain.model.GuestUser
    import com.giftregistry.domain.preferences.GuestPreferencesRepository
    import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
    import com.giftregistry.domain.usecase.ReserveItemUseCase
    import kotlinx.coroutines.channels.Channel
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.MutableSharedFlow
    import kotlinx.coroutines.flow.SharedFlow
    import kotlinx.coroutines.flow.asSharedFlow
    import kotlinx.coroutines.flow.receiveAsFlow
    ```

    **Edit B — `EditItemScreen.kt`:** Wire the action buttons + Intent.ACTION_VIEW + GuestIdentitySheet + SnackbarHost.

    1. Add `SnackbarHost` to the existing Scaffold's `snackbarHost` slot. Collect `snackbarHostState`:
       ```kotlin
       val snackbarHostState = remember { SnackbarHostState() }
       val context = LocalContext.current
       val scope = rememberCoroutineScope()
       var showGuestSheet by remember { mutableStateOf(false) }

       val unavailableMsg = stringResource(R.string.reservation_error_unavailable)
       val genericErrorMsg = stringResource(R.string.reservation_error_generic)
       ```
       Update the Scaffold call to include `snackbarHost = { SnackbarHost(snackbarHostState) }`.

    2. Collect new VM state + reservation events. Add LaunchedEffects (mirror RegistryDetailScreen.kt:137-183):
       ```kotlin
       val activeReservationId by viewModel.activeReservationId.collectAsStateWithLifecycle()
       val isReserving by viewModel.isReserving.collectAsStateWithLifecycle()
       val confirmingPurchase by viewModel.confirmingPurchase.collectAsStateWithLifecycle()
       val item by viewModel.itemFlow.collectAsStateWithLifecycle() // ADD: see Edit A note below

       LaunchedEffect(Unit) {
           viewModel.reservationEvents.collect { event ->
               when (event) {
                   is EditItemViewModel.ReservationEvent.OpenRetailer -> {
                       runCatching {
                           context.startActivity(Intent(Intent.ACTION_VIEW, event.affiliateUrl.toUri()))
                       }
                   }
                   EditItemViewModel.ReservationEvent.ShowGuestSheet -> {
                       showGuestSheet = true
                   }
                   is EditItemViewModel.ReservationEvent.ShowConflictError -> {
                       val msg = if (event.code == "ITEM_UNAVAILABLE") unavailableMsg else genericErrorMsg
                       snackbarHostState.showSnackbar(msg)
                   }
               }
           }
       }

       LaunchedEffect(Unit) {
           viewModel.snackbarMessages.collect { resId ->
               snackbarHostState.showSnackbar(context.getString(resId))
               // Pop back on confirm-purchase success — match the giver flow's UX pattern.
               if (resId == R.string.reservation_confirm_purchase_success) onBack()
           }
       }
       ```

       **Edit A note — itemFlow:** EditItemScreen needs to know `item.status` to gate the Reserve / Mark-as-purchased buttons. Today the VM only stores fields (title/url/etc.) as MutableStateFlow form-edit holders — there's no `Item` exposed. Add a small derivation in EditItemViewModel:
       ```kotlin
       val itemFlow: StateFlow<Item?> = observeItems(registryId)
           .map { items -> items.firstOrNull { it.id == itemId } }
           .catch { emit(null) }
           .stateIn(viewModelScope, SharingStarted.Eagerly, null)
       ```
       Add imports: `import com.giftregistry.domain.model.Item` (already imported), `import kotlinx.coroutines.flow.map`. The existing init block can keep using `firstOrNull()` for one-shot form population — itemFlow is a separate observation channel for status gating (which needs to update reactively when the Cloud Function flips status RESERVED → PURCHASED).

    3. Inside the `else` (invitee) branch from Task 2's Edit B, append the action buttons AFTER the `Spacer(Modifier.height(16.dp))`:
       ```kotlin
       val currentItem = item
       val canReserve = currentItem?.status == ItemStatus.AVAILABLE && !isReserving
       val canConfirmPurchase = currentItem?.status == ItemStatus.RESERVED &&
           activeReservationId != null && !confirmingPurchase

       Button(
           onClick = { viewModel.onReserveClicked(itemId) },
           enabled = canReserve,
           modifier = Modifier.fillMaxWidth(),
       ) {
           if (isReserving) {
               CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
           }
           Text(stringResource(R.string.reservation_reserve_button))
       }

       Spacer(modifier = Modifier.height(8.dp))

       Button(
           onClick = {
               val rid = activeReservationId
               if (rid != null) viewModel.onConfirmPurchase(rid)
           },
           enabled = canConfirmPurchase,
           modifier = Modifier.fillMaxWidth(),
       ) {
           if (confirmingPurchase) {
               CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
           }
           Text(stringResource(R.string.reservation_confirm_purchase_cta))
       }
       ```
       Reuses existing string resources — `reservation_reserve_button` and `reservation_confirm_purchase_cta` both exist in `values/strings.xml` (lines 129 + 143) AND `values-ro/strings.xml` since Phase 6. NO new strings.

    4. Render the `GuestIdentitySheet` when `showGuestSheet` is true (mirror RegistryDetailScreen.kt:384-393):
       ```kotlin
       if (showGuestSheet) {
           GuestIdentitySheet(
               initial = null,
               onDismiss = { showGuestSheet = false },
               onSubmit = { guest ->
                   showGuestSheet = false
                   viewModel.onGuestIdentitySubmitted(guest)
               },
           )
       }
       ```
       Import `com.giftregistry.ui.registry.detail.GuestIdentitySheet`.

    **Edit C — `EditItemViewModelReservationTest.kt`:** Mirror the test patterns described in <behavior>. Use `app.cash.turbine.test {}` if already used elsewhere in the project's tests, or a `CompletableDeferred<ReservationEvent>` pattern if not — inspect existing `RegistryDetailViewModelConfirmPurchaseTest.kt` for the exact convention.

    Constraints honoured:
    - **No new use cases, no new repository methods.** ReserveItemUseCase, ConfirmPurchaseUseCase, GuestPreferencesRepository methods all already exist and are reused as-is.
    - **No new strings.** All four labels (`reservation_reserve_button`, `reservation_confirm_purchase_cta`, success/error snackbars) are pre-existing in both EN + RO since Phase 6.
    - **No nav route changes.** EditItemKey continues to carry `(registryId, itemId)`; the screen self-derives ownership.
    - **No security rule / Cloud Function changes.** The server already accepts createReservation + confirmPurchase from authenticated callers (confirmPurchase has no auth guard per Phase 6 D-decision documented in STATE.md).
    - **Owner mode unchanged.** The `if (isOwner)` branch in EditItemScreen wraps the entire existing form body verbatim — owner sees zero behavioural change vs. today.
    - **Loading-state safe.** `isOwner` defaults false → loading state shows the invitee branch briefly. The invitee branch's reserve/confirm buttons are gated by `currentItem?.status` which is null while loading, so `canReserve = false` and `canConfirmPurchase = false` — both buttons disabled until the first item snapshot arrives. No reserves can fire on stale state.
    - **Hilt graph.** All five new EditItemViewModel deps (AuthRepository, ObserveRegistryUseCase, ReserveItemUseCase, ConfirmPurchaseUseCase, GuestPreferencesRepository) are already provided in the existing modules used by RegistryDetailViewModel — no new @Provides functions needed.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.giftregistry.ui.item.edit.*" -q</automated>
  </verify>
  <done>
    - All 5 tests in `EditItemViewModelReservationTest` GREEN, plus the 4 from `EditItemViewModelIsOwnerTest` still GREEN.
    - Full `:app:testDebugUnitTest` GREEN — no regression on RegistryDetailViewModel tests, ConfirmPurchaseBanner tests, or anything else.
    - `:app:compileDebugKotlin` GREEN.
    - At runtime: invitee taps a row → opens EditItemScreen in read-only mode → sees Reserve button (enabled when item.status == AVAILABLE) → tap → guest sheet opens (or skipped if guest already saved in DataStore) → after sheet submit, affiliate URL opens via Intent.ACTION_VIEW → activeReservationId is persisted → invitee returns to EditItemScreen → Mark-as-purchased button now enabled (item.status == RESERVED + activeReservationId != null) → tap → success snackbar fires → screen pops back. Owner mode untouched.
  </done>
</task>

</tasks>

<verification>
**Automated** (must pass before marking the quick task complete):

```bash
./gradlew :app:compileDebugKotlin -q
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.item.edit.*" -q
./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.*" -q
./gradlew :app:testDebugUnitTest -q
```

**String parity sanity check:**
```bash
# Confirm we are NOT introducing any new <string name="..."> entries.
git diff app/src/main/res/values/strings.xml app/src/main/res/values-ro/strings.xml
```
Expected: empty diff.

**Hilt graph sanity check:**
```bash
./gradlew :app:assembleDebug -q
```
A clean Hilt-graph compile confirms the new EditItemViewModel constructor params resolve through the existing DI modules.

**Manual device verification** (deferred to user; not blocking):
1. Sign in as a Registry owner. Open a registry. Tap an item row → EditItemScreen opens in full-edit mode (form fields editable, Save button visible). Tap the per-item kebab → Edit / Delete menu items still appear. Tap Edit → also opens EditItemScreen in full-edit mode (redundant entry point, expected). Behaviour identical to before this change.
2. Sign out. Sign in as a different user who has been invited to the same registry (or use a registry the user does not own). Open the registry. Confirm: per-item kebab is GONE on every row. Tap any item row → EditItemScreen opens in read-only mode (fields greyed, no Save button). Reserve button is enabled when status == OPEN/AVAILABLE; Mark-as-purchased is disabled (no active reservation yet). Tap Reserve → guest sheet appears (or skips if guest identity already cached) → fill name/email, submit → affiliate URL opens in browser. Return to the app. EditItemScreen now shows status == RESERVED → Reserve disabled, Mark-as-purchased ENABLED. Tap Mark-as-purchased → success snackbar fires, screen pops back. Refresh / reopen → status now == PURCHASED → both buttons disabled.
3. Edge: invitee tries to reserve an already-RESERVED item via direct nav → Reserve button is disabled by `canReserve = currentItem?.status == AVAILABLE` gate. Server-side conflict (race condition) → ShowConflictError snackbar fires.
</verification>

<success_criteria>
- Owner experience on RegistryDetailScreen + EditItemScreen IDENTICAL to today (full edit flow, kebab still shows Edit / Delete, Save button works).
- Invitee tapping any item row opens EditItemScreen in read-only mode with NO save / delete affordances.
- Per-item kebab on RegistryItemRow hidden for non-owners.
- Invitee Reserve button works via the EXACT SAME ReserveItemUseCase + GuestIdentitySheet + Intent.ACTION_VIEW pipeline used today on RegistryDetailScreen — zero new reserve code paths.
- Invitee Mark-as-purchased button works via the EXACT SAME ConfirmPurchaseUseCase used today on RegistryDetailScreen — zero new purchase code paths.
- 9 new unit tests GREEN (4 isOwner + 5 reservation), full app test suite GREEN, no regression.
- Zero changes to: nav signatures, security rules, Cloud Functions, GuestPreferencesRepository, ReservationRepository / Impl, ReserveItemUseCase, ConfirmPurchaseUseCase, AuthRepository, strings.xml (EN or RO).
- Hilt graph compiles cleanly (`:app:assembleDebug` green).
</success_criteria>

<output>
After completion, create `.planning/quick/260507-vrp-invitee-can-tap-item-rows-to-view-detail/260507-vrp-SUMMARY.md` summarising:
- Final files modified (6: RegistryItemRow.kt, RegistryDetailScreen.kt, EditItemViewModel.kt, EditItemScreen.kt, EditItemViewModelIsOwnerTest.kt, EditItemViewModelReservationTest.kt).
- The pattern used: row-level `Modifier.clickable` for tap, `showOverflow: Boolean` for kebab gate, replicated `RegistryDetailViewModel.isOwner` + `ReservationEvent` + reservation orchestration verbatim into EditItemViewModel.
- Test count + names (4 isOwner tests + 5 reservation tests = 9 new GREEN tests).
- Owner / invitee observable behaviour delta on EditItemScreen.
- Confirmation that NO security rules, Cloud Functions, repositories, use cases, nav keys, or string resources were touched.
- Outstanding follow-up: device verification on a real owner + invitee account pair (combine with quick-260507-uzv + quick-260507-veb device passes still pending).
</output>
