# Phase 10: Onboarding + Home Redesign - Context

**Gathered:** 2026-04-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Redesign two owner-facing screens to match the design handoff pixel-accurately while preserving all existing behaviour (auth flows, Google OAuth, registry list queries, navigation):

1. **Screen 06 — Onboarding + sign up** (`AuthScreen.kt`, ~470 lines): replace Material 3 defaults with GiftMaison visuals — wordmark top bar, "Start your / first registry." italic-accent headline, Google banner with concentric rings + shadow, "or sign up with email" divider, paperDeep input fields with accent focus ring, ink pill Sign up CTA, Terms line, "Already have an account? Log in" ghost pill footer. Sign-up / sign-in / Google OAuth logic unchanged; only visuals swap.

2. **Screen 07 — Home / all registries** (`RegistryListScreen.kt`, ~292 lines post-quick-260421-moi): replace current Material 3 TopAppBar + LazyColumn with GiftMaison — wordmark + avatar top bar, "Your registries" displayXL headline + mono-caps "N active · M items total" caption, 3-tab Active / Drafts / Past segmented control, scrolling registry cards (16:9 hero + occasion pill + date + title + stats) with exactly one dark primary card.

Out of scope (belongs to Phase 11): Registry detail (08), Create registry (09), Add item (10). Phase 10 stops at the two screens listed above.

</domain>

<decisions>
## Implementation Decisions

### Onboarding + Auth screen

- **Pre-auth carousel preserved** — the 3-slide onboarding carousel from quick-260419-ubj keeps running on first launch (`OnboardingScreen` → `AuthScreen`). Phase 10 only redesigns `AuthScreen`; `OnboardingScreen` is untouched. Redesigning the carousel is deferred (not in handoff scope).
- **Default mode: Sign up** — per handoff. The AuthViewModel's existing `isSignUpMode` state drives both modes; Phase 10 preserves the toggle logic. The footer ghost pill "Already have an account? Log in" flips the mode.
- **Terms / Privacy / Support links** — wire to existing `strings.xml` keys if present; otherwise add placeholder keys (`auth_terms_url`, `auth_privacy_url`, `auth_support_url`) with TODO-flagged empty values. Links open via `Intent.ACTION_VIEW` or placeholder no-op. Real legal content is out of scope.
- **Google OAuth** — the `AuthViewModel.signInWithGoogle()` (Phase 2 `CredentialManager` integration) stays as the sole auth entry point; Phase 10 only changes the banner visual to the terracotta-accent rounded pill with 20×20 white Google G circle, "→" italic serif arrow, concentric accentInk rings in top-right corner, and the `0 10 24 {accent}55` shadow per handoff.

### Home screen

- **Tab filter definitions (client-side, no domain model change):**
  - **Active** = `eventDateMs is null OR eventDateMs >= startOfToday` (includes undated registries).
  - **Past** = `eventDateMs < startOfToday`.
  - **Drafts** = heuristic: `title.isBlank() OR items.isEmpty()` (client-only derivation). Flagged as a rough approximation until a proper `Registry.status: 'draft'` field ships — future todo.
