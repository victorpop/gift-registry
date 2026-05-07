# Phase 13: Web Fallback Visual Refresh - Context

**Gathered:** 2026-05-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Restyle the existing Phase 5 web fallback (giver-facing flow at `https://gift-registry-ro.web.app`) to match the GiftMaison design language, per `design_handoff_web_giver_flow/README.md`. Restyle covers screens 01 (Registry detail), 02 (Reserve + 30-min timer treatment), and 03 (Auth) only. Functional behaviour, routing, repository code, Firestore rules, and Cloud Functions stay unchanged — this is a visual layer refresh on the existing Vite + React 19 + TypeScript + Tailwind v3 + Radix codebase.

**Scope anchor:** Visual layer only. The 92 existing web tests must stay green; no behaviour shift. Live deploy + guest UAT happens in Phase 14.

**Out of scope (architectural / protocol changes the handoff implies but Phase 13 explicitly defers):**
- Server-side rendering migration (Next.js / Remix / Astro)
- `/r/{slug}` routing + `slug` field on registries
- HttpOnly cookie guest sessions (server-set)
- OG image Cloud Function (1200×630 server-generated, cached)
- Token-based `/r/{slug}/re-reserve?token=…` link
- 4-occasion theme cascade (THEME-01..03 stay deferred to v1.2 per REQUIREMENTS.md)
- 5-minute pre-expiry email reminder (Phase 6 territory; new email type)
- Screen 04 (Guest → account convert modal) — net-new behaviour with timer + dismiss + email-prefill upgrade flow
- Screen 05 (Expired re-reserve rich page) — beyond the existing `/reservation/:id/re-reserve` stub
- Reserver / giver name display (`Andrei P.`) — requires server-side data exposure changes
- "Search / sort within the registry", multi-currency display, group gifting, PWA, print view, comments, owner reply-to-thanks (handoff out-of-scope sections)

</domain>

<decisions>
## Implementation Decisions

### Scope shape

- **D-01:** Visual-only re-skin. Keep the existing Vite SPA + localStorage guest identity + `/registry/:id` routing + doc-id `/reservation/:id/re-reserve`. No SSR migration, no slug routing, no cookie sessions, no OG generation, no backend changes.
- **D-02:** Housewarming palette only on web (matches Android v1.1). Wedding / Baby / Birthday tokens MAY be defined alongside Housewarming in code but only Housewarming is wired through. Full theme cascade stays deferred to v1.2 per REQUIREMENTS.md THEME-01..03.
- **D-03:** Phase 13 restyles **3 screens**: 01 Registry detail (`/registry/:id`), 02 Reserve + 30-min timer treatment, 03 Auth (sign in / sign up / guest). Screens 04 (convert modal) and 05 (expired re-reserve rich page) are deferred — defer to a follow-up phase or v1.2.
- **D-04:** Reserve UX translation — after successful reserve, the existing reserve modal closes and a **sticky reservation banner** appears at top of `/registry/:id` (ink bg, paper text, pulsing accent dot, MM:SS countdown updating every 1 s, "Continue to {retailer} →" accent CTA, "Release reservation" quiet button). Reserve-detail content from screen 02 (item card thumbnail + price + retailer, "Bought it? — I completed the purchase ✓" confirm-back card in `accentSoft`, "How the timer works" 4-step list collapsible on mobile) renders in an in-page subdued section below the hero/progress strip while the user holds an active reservation. No new routes.
- **D-05:** Auth screen 03 editorial photo + quote ships **on desktop (≥ 1024 px), hidden on mobile**. Single curated atmospheric photo (theme.jsx PHOTOS samples acceptable as starting point). Mobile pins the "Continue as guest →" affordance to the bottom of the viewport per handoff mobile rules.
- **D-06:** Reserver / giver names not displayed. Reserved cards show generic `RESERVED` mono-caps pill (accent + pulsing dot per handoff) WITHOUT a "by {name}" suffix. Purchased cards show `✓ Purchased` pill (no `by {firstName}` overlay). Server-side projection of first-name + last-initial deferred to a follow-up phase.

### Tokens, typography, layout

