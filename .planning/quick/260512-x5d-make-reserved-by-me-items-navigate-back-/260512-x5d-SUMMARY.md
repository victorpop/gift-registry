---
phase: quick-260512-x5d
plan: 01
subsystem: web-reservation
tags: [reservation, hydration, cloud-functions, callable, web-hooks, scroll-ux, i18n]
dependency_graph:
  requires:
    - Phase 05 web fallback (StickyReserveBanner, ReserveDetailSection, useActiveReservation)
    - Phase 06 confirmPurchase callable (pattern mirrored exactly)
  provides:
    - Durable reservation state across page refreshes, new tabs, other devices (signed-in)
    - Server-side manual release with ownership enforcement
    - Click-to-scroll from reserved-by-me ItemCard to ReserveDetailSection
  affects:
    - StickyReserveBanner (Release CTA now calls callable, not just clear())
    - RegistryPage (hydration hook added)
    - ItemCard / ItemGrid (reserved-by-me click prop thread)
tech_stack:
  added:
    - releaseReservationCallable (onCall, europe-west3)
    - hydrateActiveReservation (onCall, europe-west3)
  patterns:
    - httpsCallable created inside callback (vitest mock binding pattern)
    - skipNotYetExpiredGuard flag extends releaseReservationCore without altering onTaskDispatched behaviour
    - Key-based double-fire guard in useActiveReservationHydration (StrictMode safe)
key_files:
  created:
    - functions/src/reservation/hydrateActiveReservation.ts
    - functions/src/__tests__/releaseReservationCallable.test.ts
    - functions/src/__tests__/hydrateActiveReservation.test.ts
    - web/src/features/reservation/useReleaseReservation.ts
    - web/src/features/reservation/useActiveReservationHydration.ts
  modified:
    - functions/src/reservation/releaseReservation.ts
    - functions/src/index.ts
    - web/src/features/reservation/ReserveDetailSection.tsx
    - web/src/features/reservation/StickyReserveBanner.tsx
    - web/src/features/registry/ItemCard.tsx
    - web/src/features/registry/ItemGrid.tsx
    - web/src/pages/RegistryPage.tsx
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json
decisions:
  - "hydrateActiveReservation placed in separate file (functions/src/reservation/hydrateActiveReservation.ts) for grep-ability as specified in plan"
  - "releaseReservationCallable placed in same file as releaseReservationCore (functions/src/reservation/releaseReservation.ts) so the core function and its callable wrapper are co-located"
  - "No Release CTA added to ReserveDetailSection — confirmed Release lives only in StickyReserveBanner; added explanatory comment"
  - "ItemCard reserved banner split: button when onReservedByMeClick provided, div otherwise (preserves non-owner UX unchanged)"
metrics:
  completed_date: "2026-05-12"
  tasks_completed: 3
  tasks_total: 4
  files_created: 5
  files_modified: 11
  tests_added: 27
---

# Quick Task 260512-x5d: Durable Reservation State + Server-Side Release

**One-liner:** Auto-hydration of active reservation from Firestore via new callable, server-side release with ownership enforcement + Cloud Task cancellation, and click-to-scroll from reserved-by-me ItemCard to ReserveDetailSection anchor.

## What Was Built

### Task 1: Backend callables (commit f184fd7)

**`releaseReservationCallable`** (functions/src/reservation/releaseReservation.ts)
- Replaces the broken client-only `clear()` Release path with a server-side callable
- Ownership rules enforced in a read-only transaction before delegating to `releaseReservationCore`:
  - Signed-in: `request.auth.uid === reservation.giverId`
  - Guest: `request.auth === null`, `payload.giverEmail === reservation.giverEmail`, `reservation.giverId === null`
- Non-owners receive `HttpsError('permission-denied', 'RELEASE_NOT_OWNER')`
- Calls `releaseReservationCore({ skipNotYetExpiredGuard: true })` — manual release bypasses the timer guard
- Cancels the scheduled Cloud Task after the release commits (swallows NOT_FOUND / code 5)
- 9 test cases covering ownership, expiry guard bypass, Cloud Task cancellation, and error codes

**`hydrateActiveReservation`** (functions/src/reservation/hydrateActiveReservation.ts)
- Returns the caller's active reservation for a given registry
- Signed-in path: queries by `giverId == auth.uid`; ignores any `payload.giverEmail` (defence in depth)
- Guest path: requires `payload.giverEmail`; queries by `giverEmail == email AND giverId == null`
- Reads item doc for `title` (→ `itemName`) and `merchantDomain`
- Returns `{ active: null }` when no match or `affiliateUrl === ''` (legacy data guard)
- 7 test cases

**`releaseReservationCore` extension:**
- Added optional `skipNotYetExpiredGuard?: boolean` to `ReleaseReservationCoreArgs`
- `onTaskDispatched` handler does NOT pass this flag — scheduled timer behaviour unchanged

### Task 2: Web hooks (commit 7c68d40)

