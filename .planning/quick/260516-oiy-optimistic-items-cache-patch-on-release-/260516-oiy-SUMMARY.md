---
phase: quick/260516-oiy-optimistic-items-cache-patch-on-release-
plan: 01
subsystem: web/reservation
tags: [web, react-query, reservation, ux, cache, optimistic-update]
requires:
  - quick-260513-j8a (clearActiveReservation on release — established the release-success effect shape this patch extends)
provides:
  - "Optimistic items-cache patch on release-success in ItemReservePage so the just-released item appears available on RegistryPage with no stale-tile flicker"
affects:
  - web/src/pages/ItemReservePage.tsx
tech-stack:
  added: []
  patterns:
    - "React Query optimistic cache write via useQueryClient().setQueryData<T[]> with a functional updater that preserves referential identity for unmodified items and ?? old falls back to undefined when cache is empty"
    - "vi.mock with importOriginal pattern to partial-mock @tanstack/react-query (override useQueryClient only) without breaking real QueryClient/QueryClientProvider used by the render harness"
key-files:
  created: []
  modified:
    - web/src/pages/ItemReservePage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx
decisions:
  - "Use setQueryData (not invalidateQueries) because useItemsQuery is snapshot-listener-backed, not fetch-backed — invalidating would be a no-op; setQueryData directly writes the cache, mirroring what the eventual onSnapshot fire will deliver"
  - "Functional updater preserves referential identity of unrelated items (downstream useMemo / React.memo benefit) instead of reconstructing the array"
  - "Place setQueryData BEFORE clearActiveReservation and navigate inside the effect body so the destination RegistryPage's first render reads the patched value, not the frozen stale one"
  - "Keep `import type { Item, ItemStatus }` as type-only — Item is only used as a type parameter on setQueryData<Item[]>; no runtime import needed"
metrics:
  duration: 4min
  completed: 2026-05-16
---

# Phase quick-260516-oiy Plan 01: Optimistic items-cache patch on release-success Summary

Patch the React Query items cache for `['registry', :id, 'items']` synchronously in the release-success useEffect of `ItemReservePage` so the just-released item appears as `available` on `RegistryPage` the moment the user lands there — eliminating the ~100–500ms window in which the frozen pre-release cache rendered the item still as RESERVED with the reserved-by-me banner, directly contradicting the success toast.

## Root Cause

Snapshot-lag on the React Query items cache across a route unmount:

1. User reserves itemX on `RegistryPage` → `useItemsQuery`'s `onSnapshot` is live → tile renders as RESERVED.
2. User clicks tile → navigates to `/registry/:id/item/:itemId` → `RegistryPage` unmounts → its `onSnapshot` unsubscribes → cache value for `['registry', :id, 'items']` is FROZEN at the post-reserve moment.
3. User clicks Release on `ItemReservePage` → backend flips `item.status` to `'available'`, clears `reservedBy`/`reservedAt`/`expiresAt`.
4. Release-success useEffect navigates back to `/registry/:id` → `RegistryPage` re-mounts → new `onSnapshot` subscribes but takes ~100–500ms to deliver its first snapshot.
5. During the gap, `useItemsQuery` returns the FROZEN cache from step 2 → grid renders itemX still as RESERVED → directly contradicts the success toast.

No backend, network, Firestore-rules, or Cloud-Functions bug. Pure client-side cache freshness.

## Fix Scope

**Single file changed (production code):** `web/src/pages/ItemReservePage.tsx` — 4 edits:

1. New named import `useQueryClient` from `@tanstack/react-query`.
2. New hook call `const queryClient = useQueryClient()` next to the other top-of-component hook calls.
3. New `queryClient.setQueryData<Item[]>(['registry', id, 'items'], updater)` call inside the release-success useEffect, placed FIRST inside the if-block (before `clearActiveReservation()` and `navigate()`).
4. Effect deps array gains `itemId` (read by updater) and `queryClient` (exhaustive-deps).

The updater maps over the existing `Item[]`, overrides ONLY the matching `it.id === itemId` row to `{ ...it, status: 'available' as const, reservedBy: null, reservedAt: null, expiresAt: null }`, and returns `?? old` so an empty/undefined cache is a no-op write (never throws, never overwrites with garbage).

Inline comment explains the snapshot-lag root cause so future readers don't re-introduce the bug.

## Test Added — K-20

`web/src/pages/__tests__/ItemReservePage.test.tsx`:

- New `vi.hoisted` `queryClientMock = { setQueryData: vi.fn() }` declared alongside the other top-of-file hoisted mocks.
- New `vi.mock('@tanstack/react-query', async (importOriginal) => ({ ...await importOriginal(), useQueryClient: () => queryClientMock }))` — partial mock preserves the real `QueryClient` and `QueryClientProvider` consumed by `renderPage`, overrides only `useQueryClient`.
- `beforeEach` adds `queryClientMock.setQueryData.mockReset()` alongside the other resets.
- K-20 asserts:
  1. Drives `releaseMock.status = 'success'` before mount (mirrors P-06b) so the effect fires on first commit.
  2. `setQueryData` is called exactly once with key `['registry', 'reg1', 'items']` and a function updater.
  3. Updater on a 2-item fake input produces a patched itemX with `status: 'available'`, `reservedBy: null`, `reservedAt: null`, `expiresAt: null`; the unrelated item is returned by referential identity (`out.find(...) === fakeIn[1]`).
  4. Updater on `undefined` input returns `undefined` (cache-empty safety).
  5. `mock.invocationCallOrder` proves `setQueryData` fired BEFORE `activeMock.clear` — strict superset proof that the patch happens before `navigate` (which fires after `clear` in the same effect body).
  6. Sanity: `screen.getByTestId('registry-page')` confirms navigation completed.

