---
phase: 10-onboarding-home-redesign
plan: 03
subsystem: ui
tags: [compose, jetpack-compose, giftmaison-theme, auth, kotlin, annotated-string]

# Dependency graph
requires:
  - phase: 10-02
    provides: GoogleBanner, FocusedFieldCaret, ConcentricRings composables; AuthHeadline/AuthFormState stubs
  - phase: 08-design-system
    provides: GiftMaisonTheme, GiftMaisonColors, GiftMaisonTypography, GiftMaisonShapes, GiftMaisonSpacing
provides:
  - authHeadlineAnnotatedString(prefix, accent, ink, accentColor, inkSoft): AnnotatedString — pure-Kotlin unit-tested factory
  - AuthHeadline @Composable — displayL italic serif with inkSoft/ink/accent spans
  - AuthViewModel.updateFirstName + updateLastName — additive methods preserving existing contract
  - AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true — flips default mode from sign-in to sign-up
  - AuthScreen.kt GiftMaison re-skin — wordmark/headline/GoogleBanner/divider/fields/CTA/Terms/ghost-pill/guest-link
  - 23 auth_* string keys in EN + 23 in RO (46 total string entries)
  - GoogleBanner.kt rewired: auth_google_sign_in_button -> auth_google_cta
affects:
  - 10-04 (Home screen integration — builds on same pattern)
  - AppNavigation.kt (unchanged — AuthScreen signature preserved)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "authHeadlineAnnotatedString mirrors wordmarkAnnotatedString: pure-Kotlin AnnotatedString factory tested without Compose runtime"
    - "No Scaffold/SnackbarHost in AuthScreen — inline warn-banner (colors.warn 0.15 alpha) replaces snackbar per RESEARCH.md Pattern 2"
    - "AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true top-level const pins default for AuthFormStateTest without reading Compose state"
    - "mutableStateOf(AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE) pattern — const replaces hardcoded literal for testability"
    - "FocusedFieldCaret in trailingIcon slot with emailIsFocused && email.isEmpty() condition — caret only when field focused and empty"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt (stubs already present from Plan 02 — no change needed)
    - app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt (added updateFirstName + updateLastName)
    - app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt (full GiftMaison re-skin)
    - app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt (string rewire auth_google_sign_in_button -> auth_google_cta)
    - app/src/main/res/values/strings.xml (23 new auth_* keys in EN)
    - app/src/main/res/values-ro/strings.xml (23 matching auth_* keys in RO)

key-decisions:
  - "AuthUiState.kt already had firstName/lastName + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE from Plan 02 stubs — no edit needed for Task 1"
  - "mutableStateOf(false) retained for passwordVisible (separate state from isSignUpMode) — acceptance criteria check is specific to isSignUpMode"
  - "GoogleBanner.kt KDoc comment cleaned: removed the text 'auth_google_sign_in_button' to satisfy grep-count=0 acceptance check"
  - "Terms clickable modifier: single-tap opens auth_terms_url; if URL blank (current state) it no-ops silently — Privacy URL handled same way"
  - "supportingText @Composable lambda pattern for password helper text — shows in sign-up mode only"

patterns-established:
  - "Inline error banner via formState.errorMessage?.let — no SnackbarHostState needed"
  - "Mode-aware string resources: R.string.auth_or_sign_up_email_divider vs auth_or_sign_in_email_divider"
  - "Ghost pill footer toggle: TextButton + clip(shapes.pill) + clearError() on mode flip"

requirements-completed:
  - SCR-06

# Metrics
duration: 4min
completed: 2026-04-21
---

# Phase 10 Plan 03: SCR-06 Auth screen re-skin Summary

**GiftMaison-themed AuthScreen with italic-accent headline, GoogleBanner, first/last name sign-up fields + FocusedFieldCaret, ink pill CTA, Terms line, ghost Log-in footer pill, and AUTH-05 guest link — flipping AuthHeadlineTest x6 + AuthFormStateTest x6 RED → GREEN**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-04-21T17:10:16Z
- **Completed:** 2026-04-21T17:14:45Z
- **Tasks:** 2 (both complete)
- **Files modified:** 6 (1 created, 5 modified)

## Accomplishments

