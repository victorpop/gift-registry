---
phase: quick-260516-ku3
plan: 01
subsystem: web/reservation
tags: [web, reservation, sticky-banner, bug-fix, tdd]
type: tdd
wave: 1
depends_on: []
requires:
  - web/src/features/reservation/useReleaseReservation.ts (provides setStatus('pending') reset signal at line 33 of release())
  - web/src/features/reservation/useActiveReservation.ts (shared context whose clear() is invoked from the success effect)
  - web/src/features/reservation/useActiveReservationHydration.ts (re-resolves shared context between sequential releases, bringing reservation A back into view after B is released)
provides:
  - Reliable sequential-release behaviour in StickyReserveBanner — toast + clear() fire on every release, not just the first one per mount
affects:
  - web/src/features/reservation/StickyReserveBanner.tsx
tech-stack:
  added: []
  patterns:
    - "Lifecycle-reset useEffect keyed on releaseStatus === 'pending' (analogous pattern to k4f/j8a context-clear effects but local to a guard-ref instead of shared context)"
    - "Source-order placement contract — declaration order of useEffect calls is load-bearing for React's effect firing sequence"
key-files:
  created: []
  modified:
    - web/src/features/reservation/StickyReserveBanner.tsx
    - web/src/features/reservation/__tests__/StickyReserveBanner.test.tsx
decisions:
  - "Used releaseStatus === 'pending' (not 'idle') as the reset signal: 'pending' is unconditionally set by useReleaseReservation.release() at the start of every attempt, giving a deterministic per-release edge. 'idle' is only the initial state of the hook and would never re-fire."
  - "Placed the new useEffect BEFORE the success/error effects in source order: React fires effects in declaration order, so even in the defensive case of a same-render observation, the reset runs first. In practice useState batching splits pending/success across renders so there is no single-render race — but declaration order makes the contract resilient to future refactors."
  - "Reset BOTH refs (success-toasted + error-toasted-for) inside the same effect: a repeated identical-message error AFTER an intervening success was previously silently dropped because releaseErrorToastedForRef was never cleared. The fix improves this case at zero extra cost while preserving within-attempt error-toast dedupe."
  - "Inline test cleanup (activeMock.active = null at end of new test) instead of extending beforeEach: keeps the diff scoped to ku3 and avoids touching shared setup that other tests do not need."
metrics:
  duration: "~2min"
  completed: "2026-05-16"
  tasks: 2
  files_modified: 2
  test_count_before: 170
  test_count_after: 171
---

# Quick 260516-ku3: Fix StickyReserveBanner Not Clearing on Second Release in Same Mount Summary

**One-liner:** New `useEffect` in `StickyReserveBanner.tsx` resets the success/error toast guard refs whenever `releaseStatus === 'pending'`, so sequential releases in the same banner mount re-fire `showToast` + `clear()` instead of silently no-op'ing.

## Root Cause

`web/src/features/reservation/StickyReserveBanner.tsx` (lines 50, 61-67 pre-fix) held two `useRef` guards to prevent duplicate toasts on re-render:

- `releaseSuccessToastedRef` — set to `true` after the first successful release; gates `showToast` AND `clear()`.
- `releaseErrorToastedForRef` — keyed by error message; gates the error-toast within a single attempt.

`releaseSuccessToastedRef` was set to `true` after the first successful release and **never reset**. The banner stays mounted across releases (unlike `ItemReservePage`, which navigates+unmounts on success), so on the second release the success effect's `!releaseSuccessToastedRef.current` guard is `false` and the entire success branch is skipped — no toast, no `clear()`, banner appears wedged until manual refresh.

**Reproduction (browser, signed-out user):**
1. Reserve item A → shared `useActiveReservation` holds A.
2. Reserve item B → shared `useActiveReservation` now holds B → banner shows B.
3. Click Release on banner → backend cancels B, toast fires, `clear()` empties context, banner unmounts.
4. `useActiveReservationHydration` re-resolves to A → banner re-renders (same React mount) with A.
5. Click Release on banner → backend cancels A, BUT toast does NOT fire AND `clear()` is NOT called → banner stays visible until manual refresh.

The same latent issue applied to `releaseErrorToastedForRef`: a repeated identical-message error after a success would also be silently dropped (now improved as a side-effect of the fix).

## Fix Shape

Single new `useEffect` inserted immediately BEFORE the existing release-success effect (line 61 pre-fix):

```typescript
// Reset toast/clear() guard refs whenever a new release attempt begins (ku3).
useEffect(() => {
  if (releaseStatus === 'pending') {
    releaseSuccessToastedRef.current = false
    releaseErrorToastedForRef.current = null
  }
}, [releaseStatus])
```

**Why `'pending'`:** `useReleaseReservation.release()` calls `setStatus('pending')` at line 33 unconditionally on every release attempt (verified by reading the hook). The transition idle → pending therefore fires exactly once per release attempt, regardless of whether the previous attempt succeeded or failed — a deterministic per-release reset edge.

