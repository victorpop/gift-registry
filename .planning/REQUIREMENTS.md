# Requirements: Gift Registry

**Defined:** 2026-04-04
**Core Value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [ ] **AUTH-01**: User can sign up with email and password
- [ ] **AUTH-02**: User can log in with email and password
- [ ] **AUTH-03**: User can log in with Google OAuth
- [ ] **AUTH-04**: User session persists across app restarts
- [ ] **AUTH-05**: Guest can access registry and reserve items by providing first name, last name, and email
- [ ] **AUTH-06**: Guest is offered account creation after completing a reservation

### Registry Management

- [ ] **REG-01**: Owner can create a registry with a name, occasion type (Wedding, Baby shower, Anniversary, Christmas, custom), event date and time, event location, and a description
- [ ] **REG-02**: Owner can edit all registry details (name, occasion type, date/time, location, description, visibility)
- [ ] **REG-03**: Owner can delete a registry
- [ ] **REG-10**: Owner can have multiple active registries simultaneously
- [ ] **REG-04**: Owner can set registry visibility to public (shareable link) or private (invite-only)
- [ ] **REG-05**: Owner can invite specific users to a private registry via email
- [ ] **REG-06**: Invited users with accounts receive in-app notification and email
- [ ] **REG-07**: Invited users without accounts receive email only with link to registry
- [ ] **REG-08**: Invited non-users see login/signup/guest options upon accessing the link
- [ ] **REG-09**: Owner can opt in or out of purchase notifications

### Item Management

- [ ] **ITEM-01**: Owner can add an item by pasting any URL
- [ ] **ITEM-02**: URL import auto-fills item title, image, and price via Open Graph metadata
- [ ] **ITEM-03**: Owner can browse and search EMAG product catalog via API
- [ ] **ITEM-04**: Owner can add an item from EMAG catalog search results
- [ ] **ITEM-05**: Owner can manually edit item details (title, image, price, notes)
- [ ] **ITEM-06**: Owner can remove an item from a registry
- [ ] **ITEM-07**: Items display real-time status (available, reserved, purchased)

### Affiliate System

- [ ] **AFF-01**: URL transformer identifies merchant domain and appends correct affiliate tag
- [ ] **AFF-02**: EMAG items receive affiliate tags automatically on add
- [ ] **AFF-03**: Affiliate tag injection is invisible to users
- [ ] **AFF-04**: Unknown merchant URLs pass through without breaking (no affiliate tag, logged for review)

### Reservation System

- [ ] **RES-01**: Gift giver can reserve an available item
- [ ] **RES-02**: Reserved items show as unavailable to other givers in real time
- [ ] **RES-03**: 30-minute reservation timer starts on reserve action
- [ ] **RES-04**: Giver is redirected to retailer immediately upon reservation
- [ ] **RES-05**: Reservation auto-releases after 30 minutes if not confirmed as purchased
- [ ] **RES-06**: Auto-released items return to available status in real time
- [ ] **RES-07**: Giver receives expiration email when reservation lapses
- [ ] **RES-08**: Expiration email includes option to re-reserve immediately
- [ ] **RES-09**: Two givers cannot simultaneously reserve the same item (race condition prevention)

### Notifications

- [ ] **NOTF-01**: Owner receives push notification when a gift is purchased (if opted in)
- [ ] **NOTF-02**: Owner receives email when a gift is purchased (if opted in)
- [ ] **NOTF-03**: Giver receives expiration email when reservation timer expires

### Web Fallback

- [ ] **WEB-01**: Gift givers can view a registry via web browser without installing the app
- [ ] **WEB-02**: Gift givers can reserve items from the web fallback
- [ ] **WEB-03**: Gift givers can log in, create an account, or continue as guest on web
- [ ] **WEB-04**: Web fallback redirects to retailer on reservation (same as Android flow)

### Localization

- [ ] **I18N-01**: App UI supports Romanian and English languages
- [ ] **I18N-02**: All UI labels stored in separate resource files (strings.xml for Android, i18n files for web)
- [ ] **I18N-03**: Language auto-detected from device/browser locale with manual override

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

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
| In-app purchasing / payment processing | Users buy at the retailer; avoids PCI compliance and payment gateway complexity |
| iOS app | Android-first; web fallback covers iOS gift givers for v1 |
| Social features (comments, likes, public feed) | Changes product from utility to social network; moderation overhead |
| Group gifting / crowdfunding | Requires payment processing; contradicts affiliate-only revenue model |
| Real-time price tracking | Requires continuous retailer polling; stale prices create trust issues |
| Multiple persistence layers (SQLite + Firebase) | Firebase offline persistence handles caching; dual persistence adds sync complexity |
| Ads or premium tiers | Affiliate commissions are the sole revenue model; ads damage UX |
| Browser extension | Android-first; adds separate release channel to maintain |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Pending | Pending |
| AUTH-02 | Pending | Pending |
| AUTH-03 | Pending | Pending |
| AUTH-04 | Pending | Pending |
| AUTH-05 | Pending | Pending |
| AUTH-06 | Pending | Pending |
| REG-01 | Pending | Pending |
| REG-02 | Pending | Pending |
| REG-03 | Pending | Pending |
| REG-04 | Pending | Pending |
| REG-05 | Pending | Pending |
| REG-06 | Pending | Pending |
| REG-07 | Pending | Pending |
| REG-08 | Pending | Pending |
| REG-09 | Pending | Pending |
| ITEM-01 | Pending | Pending |
| ITEM-02 | Pending | Pending |
| ITEM-03 | Pending | Pending |
| ITEM-04 | Pending | Pending |
| ITEM-05 | Pending | Pending |
| ITEM-06 | Pending | Pending |
| ITEM-07 | Pending | Pending |
| AFF-01 | Pending | Pending |
| AFF-02 | Pending | Pending |
| AFF-03 | Pending | Pending |
| AFF-04 | Pending | Pending |
| RES-01 | Pending | Pending |
| RES-02 | Pending | Pending |
| RES-03 | Pending | Pending |
| RES-04 | Pending | Pending |
| RES-05 | Pending | Pending |
| RES-06 | Pending | Pending |
| RES-07 | Pending | Pending |
| RES-08 | Pending | Pending |
| RES-09 | Pending | Pending |
| REG-10 | Pending | Pending |
| NOTF-01 | Pending | Pending |
| NOTF-02 | Pending | Pending |
| NOTF-03 | Pending | Pending |
| WEB-01 | Pending | Pending |
| WEB-02 | Pending | Pending |
| WEB-03 | Pending | Pending |
| WEB-04 | Pending | Pending |
| I18N-01 | Pending | Pending |
| I18N-02 | Pending | Pending |
| I18N-03 | Pending | Pending |

**Coverage:**
- v1 requirements: 45 total
- Mapped to phases: 0
- Unmapped: 45 ⚠️

---
*Requirements defined: 2026-04-04*
*Last updated: 2026-04-04 after initial definition*
