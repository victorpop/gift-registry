---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 06
artifact: SUMMARY
status: superseded
superseded_by: [17-07, 17-08]
completed: 2026-05-30
---

# Phase 17 Plan 06 — Deploy and UAT (SUPERSEDED)

## Status

**Superseded by Plan 17-07 + Plan 17-08 (Search v2 re-scope, 2026-05-28).**

Plan 17-06 was the original deploy + 14-scenario on-device UAT for the
Gemini-`google_search`-grounding search path. UAT-6 ("cadou copil 2 ani")
exposed that Gemini hallucinated product titles onto real product IDs
(a Montessori-toy title was returned bound to an eMAG URL that actually
resolved to an Esprit T-shirt). The UAT was paused and the phase
re-scoped: Gemini does intent-only, Google Custom Search JSON API was
chosen as the product source — then closed to new customers (HTTP 403),
forcing a second pivot to Serper.dev `/shopping`.

The deploy work in this plan IS what shipped. Only the search-path internals
and the UAT were replaced.

## Deploy work performed by this plan (still live)

These actions from Plan 17-06 were completed during the original session
and remain in effect:

- `GEMINI_API_KEY` secret created in Secret Manager and bound to the
  `discoverSearch` runtime service account.
- `popularItems` collection backfilled from existing `registries/*/items`
  (12 docs from 13 items, 0 skipped — confirmed in STATE.md before the
  paused UAT).
- All 5 Phase 17 Cloud Functions deployed to `europe-west3`:
  `discoverPopular`, `discoverSearch`, `onItemCreatePopular`,
  `onItemDeletePopular`, `onItemUpdatePopular`. (`discoverSearch` was
  later replaced in-place by the Search v2 deploy under Plan 17-07.)
- Firestore TTL policies created on `discoverCache.cachedAt` and
  `discoverRateLimits.lastWriteAt`.
- Legacy `config/stores` Firestore document deleted (idempotent confirmed).

## Why the UAT was replaced rather than completed

The 14-scenario UAT in this plan validated the hallucinating search
path. Running it against the post-fix code would not have validated the
fix. Plan 17-08 contains the re-UAT that drives the Search v2 fix from
its critical failure case (the "cadou copil 2 ani" hallucination) and
covers the new Serper-shaped behaviors (price guard, allowlist post-
filter, locale parity) instead of the old CSE/Gemini-grounding ones.

## What replaced this plan

- **17-07**: Backend search re-scope — Gemini intent (JSON mode, no
  grounding) → Serper `/shopping` fan-out (≤3) → normalize → 43-store
  hostname allowlist post-filter → de-dupe → 20-result cap → cache.
  Deletes `enrichImages.ts`. Live in `europe-west3`.
- **17-08**: Android price-guard confirmation + on-device re-UAT.
  Verified on `emulator-5554` (8/8 scenarios PASS, including the
  RE-UAT-6 driver) against the deployed Search v2 backend.

Both are documented in their respective `*-SUMMARY.md` files.

## Files / artifacts left behind by this plan

- `.planning/phases/17-.../17-06-UAT-RESULTS.md` — the original UAT
  scaffold with the (paused) 14-scenario checklist. Kept as historical
  record of the failing path; not authoritative for Phase 17
  verification (17-08-UAT-RESULTS.md is).
