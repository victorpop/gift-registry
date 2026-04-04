# Project Research Summary

**Project:** Gift Registry Android App
**Domain:** Android gift registry with Firebase backend, EMAG product catalog integration, affiliate monetization, Romanian market
**Researched:** 2026-04-04
**Confidence:** HIGH (core Android/Firebase stack and architecture patterns) | MEDIUM (EMAG integration, feature market analysis)

## Executive Summary

This is a Romanian-market gift registry Android app built on Kotlin/Jetpack Compose with a Firebase-only backend. The product's core value proposition — timed, auto-releasing reservations that prevent duplicate gifts — is a technically differentiated feature that does not exist in any major competitor (Zola, MyRegistry, Giftster). The affiliate monetization model (EMAG + 2Performant network) is the revenue mechanism. The product serves two distinct user types: registry owners (Android app users) and gift givers (Android app or web fallback). Getting these two flows right with no duplicate gifts is the entire value of the product.

The recommended approach is a clean-architecture Android app (Kotlin, Jetpack Compose, Navigation3, Hilt) backed by Firebase (Firestore, Auth, Cloud Functions 2nd gen, Cloud Tasks, Hosting). The reservation system must be server-authoritative from day one: Firestore transactions for atomic reservation writes, Cloud Tasks for per-reservation expiry triggers at the 30-minute mark. The web fallback is intentionally thin — plain HTML/JS served via Firebase Hosting — and must support the full reservation flow, not just read access. Multilingual support (Romanian/English) is a cross-cutting requirement that must be in place from the first UI screen.

The primary risks are: (1) the reservation race condition — two givers simultaneously reserving the same item — which requires a Firestore transaction and is unrecoverable without one; (2) the EMAG integration uncertainty — EMAG has no public product catalog API, so browsing must be via affiliate-program deep links or a strategy that needs a dedicated spike before implementation; and (3) client-side timer expiry being mistaken for sufficient — it is not; server-side Cloud Tasks are mandatory. Build the data model and security rules before any UI to avoid expensive rework.

## Key Findings

### Recommended Stack

The Android app is Kotlin 2.3.x with Jetpack Compose BOM 2026.03.00, AGP 9.x, Hilt 2.51.x for DI, Navigation3 1.0.1 (the new stable Compose-first library that replaces Nav2), and Coil 3.4.0 for image loading. Firebase BoM 34.11.0 is the backend SDK, which consolidates Kotlin APIs into the main modules — the old KTX sub-modules (`firebase-auth-ktx`, etc.) were removed in BoM v34.0.0 and must not be used. Cloud Functions run on Node.js 22 (LTS) with TypeScript. The web fallback is React 19.x with the Firebase JS SDK 11.x, hosted on Firebase Hosting.

The single most consequential stack decision is using Cloud Tasks (not Cloud Scheduler cron) for reservation expiry. Cloud Tasks fires once per reservation at the exact 30-minute mark; cron requires scanning all reservations on an interval and introduces lag that leaves items zombie-reserved. All affiliate URL transformation logic must live in Cloud Functions, never in the Android APK, so merchant rules can be updated without a Play Store release.

**Core technologies:**
- Kotlin 2.3.x + Jetpack Compose BOM 2026.03.00: Primary language and UI framework — Google's current standard, Compose-only Navigation3 requires it
- Firebase BoM 34.11.0 (Firestore, Auth, FCM, App Check): Backend — real-time listeners, atomic transactions, no self-hosted infrastructure
- Cloud Functions 2nd gen (Node.js 22, TypeScript): Server-side logic — reservation expiry, affiliate URL injection, email notifications
- Cloud Tasks: Per-reservation expiry scheduling — more precise and efficient than cron, required for correctness
- Navigation3 1.0.1: Compose-first navigation (stable Nov 2025) — new projects should not use Nav2
- Hilt 2.51.x: Dependency injection — Google-recommended, official ViewModel and Navigation integrations
- React 19.x + Firebase JS SDK 11.x: Web fallback — shares the same Firebase backend, no separate backend required
- Retrofit 3.0.0 + Kotlin Serialization: EMAG API calls from Android client — Kotlin-first, no reflection overhead

### Expected Features

The core table-stakes features are well-established by the market. The 30-minute auto-releasing reservation timer is the only genuine differentiator — no competitor implements it. EMAG catalog integration is a secondary differentiator targeting the Romanian market.

