---
phase: quick-260513-hon
plan: 01
type: tdd
wave: 1
depends_on: []
files_modified:
  - web/src/pages/ItemReservePage.tsx
  - web/src/pages/__tests__/ItemReservePage.test.tsx
autonomous: false
requirements:
  - HON-01  # Stale-on-mount items snapshot must NOT trigger navigate-back from ItemReservePage
  - HON-02  # Real 'reserved' -> 'available' transition MUST trigger navigate-back (release path)
  - HON-03  # Real 'reserved' -> 'purchased' transition MUST trigger navigate-back (confirm-purchase path)
must_haves:
  truths:
    - "Guest who just completed reservation lands on /registry/:id/item/:itemId with full reserve-detail UI — not bounced back to /registry/:id"
    - "Signed-in user reserving a second item lands on /registry/:id/item/:newItemId (URL reflects the new item) — not bounced back"
    - "Clicking 'I completed the purchase' still navigates back to /registry/:id (regression — confirm-purchase path keeps working)"
    - "Clicking 'Release reservation' still navigates back to /registry/:id (regression — release path keeps working)"
    - "The navigate-back effect requires OBSERVING item.status === 'reserved' before any transition can fire it"
  artifacts:
    - path: "web/src/pages/ItemReservePage.tsx"
      provides: "Transition-detector for item.status flip out of 'reserved'"
      contains: "prevStatusRef"
    - path: "web/src/pages/__tests__/ItemReservePage.test.tsx"
      provides: "3 new specs covering stale-on-mount, 'reserved'->'available', 'reserved'->'purchased' transitions"
      contains: "navigate-back"
  key_links:
    - from: "useEffect at ItemReservePage.tsx:90-99"
      to: "prevStatusRef (new) + itemStatusNavigatedRef (existing)"
      via: "two-ref pattern — prev tracks last observed status, navigated tracks one-shot fire"
      pattern: "prevStatusRef\\.current"
    - from: "Test specs"
      to: "navigate mock / route assertion"
      via: "rerender or controlled mock updates of itemsQueryMock.useItemsQuery between renders"
      pattern: "registry-page|getByTestId\\('item-reserve-detail'\\)"
---

<objective>
Fix the `ItemReservePage` navigate-back-on-status-flip effect that currently fires on initial render
when the `useItemsQuery` snapshot is stale (still shows `status === 'available'` from before the
just-completed reservation), causing the user to be bounced back to `/registry/:id` immediately after
auto-navigation from a successful reserve.

User-visible symptoms today:
  (a) Guest auto-reserve → lands on "This isn't your reservation" page (after a brief flash of the detail UI).
  (b) Reserving a second item → URL does not change to the new dedicated `/registry/:id/item/:itemId` page.

Root cause: the effect only inspects the CURRENT `currentItemStatus` value. A stale `'available'` on first
render is enough to satisfy the condition. There is no detection that the item was EVER seen as `'reserved'`,
so the effect cannot distinguish "stale snapshot before reservation propagates" from "real release/confirm
transition".

Fix: introduce a `prevStatusRef` so the effect only fires when it observes a real transition OUT of
`'reserved'` into `'available'` or `'purchased'`. Stale `'available'` on first render is recorded into the
ref but does NOT fire the navigate; subsequent `'available'` (e.g. after release) IS a transition from
`'reserved'` and DOES fire the navigate. Keep the existing one-shot `itemStatusNavigatedRef` guard.

This is a TDD plan: failing specs first (RED), then minimal code change to pass (GREEN). Single source-file
change + 3 new test specs.

Purpose: Restore the reserve-detail page experience after the recent regression introduced when the
status-flip effect was added without a transition guard. Without this fix, the dedicated-per-item reserve
page is effectively unreachable in practice — the page mounts and immediately unmounts itself.

Output: Patched `ItemReservePage.tsx` (transition-detector effect), 3 new specs in the existing test file,
`npm run test:run` green, `npx tsc --noEmit` exit 0.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@CLAUDE.md

@web/src/pages/ItemReservePage.tsx
@web/src/pages/__tests__/ItemReservePage.test.tsx
@web/src/lib/firestore-mapping.ts
@web/src/features/registry/useItemsQuery.ts

<interfaces>
<!-- Key types and contracts the executor needs. Extracted from the codebase. -->
<!-- The executor should use these directly — no codebase exploration needed. -->