- Implemented `authHeadlineAnnotatedString()` with 3-colour span strategy (inkSoft/ink/accent), flipping 6 AuthHeadlineTest assertions RED → GREEN
- Extended AuthViewModel with `updateFirstName` + `updateLastName` methods (backward-compatible additive); `AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true` already in AuthUiState.kt from Plan 02 stub — AuthFormStateTest x6 now GREEN
- Added 23 auth_* string keys to EN strings.xml + 23 matching RO strings.xml (46 total entries covering headline, subline, Google CTA, dividers, field labels, CTAs, Terms, footer, error messages)
- Rewrote AuthScreen.kt top-to-bottom: dropped Scaffold/SnackbarHost; GiftMaisonTheme exclusively; preserves AppNavigation.kt signature and CredentialManager Google OAuth flow
- Rewired GoogleBanner.kt from provisional `auth_google_sign_in_button` to final `auth_google_cta`
- Full unit suite GREEN; debug APK BUILD SUCCESSFUL

## Task Commits

Each task was committed atomically:

1. **Task 1: AuthHeadline + updateFirstName/lastName + 23 auth_* string keys** - `114e246` (feat)
2. **Task 2: SCR-06 AuthScreen GiftMaison re-skin + GoogleBanner string rewire** - `3398cbe` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified

- `app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt` — authHeadlineAnnotatedString factory + AuthHeadline @Composable
- `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` — updateFirstName + updateLastName methods added
- `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` — full GiftMaison re-skin (drop Scaffold; ink pill; inline error; AUTH-05 guest link)
- `app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` — string ref rewired to auth_google_cta
- `app/src/main/res/values/strings.xml` — 23 new auth_* keys (EN)
- `app/src/main/res/values-ro/strings.xml` — 23 new auth_* keys (RO)

## Decisions Made

- AuthUiState.kt stubs from Plan 02 were already correct (firstName/lastName + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true) — no edit needed
- `mutableStateOf(false)` retained for `passwordVisible` (password eye toggle) — this is separate from `isSignUpMode` which reads from the const
- GoogleBanner.kt KDoc comment updated to avoid grep false-positive on old key name
- Terms/Privacy URLs are intentionally blank strings (`auth_terms_url`, `auth_privacy_url`) — legal content is out of v1.1 scope; onClick no-ops when URL blank

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

- **auth_terms_url / auth_privacy_url**: Empty string placeholder keys in both strings.xml files. The Terms/Privacy clickable text uses `if (termsUrl.isNotBlank())` guard — no-op until real legal URLs are provided. Tracked as follow-up.
- **Firebase displayName update**: `firstName`/`lastName` captured in form state but NOT yet written to `Firebase User.displayName` via `UserProfileChangeRequest`. This is a deferred follow-up per RESEARCH.md Open Question 2 — single non-visual enhancement that can ship post-phase without changing the AuthScreen UI.

## Issues Encountered

None. Compilation and all tests passed on first attempt.

## Known Follow-ups

1. **firstName/lastName → Firebase User.displayName**: Write `UserProfileChangeRequest` call in signUp() or via a separate profile-update use case. Non-visual, deferred per plan spec.
2. **Terms + Privacy URLs**: Populate `auth_terms_url` and `auth_privacy_url` with real legal content once available.
3. **Old string key cleanup**: Remove `auth_email_placeholder`, `auth_or_email_divider`, `auth_sign_in_title`, `auth_sign_up_title`, `auth_sign_in_button`, `auth_sign_up_button`, `auth_or_divider`, `auth_support`, `auth_terms_of_service`, `auth_privacy_policy`, `auth_subtitle`, `auth_forgot_password`, `auth_no_account_prompt`, `auth_have_account_prompt`, `auth_sign_in_no_account_hint` — no longer referenced by AuthScreen; confirm no other callers before removing.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **Plan 04 (Home screen integration):** AvatarButton + SegmentedTabs + TabFilters ready from Plan 02. Plan 04 rewires `auth_settings_title` (AvatarButton provisional) to `home_avatar_content_desc`.
- Phase 10 Wave 1 complete: both SCR-06 (Auth) and SCR-07 (Home) screens can now be integrated.

---
*Phase: 10-onboarding-home-redesign*
*Completed: 2026-04-21*
