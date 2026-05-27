---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 04
subsystem: backend
tags: [firebase-functions, firestore, triggers, admin-sdk, typescript, jest, popular-items, backfill]

requires:
  - phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini-plan-02
    provides: normalizeUrl({productId, canonicalUrl}) — used to compute productId for every trigger and every backfill row
provides:
  - onItemCreatePopular / onItemDeletePopular / onItemUpdatePopular Firestore v2 triggers (europe-west3)
  - handleItemCreate / handleItemDelete / handleItemUpdate testable handlers (injectable Firestore)
  - One-shot backfillPopularItems script (collectionGroup scan → batched set+merge)
  - npm script `backfill:popular`
  - 3 new named exports in functions/src/index.ts (alphabetically below the discover Callables)
affects: [17-06 (deploy ordering: backfill BEFORE triggers), 17-03 (popularItems is the read source for discoverPopular)]

tech-stack:
  added: []
  patterns:
    - "Testable trigger split — pure handler(db, ...) + thin onDocument* wrapper for unit-testability without firebase-functions runtime"
    - "FieldValue-sentinel idempotency guard — check registryIds before increment(1) because increment is NOT idempotent on re-fire"
    - "Delete-on-zero counter — drop popularItems doc when registryCount hits 0 to keep the collection compact"
    - "One-shot Admin SDK script with 500-doc batch limit and set+merge for idempotent re-runs"

key-files:
  created:
    - functions/src/discover/triggers.ts
    - functions/src/__tests__/discover/triggers.test.ts
    - functions/scripts/backfillPopularItems.ts
  modified:
    - functions/src/index.ts (+3 trigger exports below the discover Callables)
    - functions/package.json (+1 npm script: backfill:popular)

key-decisions:
  - "Triggers use Firestore runTransaction (NOT batch) so the read-then-write logic for the idempotency guard and the delete-on-zero check is atomic"
  - "Backfill aggregates in-memory via Map<productId, {registryIds: Set<string>, denorm fields}>; first item observed wins for title/imageUrl/price"
  - "Backfill silently skips items with missing or malformed originalUrl (logs a warning per skipped item) — never aborts the entire scan"

patterns-established:
  - "Pure-handler + trigger-wrapper split — enables Jest tests with a fake Firestore, no firebase-functions runtime needed"
  - "Idempotency guard on FieldValue.increment — always re-read the source-of-truth set before touching a denormalized counter"

requirements-completed: [D-17, D-18, D-19, D-22, D-47]

duration: 4min
completed: 2026-05-27
---

# Phase 17 Plan 04: Popular-Items Triggers + Backfill Summary

**Firestore-triggered popularItems counter (onCreate / onDelete / onUpdate) with idempotency guards, delete-on-zero cleanup, and a one-shot Admin SDK backfill script that aggregates by SHA-256 canonical productId.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-05-27T14:44:55Z
- **Completed:** 2026-05-27T14:48:35Z
- **Tasks:** 2 (Task 1 TDD with RED+GREEN commits; Task 2 single commit)
- **Files modified:** 5 (3 created, 2 modified)

## Accomplishments

- Three Firestore-triggered Functions wired to `registries/{registryId}/items/{itemId}` in `europe-west3`, exported from `functions/src/index.ts`.
- Three pure handler functions (`handleItemCreate` / `handleItemDelete` / `handleItemUpdate`) accepting an injected `Firestore` instance — unit-testable without the firebase-functions runtime.
- D-18 idempotency invariant: re-firing onCreate for the same (productId, registryId) pair is a no-op; the registryIds membership check guards every `FieldValue.increment(1)` call.
- D-18 delete-on-zero: when the decrement would bring `registryCount` to 0, the popularItems doc is deleted entirely rather than left as a zero-count corpse.
- D-19 update semantics: trigger fires on every item document update but short-circuits unless `originalUrl` actually changed; URL change is implemented as `handleItemDelete(old) + handleItemCreate(new)`.
- D-22 backfill script: scans `collectionGroup("items")`, aggregates into a `Map<productId, Aggregate>` in memory, writes back in 500-document batches with `set(..., { merge: true })` so re-runs are safe.
- 15/15 trigger unit tests passing; 77/77 discover-suite tests passing in aggregate; `npm run build` green; `npx ts-node --transpileOnly scripts/backfillPopularItems.ts` compiles and runs to the GCP-auth boundary (matches `deleteConfigStores.ts` precedent).

## Task Commits

Each task was committed atomically (parallel-agent coordination: `--no-verify` on each commit):

1. **Task 1 (TDD RED): trigger tests** — `ab2493f` (test): 15 failing tests with fake Firestore + transaction op recorder.
2. **Task 1 (TDD GREEN): trigger implementation + index.ts exports** — `aef1d98` (feat): handlers + Firestore v2 wrappers; 3 named exports appended alphabetically below the discover Callables.
3. **Task 2: backfill script + npm script** — `749e26f` (feat): `functions/scripts/backfillPopularItems.ts` + `backfill:popular` package.json entry.

