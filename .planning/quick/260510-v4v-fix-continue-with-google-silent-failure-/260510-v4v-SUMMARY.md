---
phase: quick/260510-v4v
plan: 01
subsystem: auth
tags: [android, firebase-auth, credential-manager, error-handling, logging, i18n, kotlin-coroutines, structured-concurrency]

requires:
  - phase: 02-android-core-auth
    provides: "AuthScreen Continue-with-Google flow using androidx.credentials Credential Manager + AuthViewModel.signInWithGoogle"
provides:
  - "Four-catch chain on AuthScreen's Continue-with-Google flow: GetCredentialCancellationException (silent) → GetCredentialException (log+banner) → kotlinx CancellationException (re-throw) → Exception (log+banner)"
  - "Public `AuthViewModel.setError(message)` API mirroring existing `clearError()`"
  - "Localized `auth_error_google_signin_failed` strings in EN + RO"
  - "Tagged WARN logging under tag `AuthScreen` carrying exception class name + message — turns previously-silent failures into actionable Logcat output"
affects: [on-device-auth-debugging, future-credential-manager-failure-modes, downstream-quick-tasks-needing-visible-auth-errors]

tech-stack:
  added: []
  patterns:
    - "Credential Manager catch-chain ordering: cancellation subclass MUST come first (it's a subclass of the generic GetCredentialException) — otherwise dismissal triggers an error banner"
    - "Coroutine cancellation discipline: catch `kotlinx.coroutines.CancellationException` explicitly and re-throw — never swallow into a generic `catch (e: Exception)` clause"
    - "Diagnosis-enabling pattern: when a failure is silent, the first fix is to surface it (log+UI), not to root-cause it — separates 'what failed' from 'why it failed'"

key-files:
  created: []
  modified:
    - "app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt — replaced 2 silent catch blocks with 4-catch chain + TAG const + 3 new imports"
    - "app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt — added public `setError(message: String)`"
    - "app/src/main/res/values/strings.xml — new key `auth_error_google_signin_failed` (EN)"
    - "app/src/main/res/values-ro/strings.xml — new key `auth_error_google_signin_failed` (RO)"

key-decisions:
  - "Same user-facing string for both error catches (GetCredentialException + generic Exception) — user can't act differently on the two; the Logcat differentiates for the dev via `e::class.simpleName`"
  - "Log.w (warn) not Log.e (error) — this is a user-facing failure path with recovery (retry), not an assertion failure"
  - "Cancellation re-thrown into the coroutine machinery, not consumed into UI — preserves structured concurrency contract"
  - "Out of scope: actual root-cause of any specific exception class. This fix is diagnosis-enabling; the next debugging step uses the now-visible exception name as input."

patterns-established:
  - "When investigating 'button does nothing' on Android, the FIRST fix is to surface the silent catch — Logcat-tag + tagged WARN + UI banner. Don't try to guess the root cause from no data."
  - "Credential Manager catch chain: always order specific-cancellation FIRST so user-dismissal stays silent; specific-failure SECOND with class-name log; coroutine-cancellation THIRD (re-throw); generic LAST."

requirements-completed:
  - QUICK-260510-V4V-01

duration: ~20min
completed: 2026-05-12
---

# Quick Task 260510-v4v: Continue-with-Google Silent-Failure Fix Summary

**Replaced the two silent catch blocks in AuthScreen's Continue-with-Google flow with a four-branch chain that logs the exception class name to Logcat under tag `AuthScreen` and surfaces a localized error banner via the existing `formState.errorMessage` channel — turning invisible auth failures into actionable signal.**

## Performance

- **Duration:** ~20 min
- **Completed:** 2026-05-12
- **Tasks:** 2 (1 auto + 1 human-verify checkpoint)
- **Files modified:** 4

## Accomplishments

- **No more silent failures.** Continue-with-Google failures (Credential Manager rejections, FirebaseAuth errors, unexpected exceptions) now produce both a tagged Logcat WARN with the exception class name + message AND a localized inline error banner under the password fields.
- **Dismissal still silent.** Tapping outside the Credential Manager bottom sheet still produces no log and no banner — `GetCredentialCancellationException` is caught first specifically to preserve this UX (dismissal is intentional; surfacing an error there would be noise).
- **Structured concurrency preserved.** `kotlinx.coroutines.CancellationException` is caught and re-thrown explicitly, never swallowed into the generic `catch (e: Exception)`. This means composition teardown / scope cancellation propagates correctly.
- **Localized.** New `auth_error_google_signin_failed` string in BOTH `values/strings.xml` (EN: "Couldn't sign in with Google. Try again.") and `values-ro/strings.xml` (RO: "Nu ne-am putut conecta cu Google. Încearcă din nou.") — no hardcoded strings per project i18n rule.

## Task Commits

1. **Task 1: Wire logging + user-visible error for Continue-with-Google failures** — `626f716` (fix)
2. **Task 2: Human-verify checkpoint** — no commit (device walkthrough)

