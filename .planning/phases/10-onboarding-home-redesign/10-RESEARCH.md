# Phase 10: Onboarding + Home Redesign — Research

**Researched:** 2026-04-21
**Domain:** Jetpack Compose re-skin of AuthScreen.kt + RegistryListScreen.kt using Phase 8/9 GiftMaison primitives
**Confidence:** HIGH — all findings based on direct source code inspection of the existing repo

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Onboarding + Auth screen**
- Pre-auth carousel preserved — `OnboardingScreen` is untouched; Phase 10 only redesigns `AuthScreen`.
- Default mode: Sign up — `isSignUpMode = true` as initial state. Footer ghost pill "Log in" flips the mode.
- Terms/Privacy/Support links — wire to existing `strings.xml` keys if present; otherwise add placeholder keys (`auth_terms_url`, `auth_privacy_url`) with TODO-flagged empty values. Links open via `Intent.ACTION_VIEW` or placeholder no-op.
- Google OAuth — `AuthViewModel.signInWithGoogle()` (Phase 2 CredentialManager integration) stays as sole auth entry point. Phase 10 only changes the banner visual.

**Home screen**
- Tab filter definitions (client-side, no domain model change):
  - Active = `eventDateMs is null OR eventDateMs >= startOfToday`
  - Past = `eventDateMs != null && eventDateMs < startOfToday`
  - Drafts = heuristic: `title.isBlank() OR items.isEmpty()` (client-only, deferred real `status` field)
- Tab persistence — `rememberSaveable` for rotation only; resets to Active on cold start.
- Per-tab empty state — single line text, no CTA card, no illustration.
- Avatar top-right — 30 dp circle, initials on `colors.second` (olive), Inter W600. Taps → `SettingsKey`.
- Notifications bell relocation — removed from Home top bar; `/notifications` deep link and inbox route stay intact.
- `isPrimary` card selection rule — `registries.maxByOrNull { it.updatedAt }`. Renders with `background = colors.ink`, `content = colors.paper`, image at 70% brightness.
- Registry card navigation — `onNavigateToDetail(registryId)` → `RegistryDetailKey(registryId)`. Unchanged.
- Stats line copy — `"N items · M reserved · K given"` with `\u2022` separators.

### Claude's Discretion
- Exact composable file structure (split AuthScreen into sub-composables) — keep single public `AuthScreen()` entry point.
- Segmented-control implementation — custom `SegmentedTabs` composable; Material3 `SegmentedButton` lacks pill-on-paperDeep styling.
- Avatar initials derivation — `displayName.split(" ").take(2).map { it.first().uppercaseChar() }.joinToString("")`; fallback first char of email; final fallback `"?"`.
- Concentric rings on Google banner — inline `Canvas` composable OR layered `Box(border = )` circles. Claude picks whichever matches handoff opacity curve.

### Deferred Ideas (OUT OF SCOPE)
- Drafts tab backing data — real `Registry.status: 'draft'` field deferred to v1.2.
- Accurate stats aggregation — per-registry `itemCount/reservedCount/givenCount` deferred.
- Notifications bell placement in v1.1 — deferred until Phase 11.
- `isPrimary` persistence/pinning — owner-controlled pinning out of v1.1 scope.
- Empty states — brand-new-user / empty-registry / no-results deferred.
- Dark mode — light mode only; dark mode deferred.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SCR-06 | Onboarding + sign up screen matches handoff (wordmark top bar, italic-accent headline, Google banner with concentric rings, divider, name/email/password fields with focus ring, primary pill CTA, Terms line, "Log in" footer pill) | AuthScreen.kt structure confirmed; AuthViewModel contract locked; all Phase 8 primitives verified present; string keys audited |
| SCR-07 | Home / all registries screen matches handoff (wordmark + avatar top bar, "Your registries" headline, 3-tab segmented Active/Drafts/Past, card list with 16:9 hero + occasion pill + date + title + stats; single dark "primary" card) | RegistryListScreen.kt structure confirmed; RegistryListViewModel confirmed no item aggregation; `isPrimary` resolver confirmed in AppNavigation.kt; tab filter logic fully specified in UI-SPEC |
</phase_requirements>

---

## Summary

Phase 10 re-skins two existing screens — `AuthScreen.kt` (470 lines) and `RegistryListScreen.kt` (292 lines post-quick-260421-moi) — using the Phase 8 GiftMaison design primitives. This is a visual replacement with zero behaviour changes. All ViewModels, repositories, navigation keys, and Firebase queries remain untouched.

The key complexity is not in the primitives (all verified present and correct) but in the number of composable components that must be built from scratch: `GoogleBanner`, `AuthHeadline`, `SegmentedTabs`, `RegistryCardPrimary`, `RegistryCardSecondary`, `AvatarButton`, and `HomeTopBar`. Each is a contained leaf composable with well-defined inputs from existing state.

Two integration-level concerns must be addressed: (1) `AuthFormState` does not have `firstName`/`lastName` fields — the ViewModel must be extended or those fields managed locally in the composable; (2) `RegistryListViewModel` does not eagerly load item counts per registry — the stats line will show placeholder zeros per the approved CONTEXT.md deferral.

