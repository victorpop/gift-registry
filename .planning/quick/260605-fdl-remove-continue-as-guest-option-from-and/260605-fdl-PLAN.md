---
phase: quick-260605-fdl
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt
autonomous: true
requirements: [QUICK-260605-FDL]
must_haves:
  truths:
    - "The Android auth screen no longer renders a 'Continue as guest' link"
    - "The mode-toggle ghost pill remains as the last interactive element on the screen"
    - "The app still compiles with no dangling imports or unresolved references"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt"
      provides: "Auth screen without the guest TextButton"
      contains: "auth_login_footer"
  key_links:
    - from: "AuthScreen.kt"
      to: "AuthViewModel.continueAsGuest()"
      via: "removed — button no longer calls it (function kept dormant)"
      pattern: "continueAsGuest"
---

<objective>
Hide the "Continue as guest" option on the Android auth screen by removing the single
`TextButton` that renders it. The underlying guest/anonymous-auth machinery stays intact
and dormant so the feature can be trivially re-enabled later.

Purpose: User wants to hide guest entry "for now" without ripping out the supporting
ViewModel/use-case/repository/string layers.
Output: `AuthScreen.kt` with the guest link block deleted, app still compiling.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md

@app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt

<scope_notes>
DO NOT delete or modify any of the following — they remain in place as the re-enable path:
- String resources `auth_guest_tertiary_link` (values/strings.xml ~line 293, values-ro/strings.xml ~line 289)
  and the legacy `auth_continue_as_guest` key. An unused string resource is harmless;
  Android lint may warn but the build does not fail on unused resources.
- `AuthViewModel.continueAsGuest()`, `SignInAnonymousUseCase`, `AuthRepository.signInAnonymously()`,
  `AuthRepositoryImpl`, `FirebaseAuthDataSource.signInAnonymously()`.
- `AuthViewModelTest.kt` (~lines 234-241) tests `continueAsGuest` — keep it valid and passing.
- `isAnonymous` flag on `AuthUiState.Authenticated` / `User`, and `GuestConversionBottomSheet.kt`.

Confirmed by grep: `continueAsGuest()` is referenced ONLY inside the block being removed
(AuthScreen.kt line 377). All other identifiers in the block (`TextButton`, `Text`,
`stringResource`, `typography.bodyXS`, `colors.inkFaint`, `Modifier`) are used elsewhere
in the file, so removing the block leaves no dangling imports.
</scope_notes>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Remove the guest TextButton from AuthScreen</name>
  <files>app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt</files>
  <action>
    Delete the "Tertiary guest link" block — currently lines ~375-387, the comment plus the
    `TextButton(onClick = { viewModel.continueAsGuest() }, ...)` composable that renders
    `stringResource(R.string.auth_guest_tertiary_link)`. It sits between the mode-toggle ghost
    pill `Box` (ends ~line 373) and the trailing `Spacer(modifier = Modifier.height(spacing.gap20))`
    (~line 389).

    After deletion the screen should flow directly from the mode-toggle `Box` to the trailing
    `Spacer`. Remove the leading `// Tertiary guest link ...` comment too. Prefer clean deletion
    over commenting-out — the strings + `continueAsGuest()` ViewModel function remain in place
    as the re-enable path.

    Do NOT touch any imports unless one becomes unused (grep confirms none will:
    every identifier in the removed block is used elsewhere in the file). Do NOT modify
    string resources, the ViewModel, use cases, repository methods, or tests.

    RE-ENABLE NOTE (for future): to restore the guest option, re-add a `TextButton` calling
    `viewModel.continueAsGuest()` and rendering `R.string.auth_guest_tertiary_link` in the same
    position. The string and ViewModel function are still present.
  </action>
  <verify>
    <automated>! grep -q "continueAsGuest" app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt && ./gradlew :app:compileDebugKotlin -q</automated>
  </verify>
  <done>
    The guest `TextButton` block is gone from AuthScreen.kt; `grep continueAsGuest AuthScreen.kt`
    returns nothing; `compileDebugKotlin` succeeds with no unused-import or unresolved-reference
    errors. String resources, ViewModel function, and AuthViewModelTest remain untouched.
  </done>
</task>

</tasks>

<verification>
- `grep -n "continueAsGuest" app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` → no matches
- `grep -rn "auth_guest_tertiary_link" app/src/main/res/values/strings.xml app/src/main/res/values-ro/strings.xml` → still present (unchanged)
- `grep -n "fun continueAsGuest" app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` → still present
- `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
</verification>

<success_criteria>
- Auth screen renders the email/password form and the mode-toggle ghost pill, but NO "Continue as guest" link.
- App compiles cleanly with no dangling imports.
- Guest auth machinery (strings, ViewModel function, use case, repository, datasource, tests) untouched and dormant.
</success_criteria>

<output>
After completion, create `.planning/quick/260605-fdl-remove-continue-as-guest-option-from-and/260605-fdl-SUMMARY.md`
</output>
