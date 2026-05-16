---
phase: quick-260516-iux
plan: 01
subsystem: web/reservation
tags: [bug-fix, web, reservation, hydration, ux]
dependency-graph:
  requires:
    - quick-260513-k37 (ItemReservePage browse-state branches 6/7/8)
    - quick-260513-g9g (useReservationForItem hook + per-item route)
    - quick-260513-hon (navigate-on-status-flip prev-status gate)
  provides:
    - Anon-no-identity reaches a browse branch instead of infinite spinner
    - Stale-expired-on-mount falls through to item.status browse branches (NOT expired UI, NOT reserved-by-me detail)
    - In-session expiration (countdown→0 while page open) still renders expired UI
  affects:
    - web/src/features/reservation/useReservationForItem.ts
    - web/src/pages/ItemReservePage.tsx
tech-stack:
  added: []
  patterns:
    - "Ref-based session gate (sawNonExpiredRef) that distinguishes mount-time stale state from in-session transitions"
    - "Derived `effectiveActive` value used only by render-branch decisions; useEffects observe real `active`"
    - "Stable anonymous-key (`__anon__|registryId|itemId`) for lastKeyRef in identity-flip-safe deduplication"
key-files:
  created: []
  modified:
    - web/src/features/reservation/useReservationForItem.ts (Bug A: anon-no-identity short-circuit → status='empty')
    - web/src/pages/ItemReservePage.tsx (Bug B: sawNonExpiredRef + effectiveActive gating branches 4/5; JSDoc state-machine block)
    - web/src/features/reservation/__tests__/useReservationForItem.test.tsx (U-02 rewritten, U-08 added)
    - web/src/pages/__tests__/ItemReservePage.test.tsx (K-08/K-09/K-10/K-11 added; P-05 rewritten for in-session-expiration semantics)
decisions:
  - "Use a per-mount `sawNonExpiredRef` rather than a one-shot countdown comparison: lets us distinguish stale-expired-on-mount (no prior non-expired observation in this mount) from in-session expiration (countdown ticked from positive to 0 while the page was open) without coupling render decisions to wall-clock arithmetic."
  - "Gate BOTH branch 4 (expired) AND branch 5 (reserved-by-me detail) via a derived `effectiveActive` value rather than gating only branch 4: branch 5 fires on any truthy `active` regardless of countdown, so without gating it a stale-active row would still render a misleading 00:00 + Release CTA UI for someone with no mental model of having reserved the item. Expanded scope vs the original plan and called out explicitly."
  - "Keep useEffects observing the real `active` (NOT effectiveActive): navigate-on-status-flip must still trigger when a legitimate reservation transitions out of 'reserved'; the sawNonExpiredRef tracker must observe real-`active`+countdown to be the gate's input rather than its output. Only the render switches use the gated value."
  - "Use a stable `__anon__|registryId|itemId` lastKeyRef key for the truly-anonymous short-circuit: the key shape is distinct from the signed-in/guest key shape (`registryId|itemId|uid|email`), so a mid-render sign-in flips the key and re-triggers the fetch normally (verified by U-08)."
  - "Rewrite P-05 (was the only existing test that documented the now-bugged behaviour): the original P-05 asserted that a stale-expired-on-mount active reservation rendered the expired UI. That is exactly the bug we are fixing. P-05 now uses the same fake-timers in-session-expiration pattern as K-09 to keep the 'expired UI when countdown hits 0' contract documented; K-09 remains the dedicated regression guard."
metrics:
  duration: 7m
  completed-date: 2026-05-16
---

# Quick Task 260516-iux: Fix ItemReservePage rendering "Your time ran out" + infinite loading for anonymous viewers — k37 follow-up Summary

## One-liner

Two-bug fix: (A) `useReservationForItem` truly-anonymous short-circuit now settles `status='empty'` so ItemReservePage's loading check releases for not-signed-in viewers; (B) ItemReservePage uses a per-mount `sawNonExpiredRef` + derived `effectiveActive` to short-circuit BOTH the expired branch (4) AND the reserved-by-me detail branch (5) when an active reservation arrives already-expired, falling through to the k37 browse branches; in-session expiration (countdown→0 while page open) still fires the expired UI.

## Root Causes

**Bug A — useReservationForItem.ts (line 52 before fix):**
The effect's `if (!user && !identity) return` short-circuited before any `setStatus(...)` call. Result: `status` stayed `'idle'` forever for any not-signed-in visitor with no stored guest identity. ItemReservePage's loading guard `lookupStatus === 'idle' || lookupStatus === 'loading'` therefore rendered the loading spinner indefinitely — these visitors never reached the new k37 BROWSE_* branches and saw a permanent loading state on `/registry/:id/item/:itemId`.