**Plan metadata:** committed alongside this SUMMARY.

## Files Created/Modified

- `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` (modified) — added imports for `android.util.Log`, `androidx.credentials.exceptions.GetCredentialCancellationException`, `kotlinx.coroutines.CancellationException`; added `private const val TAG = "AuthScreen"`; replaced the two empty catch blocks with the four-catch chain (GetCredentialCancellationException silent → GetCredentialException log+banner → CancellationException re-throw → Exception log+banner).
- `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` (modified) — added public `fun setError(message: String) { _formState.value = _formState.value.copy(errorMessage = message) }` mirroring the existing `clearError()` pattern.
- `app/src/main/res/values/strings.xml` (modified) — new `<string name="auth_error_google_signin_failed">Couldn\'t sign in with Google. Try again.</string>`.
- `app/src/main/res/values-ro/strings.xml` (modified) — new `<string name="auth_error_google_signin_failed">Nu ne-am putut conecta cu Google. Încearcă din nou.</string>`.

## Automated Gates

| Gate | Command | Result |
| ---- | ------- | ------ |
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | OK |
| Lint (no MissingTranslation) | `./gradlew :app:lintDebug` | OK |
| String key parity (EN+RO) | `grep -c 'auth_error_google_signin_failed' values{,-ro}/strings.xml` | 1 each |
| setError method present | `grep -n 'fun setError' AuthViewModel.kt` | 1 match |
| Catch-chain markers | `grep -n 'GetCredentialCancellationException\|Log.w(TAG\|throw e' AuthScreen.kt` | 4 matches |

## Human Verification

**Status:** approved (verified during the parallel 260510-sai on-device debugging session).

The v4v fix did exactly what it was designed to do: turn invisible Continue-with-Google failures into named, logged, actionable errors. The on-device verification of v4v was actually the inverse of "did v4v work?" — it was "could we have debugged 260510-sai without v4v?" — and the answer was no. The Logcat WARN lines that surfaced during the 260510-sai physical-device session named `Cleartext HTTP not permitted` and `Failed to connect to 192.168.1.10:9099` exceptions one after another. Each made the next layer of the on-device emulator stack diagnosable. Without v4v, the user would still be looking at "Continue with Google button does nothing."

The dismissal-stays-silent contract was also confirmed during that session (no spurious banners when the user closed the Credential Manager sheet).

## Decisions Made

- **Catch-chain ordering is load-bearing.** `GetCredentialCancellationException` is a SUBCLASS of `GetCredentialException`. If the order were reversed, user-dismissal would hit the parent catch and produce a spurious error banner. The comment in `AuthScreen.kt` documents this for future maintainers.
- **`kotlinx.coroutines.CancellationException` (not `java.util.concurrent.CancellationException`).** Re-throwing the kotlinx variant is what `coroutineScope` and structured concurrency check for. Catching the wrong type would leak cancellation into the generic catch and surface a confusing "couldn't sign in" banner on legitimate composition teardown.
- **No `viewModelScope` for `setError`.** The Credential Manager call lives in a `scope.launch` outside the ViewModel. `setError` is a synchronous mutation of `_formState.value` — no coroutine context needed.

## Deviations from Plan

None — plan executed exactly as written. The four-catch chain, the new `setError` method, and the two strings.xml additions matched the plan byte-for-byte.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Known Stubs

None. All four touched files are wired end-to-end and the contract was validated indirectly via the 260510-sai on-device debugging session (which depended entirely on v4v's logging being live).

## Next Phase Readiness

- **Future Credential Manager debugging unblocked.** Any future failure on Continue-with-Google now produces a named exception in Logcat under the `AuthScreen` tag. The class name (`NoCredentialException`, `GetCredentialProviderConfigurationException`, `GetCredentialUnknownException`, etc.) is the input for the next debugging step.
- **Pattern usable for other silent paths.** The same "make the failure visible before fixing it" approach can be applied to any other silent catch in the app (none currently identified, but worth checking if other "button does nothing" reports surface).
- **Latent caveat (out of scope):** The generic `catch (e: Exception)` will also surface FirebaseAuth rejections that happen inside `viewModel.signInWithGoogle()` — those already have their own `result.fold(onFailure = ...)` path that sets `errorMessage`. In rare cases the user might see two banners in quick succession (the FirebaseAuth one + the generic one). If this surfaces in the wild, the fix is to inspect whether `signInWithGoogle` ever throws synchronously vs returning a Result; current code returns a Result so the double-banner case shouldn't happen in practice.

## Self-Check: PASSED

Verified after writing this SUMMARY:
- `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` — exists.
- `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` — exists.
- `app/src/main/res/values/strings.xml` — exists.
- `app/src/main/res/values-ro/strings.xml` — exists.
- Commit `626f716` (Task 1, fix) — present in `git log`.

---

*Phase: quick/260510-v4v*
*Completed: 2026-05-12*