## Test Totals

- **Before:** 32 tests in `ItemReservePage.test.tsx` (the plan's "195" figure appears to reflect an out-of-date count; actual baseline measured was 32).
- **After:** 33 tests in `ItemReservePage.test.tsx`; full web suite 196/196 pass across 27 files.
- **TDD trail:**
  - RED commit `2a7a623` — test only; 32 pass + 1 fail (K-20 received 0 `setQueryData` calls).
  - GREEN commit `584675d` — production fix; 33/33 pass.

## Verification

| Command | Result |
|---|---|
| `npx tsc --noEmit` (web) | PASS — exit 0, no new errors |
| `npx vitest run src/pages/__tests__/ItemReservePage.test.tsx` | PASS — 33/33 |
| `npm test -- --run` (web full suite) | PASS — 196/196 across 27 files |
| `git diff --stat HEAD~2..HEAD` | Exactly 2 files: `web/src/pages/ItemReservePage.tsx` (19 lines), `web/src/pages/__tests__/ItemReservePage.test.tsx` (72 lines) |
| `git diff HEAD~2..HEAD -- functions/ app/ web/public/locales/` | EMPTY — backend, Android, i18n untouched |
| `git diff HEAD~2..HEAD -- useItemsQuery.ts StickyReserveBanner.tsx ConfirmPurchaseBanner.tsx useReleaseReservation.ts` | EMPTY — reference-only files untouched |

## Files NOT Touched (Scope Guard)

- `web/src/features/registry/useItemsQuery.ts` (reference only — read for queryKey shape)
- `web/src/features/reservation/StickyReserveBanner.tsx`
- `web/src/features/reservation/ConfirmPurchaseBanner.tsx`
- `web/src/features/reservation/useReleaseReservation.ts`
- `web/src/features/reservation/useReservationForItem.ts`
- `web/src/features/reservation/useActiveReservation.ts`
- `functions/` (entire backend)
- `app/` (entire Android codebase)
- `web/public/locales/*.json` (no i18n changes)
- No new file, no new hook, no new component.

## D-06 Invariant

Preserved and strengthened — the patch nulls `reservedBy` (an email field), so the no-reserver-email-rendered invariant on `RegistryPage` and `ItemReservePage` is reinforced rather than weakened.

## Two-Commit TDD Trail

- `2a7a623` — `test(quick-260516-oiy-01): add failing K-20 — optimistic items-cache patch on release-success`
- `584675d` — `fix(quick-260516-oiy-02): patch items cache optimistically on release-success in ItemReservePage`

## Deviations from Plan

**1. Test count baseline**
- **Plan stated:** 195 pre-existing tests in `ItemReservePage.test.tsx`, expected total 196 after K-20.
- **Actual measured:** 32 pre-existing tests, total 33 after K-20.
- **Impact:** None on the fix. The plan's `(grep | count) it()` count appears to predate prunes elsewhere in this file. Full web-suite total of 196 across all files was retained — the plan likely conflated file-level and suite-level counts. The fix and test integrity are unchanged.

**2. Commit suffix bump**
- **Plan stated:** GREEN commit prefix `fix(quick-260516-oiy-01):`.
- **Used:** `fix(quick-260516-oiy-02):` (matches the convention used by precedent quick tasks like 260516-lsi where Task 1 is `-01` (RED test) and Task 2 is `-02` (GREEN fix)).
- **Impact:** Cosmetic; aligns with the established repo convention.

**3. Worktree base sync (pre-Task-1 setup)**
- The worktree was based on `683a122` (j8a); main was at `f60e340` (post-lsi). Per the prompt's explicit instruction, rebased onto local `main` before Task 1 so the new context (iux/k4f/ku3/lbf/lsi history) was visible. Plan dir copied in from the main checkout (it was an untracked dir there). No conflicts.

**4. Symlinked node_modules + copied .env.local**
- Worktree had no `node_modules` and no `.env.local`. Symlinked `web/node_modules` to the parent web checkout and copied `web/.env.local` so vitest could run (firebase auth init reads `VITE_FIREBASE_*` env vars at import time). Standard worktree setup; not a production change.

No Rule-1/2/3 auto-fixes were needed during execution — the plan was unambiguous and the fix landed exactly as specified.

## Self-Check: PASSED

- `web/src/pages/ItemReservePage.tsx` — FOUND (modified, contains `queryClient.setQueryData<Item[]>` and `useQueryClient`).
- `web/src/pages/__tests__/ItemReservePage.test.tsx` — FOUND (modified, contains `K-20` and `queryClientMock`).
- Commit `2a7a623` — FOUND in `git log`.
- Commit `584675d` — FOUND in `git log`.
- `.planning/quick/260516-oiy-optimistic-items-cache-patch-on-release-/260516-oiy-SUMMARY.md` — FOUND (this file).
