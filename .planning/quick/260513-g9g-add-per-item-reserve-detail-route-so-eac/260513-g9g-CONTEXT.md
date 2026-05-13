---
name: quick-260513-g9g-context
description: Locked decisions from --discuss for per-item reserve-detail route enabling multi-reservation reachability
metadata:
  type: context
---

# Quick Task 260513-g9g: Add per-item reserve-detail route so each of multiple reservations is reachable - Context

**Gathered:** 2026-05-13
**Status:** Ready for planning

<domain>
## Task Boundary

When a user (guest or signed-in) has 2+ active reservations on the same registry, only the most-recent one is reachable today because `useActiveReservation` is single-slot and `hydrateActiveReservation` returns at most one. The user wants each reserved item to have a dedicated URL, reachable by clicking the reserved item tile, that renders the full reserve-detail UI (countdown + "I completed the purchase" + "Release reservation" + "Continue to retailer") scoped to that specific reservation.

Scope is web-only. Backend callables already exist (`hydrateActiveReservation`, `releaseReservationCallable`, `confirmPurchase`, `releaseReservation`). The new work is mostly client-side routing + a new page component + click-to-navigate wiring.

</domain>

<decisions>
## Implementation Decisions

### URL scheme
- **`/registry/:id/item/:itemId`** — item id is the stable user-visible handle, matches the Android EditItemKey pattern, survives reservation lifecycle transitions (expire / re-reserve / purchase) without breaking the URL, and is deep-linkable from notification emails.
- Reservation id is NOT in the URL. The page resolves the active reservation for the item server-side per session.

### List-page UI behavior
- **Keep both** the in-page `ReserveDetailSection` and the `StickyReserveBanner` exactly as they are today on `/registry/:id`.
- They continue to render for the **most recent** active reservation (single-slot `useActiveReservation` context unchanged).
- Older / non-most-recent reservations are reached by **clicking the reserved item card** → navigation to `/registry/:id/item/:itemId`.
- Card click for a reserved-by-me item is the discovery affordance for the dedicated page. Card click for non-reserved or reserved-by-other items: no change in behavior.
- Minimal UI churn — do not refactor the sticky banner into a stack; do not remove the in-page section.

### Post-reserve auto-navigate
- On successful reserve (any caller: `ReserveButton`, `RegistryPage` auto-reserve-from-deep-link, `GuestIdentityModal` submit path), **auto-navigate** to `/registry/:id/item/:itemId`.
- Replaces the just-removed (fk1) "auto-open retailer URL" behavior with something deliberate and contained to the app. The user lands on a page where Mark-as-purchased / Release / Continue-to-retailer are all available.
- Toast still fires.

### Claude's Discretion
- **Hydration shape**: keep `hydrateActiveReservation` returning one reservation for the list-page sticky banner (most-recent active). Add a new per-item lookup (callable or client-side query) for the dedicated page route — `/registry/:id/item/:itemId` needs to know its specific reservation's `reservationId` + `affiliateUrl` + `expiresAtMs` to render the countdown/CTAs.
- **Page component reuse**: the dedicated page should share the same visual building blocks as the existing in-page `ReserveDetailSection` — reuse `ConfirmPurchaseBanner`, the countdown, and the "How the timer works" sidebar — rather than copy-pasting markup. Keep D-06 compliance (no reserver name shown).
- **NotFound state**: if the user navigates to `/registry/:id/item/:itemId` for an item that is NOT reserved by them (not theirs, or already purchased/expired/released), show a friendly "this isn't your reservation" state with a Back link to the registry, NOT a 404. The item is real; the page just isn't reachable for that user.
- **Auth/guest paths**: the dedicated page must work for both signed-in users (uid-based ownership) and guests (email-based ownership) — identical to the hydration flow that x5d already established.
- **Tests**: add component tests for the new page covering the available-but-not-yours, reserved-by-me, purchased, expired states; add a test for the post-reserve auto-navigate; update tests that asserted no navigation on success.

</decisions>

<specifics>
## Specific Ideas

- The dedicated page route element is the React component to add; mount it via `react-router` in `web/src/App.tsx` alongside the existing routes.
- The page query likely needs a new callable `getReservationForItem({ registryId, itemId, giverEmail? })` returning `{ active: ActiveReservation | null }` — mirror the `hydrateActiveReservation` shape and ownership rules.
- Post-reserve navigation: `useNavigate()` from react-router invoked in the existing `useCreateReservation({ onSuccess })` callback site (`RegistryPage.autoReserveMutation.onSuccess` + the `ReserveButton`/`GuestIdentityModal` reserve paths). Pattern: navigate after setting `useActiveReservation` so the list-page UI is already populated when the user clicks Back.
- Honor existing patterns: `cd web && npm run test:run` + `npx tsc --noEmit` must remain green. i18n keys synced across all 4 files when added.

</specifics>

<canonical_refs>
## Canonical References

- Existing `ReserveDetailSection.tsx` — visual layout to mirror on the dedicated page
- Existing `hydrateActiveReservation` callable (functions/src/reservation/hydrateActiveReservation.ts) — ownership rules pattern to reuse for the new per-item lookup
- Phase 5 design tokens — preserve gm-paperDeep surface, gm-accentSoft confirm card, gm-ink sticky banner
- D-06 (CONTEXT) — never display reserver name in any visible string

</canonical_refs>
