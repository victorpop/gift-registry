---
status: resolved
trigger: "Two crashes on physical Android device during navigation flows in the registry feature. No logcat available — must infer from code."
created: 2026-05-12T00:00:00Z
updated: 2026-05-12T14:00:00Z
symptoms_prefilled: true
goal: find_and_fix
---

## Current Focus

hypothesis: Two independent bugs. (A) CreateRegistryViewModel(key="new") is Activity-scoped and never resets savedRegistryId — stale non-null value on re-entry causes LaunchedEffect to fire onSaved immediately, double-popping or navigating stale. (B) All in-screen onBack lambdas call backStack.removeLast() with NO size guard — on Android 14+ physical device with predictive-back gesture + simultaneous in-screen back tap, both removeLast() calls fire and the second removes HomeKey, leaving backStack=[]. NavDisplay then throws require(backStack.isNotEmpty()).
test: confirmed by reading NavDisplay source: line 199 `require(backStack.isNotEmpty())`.
expecting: fix (A) = clear savedRegistryId after consumption in VM; fix (B) = guard removeLast() with size >= 2 in all in-screen back lambdas in AppNavigation.kt
next_action: apply both fixes

## Symptoms

expected:
- CRASH 1: Tapping "Add" on new-registry creation screen saves and navigates without crashing.
- CRASH 2: Tapping back arrow on registry detail screen pops back to list without crashing.

actual:
- CRASH 1: App crashes after tapping "Add". Registry IS persisted — crash happens post-save, during navigation/state update.
- CRASH 2: App crashes when tapping the back arrow on registry detail screen.

errors: None captured (no logcat from physical device).

reproduction:
- CRASH 1: Open app → "+" → fill form → tap "Add" → crash. Registry present on reopen.
- CRASH 2: Open app → tap registry → detail screen → tap back arrow → crash.

started: Discovered while testing on physical device. Most recent Android work: quick/260510-noi (description field added to add-registry screen).

## Eliminated

(none yet)

## Evidence

- timestamp: 2026-05-12T00:30:00Z
  checked: NavDisplay.kt (navigation3-ui 1.0.1 source)
  found: Line 199: `require(backStack.isNotEmpty()) { "NavDisplay backstack cannot be empty" }`. Line 285 same check for entries. This is a hard crash (IllegalArgumentException) if backStack becomes empty.
  implication: Any path that calls backStack.removeLast() when backStack.size == 1 will empty the stack and crash on next NavDisplay composition.

- timestamp: 2026-05-12T00:30:00Z
  checked: AppNavigation.kt — all in-screen onBack lambdas for CreateRegistryKey, EditRegistryKey, RegistryDetailKey, AddItemKey, EditItemKey, SettingsKey, NotificationsKey, StoreListKey, StoreBrowserKey
  found: ALL call `backStack.removeLast()` WITHOUT a size check. NavDisplay's OWN onBack = `{ if (backStack.size > 1) backStack.removeLast() }` has the guard. In-screen backs do not.
  implication: On Android 14+ physical device, predictive back gesture (handled by NavigationBackHandler in NavDisplay) fires onBackCompleted which calls NavDisplay's guarded onBack. But if user simultaneously taps in-screen back arrow, the second removeLast() call is unguarded and can empty the stack → crash.

- timestamp: 2026-05-12T00:30:00Z
  checked: CreateRegistryViewModel.kt — savedRegistryId field; CreateRegistryScreen.kt — LaunchedEffect(savedRegistryId)
  found: `savedRegistryId` is a `MutableStateFlow<String?>(null)` in the ViewModel. Once set to non-null after a successful save, it is NEVER reset to null. ViewModel has key "new" and is Activity-scoped (no per-entry ViewModelStore in Nav3 without rememberViewModelStoreNavEntryDecorator). Second time user opens CreateRegistryKey, the same ViewModel is returned with savedRegistryId already non-null. LaunchedEffect fires immediately on re-entry, calling onSaved(staleId) → removes CreateRegistryKey + adds AddItemKey(staleId) before the user does anything.
  implication: This does not crash on FIRST use but causes incorrect navigation on second+ use. Could be the crash mechanism if this stale onSaved firing conflicts with backStack state.

