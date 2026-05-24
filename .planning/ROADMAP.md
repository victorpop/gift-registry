# Roadmap: Gift Registry

## Overview

Build a Romanian-market gift registry Android app with a web fallback for gift givers. The delivery sequence is dictated by dependencies: Firebase data model and security rules must precede all feature code; authentication unlocks owner features; the reservation system — the core value — ships as a single coherent unit; the web fallback depends on the full reservation flow existing; and the EMAG catalog is deliberately last because its API strategy is unresolved. Multilingual scaffolding is in place from Phase 2 onward to avoid expensive retrofit.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Firebase Foundation** - Define Firestore schema, security rules, and project infrastructure before any feature code exists (completed 2026-04-04)
- [x] **Phase 2: Android Core + Auth** - Android app scaffold with clean architecture, authentication flows, and multilingual scaffolding (completed 2026-04-05)
- [x] **Phase 3: Registry + Item Management** - Owners can create registries, add items via URL, and manage their lists with affiliate injection live (completed 2026-04-06)
- [x] **Phase 4: Reservation System** - Server-authoritative 30-minute reservation timer with race condition prevention and auto-expiry (completed 2026-04-11)
- [x] **Phase 5: Web Fallback** - Gift givers can view and reserve from a browser without installing the Android app (completed 2026-04-19)
- [x] **Phase 6: Notifications + Email Flows** - Expiry emails, re-reserve flow, owner push notifications, and private registry invites (completed 2026-04-19)
- [x] **Phase 7: Romanian Store Browser** - Owners can browse popular Romanian retailers in an in-app WebView with a persistent "Add to list" CTA that funnels products into the existing URL-based add flow (replaces original EMAG Catalog scope — no confirmed public EMAG catalog API) (completed 2026-04-20)
- [x] **Phase 8: GiftMaison Design Foundation** - Ship fonts, type scale, colour tokens, spacing/radii/shadows, and the reusable "GiftMaison" wordmark as the design system every v1.1 screen builds on (completed 2026-04-21)
- [x] **Phase 9: Shared Chrome + Status UI** - Bottom nav, centre FAB, Add-action bottom sheet, and the Reserved/Given/Open/Purchased status treatments shipped as shared UI the owner screens assemble from (completed 2026-04-21)
- [x] **Phase 10: Onboarding + Home Redesign** - Owner-facing Onboarding/sign up (06) and Home/all-registries (07) screens match the handoff pixel-accurately with the new design system (completed 2026-04-21)
- [x] **Phase 11: Registry Detail + Create + Add Item Redesign** - Registry detail (08), Create registry (09), and Add item via URL (10) screens match the handoff pixel-accurately (completed 2026-04-21)
- [x] **Phase 13: Web Fallback Visual Refresh** - Restyle the existing Phase 5 web fallback to match the GiftMaison design language so the giver-facing web flow visually aligns with the redesigned Android owner flow (handoff to be provided) (completed 2026-05-12)
- [x] **Phase 14: Web Fallback Live Deploy + Guest UAT** - Register Firebase Web app, ship the redesigned web bundle to gift-registry-ro.web.app with backing Cloud Functions/rules deployed, and close WEB-01..04 human-UAT items via end-to-end guest flow validation (all 4 plans complete 2026-05-22; phase pending verification) (completed 2026-05-22)

## Phase Details

### Phase 1: Firebase Foundation
**Goal**: Firebase project is configured with a stable Firestore schema and tested security rules so all subsequent feature code builds on a correct, non-reworkable foundation
**Depends on**: Nothing (first phase)
**Requirements**: I18N-02
**Success Criteria** (what must be TRUE):
  1. Firebase project exists with Auth, Firestore, Cloud Functions, Hosting, and App Check enabled and connected to the Android app
  2. Firestore collections (`registries`, `items`, `reservations`) have a defined schema with all fields named and typed
  3. Security rules pass simulator tests for public registry read, private registry owner-only read, guest reservation write, and owner-only item write
  4. `assetlinks.json` placeholder is served from Firebase Hosting for future Android App Links verification
  5. All UI string resource files (strings.xml for Android, en.json and ro.json for web) have their structure defined with keys in place
**Plans**: 3 plans
Plans:
- [x] 01-01-PLAN.md -- Firebase project setup and scaffold (checkpoint: human-action for project creation)
- [x] 01-02-PLAN.md -- Firestore security rules + automated tests (TDD)
- [x] 01-03-PLAN.md -- String resources (Android + web i18n) and hosting assets (I18N-02)

### Phase 2: Android Core + Auth
**Goal**: Users can create accounts, log in, and authenticate as guests — and the Android app scaffold with clean architecture, Hilt DI, Navigation3, and multilingual support is fully wired
**Depends on**: Phase 1
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, I18N-01, I18N-03
**Success Criteria** (what must be TRUE):
  1. User can sign up with email and password and their account persists across app restarts
  2. User can log in with email/password and with Google OAuth
  3. Gift giver can access a registry as a guest by providing first name, last name, and email — no account required
  4. Guest is offered account creation after completing a reservation action
  5. App UI displays in Romanian and English, auto-detected from device locale with a manual override available