**Primary recommendation:** Build all new composables as self-contained files in `ui/auth/` and `ui/registry/list/`, keep public entry points `AuthScreen()` and `RegistryListScreen()` signature-identical to current code, and wire Phase 8 tokens exclusively through `GiftMaisonTheme.*`.

---

## Standard Stack

All libraries are already on the project classpath. No new `build.gradle` dependencies required for Phase 10.

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose BOM | 2026.03.00 | UI framework | Already in project; all Compose versions pinned by BOM |
| Material3 | via BOM | Base components (`OutlinedTextField`, `AlertDialog`, ripple) | Only for base Material3 components that GiftMaison tokens override |
| Coil | 3.4.0 | Registry card hero image loading | Already used (quick-260420-hcw); `AsyncImage` + `ColorFilter.colorMatrix` for 70% brightness |
| Hilt | 2.51.x | ViewModel injection via `hiltViewModel()` | Unchanged pattern from existing screens |
| Navigation3 | 1.0.1 | `SettingsKey` navigation from avatar tap | Unchanged; already wired in AppNavigation |
| Kotlin Coroutines / Flow | 1.9.x | `collectAsStateWithLifecycle()` | Unchanged pattern |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.animation` | via BOM | `animateColorAsState` for tab label colour | SegmentedTabs selected-state transition |
| `androidx.compose.foundation` | via BOM | `Canvas`, `border`, `clickable`, `rememberSaveable` | GoogleBanner rings, card borders, tab filter state |
| `androidx.compose.ui.text` | via BOM | `buildAnnotatedString`, `SpanStyle` | AuthHeadline italic+accent construction, Terms tappable spans |

**Version verification:** All packages are BOM-managed. No version lookup needed — BOM 2026.03.00 is the single version pin.

---

## Architecture Patterns

### Recommended Project Structure

New files for Phase 10:

```
app/src/main/java/com/giftregistry/ui/
├── auth/
│   ├── AuthScreen.kt              ← REWRITE (preserve signature)
│   ├── AuthViewModel.kt           ← EXTEND: add firstName/lastName fields
│   ├── AuthUiState.kt             ← EXTEND: add firstName/lastName to AuthFormState
│   ├── AuthHeadline.kt            ← NEW
│   └── GoogleBanner.kt            ← NEW
├── registry/list/
│   ├── RegistryListScreen.kt      ← REWRITE (preserve signature)
│   ├── RegistryListViewModel.kt   ← NO CHANGE
│   ├── RegistryCard.kt            ← NEW (contains Primary + Secondary variants)
│   ├── SegmentedTabs.kt           ← NEW
│   └── HomeTopBar.kt              ← NEW
└── common/
    └── AvatarButton.kt            ← NEW
```

New test files:

```
app/src/test/java/com/giftregistry/ui/
├── registry/list/
│   ├── TabFilterPredicateTest.kt  ← NEW (unit: Active/Past/Drafts filter logic)
│   └── DraftHeuristicTest.kt      ← NEW (unit: title.isBlank || items.isEmpty)
└── auth/
    └── AvatarInitialsTest.kt      ← NEW (unit: initials derivation from displayName)
```

String resource files:

```
app/src/main/res/
├── values/strings.xml             ← ADD Phase 10 auth_ + home_ keys
└── values-ro/strings.xml          ← ADD Phase 10 Romanian equivalents
```

StyleGuidePreview extension:

```
app/src/main/java/com/giftregistry/ui/theme/preview/
└── StyleGuidePreview.kt           ← APPEND Phase 10 previews (Auth + Home cards + SegmentedTabs)
```

### Pattern 1: AuthFormState Extension for First/Last Name

**What:** The current `AuthFormState` only has `email`, `password`, `confirmPassword`, `isLoading`, `errorMessage`. The handoff requires `firstName` and `lastName` fields in sign-up mode.

**Decision:** Add `firstName` and `lastName` to `AuthFormState` and corresponding `updateFirstName` / `updateLastName` methods to `AuthViewModel`. This keeps field state in the ViewModel for error validation (e.g., blank name on submit) consistent with existing pattern.

**Evidence:** Existing `AuthFormState` (confirmed from source):
```kotlin
// Current AuthFormState — needs extension
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

**Extended version:**
```kotlin
data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",   // Phase 10: sign-up mode
    val lastName: String = "",    // Phase 10: sign-up mode
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
```

The `signUp()` function must pass `firstName`/`lastName` to `signUpUseCase` — or if `signUpUseCase` doesn't accept them (confirmed: `signUpUseCase(email, password)` signature), Phase 10 can update displayName post-sign-up via `FirebaseUser.updateProfile()`. **Recommendation:** Update `displayName` in `AuthRepositoryImpl.signUpWithEmail()` after account creation using the `firstName + " " + lastName` value passed as a parameter. This keeps Phase 10 self-contained and doesn't break the domain interface.

### Pattern 2: AuthScreen Structure (re-skin without Scaffold top bar)

