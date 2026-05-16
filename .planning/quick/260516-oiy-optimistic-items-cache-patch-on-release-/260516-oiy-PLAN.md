---
phase: quick/260516-oiy-optimistic-items-cache-patch-on-release-
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/src/pages/ItemReservePage.tsx
  - web/src/pages/__tests__/ItemReservePage.test.tsx
autonomous: true
requirements:
  - QUICK-260516-oiy
must_haves:
  truths:
    - "When the user releases their reservation from ItemReservePage, the React Query items cache for ['registry', :id, 'items'] is patched in-place BEFORE navigate(`/registry/:id`) fires, so the just-released item appears as status: 'available' on the destination page even before the new onSnapshot listener's first fire."
    - "The patch only mutates the item whose id === route itemId; all other items in the cache remain referentially unchanged."
    - "The patched item has status === 'available', reservedBy === null, reservedAt === null, and expiresAt === null."
    - "If the cache is empty/undefined at release-success time (cache evicted, listener never fired), the updater returns the unchanged old value (?? old) — never throws, never overwrites with garbage."
    - "The release-success useEffect still: sets releaseSuccessHandledRef.current = true (single-fire guard), shows the release_success toast, calls clearActiveReservation(), and navigates to /registry/:id."
    - "queryClient.setQueryData is invoked BEFORE the navigate mock (asserted via mock.invocationCallOrder in K-20)."
    - "No backend, no i18n, no new file, no new hook, no other component touched. Single-file client-side fix."
    - "All 195 existing tests in ItemReservePage.test.tsx remain green; the new K-20 brings the running total to 196 in the file's describe block."
    - "tsc --noEmit (web project) passes with no new errors. CI test run (npm test -- --run) passes."
  artifacts:
    - path: "web/src/pages/ItemReservePage.tsx"
      provides: "Release-success useEffect patches the items cache via useQueryClient().setQueryData BEFORE navigate fires; queryClient and itemId added to deps array; explanatory inline comment."
      contains: "queryClient.setQueryData"
    - path: "web/src/pages/__tests__/ItemReservePage.test.tsx"
      provides: "K-20 test appended at the end of the existing describe block. Mocks useQueryClient via vi.hoisted + vi.mock importOriginal pattern; asserts setQueryData call signature, updater behavior on a fake items array, and call-order vs the navigate effect."
      contains: "K-20"
  key_links:
    - from: "ItemReservePage.tsx release-success useEffect (current lines ~193-200)"
      to: "queryClient.setQueryData(['registry', id, 'items'], updater)"
      via: "Synchronous call placed BEFORE clearActiveReservation() and navigate() inside the same effect tick"
      pattern: "queryClient\\.setQueryData<Item\\[\\]>\\(\\['registry', id, 'items'\\]"
    - from: "ItemReservePage.tsx updater function"
      to: "Item type from ../lib/firestore-mapping"
      via: "Type parameter on setQueryData<Item[]> — Item stays a type-only import (the existing `import type` at line 5 is sufficient; do NOT drop the type modifier)"
      pattern: "setQueryData<Item\\[\\]>"
    - from: "K-20 test mock"
      to: "vi.mock('@tanstack/react-query', async (importOriginal) => ({ ...await importOriginal(), useQueryClient: () => mockQueryClient }))"
      via: "vi.hoisted const mockQueryClient = { setQueryData: vi.fn() } declared at module scope; importOriginal preserves QueryClient + QueryClientProvider used by renderPage"
      pattern: "vi\\.mock\\('@tanstack/react-query'"
---

