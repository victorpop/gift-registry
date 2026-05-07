---
phase: quick-260507-uzv
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  - app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt
autonomous: true
requirements:
  - QUICK-260507-UZV: Hide owner-only overflow menu actions (Edit / Share / Invite / Delete) from non-owners on RegistryDetailScreen; hide the kebab icon itself when the menu would be empty.

must_haves:
  truths:
    - "Owner viewing their own registry sees the kebab icon and all four overflow actions (Edit, Share, Invite, Delete) — unchanged behaviour."
    - "Non-owner (any signed-in user whose uid != registry.ownerId) sees NO kebab icon and NO overflow menu on RegistryDetailScreen."
    - "Unauthenticated viewer / loading state (registry == null OR user == null) sees NO kebab icon — matches the existing isOwner=false default; never flashes owner-only items."
    - "Top-bar Share icon (OpenInNew) and the hero share-pill are unchanged — visible to everyone."
    - "Cover-photo tap target on hero (D-13) remains owner-only via the existing onCoverTap pathway — no regression."
    - "RegistryDetailViewModel.isOwner is unit-tested for: match → true, mismatch → false, null registry → false, null user → false."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt"
      provides: "Toolbar kebab IconButton conditional on non-null onOverflow callback (mirrors existing onCoverTap nullable pattern in same file)"
      contains: "if (onOverflow != null)"
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      provides: "isOwner-gated overflow region: kebab callback null when !isOwner, DropdownMenu Box wrapped in `if (isOwner)`"
      contains: "if (isOwner)"
    - path: "app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt"
      provides: "Unit tests pinning isOwner StateFlow contract"
      contains: "class RegistryDetailViewModelIsOwnerTest"
  key_links:
    - from: "RegistryDetailScreen.RegistryDetailHero call site (line ~196)"
      to: "RegistryDetailHero.onOverflow IconButton (RegistryDetailHero.kt line ~186)"
      via: "nullable onOverflow callback"
      pattern: "onOverflow = if \\(isOwner\\)"
    - from: "RegistryDetailScreen overflow DropdownMenu Box (line ~275-328)"
      to: "isOwner StateFlow (RegistryDetailViewModel.kt line 182-189)"
      via: "if (isOwner) { Box { DropdownMenu(...) } }"
      pattern: "if \\(isOwner\\)"
    - from: "RegistryDetailViewModel.isOwner"
      to: "AuthRepository.authState + observeRegistryUseCase"
      via: "combine { reg, user -> reg != null && user != null && reg.ownerId == user.uid }"
      pattern: "reg.ownerId == user.uid"
---

<objective>
Hide the four owner-only overflow actions (Edit Registry, Share Registry, Invite People, Delete) from non-owners on RegistryDetailScreen. When the menu would be empty for non-owners, hide the kebab icon itself too — opening an empty menu is broken UX.

Purpose: Invitees and other guests currently see UI affordances they cannot use. Server already rejects (Cloud Function `inviteToRegistry` line 50, Firestore rules for Edit/Delete) but the client UI is misleading. This is a pure client-side UI gate that mirrors the canonical server-side `registryData.ownerId !== request.auth.uid` check.

Output: Owner-only overflow region disappears entirely for non-owners. Owner experience unchanged. Top-bar Share, hero share-pill, and cover-photo D-13 tap remain untouched per spec.
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
@app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
@app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt
@app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelConfirmPurchaseTest.kt
@functions/src/registry/inviteToRegistry.ts

<interfaces>
<!-- Key contracts the executor needs. Do NOT re-explore — these are the exact symbols in play. -->

`RegistryDetailViewModel` ALREADY exposes `isOwner` (RegistryDetailViewModel.kt:182-189):
```kotlin
val isOwner: StateFlow<Boolean> = combine(
    registry,                    // StateFlow<Registry?>
    authRepository.authState,    // Flow<User?>
) { reg, user ->
    reg != null && user != null && reg.ownerId == user.uid
}
    .catch { emit(false) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)
```
**Reuse it.** It mirrors `inviteToRegistry.ts` line 50 (`registryData.ownerId !== request.auth.uid`). Defaults to `false` while loading — no flash of owner-only UI.

`RegistryDetailScreen` already collects it (line 86):
```kotlin
val isOwner by viewModel.isOwner.collectAsStateWithLifecycle()
```
And already uses it at the cover-photo callsite (line 204):
```kotlin
onCoverTap = if (isOwner) ({ pickerSheetOpen = true }) else null,
```

