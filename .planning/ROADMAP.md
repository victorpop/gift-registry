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
- [ ] **Phase 8: GiftMaison Design Foundation** - Ship fonts, type scale, colour tokens, spacing/radii/shadows, and the reusable "GiftMaison" wordmark as the design system every v1.1 screen builds on
- [ ] **Phase 9: Shared Chrome + Status UI** - Bottom nav, centre FAB, Add-action bottom sheet, and the Reserved/Given/Open/Purchased status treatments shipped as shared UI the owner screens assemble from
- [ ] **Phase 10: Onboarding + Home Redesign** - Owner-facing Onboarding/sign up (06) and Home/all-registries (07) screens match the handoff pixel-accurately with the new design system
- [ ] **Phase 11: Registry Detail + Create + Add Item Redesign** - Registry detail (08), Create registry (09), and Add item via URL (10) screens match the handoff pixel-accurately

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
**Plans**: TBD

### Phase 9: Shared Chrome + Status UI
**Goal**: The bottom nav / centre FAB / Add-action bottom sheet, and the Reserved/Given/Open/Purchased status treatments are shipped as shared UI the owner screens can assemble from
**Depends on**: Phase 8
**Requirements**: CHROME-01, CHROME-02, CHROME-03, STAT-01, STAT-02, STAT-03, STAT-04
**Success Criteria** (what must be TRUE):
  1. The bottom nav renders 5 slots (Home · Stores · +FAB · Lists · You) with stroked icons and mono-caps labels; the selected slot shows the accentSoft pill + accent stroke, and the nav is hidden on screens 06/09/10 while visible on 07/08
  2. The centre FAB is a 54 px accent circle lifted 22 px above the bar with accent shadow and paper ring; tapping it opens the Add-action bottom sheet over a scrim/blurred home with drag handle, title, and 4 action rows (New registry / Item from URL / Browse stores / Add manually)
  3. Reserved, Given, and Open status chips render with the handoff-specified pill styles, and the Reserved chip's 4 px dot pulses at the 1.4 s cadence with an "Nm" countdown that updates once per minute
  4. A purchased item row renders at 55 % opacity with a grayscale + ink-tinted image, a centred ✓ mark, and a strikethrough title while remaining visible in the list
**Plans**: TBD
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
**Plans**: TBD
**UI hint**: yes

### Phase 11: Registry Detail + Create + Add Item Redesign
**Goal**: The Registry detail (08), Create registry (09), and Add item via URL (10) screens match the handoff pixel-accurately
**Depends on**: Phase 10
**Requirements**: SCR-08, SCR-09, SCR-10
**Success Criteria** (what must be TRUE):
  1. The Registry detail screen renders the 180 px hero with gradient + pinned toolbar, the 4-stat strip, the accentSoft share banner pill, horizontally scrolling filter chips (All / Open / Reserved / Completed), and full-width item rows with 58 px thumbnail + status chip + overflow button — backed by the existing items query and status from the reservation system
  2. The Create registry screen renders the "Step 1 of 2" app bar with Skip, the italic-accent "What's the occasion?" headline, the 2×3 occasion tile grid with selected/unselected states, name/date/time/place fields, the visibility radio card, and the bottom CTA bar — and submitting the form creates a registry through the existing repository path
  3. The Add item (paste URL) screen renders the × close app bar, the 3-tab segmented control (Paste URL / Browse stores / Manual), the URL field with "Fetching from {domain}" pulsing-dot status and affiliate confirmation row, the preview card, optional note field, info pill, and dual CTA bar — and pasting a URL drives the existing Open Graph + affiliate-tagging add-item flow
**Plans**: TBD
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
| 8. GiftMaison Design Foundation | 0/0 | Not started | - |
| 9. Shared Chrome + Status UI | 0/0 | Not started | - |
| 10. Onboarding + Home Redesign | 0/0 | Not started | - |
| 11. Registry Detail + Create + Add Item Redesign | 0/0 | Not started | - |