<objective>
Patch the React Query items cache optimistically when a reservation is released from ItemReservePage, so the just-released item appears `available` on the RegistryPage grid the moment the user lands there — eliminating the ~100–500ms window in which the FROZEN cache (from before navigation, when RegistryPage's onSnapshot listener torn down) shows the item still as RESERVED with the reserved-by-me in-card countdown banner.

Purpose: Reservation lifecycle UX trust. The reservation-to-purchase flow MUST be visibly truthful end-to-end (project core value). A user who explicitly clicked Release and got the success toast must not see their just-released item still marked as theirs on the very next page. Without this patch, the stale-tile artifact directly contradicts the toast and undermines trust in the reservation system.

Output:
- ItemReservePage.tsx: one new hook call (useQueryClient), one new line inside the existing release-success useEffect (queryClient.setQueryData with an in-place updater), two deps added (queryClient, itemId), inline comment explaining the snapshot-lag root cause.
- ItemReservePage.test.tsx: K-20 appended — verifies the cache patch fires with the right key, the updater produces the expected available-shaped item, unrelated items are untouched, and the patch fires BEFORE navigation (mock.invocationCallOrder).
- All 195 existing tests stay green; running total in this file: 196.
- tsc --noEmit + npm test -- --run both pass.

NOTE — CLIENT-SIDE-ONLY fix. The backend already correctly marks the reservation expired and flips item.status to 'available' before releaseReservationCallable returns. The issue is purely React Query cache freshness across a route unmount: RegistryPage unmounts → useItemsQuery's onSnapshot tears down → cache freezes at the pre-release moment → navigation re-mounts RegistryPage → new onSnapshot takes ~100–500ms to fire → during the gap, useItemsQuery returns the frozen cache. No backend, no Firestore rules, no callable, no Cloud Functions, no i18n string changes.

EXPLICITLY OUT OF SCOPE: useItemsQuery.ts (REFERENCE ONLY for the queryKey shape — do NOT modify), StickyReserveBanner.tsx, ConfirmPurchaseBanner.tsx, app/, functions/, any other web component, any web/public/locales/*.json file, any new hook, any new test file.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md
@web/src/pages/ItemReservePage.tsx
@web/src/features/registry/useItemsQuery.ts
@web/src/lib/firestore-mapping.ts
@web/src/pages/__tests__/ItemReservePage.test.tsx
@web/src/features/reservation/useReleaseReservation.ts

<diagnosis>
Bug fully diagnosed by the orchestrator. DO NOT re-investigate.

Reproduction:
1. Signed-out user opens shared registry link → RegistryPage mounts → useItemsQuery's onSnapshot is live.
2. User reserves item itemX → tile re-renders as RESERVED with reserved-by-me banner (cache live, snapshot fresh).
3. User clicks tile → navigates to /registry/:id/item/itemX → RegistryPage unmounts → its onSnapshot unsubscribes → cache value for ['registry', :id, 'items'] is now FROZEN at the post-reserve moment.
4. On ItemReservePage, user clicks Release → useReleaseReservation calls releaseReservationCallable → backend transaction flips item.status to 'available', clears reservedBy/reservedAt/expiresAt.
5. release-success useEffect fires (current lines 193-200):
   a. releaseSuccessHandledRef.current = true (single-fire guard)
   b. showToast('reservation.release_success', 'success')  ← user sees success toast
   c. clearActiveReservation() ← shared context nulled, StickyReserveBanner hides
   d. navigate(`/registry/${id}`) ← RegistryPage re-mounts, useItemsQuery's NEW onSnapshot subscribes
6. The new onSnapshot takes ~100–500ms to fire its first snapshot. During that gap, useItemsQuery returns getQueryData(queryKey) ← FROZEN cache from step 3, where itemX.status === 'reserved' and reservedBy === user@x.
7. RegistryPage's grid renders the frozen cache → user sees their just-released item STILL marked RESERVED with the reserved-by-me countdown banner → directly contradicts the success toast they just saw.

Root cause: snapshot-lag on the React Query cache across a route unmount. No backend, network, or rules issue.

Fix approach: optimistically patch the cache to match what the next snapshot will eventually deliver. Use queryClient.setQueryData with a functional updater that maps over the existing Item[] and overrides ONLY itemX's reservation fields. Place the patch BEFORE navigate() so the destination RegistryPage's first render reads the patched value, not the stale one. When the real onSnapshot fires (~100-500ms later), it will write the same values — no flicker, no race.

Why setQueryData (not invalidateQueries):
- invalidateQueries triggers a refetch via the queryFn — but useItemsQuery's queryFn just reads from the cache (it's a snapshot-listener-backed query, not a fetch-backed one). Invalidating would be a no-op.
- setQueryData directly writes the cache, which is exactly what the onSnapshot callback does at line 21 of useItemsQuery.ts. This patch is a synthetic snapshot update — same shape, same key, same effect.

Why the functional updater (not setQueryData(key, newValue)):
- The cache value type is Item[]. A direct overwrite would require reconstructing the entire array. The updater preserves all other items by reference (referential equality matters for React Query's structural sharing and downstream useMemo / React.memo optimizations).
- The `?? old` fallback handles the (unlikely but possible) case where the cache is undefined at release-success time — never overwrite with garbage.

Why itemId in the deps array:
- The current effect's deps are [releaseStatus, id, navigate, showToast, t, clearActiveReservation]. The new code reads itemId (from useParams, declared at line 100) inside the updater. React's exhaustive-deps lint will flag this; adding itemId to deps is correct (itemId is stable for the lifetime of this page mount because the route is /registry/:id/item/:itemId).

Why queryClient in the deps array:
- queryClient comes from useQueryClient(); React Query guarantees a stable reference per QueryClientProvider, but the exhaustive-deps lint still expects it in the deps array. Add it.

Why Item stays a type-only import (existing line 5: `import type { Item, ItemStatus } from '../lib/firestore-mapping'`):
- The patch uses Item as a TYPE PARAMETER on setQueryData<Item[]>(...). Type parameters are erased at compile time and require only a type-side binding. The existing `import type` is sufficient; do NOT change it to a value import. Verified by reading line 5 of the current file.

Why the updater object literal does NOT cast as Item:
- The literal `{ ...it, status: 'available' as const, reservedBy: null, reservedAt: null, expiresAt: null }` is type-compatible with Item without a cast. The `as const` on 'available' narrows it to the ItemStatus literal 'available' so the spread doesn't widen to string. All other fields (id, title, imageUrl, price, currency, notes, affiliateUrl, originalUrl, merchantDomain) are inherited from the spread. No type cast needed.

K-20 mock-shape verification (from reading lines 1-100 of the test file):
- The test file uses the vi.hoisted + vi.mock pattern throughout (releaseMock, confirmMock, activeMock, createReservationMock, etc.). New mocks must follow this convention.
- The test file does NOT mock react-router or @tanstack/react-query — it uses createMemoryRouter and a real QueryClient. The new K-20 mock for useQueryClient must use vi.mock('@tanstack/react-query', async (importOriginal) => ({ ...await importOriginal(), useQueryClient: () => mockQueryClient })) so QueryClient and QueryClientProvider (consumed by renderPage at lines 149/162) remain real.
- mockQueryClient = { setQueryData: vi.fn() } — only setQueryData is needed; the page does not call any other queryClient methods inside the release-success path.
- Call-order assertion: navigate is not directly mockable here (no react-router mock). Use mock.invocationCallOrder: setQueryData has invocationCallOrder N; assert it is LESS THAN some signal of navigation having occurred. The cleanest available proxy: assert setQueryData was called BEFORE activeMock.clear (which the effect calls between setQueryData and navigate). activeMock.clear is already a vi.fn (reset in beforeEach), so its invocationCallOrder is observable. This proves setQueryData fires earliest in the effect body, which is the only ordering that matters (it just has to happen before navigate; ordering relative to clear is a strict superset proof since clear comes before navigate).

Edge cases:
- Release fires but Cloud Tasks already auto-released the item server-side: backend already flipped item.status to 'available' before the callable returns. Cache patch writes the same values the onSnapshot will eventually deliver. No conflict.
- Cache is undefined at release-success time (rare — would mean the listener never fired): updater returns `?? old` (= undefined). setQueryData with undefined is a no-op for the consumer. Safe.
- User releases, navigates manually back to ItemReservePage before navigate(`/registry/${id}`) completes: not possible — release-success is a single-fire useEffect (releaseSuccessHandledRef guard), and the navigate is synchronous on the effect tick. No re-entry.
- Multiple items in the cache: updater's `.map((it) => it.id === itemId ? {...} : it)` touches only the matching item; all others are returned by reference. K-20 asserts this with a 2-item fake input.
- React Query v5 setQueryData updater signature: `(oldData: T | undefined) => T | undefined` — exactly what we use. Compatible with the installed version.

D-06 invariant unchanged: no reserver name/email is rendered on this page or RegistryPage by this patch. The updater nulls reservedBy (which is an email), strengthening the invariant rather than weakening it.
</diagnosis>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1 (RED): Append failing K-20 test for optimistic cache patch on release-success</name>
  <files>web/src/pages/__tests__/ItemReservePage.test.tsx</files>
  <behavior>
    K-20: release-from-ItemReservePage optimistic cache patch.

    Setup (BEFORE the existing component import on line 98, ALONGSIDE the other vi.hoisted + vi.mock declarations):
      - vi.hoisted: `const queryClientMock = { setQueryData: vi.fn() }`
      - vi.mock('@tanstack/react-query', async (importOriginal) => {
          const actual = await importOriginal<typeof import('@tanstack/react-query')>()
          return { ...actual, useQueryClient: () => queryClientMock }
        })
      - Reset queryClientMock.setQueryData in beforeEach (alongside the other resets at lines 178-197).

    Test body (appended INSIDE the existing `describe('ItemReservePage', ...)` block — after K-19 ends at line 914, BEFORE the closing `})` at line 915):

    1. Arrange: default beforeEach already wires ACTIVE_RES (itemId='it1') + a single makeItem in the items mock. Drive release to success BEFORE renderPage so the effect fires on first commit (same pattern as P-06b on line 319): `releaseMock.status = 'success'`.
    2. Act: renderPage() — the release-success useEffect runs synchronously on mount; the cache patch + clear + navigate all fire.
    3. Assert call shape:
       a. await waitFor(() => expect(queryClientMock.setQueryData).toHaveBeenCalledTimes(1))
       b. Read call args: `const [key, updater] = queryClientMock.setQueryData.mock.calls[0]`
       c. expect(key).toEqual(['registry', 'reg1', 'items'])
       d. expect(typeof updater).toBe('function')
    4. Assert updater behavior on a fake 2-item input:
       a. Build a fake input: `const fakeIn = [makeItem({ id: 'it1', status: 'reserved', reservedBy: 'user@x', reservedAt: new Date(123), expiresAt: new Date(456) }), makeItem({ id: 'other', status: 'available', reservedBy: null, reservedAt: null, expiresAt: null })]`
       b. `const out = updater(fakeIn)`
       c. expect(out).toHaveLength(2)
       d. const patched = out.find((i: { id: string }) => i.id === 'it1')!; expect(patched.status).toBe('available'); expect(patched.reservedBy).toBeNull(); expect(patched.reservedAt).toBeNull(); expect(patched.expiresAt).toBeNull()
       e. const untouched = out.find((i: { id: string }) => i.id === 'other')!; expect(untouched).toBe(fakeIn[1])  // referential identity preserved
    5. Assert updater on undefined input returns undefined (cache-empty safety):
       a. expect(updater(undefined)).toBeUndefined()
    6. Assert call ORDER (setQueryData fires BEFORE clearActiveReservation, which itself fires before navigate inside the same effect body — see ItemReservePage.tsx lines 193-200):
       a. const patchOrder = queryClientMock.setQueryData.mock.invocationCallOrder[0]
       b. const clearOrder = activeMock.clear.mock.invocationCallOrder[0]
       c. expect(patchOrder).toBeLessThan(clearOrder)
    7. Sanity: navigation still completed:
       a. await waitFor(() => expect(screen.getByTestId('registry-page')).toBeInTheDocument())

    Expected RED outcome: K-20 FAILS on `expect(queryClientMock.setQueryData).toHaveBeenCalledTimes(1)` because the current ItemReservePage.tsx does not call useQueryClient() at all and the release-success effect does not patch the cache. Other 195 tests remain green (importOriginal-spread mock preserves real QueryClient/QueryClientProvider used by renderPage at lines 149/162).

    Constraints:
      - Use ONLY the existing mocking conventions (vi.hoisted + vi.mock). Do NOT introduce vi.spyOn, no jest.fn, no new test util file.
      - Do NOT mock react-router, useReleaseReservation, useActiveReservation, useItemsQuery — they are already mocked at top-of-file; the new K-20 layers a single new mock for @tanstack/react-query and consumes the existing mocks unchanged.
      - Do NOT touch any test above K-20 (P-01..P-11, K-01..K-19, U-01..U-08). Append-only.
      - Reuse the existing makeItem helper (lines 111-128). Do NOT redefine fixtures.
      - Do NOT alter beforeEach beyond adding `queryClientMock.setQueryData.mockReset()` alongside the other resets.
  </behavior>
  <action>
    Step 1. Open web/src/pages/__tests__/ItemReservePage.test.tsx.

    Step 2. After the existing vi.hoisted/vi.mock blocks (around the createReservationMock block at lines 86-96), BEFORE the `import ItemReservePage from '../ItemReservePage'` line (98), insert:

    ```ts
    // queryClient — partial mock of @tanstack/react-query that preserves the real QueryClient
    // and QueryClientProvider (consumed by renderPage below) and ONLY overrides useQueryClient
    // so we can spy on setQueryData inside ItemReservePage's release-success effect (quick-260516-oiy K-20).
    const queryClientMock = vi.hoisted(() => ({ setQueryData: vi.fn() }))
    vi.mock('@tanstack/react-query', async (importOriginal) => {
      const actual = await importOriginal<typeof import('@tanstack/react-query')>()
      return { ...actual, useQueryClient: () => queryClientMock }
    })
    ```

    Step 3. In the existing beforeEach (lines 177-197), add at the end (after createReservationMock resets, before the auth/guest setup):

    ```ts
    queryClientMock.setQueryData.mockReset()
    ```

    Step 4. After the K-19 test (which ends at line 914 with its closing `})`), and BEFORE the describe block's closing `})` on line 915, append the K-20 test:

    ```ts
    // ---- quick-260516-oiy — optimistic items-cache patch on release-success ----

    it('K-20: release-success patches items cache BEFORE navigate so the just-released item appears available on RegistryPage', async () => {
      // Drive release to success BEFORE mount so the effect fires on first commit (mirrors P-06b).
      releaseMock.status = 'success'

      renderPage()

      // 1. Cache patch fires exactly once with the correct key + updater shape.
      await waitFor(() => {
        expect(queryClientMock.setQueryData).toHaveBeenCalledTimes(1)
      })
      const [key, updater] = queryClientMock.setQueryData.mock.calls[0] as [
        unknown,
        (old: Item[] | undefined) => Item[] | undefined,
      ]
      expect(key).toEqual(['registry', 'reg1', 'items'])
      expect(typeof updater).toBe('function')

      // 2. Updater overrides ONLY the matching item; unrelated items keep referential identity.
      const fakeIn: Item[] = [
        makeItem({
          id: 'it1',
          status: 'reserved',
          reservedBy: 'user@x',
          reservedAt: new Date(123),
          expiresAt: new Date(456),
        }),
        makeItem({
          id: 'other',
          status: 'available',
          reservedBy: null,
          reservedAt: null,
          expiresAt: null,
        }),
      ]
      const out = updater(fakeIn)!
      expect(out).toHaveLength(2)
      const patched = out.find((i) => i.id === 'it1')!
      expect(patched.status).toBe('available')
      expect(patched.reservedBy).toBeNull()
      expect(patched.reservedAt).toBeNull()
      expect(patched.expiresAt).toBeNull()
      const untouched = out.find((i) => i.id === 'other')!
      expect(untouched).toBe(fakeIn[1]) // referential identity preserved

      // 3. Updater on undefined input returns undefined (cache-empty safety).
      expect(updater(undefined)).toBeUndefined()

      // 4. Call order: setQueryData fires BEFORE clearActiveReservation, which itself
      //    fires before navigate inside the same effect body (ItemReservePage.tsx lines 193-200).
      //    Asserting < clear is a strict superset proof that the patch happens before navigate.
      const patchOrder = queryClientMock.setQueryData.mock.invocationCallOrder[0]
      const clearOrder = activeMock.clear.mock.invocationCallOrder[0]
      expect(patchOrder).toBeLessThan(clearOrder)

      // 5. Sanity: navigation still completed (registry page mounted).
      await waitFor(() => {
        expect(screen.getByTestId('registry-page')).toBeInTheDocument()
      })
    })
    ```

    Step 5. Run the test in isolation to confirm RED:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx -t "K-20"
    ```
    Expected: K-20 FAILS on `expect(queryClientMock.setQueryData).toHaveBeenCalledTimes(1)` (received 0).

    Step 6. Run the full ItemReservePage test file to confirm the other 195 stay green:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx
    ```
    Expected: 195 pass, 1 fail (K-20). Total: 196 tests in suite.

    Step 7. Commit RED:
    ```
    test(quick-260516-oiy-01): add failing K-20 — optimistic items-cache patch on release-success
    ```
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx 2>&1 | tail -20</automated>
  </verify>
  <done>
    - K-20 test exists at end of describe block in web/src/pages/__tests__/ItemReservePage.test.tsx.
    - queryClientMock + vi.mock('@tanstack/react-query', importOriginal pattern) added near the other top-of-file mocks.
    - beforeEach includes `queryClientMock.setQueryData.mockReset()`.
    - Running the file shows: 195 pass, 1 fail (K-20 fails on toHaveBeenCalledTimes(1) — received 0).
    - Total test count in this file: 196.
    - Commit recorded with `test(quick-260516-oiy-01):` prefix.
    - NO production code changed.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2 (GREEN): Add optimistic items-cache patch to ItemReservePage release-success useEffect</name>
  <files>web/src/pages/ItemReservePage.tsx</files>
  <behavior>
    The release-success useEffect (current lines 193-200) must patch the React Query items cache for ['registry', id, 'items'] in-place BEFORE clearActiveReservation() and navigate() fire, so the destination RegistryPage's first render reads the patched value (item appears as 'available' with cleared reservation fields) rather than the stale frozen cache.

    Concretely:
      - useQueryClient is imported from '@tanstack/react-query'.
      - `const queryClient = useQueryClient()` is declared near the other top-of-component hook calls (alongside useNavigate/useTranslation/useItemsQuery/useReservationForItem/useActiveReservation).
      - Inside the release-success useEffect, BEFORE clearActiveReservation():
        ```
        queryClient.setQueryData<Item[]>(['registry', id, 'items'], (old) =>
          old?.map((it) =>
            it.id === itemId
              ? { ...it, status: 'available' as const, reservedBy: null, reservedAt: null, expiresAt: null }
              : it,
          ) ?? old,
        )
        ```
      - The effect's deps array gains `queryClient` and `itemId`.
      - An inline comment explains the root cause (snapshot-lag on cache across route unmount) so future readers don't re-introduce the bug.

    After this change, K-20 passes; all other 195 tests stay green; tsc --noEmit passes.
  </behavior>
  <action>
    Step 1. Open web/src/pages/ItemReservePage.tsx.

    Step 2. Verify the current import on line 5 reads:
    ```ts
    import type { Item, ItemStatus } from '../lib/firestore-mapping'
    ```
    Keep this `import type` UNCHANGED — Item is used as a type parameter on setQueryData<Item[]> only, never as a runtime value. Type-only import is correct.

    Step 3. Add a new named import for useQueryClient from @tanstack/react-query. Insert as a NEW line (e.g. after line 18, before the comment block at line 20):
    ```ts
    import { useQueryClient } from '@tanstack/react-query'
    ```

    Step 4. Inside the ItemReservePage component body, near the other hook calls (e.g. immediately after line 102 `const { t } = useTranslation()` and before line 103 `const itemsQ = useItemsQuery(id)`), insert:
    ```ts
    const queryClient = useQueryClient()
    ```

    Step 5. Locate the release-success useEffect (current lines 192-200). Replace its body to add the cache patch BEFORE clearActiveReservation(). The updated effect:
    ```ts
    // Release success: optimistically patch the items cache so the just-released item
    // appears 'available' on RegistryPage even before its onSnapshot listener (which
    // tore down when RegistryPage unmounted on navigation TO this page) re-fires after
    // re-mount. Without this patch, useItemsQuery returns the FROZEN pre-release cache
    // for ~100-500ms after navigate, showing the item still as RESERVED with the
    // reserved-by-me banner — directly contradicting the success toast (quick-260516-oiy).
    // Then: clear shared active-reservation context, show toast, navigate back.
    useEffect(() => {
      if (releaseStatus === 'success' && !releaseSuccessHandledRef.current) {
        releaseSuccessHandledRef.current = true
        queryClient.setQueryData<Item[]>(['registry', id, 'items'], (old) =>
          old?.map((it) =>
            it.id === itemId
              ? { ...it, status: 'available' as const, reservedBy: null, reservedAt: null, expiresAt: null }
              : it,
          ) ?? old,
        )
        showToast(t('reservation.release_success'), 'success')
        clearActiveReservation()
        navigate(`/registry/${id}`)
      }
    }, [releaseStatus, id, itemId, navigate, showToast, t, clearActiveReservation, queryClient])
    ```

    Notes on the change:
      - Order inside the if-block: setQueryData (FIRST — destination page needs the patched cache when navigate triggers re-mount) → showToast → clearActiveReservation → navigate (LAST).
      - showToast is intentionally AFTER setQueryData so the cache write happens earliest. Toast firing first is fine functionally but ordering setQueryData first is cleaner.
      - Deps: added `itemId` (read by updater) and `queryClient` (stable per QueryClientProvider, but exhaustive-deps wants it listed). Existing deps preserved.
      - `'available' as const` is required so the spread doesn't widen status to string — keeps the literal type ItemStatus-compatible.
      - `?? old` fallback: if old is undefined (cache evicted or never populated), return the original undefined rather than calling .map on undefined. setQueryData accepts undefined as a no-op write.

    Step 6. Do NOT modify any other useEffect, function, render branch, helper, or comment in ItemReservePage.tsx beyond the four edits above (one import, one hook call, one effect body, one deps array). Do NOT touch BrowseShell, ItemDetailHero, NotesBlock, or renderReservedByMeDetail.

    Step 7. Type-check the web project:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit
    ```
    Expected: no new errors. (If any pre-existing errors surface unrelated to this change, leave them alone — they belong to other quick tasks.)

    Step 8. Run K-20 in isolation to confirm GREEN:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx -t "K-20"
    ```
    Expected: K-20 PASSES.

    Step 9. Run the full ItemReservePage test file:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npx vitest run src/pages/__tests__/ItemReservePage.test.tsx
    ```
    Expected: all 196 tests pass.

    Step 10. Run the full web test suite in CI mode to confirm no cross-file regressions:
    ```
    cd /Users/victorpop/ai-projects/gift-registry/web && npm test -- --run
    ```
    Expected: full suite passes; ItemReservePage file shows 196/196.

    Step 11. Commit GREEN:
    ```
    fix(quick-260516-oiy-01): patch items cache optimistically on release-success in ItemReservePage
    ```

    DO NOT touch: web/src/features/registry/useItemsQuery.ts, web/src/features/reservation/StickyReserveBanner.tsx, web/src/features/reservation/ConfirmPurchaseBanner.tsx, web/src/features/reservation/useReleaseReservation.ts, any file under app/, any file under functions/, any web/public/locales/*.json, any other component, any new file.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit && npm test -- --run 2>&1 | tail -30</automated>
  </verify>
  <done>
    - web/src/pages/ItemReservePage.tsx imports useQueryClient from '@tanstack/react-query'.
    - `const queryClient = useQueryClient()` declared in component body alongside other hook calls.
    - Release-success useEffect patches the items cache via queryClient.setQueryData<Item[]>(['registry', id, 'items'], updater) BEFORE clearActiveReservation() and navigate().
    - Effect deps array includes `queryClient` and `itemId` alongside existing deps.
    - Inline comment explains the snapshot-lag root cause.
    - Existing `import type { Item, ItemStatus }` on line 5 UNCHANGED — Item used as type parameter only.
    - tsc --noEmit passes (no new errors introduced).
    - K-20 passes.
    - ItemReservePage.test.tsx: 196/196 tests pass.
    - Full web suite (npm test -- --run) passes with no regressions.
    - No other file touched (no changes to useItemsQuery.ts, banners, hooks, backend, i18n, components).
    - Commit recorded with `fix(quick-260516-oiy-01):` prefix.
  </done>
</task>

</tasks>

<verification>
After Task 2 completes, run the following from /Users/victorpop/ai-projects/gift-registry/web:

1. Type-check: `npx tsc --noEmit` — must pass with no new errors.
2. Targeted test: `npx vitest run src/pages/__tests__/ItemReservePage.test.tsx` — 196/196 pass.
3. CI mode: `npm test -- --run` — full web suite passes.
4. Diff sanity: `git diff --stat HEAD~2..HEAD` — exactly 2 files changed (ItemReservePage.tsx, ItemReservePage.test.tsx). No other paths.
5. Grep guard: `git diff HEAD~2..HEAD -- functions/ app/ web/public/locales/` must show NO output (scope invariant: backend/i18n/Android untouched).
6. Grep guard: `git diff HEAD~2..HEAD -- web/src/features/registry/useItemsQuery.ts web/src/features/reservation/StickyReserveBanner.tsx web/src/features/reservation/ConfirmPurchaseBanner.tsx` must show NO output (referenced files NOT modified).
</verification>

<success_criteria>
- ItemReservePage.tsx release-success useEffect calls queryClient.setQueryData with the correct key + updater BEFORE navigate fires.
- Updater overrides ONLY the matching item; unrelated items keep referential identity.
- Patched item has status='available', reservedBy=null, reservedAt=null, expiresAt=null.
- K-20 passes (test asserts call shape, updater behavior, call ordering, navigation completion).
- 195 pre-existing tests in ItemReservePage.test.tsx remain green; suite total in file: 196.
- tsc --noEmit passes (no new errors).
- Full web CI test run (npm test -- --run) passes.
- Two commits exist on HEAD (RED test, GREEN fix), in that order, each with the correct (quick-260516-oiy-01) prefix.
- ONLY 2 files modified: web/src/pages/ItemReservePage.tsx and web/src/pages/__tests__/ItemReservePage.test.tsx.
- Backend, Android, i18n, useItemsQuery.ts, StickyReserveBanner.tsx, ConfirmPurchaseBanner.tsx, useReleaseReservation.ts all UNTOUCHED.
- D-06 invariant preserved: no reserver email rendered (the patch nulls reservedBy, strengthening the invariant).
</success_criteria>

<output>
After completion, create `.planning/quick/260516-oiy-optimistic-items-cache-patch-on-release-/260516-oiy-SUMMARY.md` summarizing:
- Root cause (snapshot-lag on React Query items cache across route unmount).
- Fix scope (one file, one new hook, one effect-body change).
- Test added (K-20 with mock setup details).
- Test totals before/after (195 → 196 in ItemReservePage.test.tsx).
- Verification commands run + their pass status.
- Files NOT touched (explicit scope guard list).
- Two-commit TDD trail (RED + GREEN SHAs).
</output>
