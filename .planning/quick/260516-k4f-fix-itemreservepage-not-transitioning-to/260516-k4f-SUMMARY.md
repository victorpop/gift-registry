---
phase: quick-260516-k4f
plan: 01
subsystem: web/reservation
tags: [web, react, reservation, bugfix, tdd, k37-followup, iux-followup]
requires:
  - web/src/features/reservation/useReservationForItem.ts
  - web/src/features/reservation/useActiveReservation.ts
  - web/src/features/reservation/useCreateReservation.ts
provides:
  - "Cross-hook derived `active` in ItemReservePage: prefers sharedActive when sharedActive.itemId === itemId, else falls back to lookupActive"
affects:
  - web/src/pages/ItemReservePage.tsx
tech-stack:
  added: []
  patterns:
    - "Cross-hook state bridge via same-scope shadowing: rename one hook's destructure (active → lookupActive), pull the other hook's value (active: sharedActive), derive `const active` that resolves at the original scope so downstream consumers compile unchanged"
    - "itemId-equality guard on shared-context reads to prevent stale-leak across SPA route changes"
key-files:
  created: []
  modified:
    - web/src/pages/ItemReservePage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx
decisions:
  - "Derive `const active = (sharedActive && sharedActive.itemId === itemId) ? sharedActive : lookupActive` at the SAME scope as the previously destructured `active` so every downstream reference (useCountdown, sawNonExpiredRef effect, navigate-on-status-flip effect, effectiveActive derivation, branches 4/5, renderReservedByMeDetail) compiles with zero textual edits"
  - "itemId equality is part of the ternary (not a separate effect or null-clear) — prevents stale sharedActive from a previous /item/:otherItemId visit from leaking onto THIS page"
  - "effectiveActive (iux) continues to wrap the derived `active` unchanged — stale-expired-on-mount short-circuit still works for shared-context-sourced actives in the rare past-30-min in-session drift case (K-09 mechanism intact)"
  - "Reorder useActiveReservation destructure to BEFORE useCountdown (safe under Rules of Hooks: both called unconditionally every render, no conditional branches between them) so `sharedActive` is in scope for the derivation that feeds useCountdown(active?.expiresAtMs)"
metrics:
  duration_minutes: 4
  completed: 2026-05-16T11:38:28Z
  tasks_completed: 2
  files_modified: 2
  tests_added: 3
  tests_total_before: 167
  tests_total_after: 170
---

# Quick 260516-k4f: Fix ItemReservePage not transitioning to reserved-by-me detail after Reserve from BROWSE_AVAILABLE — Summary

Restored the k37 BROWSE_AVAILABLE → Reserve happy path: clicking Reserve on the available-state detail page now immediately transitions to the reserved-by-me detail view (countdown + Release + Confirm CTAs) instead of falling through to BROWSE_RESERVED_BY_OTHER as if the user's own reservation belonged to someone else.

## Root Cause

`useReservationForItem` and `useActiveReservation` are independent state systems. The Reserve mutation's `onSuccess` (ItemReservePage.tsx lines 101-118) calls `setActive({ itemId: vars.itemId, ... })` on the shared `useActiveReservation` context — but the page's local `active` symbol was destructured ONLY from `useReservationForItem`, which fetches via the `getReservationForItem` callable and never observes the shared React context.

After Reserve fired, `lookupActive` stayed null on the next render, the page's `active` was therefore null, branch 5 (reserved-by-me detail) did not fire, and once the Firestore item.status flipped to 'reserved' the page rendered BROWSE_RESERVED_BY_OTHER — for the user's OWN just-made reservation.

A misleading comment at lines 94-100 had previously claimed `useReservationForItem` would pick up the new reservation on the next render. That was wrong, and the wrong comment is what allowed the bug to ship.

## Fix Shape

Single new computation in `ItemReservePage.tsx`:

```ts
const { status: lookupStatus, active: lookupActive } = useReservationForItem(id, itemId)
const { active: sharedActive, set: setActive, clear: clearActiveReservation } = useActiveReservation()
const active = (sharedActive && sharedActive.itemId === itemId) ? sharedActive : lookupActive
const countdown = useCountdown(active?.expiresAtMs ?? null)
// ... rest of refs / effects / branches unchanged
```

Everything else in the file change is mechanical:
- **Rename:** `useReservationForItem`'s destructure `active` → `lookupActive`.
- **Expand:** `useActiveReservation`'s destructure now also pulls `active: sharedActive` (previously only `set` and `clear`).
- **Reorder:** `useActiveReservation` moves up by one line (above `useCountdown`) so `sharedActive` is in scope before the derivation that feeds `useCountdown(active?.expiresAtMs)`. Safe under Rules of Hooks — both hooks called unconditionally every render with no conditional branches between them.
- **Comment cleanup:** Replaced the misleading lines 94-100 comment with an accurate explanation referencing the k4f derivation; added a NOTE clarifying that `useReservationForItem` is NOT what drives the transition.
- **JSDoc update:** New paragraph after the stale-expired-on-mount block documenting the `active` derivation, the itemId equality guard, and confirming effectiveActive (iux) keeps wrapping the derived `active`.

Zero textual edits to downstream consumers of `active`: `useCountdown(active?.expiresAtMs ?? null)`, the `sawNonExpiredRef` tracking useEffect, the navigate-on-status-flip useEffect (`if (!active) return`), the `effectiveActive` derivation, branches 4/5, `renderReservedByMeDetail({ active: effectiveActive })`. The new `const active` shadows at the same scope as the previously destructured `active`, so every reference resolves to the derived value with zero downstream changes.