**What:** Current `AuthScreen` uses `Scaffold` with a `snackbarHost`. The handoff requires a full-screen `Column` with verticalScroll and an inline wordmark (no Material3 `TopAppBar`).

**Decision:** Keep the `Scaffold` for `snackbarHost` only (needed for error snackbar fallback), but use `contentPadding` from the Scaffold to position content. The inline wordmark `Row` replaces the old `TopAppBar`. The `SnackbarHost` can stay in the Scaffold while the auth UI content is the non-TopAppBar pattern.

**Alternative approach (cleaner):** Remove `Scaffold` entirely and manage snackbar state via the inline error banner pattern already specified in UI-SPEC (warm banner, not snackbar). This is preferable since the handoff shows NO snackbar — error messages are inline banners. The `errorMessage` in `AuthFormState` already supports this.

**Recommendation:** Remove `Scaffold`, switch to a full-screen `Box` or `Column` background with `colors.paper`, render the inline error banner from `formState.errorMessage`, and drop the `SnackbarHost`. This simplifies the composable and matches the handoff exactly.

### Pattern 3: Google Banner — Concentric Rings (Canvas approach)

**What:** The handoff calls for 3 concentric decorative circles in the top-right corner of the Google banner, with `{accentInk}25` and `{accentInk}18` opacity values (fractional hex, not alpha floats).

**Mapping to Compose alpha values:**
- Outer ring: `{accentInk}25` hex = ~0x25/0xFF ≈ `alpha = 0.145f` — treat as `alpha = 0.08f` per UI-SPEC clarification (3 rings: 0.08, 0.12, 0.18)
- Middle ring: `{accentInk}18` hex = ~0x18/0xFF ≈ `alpha = 0.094f` — treat as `alpha = 0.12f`
- Inner ring: implied `alpha = 0.18f`

**Recommended implementation (Canvas):**
```kotlin
// Source: UI-SPEC.md § Google Banner — concentric rings
Canvas(
    modifier = Modifier
        .size(70.dp)
        .align(Alignment.TopEnd)
        .offset(x = 8.dp, y = (-8).dp)
) {
    val center = Offset(size.width, 0f)
    drawCircle(color = accentInk.copy(alpha = 0.08f), radius = 30.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
    drawCircle(color = accentInk.copy(alpha = 0.12f), radius = 20.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
    drawCircle(color = accentInk.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
}
```

`Canvas` is preferred over layered `Box(border = )` because rings need to be clipped at the corner (the handoff uses `overflow: hidden`) — Canvas drawing naturally stops at canvas bounds, making corner clipping seamless.

### Pattern 4: Image Brightness (Coil ColorFilter)

**What:** Primary registry card hero image must be darkened to 70% brightness. Coil 3 provides `ColorFilter.colorMatrix`.

```kotlin
// Source: Coil 3 docs — ColorMatrix brightness
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

AsyncImage(
    model = registry.imageUrl,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    colorFilter = if (isPrimary) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) })
    } else null,
    modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(16f / 9f)
)
```

`setToScale(0.7f, 0.7f, 0.7f, 1f)` scales RGB channels to 70% while leaving alpha unchanged. This is the standard Coil 3 / Compose approach. CONFIRMED: `AsyncImage` from Coil is already in the project (quick-260420-hcw confirmed coil wired; project uses `coil3.compose.AsyncImage`).

### Pattern 5: SegmentedTabs Composable (Custom — not Material3)

**What:** Material3's `SegmentedButton` is a single-select button-group for filter chips, not a pill-track segmented control. The handoff shows a pill track (`colors.paperDeep`) with a white selected pill (`colors.paper`) and animated label colour.

```kotlin
// Source: UI-SPEC.md § Segmented Tabs
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.edge)
            .clip(shapes.pill)
            .background(colors.paperDeep)
            .padding(vertical = spacing.gap8)
    ) {
        Row {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) colors.ink else colors.inkFaint,
                    animationSpec = tween(durationMillis = 200),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shapes.pill)
                        .background(if (isSelected) colors.paper else Color.Transparent)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = spacing.gap8),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = GiftMaisonTheme.typography.monoCaps,
                        color = labelColor
                    )
                }
            }
        }
    }
}
```

**Tab state pattern (per CONTEXT.md decision):**
```kotlin
// rememberSaveable persists across rotation; Int index for simplicity
var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
```

### Pattern 6: Tab Filter Predicate (pure Kotlin, testable)

**What:** Filter logic must be a pure function for unit testability. Extract as a top-level function.

```kotlin
// Source: CONTEXT.md § Tab filter definitions + UI-SPEC.md § Tab Filtering Logic

/**
 * startOfTodayMs uses Calendar for minSdk 23 compat.
 * Java 8 LocalDate.atStartOfDay() is available on API 26+ but project minSdk is 23.
 */
fun startOfTodayMs(): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun Registry.isActive(todayMs: Long): Boolean =
    eventDateMs == null || eventDateMs >= todayMs

fun Registry.isPast(todayMs: Long): Boolean =
    eventDateMs != null && eventDateMs < todayMs

/**
 * Draft heuristic: title blank OR no items.
 * items parameter is passed in — ViewModel doesn't eagerly load items,
 * so Phase 10 renders draft tab as empty when items aren't loaded.
 * See CONTEXT.md § deferred: Drafts tab backing data.
 */
fun Registry.isDraft(itemCount: Int): Boolean =
    title.isBlank() || itemCount == 0
```