**Plans**: 4 plans
Plans:
- [x] 02-01-PLAN.md -- Android project scaffold: Gradle, Hilt, Compose, Firebase, Material3 theme, DI modules (checkpoint: Firebase app registration)
- [x] 02-02-PLAN.md -- Auth domain + data layers: repository, use cases, Firebase data source, locale DataStore, DI bindings, test fake
- [x] 02-03-PLAN.md -- Auth UI screen, ViewModel with TDD, Navigation3 gating, Google Sign-In via Credential Manager, string resources
- [x] 02-04-PLAN.md -- Settings screen with language picker, locale persistence + restoration, guest conversion bottom sheet
**UI hint**: yes

### Phase 3: Registry + Item Management
**Goal**: Registry owners can create and manage registries, add items via any URL with automatic affiliate tag injection, and gift givers see real-time item status
**Depends on**: Phase 2
**Requirements**: REG-01, REG-02, REG-03, REG-04, REG-05, REG-06, REG-07, REG-08, REG-09, REG-10, ITEM-01, ITEM-02, ITEM-05, ITEM-06, ITEM-07, AFF-01, AFF-02, AFF-03, AFF-04
**Success Criteria** (what must be TRUE):
  1. Owner can create a registry with occasion type, event date/time, location, description, and visibility (public or private invite-only)
  2. Owner can add an item by pasting any URL and the title, image, and price auto-fill from Open Graph metadata with an EMAG affiliate tag injected automatically
  3. Owner can edit item details, remove items, and delete or update any registry they own
  4. Owner can invite specific users to a private registry; existing users receive an in-app notification and email, non-users receive email only
  5. Items display their current status (available, reserved, purchased) in real time visible to all viewers
**Plans**: 6 plans
Plans:
- [x] 03-00-PLAN.md -- Wave 0: Test dependencies, fake repositories, and test stubs for Nyquist compliance
- [x] 03-01-PLAN.md -- Foundation: Gradle deps, domain models, repository interfaces, AffiliateUrlTransformer (TDD), nav keys, string resources
- [x] 03-02-PLAN.md -- Cloud Functions: fetchOgMetadata callable + inviteToRegistry stub callable
- [x] 03-03-PLAN.md -- Data layer: FirestoreDataSource, RegistryRepositoryImpl, ItemRepositoryImpl, use cases, Hilt DI wiring
- [x] 03-04-PLAN.md -- Registry UI: list screen (home), create/edit screen, detail screen with ViewModels, Navigation3 wiring
- [ ] 03-05-PLAN.md -- Item UI: add item with OG auto-fill, edit item, invite bottom sheet, deep link routing (REG-08), security rule tests
**UI hint**: yes

### Phase 4: Reservation System
**Goal**: Gift givers can reliably reserve an available item with a server-authoritative 30-minute timer, no duplicate reservations, and automatic release on expiry
**Depends on**: Phase 3
**Requirements**: RES-01, RES-02, RES-03, RES-04, RES-05, RES-06, RES-07, RES-08, RES-09
**Success Criteria** (what must be TRUE):
  1. Gift giver can reserve an available item and is immediately redirected to the retailer; the item shows as unavailable to all other givers in real time
  2. Two givers attempting to reserve the same item simultaneously results in exactly one reservation succeeding and the other receiving a conflict message
  3. A reservation auto-releases after 30 minutes if not confirmed as purchased, and the item returns to available status in real time
  4. Giver receives an expiration email when their reservation lapses, with a one-click option to re-reserve
**Plans**: 4 plans
Plans:
- [x] 04-01-PLAN.md -- Wave 0: Cloud Function stubs, @google-cloud/tasks install, failing Kotlin + security rule test scaffolding
- [x] 04-02-PLAN.md -- Cloud Functions: createReservation (transaction + Cloud Tasks enqueue) + releaseReservation (onTaskDispatched guard + stub email)
- [x] 04-03-PLAN.md -- Android domain + data: ReservationRepository, ReserveItemUseCase, GuestPreferencesDataStore, Hilt wiring
- [x] 04-04-PLAN.md -- Android UI: RegistryDetailScreen reserve button, GuestIdentitySheet, countdown, retailer Intent, re-reserve deep link (checkpoint)
**UI hint**: yes

### Phase 5: Web Fallback
**Goal**: Gift givers can view a registry, log in or continue as guest, reserve an item, and be redirected to the retailer entirely from a web browser without installing the Android app
**Depends on**: Phase 4
**Requirements**: WEB-01, WEB-02, WEB-03, WEB-04
**Success Criteria** (what must be TRUE):
  1. Gift giver can open a shareable registry link in a desktop or mobile browser and see the full item list with real-time availability status
  2. Gift giver can log in, create an account, or continue as guest from the web fallback
  3. Gift giver can reserve an item on web and is redirected to the retailer — the same reservation flow and 30-minute timer as Android applies
  4. Web fallback respects private registry access rules — uninvited users cannot view a private registry