**Must have (table stakes):**
- Registry creation (multiple occasion types) — users expect this from any registry product
- Item add via URL with Open Graph auto-fill — all competitors support this; manual-only entry is a deal-breaker
- Shareable registry link with deep link + web fallback — gift givers must not be required to install the app
- 30-minute reservation with auto-release and real-time status sync — the core differentiated mechanic
- Guest access (name + email only, no account required) — 24-26% of users abandon at forced account creation
- Item status display (available / reserved / purchased) — givers must see current state to avoid duplicates
- Expiry email to giver with re-reserve CTA — closes the loop on abandoned reservations
- Multilingual UI (Romanian / English) — required from day one; retrofitting i18n is expensive
- Owner push notifications on reservation/purchase — basic engagement for registry owners
- Web fallback with full reservation flow — not read-only; givers on iOS or desktop must be able to reserve

**Should have (competitive):**
- EMAG product catalog browse — no other Romanian-focused registry integrates EMAG; key differentiator
- Affiliate tag auto-injection (EMAG via 2Performant network) — invisible to users; the sole revenue model
- Guest-to-account conversion prompt after reservation — captures repeat users without blocking the action
- Private registry with email invite + in-app notification for existing users — for close-group occasions

**Defer (v2+):**
- iOS app — web fallback covers iOS gift givers; Android proves the model first
- Additional merchant affiliate support — add when revenue data shows demand
- Registry analytics for owners — add when owners request it; privacy considerations apply
- Browser extension / share-to-app optimization — add when desktop usage patterns are confirmed

### Architecture Approach

The system is a three-tier architecture: Android client (Kotlin/Compose, clean architecture with domain/data/presentation layers), Firebase backend (Firestore as the primary datastore, Cloud Functions for server-side logic, Cloud Tasks for scheduled expiry, Firebase Hosting for the web fallback), and external services (EMAG API called directly from the Android client, email delivery via Firebase Trigger Email Extension or SendGrid). Firebase Dynamic Links is deprecated (August 2025) and must not be used — Android App Links (HTTPS + `assetlinks.json`) with Firebase Hosting serving the web fallback at the same URL is the correct pattern.

**Major components:**
1. Android Presentation (Compose + Navigation3) — screen UI, user input, navigation state; no Firebase imports
2. ViewModel + StateFlow — UI state management; calls use cases, never Firestore directly
3. Repository layer (domain interfaces, data implementations) — Firebase-specific code isolated here; testable in isolation
4. Firestore — primary datastore; real-time listeners drive reservation state across all connected clients
5. Cloud Functions (createReservation, releaseReservation, transformAffiliateUrl, sendInviteEmail, sendExpiryEmail) — all server-side logic
6. Cloud Tasks — per-reservation expiry trigger; enqueued by `createReservation`, cancelled by purchase confirmation
7. Firebase Hosting — web fallback (thin HTML/JS + Firebase JS SDK); serves `assetlinks.json` for Android App Links
8. AffiliateUrlTransformer (Cloud Function utility) — merchant detection + tag injection; change hotspot; never in APK
9. EMAG API integration (data/remote/emag/) — isolated in Android data layer; called from Android client, not Cloud Functions

**Firestore data model highlights:**
- `registries/{id}/items/{id}` — items as subcollection; status enum on each item document
- `reservations/{id}` — top-level collection (not under items) so Cloud Functions can query expiry without knowing registry/item context; stores `cloudTaskName` for cancellation
- Reservations written only by Cloud Functions; clients never write reservation documents directly (prevents `expiresAt` manipulation)

### Critical Pitfalls

1. **Reservation race condition (two givers, same item)** — use a Firestore `runTransaction` that reads `status`, aborts if not `available`, and writes `reserved` atomically. Never read-then-write as two separate calls. This is the foundational correctness guarantee; get it wrong and the core value of the product fails.

2. **Client-side timer expiry** — the Android countdown is UI-only. The server-side Cloud Task is the authority. If a Cloud Function for expiry is not implemented, items stay zombie-reserved indefinitely after app kill. This is unrecoverable without a backend deployment and a one-time cleanup migration.

3. **EMAG API assumed to be stable and public** — EMAG has no public product catalog API. The seller/merchant API is not appropriate for consumer browsing. The integration strategy (affiliate deep links via 2Performant, or URL paste as primary path) needs a dedicated research spike before the EMAG catalog feature is built. Build the EMAG catalog feature so it degrades gracefully — manual URL add must always work independently.

4. **Private registry access enforced client-side** — Firestore security rules are the only authoritative access control. Any visibility check in JavaScript or the Android app that is not backed by a security rule is bypassable. Write and test security rules before building client-side visibility logic.