## Why itemId Equality Matters

The ternary uses `(sharedActive && sharedActive.itemId === itemId) ? sharedActive : lookupActive` — NOT `sharedActive ?? lookupActive`. The equality guard prevents a stale shared context leak: if the user visited `/item/A` earlier in the SPA session and shared context still holds an active for item A, then navigates to `/item/B`, the derivation falls back to `lookupActive` for item B instead of mis-rendering item A's reservation on item B's page. K-14 locks this behavior in.

## Why effectiveActive (iux) Keeps Wrapping the Derived active

`effectiveActive = (active && countdown?.expired && !sawNonExpiredRef.current) ? null : active` is the iux-introduced gate that short-circuits stale-expired-on-mount reservations into the item.status browse branches. With the new derivation, effectiveActive's behaviour is unchanged — if sharedActive is non-null and matches itemId, effectiveActive uses sharedActive subject to the same gate. A freshly-set sharedActive from a successful Reserve has expiresAtMs in the future, so countdown.expired=false, so effectiveActive=sharedActive, so branch 5 fires.

The rare case (user reserves, drifts past 30 min without leaving the page) still renders the legitimate `item-reserve-expired` branch via the existing K-09 mechanism, because sawNonExpiredRef flips true on the first non-expired observation regardless of whether the active came from sharedActive or lookupActive.

## Tests

Was 167, now 170. Three new tests appended to `web/src/pages/__tests__/ItemReservePage.test.tsx` after K-11:

- **K-12** (RED → GREEN after fix): clicks Reserve on BROWSE_AVAILABLE with a signed-in user, fires the mutation's captured onSuccess (which calls `setActive` via a spy that mirrors real context behaviour by mutating `activeMock.active`), forces a re-render via `rerenderSame()`, asserts BROWSE_AVAILABLE is released and `item-reserve-detail` is now visible. Also asserts D-06: no email leak (`u1@x.com`, `user@example.com` both absent).
- **K-13** (was GREEN; regression guard): hydration-on-fresh-mount path — `useReservationForItem` returns active for THIS itemId AND `activeMock.active === null` — must still render reserved-by-me detail. Guards against a future refactor accidentally inverting the ternary so it only reads from sharedActive.
- **K-14** (was GREEN; cross-itemId stale-context guard): `activeMock.active = { itemId: 'OTHER-ITEM', ... }` with the route itemId being `it1` — derived `active` must fall back to `lookupActive` (null), so the page renders BROWSE_AVAILABLE and does NOT leak 'Other Item' into the DOM. Locks in the itemId equality guard so removing it would break this test.

Also added `activeMock.active = null` to the existing `beforeEach` as a one-line leak prevention safety net (no-op for all 167 pre-existing tests; protects K-12/K-14 and any future test that depends on a clean shared context).

## Constraint Compliance

- ZERO backend changes. ZERO i18n changes. ZERO new files / components / hooks.
- D-06 unchanged (K-12 explicit negative assertions on email text).
- `useReservationForItem.ts`, `useActiveReservation.ts`, `useCreateReservation.ts` NOT modified.
- The derived `active` line is the SINGLE new computation in `ItemReservePage.tsx` — rest is rename + destructure expansion + JSDoc + comment cleanup.
- iux's `effectiveActive` gate continues to wrap the derived `active` unchanged.

## Verification Results

- **Test suite:** 170/170 GREEN (was 167; +K-12 +K-13 +K-14).
- **TypeScript:** `npm --prefix web run typecheck` → zero errors.
- **Build:** `npm run build` → clean (1823 modules transformed, no errors).
- **Grep sanity:** every `\bactive\b` token in ItemReservePage.tsx either resolves to the new derived const, is part of `lookupActive` / `sharedActive` / `effectiveActive`, is a parameter/field name (`active.expiresAtMs`, `active: effectiveActive`), or is JSDoc/comment text.

## Deviations from Plan

None — plan executed exactly as written.

## Commits

- `93eca0d` — `test(quick-260516-k4f-01): add failing K-12 for Reserve→reserved-by-me transition + K-13/K-14 regression guards` (Task 1, RED).
- `7a10591` — `fix(quick-260516-k4f-01): derive active from shared+lookup so Reserve transitions to reserved-by-me detail` (Task 2, GREEN + JSDoc + comment cleanup).

## Human-Verify

Outstanding — plan does NOT include a human-verify checkpoint. Suggested browser walkthrough (from PLAN <verification>):
1. Sign in as a registry owner, create a registry with 1+ available items.
2. Open share URL → click an available item card → reach `/registry/:id/item/:itemId` on BROWSE_AVAILABLE.
3. Click "Reserve this gift" — expect immediate transition to reserved-by-me detail (countdown ticking, Release CTA, ConfirmPurchaseBanner, Continue to retailer link). URL stays at `/registry/:id/item/:itemId`. No email/displayName surfaced (D-06).
4. From reserved-by-me, click "Back to registry", then click a DIFFERENT available item → expect BROWSE_AVAILABLE (stale sharedActive from item 1 must NOT leak onto item 2).
5. Existing regressions: Release still works (j8a intact); Confirm flips status + navigates back (g9g + hon intact); refresh on reserved-by-me detail rehydrates correctly (k13 intact); anon-no-identity Reserve still round-trips through RegistryPage's GuestIdentityModal (k37 K-06 intact).

## Self-Check: PASSED