**Plans**: 7 plans
Plans:
- [x] 05-01-PLAN.md -- Vite + React 19 + TS scaffold, Tailwind v3, Vitest + Playwright, hosting/public build target
- [x] 05-02-PLAN.md -- Firebase JS SDK init (europe-west3 pin + App Check + emulator wiring) + TanStack QueryClient
- [x] 05-03-PLAN.md -- i18next setup + React Router v7 data mode + page stubs (WEB-01 precondition)
- [x] 05-04-PLAN.md -- Registry view: onSnapshot hooks, header, item grid, skeletons, generic 404 for denied/missing (WEB-01, WEB-04 privacy)
- [x] 05-05-PLAN.md -- Auth + Guest identity: useAuth, useGuestIdentity, AuthModal, GuestIdentityModal (WEB-03)
- [x] 05-06-PLAN.md -- Reservation flow: ReserveButton, Toast, ReservationBanner, createReservation callable, retailer redirect (WEB-02, WEB-04)
- [x] 05-07-PLAN.md -- Re-reserve deep link page + autoReserveItemId auto-fire on RegistryPage (WEB-02)
**UI hint**: yes

### Phase 6: Notifications + Email Flows
**Goal**: Owners receive timely purchase notifications (if opted in) and the expiry and invite email flows are fully operational end-to-end
**Depends on**: Phase 5
**Requirements**: NOTF-01, NOTF-02, NOTF-03
**Success Criteria** (what must be TRUE):
  1. Owner receives a push notification and email when a gift is purchased, only if they have opted in via registry settings
  2. Giver receives an expiration email when their reservation timer lapses, and clicking re-reserve in that email initiates a new reservation through the same transaction path
  3. Owner opting out of purchase notifications receives neither push nor email when a gift is reserved or purchased
**Plans**: TBD

### Phase 7: Romanian Store Browser
**Goal**: Registry owners can browse a curated list of popular Romanian retailers, open any store in an in-app WebView, and add products to a registry via a persistent bottom "Add to list" CTA that pipes the current URL into the existing affiliate-tagging add-item flow
**Depends on**: Phase 3
**Requirements**: STORE-01, STORE-02, STORE-03, STORE-04
**Success Criteria** (what must be TRUE):
  1. Owner can open a "Browse stores" entry point from the registry detail or home screen and see a curated list of popular Romanian retailers with logos and names
  2. Tapping a store opens an in-app WebView at the retailer's homepage; the WebView retains a persistent bottom bar with an "Add to list" primary button that remains accessible while the user browses
  3. Tapping "Add to list" opens the existing add-item sheet pre-filled with the current WebView URL; confirming adds the item to the selected registry with affiliate tag applied automatically (reuses Phase 3 AffiliateUrlTransformer and Open Graph fetch path)
  4. If the store page fails to load (offline, blocked, 500), the WebView shows an error state and the "Add to list" button is disabled; users can retry or back out without breaking the nav stack

**Scope notes:**
- Replaces the original "EMAG Catalog Integration" phase after confirming no public EMAG catalog API exists. Original REQ-IDs ITEM-03/ITEM-04 are retired (moved to Out of Scope in REQUIREMENTS.md).
- Android-only. Web fallback is giver-only (PROJECT.md constraint) so no web variant.
- Curated store list is a small static config (stored in code or Firestore config doc — decided during discuss). No store admin UI in this phase.
- The add flow itself already ships in Phase 3; this phase is about the funnel into it.

**Plans**: 4 plans
Plans:
- [x] 07-00-PLAN.md -- Wave 1 foundation: seed data/script, Firestore rules for config/stores, ProGuard keep rule, stores_* strings (en + ro), 9 bundled store logos (human checkpoint)
- [x] 07-01-PLAN.md -- Wave 2: domain + data layer (Store model, StoreRepository, LastRegistryPreferencesDataStore, StoresModule, unit tests)
- [x] 07-02-PLAN.md -- Wave 3: StoreListScreen + StoreListViewModel + Home FAB menu refactor + Registry Detail entry + AppNavigation wiring
- [x] 07-03-PLAN.md -- Wave 4: StoreBrowserScreen (AndroidView-wrapped WebView) + AddItemKey extension + end-to-end UAT (human checkpoint)
**UI hint**: yes

## v1.1 Milestone: GiftMaison visual refresh

**Milestone goal:** Replace the current owner-facing Android UI with the GiftMaison design system — pixel-accurate to the 2026-04-20 `design_handoff_android_owner_flow/` package — across all 5 owner screens plus shared chrome, without reworking navigation, repositories, ViewModels, or Cloud Functions. This is a re-skin, not a rebuild: every phase must preserve existing behaviour.

The four v1.1 phases are ordered by dependency. Phase 8 ships the design primitives every other phase consumes. Phase 9 ships the bottom nav, centre FAB, Add-action bottom sheet, and status chip treatments — the cross-cutting UI visible on multiple screens. Phase 10 rebuilds the two screens that stand on their own (Onboarding, Home). Phase 11 rebuilds the three remaining owner screens (Registry detail, Create registry, Add item).