- timestamp: 2026-05-12T00:30:00Z
  checked: RegistryDetailViewModel init block — `val registryId: String = checkNotNull(savedStateHandle["registryId"])` 
  found: checkNotNull throws IllegalStateException if the savedStateHandle key is missing. hiltViewModelWithNavArgs seeds this correctly on first creation. Since ViewModel is Activity-scoped and cached by key, this is only problematic if somehow the args are not seeded. Not the crash cause.
  implication: Not the root cause.

- timestamp: 2026-05-12T00:30:00Z
  checked: backStack declaration in AppNavigation.kt line 63
  found: `val backStack = remember { mutableStateListOf<Any>(AuthKey) }`. Uses `remember` NOT `rememberSaveable`. On configuration change (rotation), backStack resets to [AuthKey], losing navigation state. On a physical device this causes nav state loss on rotation. Does not directly crash.
  implication: Secondary bug (navigation state loss on rotation), not the primary crash cause.

- timestamp: 2026-05-12T00:30:00Z
  checked: NavDisplay source — NavigationBackHandler in the entries overload (line 296-305)
  found: `onBackCompleted = { repeat(entries.size - scene.previousEntries.size) { onBack() } }`. This calls NavDisplay's onBack `{ if (backStack.size > 1) backStack.removeLast() }` which IS guarded. The system gesture path is safe. The in-screen button path is NOT guarded.
  implication: Confirms CRASH 2 mechanism: system gesture safely pops, but in-screen button (still interactive during animation or predictive-back preview) fires unguarded removeLast() potentially on size-1 stack.

## Resolution

root_cause: Two independent bugs sharing a common theme (unguarded backStack mutations).
  CRASH 1: `CreateRegistryViewModel` (key="new", Activity-scoped) never resets `savedRegistryId` to null after a successful save. On second+ entry to CreateRegistryKey, the stale non-null `savedRegistryId` causes `LaunchedEffect(savedRegistryId)` to fire immediately on composition, calling `onSaved(staleId)` → `backStack.removeLast()` unexpectedly. Additionally: ALL in-screen onBack lambdas in AppNavigation.kt call `backStack.removeLast()` without size check, unlike NavDisplay's own guarded `onBack`. On a physical Android 14+ device, predictive back + in-screen button simultaneously can empty the stack, and NavDisplay's `require(backStack.isNotEmpty())` crashes the app.
  CRASH 2: Same unguarded `backStack.removeLast()` on RegistryDetailKey's onBack. During the exit animation (RegistryDetailScreen still in composition), a second tap or system gesture interaction fires removeLast() on a 1-element stack, producing an empty stack and NavDisplay crash.

fix:
  1. Added `fun clearSavedRegistryId()` to CreateRegistryViewModel that resets `savedRegistryId.value = null`. Called from LaunchedEffect(savedRegistryId) in CreateRegistryScreen immediately before calling onSaved/onSkip. Prevents stale non-null value from triggering spurious navigation on second+ CreateRegistryKey entry (Activity-scoped VM retains the value across navigation).
  2. In AppNavigation.kt, replaced every unguarded `backStack.removeLast()` in in-screen callback lambdas with `if (backStack.size > 1) backStack.removeLast()`. Screens affected: CreateRegistryKey.onBack/onSaved/onSkip, EditRegistryKey.onBack/onSaved, RegistryDetailKey.onBack, StoreListKey.onBack, StoreBrowserKey.onBack, AddItemKey.onBack, EditItemKey.onBack, SettingsKey.onBack, NotificationsKey.onBack. NavDisplay's own onBack already had this guard.

verification: compileDebugKotlin BUILD SUCCESSFUL, testDebugUnitTest BUILD SUCCESSFUL (29 tasks). Awaiting physical device verification.
files_changed: [
  app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt,
  app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt,
  app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt,
]

---

## CRASH 3 — Add Item to existing registry

### Evidence

- timestamp: 2026-05-12T01:00:00Z
  checked: AddItemViewModel._savedItemId + AddItemScreen.LaunchedEffect(savedItemId)
  found: `_savedItemId = MutableStateFlow<String?>(null)` is set to non-null on successful save (line 192: `_savedItemId.value = itemId`) but is NEVER reset to null on the "Save and Exit" path. `onResetForm()` does reset it, but that is only called when `addAnotherMode == true`. When `addAnotherMode == false` (normal Save and Exit), the LaunchedEffect calls `onBack()` without clearing `_savedItemId`.
  implication: Same bug class as CRASH 1's CreateRegistryViewModel.savedRegistryId stale-state bug.

