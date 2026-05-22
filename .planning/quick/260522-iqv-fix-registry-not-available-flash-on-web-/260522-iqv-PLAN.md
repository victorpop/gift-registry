---
phase: quick/260522-iqv-fix-registry-not-available-flash-on-web-
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/src/features/registry/useRegistryQuery.ts
  - web/src/features/registry/useItemsQuery.ts
  - web/src/features/registry/__tests__/useRegistryQuery.test.ts
  - web/src/features/registry/__tests__/useItemsQuery.test.ts
autonomous: true
requirements:
  - QUICK-260522-iqv-01
must_haves:
  truths:
    - "On first mount of /registry/{id}, the user does NOT see the 'Registry not available' copy while the first onSnapshot is still pending."
    - "useRegistryQuery returns data === undefined until the first onSnapshot arrives (matches the JSDoc contract on lines 7-19)."
    - "useItemsQuery returns data === undefined until the first onSnapshot arrives (parity with useRegistryQuery)."
    - "Once onSnapshot fires with a registry document, data transitions to the mapped Registry object."
    - "Once onSnapshot fires (or errors) with null/[], data transitions to null/[] (real not-found / permission-denied path still works)."
    - "Items collection still resolves to [] when the snapshot delivers an empty docs array."
  artifacts:
    - path: "web/src/features/registry/useRegistryQuery.ts"
      provides: "Hook whose queryFn suspends (returns never-resolving Promise) when cache is empty"
      contains: "new Promise"
    - path: "web/src/features/registry/useItemsQuery.ts"
      provides: "Hook whose queryFn suspends (returns never-resolving Promise) when cache is empty"
      contains: "new Promise"
    - path: "web/src/features/registry/__tests__/useRegistryQuery.test.ts"
      provides: "Test pinning data === undefined on initial mount and the undefined → Registry transition"
      contains: "toBeUndefined"
    - path: "web/src/features/registry/__tests__/useItemsQuery.test.ts"
      provides: "Test pinning data === undefined on initial mount and the undefined → Item[] transition"
      contains: "toBeUndefined"
  key_links:
    - from: "web/src/features/registry/useRegistryQuery.ts"
      to: "web/src/pages/RegistryPage.tsx"
      via: "registryQ.data === null branch (line 246) — must NOT be reached during initial loading"
      pattern: "registryQ\\.data === null"
    - from: "web/src/features/registry/useRegistryQuery.ts"
      to: "web/src/pages/RegistryPage.tsx"
      via: "isInitialLoading = registryQ.data === undefined (line 250) — must be TRUE on first mount"
      pattern: "registryQ\\.data === undefined"
---

<objective>
Eliminate the ~1 second flash of the "Registry not available" copy that appears when a user refreshes `https://gift-registry-ro.web.app/registry/{registryID}`.

**Root cause (pre-diagnosed):** `useRegistryQuery.queryFn` coerces `undefined` → `null` via `?? null`. On first mount TanStack Query calls `queryFn` before the sibling `useEffect` has subscribed to `onSnapshot`. The cache is empty, `getQueryData` returns `undefined`, `?? null` resolves the query to `data: null` (success). `RegistryPage.tsx:246-248` then renders `<NotFoundPage />` until the first snapshot arrives.

**Fix:** Make `queryFn` return a never-resolving Promise when the cache is empty. TanStack Query holds the query in `pending` state (data === undefined) until `setQueryData(...)` is called from the `onSnapshot` callback, which transitions the query to `success` with the real value. Apply the symmetric fix to `useItemsQuery` (same `?? []` anti-pattern on line 36) for parity.

Purpose: Bring the implementation into line with the JSDoc contract already documented on the hooks (`data === undefined while the first snapshot has not arrived`). RegistryPage's existing render branches (`data === undefined` ⇒ skeletons; `data === null` ⇒ NotFoundPage; `data` truthy ⇒ content) become correct as a consequence — no page-level changes needed.

Output:
- Two edited hook files (1-line behavioral change in each `queryFn`).
- Updated test suites pinning the new contract.
- All pre-existing tests still pass (RegistryPage.test.tsx, App.test.tsx).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@web/src/features/registry/useRegistryQuery.ts
@web/src/features/registry/useItemsQuery.ts
@web/src/features/registry/__tests__/useRegistryQuery.test.ts
@web/src/features/registry/__tests__/useItemsQuery.test.ts
@web/src/features/registry/__tests__/RegistryPage.test.tsx
@web/src/__tests__/App.test.tsx
@web/src/pages/RegistryPage.tsx
@web/package.json

