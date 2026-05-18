---
phase: quick/260518-j5j-fix-release-from-itemreservepage-race-pa
plan: 01
subsystem: web/reservation
tags: [react, react-router, react-query, location-state, useMemo, hydration-race, snapshot-race, optimistic-update]

requires:
  - phase: quick/260516-oiy
    provides: optimistic items-cache patch on release-success (insufficient alone — addresses snapshot race only partially; this plan extends with hydration suppression + render-shape override)
  - phase: quick/260513-j8a
    provides: clearActiveReservation in ItemReservePage release-success effect (kept; j5j is additive)
provides:
  - "Navigate-state propagation from ItemReservePage release-success: { recentReleasedReservationId, recentReleasedItemId } via react-router navigate(path, { state })"
  - "useActiveReservationHydration optional 2nd arg { ignoreReservationId }: when backend's active.reservationId matches, treat response as null (setStatus 'empty', skip set) — defeats hydration race"
  - "RegistryPage itemsForRender useMemo override: when location.state carries recentReleasedItemId AND cache still shows it 'reserved', render as 'available' for one mount — defeats snapshot race that overwrote oiy's optimistic patch"
  - "Counts memo + ItemGrid items prop both consume itemsForRender — no visual contradiction between strip/chip counts and grid tiles"
affects: [reservation lifecycle UX, post-release race elimination, RegistryPage mount hydration, ItemReservePage release-success flow]

tech-stack:
  added: []
  patterns:
    - "navigate(path, { state }) for one-shot cross-page hints — naturally scoped to a single navigation, no global mutation, no cleanup logic, readable in destination via useLocation"
    - "Read location.state at TOP of component body (NOT in useEffect) — must be visible to FIRST render to suppress flash window"
    - "Optional 2nd-arg options bag on shared hook for opt-in behavior — keeps existing 1-arg call sites compiling and behaving identically"
    - "Local render-shape override via useMemo (NOT modifying the shared query hook) — preserves referential identity when no override applies, downstream React.memo optimizations stay intact"

key-files:
  created:
    - web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts
  modified:
    - web/src/pages/ItemReservePage.tsx
    - web/src/features/reservation/useActiveReservationHydration.ts
    - web/src/pages/RegistryPage.tsx
    - web/src/pages/__tests__/ItemReservePage.test.tsx
    - web/src/features/registry/__tests__/RegistryPage.test.tsx

key-decisions:
  - "Use navigate state (not Context/sessionStorage/useRef) for one-shot semantics — refresh clears it, back-nav without state clears it, no cleanup needed"
  - "Read location.state at top of component body (NOT in useEffect) — effect tick of delay would miss the first render and leave a flash window"
  - "Override is local memo on RegistryPage, not in useItemsQuery — keeps query hook untouched and shareable; override returns referential identity when no-op"
  - "Counts memo consumes itemsForRender (same source as ItemGrid) — prevents 'progress shows 1 reserved while grid shows 0 reserved' contradiction"
  - "TDD: 6 RED tests committed first (test commit f90da85) then GREEN fix (c0aef4c) — clean signal of behavior change"
  - "K-21 uses probe-destination pattern (not react-router mock) — preserves all 196 pre-existing tests that observe real route transitions via <div data-testid='registry-page' />"

patterns-established:
  - "Cross-page race suppression via navigate state: when navigating from A to B as a side-effect of a backend write, pass identifiers of the just-mutated entities so B can suppress stale echoes during its first-render hydration window"
  - "Optional options bag on shared hooks: hooks consumed by multiple pages can grow opt-in behavior without forking — additive surface, backward compatible"
  - "Local memo override pattern: when one page needs a render-shape transform that other consumers don't, do it downstream of the shared query hook, not inside it"

requirements-completed: [QUICK-260518-j5j]

duration: ~25min
completed: 2026-05-18
---

