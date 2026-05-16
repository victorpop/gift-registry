---
phase: quick-260516-iux
plan: 01
type: tdd
wave: 1
depends_on: []
files_modified:
  - web/src/features/reservation/useReservationForItem.ts
  - web/src/pages/ItemReservePage.tsx
  - web/src/features/reservation/__tests__/useReservationForItem.test.tsx
  - web/src/pages/__tests__/ItemReservePage.test.tsx
autonomous: true
requirements:
  - IUX-A   # Anonymous-no-identity: useReservationForItem must terminate in status='empty', not 'idle'
  - IUX-B   # Stale-expired-on-mount: ItemReservePage must NOT render "Your time ran out" NOR the reserved-by-me detail for reservations that were already expired before page load — both branches (4 and 5) short-circuit and flow falls through to the item.status browse branches
must_haves:
  truths:
    - "Truly-anonymous viewer (no user, no guestIdentity) on /registry/:id/item/:itemId no longer sees the infinite loading state — page renders one of the three browse branches (available / reserved-by-other / purchased)."
    - "Anonymous viewer with stored guestIdentity who lands on a page whose backing reservation is ALREADY expired (e.g. emulator restarted, Cloud Tasks fired before they returned) no longer sees 'Your time ran out' — page also does NOT render the reserved-by-me detail (no misleading 00:00 countdown + Release CTA for someone who has no mental model of having reserved). Flow falls through to the item.status browse branch (typically reserved-by-other, since the stale active row keeps item.status === 'reserved')."
    - "In-session expiration (countdown ticks from a positive value down to 0 while the page is open) STILL renders 'Your time ran out' — this is the legitimate expired UX and must be preserved."
    - "The shared active-reservation context (set by useActiveReservation) is still observed correctly by useEffects (release-success, navigate-on-status-flip) and by the reserve-mutation onSuccess seeding — only the render-branch decisions for branches 4 and 5 use the gated `effectiveActive` value."
    - "All 162 existing web tests (P-01..P-11, K-01..K-07, U-01..U-07, plus the rest of the suite) remain green."
    - "The state-machine JSDoc block at ItemReservePage.tsx:27-37 accurately describes the new sawNonExpiredRef gate AND the effectiveActive derivation so a future reader doesn't think branches 4 or 5 fire unconditionally on a stale active row."
  artifacts:
    - path: "web/src/features/reservation/useReservationForItem.ts"
      provides: "Anonymous-no-identity branch now sets status='empty' (was: early return leaving 'idle')"
      contains: "__anon__"
    - path: "web/src/pages/ItemReservePage.tsx"
      provides: "sawNonExpiredRef gate + derived effectiveActive that short-circuits BOTH branch 4 (expired) AND branch 5 (reserved-by-me detail) on stale-expired-on-mount + updated JSDoc state-machine block"
      contains: "sawNonExpiredRef"
    - path: "web/src/features/reservation/__tests__/useReservationForItem.test.tsx"
      provides: "U-02 rewritten to assert status='empty' (was: buggy 'idle'); U-08 added for identity-flip stability"
      contains: "U-08"
    - path: "web/src/pages/__tests__/ItemReservePage.test.tsx"
      provides: "K-08 stale-expired-on-mount + item.status=='reserved' → reserved-by-other browse branch (NOT expired, NOT reserved-by-me detail); K-09 in-session expiration → expired branch (regression); K-10 stale-expired + item.status=='available' → available browse branch; K-11 stale-expired + item.status=='purchased' → purchased browse branch"
      contains: "K-08"
  key_links:
    - from: "ItemReservePage.tsx loading branch (line ~189)"
      to: "useReservationForItem status='empty'"
      via: "lookupStatus === 'idle' || lookupStatus === 'loading' check"
      pattern: "lookupStatus === 'idle'"
    - from: "ItemReservePage.tsx expired branch (line ~235) AND reserved-by-me detail branch (line ~262)"
      to: "effectiveActive derived value"
      via: "Both branches now read effectiveActive instead of active. effectiveActive = (active && countdown?.expired && !sawNonExpiredRef.current) ? null : active. The real `active` is still used by the navigate-on-status-flip useEffect (line 131-142) and the sawNonExpiredRef tracking useEffect."
      pattern: "effectiveActive"
    - from: "ItemReservePage.tsx sawNonExpiredRef tracking useEffect"
      to: "Real `active` (NOT effectiveActive)"
      via: "Effect must observe the real shared-context active so the ref flips correctly when countdown transitions from non-expired → expired during the session"
      pattern: "sawNonExpiredRef\\.current = true"
    - from: "useReservationForItem anonymous-no-identity path"
      to: "lastKeyRef stable __anon__ key"
      via: "key = `__anon__|${registryId}|${itemId}` set before setStatus('empty') so subsequent re-renders deduplicate, but identity change (user signs in) generates a different key and re-triggers the fetch"
      pattern: "__anon__\\|"
