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
- [ ] **Phase 7: EMAG Catalog Integration** - Owners can browse and add items from EMAG product catalog (requires pre-phase API research spike)

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

### Phase 7: EMAG Catalog Integration
**Goal**: Registry owners can browse and search the EMAG product catalog from within the Android app and add items directly to their registry with affiliate tags applied automatically
**Depends on**: Phase 3
**Requirements**: ITEM-03, ITEM-04
**Success Criteria** (what must be TRUE):
  1. Owner can search for products in EMAG's catalog from the item add screen and see results with images, titles, and prices
  2. Owner can add a product from EMAG search results to their registry and the item receives an affiliate tag automatically
  3. If the EMAG catalog is unavailable, the UI shows an error state and the manual URL add path remains fully functional
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Firebase Foundation | 3/3 | Complete   | 2026-04-04 |
| 2. Android Core + Auth | 0/4 | Planning complete | - |
| 3. Registry + Item Management | 5/6 | In Progress|  |
| 4. Reservation System | 6/6 | Complete   | 2026-04-11 |
| 5. Web Fallback | 7/7 | Complete   | 2026-04-19 |
| 6. Notifications + Email Flows | 6/6 | Complete   | 2026-04-19 |
| 7. EMAG Catalog Integration | 0/TBD | Not started | - |
