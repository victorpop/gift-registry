---
phase: quick-260525-gqs
plan: "01"
subsystem: functions
status: partial — awaiting Task 3 on-device verification
tags:
  - functions
  - url-metadata
  - emag
  - meta-refresh
  - tdd
dependency_graph:
  requires: []
  provides:
    - fetchOgMetadata meta-refresh follow (up to 3 hops, same-origin)
  affects:
    - Android AddItemScreen paste-URL auto-fill flow
tech_stack:
  added: []
  patterns:
    - "extractMetaRefreshTarget helper: loop over meta tags, case-insensitive http-equiv, regex on content attr"
    - "3-hop loop with same-origin guard in fetchOgMetadataHandler"
    - "fetchOgMetadataHandler named export for test isolation (onCall wrapper delegates)"
key_files:
  created:
    - functions/src/__tests__/fetchOgMetadata.test.ts
  modified:
    - functions/src/registry/fetchOgMetadata.ts
decisions:
  - "Used fetchOgMetadataHandler named export (not .run()) — cleaner test isolation without firebase-functions-test harness; no production impact"
  - "Double-quote HTML attribute test uses single-quote outer delimiter to avoid malformed HTML that node-html-parser truncates at inner quote boundary"
  - "MAX_HOPS=3: caps wall-time at 3x10s=30s, well under 60s callable default; covers all real-world EMAG redirect chains observed"
  - "Same-origin guard uses URL.origin comparison (scheme+host+port) — prevents open-redirect exploitation of the hop follower"
metrics:
  duration: "~6 minutes"
  completed_date: "2026-05-25"
  tasks_completed: 2
  tasks_total: 3
  files_created: 2
  files_modified: 1
---

# Quick 260525-gqs: Fix URL Metadata Fetch Error in Add Item — Summary

**One-liner:** Added HTML meta-refresh follow (3 hops, same-origin) to fetchOgMetadata so EMAG stale-slug product URLs auto-fill title+image instead of returning the empty "No details found" fallback.

## Status: PARTIAL — Awaiting Task 3 On-Device Verification

Tasks 1 (TDD + implementation) and 2 (deploy) are complete. Task 3 requires human on-device verification.

## Root Cause

EMAG re-slugs product URLs whenever the SKU title is edited (e.g., model-year bumps: iPhone 16 → iPhone 17 Pro Max). Old URLs return an HTML page containing `<meta http-equiv="refresh" content="0;url='/canonical-slug/pd/CODE/'">` instead of an HTTP 301/302. Node.js `fetch()` follows HTTP redirects automatically but does NOT follow HTML meta-refresh. The Cloud Function was parsing the tiny redirect-stub page (no OG tags) and returning `{title: null, imageUrl: null, ...}`. The Android client then showed "No details found for that URL" even though the destination page (one hop away) had full OG metadata.

Confirmed via curl with the function's exact User-Agent on 2026-05-25:
- Request: `https://www.emag.ro/iphone-16-pro-max-256gb-5g-cosmic-orange-rnfyn4zd-a/pd/DC99FV3BM/`
- Server returns: `<meta http-equiv="refresh" content="0;url='/telefon-mobil-apple-iphone-17-pro-max-256gb-5g-cosmic-orange-mfyn4zd-a/pd/DC99FV3BM/?'">`
- Destination (one hop): full `og:title`, `og:image` present

## Implementation

### `fetchOgMetadata.ts` changes

**New `extractMetaRefreshTarget(root)` helper:**
- Loops all `<meta>` tags, filters by lowercased `http-equiv === "refresh"` (case-insensitive attribute name)
- Regex: `/^\s*\d+\s*;\s*url\s*=\s*['"]?([^'"\s>]+)['"]?\s*$/i` — handles `0;url=/x`, `0; url='/x'`, `0; URL="/x"`, `5; url=/x?y=1`
- Returns raw (possibly relative) URL string or null

**New 3-hop loop in `fetchOgMetadataHandler`:**
- `MAX_HOPS = 3` — caps total fetches at 3 (initial + up to 2 follow-ups), 30s max wall-time
- On each hop: detects meta-refresh, resolves relative URL via `new URL(target, currentUrl)`, checks same-origin, logs and continues
- Same-origin guard: `new URL(currentUrl).origin !== resolved.origin` → warn + return empty (prevents open-redirect abuse)
- Max-hop guard: hits on `hop === MAX_HOPS` → warn + return empty
- After loop: `finalRoot` holds the last non-meta-refresh response; OG extraction + price resolution run against `finalRoot`

**`fetchOgMetadataHandler` named export:**
- Body extracted from the `onCall` arrow into a standalone `export async function fetchOgMetadataHandler(request)`
- The `onCall` wrapper now delegates: `async (request) => fetchOgMetadataHandler(request)`
- No production behavior change; enables direct test invocation without firebase-functions-test harness