5. **Affiliate URL logic in the APK** — if merchant affiliate rules are hardcoded in Android, every merchant update requires a Play Store release cycle. All transformation logic must live in Cloud Functions with the Android client invoking the function, not performing the transformation itself.

6. **Guest identity stored in ViewModel only** — process death loses the guest session; the giver cannot see their active reservation on return and tries to re-reserve. Persist guest identity (name + email) in `SharedPreferences` on first collection; query active reservations by email on app resume.

## Implications for Roadmap

Based on the dependency graph in FEATURES.md and the build order from ARCHITECTURE.md, the following phase structure is recommended. The critical insight is that Firestore schema and security rules must precede all other work, and the reservation system (the most complex component) must be built as a single coherent unit — not split across phases.

### Phase 1: Firebase Foundation + Data Model
**Rationale:** Everything else is blocked on this. Security rules written first prevent rework when client features are built on top. The schema must be stable before any repository or ViewModel code exists.
**Delivers:** Firebase project configured (Auth, Firestore, Functions, Hosting, App Check, Emulator Suite); Firestore collections defined with final schema; security rules written and tested in the Rules Simulator for public/private/guest/owner access patterns; `assetlinks.json` placeholder for Android App Links.
**Addresses:** Registry creation (data model), reservation system (data model), private registry (security rules)
**Avoids:** Private registry web fallback bypass (Pitfall 8), `expiresAt` manipulation (Security Mistakes), reworking schema after feature code exists

### Phase 2: Android Core + Authentication
**Rationale:** Authentication is the gateway to all owner actions. The Android app scaffold with Hilt, Navigation3, and the clean architecture layer structure must exist before any feature screens are built.
**Delivers:** Android project setup (Kotlin 2.3.x, Compose BOM 2026.03.00, AGP 9.x, Hilt, Navigation3, Coil, Retrofit, DataStore, Kotlin Serialization); Firebase Auth integration (email/password, anonymous/guest); login, registration, and guest entry screens; multilingual scaffolding (strings.xml English + Romanian placeholders); Firestore repository interfaces and base implementations.
**Uses:** Full stack from STACK.md; Navigation3 1.0.1; Hilt 2.51.x
**Avoids:** Hardcoded strings (Anti-Pattern 5); multilingual retrofitting cost

### Phase 3: Registry Owner Flow
**Rationale:** The owner creates the registry and adds items — this must exist before givers can view anything. URL transformer and affiliate injection ship with item add, not after; they are revenue-critical and cannot be an afterthought.
**Delivers:** Registry creation with occasion type and visibility setting; item add via URL paste with Open Graph auto-fill; AffiliateUrlTransformer Cloud Function (EMAG domain detection + 2Performant tag injection, with logging for unmatched merchants); item list with status display; registry settings screen.
**Implements:** Repository pattern for registry and item domains; AffiliateUrlTransformer with observable logging (Pitfall 5 prevention); item data model (original URL + transformed URL stored separately for re-transformation flexibility)
**Avoids:** Silent affiliate tag failure (Pitfall 5); affiliate logic in APK (Technical Debt pattern)

### Phase 4: Reservation System + Cloud Tasks Expiry
**Rationale:** This is the hardest component and the core value of the product. It must be built as a single atomic unit: transaction + Cloud Task + expiry Cloud Function. Building the timer separately or later is the most common and most expensive mistake.
**Delivers:** `createReservation` Cloud Function (Firestore transaction, Cloud Task enqueue); `releaseReservation` Cloud Task target (atomic status reset, expiry email trigger); `cancelReservation` Cloud Function (for when owner marks item purchased); real-time Firestore listener on `registries/{id}/items` filtered to `status == "available"`; guest identity persistence (SharedPreferences); countdown UI (display-only; server is authoritative).
**Avoids:** Race condition (Pitfall 1 — critical); client-side timer expiry (Pitfall 2 — critical); guest state loss (Pitfall 7); Cloud Functions cold start on reservation (set minimum instances = 1)
**Research flag:** Cloud Tasks task cancellation API and `cloudTaskName` storage pattern should be verified against current Firebase documentation before implementation.

### Phase 5: Web Fallback (Firebase Hosting)
**Rationale:** Web fallback depends on stable Firestore schema (Phase 1), security rules (Phase 1), and the full reservation system (Phase 4). It cannot be read-only — givers must be able to reserve from it. Firebase Hosting serves both the web app and `assetlinks.json` for Android App Links.
**Delivers:** Thin HTML/JS web app (no React framework required for the web fallback — plain Firebase JS SDK); registry view for gift givers; guest name/email collection; reservation flow calling `createReservation` Cloud Function; retailer redirect via affiliate URL; `assetlinks.json` for Android App Links verification; i18n JSON files (en.json, ro.json).
**Avoids:** Private registry bypass via web client (Pitfall 8 — same security rules apply); Firebase Dynamic Links (deprecated August 2025)
**Note:** The FEATURES.md recommends React 19.x for the web fallback, but ARCHITECTURE.md recommends intentionally thin plain HTML/JS. Given the web fallback is gift-giver-only (view + reserve + redirect), plain JS is sufficient and faster to ship. React is appropriate only if the web fallback scope expands.