---

<objective>
Fix two bugs from quick-260513-k37's browse-state work that together blocked truly-anonymous and stale-reservation viewers from ever reaching the new BROWSE_AVAILABLE / BROWSE_RESERVED_BY_OTHER / BROWSE_PURCHASED branches.

**Bug A (useReservationForItem.ts):** When `authReady && !user && !identity`, the effect's `if (!user && !identity) return` short-circuits BEFORE `setStatus(...)` is called. Result: `status` stays `'idle'` forever. ItemReservePage's loading check (`lookupStatus === 'idle' || lookupStatus === 'loading'`) therefore renders the loading spinner indefinitely for any not-signed-in visitor with no stored guest identity.

**Bug B (ItemReservePage.tsx) — expanded scope:** When the page mounts with an `active` reservation whose `expiresAtMs` is already in the past (e.g. user reopens the tab after emulator restart wiped the auto-release setTimeout per quick-260510-pdp, or any other legacy stale-active row), the original code renders the expired branch ("Your time ran out"). Even with a sawNonExpiredRef gate on branch 4 alone, branch 5 (reserved-by-me detail) would still match because `active` is truthy regardless of countdown — yielding a 00:00 countdown + Release CTA + "I completed the purchase" CTA for someone who has no mental model of having reserved anything. Both states reveal "you have a reservation" to a viewer who is effectively a stranger to that reservation. Correct behaviour: stale-on-mount should short-circuit BOTH branches 4 AND 5 and fall through to the `item.status === 'reserved' | 'available' | 'purchased'` branches.

**Constraints (re-stated from quick task spec):**
- Client-side only. NO backend / localStorage cleanup / emulator config changes.
- All existing P-01..P-11 and K-01..K-07 and U-01..U-07 tests stay green.
- D-06 unchanged (no reserver/giver name surfaced).
- No new i18n strings — browse branches reuse k37 keys.
- In-session expiration (countdown runs to 0 while the page is open) MUST still render the expired state — that's the legitimate UX.
- Shared `useActiveReservation` context is unchanged — only the render-branch decisions for branches 4 and 5 use the gated value. Useffects that observe `active` for navigation/tracking must keep using the real `active`.

Purpose: Restore the k37 browse-state work for not-signed-in users (the primary audience for the web fallback per PROJECT.md — "guest access must work without account creation"), and ensure a stale active row never produces a misleading reserved-by-me experience.

Output:
- Patched `useReservationForItem.ts` (anonymous-no-identity → status='empty' with stable `__anon__` key)
- Patched `ItemReservePage.tsx` (sawNonExpiredRef + derived `effectiveActive` short-circuits BOTH branches 4 and 5; navigation/tracking effects keep using real `active`; updated state-machine JSDoc block)
- Updated U-02 + new U-08 in `useReservationForItem.test.tsx`
- New K-08, K-09, K-10, K-11 in `ItemReservePage.test.tsx`
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md
@.planning/quick/260513-k37-improve-web-product-tiles-add-product-de/260513-k37-SUMMARY.md
@web/src/features/reservation/useReservationForItem.ts
@web/src/pages/ItemReservePage.tsx
@web/src/features/reservation/useCountdown.ts
@web/src/features/reservation/useActiveReservation.ts
@web/src/features/reservation/__tests__/useReservationForItem.test.tsx
@web/src/pages/__tests__/ItemReservePage.test.tsx

<interfaces>
<!-- Extracted from the codebase so the executor does NOT need to re-explore. -->

From web/src/features/reservation/useReservationForItem.ts (current shape — Bug A here):
```typescript
type HydrationStatus = "idle" | "loading" | "hydrated" | "empty" | "error"
interface HydrationResponse { active: ActiveReservation | null }
export function useReservationForItem(
  registryId: string | undefined,
  itemId: string | undefined,
): { status: HydrationStatus; active: ActiveReservation | null }

// CURRENT BUGGY GUARD (line 52):
//   if (!user && !identity) return            // ← leaves status='idle' forever
// FIXED GUARD (new):
//   if (!user && !identity) {
//     const anonKey = `__anon__|${registryId}|${itemId}`
//     if (lastKeyRef.current === anonKey) return
//     lastKeyRef.current = anonKey
//     setStatus("empty")
//     setActive(null)
//     return
//   }
```

From web/src/features/reservation/useActiveReservation.ts:
```typescript
export interface ActiveReservation {
  reservationId: string
  itemId: string
  itemName: string
  affiliateUrl: string
  merchantDomain: string | null
  expiresAtMs: number
}
```

From web/src/features/reservation/useCountdown.ts:
```typescript
export interface Countdown {
  minutes: number; seconds: number; totalSeconds: number; expired: boolean
}
// Returns null when expiresAtMs is null/undefined.
// Returns {..., expired: true} immediately when expiresAtMs <= now.
export function useCountdown(expiresAtMs: number | null | undefined): Countdown | null
```

