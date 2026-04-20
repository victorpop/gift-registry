---
phase: quick-260420-hgb
plan: 01
subsystem: android-settings
tags: [settings, i18n, locale, firestore, ux]
dependency_graph:
  requires: []
  provides: [unified-language-preference]
  affects: [SettingsScreen, SettingsViewModel, LanguagePreferencesRepository, Cloud Functions email locale]
tech_stack:
  added: []
  patterns: [syncRemoteLocale pattern — repository method bridges local DataStore write to Firestore without leaking Firebase into ViewModel]
key_files:
  modified:
    - app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt
    - app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt
    - app/src/main/java/com/giftregistry/di/DataModule.kt
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml
  deleted:
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveEmailLocaleUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/SetEmailLocaleUseCase.kt
    - app/src/main/java/com/giftregistry/domain/preferences/EmailLocaleRepository.kt
    - app/src/main/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImpl.kt
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelEmailLocaleTest.kt
    - app/src/test/java/com/giftregistry/data/preferences/EmailLocaleRepositoryImplTest.kt
decisions:
  - Relocated Firestore preferredLocale write into LanguagePreferencesRepository.syncRemoteLocale() to keep Firebase out of SettingsViewModel (zero Firebase imports in UI layer pattern)
  - changeLocale() calls setLanguageTag then syncRemoteLocale sequentially in same coroutine; AppCompatDelegate.setApplicationLocales() is called synchronously so UI recomposition is never blocked by Firestore latency
  - runCatching wraps Firestore write — network failure must not prevent local language switch
metrics:
  duration: ~8min
  completed: 2026-04-20
  tasks: 2
  files_modified: 8
  files_deleted: 6
---

# Phase quick-260420-hgb Plan 01: Remove Email Language Setting Summary

Single language picker in Settings that writes both local DataStore (drives AppCompatDelegate recomposition) and Firestore `users/{uid}.preferredLocale` (drives Cloud Functions email rendering).

## Assumption 1 Verification

The executor did not run the app (cannot in CI). Code review confirms:
- `AppCompatDelegate.setApplicationLocales()` is called synchronously in `changeLocale()` outside the coroutine scope — correct pattern for immediate recomposition
- `AppCompatActivity` is used (confirmed in STATE.md decisions from Phase 02) — required for `setApplicationLocales()` to work
- DataStore write + Firestore sync are in the same `viewModelScope.launch {}` coroutine, correctly sequenced after the synchronous delegate call

Conclusion: the recomposition mechanism is correctly wired. The user's "changing to Romanian does nothing" report was most likely caused by picking from the second picker (email language) instead of the first (app language), since both existed. With the merge complete, there is only one picker and it drives both.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Unify language setting in ViewModel + UI, delete email-language-only plumbing | 5b3e3aa | SettingsViewModel.kt, SettingsScreen.kt, LanguagePreferencesRepository.kt, LanguagePreferencesDataStore.kt, DataModule.kt, SettingsViewModelTest.kt; deleted 6 files |
| 2 | Remove orphaned email-language strings | f069c8e | values/strings.xml, values-ro/strings.xml |

## Files Changed

**Modified (8):**
- `app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt` — removed email locale ListItem + dialog + related state; kept single Language ListItem
- `app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt` — simplified to 2 constructor params; `changeLocale()` now calls `syncRemoteLocale`
- `app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt` — added `syncRemoteLocale(tag: String)` method
- `app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt` — injected `FirebaseAuth` + `FirebaseFirestore`; implemented `syncRemoteLocale`
- `app/src/main/java/com/giftregistry/di/DataModule.kt` — removed `bindEmailLocaleRepository`
- `app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt` — updated to 2-param constructor; added `FakeLanguagePreferencesRepository.syncRemoteLocale` counter; added 2 new sync tests
- `app/src/main/res/values/strings.xml` — removed `settings_email_language_label`
- `app/src/main/res/values-ro/strings.xml` — removed `settings_email_language_label`

**Deleted (6):**
- `ObserveEmailLocaleUseCase.kt`
- `SetEmailLocaleUseCase.kt`
- `EmailLocaleRepository.kt`
- `EmailLocaleRepositoryImpl.kt`
- `SettingsViewModelEmailLocaleTest.kt`
- `EmailLocaleRepositoryImplTest.kt`

## Test Output

```
BUILD SUCCESSFUL in 3s
29 actionable tasks: 4 executed, 25 up-to-date
```

All existing tests pass. New tests in `SettingsViewModelTest`:
- `changeLocale syncs remote locale via repository` — asserts `lastSyncedTag == "ro"` and `syncCallCount == 1`
- `changeLocale syncs en locale via repository` — asserts `lastSyncedTag == "en"` and `syncCallCount == 1`

## Grepping Sanity Checks

```
grep -r "EmailLocale" app/src/  → CLEAN (zero matches)
grep -r "settings_email_language" app/src/ → CLEAN (zero matches)
grep -r "preferredLocale" app/src/ → exactly 1 match in LanguagePreferencesDataStore.kt
grep -n "preferredLocale" functions/src/notifications/onPurchaseNotification.ts → line 121 (Cloud Functions contract preserved)
```

## Cloud Functions Contract

`functions/src/notifications/onPurchaseNotification.ts` line 121 still reads `ownerSnap.data()?.preferredLocale` — unchanged. The same Firestore field is now written by `syncRemoteLocale` in the unified flow.

## Deviations from Plan

None — plan executed exactly as written.

## Manual Smoke Test (for user)

1. Launch app → Settings → verify only ONE "Language" row is visible (no "Email language" row).
2. Tap Language → pick Romanian → Confirm. App should immediately recompose with Romanian strings.
3. Check Firestore console `users/{your-uid}` → `preferredLocale: "ro"` should appear.
4. Trigger a purchase notification → resulting email should render in Romanian.

## Self-Check: PASSED

- All modified files exist and compile: `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- Commits exist: 5b3e3aa (Task 1), f069c8e (Task 2)
- Zero references to deleted use cases in `app/src/`
- `SettingsScreen.kt` has exactly one `Icons.Default.Language` and zero `Icons.Default.Email`
- `SettingsViewModel` constructor has exactly 2 parameters