**v1.1 scope note — themes:** the handoff defines four occasion themes (Housewarming / Wedding / Baby / Birthday) with per-registry runtime cascade. v1.1 ships **Housewarming only**. THEME-01, THEME-02, and THEME-03 are deferred to v1.2. DES-03 (Phase 8) locks in the Housewarming palette, which is all v1.1 needs.

Out of scope for v1.1 (per handoff): giver-facing web fallback, Settings / profile, notifications inbox, dark mode, empty states, store-browser WebView chrome, email templates.

### Phase 8: GiftMaison Design Foundation
**Goal**: The GiftMaison design primitives — fonts, type scale, colour tokens, spacing/radii/shadows, and the reusable wordmark — are shipped as Compose-native values so every subsequent v1.1 screen can consume them without duplication
**Depends on**: Phase 7 (v1.0 complete — no functional dependency, but v1.0 owner screens remain the baseline being replaced)
**Requirements**: DES-01, DES-02, DES-03, DES-04, DES-05
**Success Criteria** (what must be TRUE):
  1. Instrument Serif, Inter, and JetBrains Mono render correctly on-device via Compose `FontFamily` values exposed through the app's theme
  2. A Compose preview or debug harness shows the full GiftMaison type scale (Display XL/L/M/S, Body L/M/S/XS, Mono caps) with the handoff-specified sizes, weights, letter-spacing, and line-heights
  3. A Compose preview or debug harness shows the full Housewarming colour palette (paper, paperDeep, ink, inkSoft, inkFaint, line, accent, accentInk, accentSoft, second, secondSoft, ok, warn) rendered as sRGB swatches matching the handoff values
  4. Handoff-specified spacing units, radii (8/10/12/14/16/22/999), and shadows (FAB, Google banner, bottom sheet) are available as named design-system values referenced consistently by sample previews
  5. The "GiftMaison" wordmark (Instrument Serif italic with a terracotta-accent period) renders as a single reusable composable that can be dropped into any top bar
**Plans**: 5 plans
Plans:
- [x] 08-01-PLAN.md -- Wave 0: compose-ui-text-google-fonts dep + font_certs.xml + 5 failing theme unit test files (DES-01..05 RED)
- [x] 08-02-PLAN.md -- Housewarming colour tokens (DES-03): GiftMaisonColors data class + housewarmingColors() factory + LocalGiftMaisonColors CompositionLocal
- [x] 08-03-PLAN.md -- Fonts + Typography (DES-01 + DES-02): GoogleFont provider + 3 FontFamilys + 10 TextStyle roles with em-based letter-spacing/line-height
- [x] 08-04-PLAN.md -- Shapes + Spacing + Shadows (DES-04): 7 radii, 10 spacing values, 3 shadow Modifier extensions
- [x] 08-05-PLAN.md -- Integration (DES-05): wordmark composable + Theme/Color/Type rewire (42+ screens re-skin) + StyleGuidePreview harness
**UI hint**: yes

### Phase 9: Shared Chrome + Status UI
**Goal**: The bottom nav / centre FAB / Add-action bottom sheet, and the Reserved/Given/Open/Purchased status treatments are shipped as shared UI the owner screens can assemble from
**Depends on**: Phase 8
**Requirements**: CHROME-01, CHROME-02, CHROME-03, STAT-01, STAT-02, STAT-03, STAT-04
**Success Criteria** (what must be TRUE):
  1. The bottom nav renders 5 slots (Home · Stores · +FAB · Lists · You) with stroked icons and mono-caps labels; the selected slot shows the accentSoft pill + accent stroke, and the nav is hidden on screens 06/09/10 while visible on 07/08
  2. The centre FAB is a 54 px accent circle lifted 22 px above the bar with accent shadow and paper ring; tapping it opens the Add-action bottom sheet over a scrim/blurred home with drag handle, title, and 4 action rows (New registry / Item from URL / Browse stores / Add manually)
  3. Reserved, Given, and Open status chips render with the handoff-specified pill styles, and the Reserved chip's 4 px dot pulses at the 1.4 s cadence with an "Nm" countdown that updates once per minute
  4. A purchased item row renders at 55 % opacity with a grayscale + ink-tinted image, a centred ✓ mark, and a strikethrough title while remaining visible in the list
**Plans**: 4 plans
Plans:
- [x] 09-01-PLAN.md -- Wave 0: 5 failing unit test stubs for CHROME-01 predicate + STAT-01/02/03/04 chip/dispatcher/modifier behaviour (RED)
- [x] 09-02-PLAN.md -- Wave 1 — ui/common/status/: PulsingDot + StatusChip (dispatcher + Reserved/Given/Open) + PurchasedRowModifier + 7 status string keys (STAT-01..04 RED→GREEN)
- [x] 09-03-PLAN.md -- Wave 1 — ui/common/chrome/: GiftMaisonBottomNav + showsBottomNav predicate + GiftMaisonFab + AddActionSheet + 13 chrome/sheet string keys (CHROME-01 RED→GREEN)
- [x] 09-04-PLAN.md -- Wave 2 — integration: wire GiftMaisonBottomNav + AddActionSheet into AppNavigation (replace old NavigationBar, isPrimary resolver, blur fallback); swap RegistryDetail inline ItemStatusChip + ReservationCountdown for shared StatusChip; append StyleGuidePreview sections (checkpoint: on-device)
**UI hint**: yes