- timestamp: 2026-05-12T01:00:00Z
  checked: AddItemScreen ViewModel key (`registryId ?: "add-item-no-registry-yet"`) + hiltViewModelWithNavArgs scoping
  found: `hiltViewModelWithNavArgs` uses `LocalViewModelStoreOwner.current` which in Nav3 (without rememberViewModelStoreNavEntryDecorator) is the Activity's ViewModelStore. The VM keyed by `registryId` (e.g. "abc123") persists for the Activity lifetime. On second entry to `AddItemKey(registryId = "abc123")`, the same cached ViewModel is returned with `_savedItemId` still non-null from the prior save.
  implication: Re-entering AddItemKey for the same registry yields a stale ViewModel where `savedItemId != null` on initial composition.

- timestamp: 2026-05-12T01:00:00Z
  checked: LaunchedEffect(savedItemId) in AddItemScreen (lines 102-111)
  found: On re-entry with stale `savedItemId != null`, `addAnotherMode` is `false` (fresh `remember` state) → `onBack()` is called immediately → AddItemKey is popped before user can do anything. On first entry during exit animation (NavDisplay keeps screen composed during transition), if the Firestore save completes after the user taps X/close AND before the exit animation ends, a second `onBack()` fires on the already-shrunk backStack — popping RegistryDetail unexpectedly. Under Android 14+ predictive-back timing, this double-pop can reach edge cases covered by the unguarded size scenario.
  implication: Stale state causes immediate spurious back-navigation on second AddItem entry; timing window during exit animation causes double-pop on first entry. The `if (backStack.size > 1)` guard prevents the hard crash but not the broken navigation state.

### Root Cause