From web/src/pages/ItemReservePage.tsx (state-machine — currently lines 27-37, MUST be updated):
```
State priority (top → bottom — first match wins):
  1. !id || !itemId                              → null (router safety)
  2. items undefined OR lookupStatus idle/loading → loading
  3. !item                                       → item-not-found
  4. active && countdown.expired                 → expired       ← Bug B: fires on stale-on-mount
  5. active                                      → reserved-by-me detail  ← Bug B: also fires on stale-on-mount (active truthy regardless of countdown)
  6. !active && item.status === 'available'      → BROWSE_AVAILABLE
  7. !active && item.status === 'reserved'       → BROWSE_RESERVED_BY_OTHER
  8. !active && item.status === 'purchased'      → BROWSE_PURCHASED
  9. (fallback, unreachable)                     → not-yours panel
```

Existing useEffect at lines 131-142 (navigate-back-on-status-flip):
```typescript
const currentItemStatus = itemsQ.data?.find(i => i.id === itemId)?.status
useEffect(() => {
  const prev = prevStatusRef.current
  prevStatusRef.current = currentItemStatus

  if (!active) return            // ← MUST keep `active` (real), NOT effectiveActive
  if (itemStatusNavigatedRef.current) return
  if (prev !== 'reserved') return
  if (currentItemStatus !== 'purchased' && currentItemStatus !== 'available') return

  itemStatusNavigatedRef.current = true
  navigate(`/registry/${id}`)
}, [currentItemStatus, active, id, navigate])
```
This effect must continue to observe the REAL `active` so a legitimate reservation transitioning out of 'reserved' (purchase confirmed in another tab, or release fired by Cloud Tasks) still triggers navigation. The render branches use effectiveActive; the effects use active.

