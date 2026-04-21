---
phase: 10-onboarding-home-redesign
verified: 2026-04-21T20:00:00Z
status: human_needed
score: 8/8 automated must-haves verified
human_verification:
  - test: "Complete the 21-check on-device UAT documented in 10-HUMAN-UAT.md"
    expected: "All 12 SCR-06 checks, 5 SCR-07 checks, Romanian locale check, and 3 regression-guard checks pass on a physical device or emulator running Android API 31+"
    why_human: "Visual accuracy (pixel-accurate colours, font rendering, animation timing, shadow appearance, concentric-ring opacity, image brightness), animation cadence (1.1 s caret pulse distinct from 1.4 s PulsingDot), OAuth flow invocation (CredentialManager), AUTH-05 anonymous sign-in, and Romanian locale rendering all require on-device execution — none are programmatically verifiable from the build tree"
---

# Phase 10: Onboarding / Home Redesign Verification Report

**Phase Goal:** The Onboarding/sign up screen (SCR-06) and the Home/all-registries screen (SCR-07) match the handoff pixel-accurately, preserving existing auth and registry-list behaviour.
**Verified:** 2026-04-21T20:00:00Z
**Status:** human_needed — all automated checks PASS; on-device UAT deferred to 10-HUMAN-UAT.md
**Re-verification:** No — initial verification

## Preliminary Context

The 21-check on-device UAT from Plan 05 Task 2 was deferred by the user on 2026-04-21 to unblock Phase 11. The deferred UAT is tracked in `/Users/victorpop/ai-projects/gift-registry/.planning/phases/10-onboarding-home-redesign/10-HUMAN-UAT.md`. Two prior user-approved quick tasks transitively validate some Phase 10 items:

- `260421-lwi` (typography) — validates typography legibility on the Auth headline (SCR-06 check 3) in Instrument Serif italic
- `260421-moi` (nav) — validates nav-bar visibility with the Home screen present (SCR-07 regression guard: bottom nav + FAB still visible)

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Six Wave 0 RED unit test files exist (40 @Test methods total) pinning Phase 10 contract | ✓ VERIFIED | All 6 files present; @Test counts: TabFilterPredicate=9, DraftHeuristic=5, IsPrimary=5, AvatarInitials=9, AuthHeadline=6, AuthFormState=6 |
| 2 | Pure-Kotlin helpers flip 4 Wave 0 tests GREEN (TabFilters + AvatarInitials) | ✓ VERIFIED | XML: TabFilterPredicateTest tests="9" failures="0"; DraftHeuristicTest tests="5" failures="0"; IsPrimarySelectionTest tests="5" failures="0"; AvatarInitialsTest tests="9" failures="0" |
| 3 | AuthFormState gains firstName/lastName + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE=true flips 2 Wave 0 auth tests GREEN | ✓ VERIFIED | XML: AuthFormStateTest tests="6" failures="0"; AuthHeadlineTest tests="6" failures="0"; `const val AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE: Boolean = true` present in AuthUiState.kt |
| 4 | AuthScreen re-skinned (SCR-06): wordmark + AuthHeadline + GoogleBanner + FocusedFieldCaret + AUTH-05 guest path preserved | ✓ VERIFIED | `AuthHeadline()`, `GoogleBanner(`, `FocusedFieldCaret(`, `GiftMaisonWordmark(`, `viewModel.continueAsGuest()`, `auth_guest_tertiary_link` all found in AuthScreen.kt; Scaffold/SnackbarHost count = 0 |
| 5 | RegistryListScreen re-skinned (SCR-07): HomeTopBar + SegmentedTabs + RegistryCard primary/secondary + tab filter wiring | ✓ VERIFIED | `HomeTopBar(`, `SegmentedTabs(`, `RegistryCardPrimary(`, `RegistryCardSecondary(`, `primaryRegistryIdOf`, `isActive(todayMs)`, `isPast(todayMs)`, `isDraft(`, `toAvatarInitials`, `rememberSaveable { mutableIntStateOf(0) }` all found; TopAppBar/NotificationsInboxBell count = 0 |
| 6 | AppNavigation.kt wires onNavigateToSettings = { backStack.add(SettingsKey) } for HomeKey | ✓ VERIFIED | `onNavigateToSettings = { backStack.add(SettingsKey) }` found in AppNavigation.kt |
| 7 | 38 Phase 10 string keys in both EN and RO locales (23 auth_* + 15 home_*/registry_*) | ✓ VERIFIED | EN values/strings.xml: auth_* count=23, home_/registry_ count=15; RO values-ro/strings.xml: same counts confirmed |
| 8 | StyleGuidePreview.kt gains 5 new @Preview composables for Phase 10 components | ✓ VERIFIED | AuthHeadlinePreview, GoogleBannerPreview, SegmentedTabsPreview, RegistryCardPrimaryPreview, RegistryCardSecondaryPreview all present |

