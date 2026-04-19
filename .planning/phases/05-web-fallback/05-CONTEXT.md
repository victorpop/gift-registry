# Phase 5: Web Fallback - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Gift givers (giver-only role — not owners) can open a shareable registry link in any modern browser, view the registry with real-time availability, reserve an available item either authenticated or as a guest, and be redirected to the retailer — exercising the same server-authoritative reservation flow (Phase 4 Cloud Functions) that the Android client uses. No owner-facing features exist on the web (no create registry, add item, edit item, invite, settings) — those remain Android-only per the PROJECT.md "web fallback for givers only" decision.

In scope: WEB-01 (view), WEB-02 (reserve), WEB-03 (auth/guest), WEB-04 (retailer redirect).

Out of scope: Owner features on web, PWA / offline, push notifications on web, EMAG catalog browsing on web.

</domain>

<decisions>
## Implementation Decisions

### Web Stack Specifics

- **WEB-D-01:** Vite + React 19 + TypeScript. Static build output deploys to `hosting/public/` via Firebase Hosting. Firebase Hosting is already configured with SPA rewrites (`** → /index.html`).
- **WEB-D-02:** React Router v7 for SPA routing. Handles `/registry/:id` and the `/reservation/:id/re-reserve` email deep link stubbed in Phase 4.
- **WEB-D-03:** Tailwind CSS + headless components (e.g., Radix or Headless UI) for styling. Smaller bundle than MUI for Romanian mobile users; no design-system lock-in; Material3 visual tone expressed via Tailwind config.
- **WEB-D-04:** TanStack Query for server state management on top of Firebase JS SDK calls. Mirrors Android's `callbackFlow + awaitClose` Firestore listener pattern: one query subscribes, results flow into components, cleanup on unmount. No Redux.

### Giver UX Flow

- **WEB-D-05:** `/registry/:id` renders the registry immediately for anonymous visitors. Public registries (REG-04) are shareable by design; a login wall would abandon givers.
- **WEB-D-06:** Guest identity (first name, last name, email) collected via modal dialog on Reserve click — mirrors Android's `GuestIdentitySheet`. Values persisted to `localStorage` so subsequent reserves pre-fill the fields.
- **WEB-D-07:** On successful reservation, open the retailer `affiliateUrl` in a new tab (`target=_blank rel=noopener`). The registry tab stays open so the giver sees the reservation status and countdown update.
- **WEB-D-08:** Countdown display: inline badge on each reserved item card showing "X min left", plus a sticky banner at the top of the page when the current giver has an active reservation (handles the multi-tab / post-redirect case).

### Access Control & Auth

- **WEB-D-09:** Login providers: Email/password + Google OAuth (parity with Android AUTH-01/02/03). Firebase JS SDK `signInWithPopup` for Google; `signInWithEmailAndPassword` for email. "Continue as guest" is not a Firebase Auth call — it routes to the reserve flow and collects identity at that point (WEB-D-06).
- **WEB-D-10:** Private registry gating enforced by Firestore security rules via the `invitedUsers` map (Phase 1 D-15 / Phase 3 D-15). Non-user invites keyed by `email:<address>`; UID-keyed for authenticated invitees. Client does not do access logic — it runs the query and either gets the doc or gets permission-denied, which maps to a 404.
- **WEB-D-11:** Re-reserve email deep link `/reservation/:id/re-reserve` calls the existing `resolveReservation` Cloud Function (Phase 4 plan 06) to resolve the reservation to a `registryId` + `itemId`, then navigates the user to `/registry/:id` with an `autoReserveItemId` hint query param that triggers `createReservation` automatically. No new backend.
- **WEB-D-12:** Firebase Auth `browserLocalPersistence` (default). Session survives tab close — matches AUTH-04 ("session persists across restarts") on Android.

### Content & State Presentation

- **WEB-D-13:** Registry-not-found (bad id, deleted, or rules-denied) → a single generic "Registry not available" 404 page with link back to home. The client does not distinguish "deleted" from "never existed" from "permission denied" — consistent UX that prevents enumeration of private registry IDs.
- **WEB-D-14:** Private registry viewer-not-invited → same 404 page as WEB-D-13. Does not leak that the registry exists.
- **WEB-D-15:** Localization via i18next using the seeded `web/i18n/en.json` + `ro.json` files (feature-namespaced keys: `app_`, `common_`, `auth_`, `registry_`, `reservation_`). Browser locale auto-detected; manual override persisted to localStorage. Cloud Function `HttpsError` codes (e.g., `ITEM_UNAVAILABLE`, `failed-precondition`) map to localized keys — same key set used across both clients.
- **WEB-D-16:** Loading UX: skeleton screens for registry header + item grid on initial load. Inline spinner on the Reserve button during the `createReservation` callable. No full-page spinners.