### Phase 10: Onboarding + Home Redesign
**Goal**: The Onboarding/sign up screen (06) and the Home/all-registries screen (07) match the handoff pixel-accurately, preserving existing auth and registry-list behaviour
**Depends on**: Phase 9
**Requirements**: SCR-06, SCR-07
**Success Criteria** (what must be TRUE):
  1. The Onboarding screen renders the wordmark top bar, "Start your / first registry." italic-accent headline, Google banner with concentric rings, "or sign up with email" divider, first-name/last-name/email/password fields with accent focus ring, primary ink pill CTA, Terms line, and "Log in" footer pill — and the existing sign-up / sign-in / Google OAuth flows continue to work unchanged
  2. Switching between sign-up and sign-in modes on the Onboarding screen is reachable via the footer "Log in" pill and the same ViewModel logic drives both modes
  3. The Home screen renders the wordmark + avatar top bar, "Your registries" display-serif headline with mono-caps caption, 3-tab Active / Drafts / Past segmented control, and a scrolling list of registry cards (16:9 hero + occasion pill + date + title + stats) with exactly one dark "primary" card at a time — backed by the existing registries query
  4. The bottom nav and centre FAB from Phase 9 appear on Home; tapping a registry card navigates to the existing Registry detail route
**Plans**: 5 plans
Plans:
- [x] 10-01-PLAN.md -- Wave 0: 6 failing pure-Kotlin unit test files (SCR-06 + SCR-07 RED) — TabFilterPredicate, DraftHeuristic, IsPrimarySelection, AvatarInitials, AuthHeadline, AuthFormState
- [x] 10-02-PLAN.md -- Wave 1 shared primitives: Registry.imageUrl + TabFilters helpers + AvatarInitials + 5 Compose composables (AvatarButton, FocusedFieldCaret, ConcentricRings, GoogleBanner, SegmentedTabs)
- [x] 10-03-PLAN.md -- Wave 1 SCR-06 AuthScreen re-skin: AuthFormState firstName/lastName + AuthHeadline + sign-up default + 23 auth_* strings (EN + RO) + AUTH-05 guest path preserved
- [x] 10-04-PLAN.md -- Wave 1 SCR-07 Home re-skin: RegistryListViewModel.currentUser + HomeTopBar + RegistryCard (primary + secondary) + RegistryListScreen + AppNavigation onNavigateToSettings + 15 home_* strings (EN + RO)
- [ ] 10-05-PLAN.md -- Wave 2 integration: 5 StyleGuidePreview sections + on-device human checkpoint (17-check UAT + locale + regression guards)
**UI hint**: yes

### Phase 11: Registry Detail + Create + Add Item Redesign
**Goal**: The Registry detail (08), Create registry (09), and Add item via URL (10) screens match the handoff pixel-accurately
**Depends on**: Phase 10
**Requirements**: SCR-08, SCR-09, SCR-10
**Success Criteria** (what must be TRUE):
  1. The Registry detail screen renders the 180 px hero with gradient + pinned toolbar, the 4-stat strip, the accentSoft share banner pill, horizontally scrolling filter chips (All / Open / Reserved / Completed), and full-width item rows with 58 px thumbnail + status chip + overflow button — backed by the existing items query and status from the reservation system
  2. The Create registry screen renders the "Step 1 of 2" app bar with Skip, the italic-accent "What's the occasion?" headline, the 2×3 occasion tile grid with selected/unselected states, name/date/time/place fields, the visibility radio card, and the bottom CTA bar — and submitting the form creates a registry through the existing repository path
  3. The Add item (paste URL) screen renders the × close app bar, the 3-tab segmented control (Paste URL / Browse stores / Manual), the URL field with "Fetching from {domain}" pulsing-dot status and affiliate confirmation row, the preview card, optional note field, info pill, and dual CTA bar — and pasting a URL drives the existing Open Graph + affiliate-tagging add-item flow