**Why source-order placement (before success/error effects):** React fires effects in declaration order during the commit phase. In the standard flow, `releaseStatus` moves idle → pending → success across two renders (useState batching guarantees this), so the reset effect runs in render N's commit and the success effect runs in render N+1's commit — no single-render race exists today. BUT declaration order is preserved as a defensive contract: if any future change ever caused both effects to observe a transition in the same render, the reset runs first so the success effect's guard is in the correct state.

## Why ItemReservePage Was Not Affected

`web/src/pages/ItemReservePage.tsx` has the same release-success ref-pattern (`releaseSuccessToastedRef`) but its success effect calls `navigate(...)` which unmounts the page. The guard ref state never accumulates across releases because the next release is in a fresh mount. The plan explicitly noted this and required NO changes to `ItemReservePage.tsx` — verified untouched in this fix.

## Why Error-Toast Dedupe Is Preserved AND Improved

- **Within a single release attempt** (repeated re-renders with the same `releaseError` message): the message-keyed `releaseErrorToastedForRef.current !== releaseError` guard still prevents duplicate toasts — unchanged behaviour.
- **Across release attempts** (repeated identical-message error after an intervening 'pending'): previously silently dropped because the ref retained the old message. Now the new reset effect clears the ref to `null` on each new attempt, so a repeated identical-message error fires correctly.

This is a strict improvement at zero cost — there is no scenario where the previous behaviour was desirable.

## Files Modified

- `web/src/features/reservation/StickyReserveBanner.tsx` — +20 lines (one new useEffect + multi-line inline comment).
- `web/src/features/reservation/__tests__/StickyReserveBanner.test.tsx` — +69/-1 lines (one new test + `act` import addition).

**Zero touches to:**
- `web/src/pages/ItemReservePage.tsx`
- `web/src/features/reservation/useReleaseReservation.ts`
- `web/src/features/reservation/useActiveReservation.ts`
- `web/src/features/reservation/useActiveReservationHydration.ts`
- Any backend file, i18n file, or new file.

## Commits

| Commit  | Type | Description |
| ------- | ---- | ----------- |
| 495945d | test | Add failing spec for sequential-release in StickyReserveBanner |
| 6f50c03 | fix  | Reset toast/clear guards on every new release so sequential releases re-fire toast + clear() |

## Test Counts

- **Before:** 170 web tests GREEN.
- **After:** 171 web tests GREEN (170 pre-existing + 1 new sequential-release spec).
- **New spec:** `clears toast/clear() guards on each new release so a second release in the same mount fires toast + clear() again (ku3)` — drives release lifecycle (idle → pending → success → re-hydrate → idle → pending → success), asserts `showToast` called 2x and `activeMock.clear` called 2x.

## Verification

- `npm --prefix web test -- --run` → 171/171 GREEN.
- `npm --prefix web run typecheck` → zero errors.
- `git diff --name-only` (against `683a122` baseline before this PLAN) → only the two expected files.
- `grep -n "releaseStatus === 'pending'" web/src/features/reservation/StickyReserveBanner.tsx` → 3 occurrences: 1 new (reset useEffect, line 74) + 2 pre-existing button props (disabled/aria-busy, lines 158-159). Only the useEffect occurrence is new.
- TDD discipline: Task 1 commit (495945d) was confirmed RED (`expected "spy" to be called 2 times, but got 1`) before Task 2 commit turned it GREEN.

## Deviations from Plan

**One minor deviation, no plan intent change:**

- Plan's commit step used `node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit ...`. The gsd-tools commit wrapper returned `{committed: false, reason: "nothing_to_commit"}` despite a clean `git diff` showing the staged change (likely a worktree/path mismatch in the wrapper for the current shell). Fell back to direct `git add` + `git commit -m "..."` with the same conventional-commit message body. Result is identical — two atomic commits with proper `test(quick-260516-ku3-01)` and `fix(quick-260516-ku3-01)` prefixes. No tracking impact: commits are visible to `git log` and STATE/ROADMAP updates run against the same git history.

No other deviations. Plan executed exactly as written.

## Human-Verify Browser Walkthrough

**Status:** Outstanding (PLAN did not include a checkpoint task; informational manual verification deferred to the user per the plan's `verification` block).

The plan's manual walkthrough (start emulator + web dev server, reserve A, reserve B, release B → expect toast + banner re-appears with A, release A → expect toast + banner gone) can be performed at the user's discretion. Automated coverage (171/171 GREEN including the new sequential-release spec) covers the exact reproduction in test form: two releases in the same mount, asserting `showToast` and `clear()` are each called twice. The fix is structurally minimal (one `useEffect` keyed on the deterministic `setStatus('pending')` signal from `useReleaseReservation`), so divergence between unit-test and browser behaviour is unlikely.

## Self-Check: PASSED

- `web/src/features/reservation/StickyReserveBanner.tsx` — FOUND (modified, committed in 6f50c03)
- `web/src/features/reservation/__tests__/StickyReserveBanner.test.tsx` — FOUND (modified, committed in 495945d)
- Commit 495945d — FOUND in `git log`
- Commit 6f50c03 — FOUND in `git log`
- 171/171 web tests GREEN
- TypeScript `npm run typecheck` clean
- Only 2 files in `git diff --name-only` since baseline
