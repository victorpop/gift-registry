# Feature Research

**Domain:** Gift Registry (Android app, Firebase backend, EMAG catalog integration)
**Researched:** 2026-04-04
**Confidence:** MEDIUM — Global gift registry market is well-documented; Romanian market specifics are inferred from EMAG's positioning as a major e-commerce platform with no local gift registry competitor found.

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Registry creation for any occasion | Users create registries for birthdays, weddings, baby showers — single-occasion apps feel limiting | LOW | Support occasion type labels (wedding, birthday, baby shower, etc.) stored in string resources for i18n |
| Add items from any URL | Every major registry (Giftster, GiftList, Moonsift) supports universal import; users expect this | MEDIUM | Open Graph / og:title / og:image parsing; fallback to Twitter card meta; headless rendering for JS-heavy sites is HIGH complexity — skip for v1, accept failures gracefully |
| Item detail auto-fill (title, image, price) | Users are frustrated by manual entry — all leading apps auto-fill from URLs | MEDIUM | EMAG items auto-fill via API; other URLs use metadata scraping; price staleness is acceptable |
| Shareable registry link | Gift givers must be able to access a registry without installing the app | LOW | Deep link + web fallback; visibility toggles (public vs. private invite-only) |
| Reservation / "I'll buy this" marking | Core duplicate-prevention mechanic — without it the app has no differentiated value over a plain wishlist | HIGH | 30-minute timer, Firebase real-time updates, concurrent access handling |
| Item marked as reserved/purchased | Gift givers must see item status to avoid duplicate purchases | LOW | Status enum: available, reserved, purchased; displayed on registry view |
| Guest access (no account required) | 24–26% of users abandon when forced to create accounts before completing an action | MEDIUM | Collect first name, last name, email for guest reservations; no password required |
| Visibility controls (public / private) | Users want control over who can see their registry | LOW | Two modes: public shareable link, private invite-only |
| Registry owner purchase notifications | Owners want to know when someone has reserved or purchased an item | LOW | Opt-in; Firebase Cloud Messaging; respect notification preferences |
| Multilingual UI (Romanian / English) | Target market is Romanian; Romanian-language UI is required to feel local and trustworthy | LOW | All strings in strings.xml (Android) and i18n files (web); language auto-detected from device locale |
| Web fallback for gift givers | Gift givers should not need to install the Android app to participate | MEDIUM | Read-only registry view + reservation + purchase redirect; hosted as a Firebase Hosted page or Cloud Function |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valued.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| EMAG product catalog browse | No other Romanian-focused registry integrates EMAG; gives users a curated, trusted product source with real prices and images | MEDIUM | EMAG API; browsing and search; items added with EMAG affiliate tag auto-applied |
| Affiliate tag auto-injection | Invisible to users; revenue model that requires no paywalls or ads — cleaner UX than ad-supported competitors | MEDIUM | URL transformer: detect merchant domain → inject correct affiliate parameter; EMAG and any other supported merchants |
| 30-minute reservation timer with expiry email | Most registries mark items "purchased" permanently with no recourse if the buyer abandons — timed reservations auto-release, keeping the registry accurate | HIGH | Firebase scheduled function; expiry email with re-reserve CTA; requires real-time Firestore listeners on all active clients |
| Guest-to-account conversion at reservation | Reduces friction at the critical moment (reservation) while capturing user relationship post-action — the recommended approach per Baymard/Shopify research | LOW | Offer account creation after reservation completes, not before; pre-fill email from guest session |
| Expiry notification to gift giver | Giver is reminded when their reservation lapsed and invited to re-reserve — reduces abandoned reservations that stay locked | LOW | Email via Firebase + trigger on timer expiry |
| Price range diversity enforcement (guidance) | Registry owners adding only expensive items lose budget-conscious givers — guidance encourages a spread | LOW | UI advisory: "Add items in different price ranges" — not a hard rule, just UX nudge |
| Invite-only private registries with in-app notification | Secure registries for close groups; invited users who already have accounts get in-app notifications, not just email | MEDIUM | Firebase dynamic links for invite; push notification to existing users; email-only for non-users |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| In-app purchasing / payment processing | "One-stop shop" feel; users don't want to leave the app | Requires PCI compliance, payment gateway integration, merchant relationships, return/refund handling — far outside project scope and affiliate model | Redirect to retailer immediately on reservation; this is the intended flow and keeps scope minimal |
| iOS app | Broader reach | Doubles the maintenance surface for v1; Android captures the market first; web fallback serves iOS gift givers | Web fallback covers iOS gift givers; iOS app is a v2 consideration |
| Social features (comments, likes, public feed) | Adds engagement and viral loops | Fundamentally changes the product from a utility into a social network; moderation overhead; out-of-scope per PROJECT.md | Direct sharing via link is sufficient social primitive |
| Quantity-based "group gifting" (crowdfunding toward an item) | Users want to contribute partial amounts toward expensive items | Requires payment processing, fund aggregation, disbursement logic — high complexity; contradicts affiliate-only revenue model | Items can be marked as "group gift" with a note; coordination happens outside the app |
| Real-time price tracking / price drop alerts | Useful for wishlists in general | Requires continuous polling of retailer pages or paid price API; scraped prices go stale and create trust issues when displayed as current | Show price at time of add with a "price may vary" disclaimer; link goes to current retailer page |
| Browser extension for item adding | Power-user convenience | Android-first product; browser extensions are a desktop/iOS web concern; adds a separate release channel to maintain | Share-to-app intent on Android; direct URL paste in the app |
| Multiple persistence layers (SQLite + Firebase) | Offline capability | Increases sync complexity dramatically; Firebase's offline persistence handles local caching adequately | Use Firebase's built-in offline support (Firestore disk persistence) |
| Ads or premium tiers | Alternative revenue diversification | Ads damage UX and affiliate credibility; premium tiers require paywall UX, feature gating logic, subscription management | Affiliate commissions are the sole model — keep it clean |

