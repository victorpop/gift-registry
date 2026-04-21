# Requirements: Gift Registry

**Defined:** 2026-04-04
**Core Value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.

## v1.1 Requirements — GiftMaison visual refresh

Requirements for the v1.1 milestone. Owner-facing Android redesign per `design_handoff_android_owner_flow/README.md`. Each maps to a roadmap phase.

### Design Foundation

- [x] **DES-01**: Instrument Serif, Inter, and JetBrains Mono fonts integrated via Google Fonts and available as Compose `FontFamily` values across all screens
- [x] **DES-02**: Type scale defined (Display XL/L/M/S, Body L/M/S/XS, Mono caps) with handoff-specified sizes, weights, letter-spacing, and line-heights
- [x] **DES-03**: Colour token palette defined (paper, paperDeep, ink, inkSoft, inkFaint, line, accent, accentInk, accentSoft, second, secondSoft, ok, warn) as sRGB hex matching handoff Housewarming table
- [x] **DES-04**: Spacing, radii (8/10/12/14/16/22/999), and shadow specs (FAB, Google banner, bottom sheet) applied consistently per handoff
- [x] **DES-05**: "GiftMaison" wordmark component (Instrument Serif italic + terracotta accent period) reusable across top bars

### Shared Chrome

- [ ] **CHROME-01**: Bottom nav shows 5 slots (Home · Stores · +FAB · Lists · You) with stroked icons and mono caps labels; selected state uses accentSoft pill + accent stroke; hidden on screens 06, 09, 10
- [ ] **CHROME-02**: Centre FAB is a 54 px accent circle lifted 22 px above the bar with accent shadow and 4 px paper ring; tapping opens the Add action bottom sheet
- [ ] **CHROME-03**: Add action bottom sheet (22-radius top, drag handle, scrim over blurred home) offers 4 actions: New registry (primary), Item from URL, Browse stores, Add manually

### Screens

- [ ] **SCR-06**: Onboarding + sign up screen matches handoff (wordmark top bar, italic-accent headline, Google banner with concentric rings, divider, name/email/password fields with focus ring, primary pill CTA, Terms line, "Log in" footer pill)
- [ ] **SCR-07**: Home / all registries screen matches handoff (wordmark + avatar top bar, "Your registries" headline, 3-tab segmented Active/Drafts/Past, card list with 16:9 hero + occasion pill + date + title + stats; single dark "primary" card)
- [ ] **SCR-08**: Registry detail screen matches handoff (180 px hero with gradient + pinned toolbar, 4-stat strip, share banner pill, filter chips, full-width item rows with 58 px thumbnail + status chip + ⋯)
- [ ] **SCR-09**: Create registry screen matches handoff (Step 1 of 2 app bar with Skip, italic-accent headline, 2×3 occasion tile grid, form fields, visibility radio card, bottom CTA bar)
- [ ] **SCR-10**: Add item (paste URL) screen matches handoff (× close app bar, 3-tab segmented, URL field with "Fetching from {domain}" + affiliate confirmation row, preview card, auto-fill tag, optional note, info pill, dual CTA bar)

### Status UI

- [ ] **STAT-01**: Reserved chip uses filled accent pill with pulsing 4 px dot (1.4 s interval) and "Nm" countdown updated once per minute
- [ ] **STAT-02**: Given chip uses secondSoft fill with "✓ given" label
- [ ] **STAT-03**: Open chip uses outlined pill with line border and inkFaint text
- [ ] **STAT-04**: Purchased item row renders at 55 % opacity with grayscale image, ink tint, centred ✓, and strikethrough title — remains visible to viewers per handoff trust pattern

**v1.1 Open questions (deferred to phase planning):**
- Countdown re-render cadence (handoff suggests minute, display-only on owner screens)
- `isPrimary` card selection rule on Home (most-recent vs. pinned)
- Designs for Stores and You bottom-nav tabs (only Home and Lists are built out in handoff)

