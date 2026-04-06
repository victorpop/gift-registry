# Requirements: Gift Registry

**Defined:** 2026-04-04
**Core Value:** Gift givers can reliably reserve and purchase gifts without duplicates — the reservation-to-purchase flow must be seamless and trustworthy.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

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
- [ ] **REG-08**: Invited non-users see login/signup/guest options upon accessing the link
- [x] **REG-09**: Owner can opt in or out of purchase notifications

### Item Management

- [x] **ITEM-01**: Owner can add an item by pasting any URL
- [x] **ITEM-02**: URL import auto-fills item title, image, and price via Open Graph metadata
- [ ] **ITEM-03**: Owner can browse and search EMAG product catalog via API
- [ ] **ITEM-04**: Owner can add an item from EMAG catalog search results
- [x] **ITEM-05**: Owner can manually edit item details (title, image, price, notes)
- [x] **ITEM-06**: Owner can remove an item from a registry
- [x] **ITEM-07**: Items display real-time status (available, reserved, purchased)

### Affiliate System

- [x] **AFF-01**: URL transformer identifies merchant domain and appends correct affiliate tag
- [x] **AFF-02**: EMAG items receive affiliate tags automatically on add
- [x] **AFF-03**: Affiliate tag injection is invisible to users
- [x] **AFF-04**: Unknown merchant URLs pass through without breaking (no affiliate tag, logged for review)

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

- [x] **I18N-01**: App UI supports Romanian and English languages
- [x] **I18N-02**: All UI labels stored in separate resource files (strings.xml for Android, i18n files for web)
- [x] **I18N-03**: Language auto-detected from device/browser locale with manual override

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
| REG-08 | Phase 3 | Pending |
| REG-09 | Phase 3 | Complete |
| REG-10 | Phase 3 | Complete |
| ITEM-01 | Phase 3 | Complete |
| ITEM-02 | Phase 3 | Complete |
| ITEM-03 | Phase 7 | Pending |
| ITEM-04 | Phase 7 | Pending |
| ITEM-05 | Phase 3 | Complete |
| ITEM-06 | Phase 3 | Complete |
| ITEM-07 | Phase 3 | Complete |
| AFF-01 | Phase 3 | Complete |
| AFF-02 | Phase 3 | Complete |
| AFF-03 | Phase 3 | Complete |
| AFF-04 | Phase 3 | Complete |
| RES-01 | Phase 4 | Pending |
| RES-02 | Phase 4 | Pending |
| RES-03 | Phase 4 | Pending |
| RES-04 | Phase 4 | Pending |
| RES-05 | Phase 4 | Pending |
| RES-06 | Phase 4 | Pending |
| RES-07 | Phase 4 | Pending |
| RES-08 | Phase 4 | Pending |
| RES-09 | Phase 4 | Pending |
| NOTF-01 | Phase 6 | Pending |
| NOTF-02 | Phase 6 | Pending |
| NOTF-03 | Phase 6 | Pending |
| WEB-01 | Phase 5 | Pending |
| WEB-02 | Phase 5 | Pending |
| WEB-03 | Phase 5 | Pending |
| WEB-04 | Phase 5 | Pending |
| I18N-01 | Phase 2 | Complete |
| I18N-02 | Phase 1 | Complete |
| I18N-03 | Phase 2 | Complete |

**Coverage:**
- v1 requirements: 45 total
- Mapped to phases: 45
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-04*
*Last updated: 2026-04-04 after roadmap creation*
