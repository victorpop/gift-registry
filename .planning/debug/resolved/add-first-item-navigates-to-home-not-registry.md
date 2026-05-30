---
status: resolved
trigger: "add-first-item-navigates-to-home-not-registry"
created: 2026-05-30T00:00:00Z
updated: 2026-05-30T12:00:00Z
---

## Current Focus

hypothesis: CONFIRMED. CreateRegistryKey.onSaved pops CreateRegistryKey then pushes AddItemKey WITHOUT inserting RegistryDetailKey, leaving [HomeKey, AddItemKey]. onBack() after save pops AddItemKey → Home.
test: Fix applied: insert RegistryDetailKey(registryId) between the pop and AddItemKey push so stack becomes [HomeKey, RegistryDetailKey, AddItemKey].
expecting: After save, AddItemScreen calls onBack() → pops AddItemKey → lands on RegistryDetailKey.
next_action: Awaiting human verification on device

## Symptoms

expected: After saving the first item from the post-create "Continue → add items" flow, the app should land on the newly-created registry's detail/landing screen — the same destination users get when adding items from the registry detail's "+" button.
actual: After saving, the app navigates all the way back to the Home screen (registries list).
errors: None — pure navigation/back-stack flow bug, no crash.
reproduction: |
  1. Sign in.
  2. On Home, create a new registry via "+ create".
  3. Fill registry fields and tap Save.
  4. On post-create screen tap bottom CTA "Continue / Add items".
  5. Add Item screen opens. Fill product. Tap "Save to registry".
  6. Observe: lands on Home, NOT registry detail.
started: Affects only first item via post-create CTA path. Subsequent items from registry detail correctly return to detail.

## Eliminated

## Evidence

- timestamp: 2026-05-30T00:01:00Z
  checked: AppNavigation.kt entry<CreateRegistryKey> onSaved lambda (line 243–246)
  found: |
    onSaved = { registryId ->
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(AddItemKey(registryId = registryId))
    }
    Pops CreateRegistryKey, then pushes ONLY AddItemKey. Stack becomes [HomeKey, AddItemKey].
  implication: AddItemScreen's LaunchedEffect(savedItemId) calls onBack() on save, which pops AddItemKey → lands on HomeKey.

- timestamp: 2026-05-30T00:01:00Z
  checked: AppNavigation.kt entry<RegistryDetailKey> onNavigateToAddItem (line 273)
  found: backStack.add(AddItemKey(key.registryId)) — stack is [HomeKey, RegistryDetailKey, AddItemKey]
  implication: Subsequent items work because RegistryDetailKey is already on the stack beneath AddItemKey. Pop lands on detail.

- timestamp: 2026-05-30T00:01:00Z
  checked: AddItemScreen.kt LaunchedEffect(savedItemId) (lines 126–141)
  found: When addAnotherMode=false: calls viewModel.onResetForm() then onBack(). onBack() is a single removeLast on the backStack.
  implication: Save always pops exactly one entry. Back-stack shape determines landing destination entirely.

- timestamp: 2026-05-30T00:01:00Z
  checked: Compile result after fix
  found: BUILD SUCCESSFUL in 6s
  implication: Fix is syntactically correct and type-safe.

## Resolution

root_cause: >
  AppNavigation.kt entry<CreateRegistryKey>.onSaved popped CreateRegistryKey and pushed AddItemKey
  without first pushing RegistryDetailKey. Back stack after CTA tap was [HomeKey, AddItemKey].
  AddItemScreen.onBack() (called after save) popped AddItemKey and landed on HomeKey.
  The working path (RegistryDetail → AddItem) naturally has [HomeKey, RegistryDetailKey, AddItemKey]
  so its pop lands correctly.
fix: >
  Inserted `backStack.add(RegistryDetailKey(registryId))` between the removeAt and the AddItemKey push
  in AppNavigation.kt entry<CreateRegistryKey>.onSaved. Stack is now
  [HomeKey, RegistryDetailKey, AddItemKey] — pop after save lands on RegistryDetailKey.
verification: BUILD SUCCESSFUL (compileDebugKotlin). Confirmed on physical device by user: post-create CTA path lands on RegistryDetailScreen; subsequent "+" add from detail still lands on detail (regression path clean).
files_changed:
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