**v1.1 scope note — themes:** v1.1 ships Housewarming only. The four-theme occasion system (Wedding / Baby / Birthday palettes + per-registry runtime cascade) is deferred to v1.2. DES-03 above locks in the Housewarming colour palette, which is the only palette v1.1 needs.

## v1.0 Requirements (shipped)

Requirements for initial release — all shipped in v1.0. Each maps to a roadmap phase.

### Authentication

- [x] **AUTH-01**: User can sign up with email and password
- [x] **AUTH-02**: User can log in with email and password
- [x] **AUTH-03**: User can log in with Google OAuth
- [x] **AUTH-04**: User session persists across app restarts
- [x] **AUTH-05**: Guest can access registry and reserve items by providing first name, last name, and email
- [x] **AUTH-06**: Guest is offered account creation after completing a reservation

### Registry Management

- [x] **REG-01**: Owner can create a registry with a name, occasion type (Wedding, Baby shower, Anniversary, Christmas, custom), event date and time, event location, and a description
- [x] **REG-02**: Owner can edit all registry details (name, occasion type, date/time, location, description, visibility)
- [x] **REG-03**: Owner can delete a registry
- [x] **REG-10**: Owner can have multiple active registries simultaneously
- [x] **REG-04**: Owner can set registry visibility to public (shareable link) or private (invite-only)
- [x] **REG-05**: Owner can invite specific users to a private registry via email
- [x] **REG-06**: Invited users with accounts receive in-app notification and email
- [x] **REG-07**: Invited users without accounts receive email only with link to registry
- [x] **REG-08**: Invited non-users see login/signup/guest options upon accessing the link
- [x] **REG-09**: Owner can opt in or out of purchase notifications

### Item Management

- [x] **ITEM-01**: Owner can add an item by pasting any URL
- [x] **ITEM-02**: URL import auto-fills item title, image, and price via Open Graph metadata
- [~] **ITEM-03**: ~~Owner can browse and search EMAG product catalog via API~~ — **RETIRED 2026-04-19**: No public EMAG catalog API available. Replaced by STORE-01..04 (in-app WebView store browser).
- [~] **ITEM-04**: ~~Owner can add an item from EMAG catalog search results~~ — **RETIRED 2026-04-19**: Superseded by STORE-03 (add from WebView into existing URL-import flow).
- [x] **ITEM-05**: Owner can manually edit item details (title, image, price, notes)
- [x] **ITEM-06**: Owner can remove an item from a registry
- [x] **ITEM-07**: Items display real-time status (available, reserved, purchased)

### Affiliate System

- [x] **AFF-01**: URL transformer identifies merchant domain and appends correct affiliate tag
- [x] **AFF-02**: EMAG items receive affiliate tags automatically on add
- [x] **AFF-03**: Affiliate tag injection is invisible to users
- [x] **AFF-04**: Unknown merchant URLs pass through without breaking (no affiliate tag, logged for review)

### Reservation System

- [x] **RES-01**: Gift giver can reserve an available item
- [x] **RES-02**: Reserved items show as unavailable to other givers in real time
- [x] **RES-03**: 30-minute reservation timer starts on reserve action
- [x] **RES-04**: Giver is redirected to retailer immediately upon reservation
- [x] **RES-05**: Reservation auto-releases after 30 minutes if not confirmed as purchased
- [x] **RES-06**: Auto-released items return to available status in real time
- [x] **RES-07**: Giver receives expiration email when reservation lapses
- [x] **RES-08**: Expiration email includes option to re-reserve immediately
- [x] **RES-09**: Two givers cannot simultaneously reserve the same item (race condition prevention)

### Store Browser (replaces retired ITEM-03/04 EMAG API scope)

- [x] **STORE-01**: Owner can open a "Browse stores" entry point and see a curated list of popular Romanian retailers (logo + name)
- [x] **STORE-02**: Tapping a store opens the retailer's homepage in an in-app WebView
- [x] **STORE-03**: A persistent bottom "Add to list" button opens the existing add-item sheet pre-filled with the current WebView URL; confirming adds the item with affiliate tag applied via the Phase 3 AffiliateUrlTransformer
- [x] **STORE-04**: If the store page fails to load, the WebView shows an error state and the "Add to list" button is disabled; retry available, nav stack intact