**Bug B — ItemReservePage.tsx (branches 4 and 5 before fix):**
When the page mounted with an `active` reservation whose `expiresAtMs` was already in the past (e.g. user reopened the tab after the emulator restart killed the auto-release setTimeout from quick-260510-pdp, or any other legacy stale-active row), branch 4 (`if (active && countdown?.expired)`) rendered "Your time ran out". Even gating branch 4 alone wouldn't suffice: branch 5 (`if (active)`) matched on any truthy `active` regardless of countdown — yielding a 00:00 countdown + Release CTA + "I completed the purchase" CTA for someone who had no mental model of having reserved anything. Both UIs revealed "you have a reservation" to a viewer who was effectively a stranger to that reservation.

## Fixes

**Bug A fix (`web/src/features/reservation/useReservationForItem.ts`):**
Replaced the early-return short-circuit with a block that sets a stable `__anon__|registryId|itemId` key in `lastKeyRef` and calls `setStatus('empty')` + `setActive(null)`. The anon-key shape is distinct from the signed-in/guest key (`registryId|itemId|uid|email`), so a mid-render sign-in (auth flip) generates a different key and re-triggers the fetch normally — verified by U-08.

**Bug B fix (`web/src/pages/ItemReservePage.tsx`):**
Three changes:
1. Added `sawNonExpiredRef = useRef(false)` alongside the other refs.
2. Added a tracking `useEffect` that flips the ref to `true` the first time we observe `active && countdown && !countdown.expired` during this mount. This effect MUST observe the real `active` (not the gated value) so the ref tracks reality.
3. Derived `effectiveActive = (active && countdown?.expired && !sawNonExpiredRef.current) ? null : active` just before the state-branch section. Branches 4 and 5 now read `effectiveActive`; branch 5 passes `active: effectiveActive` to `renderReservedByMeDetail` (TypeScript narrows correctly via the `if (effectiveActive)` check).

The state-machine JSDoc block at the top of `ItemReservePage.tsx` was rewritten to document:
- The new state priority list (branches 4 and 5 read `effectiveActive`).
- The `effectiveActive` derivation formula.
- The sawNonExpiredRef gate semantics (stale-on-mount vs in-session expiration).
- The critical fact that useEffects continue to use real `active` so they observe legitimate reservation lifecycle transitions.
- The Bug B scenario (emulator restart / Cloud Tasks fired before user returned) and the expected fall-through to `BROWSE_RESERVED_BY_OTHER`.

## Why useEffects still observe real `active` (not `effectiveActive`)

The render-branch decisions and the side-effect observations have different requirements:

| Concern | Reads | Why |
|---|---|---|
| Branches 4/5 render decisions | `effectiveActive` | Must skip stale-expired-on-mount to avoid misleading UI |
| `sawNonExpiredRef` tracking effect | real `active` + `countdown` | Must observe reality to be the gate's INPUT, not its output (otherwise the gate would feed back into itself and never flip true) |
| Navigate-on-status-flip useEffect (lines 131-142) | real `active` | Must still trigger when a legitimate reservation transitions out of 'reserved' (confirm in another tab, Cloud Tasks release). Gating this would break the navigate-back-on-confirm flow. |
| Release-success useEffect | `releaseStatus` only | Doesn't touch `active`. |
| Reserve-mutation onSuccess | shared `useActiveReservation` context | Seeds new reservation into shared context; unrelated to current page's gated view. |

## Tests

| Spec | Test | Pre-fix | Post-fix |
|---|---|---|---|
| U-02 | truly-anonymous (no user, no identity) settles to status='empty' (NOT idle) | RED (rewritten) | GREEN |
| U-08 (new) | identity-flip stability: anon → signed-in re-triggers fetch | RED | GREEN |
| K-08 (new) | stale-expired-on-mount + item.status='reserved' → BROWSE_RESERVED_BY_OTHER (NOT expired, NOT detail) | RED | GREEN |
| K-09 (new) | REGRESSION: in-session countdown→0 STILL renders expired branch | GREEN | GREEN |
| K-10 (new) | stale-expired-on-mount + item.status='available' → BROWSE_AVAILABLE | RED | GREEN |
| K-11 (new) | stale-expired-on-mount + item.status='purchased' → BROWSE_PURCHASED | RED | GREEN |
| P-05 (rewritten) | expired UI fires when countdown ticks to 0 in-session | (the old form documented the bug we fixed) | GREEN (now mirrors K-09 pattern) |

**Test count:** Baseline (post-merge of k37/h36/i6l/hon/j8a) was 162 passing tests in the affected suites; final is **167 passing** (162 + U-08 + K-08 + K-09 + K-10 + K-11). All 27 test files pass.