# quick-260518-j5j: Fix release-from-ItemReservePage race — pass navigation state to suppress hydration + items snapshot for one mount

**Pass `{ recentReleasedReservationId, recentReleasedItemId }` via navigate state on release-success so RegistryPage's first render can ignore the just-released reservation in hydration AND override the just-released item's render-shape to 'available' — eliminates the post-release banner + tile flash that oiy alone couldn't fully suppress.**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-05-18T~10:38:00Z (worktree fetch)
- **Completed:** 2026-05-18T11:03:22Z
- **Tasks:** 2 (TDD: RED + GREEN)
- **Files modified:** 6 (3 source + 3 test; 1 of the test files is new)

## Accomplishments

- **Hydration race eliminated.** useActiveReservationHydration now accepts an optional `{ ignoreReservationId }` 2nd arg; when the backend's hydration callable resolves with the just-released reservation (composite-index lag on `status==='active'`), the hook sets status to `'empty'` and skips `set()` — preventing the shared ActiveReservationContext from being re-seeded with a stale reservation. This is the DOMINANT race (the one that disappears with Web Inspector open because the dev-tools slowdown lets the index settle before the response).
- **Snapshot race neutralized.** RegistryPage's new `itemsForRender` useMemo overrides the just-released item's render shape to `status='available'` (clearing reservedBy/reservedAt/expiresAt) when location.state carries `recentReleasedItemId` AND the cache still shows it as `'reserved'`. This shields against the case where `useItemsQuery`'s new onSnapshot listener fires from Firestore client-cache with stale `'reserved'` data and overwrites oiy's optimistic patch.
- **Counts memo aligned with grid.** Both progress strip / filter chips and the grid tiles consume `itemsForRender`, so the strip can never briefly read "1 reserved" while the grid shows zero reserved items.
- **D-06 invariant preserved (and strengthened).** The override CLEARS `reservedBy` (an email), so the new behavior cannot leak any reserver identity.
- **6 new tests, all passing.** K-21 (ItemReservePage navigate-state shape), R-NEW-01/02/03 (RegistryPage override + ignoreReservationId pass-through + non-regression), H-NEW-01/02 (hydration hook ignore branch + non-regression).

## Task Commits

1. **Task 1 (RED): 6 failing tests** — `f90da85` (test)
2. **Task 2 (GREEN): 3 source files + ts-expect-error cleanup** — `c0aef4c` (fix)

## Files Created/Modified

**Created:**
- `web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts` — Tests H-NEW-01/02 for the hook's new `ignoreReservationId` branch. Mocks firebase/functions httpsCallable + useAuth + useGuestIdentity + useActiveReservation.

**Modified (source):**
- `web/src/pages/ItemReservePage.tsx` — Release-success useEffect: navigate call gains `{ state: { recentReleasedReservationId: active!.reservationId, recentReleasedItemId: itemId } }`; `active` added to deps; comment updated to reference j5j alongside existing oiy reference.
- `web/src/features/reservation/useActiveReservationHydration.ts` — New optional 2nd arg `options?: { ignoreReservationId?: string }`; new conditional inside `.then()` triggers `setStatus("empty") + return` when ignored ID matches; deps array adds `options?.ignoreReservationId` (primitive, safe); JSDoc extended.
- `web/src/pages/RegistryPage.tsx` — Imports `useLocation`; reads `location.state` once at top of component body; passes `{ ignoreReservationId: recentReleasedReservationId }` to hydration; new `itemsForRender` useMemo (override release-target only when cache still shows 'reserved'); counts memo + ItemGrid items prop + truthy guard now consume `itemsForRender`.

