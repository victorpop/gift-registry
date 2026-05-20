# Deferred Items — quick task 260510-v4v

Pre-existing issues observed during lint run, out of scope for this task.

## Lint NewApi errors (pre-existing, unrelated files)

- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt:177` — `List#removeLast` requires API 35; current minSdk is 23. Fix: replace with `removeAt(backStack.lastIndex)`. Not caused by this task's changes.
- Lint reported 23 errors, 129 warnings overall in the debug variant. The Kotlin `compileDebugKotlin` task succeeded (the only build verification gate the plan demands). All other lint findings are in files this task did not modify.

## Kotlin compiler warnings (pre-existing, unrelated files)

- `data/preferences/LastRegistryPreferencesDataStore.kt:24` — annotation target deprecation (KT-73255).
- `data/preferences/OnboardingPreferencesDataStore.kt:26` — annotation target deprecation (KT-73255).
- `ui/common/chrome/AddActionSheet.kt:175`, `ui/item/add/AddItemScreen.kt:487`, `ui/onboarding/OnboardingScreen.kt:64`, `ui/registry/detail/RegistryDetailHero.kt:204` — deprecated `Icons.*` accessors; should use `Icons.AutoMirrored.*`.
- `ui/registry/detail/RegistryDetailScreen.kt:90`, `ui/registry/detail/ShareBanner.kt:48` — `LocalClipboardManager` deprecated; should use `LocalClipboard`.

These are all in files outside the four files this task modified (AuthScreen.kt, AuthViewModel.kt, the two strings.xml files). Per scope boundary rule, not auto-fixed.
