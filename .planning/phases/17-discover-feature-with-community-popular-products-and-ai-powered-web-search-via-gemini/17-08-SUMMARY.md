---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 08
artifact: SUMMARY
status: complete
completed: 2026-05-30
---

# Phase 17 Plan 08 — Android price guard + on-device re-UAT (Search v2)

## Objective

Confirm the Android Discover card guards against the no-price case (`price <= 0` must not render "0,00 RON"), then run the on-device re-UAT that re-validates the UAT-6 hallucination driver plus the new Search v2 scenarios against the live deployed backend from Plan 17-07.

## Outcome

**uat pass** — 8/8 scenarios PASS including the critical RE-UAT-6 driver. The Search v2 re-scope (Gemini intent + Serper.dev `/shopping`) is verified on-device. The UAT-6 hallucination is structurally gone.

## Tasks executed

| Task | Type | Result |
|------|------|--------|
| 1 — Confirm price guard at `DiscoverProductCard.kt:112` | grep-confirmation | PASS, no edit (guard was already present from Plan 17-05, commit 96448bc) |
| 2 — Build debug APK with `-Puse_emulator=false` | gradle build | PASS, `app-debug.apk` produced; installed on `emulator-5554` |
| 3 — On-device re-UAT (human-verify checkpoint) | human checkpoint | PASS, all 8 scenarios |

## Files touched

| Path | Change |
|------|--------|
| `.planning/phases/17-.../17-08-UAT-RESULTS.md` | filled in (8 PASS, mid-UAT refinements documented) |
| `.planning/phases/17-.../17-08-SUMMARY.md` | this file |

No source code changes were required by this plan. (The price guard already existed; the 20-result cap shipped under Plan 17-07's umbrella — commit `c4e9077` — as a UAT-driven refinement.)

## Mid-UAT refinements (logged in UAT-RESULTS)

Two issues surfaced during the on-device UAT and were resolved live:

1. **Stale `discoverCache`** masked the new backend on the first RE-UAT-6 attempt. Function logs showed `Callable request verification passed` with no subsequent Serper logs — cache hits short-circuiting the new pipeline. Resolved with `firebase firestore:delete discoverCache --recursive --force`. Future search-backend changes need to plan for a cache purge (30-day TTL).
2. **Result list uncapped** at ~30 products. Added `MAX_SEARCH_RESULTS = 20` in `search.ts` + unit test in `serperOrchestration.test.ts`, deployed, re-purged the cache. Serper cost is unchanged (already capped at ≤3 fan-out queries); the cap bounds the cached doc and client payload.

## Verification status

- All 8 UAT scenarios PASS on-device (see UAT-RESULTS.md for per-scenario detail).
- Backend test suite green: `cd functions && npm test -- --testPathPattern discover` → 118/118 across 10 suites.
- Live deployment confirmed: `discoverSearch` revision serves Gemini intent + Serper `/shopping` + 20-result cap, with both `GEMINI_API_KEY` and `SERPER_API_KEY` bound.
- Android contract unchanged — zero Android edits in this plan.

## Next

Phase-level verification (`gsd-verifier`) → mark Phase 17 complete in ROADMAP + STATE.