## Feature Dependencies

```
[Registry Creation]
    └──requires──> [User Authentication (owner account)]

[Item Add via URL]
    └──requires──> [URL Transformer / Affiliate Tag Injector]
                       └──requires──> [Merchant Domain Registry]

[Item Add via EMAG Catalog]
    └──requires──> [EMAG API Integration]
    └──requires──> [URL Transformer / Affiliate Tag Injector]

[Reservation System]
    └──requires──> [Registry Item Listing]
    └──requires──> [Guest Access (name + email)]
    └──requires──> [30-Minute Timer (Firebase scheduled function or client-side countdown)]
                       └──requires──> [Expiry Email Notification]
                       └──requires──> [Real-Time Status Sync (Firestore listeners)]

[Web Fallback]
    └──requires──> [Registry Item Listing]
    └──requires──> [Reservation System]
    └──requires──> [Guest Access]

[Private Registry / Invite]
    └──requires──> [Registry Creation]
    └──requires──> [User Authentication (invitee)]

[Guest-to-Account Conversion]
    └──enhances──> [Guest Access]

[Purchase Notification to Owner]
    └──enhances──> [Reservation System]

[Expiry Email to Giver]
    └──enhances──> [Reservation System → 30-Minute Timer]

[EMAG Catalog Browse]
    ──enhances──> [Item Add via URL] (alternative path, not a dependency)

[Multilingual Support]
    ──enhances──> [All UI features] (cross-cutting; must be in place before any UI ships)
```

### Dependency Notes

- **Reservation System requires Real-Time Status Sync:** Multiple gift givers viewing the same registry concurrently must see live reservation state to avoid race conditions where two people try to reserve the same item simultaneously. Firestore's optimistic locking or a transaction-based approach is required.
- **Multilingual Support enhances all UI:** Strings must be externalized from day one. Retrofitting i18n into hardcoded UI strings is painful and error-prone. This is a cross-cutting concern, not a standalone feature.
- **URL Transformer requires Merchant Domain Registry:** The affiliate tag to inject differs by merchant. A mapping of `domain → affiliate_param → affiliate_id` must exist before any URL import or EMAG item add can generate revenue.
- **Web Fallback requires Reservation System:** The web fallback is not a read-only view — gift givers must be able to reserve items from it. This means the full reservation flow (including the 30-minute timer) must be implemented before the web fallback is complete.
- **Guest-to-Account Conversion conflicts with Guest-First philosophy:** Offering account creation must happen after reservation completes, not before. Prompting before the action increases abandonment by 24–26%.

## MVP Definition

### Launch With (v1)

Minimum viable product — validates the core reservation-to-purchase flow.

- [ ] User authentication (registry owner account creation and login) — gateway to all owner actions
- [ ] Registry creation with occasion type and visibility setting — core owner action
- [ ] Item add via URL paste with auto-fill (title, image, price from Open Graph) — primary item-add path
- [ ] Item add via EMAG product catalog browse/search — differentiator; key for Romanian market
- [ ] Affiliate tag injection for EMAG URLs (and any pre-mapped merchants) — revenue enabler; must ship with item add
- [ ] Item listing with availability status (available / reserved / purchased) — gift giver's primary view
- [ ] Shareable registry link (deep link + web fallback URL) — required to invite gift givers
- [ ] Web fallback: view registry + reserve + redirect to retailer — gift givers on any device
- [ ] Guest access with name/email collection — reduces friction for gift givers
- [ ] 30-minute reservation timer with auto-release on expiry — core duplicate-prevention mechanic
- [ ] Redirect to retailer immediately on reservation — purchase path
- [ ] Expiry email to gift giver with re-reserve CTA — closes the loop on abandoned reservations
- [ ] Multilingual UI (Romanian / English) — required for target market from day one
- [ ] Owner opt-in purchase/reservation notifications — basic owner engagement

