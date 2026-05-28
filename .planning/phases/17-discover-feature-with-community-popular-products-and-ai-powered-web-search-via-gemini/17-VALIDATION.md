---
phase: 17
slug: discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-28
scope: search-v2-rescope
---

# Phase 17 (Search v2 re-scope) — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Scope: the search-path re-scope only (Gemini intent + Serper.dev /shopping). The community-popular
> path (discoverPopular, triggers, backfill) is DONE and out of scope for this validation.
>
> Provider note (2026-05-28): the original plan targeted Google Custom Search JSON API (CSE), but the
> CSE access gate returned HTTP 403 (closed to new customers). The search provider pivoted to
> **Serper.dev** (`/shopping` endpoint) with a hostname post-filter. Test files renamed
> `cse*` → `serper*`; added `parsePrice` (Romanian price-format) and `applyAllowlistFilter` coverage.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Jest (already in project — see `functions/src/__tests__/`) |
| **Config file** | `functions/jest.config.js` (or `package.json` jest field) |
| **Quick run command** | `cd functions && npm test -- --testPathPattern discover` |
| **Full suite command** | `cd functions && npm test` |
| **Estimated runtime** | ~15 seconds (unit, mocked — no live API calls) |

---

## Sampling Rate

- **After every task commit:** Run `cd functions && npm test -- --testPathPattern discover`
- **After every plan wave:** Run `cd functions && npm test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~15 seconds

---

## Per-Task Verification Map

| Requirement | Behavior | Test Type | Automated Command | File Exists |
|-------------|----------|-----------|-------------------|-------------|
| Intent extraction | Gemini returns valid `IntentResult` with `giftCategories[]` (`searchQuery` field) | unit | `npm test -- parseIntentResponse` | ❌ W0 |
| Intent fallback | Malformed JSON → fallback to single raw-query Serper call | unit | `npm test -- parseIntentResponse` | ❌ W0 |
| Serper normalizer | `SerperShoppingItem[]` → `DiscoverProduct[]` correct field mapping (source→retailer_name, imageUrl→image_url) | unit | `npm test -- serperNormalizer` | ❌ W0 |
| Price parsing | `parsePrice`: "179 RON"→179; "1.299,00 lei"→1299 (RO format); "2,499.00 RON"→2499; undefined/non-numeric→0 | unit | `npm test -- serperNormalizer` | ❌ W0 |
| Allowlist post-filter | `applyAllowlistFilter`: in-allowlist first; pad with out-of-allowlist when < 3; google.com redirect → padding; www./subdomain strip; .com TLDs (ikea/etsy) | unit | `npm test -- serperNormalizer` | ❌ W0 |
| Retailer name | `item.source` → retailer_name; empty source → DOMAIN_TO_RETAILER else raw root domain | unit | `npm test -- serperNormalizer` | ❌ W0 |
| De-dupe by URL | Same URL via different categories → one product | unit | `npm test -- serperNormalizer` | ❌ W0 |
| http→https rewrite | `imageUrl` with `http://` rewritten to `https://` (defensive; Serper is usually already https) | unit | `npm test -- serperNormalizer` | ❌ W0 |
| Price absent | Serper has no parseable price → `price = 0` (never fabricated) | unit | `npm test -- serperNormalizer` | ❌ W0 |
| Fan-out cap | Max 3 Serper queries regardless of `giftCategories` length | unit | `npm test -- serperOrchestration` (mocked) | ❌ W0 |
| Serper call failure | One Serper call rejects → others succeed; partial results returned | unit | `npm test -- serperOrchestration` (mocked) | ❌ W0 |
| Empty intent fallback | `giftCategories: []` → 1 Serper call with raw query | unit | `npm test -- serperOrchestration` (mocked) | ❌ W0 |
| Intent prompt | `buildIntentPrompt` asks for 1-3 categories, Romanian queries, RON; diacritics preserved | unit | `npm test -- promptTemplate` | ⬜ rewrite |
| Existing: cacheKey | Cache key normalization unchanged | unit | `npm test -- cacheKeyNormalization` | ✅ |
| Existing: rateLimit | Rate limiting unchanged | unit | `npm test -- rateLimit` | ✅ |
| Existing: urlNorm | URL normalization unchanged | unit | `npm test -- urlNormalization` | ✅ |

*Status legend: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · ❌ W0 = missing, Wave 0 creates it*

---

## Wave 0 Requirements

New test files to create before/with the backend re-scope (Plan 17-07):

- [ ] `functions/src/__tests__/discover/parseIntentResponse.test.ts` — `IntentResult` parsing (`searchQuery` field), malformed-JSON fallback, missing-`giftCategories` fallback
- [ ] `functions/src/__tests__/discover/serperNormalizer.test.ts` — `parsePrice` (Romanian + US format, undefined/non-numeric→0), `normalizeSerperItems` field mapping (source→retailer_name, imageUrl→image_url https), `applyAllowlistFilter` (in-allowlist-first + graceful padding + google.com redirect → padding + www./subdomain strip + .com TLDs), de-dupe by URL, `price=0` when absent
- [ ] `functions/src/__tests__/discover/serperOrchestration.test.ts` — fan-out cap (max 3), `Promise.allSettled` partial-failure, empty-intent fallback (mocks `callSerper` + `callGeminiIntent`)

Existing test files to UPDATE (not replace):

- [ ] `functions/src/__tests__/discover/promptTemplate.test.ts` — rewrite for the new `buildIntentPrompt()` shape (`searchQuery` terminology); drop the old product-listing assertions
- [ ] `functions/src/__tests__/discover/retailers.test.ts` — verify retailer-context selection still returns correct occasion lists (behavior unchanged)

---

## Manual-Only Verifications

| Behavior | Why Manual | Test Instructions |
|----------|------------|-------------------|
| Serper API-key gate | Cannot verify remotely — needs a live API call with a real key | Human checkpoint at the START of Plan 17-07: sign up at serper.dev (no credit card) → copy API key → `firebase functions:secrets:set SERPER_API_KEY` → run the proof curl `POST https://google.serper.dev/shopping` with `X-API-KEY` header and body `{"q":"cadou copil","gl":"ro","hl":"ro","location":"Romania"}`. Success = HTTP 200 with a `shopping` array. No PSE / cx / engine to configure |
| UAT-6 re-validation (the driver) | End-to-end product correctness needs a real device + real Serper results | On-device: search "cadou copil 2 ani" → returns REAL products whose titles match the linked pages (the original hallucination case) |
| UAT-07 | Real-device search behavior | Search "Gift for coffee lover" → relevant coffee product cards |
| UAT-08 | Real-device occasion routing | Search "Wedding gift for friends" → relevant results incl. wedding-context stores |
| UAT-11 | Real-device locale behavior | Search "cadou Craciun bunica" → Romanian-language results |
| Android price guard | Visual confirmation on device | When Serper returns no parseable price, card does NOT show "0,00 RON" (Serper usually returns a real price, so most cards now show one) |

---

## Validation Sign-Off

- [ ] All backend tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (3 new test files + 2 updated)
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] Serper API-key gate cleared (SERPER_API_KEY set + proof curl 200) before Serper code is written
- [ ] `nyquist_compliant: true` set in frontmatter once plans wired

**Approval:** pending