### Phase 6: Notifications + Email Flows
**Rationale:** Notification infrastructure (FCM, email) depends on reservation system being stable. Expiry email requires the Cloud Task expiry path to exist. Owner notifications require FCM setup.
**Delivers:** Expiry email to giver (triggered by `releaseReservation`); re-reserve flow from email deep link (must go through the same Firestore transaction — no shortcut path); owner push notification on reservation/purchase (Firebase Cloud Messaging, opt-in); invite email for private registries (`sendInviteEmail` Cloud Function); in-app notification for existing users who receive an invite.
**Avoids:** Re-reserve path skipping transaction (Pitfall 3 — stale item state after expiry)

### Phase 7: EMAG Catalog Integration
**Rationale:** Deliberately placed after the core flow is stable because the EMAG API situation is a known uncertainty. Manual URL add must work independently before the catalog is layered on. This phase requires a research spike to resolve the API strategy before implementation begins.
**Delivers:** EMAG product catalog browse and search screen (Android); product results with images (Coil); "Add to registry" action with affiliate tag injection; graceful degradation when EMAG API is unavailable (error state + fallback to manual URL add).
**Avoids:** EMAG API dependency without fallback (Pitfall 6); EMAG API calls from Cloud Functions (Anti-Pattern 3 — unnecessary latency and cost for a public API)
**Research flag:** NEEDS PHASE RESEARCH before implementation. The EMAG public catalog API does not exist as documented. Options (scraping, WebView, URL paste as primary, 2Performant deep links) must be evaluated. Confidence on EMAG integration is LOW.

### Phase 8: Polish + v1.x Features
**Rationale:** Once the core flow is validated with real users, add conversion-optimization and engagement features.
**Delivers:** Guest-to-account conversion prompt (after reservation, not before); private registry invite with in-app notification for existing users; price range advisory nudge in registry editor; multilingual string review and completion; additional merchant affiliate support (beyond EMAG).
**Avoids:** Conversion prompt before reservation (blocks guest flow, increases abandonment by 24-26%)

### Phase Ordering Rationale

- **Schema before code:** Firestore document structure and security rules are expensive to change after feature code references them. Phase 1 front-loads this pain.
- **Auth before owner features:** Registry creation requires an owner identity. Phase 2 unblocks Phase 3 entirely.
- **Reservation as one unit:** Phases 1-4 must be completed before the web fallback ships because the web fallback requires the full reservation flow. Splitting the reservation system across phases risks shipping a half-implemented timer.
- **EMAG last:** The EMAG catalog is an enhancement, not a dependency. Manual URL add ships in Phase 3 and is fully functional. Phase 7 adds EMAG on top without blocking the core flow.
- **Affiliate injection in Phase 3, not Phase 7:** The URL transformer ships with item add (Phase 3). EMAG catalog is a different entry point that reuses the same transformer. Revenue starts from the moment items are added.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 7 (EMAG Catalog):** EMAG has no documented public product catalog API. The integration strategy is unresolved. Needs a dedicated research spike covering: 2Performant affiliate program API details, EMAG ToS on scraping/embedding, and whether a WebView embed meets UX requirements. Do not plan implementation details until this is resolved.
- **Phase 4 (Reservation + Cloud Tasks):** Cloud Tasks task cancellation pattern (storing `cloudTaskName` and calling delete) should be verified against current Firebase documentation — the cancellation API may have changed.