From `web/src/lib/firestore-mapping.ts`:
```typescript
export type ItemStatus = 'available' | 'reserved' | 'purchased'

export interface Item {
  id: string
  title: string
  imageUrl: string | null
  price: number | null
  currency: string | null
  notes: string | null
  status: ItemStatus
  reservedBy: string | null
  reservedAt: Date | null
  expiresAt: Date | null
  affiliateUrl: string
  originalUrl: string
  merchantDomain: string | null
}
```

From `web/src/features/registry/useItemsQuery.ts`:
```typescript
// Returns a react-query result whose `.data` is `Item[] | undefined`.
export function useItemsQuery(registryId: string | undefined): UseQueryResult<Item[]>
```

Current (buggy) effect in `web/src/pages/ItemReservePage.tsx` (lines 85-99):
```typescript
const currentItemStatus = itemsQ.data?.find(i => i.id === itemId)?.status
useEffect(() => {
  if (
    active &&
    !itemStatusNavigatedRef.current &&
    (currentItemStatus === 'purchased' || currentItemStatus === 'available')
  ) {
    itemStatusNavigatedRef.current = true
    navigate(`/registry/${id}`)
  }
}, [currentItemStatus, active, id, navigate])
```

Existing one-shot guard ref already declared on line 62:
```typescript
const itemStatusNavigatedRef = useRef(false)
```

Existing helpers in the test file (`__tests__/ItemReservePage.test.tsx`):
- `makeItem(overrides)` — builds an `Item` with sensible defaults; pass `{ status: 'reserved' }` etc.
- `renderPage(registryId='reg1', itemId='it1', extraRoutes=[])` — mounts `<ItemReservePage>` in a memory
  router with a fallback route `/registry/:id → <div data-testid="registry-page" />`. Navigating back
  causes the `registry-page` testid to appear and `item-reserve-detail` to disappear.
- Mocks already wired: `itemsQueryMock.useItemsQuery`, `reservationForItemMock.useReservationForItem`,
  `authMock.useAuth`, `guestMock.useGuestIdentity`, plus `ConfirmPurchaseBanner` and `HowTimerWorks`.
- Top-level imports include `act` from `@testing-library/react`.
- `beforeEach` defaults to `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })` —
  note `makeItem()` defaults to `status: 'reserved'`. Tests that need a different initial status MUST
  override the mock BEFORE `renderPage()`.

