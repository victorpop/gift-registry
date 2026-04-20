---
phase: quick/260420-iic
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
autonomous: true
requirements:
  - QUICK-260420-iic
must_haves:
  truths:
    - "After signing out as user A and signing in as user B, the registry list shows B's registries (not A's, not empty)."
    - "Cold-start flow still shows the signed-in user's registries (no regression)."
    - "Signing out (from within the app) does not crash or leak A's registries into the post-sign-out UI state."
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt"
      provides: "Reactive registry list StateFlow that swaps upstream when auth user changes"
      contains: "flatMapLatest"
  key_links:
    - from: "RegistryListViewModel.uiState"
      to: "AuthRepository.authState"
      via: "flatMapLatest swapping observeRegistries(user.uid) on each auth emission"
      pattern: "authRepository\\.authState[\\s\\S]*flatMapLatest"
    - from: "flatMapLatest block"
      to: "ObserveRegistriesUseCase"
      via: "observeRegistries(user.uid) called per auth emission with the fresh uid"
      pattern: "observeRegistries\\(user\\.uid\\)"
---

<objective>
Fix a stale-UID bug in `RegistryListViewModel` that causes the registry list to keep querying the previous user's UID after a sign-out/sign-in on the same Activity.

Purpose: Registries created by the currently signed-in user must appear in the list. Currently, because the ViewModel is scoped to the Activity (per Phase 02 decision "rememberViewModelStoreNavEntryDecorator absent from Navigation3 1.0.1 stable; hiltViewModel() uses Activity ViewModelStoreOwner"), it survives auth flips and keeps filtering by a captured-at-init UID.

Output: `RegistryListViewModel.uiState` derived reactively from `authRepository.authState` via `flatMapLatest`, so the Firestore listener is swapped whenever the signed-in user changes.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@./CLAUDE.md
@.planning/STATE.md

# File being modified
@app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt

# Interfaces consumed (do NOT modify)
@app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt
@app/src/main/java/com/giftregistry/domain/model/User.kt
@app/src/main/java/com/giftregistry/domain/usecase/ObserveRegistriesUseCase.kt

<interfaces>
<!-- Key contracts the executor needs. Do NOT change these — this is a VM-layer-only fix. -->

From app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt:
```kotlin
interface AuthRepository {
    val authState: Flow<User?>          // <-- the Flow to subscribe to
    val currentUser: User?              // <-- do NOT use this for init-time capture
    // ... other methods unchanged
}
```

From app/src/main/java/com/giftregistry/domain/model/User.kt:
```kotlin
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean,
)
```

From app/src/main/java/com/giftregistry/domain/usecase/ObserveRegistriesUseCase.kt:
```kotlin
class ObserveRegistriesUseCase @Inject constructor(
    private val repository: RegistryRepository,
) {
    operator fun invoke(ownerId: String): Flow<List<Registry>>
}
```
</interfaces>

<audit_findings>
<!-- Audit of VM-layer stale-UID anti-pattern (grep: authRepository.currentUser + uid at VM init time). -->

Audited VMs under `app/src/main/java/com/giftregistry/ui/`:

| ViewModel | UID at init? | Verdict |
|-----------|--------------|---------|
| RegistryListViewModel | YES (line 38, `val uid = authRepository.currentUser?.uid ?: ""` inside `init {}` used to seed a long-lived StateFlow) | **FIX** |
| CreateRegistryViewModel | No — reads `authRepository.currentUser?.uid` inside `onSave()` (a user-triggered method). UID is read fresh per save. | Safe, no fix. |
| SettingsViewModel | No — does not touch `AuthRepository` at all. | Safe, no fix. |
| RegistryDetailViewModel | No — reads `registryId` from `SavedStateHandle`; no `currentUser` access. | Safe, no fix. |
| InviteViewModel | No `currentUser` usage. | Safe, no fix. |
| AddItemViewModel | No `currentUser` usage (uses `SavedStateHandle`). | Safe, no fix. |
| EditItemViewModel | No `currentUser` usage (uses `SavedStateHandle`). | Safe, no fix. |

Non-VM hits (out of scope per constraint "purely a VM-layer bug"):
- `FcmTokenRepositoryImpl` reads UID inside suspend functions (fresh per call). Safe.
- `LanguagePreferencesDataStore.syncRemoteLocale()` reads UID per call. Safe.

**Conclusion:** Exactly ONE file needs to change: `RegistryListViewModel.kt`.

Test coverage check: `app/src/test/java/com/giftregistry/ui/registry/list/` does not exist — no `RegistryListViewModelTest`. Per constraint ("If no test file, skip tests"), no test work in this plan.
</audit_findings>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Make RegistryListViewModel.uiState reactive to authState</name>
  <files>app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt</files>
  <action>
Replace the init-time UID capture with a reactive chain off `authRepository.authState`. This is a surgical change: touch only the `uiState` construction in `init {}` and the imports. Leave `deleteError`, `onDeleteRegistry`, `clearDeleteError`, and the `RegistryListUiState` sealed interface unchanged.

**Why a fix is needed:** Per STATE.md Phase 02 decision "rememberViewModelStoreNavEntryDecorator absent from Navigation3 1.0.1 stable; hiltViewModel() uses Activity ViewModelStoreOwner", this VM is scoped to the Activity and survives sign-out/sign-in. The captured `val uid` at init freezes the query to the first user's UID forever.

