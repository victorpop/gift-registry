---
phase: 02-android-core-auth
plan: 04
subsystem: android-settings-i18n
tags: [settings, i18n, locale, language-picker, guest-conversion, navigation3, datastore]
dependency_graph:
  requires: ["02-03"]
  provides: ["settings-screen", "locale-persistence", "guest-conversion-bottomsheet"]
  affects: ["MainActivity", "AppNavigation", "Phase-4-reservation-flow"]
tech_stack:
  added: []
  patterns:
    - "SharingStarted.Eagerly for StateFlow that must emit immediately on ViewModel init"
    - "runBlocking in onCreate before super to restore locale without flicker"
    - "Hilt @Inject field in AppCompatActivity subclass"
key_files:
  created:
    - app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/giftregistry/ui/auth/GuestConversionBottomSheet.kt
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/MainActivity.kt
decisions:
  - "SharingStarted.Eagerly instead of WhileSubscribed(5000) for SettingsViewModel.currentLocale — ensures immediate emission on ViewModel init so tests and UI both see the stored value without requiring a subscriber"
metrics:
  duration: "~4 minutes"
  completed: "2026-04-05"
  tasks: 2
  files: 6
---

# Phase 02 Plan 04: Settings Screen, Language Picker, and Guest Conversion Summary

**One-liner:** Settings screen with DataStore-persisted language picker (English/Romanian), synchronous locale restoration on MainActivity startup, and standalone GuestConversionBottomSheet for Phase 4.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | SettingsViewModel, SettingsScreen, GuestConversionBottomSheet | 9917043 | SettingsViewModel.kt, SettingsScreen.kt, GuestConversionBottomSheet.kt, SettingsViewModelTest.kt |
| 2 | Wire SettingsScreen into Navigation3, locale restoration in MainActivity | c36e74f | AppNavigation.kt, MainActivity.kt |

## What Was Built

**SettingsViewModel** manages language preference state with `SharingStarted.Eagerly` so the current locale is available immediately. `changeLocale()` persists via DataStore and calls `AppCompatDelegate.setApplicationLocales()` which triggers activity recreation for immediate UI update.

**SettingsScreen** renders a `Scaffold` with `TopAppBar` (back navigation), a `ListItem` showing current language, and an `AlertDialog` with two `RadioButton` options (English/Romanian). All strings come from `strings.xml` — no hardcoded text.

**GuestConversionBottomSheet** is a standalone `ModalBottomSheet` composable with title, body, "Create Account" button, and "Maybe later" dismiss. Designed to be triggered by Phase 4 reservation flow without any modification to this component.

**AppNavigation** SettingsKey entry now renders the real `SettingsScreen(onBack = { backStack.removeLast() })` instead of the placeholder text.

**MainActivity** injects `LanguagePreferencesRepository` via Hilt and restores the persisted locale synchronously via `runBlocking` before `super.onCreate()` — this prevents a flash of the wrong locale on startup.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Changed SharingStarted.WhileSubscribed to SharingStarted.Eagerly**
- **Found during:** Task 1 GREEN phase (test run)
- **Issue:** `SharingStarted.WhileSubscribed(5000)` delays upstream collection until a subscriber attaches. In Test 2, the repo emitted "ro" before ViewModel construction but `currentLocale.value` returned the initial "en" because no subscriber was active to trigger upstream collection.
- **Fix:** Changed to `SharingStarted.Eagerly` — the upstream flow starts immediately on ViewModel init, so the stored locale is reflected in `currentLocale.value` before any UI subscriber attaches.
- **Files modified:** SettingsViewModel.kt
- **Commit:** included in 9917043

## Verification

All tests passed:
- `SettingsViewModelTest` — 3 tests (locale defaults to en, emits stored ro, changeLocale calls repository)
- `AuthViewModelTest` — 8 tests (all passing, no regression)
- `assembleDebug` — BUILD SUCCESSFUL

## Known Stubs

None — all functionality is fully wired.

## Self-Check

Created files:
- app/src/main/java/com/giftregistry/ui/settings/SettingsViewModel.kt — FOUND
- app/src/main/java/com/giftregistry/ui/settings/SettingsScreen.kt — FOUND
- app/src/main/java/com/giftregistry/ui/auth/GuestConversionBottomSheet.kt — FOUND
- app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt — FOUND

Commits:
- c42f7f0 — test(02-04): add failing tests for SettingsViewModel — FOUND
- 9917043 — feat(02-04): SettingsViewModel, SettingsScreen, GuestConversionBottomSheet — FOUND
- c36e74f — feat(02-04): wire SettingsScreen into Navigation3, add locale restoration to MainActivity — FOUND

## Self-Check: PASSED