- **D-07:** Token strategy — Tailwind config extended with **CSS custom properties on `:root`** (e.g. `--gm-paper`, `--gm-ink`, `--gm-accent`, `--gm-accentSoft`, etc.) bound to `theme.colors.gm.*` in `tailwind.config.ts`. Keeps the door open for v1.2 occasion-theme cascade (swap variable values at the registry-detail root) without rewriting class names. Source-of-truth values: handoff Housewarming sRGB hex column (oklch values commented for reference / future direct-use migration).
- **D-08:** Type scale — **3 fonts via Google Fonts** with preconnect to both `fonts.googleapis.com` and `fonts.gstatic.com`: Instrument Serif (display, italic accent), Inter (body 400/500), JetBrains Mono (mono caps). Roles per handoff table (Display XL/L/M/S, Body L/M/S, Mono caps), with desktop / mobile size pairs and explicit letter-spacing + line-height. Italic display serif reserved for emphasised phrases inside headlines + the wordmark's terminal period accent (always paired with `accent`).
- **D-09:** Mobile-first commitment — design target 375 × 812. Breakpoints at **640 px** (tablet, 2-col grid, 28 px page padding) and **1024 px** (desktop, 3-col grid, 40 px page padding, side-by-side layouts). All component breakpoints follow the handoff "Responsive behaviour" table.
- **D-10:** Item card — 14 radius, `line` border, `paper` bg. Image **4:3 on mobile, 16:10 from 640 px+** (deliberate switch from Phase 5's 16:9). Status pill top-left over image. Body shows title (15 px body 500, −0.2 letter-spacing), price + retailer row, then status-conditional CTA / banner.
- **D-11:** Pulse animation — single shared CSS keyframe (`opacity 1→0.5, scale 1→0.85, 1.4 s alternate`) applied to Reserved card pill dot + sticky reserve banner dot. Disabled under `prefers-reduced-motion: reduce` (fade-only fallback).
- **D-12:** Sticky CTAs on mobile — primary CTA pins to viewport bottom on screens 02 and 05 with `paper` blurred backdrop + 1 px `line` top-border, 8–12 px from safe-area inset.

### Wordmark + chrome

- **D-13:** Reusable `<Wordmark />` component — Instrument Serif italic + accent-coloured terminal period. Mirrors the Android wordmark but in CSS / `<span>` form. Used in top nav (size 22–24) and footer.
- **D-14:** Top nav (web shared chrome) — wordmark left, EN / RO mono-caps switch + "Sign in" ghost button right (or avatar circle when authenticated). 1 px `line` bottom-border, `paper` bg. No collapse to hamburger on mobile (keep all three visible per handoff).
- **D-15:** Footer — minimal: "© giftmaison 2026 · terms · privacy · en / ro".

### State, status, content

- **D-16:** Status pill set — `Available` neutral pill (paperDeep bg, inkSoft fg, line border), `Reserved` accent pill (accentSoft bg, accent fg, pulsing dot), `Purchased` ok pill (oklch-derived green bg + `ok` fg, ✓ glyph). Sizes: pill height ~22 px, mono-caps copy 11 px small / 12 px regular, letter-spacing 0.3.
- **D-17:** Purchased items remain in the list (opacity 0.55, image grayscale) — trust pattern. No "Given by" name overlay (per D-06).
- **D-18:** Localization — i18next as in Phase 5; EN / RO toggle persists to localStorage. Add new namespaced keys for handoff copy: `web_hero_*`, `web_reserve_*`, `web_auth_*`, `web_pill_*`. `<html lang>` reflects current locale.

### Folded todos

None directly into Phase 13. The two todos surfaced in cross-reference (`2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` and `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md`) belong to Phase 14 (deploy + UAT) and are tracked there.

### Claude's Discretion

- Component file layout under `web/src/` (e.g., `components/giftmaison/`, refactor of existing pages)
- Icon set — `lucide-react` already in deps, keep using it for iconography (G glyph, ✓, ⌛, ◯, ⌃)
- Whether to introduce a small `<Pill>` / `<Btn>` / `<Field>` atom layer mirroring `web-screens.jsx` (recommended for consistency) or inline Tailwind classes
- Specific Unsplash asset for auth editorial photo (or curated alternative)
- Whether the in-page reserve-detail section (D-04) is routed-by-query-param (`?reserved={itemId}`) or pure derived state
- Exact CSS implementation of the focus ring (handoff: 2 px `accent` outline, 2 px offset, `:focus-visible` only)
- Skeleton designs for registry-list / item-grid loading states (Phase 5 D-16 carries forward)
- Whether to keep Phase 5's Radix-based modals as-is for the auth screen or restructure layout

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary handoff (web giver flow)

- `design_handoff_web_giver_flow/README.md` — full spec; sections "Screens" (01–05), "Responsive behaviour", "Design tokens", "Interactions & behaviour", "Accessibility"
- `design_handoff_web_giver_flow/reference/theme.jsx` — Housewarming oklch tokens + 3 deferred occasion palettes + TYPE families + sample data
- `design_handoff_web_giver_flow/reference/web-screens.jsx` — 760 lines of layout / copy / spacing reference for screens 01–05 (NOT production code; reference only)
- `design_handoff_web_giver_flow/reference/browser-window.jsx` — chrome wrapper used in the prototype (visual presentation only, not part of design)
- `design_handoff_web_giver_flow/screens/01-registry-detail.png` — desktop registry detail
- `design_handoff_web_giver_flow/screens/02-reserve-timer.png` — desktop reserve + timer
- `design_handoff_web_giver_flow/screens/03-auth.png` — desktop auth split layout
- `design_handoff_web_giver_flow/screens/04-guest-convert.png` — desktop convert modal (DEFERRED — screen 04 not in Phase 13 scope; PNG kept as reference for the deferred follow-up)
- `design_handoff_web_giver_flow/screens/05-expired.png` — desktop expired page (DEFERRED — same)

### Cross-app consistency (Android handoff)

- `design_handoff_android_owner_flow/README.md` — wordmark spec, status-pill semantics, pulse-animation cadence (1.4 s alternate), per-occasion theming model — web mirrors these for visual cross-app consistency
- `.planning/phases/08-giftmaison-design-foundation/08-CONTEXT.md` — Android-shipped GiftMaison tokens (Housewarming sRGB) — web port should match values

### Project-level context

- `.planning/PROJECT.md` — Current Milestone section ("Giver-facing web fallback is out of scope for v1.1"); will need a wording correction at Phase 14 / milestone evolution since visual refresh + deploy now ARE in v1.1 (functional readiness was always implied; visual refresh is the formal scope addition)
- `.planning/REQUIREMENTS.md` — v1.1 Out of Scope table (THEME-01..03 deferred to v1.2 — web stays Housewarming-only per D-02); v1.0 Web Fallback section (WEB-01..04, complete in Phase 5)
- `.planning/phases/05-web-fallback/05-CONTEXT.md` — locked Phase 5 decisions still in effect (WEB-D-01..D-19); especially D-15 (i18next), D-17 (europe-west3 region pin), D-18 (App Check reCAPTCHA v3 in production)
- `.planning/STATE.md` — Roadmap Evolution (Phase 13 renamed to Visual Refresh, Phase 14 added on 2026-04-30)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets

- `web/src/firebase.ts` — Firebase init, emulator wiring, `browserLocalPersistence`, **`FUNCTIONS_REGION = 'europe-west3'` constant — DO NOT REMOVE** (per Phase 5 WEB-D-17 + Android comment in file)
- `web/src/i18n/` — i18next config with EN/RO, feature-namespaced keys (`app_`, `common_`, `auth_`, `registry_`, `reservation_`); add web-flow keys here
- `web/src/i18n/en.json`, `ro.json` — translation seed files; new strings extend these
- `web/src/pages/AppRootPage.tsx` — top nav target for restyle (wordmark + EN/RO + Sign in)
- `web/src/pages/RegistryPage.tsx` — primary restyle target (registry detail screen 01); existing `useRegistryQuery` + `useActiveReservation` hooks stay
- `web/src/pages/ReReservePage.tsx` — existing stub for `/reservation/:id/re-reserve`; minimal restyle only (rich screen-05 visual deferred)
- Auth screens (current location TBD by planner during scout) — restyle target for screen 03
- `web/tailwind.config.ts` — extend `theme.colors.gm.*` with handoff Housewarming tokens; add `fontFamily.{display,body,mono}` for Instrument Serif / Inter / JetBrains Mono; add CSS-variable plumbing per D-07
- `web/index.html` — `<body class="bg-surface text-surface-on">` will need updating to handoff token names; add Google Fonts preconnect + font-display swap
- `web/public/` — Vite static asset source (e.g., `assetlinks.json` lives here); editorial auth photo could land here or be loaded from Unsplash
- `lucide-react` already in deps for icons
- Radix UI primitives (`Dialog`, `Toast`, `AlertDialog`, `Label`, `VisuallyHidden`) already in deps and used by current modals
- `react-hook-form` + `zod` + `@hookform/resolvers` already in deps for forms

### Established Patterns

- TanStack Query (`@tanstack/react-query` ^5) wraps Firestore `onSnapshot` for reactive reads (Phase 5 pattern)
- React Hook Form + zod for the auth forms and any new fields
- Radix Dialog with `Dialog.Description` as `sr-only` for a11y (Phase 5 D-locked)
- httpsCallable singletons defined at module level for callables that need stable test mocks; created inside hooks for one-shot callables (Phase 5 lessons learned)
- 92 existing web tests (Vitest unit + Playwright e2e) — regression safety net; visual refresh must not break them. Snapshot tests will need re-baselining; behavioural assertions stay
- Image assets that must survive `emptyOutDir: true` go in `web/public/`, not `hosting/public/` (Phase 5 D-locked: Vite empties output on each build)

### Integration Points

- `firebase.json` — hosting config + SPA rewrite stays untouched
- Firestore rules — no change required (no new data model in visual-only)
- Cloud Functions — no change required
- Build pipeline: `npm --prefix web run build` outputs to `hosting/public/` (Phase 5 D-01); preserved
- Tests: Vitest (`npm --prefix web run test:run`) and Playwright (`npm --prefix web run e2e`); both must stay green through the refresh

</code_context>

<specifics>
## Specific Ideas

- The handoff is unambiguous about mobile-first ("design target 375 × 812"; sticky CTAs; bottom-of-viewport guest skip on auth). Plan for mobile from the first plan; desktop is the upper-bound elaboration, not the starting point.
- Tailwind config strategy via CSS custom properties (D-07) is deliberate setup for v1.2's 4-occasion cascade — pay the cost once now, swap variable values later.
- Reserve UX translation (D-04) deliberately keeps the doc-id route while honouring the screen-02 visual contract via sticky banner + in-page reserve-detail section. This is the spirit of the handoff under visual-only scope.
- "If anything here contradicts the prototype, the prototype wins — flag the discrepancy back" (handoff README, "Questions for the design author"). Planner should reconcile any prototype-vs-README differences and surface in Wave 0 / RED tests if material.
- Wordmark cross-app consistency: matches Android Phase 8's letterSpacing fix (`em → sp`/`px` units) — verify visual parity at component-level.
- Status-pill semantics: handoff says `RESERVED → RESERVED`, `PURCHASED → ✓ Purchased` (web), distinct from Android's `PURCHASED → GIVEN` mapping. Web deliberately does NOT mirror Android's "Given" label — handoff is explicit.

</specifics>

<deferred>
## Deferred Ideas

**Architectural / protocol (carried forward to a future web-fallback phase or v1.2):**
- SSR migration (Next.js / Remix / Astro) — handoff prefers SSR for first-paint perf on 4G mobile
- `/r/{slug}` routing + slug field on registries
- HttpOnly cookie guest sessions (server endpoint required)
- OG image Cloud Function (1200×630 generated, cached, revalidate on registry change)
- Token-based `/r/{slug}/re-reserve?token={one-time-token}` link
- 4-occasion theme cascade on web (THEME-01..03 stay deferred to v1.2)
- 5-minute pre-expiry email reminder (Phase 6 territory; new email type)
- Reserver / giver name display (`Andrei P.` — first name + last initial) — requires server-side projection
- "Reserved-by banner with countdown" rendered for non-reserver visitors (depends on reserver-name display)

**Net-new screens not in Phase 13 (defer to follow-up phase / v1.2):**
- Screen 04: Guest → account convert modal (timer + dismiss + email-prefill upgrade flow)
- Screen 05: Expired re-reserve rich page (current `ReReservePage.tsx` stub stays minimal)

**Out-of-scope sections from handoff README (Phase 13 inherits these as project-level deferrals):**
- Search / sort within the registry
- Multi-currency display per visitor locale
- Comments / messages from giver to owner
- Group gifting / chip-in
- Wishlist import
- Email templates (transactional emails)
- Owner reply-to-thanks flow
- PWA / installable variant
- Print view
- Empty states ("registry has no items yet", etc.)

### Reviewed Todos (carried to Phase 14)

- `2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` — folded into Phase 14 (deploy phase) where it belongs functionally
- `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` — folded into Phase 14
- `2026-04-28-deploy-phase-12-storage-rules.md` — stays separate (Phase 12 cleanup, not coupled to web work)

</deferred>

---

*Phase: 13-web-fallback-visual-refresh*
*Context gathered: 2026-05-07*