**IMPORTANT:** `isDraft` requires `itemCount` which is not available in `RegistryListViewModel.uiState`. Per the approved CONTEXT.md deferral, Phase 10 will treat all registries as non-draft (itemCount = 0 means draft, so the Drafts tab will show registries with blank titles only, until the stats aggregation is shipped). The TODO comment must be added.

### Pattern 7: Avatar Initials from User.displayName

**What:** `User.displayName: String?` (confirmed from source). When null, fall back to email first char, then `"?"`.

```kotlin
// Source: AuthRepository.kt — User domain model has displayName: String?
fun String?.toAvatarInitials(email: String?): String {
    if (!this.isNullOrBlank()) {
        val parts = this.trim().split("\\s+".toRegex())
        return parts.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .takeIf { it.isNotEmpty() } ?: "?"
    }
    return email?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
```

The `RegistryListViewModel` currently does NOT expose the current `User` object — it only uses `authRepository.authState` internally. Phase 10 needs the avatar to display in `RegistryListScreen`. Options:
1. Expose `currentUser: StateFlow<User?>` from `RegistryListViewModel` (cheapest, already has `authRepository` reference)
2. Add a separate `HomeViewModel` that exposes the user  
3. Read `authRepository.currentUser` (non-reactive, but safe for display-only)

**Recommendation:** Add `val currentUser: StateFlow<User?>` to `RegistryListViewModel` by collecting `authRepository.authState`. This is a one-line addition consistent with existing VM patterns.

### Anti-Patterns to Avoid

- **Do not shadow theme tokens:** Never write `Color(0xFF.....)` in screen files — always use `GiftMaisonTheme.colors.*`.
- **Do not use MaterialTheme.typography in new Phase 10 composables:** Use `GiftMaisonTheme.typography.*` exclusively on the new composables. Existing non-GiftMaison composables (`AlertDialog`) can continue to use MaterialTheme.
- **Do not add a `@Composable` signature change to `AuthScreen()`:** The entry point `AuthScreen(viewModel: AuthViewModel = hiltViewModel())` must remain identical for `AppNavigation.kt` to compile without changes.
- **Do not add a `@Composable` signature change to `RegistryListScreen()`:** Current signature has `onNavigateToDetail`, `onNavigateToEdit`, `onNavigateToNotifications` parameters. Phase 10 keeps these (the edit and notifications params can become no-ops if not used in new layout, but must not be removed to preserve AppNavigation call sites).
- **Do not use `rememberSaveable` with sealed class directly without Saver:** Use `Int` index for tab state (`var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }`) rather than a sealed class — sealed classes require a custom Saver for `rememberSaveable`.
- **Do not use `android.text.format.DateUtils` for date formatting:** The existing `formatDate()` in RegistryListScreen uses `SimpleDateFormat` — keep the same pattern in Phase 10.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 70% brightness image | Custom image processor | `ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) })` on `AsyncImage` | Coil handles all memory/lifecycle concerns |
| Animated label colour in tabs | Manual `LaunchedEffect` colour lerp | `animateColorAsState(durationMillis = 200)` | Compose Animation API handles interruption correctly |
| Ripple on tappable composables | Custom touch feedback | `Modifier.clickable {}` — Material ripple built-in | Compose `Clickable` uses platform ripple |
| Annotated headline with colour split | Manual `drawText` | `buildAnnotatedString { withStyle(SpanStyle(color = ...)) }` | Same pattern already used in `GiftMaisonWordmark.kt` |
| Focus-driven border colour on text fields | Custom InteractionSource polling | `OutlinedTextField` with `colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.accent)` | Material3 TextField handles focus state natively |

---

## Key Research Findings: Existing Code Contract

### AuthScreen.kt Current State (470 lines, confirmed)

Structure:
- `Scaffold` with `snackbarHost` wrapping a `Column(verticalScroll)`
- Inner `Card` (RoundedCornerShape(24.dp)) containing all auth UI
- `isSignUpMode` is local `remember { mutableStateOf(false) }` — NOT in ViewModel
- Google sign-in calls `CredentialManager.create(context)` inline in a coroutine — the banner `onClick` lambda must replicate this exact pattern
- **CRITICAL:** `isSignUpMode` starts as `false` (sign-in mode), but CONTEXT.md mandates it start as `true` (sign-up mode). Phase 10 must change this initial value.
- `formState` comes from `viewModel.formState.collectAsStateWithLifecycle()` — expose `firstName`/`lastName` there
- `SnackbarHostState` and `scope` are used only for Google sign-in error fallback — this can be kept or replaced with error banner; error banner is cleaner and matches handoff
- `OrDivider()` is a private helper — replace with GiftMaison-styled version
- Footer currently has 3 `TextButton` links (Terms, Privacy, Support) in a `Row` — replace with single-line Terms+Privacy `AnnotatedString` per UI-SPEC