### Add After Validation (v1.x)

Features to add once core flow is working and real users are using it.

- [ ] Guest-to-account conversion prompt after reservation — add once guest flow is validated; captures repeat users
- [ ] Invite-only registry with email invites + in-app notifications for existing users — add when private registry use cases are confirmed
- [ ] Affiliate tag support for additional merchants beyond EMAG — add as revenue data shows demand
- [ ] Price range advisory nudge in registry editor — low-cost UX improvement; add when registry editing UX is polished

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] iOS app — only if Android proves the model; web fallback covers iOS gift givers for now
- [ ] Browser extension or share-to-app intent optimization — add when user acquisition data shows desktop usage patterns
- [ ] Enhanced item metadata (multi-image, variants, size/color selection) — add when catalog richness becomes a retention driver
- [ ] Registry analytics for owners (who viewed, when) — add when owners ask for it; privacy considerations apply

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| 30-minute reservation with real-time sync | HIGH | HIGH | P1 |
| Registry creation + shareable link | HIGH | LOW | P1 |
| Item add via URL (Open Graph auto-fill) | HIGH | MEDIUM | P1 |
| EMAG product catalog browse | HIGH | MEDIUM | P1 |
| Affiliate tag injection | HIGH (revenue) | MEDIUM | P1 |
| Web fallback for gift givers | HIGH | MEDIUM | P1 |
| Guest access (no account for givers) | HIGH | MEDIUM | P1 |
| Multilingual (RO/EN) | HIGH | LOW | P1 |
| Expiry email to gift giver | MEDIUM | LOW | P1 |
| Owner purchase notifications | MEDIUM | LOW | P1 |
| Private registry + invite system | MEDIUM | MEDIUM | P2 |
| Guest-to-account conversion | MEDIUM | LOW | P2 |
| Additional merchant affiliate support | MEDIUM (revenue) | LOW | P2 |
| Price range advisory | LOW | LOW | P3 |
| Registry analytics for owners | LOW | MEDIUM | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Zola / The Knot | MyRegistry / Giftster | GiftList | Our Approach |
|---------|-----------------|----------------------|----------|--------------|
| Universal URL import | Yes | Yes | Yes (100+ stores) | Yes — any URL via Open Graph scraping |
| Catalog browse | Curated partners only | No | 100+ stores | EMAG catalog only (Romanian market focus) |
| Reservation / duplicate prevention | Purchase tracking (no timer) | Purchase marking (no timer) | Purchase tracking | 30-minute timed reservation with auto-release |
| Guest checkout | Yes | Yes | Yes | Yes — name + email only |
| Affiliate monetization | No (retailer-direct) | No | Yes (disclosed) | Yes — invisible to users, injected at URL transform |
| Web fallback | Full website | Full website | Full website | Lightweight web fallback (view + reserve + redirect) |
| Mobile app | Yes (iOS + Android) | Yes (iOS + Android) | Yes (iOS + Android) | Android only; iOS via web fallback |
| Multilingual / local market | English only | English only | English only | Romanian + English (local market advantage) |
| Expiry / timer on reservation | No — permanent marking | No — permanent marking | No | Yes — 30-minute auto-release; unique in this category |
| Social features | Yes (Zola: wedding website) | No | No | Explicitly out of scope |

## Sources

- [MyRegistry.com Feature Comparisons (2026)](https://guides.myregistry.com/gift-list/best-gift-list-apps-2026-the-ultimate-comparison-guide/)
- [Zola vs MyRegistry Honest Comparison (2026)](https://guides.myregistry.com/gift-list/zola-vs-myregistry-com-the-honest-2026-comparison/)
- [Registry Valet Gift Registry Software Features](https://www.registryvalet.com/gift-registry-software-features/)
- [Nielsen Norman Group — Wishlists, Gift Certificates and Gift Giving in E-Commerce](https://www.nngroup.com/articles/wishlists-gift-certificates/)
- [Baymard Institute — Gifting UX Best Practices](https://baymard.com/blog/gifting-flow)
- [Guest Checkout vs Account Creation: Data and Conversion Impact](https://ecomhint.com/blog/guest-checkout-vs-account-creation)
- [Shopify — Guest Checkout: Simplify Purchases and Boost Sales (2025)](https://www.shopify.com/enterprise/blog/guest-checkout)
- [GiftList — Best Wishlist, Gift Registry App (Google Play)](https://mygiftlistapp.com/)
- [Giftster — Android Wish List App (2026)](https://www.giftster.com/android/)
- [Best Gift Registries 2025: Complete Comparison](https://www.moonsift.com/guides/best-gift-registry)
- [URL Metadata API for auto-fill (DEV Community)](https://dev.to/fistonuser/i-built-a-url-metadata-api-after-wasting-days-on-manual-scraping-3m81)

---
*Feature research for: Gift Registry (Android, Firebase, EMAG, Romanian market)*
*Researched: 2026-04-04*