Test infrastructure already in `ItemReservePage.test.tsx`:
- `renderPage(registryId, itemId, extraRoutes)` — sets up MemoryRouter + QueryClient
- `ItemReservePageWithForceUpdate` wrapper exposes `rerenderSame()` for transition tests
- `makeItem(overrides)` — defaults to status='reserved', reservedBy='user@example.com'
- `ACTIVE_RES` constant with expiresAtMs = Date.now() + 30 * 60 * 1000
- Mocks: useItemsQuery, useReservationForItem, useAuth, useGuestIdentity, useReleaseReservation, useConfirmPurchase, useActiveReservation, ConfirmPurchaseBanner, HowTimerWorks, useCreateReservation
- beforeEach already calls `mockReset()` / `mockReturnValue(...)` defaults — new tests inherit those then override as needed
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: RED — add failing tests covering both bugs, the in-session-expiration regression, and all three stale-expired fall-through cases</name>
  <files>
    web/src/features/reservation/__tests__/useReservationForItem.test.tsx
    web/src/pages/__tests__/ItemReservePage.test.tsx
  </files>
  <behavior>
    Five new/updated spec assertions — all the new ones MUST be RED before any production code is touched. K-09 (regression guard) must be GREEN immediately as it already represents working behaviour.

    **U-02 update (in useReservationForItem.test.tsx):** The existing U-02 test currently asserts the buggy behaviour (`status` stays `'idle'`). Rewrite it to assert the fixed behaviour:
      - Setup: `useAuth → { user: null, isReady: true }`, `useGuestIdentity → { identity: null }`, callable NOT mocked to resolve (it should never be called).
      - Assert (after a 20ms tick): `result.current.status === 'empty'`, `result.current.active === null`, `callableFn` not called.
      - Rename the test to: `U-02: truly-anonymous (no user, no identity) settles to status='empty' (NOT idle) without invoking callable`
      - This test MUST currently fail because the existing code returns early before `setStatus('empty')`.

    **U-08 (new, in useReservationForItem.test.tsx):** Identity-flip stability — sign-in mid-render re-triggers the fetch.
      - Setup: start with `useAuth → { user: null, isReady: true }`, `useGuestIdentity → { identity: null }`. Render the hook.
      - Wait for `status === 'empty'` (the new fixed state). Assert `callableFn` not called.
      - Then flip the auth mock: `useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com' }, isReady: true })`, mock callable to resolve with `{ data: { active: ACTIVE_RES } }`.
      - Rerender the hook. Assert `status` transitions to `'hydrated'` and `callableFn` was called exactly once with `{ registryId, itemId }`.
      - Purpose: verifies the `__anon__|registryId|itemId` key correctly differs from the signed-in `${registryId}|${itemId}|${uid}|${email}` key so identity changes don't get deduplicated.

    **K-08 (new, in ItemReservePage.test.tsx) — stale-expired-on-mount + item.status=='reserved' falls through to reserved-by-other:**
      - Setup: `useAuth → { user: null, isReady: true }`, `useGuestIdentity → { identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' } }`.
      - `useReservationForItem.mockReturnValue({ status: 'hydrated', active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 } })` — an active reservation whose expiry is already 1 minute in the past.
      - `useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })` — items snapshot still shows reserved (stale row).
      - Render. Assert ALL of:
        - `queryByTestId('item-reserve-expired')` is null (branch 4 short-circuited)
        - `queryByTestId('item-reserve-detail')` is null (branch 5 ALSO short-circuited — this is the expanded scope vs the original plan)
        - `getByTestId('item-reserve-reserved-by-other')` is present (fall-through to branch 7)
        - `queryByText(/time ran out/i)` is null (i18n-safe assert no expired copy)

    **K-09 (new, in ItemReservePage.test.tsx) — REGRESSION GUARD for in-session expiration:** Countdown ticking to 0 while page is open STILL renders expired.
      - Setup: default mocks (signed-in u1, active reservation, items=[reserved makeItem]).
      - Render with `expiresAtMs = Date.now() + 2000` (2 seconds in future).
      - Initial state: assert `getByTestId('item-reserve-detail')` is present (NOT expired). At this point sawNonExpiredRef flips to true via its tracking effect.
      - Advance time by 3 seconds and force a re-render so useCountdown's interval ticks and the page re-renders with countdown.expired=true. Use vitest's fake timers (`vi.useFakeTimers()` in this test only) — `vi.advanceTimersByTime(3000)` wrapped in `act(() => { ... })`, then `rerenderSame()`.
      - Assert: `queryByTestId('item-reserve-detail')` is null, `getByTestId('item-reserve-expired')` is present, and `getByText(/time ran out/i)` is present.
      - This is the critical regression test: confirms the `effectiveActive` derivation correctly distinguishes "saw non-expired then expired" (sawNonExpiredRef=true → effectiveActive=active → branch 4 fires) from "expired-on-mount" (sawNonExpiredRef=false → effectiveActive=null → branches 4 and 5 both skip).
      - Restore real timers via `vi.useRealTimers()` in a try/finally so a failed assertion doesn't leak fake timers into later tests.

    **K-10 (new, in ItemReservePage.test.tsx) — stale-expired-on-mount + item.status=='available' falls through to BROWSE_AVAILABLE:**
      - Rare combo: the auto-release ran on the item (item.status flipped to 'available') but the active reservation row in the context is stale. Could happen if the items snapshot updates first and the active context lags. Forced via test fixture here.
      - Setup: same as K-08 but `useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })`.
      - `useReservationForItem.mockReturnValue({ status: 'hydrated', active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 } })`.
      - Render. Assert:
        - `queryByTestId('item-reserve-expired')` is null
        - `queryByTestId('item-reserve-detail')` is null
        - `getByTestId('item-reserve-available')` is present (branch 6 wins)
      - Note: this scenario currently triggers the navigate-back-on-status-flip effect ONLY when prevStatusRef.current === 'reserved' — on initial mount prevStatusRef is undefined, so no navigation happens. The browse branch renders. This test confirms both the gate AND that the navigation effect doesn't fire spuriously on mount.

    **K-11 (new, in ItemReservePage.test.tsx) — stale-expired-on-mount + item.status=='purchased' falls through to BROWSE_PURCHASED:**
      - Setup: same as K-08 but `useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'purchased' })] })`.
      - `useReservationForItem.mockReturnValue({ status: 'hydrated', active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 } })`.
      - Render. Assert:
        - `queryByTestId('item-reserve-expired')` is null
        - `queryByTestId('item-reserve-detail')` is null
        - `getByTestId('item-reserve-purchased')` is present (branch 8 wins)
      - Same note as K-10 re: navigation effect.

    Existing K-06 (anonymous-no-identity falls back to `/registry/:id?autoReserveItemId=:itemId`) MUST still pass — when there's no active reservation AND item.status is 'available', the Reserve CTA's `handleReserveClick` does the round-trip navigation. K-08/K-10/K-11 cover a different scenario (anonymous-WITH-identity + stale-expired-active).
  </behavior>
  <action>
    1. Open `web/src/features/reservation/__tests__/useReservationForItem.test.tsx`:
       - Locate U-02 — rewrite the assertion as described above. To make Task 1 strictly RED-first, run the test BEFORE rewriting to confirm the current assertion (`status === 'idle'`) still passes against the existing code. Then commit the rewrite (which makes it fail).
       - Append new test U-08 to the `describe('useReservationForItem', ...)` block.
       - Keep the `ACTIVE_RES` constant and existing mock structure as-is — these new tests integrate with the same `beforeEach` setup.

    2. Open `web/src/pages/__tests__/ItemReservePage.test.tsx`:
       - Append K-08, K-09, K-10, K-11 inside the existing `describe('ItemReservePage', ...)` block (after K-07).
       - For K-09 use `vi.useFakeTimers()` inside the test body. Wrap setup of `Date.now()`-based `expiresAtMs` AFTER calling `vi.useFakeTimers()` so the timer baseline is deterministic. Restore real timers via `vi.useRealTimers()` in a try/finally so a failed assertion doesn't leak fake timers into later tests.
       - For K-08, K-10, K-11, no fake timers needed — the expired-in-the-past `expiresAtMs` makes useCountdown return `expired: true` on first render synchronously.

    3. Run the test suite and confirm RED:
       ```
       cd web && npm run test -- --run useReservationForItem.test.tsx ItemReservePage.test.tsx
       ```
       Expected failures BEFORE Task 2:
       - U-02 fails (rewrite asserts `status === 'empty'` but production still returns early leaving 'idle')
       - U-08 fails (the precondition wait for status='empty' never resolves — same root cause as U-02)
       - K-08 fails (current code renders item-reserve-expired OR item-reserve-detail for stale-active, not item-reserve-reserved-by-other)
       - K-10 fails (current code renders item-reserve-expired or item-reserve-detail, not item-reserve-available)
       - K-11 fails (current code renders item-reserve-expired or item-reserve-detail, not item-reserve-purchased)
       - K-09 PASSES already (in-session expiration works in the current code because the unguarded branch 4 fires whenever countdown.expired=true) — this is the regression guard for Task 2's fix.

    4. Run the FULL web suite once to confirm no other tests inadvertently broke during edits:
       ```
       cd web && npm run test -- --run
       ```
       Expected: 162 previously-passing tests still pass; 5 new failures (U-02, U-08, K-08, K-10, K-11). K-09 should already pass — if it doesn't, the test setup is wrong (fix the test, not the production code).

    5. Commit:
       ```
       node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "test(quick-260516-iux-01): add failing tests for anon-no-identity loading + stale-expired-on-mount (branches 4 and 5)" --files web/src/features/reservation/__tests__/useReservationForItem.test.tsx web/src/pages/__tests__/ItemReservePage.test.tsx
       ```
  </action>
  <verify>
    <automated>cd web && npm run test -- --run useReservationForItem.test.tsx ItemReservePage.test.tsx 2>&1 | tail -40</automated>
  </verify>
  <done>
    - U-02 rewritten to assert `status === 'empty'` (currently RED).
    - U-08 appended (currently RED — the precondition wait for status='empty' never resolves).
    - K-08 appended (currently RED — current code does not render item-reserve-reserved-by-other for stale-active+reserved).
    - K-09 appended (currently GREEN — regression guard for in-session expiration).
    - K-10 appended (currently RED — current code does not render item-reserve-available for stale-active+available).
    - K-11 appended (currently RED — current code does not render item-reserve-purchased for stale-active+purchased).
    - Full suite shows exactly 5 new failures, no other regressions.
    - Test file changes committed.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: GREEN — fix both bugs (anon hydration + derived effectiveActive gating branches 4 AND 5) and update the state-machine JSDoc block</name>
  <files>
    web/src/features/reservation/useReservationForItem.ts
    web/src/pages/ItemReservePage.tsx
  </files>
  <behavior>
    Production code must make U-02, U-08, K-08, K-10, K-11 transition RED → GREEN while keeping K-09 (in-session expiration) and ALL existing P-01..P-11 / K-01..K-07 / U-01..U-07 tests GREEN.

    **Bug A fix — useReservationForItem.ts (anonymous short-circuit leaves status='idle'):**
    Replace the line-52 short-circuit `if (!user && !identity) return` with:
    ```typescript
    if (!user && !identity) {
      // Truly anonymous: no user, no stored guest identity. We cannot
      // hydrate a reservation, but we MUST settle status to 'empty' so
      // ItemReservePage's loading check (lookupStatus === 'idle') releases.
      // The lastKeyRef with a stable `__anon__|registryId|itemId` key
      // prevents this branch from re-firing on every render BUT remains
      // distinct from any signed-in/guest key shape — so a mid-render
      // sign-in (user becomes non-null) generates a different key and
      // re-triggers the fetch normally.
      const anonKey = `__anon__|${registryId}|${itemId}`
      if (lastKeyRef.current === anonKey) return
      lastKeyRef.current = anonKey
      setStatus("empty")
      setActive(null)
      return
    }
    ```
    Also: the subsequent `if (!effectiveEmail) return` is effectively dead code (identity always has email per useGuestIdentity's localStorage shape). Leave it as defence-in-depth but optionally add a comment.

    **Bug B fix — ItemReservePage.tsx (expired-on-mount fires for stale reservations AND reserved-by-me detail also fires on stale):**

    Two-step gate: track whether we've ever observed a non-expired countdown this mount, then derive an `effectiveActive` value that is null when the active row is stale-on-mount. Branches 4 AND 5 read effectiveActive instead of active; ALL useEffects continue to use real `active`.

    1. Add a ref alongside the existing refs (after `prevStatusRef`, around line 102):
       ```typescript
       // sawNonExpiredRef: tracks whether we have EVER observed a non-expired
       // countdown for this active reservation during the current page mount.
       // - false on mount ⇒ if active+expired on first observation, the reservation
       //   was already stale before we arrived. We MUST short-circuit BOTH the
       //   expired branch (4) AND the reserved-by-me detail branch (5) — neither
       //   makes sense for a viewer who has no mental model of having reserved
       //   this item. Fall through to the item.status browse branches.
       // - true once observed non-expired ⇒ subsequent expiry (countdown ticking
       //   to 0 while user is on the page) is an IN-SESSION expiration → render
       //   the legitimate "Your time ran out" UI (branch 4 fires normally).
       // Resets per mount (useRef in component scope) — desired behaviour: a user
       // who navigates AWAY and BACK to a now-expired reservation should not see
       // the expired page NOR the reserved-by-me detail if it had already expired
       // before they returned.
       const sawNonExpiredRef = useRef(false)
       ```
    2. Add a useEffect that flips the ref to true the first time the REAL `active` is present and not expired. This effect MUST observe the real `active`, not effectiveActive (effectiveActive is the gated render value; the ref tracking is the gate's input).
       ```typescript
       useEffect(() => {
         if (active && countdown && !countdown.expired) {
           sawNonExpiredRef.current = true
         }
       }, [active, countdown])
       ```
    3. Derive `effectiveActive` near the top of the state-branch section (just before the `if (!id || !itemId)` guard at line 183, or just after item lookup if simpler). It is purely a derived value used only by the branch conditions:
       ```typescript
       // Gated `active` for render-branch decisions ONLY. When the page mounts
       // with an already-expired reservation (sawNonExpiredRef still false),
       // treat active as null so branches 4 and 5 both fall through. The real
       // `active` is still used by useEffects (release-success, navigate-on-
       // status-flip, sawNonExpiredRef tracking) and by the reserve-mutation
       // onSuccess. Only the render switches care about effectiveActive.
       const effectiveActive =
         active && countdown?.expired && !sawNonExpiredRef.current
           ? null
           : active
       ```
    4. Rewrite branches 4 and 5 to read `effectiveActive`:
       - Branch 4 (expired): `if (active && countdown?.expired)` → `if (effectiveActive && countdown?.expired)`
       - Branch 5 (reserved-by-me detail): `if (active) {` → `if (effectiveActive) {` — and update the call to `renderReservedByMeDetail({ id, item, active: effectiveActive, ... })` so the inner JSX receives a non-null active typed correctly.

       Note on branch-4 simplification: with `effectiveActive` defined as above, `effectiveActive && countdown?.expired` is exactly equivalent to `active && countdown?.expired && sawNonExpiredRef.current` (because effectiveActive is null whenever expired+not-seen-non-expired). Either form works — prefer the effectiveActive form for symmetry with branch 5.
    5. Do NOT modify any of:
       - The release-success useEffect (line ~105) — uses releaseStatus, not active.
       - The release-error useEffect (line ~115) — uses releaseStatus/releaseError.
       - The navigate-back-on-status-flip useEffect (lines 131-142) — its `if (!active) return` MUST keep `active` (real), not effectiveActive, so a legitimate reservation transitioning out of 'reserved' still triggers navigation.
       - The reserveMutation onSuccess seeding (line 74-85) — operates on shared useActiveReservation context independent of effectiveActive.
       - The handleReserveClick handler (line 146) — uses user/identity directly, no active dependency.
       - The inner JSX of branch 4 or `renderReservedByMeDetail`.
       - Browse branches 6/7/8, BrowseShell, ItemDetailHero, NotesBlock — unchanged.

    6. Update the JSDoc state-machine block at lines 27-37 to document both the gate and the effectiveActive derivation:
       ```
       State priority (top → bottom — first match wins; branches 4 and 5
       read `effectiveActive` instead of `active`):
         1. !id || !itemId                                       → null (router safety)
         2. items undefined OR lookupStatus idle/loading          → loading
         3. !item                                                → item-not-found
         4. effectiveActive && countdown.expired                  → expired (in-session only)
         5. effectiveActive                                      → reserved-by-me detail (happy path)
         6. !effectiveActive && item.status === 'available'      → BROWSE_AVAILABLE (k37)
         7. !effectiveActive && item.status === 'reserved'       → BROWSE_RESERVED_BY_OTHER (k37)
         8. !effectiveActive && item.status === 'purchased'      → BROWSE_PURCHASED (k37)
         9. (fallback, unreachable)                              → not-yours panel

       `effectiveActive` is derived as:
         effectiveActive = (active && countdown?.expired && !sawNonExpiredRef.current)
                           ? null
                           : active

       sawNonExpiredRef flips to true the first time we observe an active
       reservation whose countdown is not yet expired during this mount.
       This distinguishes legitimate in-session expiration (sawNonExpiredRef
       true → effectiveActive = active → branch 4 renders "Your time ran
       out") from stale-expired-on-mount (sawNonExpiredRef false →
       effectiveActive = null → branches 4 and 5 both skip → flow falls
       through to item.status browse branches).

       IMPORTANT: useEffects that observe the reservation lifecycle (the
       sawNonExpiredRef tracking effect AND the navigate-on-status-flip
       effect at line ~131) continue to use the real `active`, NOT
       effectiveActive. The reserve-mutation onSuccess also operates on
       the real shared useActiveReservation context. effectiveActive is
       purely a render-branch gate.

       Stale-expired-on-mount handling (quick-260516-iux Bug B): when the
       page loads with active.expiresAtMs already in the past (e.g. emulator
       restart killed the auto-release setTimeout per quick-260510-pdp, or
       any other legacy stale-active row), sawNonExpiredRef is false, so
       effectiveActive is null, so branches 4 and 5 are skipped. The viewer
       lands on the item.status browse branch — typically BROWSE_RESERVED_
       BY_OTHER because the stale row keeps item.status === 'reserved' until
       Cloud Tasks (or a manual refresh of items) flips it.
       ```

    Run tests incrementally:
    1. Fix useReservationForItem.ts → run useReservationForItem.test.tsx → confirm U-01 through U-08 all GREEN.
    2. Fix ItemReservePage.tsx (ref + effect + effectiveActive + branch 4/5 rewrites + JSDoc) → run ItemReservePage.test.tsx → confirm P-01..P-11 + K-01..K-11 all GREEN.
    3. Run full suite to confirm no broader regressions.
  </behavior>
  <action>
    1. Edit `web/src/features/reservation/useReservationForItem.ts`:
       - Replace the `if (!user && !identity) return` line with the multi-line block from Bug A fix above.

    2. Run useReservationForItem tests:
       ```
       cd web && npm run test -- --run useReservationForItem.test.tsx 2>&1 | tail -20
       ```
       Confirm all U-01..U-08 GREEN.

    3. Edit `web/src/pages/ItemReservePage.tsx`:
       - Add `sawNonExpiredRef` ref declaration alongside the other refs (after `prevStatusRef`, around line 102).
       - Add the sawNonExpiredRef tracking useEffect that flips it to true on first observed non-expired countdown (using real `active`).
       - Add the `effectiveActive` derivation in the state-branch section.
       - Rewrite branch 4 (line 235) to read `effectiveActive && countdown?.expired`.
       - Rewrite branch 5 (line 262) to read `if (effectiveActive)` and pass `active: effectiveActive` to `renderReservedByMeDetail`.
       - Update the JSDoc state-machine block at lines 27-37 per the spec above.
       - DO NOT touch the navigate-on-status-flip useEffect (lines 131-142). Confirm `if (!active) return` still uses `active` (real).

    4. Run ItemReservePage tests:
       ```
       cd web && npm run test -- --run ItemReservePage.test.tsx 2>&1 | tail -40
       ```
       Confirm all P-01..P-11 + K-01..K-11 GREEN. Common failure modes if RED:
       - K-09 fails: the `sawNonExpiredRef` effect isn't flipping the ref before the expired branch re-evaluates. Verify the effect dependency array includes `[active, countdown]` and that on the first non-expired render, the effect commits before the next render where countdown.expired flips to true.
       - K-08/K-10/K-11 fail: effectiveActive derivation is wrong, or branch 4/5 still reads `active`. Re-check that BOTH branches read effectiveActive.
       - Existing P-tests fail: likely renderReservedByMeDetail TypeScript complaint about active being nullable. The cast in the branch (`if (effectiveActive)` narrows to non-null) should satisfy TS — if not, pass `active: effectiveActive as ActiveReservation`.

    5. Run full web test suite:
       ```
       cd web && npm run test -- --run 2>&1 | tail -15
       ```
       Expected: all tests GREEN (was 162, now 167 with U-08, K-08, K-09, K-10, K-11).

    6. TypeScript check:
       ```
       cd web && npx tsc --noEmit 2>&1 | tail -10
       ```
       Expected: zero errors.

    7. Build check:
       ```
       cd web && npm run build 2>&1 | tail -10
       ```
       Expected: clean build.

    8. Commit:
       ```
       node "/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/bin/gsd-tools.cjs" commit "fix(quick-260516-iux-01): anon-no-identity hydration + effectiveActive gates branches 4 and 5 on stale-expired-on-mount" --files web/src/features/reservation/useReservationForItem.ts web/src/pages/ItemReservePage.tsx
       ```
  </action>
  <verify>
    <automated>cd web && npm run test -- --run 2>&1 | tail -15 && npx tsc --noEmit 2>&1 | tail -5</automated>
  </verify>
  <done>
    - useReservationForItem.ts: anonymous-no-identity branch sets status='empty' with stable `__anon__|registryId|itemId` lastKeyRef key.
    - ItemReservePage.tsx: sawNonExpiredRef ref + tracking useEffect (observes real `active`) + derived `effectiveActive` + branches 4 and 5 both read effectiveActive.
    - ItemReservePage.tsx: navigate-on-status-flip useEffect (lines 131-142) and reserveMutation onSuccess remain unchanged — both still operate on real `active` / shared useActiveReservation context.
    - ItemReservePage.tsx: JSDoc state-machine block at lines 27-37 updated to document both the effectiveActive derivation and the fact that useEffects use the real `active`.
    - All web tests GREEN (167/167: 162 pre-existing + U-08 + K-08 + K-09 + K-10 + K-11).
    - `tsc --noEmit` clean.
    - `npm run build` clean.
    - Fix committed.
  </done>
</task>

</tasks>

<verification>
**Test-suite verification (automated):**
```
cd web && npm run test -- --run
```
Must pass 167/167. Specifically:
- U-01..U-08 in useReservationForItem.test.tsx (U-02 rewritten, U-08 new)
- P-01..P-11 in ItemReservePage.test.tsx (unchanged)
- K-01..K-11 in ItemReservePage.test.tsx (K-08, K-09, K-10, K-11 new)
- All other 130+ web tests unchanged

**TypeScript check (automated):**
```
cd web && npx tsc --noEmit
```
Must be zero errors.

**Build check (automated):**
```
cd web && npm run build
```
Must be clean.

**Manual browser verification (deferred to human after PLAN executes):**
1. Start emulator + web dev server. Reproduce the original screenshot scenario:
   a. Sign in as a registry owner, create registry with 1 item.
   b. Open a NEW private/incognito window (no stored guestIdentity).
   c. Navigate to the registry's share URL → click an item card → reach `/registry/:id/item/:itemId`.
   d. Confirm: page renders the BROWSE_AVAILABLE branch (Reserve CTA + hero + notes), NOT the loading spinner.
2. Reproduce Bug B: as a guest with stored identity, create a reservation, restart the Firestore emulator (kills the auto-release setTimeout from quick-260510-pdp), wait until the reservation's expiresAtMs is in the past, refresh the page.
   - Expected: page renders the BROWSE_RESERVED_BY_OTHER browse branch (NOT "Your time ran out" AND NOT the reserved-by-me detail with 00:00 countdown + Release CTA). The viewer should see the standard "Reserved by someone else" view.
3. Regression: as a guest with stored identity, create a reservation with normal 30-min timer. Stay on the page. Wait for countdown to tick to 00:00.
   - Expected: page transitions to "Your time ran out" (in-session expiration still works — sawNonExpiredRef flipped to true while the countdown was positive).
4. Existing flows: confirm release-from-reserve-page still navigates back + clears sticky banner (quick-260513-j8a regression); confirm reserve-from-RegistryPage still navigates into ItemReservePage detail and shows the reserved-by-me detail; confirm not-yours items render the reserved-by-other browse branch; confirm confirm-purchase navigation-on-status-flip still works (uses real `active`, unchanged).
</verification>

<success_criteria>
- Both bugs fixed:
  - Anonymous-no-identity reaches a browse branch (not infinite loading).
  - Stale-expired-on-mount falls through to the item.status browse branches — neither the expired page NOR the reserved-by-me detail renders for stale-active rows.
- 5 new/updated tests committed in Task 1 (RED) and pass in Task 2 (GREEN): U-02 (rewritten), U-08, K-08, K-10, K-11. K-09 is the regression guard and is GREEN throughout.
- K-09 confirms in-session countdown-to-zero STILL renders the expired branch — the gate distinguishes stale-on-mount from legitimate in-session expiration.
- All 162 pre-existing web tests stay GREEN.
- `tsc --noEmit` clean.
- `npm run build` clean.
- State-machine JSDoc block at ItemReservePage.tsx:27-37 updated to describe the sawNonExpiredRef gate, the effectiveActive derivation, AND the fact that useEffects continue to use real `active`.
- Two atomic commits (one per task) with the gsd-tools commit signature.
- No backend / localStorage / emulator changes.
- No new i18n keys.
- No new files created (only edits to 4 existing files).
- Shared useActiveReservation context unchanged — only the render branches use the gated value.
</success_criteria>

<output>
After completion, create `.planning/quick/260516-iux-fix-itemreservepage-rendering-your-time-/260516-iux-SUMMARY.md` summarising:
- The two root causes (Bug A early-return; Bug B unguarded branches 4 AND 5).
- The two fixes (anon-key empty-state hydration; sawNonExpiredRef + derived effectiveActive gating branches 4 and 5).
- Why the navigation/tracking useEffects continue to use real `active` (must observe legitimate transitions even when render is gated).
- The state-machine JSDoc update.
- Test counts (was 162, now 167) and the spec IDs added/changed (U-02 rewritten, U-08 added, K-08/K-09/K-10/K-11 added).
- Confirmation of `tsc --noEmit` and `npm run build` clean.
- Any human-verify findings from the manual browser walkthrough (if executed before the SUMMARY is written; otherwise mark "Human-verify outstanding").
</output>
