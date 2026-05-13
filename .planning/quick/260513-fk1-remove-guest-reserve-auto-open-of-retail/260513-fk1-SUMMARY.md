---
phase: quick-260513-fk1
plan: 01
subsystem: web-reservation
tags: [reservation, ux, window-open, i18n, tdd]
dependency_graph:
  requires: []
  provides: [WEB-04-FIX]
  affects: [useCreateReservation, StickyReserveBanner (user-initiated CTA unaffected)]
tech_stack:
  added: []
  patterns: [TDD red-green, pure-data mutation hook]
key_files:
  created: []
  modified:
    - web/src/features/reservation/useCreateReservation.ts
    - web/src/features/reservation/__tests__/useCreateReservation.test.ts
    - web/src/i18n/en.json
    - web/src/i18n/ro.json
    - web/i18n/en.json
    - web/i18n/ro.json
    - .planning/phases/05-web-fallback/05-UI-SPEC.md
decisions:
  - "Deleted window.open side-effect entirely from useCreateReservation onSuccess; retailer navigation is now 100% user-initiated via StickyReserveBanner anchor"
  - "Updated toast copy in both en/ro and both i18n locations (web/src/i18n/ and web/i18n/) to drop the auto-redirect implication"
  - "UI-SPEC row 269 updated with new copy and inline WEB-D-07 revision note for historical traceability"
metrics:
  duration: ~8 minutes
  completed_date: 2026-05-13
  tasks_completed: 1
  tasks_pending_verify: 1
  files_changed: 7
---

# Phase quick-260513-fk1 Plan 01: Remove Guest Reserve Auto-Open of Retailer URL Summary

## One-liner

Removed the `window.open` side-effect from `useCreateReservation` so reservation success never auto-opens a retailer tab — the existing `StickyReserveBanner` "Continue to retailer" anchor is now the sole, user-initiated entry point to the affiliate URL.

## Change Summary

The `useCreateReservation` mutation hook previously called `window.open(data.affiliateUrl, '_blank', 'noopener,noreferrer')` inside its internal `onSuccess` callback (WEB-D-07 + WEB-04 decision, lines 40–43). This caused a new browser tab to open immediately on every successful reservation — before the user could choose to proceed. The behaviour was especially jarring for guests and on the auto-reserve-from-deep-link path.

This fix deletes those three lines entirely. The hook is now a pure data mutator: its internal `onSuccess` does nothing except delegate to `options.onSuccess?.(data, variables)`. All callers (`ReserveButton`, `RegistryPage` auto-reserve) are untouched and continue to call `setActive({ affiliateUrl: data.affiliateUrl, ... })`, which feeds `StickyReserveBanner` — which already renders a proper `<a href={active.affiliateUrl} target="_blank" rel="noopener noreferrer">` anchor. The retailer opens only when the user clicks that CTA.

## TDD Flow

- **RED:** Inverted the existing `'opens affiliateUrl in a new tab on success (WEB-D-07)'` test to assert `expect(windowOpenSpy).not.toHaveBeenCalled()`. Confirmed the new assertion FAILED against the unmodified source (window.open was called once with the affiliate URL).
- **GREEN:** Deleted the `window.open` block + comment from `useCreateReservation.ts`. All 6 tests in the file passed.

## Grep Verification

```
grep -n "window.open" web/src/features/reservation/useCreateReservation.ts
→ ZERO MATCHES (PASS)

grep -rn "WEB-D-07" web/src/features/reservation/
→ ZERO MATCHES (PASS — only occurrence was in the deleted comment)

grep -rn "Redirecting to retailer|Redirecționare către magazin" web/src/i18n/ web/i18n/
→ ZERO MATCHES (PASS)

grep -n "href={active.affiliateUrl}" web/src/features/reservation/StickyReserveBanner.tsx
→ Line 145 — FOUND (PASS — manual CTA is unchanged)
```

## Test Counts

Before: 116 tests, 1 failing (the inverted test was failing against old source).
After: 116 tests, all passing. Test count is identical — one test was repurposed (renamed + assertion inverted), one was renamed for clarity; no tests added or removed.

TypeScript: `cd web && npx tsc --noEmit` exits 0.

## Scope of WEB-D-07 References

`05-UI-SPEC.md` row 269 was updated with the new toast copy and an inline revision note. The following files reference `WEB-D-07` in a purely historical/decision-record context and were intentionally left unchanged:

- `.planning/phases/05-web-fallback/05-CONTEXT.md` — records the original WEB-D-07 decision; leaving it as historical record is correct.
- `.planning/phases/05-web-fallback/05-RESEARCH.md` — if present, same rationale.
- `.planning/phases/05-web-fallback/05-VERIFICATION.md` — if present, same rationale.

No source code outside `web/src/features/reservation/useCreateReservation.ts` referenced `window.open` for this flow.

## i18n Changes

| File | Old | New |
|------|-----|-----|
| web/src/i18n/en.json | "Gift reserved! Redirecting to retailer…" | "Gift reserved! Continue to retailer when you're ready." |
| web/src/i18n/ro.json | "Cadou rezervat! Redirecționare către magazin…" | "Cadou rezervat! Continuă către magazin când ești gata." |
| web/i18n/en.json | (same as src) | (same as src) |
| web/i18n/ro.json | (same as src) | (same as src) |

## Deviations from Plan

None — plan executed exactly as written. The constraint to update both `web/src/i18n/` and `web/i18n/` added 2 extra file changes (7 total vs. the plan's stated 5), which is expected per the task constraints.

## Known Stubs

None. All data flows are live; the `affiliateUrl` field is still returned by the callable and flows through `setActive` to `StickyReserveBanner`.

## Future Work Note

The `reservation.success` toast key uses a single string for all reservation paths (signed-in, guest, auto-reserve-from-deep-link). Depending on human-verify feedback, a future task may consider splitting this into a guest vs. signed-in variant (e.g., the signed-in path might warrant a shorter toast since the user already understands the flow). This is out of scope for this fix.

## Task 2: Human-Verify Status

**PENDING** — checkpoint:human-verify not yet cleared. See verification scenarios in the PLAN.md Task 2 block:
- Scenario A: Signed-in user, click Reserve from item card
- Scenario B: Guest user, fill GuestIdentityModal, submit
- Scenario C: Auto-reserve-from-deep-link (signed-in, guest with stored identity, guest without)
- Scenario D: Toast copy regression check (Romanian)
- Negative check: StickyReserveBanner CTA still opens new tab on explicit click

## Commits

| Task | Commit | Message |
|------|--------|---------|
| Task 1 | ffd59bd | fix(quick-260513-fk1-01): remove auto-open of retailer URL on successful reservation |

## Self-Check: PASSED

All modified files confirmed on disk. Commit `ffd59bd` confirmed in git log. SUMMARY.md exists at expected path.