**Fields to retain in new AuthScreen:**
- `passwordVisible by remember { mutableStateOf(false) }` — local state, fine as-is
- `isSignUpMode by remember { mutableStateOf(true) }` — change initial to `true`
- The CredentialManager Google sign-in coroutine block — move inside `GoogleBanner.onClick`

### RegistryListScreen.kt Current State (292 lines post-quick-260421-moi, confirmed)

Structure:
- `Scaffold` with `TopAppBar` (replace with inline `HomeTopBar` Row) + `snackbarHost`
- Params: `onNavigateToDetail`, `onNavigateToEdit`, `onNavigateToNotifications` — keep all
- `registryToDelete` AlertDialog — keep unchanged (existing `registry_delete_confirm_*` strings)
- `NotificationsInboxBell` in TopAppBar actions — REMOVE (bell moved per CONTEXT.md decision)
- `RegistryCard` private composable (197-287) — REPLACE entirely with new `RegistryCardPrimary`/`RegistryCardSecondary`
- `formatDate()` private function — KEEP, reuse in new cards
- `LazyColumn` with `items(state.registries)` — KEEP the `LazyColumn` structure; add tab filtering above it
- `DropdownMenu` for edit/delete — evaluate whether to keep in Phase 10 design (handoff shows no `⋯` menu on cards; long-press to delete is an alternative). **Decision per CONTEXT.md:** existing delete functionality must be preserved. Recommend retaining the `DropdownMenu` on long-press (combined clickable) but removing the `MoreVert` icon from the card header — a long-press gesture is the only way to reveal it, consistent with clean card design.

**IMPORTANT: `onNavigateToEdit` parameter.** The handoff card design doesn't show an edit button. The `onNavigateToEdit` param is called from the old `DropdownMenu`. In Phase 10's redesigned cards, the edit action can remain accessible via long-press dropdown while keeping the card clean. Do NOT remove the `onNavigateToEdit` parameter from the function signature.

### RegistryListViewModel.kt (67 lines, confirmed)

- Exposes: `uiState: StateFlow<RegistryListUiState>` (Success contains `List<Registry>`)
- Exposes: `deleteError: StateFlow<String?>`
- Does NOT expose: current user, item counts, any per-registry stats
- `Registry` domain model confirmed: has `title`, `occasion`, `eventDateMs`, `updatedAt`, `createdAt`. Has NO `imageUrl`, `itemCount`, `reservedCount`, `givenCount`, `isPrimary`, `status` fields.
- **imageUrl gap:** Registry cards need a hero image but `Registry` has no `imageUrl` field. Phase 10 must either (a) show `colors.paperDeep` placeholder fill for all cards, or (b) add `imageUrl: String? = null` to the `Registry` domain model. The handoff shows real images. **Recommendation:** Add `imageUrl: String? = null` to `Registry.kt` — this is a backward-compatible default-null addition that Firestore will serialize as absent (no migration needed).

### User Domain Model (confirmed)

```kotlin
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean
)
```

No `firstName`/`lastName` fields. `displayName` is from Firebase Auth's `User.displayName` which is set during Google sign-in (full name) or `updateProfile()` post-email sign-up. Phase 10 must call `UserProfileChangeRequest.Builder().setDisplayName(firstName + " " + lastName)` after successful email sign-up.

### Existing String Keys Reused Without Change (auth_ namespace)

From `strings.xml` audit (confirmed present):
- `auth_email_label`, `auth_password_label`, `auth_confirm_password_label`
- `auth_password_show`, `auth_password_hide`
- `auth_error_invalid_credentials`, `auth_error_email_exists`, `auth_error_weak_password`, `auth_error_network`
- `auth_have_account_prompt`, `auth_no_account_prompt`
- `auth_or_email_divider` (currently "or sign in with email" — Phase 10 changes copy to "or sign up with email" for sign-up mode; consider making it mode-aware)

**New keys required (not present in current strings.xml):**
`auth_headline_prefix`, `auth_headline_accent`, `auth_google_cta` (maps to existing `auth_google_sign_in_button`), `auth_first_name_label`, `auth_last_name_label`, `auth_password_helper`, `auth_signup_cta`, `auth_login_cta`, `auth_terms_line`, `auth_terms_link`, `auth_privacy_link`, `auth_login_footer`, `auth_terms_url`, `auth_privacy_url`, `home_headline`, `home_stats_caption`, `home_tab_active`, `home_tab_drafts`, `home_tab_past`, `home_empty_active`, `home_empty_drafts`, `home_empty_past`, `home_avatar_content_desc`

### AppNavigation.kt (confirmed — no changes needed)