<interfaces>
<!-- The exact contract both hooks now promise. Lifted from useRegistryQuery.ts JSDoc (lines 7-19) — extended for items. -->

```typescript
// useRegistryQuery — returns UseQueryResult<Registry | null>
//   data === undefined  → first onSnapshot has not arrived yet (initial loading)
//   data === null       → not-found OR permission-denied (WEB-D-13 + WEB-D-14)
//   data === Registry   → readable registry
// useItemsQuery — returns UseQueryResult<Item[]>
//   data === undefined  → first onSnapshot has not arrived yet (initial loading)
//   data === []         → real empty collection (no items in the registry)
//   data === Item[]     → loaded items
```

The downstream consumer (`RegistryPage.tsx`) already branches on exactly these three states:

```typescript
// web/src/pages/RegistryPage.tsx:244-250
if (registryQ.data === null) {
  return <NotFoundPage />
}
const isInitialLoading = registryQ.data === undefined
```

DO NOT touch RegistryPage. Its branches are already correct; this plan brings the hooks into compliance.
</interfaces>

<test_helpers_to_keep>
<!-- Pre-existing mock plumbing the new tests should reuse. Do NOT change the mocks. -->

- `vi.hoisted` snapshot handles (`snapshotHandles` / `itemHandles`) — already capture `onNext`/`onError` and expose `unsubscribe`.
- The shared `wrapper(client)` factory + `QueryClient` instantiation in `beforeEach`.
- `firebase/firestore` mock (returns the captured callbacks from `onSnapshot`).
</test_helpers_to_keep>