`RegistryDetailHero` signature (RegistryDetailHero.kt:61-75):
```kotlin
internal fun RegistryDetailHero(
    registry: Registry?,
    listState: LazyListState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOverflow: () -> Unit,           // <-- becomes onOverflow: (() -> Unit)? = null in Task 2
    modifier: Modifier = Modifier,
    onCoverTap: (() -> Unit)? = null, // <-- already-established nullable pattern for owner-gated affordances
)
```

`AuthRepository` surface (AuthRepository.kt:7-8):
```kotlin
val authState: Flow<User?>
val currentUser: User?
```

`Registry` domain model uses `ownerId: String`. `User` (Firebase wrapper) uses `uid: String`.

Test pattern (from RegistryDetailViewModelConfirmPurchaseTest.kt:35-63): MockK `relaxed = true` for all use case deps, `MainDispatcherRule()`, `runTest { advanceUntilIdle() }`, optional `app.cash.turbine.test { }` for flow assertions. The shared `vm()` helper wires every constructor param including `authRepository = mockk(relaxed = true)`.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add RegistryDetailViewModelIsOwnerTest pinning the isOwner contract (RED first, GREEN immediately because VM code already exists)</name>
  <files>app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt</files>
  <behavior>
    - Test 1: `isOwner_isTrue_whenRegistryOwnerIdMatchesAuthUid` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authRepository.authState returns flowOf(User(uid="user-1")), expect isOwner.value == true after advanceUntilIdle.
    - Test 2: `isOwner_isFalse_whenRegistryOwnerIdDiffersFromAuthUid` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authState returns flowOf(User(uid="user-2")), expect isOwner.value == false (this is the invitee case — the bug we are fixing).
    - Test 3: `isOwner_isFalse_whenRegistryIsNull` — observeRegistry emits null (loading / not-found), authState returns flowOf(User(uid="user-1")), expect isOwner.value == false (no flash of owner UI during load).
    - Test 4: `isOwner_isFalse_whenAuthStateIsNull` — observeRegistry returns flowOf(Registry(ownerId="user-1", ...)), authState returns flowOf(null) (signed-out), expect isOwner.value == false.
  </behavior>
  <action>
    Create `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt`. Follow the EXACT structure of `RegistryDetailViewModelConfirmPurchaseTest.kt` (same package, `MainDispatcherRule`, `@OptIn(ExperimentalCoroutinesApi::class)`, MockK `relaxed = true` deps).

    Per QUICK-260507-UZV implementation guidance: reuse the existing `isOwner` StateFlow on the VM. These tests pin its contract.

    Skeleton:
    ```kotlin
    package com.giftregistry.ui.registry.detail

    import androidx.lifecycle.SavedStateHandle
    import com.giftregistry.MainDispatcherRule
    import com.giftregistry.domain.auth.AuthRepository
    import com.giftregistry.domain.model.Registry
    import com.giftregistry.domain.model.User
    import com.giftregistry.domain.preferences.GuestPreferencesRepository
    import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
    import com.giftregistry.domain.usecase.DeleteItemUseCase
    import com.giftregistry.domain.usecase.DeleteRegistryUseCase
    import com.giftregistry.domain.usecase.ObserveItemsUseCase
    import com.giftregistry.domain.usecase.ObserveRegistryUseCase
    import com.giftregistry.domain.usecase.ReserveItemUseCase
    import com.giftregistry.ui.notifications.NotificationBus
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
    class RegistryDetailViewModelIsOwnerTest {
        @get:Rule val mainDispatcherRule = MainDispatcherRule()

        private val observeRegistry: ObserveRegistryUseCase = mockk(relaxed = true)
        private val observeItems: ObserveItemsUseCase = mockk(relaxed = true)
        private val authRepository: AuthRepository = mockk(relaxed = true)

        private fun fakeRegistry(ownerId: String) = Registry(
            id = "reg-1",
            ownerId = ownerId,
            title = "Test",
            occasion = "Birthday",
            // ... fill remaining required fields with default-valid values; consult Registry data class
        )

        private fun vm(): RegistryDetailViewModel {
            val ssh = SavedStateHandle(mapOf("registryId" to "reg-1"))
            return RegistryDetailViewModel(
                observeRegistryUseCase = observeRegistry,
                observeItemsUseCase = observeItems,
                deleteRegistryUseCase = mockk(relaxed = true),
                deleteItemUseCase = mockk(relaxed = true),
                reserveItemUseCase = mockk(relaxed = true),
                guestPreferencesRepository = mockk<GuestPreferencesRepository>(relaxed = true),
                deepLinkBus = ReservationDeepLinkBus(),
                confirmPurchaseUseCase = mockk<ConfirmPurchaseUseCase>(relaxed = true),
                notificationBus = NotificationBus(),
                authRepository = authRepository,
                updateRegistryUseCase = mockk(relaxed = true),
                storageRepository = mockk(relaxed = true),
                coverImageProcessor = mockk(relaxed = true),
                savedStateHandle = ssh,
            )
        }

        @Test
        fun `isOwner is true when registry ownerId matches auth uid`() = runTest {
            every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
            every { authRepository.authState } returns flowOf(User(uid = "user-1", /* other defaults */))
            val viewModel = vm()
            advanceUntilIdle()
            assertTrue(viewModel.isOwner.value)
        }
        // + 3 more tests for the other behavior cases
    }
    ```

    IMPORTANT details for the executor:
    - Inspect the `Registry` and `User` data classes for required fields and supply minimal valid defaults — do NOT add new test fixtures elsewhere.
    - If a `User` constructor is awkward (Firebase model), use `mockk<User>().also { every { it.uid } returns "user-1" }` instead of constructing directly. Same for `Registry` if its constructor is broad.
    - The VM's `isOwner` uses `SharingStarted.Eagerly`, so `advanceUntilIdle()` is sufficient — no `turbine.test {}` needed; assert `viewModel.isOwner.value` directly.
    - Do NOT modify `RegistryDetailViewModel.kt`. The VM already has the correct `isOwner` flow; these tests pin it.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.detail.RegistryDetailViewModelIsOwnerTest" -q</automated>
  </verify>
  <done>All 4 tests in RegistryDetailViewModelIsOwnerTest pass. The contract is pinned: match → true, mismatch → false, null registry → false, null user → false.</done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: Gate kebab icon in RegistryDetailHero on nullable onOverflow + gate overflow DropdownMenu region in RegistryDetailScreen on isOwner</name>
  <files>
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  </files>
  <action>
    Two coordinated edits implementing the QUICK-260507-UZV implementation guidance ("gate the entire kebab Box (icon + DropdownMenu) on isOwner"):

    **Edit A — `RegistryDetailHero.kt`:** Make the kebab IconButton conditional on a nullable `onOverflow` callback. This mirrors the existing `onCoverTap: (() -> Unit)? = null` pattern already in this same file (line 74) — the established convention for owner-gated affordances on the hero. Do NOT introduce a separate `showOverflow: Boolean` flag; the nullable callback IS the gate.

    1. Change the parameter signature on line 66 from:
       ```kotlin
       onOverflow: () -> Unit,
       ```
       to:
       ```kotlin
       onOverflow: (() -> Unit)? = null,
       ```
       Move it AFTER `modifier` to match the established trailing-nullable convention used by `onCoverTap`. Final order: `onBack, onShare, modifier, onOverflow, onCoverTap` — verify no other call sites pass `onOverflow` positionally; if all call sites use named args (current call site at RegistryDetailScreen.kt:201 does), this reorder is safe.

    2. Wrap the kebab IconButton (lines 186-192) in a `null`-check:
       ```kotlin
       if (onOverflow != null) {
           IconButton(onClick = onOverflow) {
               Icon(
                   imageVector = Icons.Default.MoreVert,
                   contentDescription = stringResource(R.string.registry_detail_overflow_desc),
                   tint = if (toolbarAlpha > 0.5f) colors.ink else colors.paper,
               )
           }
       }
       ```
       The `Spacer(Modifier.weight(1f))` and Share IconButton above stay untouched — top-bar Share is intentionally NOT gated per spec ("Out of scope: top-bar Share icon").

    **Edit B — `RegistryDetailScreen.kt`:** Two changes, both gated on the already-collected `isOwner` (line 86 of the screen — already in place from Phase 12 D-13).

    1. At the `RegistryDetailHero` call site (line 196-205), change `onOverflow = { overflowMenuExpanded = true }` to:
       ```kotlin
       onOverflow = if (isOwner) ({ overflowMenuExpanded = true }) else null,
       ```
       This mirrors the existing `onCoverTap = if (isOwner) (...) else null` pattern on line 204 — same convention, same file, two lines apart.

    2. Wrap the entire overflow `Box` containing the `DropdownMenu` (lines 275-328) in `if (isOwner) { ... }`:
       ```kotlin
       if (isOwner) {
           Box(
               modifier = Modifier
                   .align(Alignment.TopEnd)
                   .padding(top = 56.dp, end = 8.dp),
           ) {
               DropdownMenu(
                   expanded = overflowMenuExpanded,
                   onDismissRequest = { overflowMenuExpanded = false },
               ) {
                   // ... all four DropdownMenuItem entries unchanged ...
               }
           }
       }
       ```
       Defence-in-depth: even if `overflowMenuExpanded` were somehow flipped true (it cannot be — Edit B.1 makes the trigger null for non-owners), the menu cannot render for a non-owner because the entire Box is removed from the composition.

    Constraints honoured:
    - **No new auth abstraction.** Reuses the existing `viewModel.isOwner` flow which is already collected at line 86.
    - **No string resource changes.** We are hiding, not adding labels.
    - **No security rule / Cloud Function changes.** The server already enforces ownership (`inviteToRegistry.ts` line 50, Firestore rules for Edit/Delete) — this is a UI-only gate.
    - **Top-bar Share icon, hero share-pill, ShareBanner row — UNCHANGED.** Those affordances are intentionally available to anyone viewing the registry per CONTEXT.
    - **D-13 cover-photo tap — UNCHANGED.** Already gated on `isOwner` at line 204; no regression.
    - **Loading state safe.** `isOwner` defaults to `false` (Eagerly initial value + .catch { emit(false) }) so non-owner UI is the safe default until the first emission resolves; no flash of owner-only items.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin :app:testDebugUnitTest -q</automated>
  </verify>
  <done>
    - `:app:compileDebugKotlin` succeeds (signature change on `onOverflow` does not break any callers).
    - All existing app unit tests still pass (no regression on `RegistryDetailViewModelConfirmPurchaseTest`, `HeroToolbarAlphaTest`, etc.).
    - `RegistryDetailViewModelIsOwnerTest` from Task 1 still passes.
    - When a Registry's ownerId equals the signed-in user's uid → `isOwner = true` → kebab IconButton renders (nullable callback non-null) AND DropdownMenu Box is present → owner sees Edit / Share / Invite / Delete.
    - When uid != ownerId (or registry/user is null) → `isOwner = false` → `onOverflow = null` → kebab IconButton is absent from composition AND DropdownMenu Box is absent → non-owner sees no overflow region at all.
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
# Confirm no other file passes onOverflow positionally — the parameter reorder must be safe.
grep -rn "RegistryDetailHero(" app/src/ | grep -v "internal fun"
```
Expected: a single hit at `RegistryDetailScreen.kt` using named arguments (already the case).

**Manual device verification** (deferred to user; not blocking):
1. Sign in as a Registry's owner → open RegistryDetailScreen → kebab icon visible top-right → tap → see Edit / Share / Invite / Delete. Unchanged behaviour.
2. Sign in as a different user who has been invited to that registry (or open a registry the user does not own) → kebab icon gone from top-right toolbar → no overflow menu accessible. Top-bar Share icon (OpenInNew) and the hero share pill remain visible. Cover-photo tap target on hero is also gone (existing D-13 behaviour, unchanged).
3. Edit / Delete / Invite via Cloud Functions still rejected server-side for non-owners (defence in depth — already in place, just confirming nothing was relaxed).
</verification>

<success_criteria>
- All 4 tests in `RegistryDetailViewModelIsOwnerTest` GREEN, pinning the existing `isOwner` contract.
- `RegistryDetailHero.onOverflow` is nullable; kebab `IconButton` only renders when non-null.
- `RegistryDetailScreen` passes `onOverflow = null` and skips the `DropdownMenu` Box entirely when `isOwner == false`.
- Zero changes to: `RegistryDetailViewModel.kt`, `AuthRepository.kt`, `firestore.rules`, `storage.rules`, `inviteToRegistry.ts`, any `strings.xml`.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` both green.
- Top-bar Share icon, hero share-pill, ShareBanner, D-13 cover-photo tap, and confirm-purchase banner — all unchanged.
</success_criteria>

<output>
After completion, create `.planning/quick/260507-uzv-hide-owner-only-overflow-menu-actions-fr/260507-uzv-SUMMARY.md` summarising:
- Final files modified (3: RegistryDetailHero.kt, RegistryDetailScreen.kt, RegistryDetailViewModelIsOwnerTest.kt)
- The pattern used (reused existing `viewModel.isOwner`; mirrored existing `onCoverTap` nullable callback convention from D-13)
- Test count + names (4 isOwner tests pinned)
- Owner / non-owner observable behaviour delta
- Confirmation that no security rules, Cloud Functions, or string resources were touched
- Outstanding follow-up: device verification (kebab present for owner, absent for invitee)
</output>
