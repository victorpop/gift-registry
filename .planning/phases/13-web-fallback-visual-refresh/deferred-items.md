# Phase 13 — Deferred Items (out-of-scope discoveries)

Items discovered during Phase 13 plan execution that are NOT in the current plan's scope and have been deferred for follow-up. Logged per GSD scope-boundary rules.

## Plan 13-06 (Auth screen restyle)

### 1. Pre-existing test failures in `ItemCard.test.tsx` (4 tests) and `RegistryPage.test.tsx` (1 test)

**Status:** Pre-existing — predates Plan 13-06 commits.

**Failing tests:**
- `ItemCard > renders title, price, and image alt=title for available item`
- `ItemCard > shows Available badge with surface-variant bg for available status`
- `ItemCard > shows Reserved badge with bg-primary for reserved status`
- `ItemCard > shows Purchased badge with bg-surface-on for purchased status`
- `RegistryPage > renders RegistryHeader + ItemGrid when registry and items load`

**Root cause:** Plan 13-04 restyled ItemCard with the new GiftMaison `Pill` / `PulseDot` / `MonoCaption` atoms (commit b8efae4). The existing tests still query for the legacy Phase 5 token classes (`bg-primary`, `bg-surface-variant`, `bg-surface-on`) which no longer exist on the restyled component, plus the `RegistryPage` test fails on a related cascade.

**Why not auto-fixed in Plan 13-06:** Out of scope. Plan 13-06 only touches auth files (`AuthScreen.tsx`, `AuthModal.tsx`, `EditorialPhoto.tsx`, `GuestSkipCard.tsx`), `App.tsx`, and the two public assets. Test re-baselining for ItemCard/RegistryPage was not part of Plan 13-06's brief.

**Where it should land:** Plan 13-07 (regression sweep / re-baseline), as Plan 13-06's `<verification>` section explicitly notes: *"Plan 07 may restyle GuestIdentityModal as a polish item ... and re-baseline snapshot tests / DOM-shape tests."*

**Workaround in the meantime:** None needed for the auth screen ship — the failing tests are in unrelated registry surface code; the auth surface (4 suites, 21 tests) is fully green.

---

### 2. Stashed WIP edits to `ItemCard.tsx` + `RegistryPage.tsx` (already-attempted barrel-bypass workaround)

**Status:** Reverted in this plan; not part of Plan 13-06 scope.

**What was found:** A `git stash pop` earlier in the working tree had introduced direct atom imports (`from '../../components/giftmaison/Pill'`) in `ItemCard.tsx` to bypass the barrel-→TopNav-→useAuth-→firebase chain that breaks jsdom tests. This is the same workaround pattern that would resolve the failing tests in item 1 above.

**Why reverted:** Out of Plan 13-06's scope. The fix belongs in Plan 13-07 alongside the test re-baselining so the two changes ship as one coherent commit.

**Note for Plan 13-07:** the barrel-bypass pattern has precedent and is safe — same approach used in `AuthModal.test.tsx` mocking strategy when Plan 13-06 chose the test-side mock route instead.