`AddItemViewModel._savedItemId` is never reset to null after a "Save and Exit" save. The ViewModel is Activity-scoped (key = registryId, stored in the Activity's ViewModelStore via `hiltViewModelWithNavArgs`). On second entry to the same `AddItemKey`, the LaunchedEffect(savedItemId) fires immediately with the stale non-null value and calls `onBack()` — popping the screen before the user does anything. This is the same pattern as CRASH 1 (CreateRegistryViewModel.savedRegistryId) which was already fixed.

### Fix Applied

1. Added `fun clearSavedItemId()` to `AddItemViewModel` (resets `_savedItemId.value = null`). Mirrors the existing `CreateRegistryViewModel.clearSavedRegistryId()` method.
2. In `AddItemScreen.LaunchedEffect(savedItemId)`, on the `addAnotherMode == false` (Save and Exit) branch, call `viewModel.clearSavedItemId()` immediately before `onBack()`. The `addAnotherMode == true` branch already calls `onResetForm()` which resets `_savedItemId` — no change needed there.

### Verification

- `./gradlew compileDebugKotlin testDebugUnitTest` → BUILD SUCCESSFUL (29 tasks). Awaiting physical device verification.

### Files Changed

- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` — added `clearSavedItemId()` method
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — call `viewModel.clearSavedItemId()` before `onBack()` in the Save-and-Exit LaunchedEffect branch

---

## ACTUAL ROOT CAUSE — Kotlin 2.x `removeLast()` / JDK SequencedCollection clash

### FATAL Stack Trace (CRASH 2, logcat captured)

```
java.lang.NoSuchMethodError: No virtual method removeLast()Ljava/lang/Object;
  in class Landroidx/compose/runtime/snapshots/SnapshotStateList;
  or its super classes (declaration of 'androidx.compose.runtime.snapshots.SnapshotStateList'
  appears in /data/app/com.giftregistry-m1tR_YJkw2J6OoUgzSpVKg==/base.apk!classes18.dex)
    at com.giftregistry.ui.navigation.AppNavigationKt.AppNavigation$lambda$12$0$0$3$3$0(AppNavigation.kt:231)
    at androidx.compose.foundation.ClickableNode.onPointerEvent-H0pRuoY(Clickable.kt:935)
    ...
```

Device: Huawei (HwActivityThreadImpl), targetSdkVersion 36.

### Why the Prior Hypothesis Was Wrong

The previous diagnosis correctly identified unguarded `backStack.removeLast()` calls as unsafe, and added `if (backStack.size > 1)` guards. Those guards are valid defensive code. However, they did NOT fix the crash.

The throw happens INSIDE `removeLast()` itself — `NoSuchMethodError` fires before the call even executes, before the size guard can help. The guards check whether it is safe to call `removeLast()`, but on this device that method simply does not exist on `SnapshotStateList`.

**Mechanism:** Kotlin 2.x (this project: 2.3.20) resolves `MutableList<T>.removeLast()` to `java.util.SequencedCollection.removeLast()` — a Java 21 API added in JDK 21. `SnapshotStateList` does not implement `SequencedCollection`, and older Android Runtime (ART) versions (including this Huawei running an older ART patch level) do not expose the `SequencedCollection` interface on list types. Result: `NoSuchMethodError` at runtime on every device where ART has not caught up with Java 21's `SequencedCollection`.

**Note on JVM target:** The project targets JVM 17 (`jvmTarget = JVM_17`), not JVM 21. This does not protect against the clash because the issue is in Kotlin's stdlib extension function resolution, not the bytecode target level. Kotlin 2.x prefers `java.util.SequencedCollection.removeLast()` over the Kotlin extension when the type is a `MutableList<T>`. Changing the JVM target to 21 would make things worse (even more devices affected), not better. No version changes are needed.

### Fix Applied

Replaced every `backStack.removeLast()` call in `AppNavigation.kt` with `backStack.removeAt(backStack.lastIndex)`.

- `removeAt(Int)` is a `java.util.List` method present since Java 1.2 — guaranteed available on every Android runtime.
- All 13 occurrences replaced via `replace_all` edit.
- Size guards (`if (backStack.size > 1)`) retained — they remain correct independent defensive code.
- Prior fixes (clearSavedRegistryId, clearSavedItemId, size guards) also retained — they address separate latent bugs.

**File changed:** `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — 13 call sites

### Verification

- `grep -rn "\.removeLast()" app/src/main/java` → CLEAN (zero results)
- `./gradlew compileDebugKotlin` → BUILD SUCCESSFUL (19 tasks)
- `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL (29 tasks)
- Awaiting physical device verification

### Device Verification Steps for User

1. Build and install the debug APK on the Huawei (or any physical device).
2. **CRASH 2 (back arrow on registry detail):** Open app → tap any registry → on detail screen tap the back arrow. Previously crashed with `NoSuchMethodError`. Expected: smooth pop back to list.
3. **CRASH 1 (Add registry flow):** Open app → "+" → fill form → tap "Add". Previously crashed post-save. Expected: navigates to AddItem screen without crash.
4. **CRASH 3 (Add item, second entry):** Add an item to a registry → save → re-enter AddItem for the same registry. Expected: blank form shown (no spurious immediate back-pop).
5. **Back arrow stress test:** On each screen (Settings, StoreList, StoreBrowser, AddItem, EditItem, Notifications), tap back arrow multiple times rapidly. Expected: all pop cleanly without crash.

---

## RESOLVED

**Date:** 2026-05-12

**Human verification:** Confirmed — all four verification scenarios passed on physical device.

**Root cause (final):** Kotlin 2.x resolves `MutableList<T>.removeLast()` to `java.util.SequencedCollection.removeLast()` — a JDK 21 API. `SnapshotStateList` does not implement `SequencedCollection`, and ART on older Android runtime patch levels (observed: Huawei with targetSdkVersion 36) does not expose that interface on list types. Every call site produced `java.lang.NoSuchMethodError: No virtual method removeLast()` at runtime. Two latent secondary bugs were also fixed: Activity-scoped ViewModels (`CreateRegistryViewModel`, `AddItemViewModel`) never reset their saved-ID StateFlow after navigation, causing spurious immediate back-navigation on second screen entry.

**Files changed:**
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — replaced all 13 `backStack.removeLast()` calls with `backStack.removeAt(backStack.lastIndex)`; added `size > 1` guards on all in-screen onBack lambdas
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt` — added `clearSavedRegistryId()` to reset `savedRegistryId` after consumption
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` — call `viewModel.clearSavedRegistryId()` in `LaunchedEffect(savedRegistryId)` before invoking `onSaved`
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` — added `clearSavedItemId()` to reset `_savedItemId` after Save-and-Exit
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — call `viewModel.clearSavedItemId()` in the Save-and-Exit `LaunchedEffect` branch before `onBack()`