### Infrastructure Parity with Android

- **WEB-D-17:** Firebase JS SDK Functions instance must be pinned to `europe-west3` region (same defect fixed today in `AppModule.kt` on Android — JS SDK defaults to `us-central1`).
- **WEB-D-18:** Firebase App Check enabled with the **reCAPTCHA v3** provider on web (Play Integrity is Android-only). Same enforcement posture as Android — Firestore and Cloud Functions reject requests without an App Check token.
- **WEB-D-19:** Affiliate URL handling: givers use the `affiliateUrl` stored on the item doc — the web client never runs `AffiliateUrlTransformer` (that's owner-side, Android-only). Redirect uses whatever is on the doc.

### Claude's Discretion

- Exact Tailwind color tokens, component shape language, typography scale
- Component library choice within "headless" category (Radix UI vs Headless UI vs ark-ui) — planner decides based on bundle-size and a11y research
- File/folder layout under the web app root (`src/pages`, `src/features`, `src/components`)
- Form validation library (zod + react-hook-form is a reasonable default but not mandated)
- Whether the sticky reservation banner persists in localStorage or only in React state (both acceptable for the 30-min window)
- Exact skeleton visual rhythm

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets

- `hosting/public/index.html` — placeholder exists; will be replaced by Vite's `index.html` entry
- `web/i18n/en.json` + `web/i18n/ro.json` — i18n seeds already populated with `app_`, `common_`, `auth_`, `registry_`, `reservation_` namespaces from Phase 1 (D-10 / D-11)
- `firebase.json` — Hosting already configured: `public: hosting/public`, SPA rewrite `** → /index.html`, assetlinks.json Content-Type header
- `firestore.rules` — access control for public + private registries, `invitedUsers` map enforcement, items subcollection, reservations hard-deny — ALL shared with web; no rule changes required
- `functions/src/registry/fetchOgMetadata.ts` — owner-only, not called from web
- `functions/src/reservation/createReservation.ts` — callable from web via Firebase JS SDK `httpsCallable(functions, 'createReservation')`
- `functions/src/reservation/resolveReservation.ts` (Phase 4 plan 06) — web calls this for the re-reserve deep link
- Shared Firestore data model: `registries/{id}` + `registries/{id}/items/{itemId}` + `reservations/{resId}` — web consumes the exact same shapes Android does

### Established Patterns

- **Feature-namespaced i18n keys** — Phase 1 convention; web uses identical namespace prefixes
- **Clean separation of read vs write paths** — Android observes Firestore via `callbackFlow`; web observes via `onSnapshot` wrapped in TanStack Query. Writes go through callables.
- **Server-authoritative reservation flow** — no client writes to `reservations/`; web follows the same rule
- **Region pinning** — europe-west3 everywhere (Functions, potentially Firestore location ID)
- **Localized error mapping** — `HttpsError` codes → localized strings
- **Guest identity persistence** — Android uses DataStore; web uses localStorage. Shape (`{firstName, lastName, email}`) identical.

### Integration Points

- **Firebase Hosting** — `firebase.json` already wired; web build output goes into `hosting/public/`. Currently ignores `**/node_modules/**` so `web/` source tree can live outside the deploy root.
- **Shared backend** — No Cloud Function changes required (all callables from Phase 3/4 are web-usable as-is). Rules may need a spot-check that items subcollection reads work for unauthenticated callers (public registry scenario).
- **Email deep links** — Expiry email URLs (Phase 4 `/reservation/:id/re-reserve` and Phase 6 full email flow) will land on the web fallback even for Android-install users on desktop.

</code_context>

<specifics>
## Specific Ideas

- Pin Firebase JS SDK Functions instance to `europe-west3` — deliberately preempts the us-central1 default bug just fixed on Android today.
- Keep the build isolated under a new `web/` directory for source code (separate from the existing `web/i18n/` seeds — those move into `web/src/i18n/` or similar during execution). Vite builds into `hosting/public/` so Firebase Hosting picks up the output.
- Use the same `createReservation` + `resolveReservation` callables — do NOT duplicate reservation logic on the web side.
- Generic 404 for both "not found" and "permission denied" cases — privacy posture, not a UX shortcut.

</specifics>

<deferred>
## Deferred Ideas

- PWA / offline mode — not in scope; Firebase persistence is for the Android client
- Web push notifications — owner-only feature, and web is giver-only; deferred permanently for web
- Visual design refinement (exact tokens, custom illustrations) — acceptable to ship functional-first UI in Phase 5; polish can come as a post-milestone enhancement if needed
- Analytics / funnel tracking on web — not requested; would be a v2 consideration

</deferred>