- **Tab persistence** — not persisted across app restarts (defaults to Active each cold start). Local `rememberSaveable` preserves tab across rotation only. Matches handoff behaviour (no tab-state spec).
- **Per-tab empty state** — shows a single line "No active / draft / past registries yet" (inkFaint, body) with bottom padding — no CTA card, no illustration. Full empty-state design is out of v1.1 scope per REQUIREMENTS.md § Out of Scope.
- **Avatar top-right** — replaces the current notifications bell. Avatar = 30 dp circle with owner initials on `colors.second` (olive), Inter W600. Tapping it navigates to `SettingsKey` (settings/preferences — matches Phase 9's "You" tab routing).
- **Notifications bell relocation** — removed from Home top bar. The `/notifications` deep link and the inbox route stay intact; reaching the inbox from Home moves to the "You" → Settings nav flow in v1.1, with a formal placement (bell in topbar OR Settings row OR FAB sheet action) deferred as a Phase 11 follow-up todo.
- **`isPrimary` card selection rule** — most-recently-updated registry via `registries.maxByOrNull { it.updatedAt }`. Matches Phase 9's Lists-tab resolver (`RegistryListViewModel`). Rendered with `background = colors.ink`, `content = colors.paper`, no border, image darkened to 70 % brightness. All other cards use `colors.paperDeep` background + `colors.line` border + full-brightness image.
- **Registry card navigation** — tap-a-card continues to dispatch the existing `onNavigateToDetail(registryId)` → `RegistryDetailKey(registryId)`. Navigation3 back stack logic unchanged.
- **Stats line copy** — "N items · M reserved · K given" rendered with mono-caps bullet separators (• via `\u2022`). Stats sourced from `registry.computedStats()` or equivalent client-side derivation over the Items Flow; if an aggregate isn't readily available, use `items.count { status == AVAILABLE } / RESERVED / PURCHASED` on the reactive list (no Firestore query change).

### Claude's Discretion

- Exact composable file structure (e.g., split AuthScreen into `AuthScreenContent` + `AuthForm` + `GoogleBanner` composables) is Claude's choice. Keep the single public `AuthScreen()` entry point identical to the current file.
- Segmented-control implementation — use a custom `SegmentedTabs` composable (Material 3's `SegmentedButton` is for single-select ranges and lacks the handoff's pill-on-paperDeep styling). Live inside `ui/registry/list/` or `ui/common/` — Claude picks.
- Avatar initials derivation — use first char of `user.firstName` + first char of `user.lastName` uppercased; if either missing, fallback to first char of email or "?".
- Concentric rings on Google banner — inline `Canvas` composable OR layered `Box(border = )` circles with `Modifier.offset`. Claude picks whichever matches the handoff opacity curve.

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets (Phase 8 + Phase 9)
- `GiftMaisonTheme` — all colour tokens, typography roles, shapes, spacing, shadows are available via `GiftMaisonTheme.colors / typography / shapes / spacing`.
- `GiftMaisonWordmark` — drop-in wordmark composable for the Auth + Home top bars.
- `GiftMaisonBottomNav` + `AddActionSheet` (Phase 9) — already wired at `AppNavigation` level. Home (HomeKey) shows nav; Auth (AuthKey) hides nav. No Phase 10 work needed to consume them.
- `GiftMaisonFab` (Phase 9) — not used directly in screens; it lives in the nav chrome.
- `StatusChip` (Phase 9) — used on registry cards if the 4-stat row includes a Reserved/Given chip (handoff shows stats line, not chips — likely not needed on Home cards).

### Established Patterns
- **AuthScreen** (`app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt:470 lines`): uses Material 3 `OutlinedTextField`, `Button`, `Divider`, `TextButton`. Consumes `AuthViewModel` via `hiltViewModel()`. State = `AuthUiState` (Loading / Unauthenticated / Authenticated / Error).
- **RegistryListScreen** (`app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt:292 lines`): Scaffold + TopAppBar + LazyColumn of `RegistryCard`. Consumes `RegistryListViewModel` (StateFlow of `RegistryListUiState`). Quick-260421-moi removed the legacy FAB and `onNavigateToCreate` param.
- **Registry domain** (`app/src/main/java/com/giftregistry/domain/model/Registry.kt:17 lines`): title, occasion, visibility, eventDateMs, eventLocation, description, createdAt, updatedAt, invitedUsers. NO `status` field, NO `isPrimary` field — both derived client-side.
- **Item count for stats** — Items live in a subcollection; `RegistryListViewModel` currently doesn't eagerly load items per registry. Phase 10 may need an aggregated `itemCount` on Registry (requires Firestore backfill) OR per-card Flow observation (adds listeners). Recommend deferring accurate stats to a follow-up todo — Phase 10 can show "N items" where N = 0 for now if item aggregation isn't already in the query.
- **Strings**: I18N-02 (all labels in `strings.xml` + `values-ro/strings.xml`). Phase 10 adds new keys: `onboarding_headline_prefix` ("Start your"), `onboarding_headline_accent` ("first registry."), `onboarding_subline`, `onboarding_google_cta`, `onboarding_or_divider`, `onboarding_first_name_label`, `onboarding_last_name_label`, `onboarding_email_label`, `onboarding_password_label`, `onboarding_password_helper`, `onboarding_signup_cta`, `onboarding_terms`, `onboarding_login_footer`, `home_headline` ("Your registries"), `home_stats_template` ("%d active · %d items total"), `home_tab_active`, `home_tab_drafts`, `home_tab_past`, `home_empty_active`, `home_empty_drafts`, `home_empty_past`, plus RO equivalents.

### Integration Points
- **`AuthScreen.kt`** (replace visuals) — preserves `AuthViewModel` consumption.
- **`RegistryListScreen.kt`** (replace visuals + add tab filter state) — preserves `RegistryListViewModel` consumption; `itemCount` / stats may need a helper.
- **`AppNavigation.kt`** — no changes expected. HomeKey already shows bottom nav + FAB per Phase 9.
- **`strings.xml` + `values-ro/strings.xml`** — new Phase 10 keys added in both locales.
- **`StyleGuidePreview.kt`** — appended with new Phase 10 preview sections (Auth form, Home card primary + secondary, segmented tabs).
- **`user.firstName` / `user.lastName`** — sourced from Firebase `User.displayName` split OR a `users/{uid}` Firestore doc. Need to check what AuthRepository exposes. If firstName/lastName aren't on the domain `User`, add client-side split of `displayName` as a Phase 10 helper.

</code_context>

<specifics>
## Specific Ideas

- **Design handoff is the visual contract.** Every pixel in `design_handoff/design_handoff_android_owner_flow/README.md` § 06 Onboarding + sign up and § 07 Home — all registries is a must-match. The Google banner's 16 radius + `14 16` padding + concentric-ring placement + italic serif `→` arrow + `0 10 24 {accent}55` shadow are all locked. The primary card's `background: ink`, `color: paper`, image at 70 % brightness are locked.
- **Re-skin, not rebuild.** Per PROJECT.md v1.1 scope: navigation graph, ViewModels, repositories, Firestore schema, and Cloud Functions all stay as-is. Only Compose visuals change.
- **Phase 10 does not touch Phase 9 chrome.** GiftMaisonBottomNav, AddActionSheet, StatusChip all consumed as-is.
- **Existing behaviour MUST keep working.** Sign-up, sign-in, Google OAuth, guest continuation (REQUIREMENT AUTH-01..06), registry list query (REG-10), card-tap → Registry detail navigation (REG-08). The redesign is visual only.

</specifics>

<deferred>
## Deferred Ideas

- **Drafts tab backing data** — Phase 10 uses a heuristic (`title.isBlank() OR items.isEmpty()`). A real `Registry.status: 'draft'` field with explicit save-as-draft behaviour is deferred (would be a v1.2 milestone item if product wants it).
- **Accurate stats aggregation** — if per-registry `itemCount / reservedCount / givenCount` isn't already in the query, Phase 10 renders best-effort values (may show zeros for counts it can't cheaply compute). Proper aggregation deferred.
- **Notifications bell placement in v1.1** — moved off Home top bar. Final home for the bell (Settings row, AddActionSheet action, or new Home icon slot) deferred until Phase 11 sees the full owner nav picture.
- **isPrimary persistence / pinning** — Phase 10 ships the derived-from-updatedAt rule. Owner-controlled pinning is out of v1.1 scope.
- **Empty states** — v1.1 explicitly defers brand-new-user, empty-registry, and no-results empty states per REQUIREMENTS.md § Out of Scope. Phase 10 ships the minimum single-line empty state per tab.
- **Dark mode** — light mode only per Phase 8 Theme.kt v1.1 force; dark mode deferred.

</deferred>