<hard_constraints>
- DO NOT modify `web/src/pages/RegistryPage.tsx`.
- DO NOT modify the `onSnapshot` callbacks or the `mapRegistrySnapshot` / `mapItemSnapshot` helpers.
- DO NOT change the `queryKey` shape on either hook.
- DO NOT modify `web/src/queryClient.ts` — `staleTime: Infinity` is intentional.
- DO NOT change any i18n strings — "Registry not available" copy is correct for the genuine not-found case.
- DO preserve the JSDoc block on `useRegistryQuery.ts:7-19`. Add an equivalent JSDoc block to `useItemsQuery.ts` (it currently has none) documenting the same contract for items.
- Keep commits atomic per file: one commit for `useRegistryQuery.ts` + its tests, one commit for `useItemsQuery.ts` + its tests.
</hard_constraints>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Fix useRegistryQuery + update tests to pin the data === undefined contract</name>
  <files>
    web/src/features/registry/useRegistryQuery.ts
    web/src/features/registry/__tests__/useRegistryQuery.test.ts
  </files>
  <behavior>
    - Test 1 (NEW): On mount with no cached data, before any `snapshotHandles.onNext` invocation, `result.current.data === undefined` (NOT null). This is the regression test for the "Registry not available" flash bug.
    - Test 2 (NEW): After mount, calling `queryClient.setQueryData(['registry', id], null)` externally still produces `result.current.data === null` (preserves the not-found / permission-denied path).
    - Test 3 (EXISTING — must still pass): "returns registry data when first snapshot arrives" — the onSnapshot → setQueryData → data transition.
    - Test 4 (EXISTING — must still pass): "returns null data when permission-denied error fires (WEB-D-14)".
    - Test 5 (EXISTING — must still pass): "returns null data when not-found error fires (WEB-D-13)".
    - Test 6 (EXISTING — must still pass): "calls unsubscribe exactly once on unmount".
    - Test 7 (EXISTING — must still pass): "maps snapshot with exists() false to null (deleted registry)".
  </behavior>
  <action>
    **Step 1 — Add the new failing test FIRST (TDD red).** Open `web/src/features/registry/__tests__/useRegistryQuery.test.ts` and add two new `it` blocks INSIDE the existing `describe('useRegistryQuery', ...)` block (no new describe). Place them at the top, before the existing "returns registry data when first snapshot arrives" test:

    ```typescript
    it('returns data === undefined on mount before the first snapshot arrives (regression: no "Registry not available" flash)', async () => {
      const { result } = renderHook(() => useRegistryQuery('reg-pending'), { wrapper: wrapper(client) })
      // Wait for the useEffect to register the subscription so we know the mount completed.
      await waitFor(() => expect(snapshotHandles.onNext).not.toBeNull())
      // CRITICAL: data must be undefined (initial loading), NOT null (NotFoundPage trigger).
      // Do NOT fire snapshotHandles.onNext — we are testing the pre-snapshot state.
      expect(result.current.data).toBeUndefined()
    })

    it('transitions to data === null when setQueryData(key, null) is called externally (preserves WEB-D-13 + WEB-D-14)', async () => {
      const { result } = renderHook(() => useRegistryQuery('reg-explicit-null'), { wrapper: wrapper(client) })
      await waitFor(() => expect(snapshotHandles.onNext).not.toBeNull())
      // Simulate the onError path by directly calling the error handler (which calls setQueryData(key, null)).
      snapshotHandles.onError!({ code: 'permission-denied', message: 'Denied' })
      await waitFor(() => expect(result.current.data).toBeNull())
    })
    ```

    Run `cd web && npm test -- useRegistryQuery` and confirm the first new test FAILS with `expected undefined to be undefined` reporting actual `null` (red). The second new test is largely redundant with existing tests but documents intent — it should already pass.

    **Step 2 — Apply the fix (TDD green).** Open `web/src/features/registry/useRegistryQuery.ts`. Replace lines 52-58 (the `return useQuery<Registry | null>({...})` block) with:

    ```typescript
      return useQuery<Registry | null>({
        queryKey: queryKey as unknown as readonly unknown[],
        // Passive reader — the real source of truth is the onSnapshot callback above.
        // When the cache has no value yet (initial mount, before onSnapshot fires),
        // return a never-resolving Promise so the query stays in 'pending' state
        // (data === undefined) instead of resolving to null. The onSnapshot callback
        // will call setQueryData(key, value), which transitions the query to 'success'
        // with the real value. This honors the JSDoc contract above and prevents the
        // RegistryPage NotFoundPage branch from firing during initial loading.
        queryFn: () => {
          const cached = queryClient.getQueryData<Registry | null>(
            queryKey as unknown as readonly unknown[],
          )
          if (cached === undefined) {
            return new Promise<Registry | null>(() => {})
          }
          return cached
        },
        enabled: Boolean(registryId),
      })
    ```

    Do NOT touch the JSDoc on lines 7-19 — leave it intact.
    Do NOT touch the `useEffect` (lines 24-50).
    Do NOT touch the `queryKey` computation (line 22).

    **Step 3 — Verify (TDD green).** Run `cd web && npm test -- useRegistryQuery`. All 7 tests (5 existing + 2 new) must pass.

    **Fallback (only if the never-resolving Promise approach fails any test):** If TanStack Query v5 does NOT transition `pending → success` when `setQueryData` is called on a query whose `queryFn` is mid-flight, swap the approach to:

    ```typescript
      queryFn: () => {
        const cached = queryClient.getQueryData<Registry | null>(
          queryKey as unknown as readonly unknown[],
        )
        // If cache empty, throw a sentinel that React Query won't catch — but actually
        // the simpler v5-compatible fallback is to just use `initialData: undefined` and
        // a deferred queryFn. Concretely: keep `queryFn` returning the never-resolving
        // Promise, but ALSO set `notifyOnChangeProps: ['data']` to ensure setQueryData
        // re-renders consumers.
        if (cached === undefined) return new Promise<Registry | null>(() => {})
        return cached
      },
    ```

    If the fallback is needed, document it in SUMMARY.md under a "Fallback engaged" section explaining which test forced the deviation.

    Reference: This implements requirement QUICK-260522-iqv-01 per the pre-diagnosed root cause.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit && npm test -- --run useRegistryQuery</automated>
  </verify>
  <done>
    - `web/src/features/registry/useRegistryQuery.ts` contains `new Promise<Registry | null>(() => {})` inside `queryFn` (grep confirms).
    - `web/src/features/registry/__tests__/useRegistryQuery.test.ts` contains the two new `it` blocks; the regression test asserts `expect(result.current.data).toBeUndefined()`.
    - `npx tsc --noEmit` exits 0.
    - `npm test -- --run useRegistryQuery` reports all tests passing.
    - Commit: `fix(quick/260522-iqv): stop coercing undefined → null in useRegistryQuery.queryFn`
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Fix useItemsQuery (parity) + update tests + full repo verification</name>
  <files>
    web/src/features/registry/useItemsQuery.ts
    web/src/features/registry/__tests__/useItemsQuery.test.ts
  </files>
  <behavior>
    - Test 1 (NEW): On mount with no cached data, before any `itemHandles.onNext` invocation, `result.current.data === undefined`. This pins the contract symmetrical to useRegistryQuery.
    - Test 2 (EXISTING — must still pass): "maps items from snapshot.docs" — onSnapshot → data transition with non-empty docs.
    - Test 3 (EXISTING — must still pass): "returns [] on empty collection (not undefined)" — when `onNext` fires with `docs: []`, `setQueryData(key, [])` is called and `data === []`. This is the empty-registry case and is genuinely different from "still loading".
    - Test 4 (EXISTING — must still pass): "does not double-subscribe when the hook re-renders with same registryId".
  </behavior>
  <action>
    **Step 1 — Add the new failing test FIRST (TDD red).** Open `web/src/features/registry/__tests__/useItemsQuery.test.ts` and add ONE new `it` block at the top of the existing `describe('useItemsQuery', ...)` block, before the "maps items from snapshot.docs" test:

    ```typescript
    it('returns data === undefined on mount before the first snapshot arrives (parity with useRegistryQuery — distinguishes loading from empty)', async () => {
      const { result } = renderHook(() => useItemsQuery('reg-pending'), { wrapper: wrapper(client) })
      // Wait for useEffect to register the subscription so we know the mount completed.
      await waitFor(() => expect(itemHandles.onNext).not.toBeNull())
      // CRITICAL: data must be undefined (initial loading), NOT [] (which means "registry has no items").
      // Do NOT fire itemHandles.onNext — we are testing the pre-snapshot state.
      expect(result.current.data).toBeUndefined()
    })
    ```

    Run `cd web && npm test -- useItemsQuery` and confirm the new test FAILS reporting actual `[]` (red).

    **Step 2 — Apply the fix (TDD green).** Open `web/src/features/registry/useItemsQuery.ts`. Replace lines 33-38 (the `return useQuery<Item[]>({...})` block) with:

    ```typescript
      return useQuery<Item[]>({
        queryKey: queryKey as unknown as readonly unknown[],
        // Passive reader — the real source of truth is the onSnapshot callback above.
        // When the cache has no value yet (initial mount, before onSnapshot fires),
        // return a never-resolving Promise so the query stays in 'pending' state
        // (data === undefined). Returning [] here would conflate "still loading" with
        // "registry has no items", breaking downstream loading-state branches.
        queryFn: () => {
          const cached = queryClient.getQueryData<Item[]>(
            queryKey as unknown as readonly unknown[],
          )
          if (cached === undefined) {
            return new Promise<Item[]>(() => {})
          }
          return cached
        },
        enabled: Boolean(registryId),
      })
    ```

    **Step 3 — Add JSDoc to useItemsQuery.ts.** The hook currently lacks the documentation block that `useRegistryQuery` has. Add this JSDoc IMMEDIATELY above the `export function useItemsQuery(...)` line:

    ```typescript
    /**
     * Real-time items subscription wrapped in TanStack Query cache.
     *
     * - onSnapshot lives in useEffect (unsubscribes on unmount; single subscription per registry).
     * - Successful snapshots call queryClient.setQueryData — the queryFn is a passive reader.
     * - On error (permission-denied, unavailable, etc.) data resolves to [] — clients see an empty
     *   list rather than a partial UI; the parent useRegistryQuery owns the not-found 404 branch.
     * - staleTime: Infinity + refetchOn*: false are inherited from the global QueryClient defaults.
     *
     * Returns:
     *   - data === undefined while the first snapshot has not arrived (initial loading)
     *   - data === [] when the snapshot is empty OR an error occurred
     *   - data === Item[] when items load successfully
     */
    ```

    Do NOT touch the `useEffect` (lines 11-31).
    Do NOT touch the `queryKey` computation (line 9).
    Do NOT touch the onSnapshot error handler (line 26 still calls `setQueryData(key, [])` — that's correct for the error case).

    **Step 4 — Verify per-file (TDD green).** Run `cd web && npm test -- --run useItemsQuery`. All 4 tests (3 existing + 1 new) must pass.

    **Step 5 — FULL repo verification (mandatory before done).** Run, in order:
      1. `cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit` → exit 0
      2. `cd /Users/victorpop/ai-projects/gift-registry/web && npm test -- --run` → all suites green, in particular: `useRegistryQuery.test.ts`, `useItemsQuery.test.ts`, `RegistryPage.test.tsx`, `App.test.tsx`

    `App.test.tsx:92-98` is the most important pre-existing canary: it asserts that visiting `/registry/abc123` does NOT show "Registry not available" — the bug this plan fixes is exactly the violation that test was protecting against (the test passed despite the bug because the mock returned `{ data: undefined, isLoading: true }` directly, bypassing the buggy queryFn). It must remain green.

    `RegistryPage.test.tsx:121-126` ("renders NotFoundPage when registry data is null") must also remain green — the real null path (permission-denied / not-found error) is preserved by the fix.

    **Fallback:** If `useItemsQuery` exhibits the same TanStack v5 quirk noted in Task 1, apply the same approach. Document in SUMMARY.md if the fallback is engaged.

    Reference: This implements requirement QUICK-260522-iqv-01 (parity arm).
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit && npm test -- --run</automated>
  </verify>
  <done>
    - `web/src/features/registry/useItemsQuery.ts` contains `new Promise<Item[]>(() => {})` inside `queryFn` AND a JSDoc block immediately above the `export function` line (grep confirms both).
    - `web/src/features/registry/__tests__/useItemsQuery.test.ts` contains the new "returns data === undefined on mount" `it` block.
    - `npx tsc --noEmit` exits 0.
    - `npm test -- --run` reports the entire web suite green — including the previously-existing `App.test.tsx` and `RegistryPage.test.tsx` cases that exercise both the loading and not-found paths.
    - Commit: `fix(quick/260522-iqv): stop coercing undefined → [] in useItemsQuery.queryFn + add JSDoc`
    - SUMMARY.md notes whether the primary `new Promise(() => {})` approach worked or a fallback was engaged.
  </done>
</task>

</tasks>

<verification>
After both tasks complete:

1. `cd /Users/victorpop/ai-projects/gift-registry/web && npx tsc --noEmit` → exit 0.
2. `cd /Users/victorpop/ai-projects/gift-registry/web && npm test -- --run` → all suites green.
3. Spot-grep guardrails (must all return non-empty hits — confirms hard_constraints were honored):
   - `grep -n "new Promise" web/src/features/registry/useRegistryQuery.ts` → 1 match
   - `grep -n "new Promise" web/src/features/registry/useItemsQuery.ts` → 1 match
   - `grep -n "toBeUndefined" web/src/features/registry/__tests__/useRegistryQuery.test.ts` → ≥1 match
   - `grep -n "toBeUndefined" web/src/features/registry/__tests__/useItemsQuery.test.ts` → ≥1 match
4. Spot-grep that RegistryPage.tsx was NOT touched:
   - `git diff --stat web/src/pages/RegistryPage.tsx` → empty
5. Spot-grep that queryClient.ts was NOT touched:
   - `git diff --stat web/src/queryClient.ts` → empty
6. Spot-grep that i18n strings were NOT touched:
   - `git diff --stat web/src/i18n web/i18n` → empty
</verification>

<success_criteria>
- Refreshing a public registry URL on the deployed web app no longer shows the "Registry not available" copy before the registry loads. (Cannot be verified locally without a live test, but the unit-test regression now pins the underlying contract.)
- Both hooks expose `data === undefined` on first mount until `onSnapshot` delivers a value.
- The genuine not-found and permission-denied paths still produce `data === null` (registry) and `data === []` (items).
- `npx tsc --noEmit` clean; `npm test -- --run` clean.
- Two atomic commits land: one per modified hook + its tests.
</success_criteria>

<output>
After completion, create `.planning/quick/260522-iqv-fix-registry-not-available-flash-on-web-/260522-iqv-SUMMARY.md` summarizing:
- The before/after diff snippet for each `queryFn`.
- Confirmation that the primary `new Promise(() => {})` approach worked (or, if not, which fallback was engaged and why).
- Test counts before/after (new tests added, all passing).
- Note for the user: this fix is unit-test verified. Live verification on `https://gift-registry-ro.web.app/registry/{registryID}` requires a hosting redeploy (`cd web && npm run build && firebase deploy --only hosting`) — surface as a manual next-step in SUMMARY.md.
</output>