**Plans**: 6 plans
Plans:
- [x] 11-01-PLAN.md -- Wave 0: 7 failing pure-Kotlin unit test files (SCR-08/09/10 RED) — FilterChipState, HeroToolbarAlpha, RegistryStats, ShareUrl, OccasionCatalog, AddItemMode, AffiliateRowVisibility
- [x] 11-02-PLAN.md -- Wave 1 shared helpers: AffiliateUrlTransformer.isAffiliateDomain + 7 pure-Kotlin Phase 11 helpers (flips all 7 Wave 0 tests GREEN)
- [x] 11-03-PLAN.md -- Wave 1 SCR-08 Registry Detail re-skin: 5 sub-composables (RegistryDetailHero / StatsStrip / ShareBanner / FilterChipsRow / RegistryItemRow) + 13 registry_detail_* strings (EN + RO) + Box+LazyColumn shell (preserves reservation / delete / invite / FCM push)
- [x] 11-04-PLAN.md -- Wave 1 SCR-09 Create Registry re-skin: OccasionTileGrid + VisibilityRadioCard + 13 registry_create_* strings + AppNavigation onSaved rewired to AddItemKey + onSkip → HomeKey
- [x] 11-05-PLAN.md -- Wave 1 SCR-10 Add Item re-skin: 6 sub-composables (FetchingIndicator / AffiliateConfirmationRow / ItemPreviewCard / AutoFillTag / InfoPill / AddItemDualCtaBar) + VM derived flows + 12-14 add_item_* strings + AppNavigation onNavigateToBrowseStores wiring
- [x] 11-06-PLAN.md -- Wave 2 integration: 7 StyleGuidePreview sections + on-device human UAT (41-check checklist covering SCR-08/09/10 + locale + regression guards)
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9 -> 10 -> 11

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Firebase Foundation | 3/3 | Complete   | 2026-04-04 |
| 2. Android Core + Auth | 0/4 | Planning complete | - |
| 3. Registry + Item Management | 5/6 | In Progress|  |
| 4. Reservation System | 6/6 | Complete   | 2026-04-11 |
| 5. Web Fallback | 7/7 | Complete   | 2026-04-19 |
| 6. Notifications + Email Flows | 6/6 | Complete   | 2026-04-19 |
| 7. Romanian Store Browser | 4/4 | Complete   | 2026-04-20 |
| 8. GiftMaison Design Foundation | 0/5 | Planning complete | - |
| 9. Shared Chrome + Status UI | 4/4 | Complete   | 2026-04-21 |
| 10. Onboarding + Home Redesign | 4/5 | Complete    | 2026-04-21 |
| 11. Registry Detail + Create + Add Item Redesign | 6/6 | Complete   | 2026-04-21 |
| 12. Registry Cover Photo & Themed Placeholder | 5/5 | Complete    | 2026-04-28 |
| 13. Web Fallback Visual Refresh | 8/8 | Complete    | 2026-05-12 |
| 14. Web Fallback Live Deploy + Guest UAT | 4/4 | Complete    | 2026-05-22 |
| 15. Web Invite-Landing + Magic-Link Guest Flow | 0/5 | Planning complete | - |
| 16. Android Notifications Inbox + Invite Accept/Decline | 1/6 | In Progress|  |

### Phase 12: Registry Cover Photo & Themed Placeholder
**Goal**: Registry owners can pick a cover photo (bundled per-occasion preset OR Android Photo Picker upload to Firebase Storage) on Create, Edit, and Registry Detail surfaces; registries without a cover render the GiftMaison gradient + occasion-glyph placeholder consistently across the 180 dp hero and both registry card variants
**Depends on**: Phase 11
**Requirements**: D-01..D-16 (see .planning/phases/12-registry-cover-photo-themed-placeholder/12-CONTEXT.md — Phase 12 has no REQ-IDs in REQUIREMENTS.md; CONTEXT decisions are the requirement set)
**Success Criteria** (what must be TRUE):
  1. RegistryCardPrimary AND RegistryCardSecondary render the gradient + occasion glyph when `registry.imageUrl == null` (visible bug fix)
  2. RegistryDetailHero pixel contract from Phase 11 is preserved (40 sp glyph, 3-stop dark overlay only on real images)
  3. Owner can pick from 6 bundled per-occasion preset JPEGs via a Material3 ModalBottomSheet picker on Create / Edit / Detail
  4. Owner can pick from gallery via Android Photo Picker; image is downscaled to 1280×720 JPEG q=85 and uploaded to Firebase Storage at `/users/{uid}/registries/{registryId}/cover.jpg`
  5. Picker is disabled until an occasion is selected; switching occasion clears any picked preset; tap target on the Detail hero is owner-only
  6. `storage.rules` cross-service rules deny non-owner write and non-member read; deployed to gift-registry-ro
  7. `RegistryDto` + `RegistryRepositoryImpl.toMap`/`toUpdateMap` round-trip `imageUrl` (Phase 12 fixes the silent-data-loss bug from Phase 10)
**Plans**: 5 plans
Plans:
- [x] 12-01-PLAN.md -- Wave 0: 8 failing pure-Kotlin RED tests + 6 stub files + firebase-storage dep + storage emulator wiring (D-02/05/06/07/11/12/14/16 RED)
- [x] 12-02-PLAN.md -- Wave 1: Pitfall 1 fix (RegistryDto.imageUrl + RegistryRepositoryImpl mappers) + 36 placeholder JPEGs + PresetCatalog populated + StorageRepository/CoverImageProcessor impls + StorageModule + storage.rules
- [x] 12-03-PLAN.md -- Wave 1: HeroImageOrPlaceholder + CoverPhotoPickerInline + CoverPhotoPickerSheet + PresetThumbnail; refactor RegistryCard primary/secondary + RegistryDetailHero to consume the shared composable
- [x] 12-04-PLAN.md -- Wave 2: CreateRegistryViewModel + Screen wiring (inline picker above OccasionTileGrid; sheet host; create-mode two-writes-zero-orphans upload); RegistryDetailScreen owner-only tap target; 10 cover_photo_* strings × 2 locales
- [x] 12-05-PLAN.md -- Wave 3: 4 StyleGuidePreview sections + on-device UAT (12-check checklist) + storage.rules deploy (human checkpoint)
**UI hint**: yes

