# Gift Registry

## What This Is

A general-purpose gift registry Android application with a web fallback for gift givers. Registry owners create and manage wishlists for any occasion (birthdays, weddings, baby showers, etc.), adding items via URL import or browsing an EMAG-backed product catalog. Gift givers access registries through shareable links, reserve items with a 30-minute purchase window, and click through to buy at the retailer. Monetized through affiliate link commissions.

## Core Value

Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.

## Requirements

### Validated

- ✓ Multilingual support — all UI labels in separate resource files (strings.xml, i18n JSON) — Phase 1
- ✓ Gift givers can log in, create an account, or continue as guest — Phase 2
- ✓ Guest-to-account conversion offered at reservation time (UI ready, trigger in Phase 4) — Phase 2
- ✓ Multilingual support (Romanian and English) with manual override and persistence — Phase 2
- ✓ Web fallback for gift givers — view registry, log in / sign up / guest, reserve, retailer redirect — Phase 5 (automated; 7 items pending real-browser UAT)
- ✓ Owners can opt in/out of purchase notifications — Phase 6 (automated; 5 items pending real-device / real-SMTP UAT)
- ✓ Owners can invite specific users to private registries (email + in-app notification for existing users, email-only for non-users) — Phase 6
- ✓ Expiration email sent to giver when reservation lapses, with option to re-reserve — Phase 6
- ✓ Registry owners can create registries for any occasion — Phase 3
- ✓ Owners can add items via any URL with automatic affiliate tag injection — Phase 3
- ✓ Owners can set registry visibility (public link or private invite-only) — Phase 3
- ✓ Gift givers can access registries via shareable links — Phase 3
- ✓ Guests provide first name, last name, and email to reserve items — Phase 4
- ✓ 30-minute reservation timer with auto-release on expiry — Phase 4
- ✓ Givers are redirected to retailer immediately upon reservation — Phase 4
- ✓ URL transformer identifies merchant and appends correct affiliate tag — Phase 3
- ✓ Owners can browse popular Romanian retailers in an in-app WebView and add products via persistent "Add to list" CTA — Phase 7 (automated; 8 items pending real-device UAT). Replaces the retired EMAG catalog API scope (no public catalog API available)

### Active

_(All v1 active requirements complete — milestone v1.0 ready for audit)_

### Out of Scope

- EMAG public product catalog API — no confirmed public API; replaced by WebView store browser in Phase 7
- In-app purchasing — users buy at the retailer, not in the app
- iOS app — Android only for v1
- Multiple persistence layers — Firebase only, no SQLite
- Ads or premium tiers — affiliate commissions are the sole revenue model
- Social features (comments, likes on registries) — not part of core value

## Context

- **Platform:** Android (Java/Kotlin) with web fallback for gift givers
- **Backend:** Firebase (authentication, database, cloud functions)
- **Product catalog:** EMAG public API for product recommendations and browsing
- **Monetization:** Affiliate link commissions via URL transformer that identifies merchants and injects affiliate tags
- **Target market:** Romanian market (EMAG is a Romanian e-commerce platform)
- **Localization:** Multilingual support for Romanian and English; all UI element labels stored in separate resource files for translation
- **Key technical challenge:** Real-time reservation system with 30-minute expiry across concurrent users

## Constraints

- **Tech stack**: Java/Kotlin for Android, Firebase for backend — no other persistence layer
- **Retailer integration**: EMAG API for catalog; other retailers supported via URL import only
- **Reservation model**: 30-minute hard timer, no extensions — keeps inventory state simple and predictable
- **Guest access**: Must work without account creation to reduce friction for gift givers
- **Localization**: All UI labels externalized in resource files (strings.xml for Android, i18n files for web) — no hardcoded strings

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Firebase over SQLite | Registries must be shared between users; Firebase provides real-time sync, auth, and cloud functions out of the box | — Pending |
| Affiliate-only monetization | Clean user experience, no ads or paywalls; revenue scales with gift purchases | — Pending |
| 30-minute reservation window | Long enough to complete a purchase, short enough to prevent indefinite holds | — Pending |
| Web fallback for givers only | Reduces scope while ensuring givers don't need to install an app to participate | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd:transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd:complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-20 after Phase 7 completion — milestone v1.0 all phases done*