```
Test Files  27 passed (27)
     Tests  167 passed (167)
```

## TypeScript & Build

```
$ npm --prefix web run typecheck
> tsc --noEmit
(zero errors)

$ npm --prefix web run build
✓ 1823 modules transformed.
✓ built in 2.27s
```

## Deviations from Plan

### Rewritten (not just appended) test: P-05

**Found during:** Task 2 GREEN — the full ItemReservePage suite went RED on the existing P-05 test after the Bug B fix.

**Issue:** P-05 constructed `expiresAtMs: Date.now() - 1000` (already 1 second in the past at mount), called `renderPage()`, and asserted `item-reserve-expired` was visible. That is precisely the stale-expired-on-mount scenario the Bug B fix is designed to short-circuit — i.e. the test documented and locked in the bug we are fixing.

**Fix:** Rewrote P-05 to use the same `vi.useFakeTimers()` + `vi.advanceTimersByTime()` + `rerenderSame()` pattern as the newly-added K-09. P-05 now mounts with `expiresAtMs: Date.now() + 2_000` (non-expired), advances time past expiry, forces a re-render, and asserts the expired branch. The "expired UI when countdown reaches 0" contract is preserved, now correctly testing the in-session expiration path. K-09 remains the dedicated regression guard with a more comprehensive assertion shape; P-05 is kept for continuity with the existing P-* test numbering.

**Files modified:** `web/src/pages/__tests__/ItemReservePage.test.tsx` (P-05 body rewritten; structure and assertions match K-09).

**Commit:** rolled into `5ef542b` (Task 2 GREEN commit).

**Classification:** Rule 1 — fixing a test that asserted buggy behaviour the production fix correctly removes. Without this update, Task 2 cannot reach the "all tests green" success criterion.

### Worktree state recovery — merged main before starting Task 1

**Found during:** Task 1 baseline check.

**Issue:** Worktree HEAD was at `683a122` but the plan's prerequisites (k37 browse-state branches, ItemReservePage state-machine numbered 1-9, K-01..K-07 tests, useCreateReservation in ItemReservePage) live in commits `6e2a2cb..5ec14f5` (k37) which were on main but not yet in this worktree branch. The plan was written against the post-k37 codebase.

**Fix:** Stashed in-progress test edits, ran `git merge main --no-edit` to bring in the k37 + related commits (6 commits, ~1036 line changes touching ItemCard.tsx, ItemReservePage.tsx, i18n, tests, SUMMARY), restored the stash, then proceeded normally. No conflicts. After merge, baseline was 163 tests (161 passing + 2 newly-RED U-02/U-08 from the in-progress stash), matching the plan's stated "162 prior tests" within rounding.

**Classification:** Rule 3 — fixing a blocking environmental issue (missing prerequisite commits) so the plan can execute.

### Installed npm dependencies + copied .env.local from parent worktree

**Found during:** First test run attempt.

**Issue:** Worktree `web/` directory had no `node_modules/` and no `.env.local` (only `.env.example`). Vitest could not run; even after installing, the test setup imports from `src/firebase.ts` which calls `getAuth()` and requires `VITE_FIREBASE_API_KEY` at module evaluation time — this happens before any per-file vi.mock() takes effect.

**Fix:** Ran `npm install` in `web/`. Copied `.env.local` from the sibling main worktree (`/Users/victorpop/ai-projects/gift-registry/web/.env.local`) into this worktree's `web/` directory. After this, all 27 test files collected and 161 baseline tests passed.

**Classification:** Rule 3 — environment setup needed for plan execution. The copied `.env.local` is gitignored so this does not pollute the commit.

## Known Stubs

None.

## Human-verify outstanding

The plan documents a manual browser walkthrough (incognito flow, emulator-restart stale-reservation reproduction, in-session countdown regression) to be performed by a human after this plan executes. Not executed automatically — the plan's automated verify (vitest, tsc, build) is complete and green.

## Self-Check: PASSED

- web/src/features/reservation/useReservationForItem.ts contains `__anon__\|` anon-key pattern: FOUND
- web/src/pages/ItemReservePage.tsx contains `sawNonExpiredRef` and `effectiveActive`: FOUND
- web/src/features/reservation/__tests__/useReservationForItem.test.tsx contains `U-08`: FOUND
- web/src/pages/__tests__/ItemReservePage.test.tsx contains `K-08` through `K-11`: FOUND
- Commit `836442b` (test RED): FOUND
- Commit `5ef542b` (fix GREEN): FOUND
- All 167/167 tests green, tsc --noEmit clean, npm run build clean.