**Modified (tests):**
- `web/src/pages/__tests__/ItemReservePage.test.tsx` — Adds `useLocation` to existing react-router import; appends K-21 with a probe-destination pattern (no file-wide react-router mock — preserves 33 existing tests).
- `web/src/features/registry/__tests__/RegistryPage.test.tsx` — Adds `hydrationMock` + file-top `vi.mock('../../reservation/useActiveReservationHydration', ...)`; adds `beforeEach` inside describe with `mockClear()`; appends R-NEW-01 (data-status='available' under state), R-NEW-02 (data-status='reserved' without state — non-regression), R-NEW-03 (hydration called with `(id, { ignoreReservationId })`).

## Decisions Made

- **Use navigate state (not Context, sessionStorage, or useRef).** Naturally scoped to a single navigation; refresh / nav-away-and-back / new-tab all clear it; no cleanup logic; type-safe and observable in tests via `initialEntries: [{ pathname, state }]`.
- **Read `location.state` at top of component body, NOT inside a useEffect.** An effect tick of delay would let the FIRST render observe stale data before the override engages — the flash window would remain. React Router guarantees `useLocation()` returns a stable value during render.
- **`itemsForRender` is a local memo on RegistryPage, not a modification to `useItemsQuery`.** Keeps the shared query hook untouched and shareable; preserves referential identity for downstream `React.memo` consumers when no override applies; returns the original `itemsQ.data` reference unchanged in the no-op path.
- **`counts` memo input changed from `itemsQ.data` to `itemsForRender`.** Same source as `ItemGrid` — eliminates visual contradiction during the override window.
- **K-21 uses probe-destination pattern, not a file-wide `react-router` mock.** Several existing tests (e.g. K-20) observe the destination route element via real router transitions; a file-wide `useNavigate` mock would no-op those navigations and break the existing 33 tests in `ItemReservePage.test.tsx`. The probe captures `location.state` into a `data-state` attribute on the destination — zero regression risk.
- **`@ts-expect-error` directives in the new H-NEW tests during RED → removed in GREEN.** Clean RED→GREEN type-signal transition: directive REMOVES the error in RED, CAUSES an "Unused @ts-expect-error" error in GREEN, prompting the removal.

## Deviations from Plan

None — plan executed exactly as written.

Minor clarification: the plan's `<verify>` section expected "3 test failures" in RED but the per-task breakdown listed 4 (K-21 + R-NEW-01 + R-NEW-03 + H-NEW-01). I confirmed 4 RED failures, which matches the per-task expectations precisely (the "3" was an undercount in the verify summary that omitted H-NEW-01). No action needed; treated as documentation typo, not a deviation.

## Issues Encountered

None.

## Test Counts

| Stage | Total | Passing | Failing | Files |
| --- | --- | --- | --- | --- |
| Pre-j5j (baseline before RED) | 196 | 196 | 0 | 27 |
| After Task 1 (RED) | 202 | 198 | 4 | 28 |
| After Task 2 (GREEN) | 202 | 202 | 0 | 28 |

RED failures (each fixed by the corresponding GREEN edit):

- `K-21` — navigate call shape (fixed by ItemReservePage.tsx edit 1)
- `R-NEW-01` — data-status='available' under state (fixed by RegistryPage.tsx `itemsForRender` memo)
- `R-NEW-03` — hydration call args (fixed by RegistryPage.tsx 2-arg pass-through + hook signature)
- `H-NEW-01` — status='empty' + set() not called (fixed by useActiveReservationHydration.ts new conditional)

R-NEW-02 and H-NEW-02 passed in both RED and GREEN — they are non-regression baselines for the unchanged code paths.

## Verification

All commands run from `/Users/victorpop/ai-projects/gift-registry/web`:

```
npm run typecheck                                              # PASS (tsc --noEmit, zero errors)
npx vitest run src/pages/__tests__/ItemReservePage.test.tsx    # 34/34 PASS
npx vitest run src/features/registry/__tests__/RegistryPage.test.tsx   # 7/7 PASS
npx vitest run src/features/reservation/__tests__/useActiveReservationHydration.test.ts   # 2/2 PASS
npm test -- --run                                              # 202/202 PASS across 28 files
```

Scope-guard diffs:

```
git diff --stat HEAD~2..HEAD               # 6 files (3 sources + 3 tests)
git diff HEAD~2..HEAD -- functions/        # empty
git diff HEAD~2..HEAD -- app/              # empty
git diff HEAD~2..HEAD -- web/public/locales/   # empty
git diff HEAD~2..HEAD -- web/src/features/reservation/useActiveReservation.ts \
                        web/src/features/reservation/useReleaseReservation.ts \
                        web/src/features/reservation/useReservationForItem.ts \
                        web/src/features/registry/useItemsQuery.ts \
                        web/src/features/reservation/StickyReserveBanner.tsx \
                        web/src/features/registry/ItemCard.tsx \
                        web/src/features/registry/ItemGrid.tsx   # empty
```

## Files NOT Touched (Scope-Guard List)

- `web/src/features/reservation/useReservationForItem.ts`
- `web/src/features/reservation/useActiveReservation.ts`
- `web/src/features/reservation/useReleaseReservation.ts`
- `web/src/features/registry/useItemsQuery.ts`
- `web/src/features/reservation/StickyReserveBanner.tsx`
- `web/src/features/registry/ItemCard.tsx`
- `web/src/features/registry/ItemGrid.tsx`
- `app/` (Android — entire tree)
- `functions/` (backend — entire tree)
- `web/public/locales/`, `web/src/i18n/en.json`, `web/src/i18n/ro.json` (no i18n changes)

## D-06 Invariant Preserved

- The `itemsForRender` override CLEARS `reservedBy` (an email) when it applies. The override can NEVER cause reserver identity to leak into render — strengthens D-06 rather than weakening it.
- No new render path renders reserver/giver name/email.

## No-State Path Behavior

When `location.state` is null/absent (page refresh, deep link, navigate without state, manual URL entry):
- `recentReleasedReservationId` and `recentReleasedItemId` are both `undefined`.
- `useActiveReservationHydration(id, { ignoreReservationId: undefined })` — the new conditional `options?.ignoreReservationId === r.data.active.reservationId` is always false (strict equality with a string ID is never true for `undefined`), so the existing populated branch (`set(r.data.active); setStatus("hydrated")`) fires normally.
- `itemsForRender` returns `itemsQ.data` unchanged (referential identity preserved).
- Counts and ItemGrid see the same `itemsQ.data` they would have pre-j5j.

Pre-j5j behavior is preserved exactly in every non-release flow.

## Forward Compatibility

- The hydration hook's new 2nd arg is optional — future call sites can opt in to `ignoreReservationId` without disruption to existing 1-arg consumers. There are currently zero other call sites besides RegistryPage.
- The navigate-state shape `{ recentReleasedReservationId, recentReleasedItemId }` is read once on RegistryPage mount and is forward-compatible with adding additional one-shot post-mutation hints (e.g. `recentReleasedAt` for telemetry) without breaking existing consumers — RegistryPage's destructure ignores unknown keys.

## Self-Check: PASSED

- Created: `web/src/features/reservation/__tests__/useActiveReservationHydration.test.ts` — FOUND.
- Modified: `web/src/pages/ItemReservePage.tsx`, `web/src/features/reservation/useActiveReservationHydration.ts`, `web/src/pages/RegistryPage.tsx`, `web/src/pages/__tests__/ItemReservePage.test.tsx`, `web/src/features/registry/__tests__/RegistryPage.test.tsx` — all FOUND.
- Commit `f90da85` (RED test) — FOUND in `git log`.
- Commit `c0aef4c` (GREEN fix) — FOUND in `git log`.
- Full suite 202/202 — VERIFIED.
- tsc --noEmit clean — VERIFIED.
- Scope-guard diffs empty for all out-of-scope paths — VERIFIED.

---

*Phase: quick/260518-j5j-fix-release-from-itemreservepage-race-pa*
*Completed: 2026-05-18*