**`useReleaseReservation`** (web/src/features/reservation/useReleaseReservation.ts)
- Mirrors `useConfirmPurchase` exactly: `httpsCallable` created inside `release()` callback for vitest mock binding
- Status lifecycle: `idle → pending → success | error`
- Signed-in: pass only `reservationId`; Guest: pass `reservationId + giverEmail`

**`useActiveReservationHydration`** (web/src/features/reservation/useActiveReservationHydration.ts)
- Effect hook; gates on `authReady && registryId && (user || identity) && !active`
- `active === null` guard: does NOT clobber a fresh in-session reservation set by `useCreateReservation.onSuccess`
- Key-based ref (`${registryId}|${uid}|${email}`) prevents double-fire under React StrictMode
- Cleanup cancellation via `cancelled` ref (safe on unmount)
- Signed-in sends only `registryId`; Guest sends `registryId + giverEmail`
- Failure is best-effort: logs warning, does not block page render

### Task 3: Web UI wiring (commit 90abfba)

**ReserveDetailSection:** Added `id="reserve-detail-section"` to outer `<section>` — stable scroll target.

**RegistryPage:**
- Added `useActiveReservationHydration(id)` call after existing auth hooks
- Added `effectiveEmail` derivation (`user?.email ?? identity?.email ?? null`)
- Added `renderReservedByMeClick` factory using `useCallback` — returns a scroll callback only when `item.status === 'reserved' && item.reservedBy === effectiveEmail` (D-06 compliant: never reveals reserver identity to others)
- Passes `renderReservedByMeClick` to `<ItemGrid />`
- Imported `useCallback`, `useActiveReservationHydration`, `type Item`

**ItemGrid:** Added `renderReservedByMeClick?: (item: Item) => (() => void) | undefined` prop; passes `onReservedByMeClick={renderReservedByMeClick?.(item)}` to each ItemCard.

**ItemCard:** Added `onReservedByMeClick?: () => void` prop. When provided for a reserved item, the reserved banner row renders as a `<button>` with `aria-label={t('web_pill.reserved_by_me_scroll_aria')}` and `focus-visible:outline` for keyboard accessibility. When omitted, renders as existing non-interactive `<div>` (unchanged for non-owners).

**StickyReserveBanner:**
- Replaced `onClick={clear}` on Release button with `onClick={async () => { if (!active) return; await release(active.reservationId, giverEmailToSend) }}`
- Added `disabled={releaseStatus === 'pending'}` and `aria-busy={releaseStatus === 'pending'}`
- `giverEmailToSend = user ? undefined : identity?.email ?? undefined`
- Success effect: shows `t('reservation.release_success')` toast, then calls `clear()`
- Error effect: shows `t('reservation.release_error')` toast (once per error message via ref guard)
- Expiry auto-clear effect preserved unchanged

**i18n (3 new keys, 4 files):**
- `reservation.release_success`: EN "Reservation released." / RO "Rezervare eliberată."
- `reservation.release_error`: EN "Could not release reservation. Please try again." / RO "Nu s-a putut elibera rezervarea. Încearcă din nou."
- `web_pill.reserved_by_me_scroll_aria`: EN "Show your reservation details" / RO "Arată detaliile rezervării tale"

## Hydration Contract

| Path | Caller sends | Backend uses | Query |
|------|-------------|--------------|-------|
| Signed-in | `{ registryId }` | `auth.uid` | `giverId == uid AND registryId == X AND status == 'active'` |
| Guest | `{ registryId, giverEmail }` | `giverEmail` from payload | `giverEmail == email AND giverId == null AND registryId == X AND status == 'active'` |

Response shape: `{ active: { reservationId, itemId, itemName, affiliateUrl, merchantDomain, expiresAtMs } | null }`

## Ownership Rules in releaseReservationCallable

```
if (request.auth):
  auth.uid !== reservation.giverId → permission-denied RELEASE_NOT_OWNER
else (guest):
  !payload.giverEmail OR reservation.giverId !== null OR reservation.giverEmail !== payload.giverEmail → permission-denied RELEASE_NOT_OWNER
```

## Deviations from Plan

None — plan executed exactly as written.

## Task 4: Pending Human Verification

Task 4 is a `checkpoint:human-verify` gate. Tasks 1–3 are complete. UAT scenarios A–I require manual testing with Firebase emulators + web dev server running.

## Self-Check

**Files created:**
- functions/src/reservation/hydrateActiveReservation.ts — FOUND
- functions/src/__tests__/releaseReservationCallable.test.ts — FOUND
- functions/src/__tests__/hydrateActiveReservation.test.ts — FOUND
- web/src/features/reservation/useReleaseReservation.ts — FOUND
- web/src/features/reservation/useActiveReservationHydration.ts — FOUND

**Commits:**
- f184fd7 — feat(quick-260512-x5d-01) — FOUND
- 7c68d40 — feat(quick-260512-x5d-02) — FOUND
- 90abfba — feat(quick-260512-x5d-03) — FOUND

**Test results:**
- `cd functions && npm test` — 78 tests, 11 suites, all passed
- `cd web && npx tsc --noEmit` — exit 0
- `cd web && npm run test:run` — 115 tests, 23 files, all passed

## Self-Check: PASSED