All existing helpers (`resolvePrice`, `findPriceInJsonLd`, `parsePriceString`, `normalizeCurrency`, `extractAmount`, `normalizeImageUrl`, `formatPriceForDisplay`) unchanged.

### `fetchOgMetadata.test.ts` — 9 specs

| # | Spec | Behavior Tested |
|---|------|-----------------|
| 1 | happy path: og:title + og:image on first response | Existing happy path doesn't regress |
| 2 | meta-refresh: follows one hop, parses OG from destination | Core fix — single hop, relative URL resolution, 2 fetch calls |
| 3a | content variant: `0; url=...` (space, no quotes) | Space-separated, unquoted URL |
| 3b | content variant: `0;URL='...'` (uppercase key) | Case-insensitive key matching |
| 3c | content variant: `0;url="..."` (double quotes) | Double-quote URL delimiter |
| 4 | max 3 fetches total, then returns empty | Hop ceiling, console.warn "max hops", exactly 3 calls |
| 5 | cross-origin redirect NOT followed | Same-origin guard, console.warn "cross-origin", exactly 1 call |
| 6 | non-meta-refresh empty page returns empty | No regression on pages without meta-refresh or OG tags |
| 7 | fetch throws (network error) returns empty | Outer catch block, console.error with error message |

**Deviation noted:** The double-quote test (spec 3c) uses single-quote outer HTML attribute delimiter (e.g., `content='0;url="/dest/"'`) to avoid producing malformed HTML that `node-html-parser` would truncate at the inner `"` boundary. This correctly represents how real-world pages encode double-quoted URLs in attributes.

## Test Results

```
PASS src/__tests__/fetchOgMetadata.test.ts
  fetchOgMetadata — meta-refresh + happy path + empty fallback
    ✓ happy path: returns og:title + og:image when present on first response
    ✓ meta-refresh: follows one hop and parses OG tags from destination
    ✓ meta-refresh: handles content variant — space no quotes
    ✓ meta-refresh: handles content variant — uppercase URL key with single quotes
    ✓ meta-refresh: handles content variant — double quotes
    ✓ meta-refresh: max 3 fetches total, then returns empty
    ✓ meta-refresh: cross-origin redirect is NOT followed
    ✓ non-meta-refresh empty page: returns empty (no regression)
    ✓ fetch throws (network error): returns empty shape, does not crash

Tests: 9 passed
```

Full suite: 107/110 passing (3 pre-existing failures in `createReservation.test.ts` — unrelated Firebase app init issue, present before this quick).

TypeScript: `tsc --noEmit` exits 0. Build: `npm run build` exits 0.

## Deploy

- **Command:** `firebase deploy --only functions:fetchOgMetadata --project gift-registry-ro`
- **Result:** `✔ Deploy complete!` — `functions[fetchOgMetadata(europe-west3)] Successful update operation.`
- **Verified:** `firebase functions:list` shows `fetchOgMetadata | v2 | callable | europe-west3 | nodejs22`
- **Advisory:** `firebase-functions` outdated notice — non-blocking (same as Phase 16-06)
- **Scope:** Only `fetchOgMetadata` redeployed; no other functions, rules, hosting, or Firestore indexes touched

## Out of Scope

**EMAG price extraction** — EMAG ships price via JS globals (`EM.used_offers`), not OG tags. The `priceAmount`/`priceCurrency` fields will remain null for EMAG even with this fix. This is explicitly out of scope for this quick. File as a separate todo if desired.

## Commits

| Hash | Type | Description |
|------|------|-------------|
| f3c1ce4 | test | RED — pin meta-refresh follow contract |
| f041eee | feat | Follow HTML meta-refresh in fetchOgMetadata (up to 3 hops, same-origin only) |
| 10e086a | chore | Deploy fetchOgMetadata to gift-registry-ro |

## Task 3 On-Device Verification (PENDING)

See PLAN.md Task 3 for full verification steps. Key check: paste `https://www.emag.ro/iphone-16-pro-max-256gb-5g-cosmic-orange-rnfyn4zd-a/pd/DC99FV3BM/` into Add Item → Paste URL. Expected: title and image auto-fill. "No details found" should NOT appear.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Double-quote HTML attribute malformation in test metaRefreshHtml helper**
- **Found during:** Task 1 GREEN verification (1 of 9 specs failing)
- **Issue:** `metaRefreshHtml` with `quote: "\""` generated `content="0;url="/dest/""` — malformed HTML where `node-html-parser` truncates the content at the first inner `"`, yielding `content = "0;url="` (no URL)
- **Fix:** When `quote === '"'`, use single-quote outer delimiter for the HTML attribute: `content='0;url="/dest/"'` — this matches how real pages actually encode double-quoted URLs in meta-refresh attributes
- **Files modified:** `functions/src/__tests__/fetchOgMetadata.test.ts`
- **Commit:** f041eee (included in GREEN commit)

## Known Stubs

None — the fix is fully wired. The `fetchOgMetadataHandler` is exported and invoked by `onCall`. The meta-refresh loop runs against real fetch responses in production.