- `RegistryListScreen` is called from `entry<HomeKey>` — signature must stay compatible
- `AuthScreen` is called from `entry<AuthKey>` — no params passed, stays compatible
- `primaryRegistryId` is computed in AppNavigation via `RegistryListViewModel` at nav scope — this is the `isPrimary` resolver. The Phase 10 card list will compute `isPrimary` the same way inside `RegistryListScreen` (it has access to `state.registries` and can call `maxByOrNull { it.updatedAt }`).
- `SettingsKey` exists and is registered — avatar tap can navigate to it via `onNavigateToSettings: () -> Unit` parameter added to `RegistryListScreen`, or by accessing the back stack directly. **Recommendation:** Add `onNavigateToSettings: () -> Unit = {}` parameter to `RegistryListScreen` and wire it from AppNavigation. This is the minimal change.

---

## Common Pitfalls

### Pitfall 1: isSignUpMode Initial State
**What goes wrong:** Copy-pasting the existing `isSignUpMode = false` initial value. The current screen defaults to sign-IN mode, but the handoff and CONTEXT.md mandate sign-UP mode as default.
**Why it happens:** The existing code has `var isSignUpMode by remember { mutableStateOf(false) }` which is easy to miss.
**How to avoid:** Change to `mutableStateOf(true)` and include a unit test or comment.
**Warning signs:** Screen opens showing "Sign In" button instead of "Sign up" button.

### Pitfall 2: Missing onNavigateToSettings in RegistryListScreen signature
**What goes wrong:** Avatar button has no nav callback, tapping does nothing, or crashes because there's no nav target.
**Why it happens:** Current `RegistryListScreen` has no settings navigation parameter.
**How to avoid:** Add `onNavigateToSettings: () -> Unit = {}` as a parameter and wire in AppNavigation.

### Pitfall 3: rememberSaveable with Sealed Class
**What goes wrong:** Defining a `sealed class Tab { Active, Drafts, Past }` and using it with `rememberSaveable` causes a runtime crash — sealed classes require a custom `Saver`.
**Why it happens:** `rememberSaveable` can only auto-save types that are `Parcelable` or primitives.
**How to avoid:** Use `var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }` (Int index). Map index to tab label via a `listOf(...)`.

### Pitfall 4: Coil 3 Import Path
**What goes wrong:** Using `coil.compose.AsyncImage` (Coil 2) instead of `coil3.compose.AsyncImage` (Coil 3). Project uses Coil 3.
**Why it happens:** IDE autocomplete may suggest Coil 2 if both are on classpath, but only Coil 3 is in the project.
**How to avoid:** Always import from `coil3.*`.

### Pitfall 5: AuthFormState firstName/lastName Not Passed to signUp()
**What goes wrong:** `firstName`/`lastName` are added to `AuthFormState` but `signUpUseCase(email, password)` is called without them — display name is never set.
**Why it happens:** `SignUpUseCase` only takes email + password. The domain interface doesn't know about display names.
**How to avoid:** After `signUpUseCase` succeeds in `AuthViewModel.signUp()`, call `authRepository.updateDisplayName(firstName + " " + lastName)`. Alternatively, pass them through the use case. The repository already has a `signUpWithEmail(email, password)` — add an overload or add `displayName` parameter.

### Pitfall 6: Registry imageUrl Field Missing
**What goes wrong:** `RegistryCardPrimary` references `registry.imageUrl` but the field doesn't exist on `Registry.kt`.
**Why it happens:** The `Registry` domain model was designed before the visual redesign; `imageUrl` was not in the original schema.
**How to avoid:** Add `val imageUrl: String? = null` to `Registry.kt`. Firestore reads will populate it when present in docs; null when absent. AsyncImage handles null gracefully with placeholder.

### Pitfall 7: startOfToday Calculation on API 23
**What goes wrong:** Using `LocalDate.now().atStartOfDay().toEpochMilli()` which requires API 26.
**Why it happens:** Java 8 time APIs require API 26; project minSdk is 23.
**How to avoid:** Use `Calendar.getInstance()` with `set(HOUR_OF_DAY, 0)` etc. as specified in Pattern 6 above.

### Pitfall 8: letterSpacing on GiftMaisonWordmark
**What goes wrong:** The existing `GiftMaisonWordmark.kt` uses `letterSpacing = (-0.4).em` (EM units). This is a pre-existing deviation from the typography convention (all other TextStyle uses `.sp`). Do not "fix" this in Phase 10 — the wordmark's letter-spacing is intentionally in EM for optical reasons documented in GiftMaisonWordmark.kt.
**Warning signs:** If you change the wordmark letter-spacing, `WordmarkTest` will fail.

---

## Code Examples

### Auth headline (AnnotatedString — same pattern as wordmark)
```kotlin
// Source: GiftMaisonWordmark.kt (wordmarkAnnotatedString pattern) + UI-SPEC.md § Auth Headline
@Composable
fun AuthHeadline(modifier: Modifier = Modifier) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_headline_prefix), // "Start your"
            style = typography.displayL.copy(fontStyle = FontStyle.Italic),
            color = colors.inkSoft,
        )
        val line2 = buildAnnotatedString {
            withStyle(SpanStyle(color = colors.ink)) {
                append(stringResource(R.string.auth_headline_accent).dropLast(1)) // "first registry"
            }
            withStyle(SpanStyle(color = colors.accent)) {
                append(".") // accent period
            }
        }
        Text(
            text = line2,
            style = typography.displayL.copy(fontStyle = FontStyle.Italic),
        )
    }
}
```