**Exact change — `init {}` block:** Replace

```kotlin
init {
    val uid = authRepository.currentUser?.uid ?: ""
    uiState = observeRegistries(uid)
        .map { RegistryListUiState.Success(it) as RegistryListUiState }
        .catch { emit(RegistryListUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RegistryListUiState.Loading)
}
```

with

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
init {
    uiState = authRepository.authState
        .flatMapLatest { user ->
            if (user == null) {
                flowOf<RegistryListUiState>(RegistryListUiState.Loading)
            } else {
                observeRegistries(user.uid)
                    .map { RegistryListUiState.Success(it) as RegistryListUiState }
            }
        }
        .catch { emit(RegistryListUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RegistryListUiState.Loading)
}
```

**Imports to add** (alongside the existing `kotlinx.coroutines.flow.*` imports — keep them alphabetized):
- `kotlinx.coroutines.ExperimentalCoroutinesApi`
- `kotlinx.coroutines.flow.flatMapLatest`
- `kotlinx.coroutines.flow.flowOf`

**Imports that are no longer needed after the change** (remove if present and otherwise unused in the file): none — `map`, `catch`, `stateIn`, `SharingStarted`, `StateFlow`, `MutableStateFlow`, `viewModelScope`, `launch` all remain in use.

**Design notes (do not deviate):**
- Per constraint: emit `RegistryListUiState.Loading` (not `Success(emptyList())`) when `user == null` to avoid briefly showing "no registries" during the sign-out transient. This is the correct UX — the auth screen will replace this UI shortly anyway.
- Keep `SharingStarted.Eagerly` — matches the existing pattern and ensures the Firestore listener attaches immediately on first user emission.
- Keep `.catch` at the END of the chain (after `flatMapLatest`) so errors from BOTH the auth flow AND the registries flow are routed to `RegistryListUiState.Error`. `flatMapLatest` correctly cancels the previous inner flow on user change.
- `@OptIn(ExperimentalCoroutinesApi::class)` is the idiomatic place for `flatMapLatest` — annotate the `init` block (or the file; init scope is tighter and preferred here).
- Do NOT change `AuthRepository`, `ObserveRegistriesUseCase`, `RegistryRepository`, or any domain/data-layer code. This bug is VM-only.
- Do NOT add new tests — no existing `RegistryListViewModelTest` exists to update (verified via `app/src/test/java/com/giftregistry/ui/registry/list/` not existing).

**Do NOT audit/fix other VMs in this task.** Audit already ran in planning (see `<audit_findings>` in context): `CreateRegistryViewModel` reads UID fresh inside `onSave()` (safe), `SettingsViewModel` doesn't use auth (safe), and no other VM caches UID at init. Fixing them speculatively violates the constraint.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>

Post-build sanity (automated, fast):
- File contains `authRepository.authState` AND `flatMapLatest` AND `observeRegistries(user.uid)`:
  `grep -q "authRepository\.authState" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt && grep -q "flatMapLatest" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt && grep -q "observeRegistries(user\.uid)" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt`
- File no longer contains the stale pattern:
  `! grep -E 'val uid = authRepository\.currentUser\?\.uid' app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt`
- `@OptIn(ExperimentalCoroutinesApi::class)` present:
  `grep -q "@OptIn(ExperimentalCoroutinesApi::class)" app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt`
  </verify>
  <done>
    - `./gradlew :app:assembleDebug` exits 0.
    - `RegistryListViewModel.uiState` is built from `authRepository.authState.flatMapLatest { ... }`.
    - The stale `val uid = authRepository.currentUser?.uid ?: ""` line is gone from the file.
    - `SharingStarted.Eagerly` and the `.catch { emit(Error) }` clause are preserved.
    - When `user == null`, the inner flow is `flowOf(Loading)` (not `Success(emptyList())`).
    - Only `RegistryListViewModel.kt` is modified — no domain/data changes.
  </done>
</task>

</tasks>

<verification>
**Build gate:** `./gradlew :app:assembleDebug` must succeed.

**Manual repro of the original bug (to confirm fix — optional human step during PR review, not required for task completion):**
1. Launch app on emulator with Firebase emulators running.
2. Sign in as user A; create a registry; verify it appears in the list.
3. Sign out.
4. Sign in as user B (previously existing account with its own registries, e.g., Maria's `Hy347m5jLTjItAsDEwaARaIhVgRN`).
5. Expected: B's registries appear in the list. A's do not. The list is never permanently stuck on A's UID.
</verification>

<success_criteria>
- Debug APK builds cleanly.
- `RegistryListViewModel.uiState` is derived from `authRepository.authState` via `flatMapLatest`, swapping the Firestore listener on every user change.
- When the user is `null` (signed out), the inner flow emits `RegistryListUiState.Loading` (not an empty-success).
- No changes to `AuthRepository`, `ObserveRegistriesUseCase`, `RegistryRepository`, or any other ViewModel — per-audit, no other VM holds a UID-at-init.
- `SharingStarted.Eagerly` preserved so the StateFlow attaches immediately on first auth emission (no first-emission delay regression).
</success_criteria>

<output>
After completion, create `.planning/quick/260420-iic-fix-stale-uid-bug-make-registrylistviewm/260420-iic-SUMMARY.md` per the standard quick-task summary template.
</output>