### Phase 13: Web Fallback Visual Refresh

**Goal:** Restyle the existing Phase 5 web fallback (giver flow: registry view, reserve, retailer redirect, re-reserve deep link) to match the GiftMaison design language shipped on Android in Phases 8-12, per the web-specific design handoff at `design_handoff_web_giver_flow/`. Functional behaviour, routing, and Firebase wiring stay unchanged — this is a visual layer refresh on the existing React/Vite codebase.
**Requirements**: D-01..D-18 (CONTEXT decisions; WEB-01..04 already complete in Phase 5 — this phase is the *visual* refresh of them)
**Depends on:** Phase 12
**Plans:** 8/8 plans complete

Plans:
- [x] 13-00-PLAN.md — Wave 1 foundation: Tailwind extension + :root --gm-* CSS vars + Google Fonts preconnect + body class swap + gm-pulse keyframe with reduced-motion fallback
- [x] 13-01-PLAN.md — Wave 2 atoms: Wordmark + Pill + Btn + Field + PulseDot + MonoCaption (web/src/components/giftmaison/) + barrel index
- [x] 13-02-PLAN.md — Wave 2 chrome: TopNav + Footer + StickyMobileBar + restyled LanguageSwitcher + AppRootPage canary
- [x] 13-03-PLAN.md — Wave 2 i18n: ~50 new keys in 6 web_* namespaces (en + ro + legacy seed at web/i18n/)
- [x] 13-04-PLAN.md — Wave 3 Screen 01 Registry detail: RegistryHeader + ProgressStrip + FilterChips + restyled ItemCard + ItemGrid + RegistryPage rewire (preserves auto-reserve plumbing)
- [x] 13-05-PLAN.md — Wave 4 Screen 02 Reserve: StickyReserveBanner (replaces ReservationBanner) + HowTimerWorks + ReserveDetailSection + restyled ConfirmPurchaseBanner; rendered in-page on /registry/:id when active
- [x] 13-06-PLAN.md — Wave 3 Screen 03 Auth: AuthScreen full-page (/sign-in route) + EditorialPhoto + GuestSkipCard + restyled AuthModal + og-default.png placeholder
- [x] 13-07-PLAN.md — Wave 5 polish + UAT: ReReservePage + NotFoundPage + GuestIdentityModal + ToastProvider token-pass + full Vitest re-baselining + human visual UAT (mobile + desktop, locale + reduced-motion + a11y)

### Phase 14: Web Fallback Live Deploy + Guest UAT

**Goal:** Get the redesigned web fallback rendering at https://gift-registry-ro.web.app against the production gift-registry-ro Firebase project (Firebase Web app registered, real env config wired, App Check posture matched), unblock the Cloud Functions deploy pipeline, and close the 7 outstanding WEB-01..04 human-UAT items by running an end-to-end guest reservation flow against prod.
**Requirements**: WEB-01, WEB-02, WEB-03, WEB-04 (human-UAT closure)
**Depends on:** Phase 13
**Plans:** 4/4 plans complete

Plans:
- [x] 14-01-register-web-app-and-hosting-deploy-PLAN.md — Register Firebase Web app + write web/.env.local + rebuild + hosting-only deploy (fixes blank page; closes folded todo register-firebase-web-app-and-deploy-real-web-config)
- [x] 14-02-functions-tsconfig-cleanup-and-deploy-PLAN.md — Apply tsconfig rootDir fix + commit functions/.env + deploy functions (closes folded todo fix-functions-tsconfig-and-env-handling)
- [x] 14-03-firestore-and-storage-rules-deploy-PLAN.md — Deploy firestore.rules + storage.rules (first-time storage deploy with cross-service grant; closes folded todo deploy-phase-12-storage-rules)
- [x] 14-04-layered-uat-and-appcheck-enforcement-PLAN.md — Register reCAPTCHA v3 + add OAuth origins + layered UAT (solo Pass 1 + recruited-giver Pass 2) covering all 7 manual UAT items + App Check monitor→enforce flip

### Phase 15: Web Invite-Landing + Magic-Link Guest Flow

**Goal:** Email-invited recipients who click the registry CTA link in their invite email see a dedicated landing modal that lets them either create an account (with inline email/password form) or continue as a magic-link guest (Firebase passwordless email-link sign-in). Both paths end with the user authenticated, their UID swapped into `registries.invitedUsers` by a 2nd-gen `beforeUserCreated` blocking Cloud Function, and landed on the shared registry page — with the modal dismissible at any point and the `?invite=1` URL signal stripped on close to keep the registry URL clean.
**Requirements**: TBD — Phase 15 has no formal REQ-IDs in REQUIREMENTS.md; the implementation decisions in `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md` `<decisions>` block are the requirement set. Each plan's `context_decisions` frontmatter field lists the sub-sections that plan addresses.
**Depends on:** Phase 14
**Plans:** 5 plans

