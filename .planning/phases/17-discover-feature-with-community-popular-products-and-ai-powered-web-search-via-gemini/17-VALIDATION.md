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
> Scope: the search-path re-scope only (Gemini intent + Google CSE). The community-popular
> path (discoverPopular, triggers, backfill) is DONE and out of scope for this validation.

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
| Intent extraction | Gemini returns valid `IntentResult` with `giftCategories[]` | unit | `npm test -- parseIntentResponse` | ❌ W0 |
| Intent fallback | Malformed JSON → fallback to single raw-query CSE call | unit | `npm test -- parseIntentResponse` | ❌ W0 |
| CSE normalizer | `CseItem[]` → `DiscoverProduct[]` correct field mapping | unit | `npm test -- cseNormalizer` | ❌ W0 |
| Image extraction | Priority: `cse_image` > `cse_thumbnail` > `og:image` > "" | unit | `npm test -- cseNormalizer` | ❌ W0 |
| Domain→retailer map | `www.emag.ro` → "eMAG"; unknown → raw `displayLink` | unit | `npm test -- cseNormalizer` | ❌ W0 |
| De-dupe by URL | Same URL via different CSE categories → one product | unit | `npm test -- cseNormalizer` | ❌ W0 |
| http→https rewrite | `cse_image` with `http://` rewritten to `https://` | unit | `npm test -- cseNormalizer` | ❌ W0 |
| Price absent | CSE has no price → `price = 0` (never fabricated) | unit | `npm test -- cseNormalizer` | ❌ W0 |
| Fan-out cap | Max 3 CSE queries regardless of `giftCategories` length | unit | `npm test -- search` (mocked) | ❌ W0 |
| CSE call failure | One CSE call rejects → others succeed; partial results returned | unit | `npm test -- search` (mocked) | ❌ W0 |
| Empty intent fallback | `giftCategories: []` → 1 CSE call with raw query | unit | `npm test -- search` (mocked) | ❌ W0 |
| Existing: cacheKey | Cache key normalization unchanged | unit | `npm test -- cacheKeyNormalization` | ✅ |
| Existing: rateLimit | Rate limiting unchanged | unit | `npm test -- rateLimit` | ✅ |
| Existing: urlNorm | URL normalization unchanged | unit | `npm test -- urlNormalization` | ✅ |

*Status legend: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · ❌ W0 = missing, Wave 0 creates it*

---

## Wave 0 Requirements

New test files to create before/with the backend re-scope (Plan 17-07):

- [ ] `functions/src/__tests__/discover/parseIntentResponse.test.ts` — `IntentResult` parsing, malformed-JSON fallback, missing-`giftCategories` fallback
- [ ] `functions/src/__tests__/discover/cseNormalizer.test.ts` — image-extraction priority chain, domain→retailer map, de-dupe by URL, https rewrite, `price=0` when absent
- [ ] `functions/src/__tests__/discover/cseOrchestration.test.ts` — fan-out cap (max 3), `Promise.allSettled` partial-failure, empty-intent fallback

Existing test files to UPDATE (not replace):

- [ ] `functions/src/__tests__/discover/promptTemplate.test.ts` — add cases for the new `buildIntentPrompt()` shape
- [ ] `functions/src/__tests__/discover/retailers.test.ts` — verify retailer-context selection still returns correct occasion lists (behavior unchanged)

---

## Manual-Only Verifications

| Behavior | Why Manual | Test Instructions |
|----------|------------|-------------------|
| CSE access gate | Cannot verify remotely — needs a live API call against the project | Human checkpoint at the START of Plan 17-07: make one CSE request with real key+cx. If 403 PERMISSION_DENIED, STOP and pivot to Brave Search API fallback before writing CSE code |
| PSE engine config | Requires Google control-panel setup of the 43-store allowlist | Human checkpoint: create Programmable Search Engine, add all spec stores, enable "Search only included sites", record `cx` into Secret Manager as `CSE_ENGINE_ID` |
| UAT-6 re-validation (the driver) | End-to-end product correctness needs a real device + real CSE results | On-device: search "cadou copil 2 ani" → returns REAL products whose titles match the linked pages (the original hallucination case) |
| UAT-07 | Real-device search behavior | Search "Gift for coffee lover" → relevant coffee product cards |
| UAT-08 | Real-device occasion routing | Search "Wedding gift for friends" → relevant results incl. wedding-context stores |
| UAT-11 | Real-device locale behavior | Search "cadou Craciun bunica" → Romanian-language results |
| Android price guard | Visual confirmation on device | When CSE returns no price, card does NOT show "0,00 RON" |

---

## Validation Sign-Off

- [ ] All backend tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (3 new test files + 2 updated)
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] CSE access gate cleared (or Brave fallback adopted) before CSE code is written
- [ ] `nyquist_compliant: true` set in frontmatter once plans wired

**Approval:** pending