**Plan metadata:** see final commit below.

_TDD note: Task 1 used the RED→GREEN cycle and produced two commits as expected. No refactor commit was needed — the GREEN implementation matched the plan's spec verbatim._

## Files Created/Modified

- `functions/src/discover/triggers.ts` — 3 pure handlers + 3 Firestore v2 trigger wrappers. Document path: `registries/{registryId}/items/{itemId}` in `europe-west3`. All counter writes go through `runTransaction` for atomicity.
- `functions/src/__tests__/discover/triggers.test.ts` — 15 unit tests with a custom fake Firestore that records every `set`/`update`/`delete` op for assertion. Covers idempotency, delete-on-zero, URL normalization integration, undefined/empty `originalUrl` handling, and the full `handleItemUpdate` matrix (no-change, URL-only change, empty-before, empty-after).
- `functions/scripts/backfillPopularItems.ts` — Admin SDK script. `collectionGroup("items").get()` → in-memory aggregation → `batch.set(..., { merge: true })` in 500-doc commits. Skips items without `originalUrl` (with a per-item warning log) and gracefully handles malformed URLs by skipping rather than aborting.
- `functions/src/index.ts` — added 3 named exports (`onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`) below the `discoverPopular` / `discoverSearch` Callables that plan 17-03 added in parallel. Alphabetical order within the trigger group; no other exports touched.
- `functions/package.json` — added `backfill:popular` npm script (`ts-node scripts/backfillPopularItems.ts`).

## Decisions Made

- **Firestore document path:** `registries/{registryId}/items/{itemId}` (matches the Phase 3 storage layer for `Item` docs).
- **Aggregation strategy in backfill:** in-memory `Map<productId, Aggregate>` where each `Aggregate` carries the canonical URL, denorm fields (first-observed wins for title / imageUrl / price), and a `Set<string>` of registryIds. Set semantics dedupe automatically — a registry that has the same product added twice (e.g. via different utm-tagged URLs) still contributes 1 to the count.
- **500-doc batch ceremony:** track `inBatch` counter, on hitting 500 commit + reset + log progress, after the loop commit any remainder. Mirrors Firestore's hard batch limit.
- **Atomicity model:** every trigger handler uses `db.runTransaction` (NOT `db.batch`) because the idempotency guard and the delete-on-zero check both require reading the current state inside the same atomic operation as the write. `runTransaction` is supported in Firestore triggers; no deviation from D-18.
- **Out-of-band staleness on update:** D-19 explicitly accepts that title/image/price changes on an item do NOT refresh the denormalized fields in popularItems. The next create event from any registry will refresh them via `handleItemCreate`'s "most-recent-write wins" branch.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The parallel coordination with plan 17-03 on `functions/src/index.ts` worked cleanly — plan 17-03's two callable exports (`discoverPopular`, `discoverSearch`) landed before this plan's trigger exports, so the final file shows callables on lines 25-26 and triggers on lines 27-31 in alphabetical group order. No git conflict.

## Deploy-ordering reminder for plan 17-06

**CRITICAL — D-22 verbatim:** The backfill script MUST run BEFORE the triggers are deployed. If the triggers go live first and then the backfill runs, every backfill `batch.set` will race with the onCreate trigger that fires for ongoing user item additions, causing double-counts. Recommended deploy ceremony in plan 17-06:

1. Run `firebase functions:secrets:set GEMINI_API_KEY` (plan 17-03 prerequisite).
2. Deploy *only* the Callables (`discoverPopular`, `discoverSearch`) — skip the triggers.
3. From the local CLI: `cd functions && npm run backfill:popular` (with `gcloud auth application-default login` first for prod).
4. Confirm `popularItems` collection is populated via the Firebase console.
5. Deploy the three triggers (`onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`) — `firebase deploy --only functions:onItemCreatePopular,functions:onItemDeletePopular,functions:onItemUpdatePopular`.

The backfill is idempotent (`set + merge`), so re-running it after the triggers are live is safe — but the gap between backfill start and trigger deploy must be small to minimize the count discrepancy window.

## User Setup Required

None — the deployment ceremony above (when plan 17-06 runs) is the only manual step.

## Next Phase Readiness

- `popularItems` data source for `discoverPopular` (plan 17-03) is fully wired — once plan 17-06 runs the backfill, the Callable will start returning real data.
- Plan 17-05 (Android UI) is unblocked — it only depends on the Callable contract from 17-03, not on this plan's triggers.
- Plan 17-06 must consume the deploy-ordering note above.

## Self-Check: PASSED

All 6 files exist on disk; all 3 task commits resolve in `git log --all`.

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Completed: 2026-05-27*