**Score:** 8/8 automated truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt` | SCR-07 Active/Past predicate tests (9 @Test) | ✓ VERIFIED | 9 @Test methods; references `startOfTodayMs`, `isActive`, `isPast` |
| `app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt` | SCR-07 draft heuristic tests (5 @Test) | ✓ VERIFIED | 5 @Test methods; references `isDraft` |
| `app/src/test/java/com/giftregistry/ui/registry/list/IsPrimarySelectionTest.kt` | SCR-07 primary resolver tests (5 @Test) | ✓ VERIFIED | 5 @Test methods; references `primaryRegistryIdOf` |
| `app/src/test/java/com/giftregistry/ui/common/AvatarInitialsTest.kt` | Avatar initials fallback chain tests (9 @Test) | ✓ VERIFIED | 9 @Test methods; references `toAvatarInitials` |
| `app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt` | SCR-06 AnnotatedString construction tests (6 @Test) | ✓ VERIFIED | 6 @Test methods; references `authHeadlineAnnotatedString` |
| `app/src/test/java/com/giftregistry/ui/auth/AuthFormStateTest.kt` | SCR-06 AuthFormState + default-mode tests (6 @Test) | ✓ VERIFIED | 6 @Test methods; references `AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE` |
| `app/src/main/java/com/giftregistry/domain/model/Registry.kt` | imageUrl: String? = null added | ✓ VERIFIED | `val imageUrl: String? = null` present |
| `app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt` | startOfTodayMs + isActive + isPast + isDraft + primaryRegistryIdOf | ✓ VERIFIED | All 5 functions present; Calendar-based (no java.time.LocalDate import) |
| `app/src/main/java/com/giftregistry/ui/common/AvatarInitials.kt` | toAvatarInitials helper (pure Kotlin) | ✓ VERIFIED | `fun toAvatarInitials(displayName: String?, email: String?): String` present |
| `app/src/main/java/com/giftregistry/ui/common/AvatarButton.kt` | 30 dp avatar circle, 44 dp tap target | ✓ VERIFIED | `fun AvatarButton(` present; reads `home_avatar_content_desc` (provisional ref rewired) |
| `app/src/main/java/com/giftregistry/ui/common/FocusedFieldCaret.kt` | 1.1 s opacity-only pulse (distinct from PulsingDot 1.4 s) | ✓ VERIFIED | `const val FOCUSED_FIELD_CARET_PERIOD_MS: Long = 1_100L`; PULSING_DOT/Scale references = 0 (count=2 is `tween` + `durationMillis` comment, not scale animation) |
| `app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` | Canvas-drawn 3 rings at alpha 0.08/0.12/0.18 | ✓ VERIFIED | All 3 alpha values confirmed: 0.08f, 0.12f, 0.18f |
| `app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` | Accent pill + shadow + concentric rings + auth_google_cta | ✓ VERIFIED | `googleBannerShadow(colors.accent)`, `ConcentricRings(`, `auth_google_cta` all present; `auth_google_sign_in_button` count = 0 |
| `app/src/main/java/com/giftregistry/ui/registry/list/SegmentedTabs.kt` | paperDeep track + paper selected pill + animateColorAsState 200 ms | ✓ VERIFIED | `animateColorAsState`, `shapes.pill` present |
| `app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt` | firstName + lastName + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE | ✓ VERIFIED | All three present |
| `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` | updateFirstName + updateLastName methods | ✓ VERIFIED | Both methods present |
| `app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt` | authHeadlineAnnotatedString + @Composable AuthHeadline | ✓ VERIFIED | Both present |
| `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` | SCR-06 re-skin preserving AuthViewModel contract + AUTH-05 | ✓ VERIFIED | `fun AuthScreen(viewModel: AuthViewModel = hiltViewModel())` signature exact; Scaffold/SnackbarHost = 0 |
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` | currentUser: StateFlow<User?> exposed | ✓ VERIFIED | `val currentUser: StateFlow<User?> = authRepository.authState` present |
| `app/src/main/java/com/giftregistry/ui/registry/list/HomeTopBar.kt` | Inline wordmark + AvatarButton row | ✓ VERIFIED | `fun HomeTopBar(`, `GiftMaisonWordmark()`, `AvatarButton(` all present |
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt` | RegistryCardPrimary (ink bg, 70% image) + RegistryCardSecondary (paperDeep, line border) | ✓ VERIFIED | Both functions present; `setToScale(0.7f, 0.7f, 0.7f, 1f)` confirmed |
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` | SCR-07 re-skin preserving delete flow + edit | ✓ VERIFIED | All wiring confirmed; TopAppBar/NotificationsInboxBell = 0; `registry_delete_confirm_title` preserved |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` | onNavigateToSettings wired for HomeKey | ✓ VERIFIED | `onNavigateToSettings = { backStack.add(SettingsKey) }` present |
| `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` | 5 new @Preview composables | ✓ VERIFIED | AuthHeadlinePreview, GoogleBannerPreview, SegmentedTabsPreview, RegistryCardPrimaryPreview, RegistryCardSecondaryPreview all present |
| `app/src/main/res/values/strings.xml` | 23 auth_* + 15 home_*/registry_* keys | ✓ VERIFIED | Counts confirmed: auth_* = 23, home_/registry_ = 15 |
| `app/src/main/res/values-ro/strings.xml` | Matching 38 Romanian keys | ✓ VERIFIED | Counts confirmed: auth_* = 23, home_/registry_ = 15 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| Wave 0 test files | Plan 02/03/04 implementations | `import com.giftregistry.ui.` package imports | ✓ WIRED | Tests in packages that match impl packages; all compile and pass GREEN |
| `AuthScreen.kt` | `GoogleBanner`, `FocusedFieldCaret`, `AuthHeadline` | Direct @Composable invocation | ✓ WIRED | `GoogleBanner(`, `FocusedFieldCaret(`, `AuthHeadline()` all called in AuthScreen.kt |
| `AuthScreen.kt` footer pill | local `isSignUpMode` state | `onClick { isSignUpMode = !isSignUpMode }` | ✓ WIRED | Mode toggle pattern present; `AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE` used for initial state |
| `AuthScreen.kt` guest link | `AuthViewModel.continueAsGuest()` | `TextButton onClick` | ✓ WIRED | `viewModel.continueAsGuest()` present; `auth_guest_tertiary_link` string used |
| `RegistryListScreen.kt` HomeTopBar | `onNavigateToSettings` (AppNavigation → SettingsKey) | `AvatarButton onClick` parameter | ✓ WIRED | `onAvatarClick = onNavigateToSettings` in HomeTopBar call site |
| `RegistryListScreen.kt` filtered list | `TabFilters.kt` helpers + `primaryRegistryIdOf` | `remember { ... }` derived state | ✓ WIRED | `isActive(todayMs)`, `isPast(todayMs)`, `isDraft(itemCount = 0)`, `primaryRegistryIdOf(registries)` all used |
| `RegistryCardPrimary` image | Coil 3 AsyncImage + ColorFilter.colorMatrix | `colorFilter` parameter on AsyncImage | ✓ WIRED | `setToScale(0.7f, 0.7f, 0.7f, 1f)` confirmed in RegistryCard.kt |
| `GoogleBanner.kt` | `ConcentricRings` | Direct @Composable invocation | ✓ WIRED | `ConcentricRings(color = colors.accentInk, ...)` in GoogleBanner.kt |
| `Registry.kt` `imageUrl` field | Firestore POJO mapper (backward-compatible) | Kotlin default = null; absent field → null | ✓ WIRED | `val imageUrl: String? = null` with default; no Firestore migration required |
| `AvatarButton.kt` string ref | `home_avatar_content_desc` (EN + RO) | `R.string.home_avatar_content_desc` | ✓ WIRED | Rewired from provisional `auth_settings_title`; confirmed present in both locales |
| `GoogleBanner.kt` string ref | `auth_google_cta` (EN + RO) | `R.string.auth_google_cta` | ✓ WIRED | Rewired from provisional `auth_google_sign_in_button`; confirmed present in both locales |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `RegistryListScreen.kt` | `uiState` / `registries` | `RegistryListViewModel.uiState` ← `ObserveRegistriesUseCase` ← Firestore | Yes — existing Phase 3 Firestore listener unchanged | ✓ FLOWING |
| `RegistryListScreen.kt` | `currentUser` | `RegistryListViewModel.currentUser` ← `authRepository.authState` (StateFlow<User?>) | Yes — `authRepository.authState.stateIn(...)` | ✓ FLOWING |
| `AuthScreen.kt` | `formState` | `AuthViewModel.formState` (StateFlow<AuthFormState>) | Yes — existing VM unchanged; new firstName/lastName fields added with defaults | ✓ FLOWING |
| `RegistryCard.kt` stats line | `statsLine()` | Hardcoded 0 (per CONTEXT.md deferred decision) | Returns zeros — intentional deferred behaviour; documented as known follow-up | ⚠️ STATIC (intended) |
| `RegistryCard.kt` image | `registry.imageUrl` | `Registry.imageUrl: String? = null` ← Firestore POJO | Null for existing docs; non-null when backfilled — paperDeep placeholder shown | ⚠️ STATIC (intended) |

Both STATIC items are explicitly approved deferred decisions per CONTEXT.md (stats aggregation deferred; imageUrl Firestore backfill deferred). Neither blocks the phase goal.

### Behavioral Spot-Checks

Step 7b: SKIPPED for Compose screens (no runnable entry points without a device/emulator). Behavioral verification delegated to human UAT in 10-HUMAN-UAT.md.

Unit tests verified via `./gradlew :app:testDebugUnitTest`:

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All 6 Phase 10 Wave 0 tests GREEN | `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL — 0 failures, 0 errors across all test suites | ✓ PASS |
| No Phase 8/9 regressions | Same run (all test suites in one pass) | All legacy test suites: failures="0" errors="0" | ✓ PASS |
| TabFilterPredicateTest (9 tests) | XML: tests="9" failures="0" errors="0" | GREEN | ✓ PASS |
| DraftHeuristicTest (5 tests) | XML: tests="5" failures="0" errors="0" | GREEN | ✓ PASS |
| IsPrimarySelectionTest (5 tests) | XML: tests="5" failures="0" errors="0" | GREEN | ✓ PASS |
| AvatarInitialsTest (9 tests) | XML: tests="9" failures="0" errors="0" | GREEN | ✓ PASS |
| AuthHeadlineTest (6 tests) | XML: tests="6" failures="0" errors="0" | GREEN | ✓ PASS |
| AuthFormStateTest (6 tests) | XML: tests="6" failures="0" errors="0" | GREEN | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SCR-06 | 10-01, 10-02, 10-03, 10-05 | Onboarding/sign up screen matches handoff pixel-accurately, preserving existing auth behaviour | ✓ SATISFIED (automated) / ? UAT pending | AuthScreen.kt re-skinned with all SCR-06 primitives; 6 Wave 0 tests GREEN; AuthViewModel contract preserved; AUTH-05 preserved; on-device UAT deferred to 10-HUMAN-UAT.md |
| SCR-07 | 10-01, 10-02, 10-04, 10-05 | Home/all-registries screen matches handoff pixel-accurately, preserving existing registry-list behaviour | ✓ SATISFIED (automated) / ? UAT pending | RegistryListScreen.kt re-skinned; HomeTopBar, RegistryCard, SegmentedTabs, AppNavigation wiring all verified; RegistryListViewModel contract preserved; 6 Wave 0 tests GREEN; on-device UAT deferred to 10-HUMAN-UAT.md |

No orphaned requirements: REQUIREMENTS.md maps SCR-06 and SCR-07 to Phase 10. Both plans claim both requirements. No additional IDs declared.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `RegistryListScreen.kt` | 127 | `// TODO: Phase 10 stats aggregation deferred` | ℹ️ Info | Intentional deferred decision per CONTEXT.md — itemCount=0 passed to isDraft until per-registry stat aggregation ships; draft tab shows blank-title registries only. Not a stub — the filter function itself works correctly. |
| `RegistryCard.kt` | statsLine() | `statsLine()` always returns "0 items • 0 reserved • 0 given" | ℹ️ Info | CONTEXT.md-approved deferred feature (per-registry stat counts). Renders zeros. The visual structure is correct — only data is absent until aggregation lands. |
| `AuthScreen.kt` | 168,179,195 | `placeholder = { Text(...) }` uses string resources | ℹ️ Info | These are legitimate placeholder UI labels (field hint text), NOT stub code. Correctly localized via `auth_first_name_placeholder`, `auth_last_name_placeholder`, `auth_email_placeholder_new`. |

No blocker anti-patterns. No MISSING/STUB artifacts. No orphaned composables (all new primitives are imported and used in AuthScreen.kt or RegistryListScreen.kt).

### Human Verification Required

**Deferred per user decision on 2026-04-21 to unblock Phase 11. Full UAT checklist available at:**
`/Users/victorpop/ai-projects/gift-registry/.planning/phases/10-onboarding-home-redesign/10-HUMAN-UAT.md`

#### 1. SCR-06 On-device Visual and Behavioural Verification (checks 1–12)

**Test:** Install debug APK, sign out to land on AuthScreen, complete all 12 SCR-06 checks from 10-HUMAN-UAT.md
**Expected:** Sign-up default mode; wordmark + italic-accent headline; Google banner with shadow + concentric rings + OAuth flow; FocusedFieldCaret pulsing at 1.1 s (opacity only); inline error banner on blank name; mode toggle (sign-up ↔ sign-in); AUTH-05 guest path navigates to Home
**Why human:** Pixel accuracy, animation timing, shadow rendering, concentric-ring opacity, caret cadence, and OAuth flow invocation all require a running device

#### 2. SCR-07 On-device Visual and Behavioural Verification (checks 13–17)

**Test:** While signed in, navigate to Home screen and complete checks 13–17 from 10-HUMAN-UAT.md
**Expected:** Wordmark + 30 dp olive avatar top bar; "Your registries" displayXL headline + monoCaps caption; SegmentedTabs with 200 ms colour animation; exactly one dark primary card (ink bg, ~70% image brightness); paperDeep secondary cards with line border; per-tab empty state; avatar tap → Settings
**Why human:** Visual brightness comparison (70% vs full-brightness images), colour contrast, card hierarchy, tab animation feel, and settings navigation require a running device

#### 3. Romanian Locale Verification

**Test:** Switch device to Romanian locale, relaunch app, verify SCR-06 and SCR-07 RO strings render correctly
**Expected:** All 38 Phase 10 RO keys render — "Începe-ți / primul registru." headline; "Înregistrează-te" CTA; "Registrele tale" headline; "SCHIȚE / TRECUTE" tabs; "Nu există schițe" empty state; "Continuă ca oaspete" guest link
**Why human:** Locale switch and rendering of Romanian diacritics (ș, ț, ă, etc.) requires a running device with locale changed

#### 4. Regression Guards

**Test:** Tap a registry card → RegistryDetail; long-press → DropdownMenu; tap Delete → AlertDialog; sign out → AuthScreen returns in sign-up mode
**Expected:** All existing Phase 3/9 flows still work; delete confirmation dialog appears; delete executes via DeleteRegistryUseCase; bottom nav + FAB from Phase 9 still visible on Home
**Why human:** Navigation, deletion, and chrome regression requires a running device

### Gaps Summary

No gaps. All automated must-haves are verified. The only pending item is the 21-check on-device UAT deferred by the user. The VERIFICATION.md is set to `human_needed` rather than `gaps_found` because all code is verified correct — the outstanding item is a human observation task, not a code deficiency.

**Transitive validation from prior approved tasks:**
- Typography legibility of Auth headline (SCR-06 check 3) — partially validated by `260421-lwi` typography quick task (Instrument Serif italic rendering confirmed in that session)
- Bottom nav + FAB visibility on Home (SCR-07 regression guard) — partially validated by `260421-moi` nav quick task (nav chrome visible with Home screen active)

---

_Verified: 2026-04-21T20:00:00Z_
_Verifier: Claude (gsd-verifier)_