Note: `auth_headline_accent` string value should be `"first registry."` (with period) so the trailing character is the accent period. Alternatively store `"first registry"` (no period) and append `.` in code — cleaner for localization.

### Primary card with darkened image
```kotlin
// Source: UI-SPEC.md § Registry Card + Coil 3 API
@Composable
fun RegistryCardPrimary(
    registry: Registry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.radius16)
            .background(colors.ink)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = registry.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        // Occasion pill overlay, date overlay, title + stats below image...
    }
}
```

### Tab filter usage in RegistryListScreen
```kotlin
// Source: UI-SPEC.md § Tab Filtering Logic
val todayMs = remember { startOfTodayMs() }
val filteredRegistries = remember(registries, selectedTabIndex, todayMs) {
    when (selectedTabIndex) {
        0 -> registries.filter { it.isActive(todayMs) }
        1 -> registries.filter { it.isDraft(itemCount = 0) } // TODO: Phase 10 stats aggregation deferred
        2 -> registries.filter { it.isPast(todayMs) }
        else -> registries
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `MaterialTheme.colorScheme.*` for all colours | `GiftMaisonTheme.colors.*` for Phase 8+ screens | Phase 8 | Existing screens still use MaterialTheme; only new screens use GiftMaison tokens |
| `MaterialTheme.typography.*` | `GiftMaisonTheme.typography.*` | Phase 8 | Same as above — migration is screen-by-screen |
| `TopAppBar` in RegistryListScreen | Inline `Row` (HomeTopBar) | Phase 10 | No more Material3 TopAppBar chrome on Home |
| `NotificationsInboxBell` in Home top bar | Removed from Home; bell stays via "You" tab flow | Phase 10 | Navigation path to inbox changes |
| `Card` elevation in AuthScreen | Full-screen Column + `colors.paper` background | Phase 10 | No card shadow; handoff is flat full-screen layout |
| `isSignUpMode = false` default | `isSignUpMode = true` default | Phase 10 | Sign-up is the first-run default |

---

## Open Questions

1. **`onNavigateToEdit` in new card design**
   - What we know: Handoff shows no edit button on registry cards; edit flow is Phase 11 scope.
   - What's unclear: Should edit be accessible at all from Home in v1.1, or is it temporarily inaccessible?
   - Recommendation: Retain long-press DropdownMenu with Edit and Delete options. Edit takes user to `EditRegistryKey`. The DropdownMenu trigger is the `MoreVert` icon — either keep it faint on the card or trigger it from long-press only. Cleanest: long-press reveals the menu (no icon on card face), keeping card visual clean.

2. **`signUpUseCase` displayName parameter**
   - What we know: `SignUpUseCase(email, password)` takes only email + password.
   - What's unclear: Should Phase 10 update the use case signature, or update display name post-sign-up?
   - Recommendation: Add optional `displayName: String? = null` to `signUpUseCase` and `AuthRepositoryImpl.signUpWithEmail()`. Call `firebaseUser.updateProfile(...)` inside the repository after account creation. This is the least invasive change.

3. **Registry imageUrl Firestore backfill**
   - What we know: `Registry.kt` has no `imageUrl` field; Firestore docs may have `imageUrl` from past quick tasks (quick-260420-hcw added Coil rendering for AddItemScreen).
   - What's unclear: Do existing registry Firestore documents have an `imageUrl` field?
   - Recommendation: Add `val imageUrl: String? = null` to `Registry.kt`. If Firestore docs have it, it will populate. If not, cards show `colors.paperDeep` placeholder. No Firestore migration needed.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 10 is purely a Compose re-skin. No new external dependencies, services, CLIs, or runtime tools are required beyond what is already in the project (Android Studio, Gradle, Firebase emulator — all confirmed operational from Phase 9 completion).

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (pure Kotlin / JVM unit tests) |
| Config file | `app/build.gradle.kts` — `testImplementation("junit:junit:4.13.2")` |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

No Compose UI test framework is used in this project (all existing Phase 8/9 tests are pure JVM unit tests). Phase 10 follows the same pattern.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SCR-06 | Auth headline annotated string construction (ink + accent split) | unit | `./gradlew :app:testDebugUnitTest --tests "*.AuthHeadlineTest"` | ❌ Wave 0 |
| SCR-06 | AuthFormState has firstName + lastName fields | unit | `./gradlew :app:testDebugUnitTest --tests "*.AuthFormStateTest"` | ❌ Wave 0 |
| SCR-06 | Avatar initials derivation from displayName / email / fallback | unit | `./gradlew :app:testDebugUnitTest --tests "*.AvatarInitialsTest"` | ❌ Wave 0 |
| SCR-07 | Tab filter: Active predicate (null date = active; future date = active; past date = inactive) | unit | `./gradlew :app:testDebugUnitTest --tests "*.TabFilterPredicateTest"` | ❌ Wave 0 |
| SCR-07 | Tab filter: Past predicate (past date = past; null date = not past) | unit | `./gradlew :app:testDebugUnitTest --tests "*.TabFilterPredicateTest"` | ❌ Wave 0 |
| SCR-07 | Tab filter: Draft heuristic (blank title = draft; empty items = draft; both non-blank/non-zero = not draft) | unit | `./gradlew :app:testDebugUnitTest --tests "*.DraftHeuristicTest"` | ❌ Wave 0 |
| SCR-07 | isPrimary selection (maxByOrNull updatedAt returns correct registry) | unit | `./gradlew :app:testDebugUnitTest --tests "*.IsPrimarySelectionTest"` | ❌ Wave 0 |

All tests are pure Kotlin / JVM — no Compose test runtime, no `@HiltAndroidTest`, no Robolectric. This is consistent with Phase 8/9 test patterns (`BottomNavVisibilityTest`, `PulsingDotTest`, `ColorsTest` — all pure unit tests).

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt` — covers SCR-06 headline AnnotatedString construction
- [ ] `app/src/test/java/com/giftregistry/ui/auth/AuthFormStateTest.kt` — covers SCR-06 firstName/lastName fields on AuthFormState
- [ ] `app/src/test/java/com/giftregistry/ui/common/AvatarInitialsTest.kt` — covers SCR-06 + SCR-07 avatar initials helper
- [ ] `app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt` — covers SCR-07 Active/Past filter predicates
- [ ] `app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt` — covers SCR-07 draft heuristic
- [ ] `app/src/test/java/com/giftregistry/ui/registry/list/IsPrimarySelectionTest.kt` — covers SCR-07 isPrimary selection rule