### Notifications

- [x] **NOTF-01**: Owner receives push notification when a gift is purchased (if opted in)
- [x] **NOTF-02**: Owner receives email when a gift is purchased (if opted in)
- [x] **NOTF-03**: Giver receives expiration email when reservation timer expires

### Web Fallback

- [x] **WEB-01**: Gift givers can view a registry via web browser without installing the app
- [x] **WEB-02**: Gift givers can reserve items from the web fallback
- [x] **WEB-03**: Gift givers can log in, create an account, or continue as guest on web
- [x] **WEB-04**: Web fallback redirects to retailer on reservation (same as Android flow)

### Localization

- [x] **I18N-01**: App UI supports Romanian and English languages
- [x] **I18N-02**: All UI labels stored in separate resource files (strings.xml for Android, i18n files for web)
- [x] **I18N-03**: Language auto-detected from device/browser locale with manual override

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Occasion Themes (deferred from v1.1)

- **THEME-01**: Four occasion themes (Housewarming, Wedding, Baby, Birthday) ship with handoff-specified values for accent, accentInk, accentSoft, second, secondSoft — v1.1 ships Housewarming only
- **THEME-02**: Per-registry occasion theme applies at runtime on Registry detail, Create registry, and Add item — tokens cascade from `registry.occasion`
- **THEME-03**: Greyscale tokens (paper → line) shift hue subtly per theme per `theme.jsx`

### Additional Merchants

- **MERCH-01**: Affiliate tag support for additional merchants beyond EMAG
- **MERCH-02**: Merchant domain registry is extensible without code changes

### Enhanced Registry

- **EREG-01**: Price range advisory nudge when owner adds only high-priced items
- **EREG-02**: Registry analytics for owners (views, visitor count)

### Platform

