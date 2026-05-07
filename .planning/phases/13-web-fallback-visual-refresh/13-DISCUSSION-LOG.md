# Phase 13: Web Fallback Visual Refresh - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-07
**Phase:** 13-web-fallback-visual-refresh
**Mode:** discuss (interactive)
**Areas discussed:** Phase scope shape, Themes scope, Screen coverage, Reserve UX, Auth photo, Reserver names

---

## Round 1: Phase 13 scope ruling

The handoff was reviewed and found to imply architectural/protocol changes beyond pure styling (SSR, slug routing, HttpOnly cookies, OG generation, token re-reserve, 4 themes, 5-min email warning). Three sane interpretations were presented.

| Option | Description | Selected |
|--------|-------------|----------|
| Visual-only re-skin | Tokens + restyle on existing Vite SPA + localStorage + /registry/:id. Defer all architecture/protocol changes. | ✓ |
| Visual + routing + page-model | Adds /r/{slug} routes + slug resolver to the visual layer. Keeps SPA + localStorage + doc-id re-reserve. | |
| Full handoff contract | All visual + routes + SSR + cookies + OG + token re-reserve + 4 themes. 8–12 plans. | |

**User's choice:** Visual-only re-skin.
**Rationale:** Smallest, cleanest phase. Matches the user's earlier "Split P13 visual / P14 deploy + UAT" structure choice. Architecture changes deferred to a later web-fallback phase or v1.2.

---

## Round 1: Theming scope on web

REQUIREMENTS.md defers THEME-01..03 (Wedding / Baby / Birthday) to v1.2; handoff mandates 4 themes. Three options offered.

| Option | Description | Selected |
|--------|-------------|----------|
| Housewarming only | Match Android v1.1 — single palette wired through. | ✓ |
| All 4 themes on web | Web leads Android. Reverses THEME deferral for web only. | |
| Theming infrastructure, single applied theme | CSS-variable infra in place, only Housewarming wired; v1.2 flips others on without code change. | |

**User's choice:** Housewarming only.
**Note:** D-07 in CONTEXT.md keeps the CSS-variable infra approach anyway, which delivers most of the third option's benefit without expanding REQUIREMENTS scope.

---

## Round 2: Screen coverage

Multi-select on which of the 5 handoff screens Phase 13 restyles.

| Option | Description | Selected |
|--------|-------------|----------|
| 01 Registry detail | Hero, progress strip, filter chips, item grid, item card visuals. | ✓ |
| 02 Reserve + 30-min timer | Restyle reserve modal/flow + sticky reservation banner with countdown. | ✓ |
| 03 Auth | Tab switcher, Google ghost button, dashed-border guest skip card, editorial right column on desktop. | ✓ |
| 04 Convert modal + 05 Expired re-reserve | Net-new behaviour (timer + dismiss + upgrade) and rich expired page. | |

**User's choice:** Screens 01 + 02 + 03. Screens 04 and 05 deferred.

---

## Round 2: Reserve UX translation

Handoff has a dedicated `/r/{slug}/reserve/{itemId}` route for screen 02; visual-only scope keeps `/registry/:id`.

| Option | Description | Selected |
|--------|-------------|----------|
| Sticky banner on registry page after reserve | Modal closes; sticky banner + countdown + retailer CTA appears at top of /registry/:id. Reserve-detail content (item card, "Bought it?" confirm-back, how-it-works) renders in an in-page subdued section while reservation is active. | ✓ |
| Reserve becomes a fullscreen modal/route | Modal expands to fullscreen with screen-02 layout (sticky banner + 2-col main+sidebar). | |
| Banner only — skip detail | Just sticky banner + countdown + retailer CTA. Smallest scope. | |

**User's choice:** Sticky banner + in-page detail section.

---

## Round 2: Auth screen-03 editorial photo

Desktop ref shows a curated atmospheric photo + quote in the right column.

| Option | Description | Selected |
|--------|-------------|----------|
| Ship photo + quote on desktop, hide on mobile | Match handoff. Source one Unsplash atmosphere photo. Hidden < 1024 px. | ✓ |
| Skip editorial column on all viewports | Form full-width on desktop too. Cleaner ship, no asset curation. | |

**User's choice:** Ship the photo on desktop.

---

## Round 2: Reserver name display

Handoff says reserver/giver names display as `Andrei P.` (first name + last initial). Phase 5 currently displays no names.

| Option | Description | Selected |
|--------|-------------|----------|
| Defer to follow-up phase / v1.2 | Server-side data exposure required (Firestore rules + projection); out of pure visual scope. Phase 13 uses generic `RESERVED` chip without name. | ✓ |
| Build it in Phase 13 | Extend reservation/item docs to expose firstName + last-initial publicly + update Firestore rules. Honours handoff fully but expands scope into backend. | |

**User's choice:** Defer.

---

## Folded Todos

None directly into Phase 13. Two todos surfaced via cross-reference belong functionally to Phase 14 (deploy + UAT) and were tagged accordingly:

- `2026-04-20-register-firebase-web-app-and-deploy-real-web-config.md` → Phase 14
- `2026-04-20-fix-functions-tsconfig-and-env-handling-to-unblock-firebase-deploy.md` → Phase 14

The third matched todo (`2026-04-28-deploy-phase-12-storage-rules.md`) stays separate — it's a Phase 12 cleanup, not coupled to web work.

---

## Deferred Ideas captured during discussion

Architectural / protocol changes deferred:
- SSR migration (Next.js / Remix / Astro)
- `/r/{slug}` slug-based routing + slug field on registries
- HttpOnly cookie guest sessions
- OG image Cloud Function (1200×630 generated)
- Token-based re-reserve link
- 4-occasion theme cascade on web
- 5-minute pre-expiry email reminder (new email type — Phase 6 territory)
- Reserver / giver name display (`Andrei P.`)
- Reserved-by banner on cards (depends on reserver-name display)

Screens deferred to follow-up:
- Screen 04 Guest → account convert modal
- Screen 05 Expired re-reserve rich page

Out-of-scope sections inherited from handoff README:
- Search / sort within registry
- Multi-currency display
- Comments / messages
- Group gifting / chip-in
- Wishlist import
- Email templates
- Owner reply-to-thanks flow
- PWA / installable variant
- Print view
- Empty states

---

## Claude's Discretion (recorded for planner)

- Component file layout under `web/src/`
- Specific Unsplash asset for auth editorial photo
- Whether the in-page reserve-detail section is routed-by-query-param or pure derived state
- Atom layer (`<Pill>`, `<Btn>`, `<Field>`) inspired by `web-screens.jsx` vs inline Tailwind
- Exact focus-ring CSS implementation
- Skeleton designs for loading states (Phase 5 D-16 carries forward)
- Whether to keep Phase 5's Radix-based modals as-is for auth or restructure layout