---

## Project Constraints (from CLAUDE.md)

| Constraint | Impact on Phase 10 |
|-----------|-------------------|
| Kotlin only (no Java) | All new composables in Kotlin |
| Firebase only — no other persistence | Tab state is `rememberSaveable` (memory only), not DataStore — this is fine since it's UI state, not user preference |
| Guest access must work without account | `AuthScreen` preserves the `continueAsGuest()` path — the "Continue as Guest" button must remain accessible. Handoff doesn't show it but it's required by AUTH-05. Place it as a tertiary TextButton below the footer ghost pill or keep in an accessible but visually secondary position. |
| All UI labels in strings.xml + values-ro/strings.xml | All Phase 10 string keys must be added to BOTH locales |
| No hardcoded strings | No string literals on composables; only `stringResource()` |
| GSD workflow enforcement | All changes made through `gsd:execute-phase` |

**Critical: AUTH-05 guest path.** The handoff does NOT show a "Continue as Guest" button on the redesigned auth screen. However, AUTH-05 is a shipped requirement that must keep working. Phase 10 must preserve `viewModel.continueAsGuest()` accessibility. Recommended: add a subtle `TextButton` below the "Log in" footer pill, using `typography.bodyXS` and `colors.inkFaint`, with the existing `auth_continue_as_guest` string key. This keeps it accessible without cluttering the primary design flow.

---

## Sources

### Primary (HIGH confidence)

Direct source code inspection of the repository (all files read during this session):

- `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` — 470 lines, full structure confirmed
- `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` — ViewModel contract confirmed
- `app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt` — AuthFormState fields confirmed
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` — 292 lines, post-quick-260421-moi confirmed
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` — no item aggregation confirmed
- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` — 17 lines, no imageUrl/stats fields confirmed
- `app/src/main/java/com/giftregistry/domain/model/User.kt` — displayName: String? confirmed
- `app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt` — interface confirmed
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — isPrimary resolver pattern confirmed
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — SettingsKey confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonColors.kt` — all 13 colour tokens confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTypography.kt` — all 10 type roles confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShapes.kt` — all 7 shape tokens confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonSpacing.kt` — all spacing tokens confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonShadows.kt` — googleBannerShadow() confirmed
- `app/src/main/java/com/giftregistry/ui/theme/GiftMaisonWordmark.kt` — wordmarkAnnotatedString pattern confirmed
- `app/src/main/java/com/giftregistry/ui/theme/Theme.kt` — GiftMaisonTheme.colors/typography/shapes/spacing confirmed
- `app/src/main/res/values/strings.xml` — existing auth_ keys audited
- `.planning/phases/10-onboarding-home-redesign/10-CONTEXT.md` — 12 locked decisions
- `.planning/phases/10-onboarding-home-redesign/10-UI-SPEC.md` — full pixel-level contract
- `design_handoff_android_owner_flow/README.md` — § 06 Onboarding, § 07 Home

### Secondary (MEDIUM confidence)

- `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — Phase 9 test pattern; confirms pure-JVM unit test approach
- `app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt` — confirms `const val` pattern for testable animation constants

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries confirmed present in project; no new dependencies
- Architecture: HIGH — all existing file structures and ViewModel contracts verified from source
- Pitfalls: HIGH — all pitfalls derived from direct code inspection, not speculation
- String keys: HIGH — `strings.xml` audited; existing/missing keys confirmed

**Research date:** 2026-04-21
**Valid until:** 2026-05-21 (stable Compose BOM + Coil; no fast-moving dependencies)