- **PLAT-01**: iOS native application
- **PLAT-02**: Browser extension for adding items from desktop

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| EMAG public product catalog API | No confirmed public catalog API exists (Marketplace API is seller-only). Replaced by curated WebView store browser in Phase 7 (STORE-01..04) |
| In-app purchasing / payment processing | Users buy at the retailer; avoids PCI compliance and payment gateway complexity |
| iOS app | Android-first; web fallback covers iOS gift givers for v1 |
| Social features (comments, likes, public feed) | Changes product from utility to social network; moderation overhead |
| Group gifting / crowdfunding | Requires payment processing; contradicts affiliate-only revenue model |
| Real-time price tracking | Requires continuous retailer polling; stale prices create trust issues |
| Multiple persistence layers (SQLite + Firebase) | Firebase offline persistence handles caching; dual persistence adds sync complexity |
| Ads or premium tiers | Affiliate commissions are the sole revenue model; ads damage UX |
| Browser extension | Android-first; adds separate release channel to maintain |
| v1.1: Web fallback visual refresh | Handoff explicitly scopes design to Android owner flow only |
| v1.1: Settings / profile screen redesign | Deferred — not in handoff |
| v1.1: Notifications inbox redesign | Deferred — not in handoff |
| v1.1: Dark mode | Deferred — not in handoff |
| v1.1: Empty states (new user, empty registry, no results) | Deferred — flagged by handoff as out of scope |
| v1.1: Store browser WebView chrome redesign | Deferred — handoff covers only owner flow screens 06-10 |
| v1.1: Email template redesign | Deferred — not in handoff |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 2 | Complete |
| AUTH-02 | Phase 2 | Complete |
| AUTH-03 | Phase 2 | Complete |
| AUTH-04 | Phase 2 | Complete |
| AUTH-05 | Phase 2 | Complete |
| AUTH-06 | Phase 2 | Complete |
| REG-01 | Phase 3 | Complete |
| REG-02 | Phase 3 | Complete |
| REG-03 | Phase 3 | Complete |
| REG-04 | Phase 3 | Complete |
| REG-05 | Phase 3 | Complete |
| REG-06 | Phase 3 | Complete |
| REG-07 | Phase 3 | Complete |
| REG-08 | Phase 3 | Complete |
| REG-09 | Phase 3 | Complete |
| REG-10 | Phase 3 | Complete |
| ITEM-01 | Phase 3 | Complete |
| ITEM-02 | Phase 3 | Complete |
| ITEM-03 | — | Retired (no EMAG API) |
| ITEM-04 | — | Retired (superseded by STORE-03) |
| STORE-01 | Phase 7 | Complete |
| STORE-02 | Phase 7 | Complete |
| STORE-03 | Phase 7 | Complete |
| STORE-04 | Phase 7 | Complete |
| ITEM-05 | Phase 3 | Complete |
| ITEM-06 | Phase 3 | Complete |
| ITEM-07 | Phase 3 | Complete |
| AFF-01 | Phase 3 | Complete |
| AFF-02 | Phase 3 | Complete |
| AFF-03 | Phase 3 | Complete |
| AFF-04 | Phase 3 | Complete |
| RES-01 | Phase 4 | Complete |
| RES-02 | Phase 4 | Complete |
| RES-03 | Phase 4 | Complete |
| RES-04 | Phase 4 | Complete |
| RES-05 | Phase 4 | Complete |
| RES-06 | Phase 4 | Complete |
| RES-07 | Phase 4 | Complete |
| RES-08 | Phase 4 | Complete |
| RES-09 | Phase 4 | Complete |
| NOTF-01 | Phase 6 | Complete |
| NOTF-02 | Phase 6 | Complete |
| NOTF-03 | Phase 6 | Complete |
| WEB-01 | Phase 5 | Complete |
| WEB-02 | Phase 5 | Complete |
| WEB-03 | Phase 5 | Complete |
| WEB-04 | Phase 5 | Complete |
| I18N-01 | Phase 2 | Complete |
| I18N-02 | Phase 1 | Complete |
| I18N-03 | Phase 2 | Complete |
| DES-01 | Phase 8 | Complete |
| DES-02 | Phase 8 | Complete |
| DES-03 | Phase 8 | Complete |
| DES-04 | Phase 8 | Complete |
| DES-05 | Phase 8 | Complete |
| THEME-01 | — | Deferred to v1.2 |
| THEME-02 | — | Deferred to v1.2 |
| THEME-03 | — | Deferred to v1.2 |
| CHROME-01 | Phase 9 | Pending |
| CHROME-02 | Phase 9 | Pending |
| CHROME-03 | Phase 9 | Pending |
| SCR-06 | Phase 10 | Pending |
| SCR-07 | Phase 10 | Pending |
| SCR-08 | Phase 11 | Pending |
| SCR-09 | Phase 11 | Pending |
| SCR-10 | Phase 11 | Pending |
| STAT-01 | Phase 9 | Pending |
| STAT-02 | Phase 9 | Pending |
| STAT-03 | Phase 9 | Pending |
| STAT-04 | Phase 9 | Pending |

**v1.0 Coverage:**
- v1.0 requirements: 45 total (ITEM-03 and ITEM-04 retired — 43 active + 4 STORE-* added = 47 mapped)
- Mapped to phases: 47
- Unmapped: 0 ✓

**v1.1 Coverage:**
- v1.1 requirements: 17 total (DES-01..05, CHROME-01..03, SCR-06..10, STAT-01..04)
- Mapped to phases: 17 (Phase 8: 5 · Phase 9: 7 · Phase 10: 2 · Phase 11: 3)
- Unmapped: 0 ✓
- Deferred to v1.2: THEME-01, THEME-02, THEME-03 (Housewarming-only scope for v1.1)

---
*Requirements defined: 2026-04-04*
*Last updated: 2026-04-20 — v1.1 scope narrowed to Housewarming only; THEME-01/02/03 deferred to v1.2*