Phases with standard patterns (skip research-phase):
- **Phase 1 (Firebase Foundation):** Firestore data modeling, security rules, and Firebase project setup are extremely well-documented.
- **Phase 2 (Android Core + Auth):** Kotlin/Compose/Hilt/Navigation3 stack has high-quality official documentation and guides.
- **Phase 3 (Registry Owner Flow):** Open Graph scraping for URL auto-fill and clean architecture repository pattern are standard.
- **Phase 5 (Web Fallback):** Firebase Hosting + Firebase JS SDK + plain HTML is a simple, well-documented pattern.
- **Phase 6 (Notifications + Email):** Firebase Cloud Messaging and Firebase Trigger Email Extension are well-documented; expiry email trigger pattern is standard.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Core technologies verified against official release notes and changelogs. Firebase BoM 34.11.0, Compose BOM 2026.03.00, Navigation3 1.0.1, AGP 9.x all confirmed. Hilt version from community sources (MEDIUM for exact version). |
| Features | MEDIUM | Global gift registry market well-documented. Romanian market specifics inferred — no local competitor found to validate EMAG catalog demand. Guest checkout abandonment statistics (24-26%) from Baymard/Shopify research (HIGH confidence). |
| Architecture | HIGH | Firebase patterns (Firestore transactions, Cloud Tasks, App Links) from official Firebase documentation. Clean architecture for Android is well-established. EMAG API architecture is MEDIUM — API nature confirmed but integration pattern unresolved. |
| Pitfalls | MEDIUM-HIGH | Race condition and timer expiry pitfalls verified against Firebase documentation and multiple independent sources. EMAG-specific pitfalls are inferred from the API situation. Security pitfalls (rules, client trust) are well-documented. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **EMAG public catalog API:** No evidence this exists as a consumer-browsable API. The seller/merchant API (v4.5.1) is not appropriate. Before implementing catalog browse, determine the actual integration approach (URL paste as primary, 2Performant affiliate links, WebView embed, or scraping). This decision affects Phase 7 scope significantly.
- **2Performant affiliate program details:** Confirmed EMAG's affiliate program is managed by 2Performant (acquired ProfitShare May 2025). Exact affiliate link parameter format and API access require a registered 2Performant account to verify. Implement EMAG URL transformation conservatively and test against a real affiliate account before shipping.
- **Cloud Tasks cancellation API:** `cloudTaskName` storage for task cancellation is the recommended pattern, but the exact cancellation call signature should be verified against current Firebase Functions task documentation during Phase 4 planning.
- **Android App Links domain setup:** Requires a real domain and DNS control before `assetlinks.json` can be verified by Android. This should be set up in Phase 1 even if the web fallback ships later.

## Sources

### Primary (HIGH confidence)
- [Firebase Android SDK Release Notes](https://firebase.google.com/support/release-notes/android) — BoM 34.11.0, KTX module removal
- [Firebase KTX Migration Guide](https://firebase.google.com/docs/android/kotlin-migration) — main module pattern for Kotlin
- [Jetpack Navigation 3 stable announcement](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html) — Navigation3 1.0.0 stable Nov 2025
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — 2026.03.00 confirmed
- [AGP 9.0 release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — built-in Kotlin support
- [Firebase Firestore Transactions](https://firebase.google.com/docs/firestore/manage-data/transactions) — atomic reservation pattern
- [Firebase Cloud Tasks](https://firebase.google.com/docs/functions/task-functions) — delayed expiry approach
- [Firebase Dynamic Links Deprecation FAQ](https://firebase.google.com/support/dynamic-links-faq) — confirmed shutdown August 2025
- [Android App Links](https://developer.android.com/training/app-links/deep-linking) — replacement for Dynamic Links
- [Firestore transaction serializability](https://firebase.google.com/docs/firestore/transaction-data-contention) — concurrency model
- [Baymard Institute — Gifting UX Best Practices](https://baymard.com/blog/gifting-flow) — guest checkout abandonment data

### Secondary (MEDIUM confidence)
- [2Performant acquires ProfitShare (May 2025)](https://2performant.com/blog/2performant-signs-final-contract-for-the-acquisition-of-profitshare-romania-and-bulgaria/) — EMAG affiliate network
- [EMAG Marketplace API docs](https://marketplace.emag.ro/infocenter/) — confirmed seller-only API, not public catalog
- [Coil changelog](https://coil-kt.github.io/coil/changelog/) — version 3.4.0
- [Retrofit releases](https://github.com/square/retrofit/releases) — 3.0.0 released May 2025
- [MyRegistry.com Feature Comparisons (2026)](https://guides.myregistry.com/gift-list/best-gift-list-apps-2026-the-ultimate-comparison-guide/) — competitor feature analysis
- [Race Conditions in Firestore — QuintoAndar Tech Blog](https://medium.com/quintoandar-tech-blog/race-conditions-in-firestore-how-to-solve-it-5d6ff9e69ba7) — reservation pattern validation

### Tertiary (LOW confidence)
- EMAG public catalog API existence — no evidence found; assumed absent until proven otherwise
- 2Performant affiliate link parameter format — requires registered account to verify exact format

---
*Research completed: 2026-04-04*
*Ready for roadmap: yes*
