---
created: 2026-05-21T14:13:04.665Z
title: Fix URL import for JS-rendered retailer pages
area: api
files:
  - functions/src/registry/fetchOgMetadata.ts:245-332
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt:225-253
  - app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt:44-63
  - app/src/main/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCase.kt
---

## Problem

User pastes a product URL from a JS-rendered retailer (e.g. LEGO) into the Android "Add an item" flow and gets the soft-failure message **"No details found for that URL — fill in below."** They then have to enter name/price/image manually.

**Reproduction URL:** `https://www.lego.com/ro-ro/product/scuderia-ferrari-hp-charles-leclerc-helmet-43014`

### Root cause

`functions/src/registry/fetchOgMetadata.ts:245-332` is well-built — sends a realistic Chrome User-Agent, `Accept-Language: ro-RO,ro;q=0.9,en;q=0.8`, 10s timeout, follows redirects, walks Open Graph → microdata → JSON-LD → `meta name=`, with multilingual price/currency parsing (`1.234,56` and `1,234.56` both handled).

But lego.com is a **Next.js SPA behind Akamai bot protection**. The fetch returns HTTP 200, but the static HTML body contains no `og:*` tags, no JSON-LD `Product` schema, no microdata — the product data is hydrated client-side by JavaScript after the page loads. There's nothing for our scraper to parse.

The scraper handles this gracefully (no crash) — it returns all-null metadata. `AddItemViewModel.kt:225-253` then sets `ogFetchEmpty = true` and renders the soft-failure UX.

### Affected merchant class

Likely fails (not yet exhaustively tested): **lego.com, nike.com, apple.com, zara.com, adidas.com, hm.com, zalando.ro** — any retailer using Next.js/React SSR-shell + client-side hydration with bot protection.

Currently works (static HTML with OG/JSON-LD): **emag.ro, ikea.com, gentlemanstore.ro**.

### Severity

UX degradation, not a blocker. The manual-entry fallback path works. No security or data-integrity risk. Defer until enough URL-import failures show up to justify the engineering cost.

## Solution

Four options, ordered by effort:

1. **Improve the soft-failure UX (30 min, ship independently).** Detect a "known JS-rendered retailer" via domain match (lego.com / nike.com / apple.com / zara.com / adidas.com / hm.com / zalando.ro) and replace the generic "No details found for that URL" with something like _"LEGO doesn't share product details — please add manually below."_ Honest, lowers user confusion. Doesn't actually fetch the data.

2. **Metadata-as-a-service fallback (recommended for first real fix).** When our scraper returns all-null, retry through Microlink / LinkPreview.net / urlmeta.org. Lowest integration effort, ~$10–50/month at modest volume. Adds 1–3s latency to the fallback path only (cached-hit path unchanged).

3. **Headless renderer for unsupported domains (gold standard).** Proxy through Browserless / ScrapingBee / Playwright-on-Cloud-Run. Unlocks all JS-rendered sites including any future ones. ~$50–200/month + 2–5s latency per fetch. Probably overkill until users complain about more sites.

4. **Per-merchant adapter library.** Custom parser per top retailer (lego.com regex extraction, etc.). Free + fast for covered sites; ongoing maintenance burden as retailers redesign. Not recommended unless we have a small fixed list of high-value merchants.

**Recommendation:** Ship Option 1 now (cheap, honest), and pick Option 2 when at least 2–3 users report URL-import failures.

### Trigger to revisit

- Multiple user reports of "URL import didn't work for X retailer"
- Decision to add a specific high-value retailer (e.g. partnership with LEGO Romania)
- Affiliate program added for any merchant on the JS-rendered list
