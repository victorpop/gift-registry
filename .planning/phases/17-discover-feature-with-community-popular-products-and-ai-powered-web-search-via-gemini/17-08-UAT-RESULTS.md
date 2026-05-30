---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 08
artifact: UAT-RESULTS
created: 2026-05-29
updated: 2026-05-30
status: passed
device: Android emulator (emulator-5554, AVD sdk_gphone64_arm64, en + ro locales)
build: c4e9077 (54d5236 initial install; cap+redeploy c4e9077 during UAT)
---

# Phase 17 Plan 08 — On-Device Re-UAT Results (Search v2 + UAT-6 Driver)

## Setup

```bash
cd /Users/victorpop/ai-projects/gift-registry
# APK built with -Puse_emulator=false (production backend, not local emulator suite)
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

App Check debug token for `emulator-5554` (`69f0f83a-e66a-4b91-8dbc-4f019147a4a6`) was registered in Firebase Console → App Check → debug tokens before the first Callable. Without it, every `discoverPopular`/`discoverSearch` invocation rejects.

Signed in as a registered (non-anonymous) user.

## Backend state going into UAT

- **discoverSearch** (europe-west3): Search v2 live — Gemini JSON-mode intent extraction (no grounding tools) → up to 3 Serper.dev `/shopping` queries (gl=ro, hl=ro, location=Romania, num=10) → 43-store hostname allowlist post-filter → de-dupe → **cap at 20 results** → cache write → return `{ products, cached_at }`.
- **SERPER_API_KEY** and **GEMINI_API_KEY** bound to the discoverSearch runtime SA (Secret Manager).
- **discoverPopular** / community-popular triggers: UNCHANGED (17-07 did not touch them).
- **Android app**: price guard `if (product.price > 0.0)` already present at `DiscoverProductCard.kt:112` (no-edit confirmation per Task 1).

---

## UAT Scenario Results

---

### RE-UAT-6 DRIVER — "cadou copil 2 ani" (CRITICAL GATE)

- **Result:** **PASS**
- **What we saw:** After clearing the stale cache (see *Mid-UAT refinements* below), the search returned real Romanian-retailer products for a 2-year-old gift. Card titles displayed in-app matched the actual products on the linked retailer pages — no T-shirts, no Esprit mismatches, no phone covers. The original UAT-6 hallucination is structurally gone.
- **Mechanism:** Gemini now only extracts intent (no `google_search` grounding). The real product titles + URLs come from Serper `/shopping`, which is what Google actually indexed for the merchant page. There is no longer a step where Gemini gets to *name* a product.

### RE-UAT-07 — "Gift for coffee lover"

- **Result:** PASS
- **Notes:** English query produced coffee-relevant product cards from supported Romanian stores.

### RE-UAT-08 — "Wedding gift for friends"

- **Result:** PASS
- **Notes:** Wedding/home-relevant results returned from in-allowlist retailers; graceful padding kicked in cleanly where in-allowlist matches were thin.

### RE-UAT-11 — Romanian locale parity

- **Result:** PASS
- **Notes:** Nav showed **DESCOPERĂ**, section headers **DE PE WEB** / **DIN COMUNITATE**, placeholder **"Caută orice produs..."**. Search "cadou Craciun bunica" returned Romanian-language product titles (Serper `gl=ro` honored regardless of UI locale).

### RE-UAT-PRICE — Price guard (no "0,00 RON")

- **Result:** PASS
- **Notes:** Cards with no Serper-parseable price showed no price line. Cards with Serper price showed it correctly formatted (e.g. "47,99 RON"). The `price > 0.0` guard at `DiscoverProductCard.kt:112` works as designed. With Serper `/shopping` populating real prices, most cards now show one — a UX win over the original CSE plan (which would have had `price = 0` everywhere).

### RE-UAT-IMAGES — Product images load

- **Result:** PASS
- **Notes:** Most images loaded via `encrypted-tbn.gstatic.com`. Placeholder rendered for the occasional missing-image card.

### RE-UAT-TAP — Card tap opens retailer URL

- **Result:** PASS
- **Notes:** Default browser opened the retailer page. Where Serper returned a `google.com/shopping` redirect link, the browser followed through to the merchant page.

### RE-UAT-REGRESSION — Community-popular section

- **Result:** PASS
- **Notes:** The community-popular section rendered as before; 17-07 left `getPopular.ts` and the Firestore triggers untouched and the test suite confirms no regression.

---

## Final Tally

- **PASS:** 8/8
- **FAIL:** 0/8
- **DEFER:** 0/8

Critical (Phase 17 gate):
- [x] RE-UAT-6 DRIVER PASS — hallucination disproved
- [x] RE-UAT-PRICE PASS — no "0,00 RON" displayed
- [x] RE-UAT-REGRESSION PASS — community-popular not broken

Non-critical:
- [x] RE-UAT-07, RE-UAT-08, RE-UAT-11, RE-UAT-IMAGES, RE-UAT-TAP — all PASS

## Mid-UAT refinements (shipped during this UAT session)

Two production issues surfaced and were resolved live during this UAT — both worth recording for future reference because neither is captured in the code itself:

### 1. Stale `discoverCache` masked the new backend (initial RE-UAT-6 FAIL)

First attempt at "cadou copil 2 ani" returned the original UAT-6 results — the Mustang T-shirt and other irrelevant items — even though the Serper code was deployed (revision `discoversearch-00006-tab`, SERPER_API_KEY bound). Function logs showed the smoking gun: recent calls had only `Callable request verification passed` followed by nothing — no `[discoverSearch]` query/Serper logs. **The 30-day `discoverCache` was short-circuiting every previously-tested query before the new pipeline could run.**

**Fix:** purged the collection with `firebase firestore:delete discoverCache --recursive --force`. Re-test passed cleanly: real, relevant products.

**Why this matters going forward:** any change to the search backend that needs to alter what gets cached — different normalizer, different post-filter, different provider — requires a cache purge to be observable on previously-searched queries. The TTL is 30 days. Worth folding into any future search-stack change checklist.

### 2. Result list uncapped (UAT feedback → ≤20 cap shipped live)

The first successful search returned ~30 products (3 fan-out × 10 each, after de-dupe). User asked for a 20-result cap to keep the cached doc + client payload bounded.

**Fix:** added `MAX_SEARCH_RESULTS = 20` in `functions/src/discover/search.ts` (cap applied in `runSearchPipeline` after de-dupe, so the cached doc is also bounded). Added a unit test in `serperOrchestration.test.ts` asserting the cap. Committed `c4e9077`, deployed, re-purged `discoverCache` so already-tested queries repopulated through the capped pipeline. UAT continued and all scenarios passed against the capped backend.

**Note on costs:** Serper spend is driven by the per-search query count (already capped at ≤3 fan-out queries), NOT by the result cap. The 20 cap saves on Firestore doc size and client payload, not Serper.

## Defects discovered

None outstanding. The two items above were resolved during this UAT session.

## Sign-off

- **UAT walked through by:** victorpop (emulator-5554)
- **Date:** 2026-05-30
- **Outcome:** **uat pass** — all 8 scenarios PASS, including the critical RE-UAT-6 driver. Phase 17 search re-scope is verified on-device against the live deployed backend.