Plans:
- [ ] 15-01-i18n-and-auth-providers-PLAN.md — Wave 1 foundation: extend en.json + ro.json with `invite_landing.*` namespace (18 keys × 2 locales); add `sendInviteSignInLink` + `completeInviteSignIn` + re-export `isSignInWithEmailLink` wrappers to `web/src/features/auth/authProviders.ts`
- [ ] 15-02-backend-url-builder-and-email-PLAN.md — Wave 1 backend: extend `buildRegistryUrl(id, { invite?: boolean })` in `functions/src/config/publicUrls.ts` with TDD coverage (8 tests); update `functions/src/registry/inviteToRegistry.ts` line 95 to pass `{ invite: true }` so invite emails carry `?invite=1`
- [ ] 15-03-invite-landing-modal-PLAN.md — Wave 2: new `web/src/features/auth/InviteLandingModal.tsx` (Radix Dialog mirroring SaveYourSpotModal style; two-state UI: initial choice → check-email confirmation; inline create-account form + sendInviteSignInLink secondary CTA + dismissible "Not now"); 6 Vitest cases
- [ ] 15-04-magic-link-callback-and-cloud-function-PLAN.md — Wave 2: new `web/src/pages/EmailLinkCallbackPage.tsx` + `/auth/email-link` route in `App.tsx` (6 Vitest cases); new `functions/src/auth/linkInviteOnSignup.ts` 2nd-gen `beforeUserCreated` blocking function that swaps `invitedUsers["email:{email}"]` → `invitedUsers[{newUid}]` in a transaction per matching registry (4 Jest cases); register in `functions/src/index.ts`. Requires Identity Platform + email-link sign-in enabled in Firebase Console.
- [ ] 15-05-registry-page-wiring-PLAN.md — Wave 3 integration: wire `InviteLandingModal` into `web/src/pages/RegistryPage.tsx` under the 3-gate condition (`searchParams.invite === '1'` + `useAuth().isReady` + `!user`) with `?invite=1` URL stripping on dismiss AND post-account-creation (5 Vitest cases); checkpoint:human-verify 16-step UAT against the Firebase emulator covering both create-account and magic-link paths

### Phase 16: Android Notifications Inbox + Invite Accept/Decline

**Goal:** Move the Android invite flow from auto-add-to-invitedUsers to a strict accept-gate model — new invites land in `registries.pendingInvitedUsers`; invited Android users see an actionable INVITE inbox card; tapping it opens a GiftMaison-styled bottom sheet with Accept/Decline CTAs; Accept atomically promotes the uid into `invitedUsers` (granting read access); owner sees both outcomes in their own inbox. Inbox screen is re-skinned to GiftMaison design language. Phase 15's `linkInviteOnSignup` blocking function must target `pendingInvitedUsers` when Phase 15 resumes.
**Requirements**: D-01..D-28 (CONTEXT.md decisions — Phase 16 has no formal REQ-IDs in REQUIREMENTS.md; the 28 decisions constitute the requirement set)
**Depends on:** Phase 15
**Plans:** 1/6 plans executed

Plans:
- [x] 16-01-wave-0-red-tests-and-index-PLAN.md — Wave 1 RED test scaffolding (5 Android + 2 Functions test files), extend rules tests for D-18/D-19, add composite index for inbox cleanup query
- [ ] 16-02-backend-callables-and-invite-pending-PLAN.md — Wave 2 backend: acceptInvite + declineInvite 2nd-gen onCall functions (europe-west3, enforceAppCheck: true), shared helpers, modify inviteToRegistry to write pendingInvitedUsers + enriched payload + D-16 already-member branch
- [ ] 16-03-android-domain-data-layer-PLAN.md — Wave 2 Android: extend NotificationType enum (3 new wire-mappable values), extend NotificationRepository interface + impl with acceptInvite/declineInvite httpsCallable wrappers
- [ ] 16-04-invite-response-sheet-and-viewmodel-PLAN.md — Wave 3: InviteResponseViewModel state machine, InviteResponseSheet ModalBottomSheet + DeclineConfirmDialog, shouldOpenInviteSheet predicate, wire NotificationsScreen tap-branching + sheet host
- [ ] 16-05-inbox-reskin-and-strings-PLAN.md — Wave 4: NotificationsScreen + NotificationCard GiftMaison re-skin (D-09), extend localizedTitle/Body for 3 new types, add 20 new strings × 2 locales, append StyleGuidePreview sections
- [ ] 16-06-deploy-and-uat-PLAN.md — Wave 5: wire Android App Check provider (resolves Phase 14 follow-up todo), deploy composite index + Cloud Functions, 18-scenario on-device UAT (human checkpoint)