Two-ref pattern (the fix sketch — reproduced here so it is in the executor's context):
```typescript
const prevStatusRef = useRef<ItemStatus | undefined>(undefined)
useEffect(() => {
  const prev = prevStatusRef.current
  prevStatusRef.current = currentItemStatus

  if (!active) return
  if (itemStatusNavigatedRef.current) return
  if (prev !== 'reserved') return
  if (currentItemStatus !== 'purchased' && currentItemStatus !== 'available') return

  itemStatusNavigatedRef.current = true
  navigate(`/registry/${id}`)
}, [currentItemStatus, active, id, navigate])
```

Import the type at the top of `ItemReservePage.tsx`:
```typescript
import type { ItemStatus } from '../lib/firestore-mapping'
```
(Verify the relative path. From `web/src/pages/ItemReservePage.tsx` the path to `firestore-mapping.ts`
in `web/src/lib/` is `../lib/firestore-mapping`. If the existing file already imports from that module
for another reason, reuse the same import statement.)
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add failing transition-detector specs, then fix the navigate-back effect</name>
  <files>
    web/src/pages/__tests__/ItemReservePage.test.tsx
    web/src/pages/ItemReservePage.tsx
  </files>
  <behavior>
    Three new specs, all in the existing `describe('ItemReservePage', ...)` block in
    `web/src/pages/__tests__/ItemReservePage.test.tsx`. Add them AFTER the existing P-07 spec.
    Use stable spec IDs P-08, P-09, P-10 so they read consistently with the existing ID scheme.

    P-08 (HON-01) — Stale-on-mount does NOT navigate back:
      Setup:
        - `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })`
          — i.e. the items snapshot still shows the OLD pre-reservation state (stale).
        - `reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })`
          — but the reservation lookup already sees the new reservation (this models the real race).
      Render: `renderPage()`.
      Assert (after a `waitFor` micro-tick to flush effects):
        - `screen.getByTestId('item-reserve-detail')` IS present.
        - `screen.queryByTestId('registry-page')` is NULL — the router did NOT redirect back.

    P-09 (HON-02) — Real 'reserved' -> 'available' transition DOES navigate back:
      Setup:
        - `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })`
        - `reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })`
      Render: `renderPage()`.
      Assert intermediate state: `getByTestId('item-reserve-detail')` is present, `queryByTestId('registry-page')` is null.
      Transition:
        - In an `act(() => { ... })` block, flip the mock:
          `itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })`
        - Then re-render the same component instance. Approach: refactor `renderPage` to return the
          `rerender` helper from `@testing-library/react` (already returned by `render`). The
          QueryClient and router are created INSIDE `renderPage`, then the rendered JSX tree is
          stored in a `tree` constant and `rerenderSame: () => result.rerender(tree)` is exposed on
          the returned object. The test then calls `rerenderSame()` after flipping the mock so React
          re-renders the SAME `ItemReservePage` instance (preserving `prevStatusRef`).
      Assert (after `waitFor`):
        - `screen.queryByTestId('item-reserve-detail')` is NULL (component unmounted by navigation).
        - `screen.getByTestId('registry-page')` IS present.

    P-10 (HON-03) — Real 'reserved' -> 'purchased' transition DOES navigate back:
      Identical structure to P-09 but the second snapshot uses `status: 'purchased'`.
      Assert post-transition: `registry-page` testid is rendered; `item-reserve-detail` is gone.

    Implementation contract for the source file (`web/src/pages/ItemReservePage.tsx`):
      - Introduce `const prevStatusRef = useRef<ItemStatus | undefined>(undefined)` adjacent to the
        existing `itemStatusNavigatedRef` declaration (around line 62).
      - Add `import type { ItemStatus } from '../lib/firestore-mapping'` to the imports (verify path).
      - Replace the body of the existing `useEffect` at lines 90-99 with the transition-detector
        sketch shown in `<interfaces>`. CRITICAL: `prevStatusRef.current = currentItemStatus` MUST run
        BEFORE the early returns, so the ref is always updated on every effect run (including the
        initial render when `prev === undefined`). The effect's dependency list MUST remain
        `[currentItemStatus, active, id, navigate]`.
      - Do NOT touch the release-success effect (lines 65-71), the release-error effect (lines 74-83),
        the loading branch, the not-found branch, the not-yours branch, the expired branch, the
        SaveYourSpotModal effect (lines 108-120), or any rendering JSX. The ONLY change to the source
        file is: add one import, add one ref, rewrite the body of ONE existing useEffect.
      - Do NOT modify ANY other file. Protected list (do not touch): `useItemsQuery`,
        `useReservationForItem`, `useReleaseReservation`, `useConfirmPurchase`, `ConfirmPurchaseBanner`,
        `useActiveReservation`, `useActiveReservationHydration`.
  </behavior>
  <action>
    Execute RED → GREEN.

    STEP 1 (RED) — Add the failing specs FIRST. Edit ONLY
    `web/src/pages/__tests__/ItemReservePage.test.tsx`:

      1a. Refactor `renderPage` so it exposes a `rerenderSame()` helper that re-renders the SAME JSX
          tree (so React keeps the same component instance and `prevStatusRef` survives across
          renders). The QueryClient + router are constructed inside the function; the JSX is built
          into a `tree` constant; the helper calls `result.rerender(tree)`:
          ```typescript
          function renderPage(registryId = 'reg1', itemId = 'it1', extraRoutes: { path: string; element: React.ReactNode }[] = []) {
            const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
            const router = createMemoryRouter(
              [
                { path: '/registry/:id/item/:itemId', element: <ItemReservePage /> },
                { path: '/registry/:id', element: <div data-testid="registry-page" /> },
                ...extraRoutes,
              ],
              { initialEntries: [`/registry/${registryId}/item/${itemId}`] },
            )
            const tree = (
              <QueryClientProvider client={client}>
                <RouterProvider router={router} />
              </QueryClientProvider>
            )
            const result = render(tree)
            return {
              ...result,
              rerenderSame: () => result.rerender(tree),
            }
          }
          ```
          This is a non-breaking change for existing P-01..P-07 specs (they ignore the return value).

      1b. Add P-08, P-09, P-10 specs verbatim per the `<behavior>` block. P-09 / P-10 use the pattern:
          ```typescript
          const { rerenderSame } = renderPage()
          expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
          act(() => {
            itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })
          })
          rerenderSame()
          await waitFor(() => {
            expect(screen.queryByTestId('item-reserve-detail')).not.toBeInTheDocument()
            expect(screen.getByTestId('registry-page')).toBeInTheDocument()
          })
          ```

      1c. Run: `cd web && npm run test:run -- ItemReservePage`.
          Confirm: P-08 FAILS on stock source (the stable failure that motivates the fix). P-09 / P-10
          may pass coincidentally on the buggy code (because the buggy effect ALSO fires on any
          `'available'`-or-`'purchased'`) — that's acceptable as a starting point. The binding contract
          is: after the source fix, ALL THREE must pass AND ALL EXISTING specs must still pass.

    STEP 2 (GREEN) — Apply the source fix to `web/src/pages/ItemReservePage.tsx`:

      2a. Add the import `import type { ItemStatus } from '../lib/firestore-mapping'` to the existing
          imports block.

      2b. Add `const prevStatusRef = useRef<ItemStatus | undefined>(undefined)` directly below the
          existing `const itemStatusNavigatedRef = useRef(false)` (around line 62).

      2c. Replace the body of the existing `useEffect` at lines 90-99 with:
          ```typescript
          useEffect(() => {
            const prev = prevStatusRef.current
            prevStatusRef.current = currentItemStatus

            if (!active) return
            if (itemStatusNavigatedRef.current) return
            if (prev !== 'reserved') return
            if (currentItemStatus !== 'purchased' && currentItemStatus !== 'available') return

            itemStatusNavigatedRef.current = true
            navigate(`/registry/${id}`)
          }, [currentItemStatus, active, id, navigate])
          ```
          Update the comment block above the effect (lines 85-88) to reflect the transition-detector
          semantics:
          ```
          // Detect a real status TRANSITION out of 'reserved' (covers confirm-purchase success and
          // release fired from another tab / via Cloud Tasks). We must observe 'reserved' first —
          // otherwise a stale 'available' snapshot on initial mount would bounce the user back before
          // the reservation propagates. itemStatusNavigatedRef ensures we fire at most once per mount.
          ```

      2d. Re-run: `cd web && npm run test:run`. All ItemReservePage specs MUST pass, AND no other
          test file should regress. If anything else breaks, investigate and fix without modifying
          the protected files listed above.

      2e. Run: `cd web && npx tsc --noEmit`. Exit code MUST be 0. If the `ItemStatus` import path is
          wrong, fix it; if any other type error surfaces, investigate.

    STEP 3 — Inspect for unintended diffs. Run:
      `git diff --stat web/src/pages/ItemReservePage.tsx web/src/pages/__tests__/ItemReservePage.test.tsx`
      Confirm only the two listed files changed. Confirm no protected files were touched:
      ```
      git diff --name-only HEAD -- web/src/features/registry/useItemsQuery.ts \
        web/src/features/reservation/useReservationForItem.ts \
        web/src/features/reservation/useReleaseReservation.ts \
        web/src/features/reservation/useConfirmPurchase.ts \
        web/src/features/reservation/ConfirmPurchaseBanner.tsx \
        web/src/features/reservation/useActiveReservation.tsx \
        web/src/features/reservation/useActiveReservationHydration.ts
      ```
      Expected output: empty.
  </action>
  <verify>
    <automated>cd web && npm run test:run -- ItemReservePage && npx tsc --noEmit</automated>
  </verify>
  <done>
    - All existing P-01..P-07 specs in `ItemReservePage.test.tsx` still pass.
    - P-08 (stale-on-mount): `item-reserve-detail` present, no redirect to `/registry/:id`.
    - P-09 ('reserved' -> 'available'): redirect to `/registry/:id` happens AFTER the transition (only).
    - P-10 ('reserved' -> 'purchased'): redirect to `/registry/:id` happens AFTER the transition (only).
    - `cd web && npx tsc --noEmit` exits 0.
    - Source change is minimal: ONE new import, ONE new ref, ONE rewritten effect body. No JSX changes.
    - No protected file was touched.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 2: Human verification — end-to-end reserve, release, confirm-purchase flows on the web app</name>
  <files>web/src/pages/ItemReservePage.tsx</files>
  <action>
    Pause for human verification. Do NOT proceed past this checkpoint until the user types "approved".
    The user will exercise the four scenarios listed in `<verify>` against a running dev server. The
    fix from Task 1 is what's being validated end-to-end. If any scenario fails, capture the symptom
    and return to planning rather than patching blind.

    Setup the dev server before pausing:
      `cd web && npm run dev`
    (If the user is already running a dev server pointed at the right backend, skip the start step
    and just remind them which URL to open.)
  </action>
  <verify>
    Manual scenarios — all four must pass before the user types "approved":

    (a) GUEST AUTO-RESERVE — fresh path:
        1. Open the registry as a guest in an incognito window: `/registry/<some-public-id>`.
        2. Click "Reserve" on an item.
        3. Complete the guest-identity step if prompted (email + first name).
        4. The page auto-navigates to `/registry/<id>/item/<itemId>`.
        EXPECTED: You see the full reserve-detail UI — `data-testid="item-reserve-detail"`, the item
        card with name and price, the countdown mm:ss, the "I completed the purchase" banner, and the
        release CTA.
        BUG (pre-fix): you saw "This isn't your reservation" OR you were bounced back to
        `/registry/<id>` after a brief flash. Confirm this is NO LONGER the case.

    (b) SIGNED-IN, SECOND ITEM:
        1. Sign in with a real account.
        2. On a registry with at least 2 available items, reserve Item A. URL becomes
           `/registry/<id>/item/<itemA>`. Verify the reserve-detail UI renders.
        3. Click "Back to registry" to go to `/registry/<id>`.
        4. Reserve Item B.
        EXPECTED: URL changes to `/registry/<id>/item/<itemB>` (NOT staying at /item/<itemA>, NOT bouncing
        back to /registry/<id>). The banner shows Item B's name. The countdown reflects Item B's
        reservation, not Item A's.

    (c) CONFIRM-PURCHASE REGRESSION:
        1. On a reserve-detail page for an item you have actively reserved, click "I completed the
           purchase" in the ConfirmPurchaseBanner.
        2. Wait for the confirm to round-trip (the banner enters its "saved" state).
        EXPECTED: page auto-navigates back to `/registry/<id>`. The item now shows as purchased in the
        registry list. (This is the original intent of the effect — it MUST still work.)

    (d) RELEASE REGRESSION:
        1. Reserve an item. Land on `/registry/<id>/item/<itemId>`.
        2. Click "Release reservation".
        EXPECTED: page auto-navigates back to `/registry/<id>`. A success toast appears with the
        release_success copy. The item now shows as available in the registry list. (This is the
        release-effect — separate from the bug fix — verifying it did not regress.)

    Resume signal: user types "approved" or describes the failing scenario.
  </verify>
  <done>
    User has typed "approved" after exercising all four scenarios. The buggy bounce-back on initial
    reserve is gone (scenarios a + b), AND the legitimate navigate-back on confirm-purchase and
    release continues to work (scenarios c + d).
  </done>
</task>

</tasks>

<verification>
- All 10 ItemReservePage specs pass: `cd web && npm run test:run -- ItemReservePage`.
- Full web test suite passes (no other regressions): `cd web && npm run test:run`.
- TypeScript clean: `cd web && npx tsc --noEmit` exits 0.
- Source diff scoped: `git diff --name-only HEAD` shows ONLY `web/src/pages/ItemReservePage.tsx` and
  `web/src/pages/__tests__/ItemReservePage.test.tsx`.
- Human verification of all four scenarios (guest auto-reserve lands on detail page, second-item
  reserve navigates to new URL, confirm-purchase still navigates back, release still navigates back).
</verification>

<success_criteria>
- The buggy initial-render bounce-back is gone (proven by P-08).
- Real transitions still navigate back (proven by P-09, P-10 + human-verify scenarios c, d).
- No protected files touched (proven by scoped diff).
- Single-import + single-ref + single-effect-rewrite source change (minimal blast radius).
- `npm run test:run` green; `npx tsc --noEmit` clean.
</success_criteria>

<output>
After completion, create `.planning/quick/260513-hon-fix-itemreservepage-navigate-back-firing/260513-hon-SUMMARY.md`
documenting:
  - The two-ref pattern (`prevStatusRef` + `itemStatusNavigatedRef`) and why both are needed.
  - The 3 new specs and what each proves.
  - The scenarios verified by hand.
  - Any deferred follow-ups (e.g., extracting the transition-detector into a reusable hook — only if
    a second use-case has emerged).
</output>
