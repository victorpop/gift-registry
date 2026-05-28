# Phase 17 Search Re-Scope — "Discover Search v2" Research

**Researched:** 2026-05-28
**Domain:** Google Custom Search JSON API + Gemini 2.5 Flash JSON-mode intent extraction
**Confidence:** HIGH for Gemini intent; MEDIUM for CSE (with one CRITICAL caveat documented below)

---

<user_constraints>
## User Constraints (from CONTEXT.md and SEARCH-V2-SPEC.md)

### Locked Decisions
- D-01 through D-51 in 17-CONTEXT.md (original; unchanged decisions still apply: Callable transport, App Check, rate-limiting at 20/hr, cache TTL 30 days, anonymous rejection, europe-west3 region, KSP not KAPT, Navigation3, Hilt, Kotlin Coroutines + StateFlow)
- V2-SPEC locked (2026-05-28): formal re-plan as re-scope; search path only; flat product list UI; Google CSE setup as human-action checkpoint
- FLAT product list on Android — no nested category/reason/products UI grouping
- AI provider: Gemini API only; product search: Google Custom Search JSON API only
- No scraping; no hallucinated products; no microservices; minimal complexity
- community-popular path (discoverPopular, triggers, backfill) is DONE and UNTOUCHED
- enrichImages.ts (OG scrape) is REPLACED — CSE provides images via pagemap

### Claude's Discretion
- Number of CSE queries per intent (fan-out cap) — research recommends 3 max
- Whether to include category/reason as optional card metadata on Android DiscoverProductCard
- Exact Gemini responseSchema structure for intent — research recommends the schema defined below
- Error handling and logging for CSE call failures

### Deferred Ideas (OUT OF SCOPE)
- Analytics events
- "Add to my registry" from Discover cards
- Infinite scroll / pagination
- Per-user personalization
- Time-windowed popularity
- Price-refresh on cached results
- Affiliate URL transformation on Discover taps
- Web fallback variant
</user_constraints>

---

## Summary

The re-scope replaces `discoverSearch`'s Gemini-with-grounding pipeline with a two-step pipeline:
(1) Gemini 2.5 Flash with JSON-mode structured output for intent extraction and query generation (no grounding, no google_search tool), and (2) Google Custom Search JSON API (CSE) for real product pages from a curated set of Romanian retailer sites.

**Critical finding — CSE availability:** The Custom Search JSON API has been closed to new customers since early 2025. New Google Cloud accounts/organizations receive a 403 PERMISSION_DENIED regardless of project setup. "Existing customers" (accounts with prior API access) retain access until January 1, 2027. The project's Firebase account (`gift-registry-ro` owned by `pop.v.victor@gmail.com`) was created well before 2025 and almost certainly qualifies as an existing customer. **The human-action checkpoint (V2 locked decision 4) MUST verify this by attempting to enable the Custom Search JSON API on the `gift-registry-ro` Google Cloud project before coding begins.** If the project gets a 403, an alternate approach is needed.

**Primary recommendation:** Proceed with the CSE architecture as planned, but open the implementation plan with an explicit GATE task that verifies CSE access. Backend code changes are isolated to `functions/src/discover/` (search.ts, geminiClient.ts, promptTemplate.ts, retailers.ts) plus two new files (cseClient.ts, cseNormalizer.ts). Android is ZERO changes — `DiscoverRepositoryImpl` already maps `products[]` from the Callable response into `DiscoverProduct` regardless of what's inside.

---

## CRITICAL: Google Custom Search JSON API Availability

**Confidence: HIGH (multiple Google official sources)**

### Status as of May 2026

| Dimension | Status |
|-----------|--------|
| New customers | BLOCKED — 403 PERMISSION_DENIED on every call |
| Existing customers | ACCESS until January 1, 2027, then must migrate |
| Site-restricted JSON API (free, unlimited quota) | DISCONTINUED January 8, 2025 |
| Regular JSON API (free 100/day + paid) | Available to existing customers only |
| Programmable Search Engine control panel | Still accessible to create engines |
| Full-web search for new engines | Removed — new engines limited to max 50 sites |

### What "existing customer" means in practice

Access is tied to the **Google Cloud organization/account** that first enabled the API, NOT to individual projects under that account. The project `gift-registry-ro` uses the Google account `pop.v.victor@gmail.com`. If this account previously enabled the Custom Search API (or created a PSE before 2025), the API will work. If not, a 403 will appear.

### Gate task for the plan (MANDATORY)

Plan 17-07 MUST begin with a human checkpoint:
1. Go to `console.cloud.google.com/apis/library/customsearch.googleapis.com` on the `gift-registry-ro` project.
2. Enable the API. If the enable button is greyed out with a "not available for new customers" message, the account is blocked.
3. Create a test PSE at `programmablesearchengine.google.com/controlpanel/create` — add a few test sites, toggle "Search the entire web" OFF.
4. Generate a browser API key restricted to Custom Search JSON API.
5. Make one test curl call: `curl "https://customsearch.googleapis.com/customsearch/v1?key=KEY&cx=ENGINE_ID&q=cadou+copil"` — a 200 response confirms access; a 403 PERMISSION_DENIED confirms blockage.

**If 403:** The plan must switch to an alternative. Best option given the stack constraints is to have Gemini generate more targeted product searches (but this returns to the hallucination problem). A realistic alternative at low cost is SerpAPI ($50/month) or Brave Search API (free 2,000/month). Research for those alternatives is in "Open Questions" below.

### Programmable Search Engine (PSE) setup — if CSE access confirmed

**Site restriction strategy:** Create one PSE in the control panel with "Search the entire web" set to OFF and add all 40+ Romanian retailers in "Sites to search." This is the CORRECT mechanism — not the per-request `siteSearch` param (which accepts only ONE site and cannot express a multi-site allowlist). The PSE engine ID (`cx`) is then passed with every API call and limits results to those sites automatically.

**Site count limit:** New PSEs are capped at 50 domains (Google's 2026 restriction). The V2 spec lists 43 stores — well within the 50-domain limit. All stores can be configured in a single PSE.

**Maintenance:** Sites are managed via the PSE control panel UI. No API for managing the engine list at runtime. The `retailers.ts` file in the codebase CONTINUES to serve its occasion-detection role for Gemini query generation; CSE itself enforces the site restriction via the PSE config rather than per-request parameters.

---

## Standard Stack

### Core (Backend — Cloud Functions)

| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| Node.js | 22 (LTS) | Functions runtime | Already in project |
| TypeScript | 5.x | Functions language | Already in project |
| Node 22 built-in `fetch` | native | HTTP calls (CSE + Gemini) | No node-fetch dep — already used in geminiClient.ts |
| `firebase-functions/v2/https` | via BoM 34.11.0 | Callable wiring | Already in project |
| `firebase-admin` | via BoM | Firestore (cache, rate-limit) | Already in project |
| `defineSecret` from `firebase-functions/params` | 2nd gen | Secret Manager | Already pattern in secrets.ts |

**New dependencies:** None. CSE is called via Node 22 native `fetch`. No SDK needed — the CSE API is a simple GET with query params.

**New secrets needed:**
- `CSE_API_KEY` — Google Cloud API key restricted to Custom Search JSON API
- `CSE_ENGINE_ID` — The PSE `cx` value (not a secret per se, but must be configurable without code deploy; treat as a secret for ease)

Set via:
```bash
firebase functions:secrets:set CSE_API_KEY
firebase functions:secrets:set CSE_ENGINE_ID
```

### Core (Android — NO CHANGES NEEDED)

The existing `DiscoverRepositoryImpl.mapResponseToProducts()` already maps whatever is in `products[]` to `DiscoverProduct`. The backend response contract (a top-level `{ products: [...], cached_at: "..." }` structure) is UNCHANGED. Zero Android code changes required per V2 locked decision 3.

---

## Architecture Patterns

### New Orchestration Flow (replaces search.ts internals)

```
discoverSearch Callable (unchanged wrapper: auth gate → rate-limit → cache check)
    │
    ├── [cache hit] → return cached.results immediately (unchanged)
    │
    └── [cache miss]
         │
         ├── Step 1: callGeminiIntent(query) → IntentResult
         │     - Gemini 2.5 Flash, NO tools, JSON mode
         │     - Returns: { recipient, occasion, interests, budget,
         │                  giftCategories[{name, cseQuery}] }
         │     - Cap: generate max 3 cseQuery strings
         │
         ├── Step 2: fan-out CSE calls (parallel Promise.all)
         │     - One CSE query per giftCategory.cseQuery (max 3 calls)
         │     - Each: GET customsearch.googleapis.com/customsearch/v1
         │             ?key=CSE_API_KEY&cx=CSE_ENGINE_ID&q=QUERY
         │             &num=10&gl=ro&lr=lang_ro&hl=ro
         │
         ├── Step 3: normalize + merge + de-dupe by URL
         │     - normalizeCseItem() per item: extract title, url, store, imageUrl
         │     - de-dupe: use normalizeUrl() (already in urlNormalization.ts)
         │     - format to DiscoverProduct shape
         │
         ├── Step 4: wrap in giftIdeas[].products[] (backend response contract)
         │     - category and reason from Gemini intent
         │     - products[] = CSE items assigned to that category
         │
         └── Step 5: cache + return (unchanged)
               - Only cache if products.length > 0
               - DiscoverRepositoryImpl FLATTENS giftIdeas[].products[] to flat list
```

### Recommended Project Structure (changed files only)

```
functions/src/discover/
├── search.ts              REPLACE internals — same Callable wrapper,
│                          replaces Gemini+enrichImages with intent+CSE pipeline
├── geminiClient.ts        REPLACE — drop google_search tool, add JSON-mode intent call
│                          Rename export: callGemini → callGeminiIntent
├── promptTemplate.ts      REPLACE — intent-extraction prompt (not product-listing)
├── retailers.ts           KEEP — selectSitesForQuery still used for
│                          occasion-detection context in Gemini prompt;
│                          the CSE site filtering is via PSE config, not retailers.ts
├── cseClient.ts           NEW — CSE HTTP GET wrapper (pure function, no state)
├── cseNormalizer.ts       NEW — maps CSE items[] to DiscoverProduct[], handles
│                          pagemap image extraction, domain→retailer label, price
├── enrichImages.ts        DELETE — OG scraping no longer needed; CSE provides images
├── parseGeminiResponse.ts REPLACE — new IntentResult parser (replaces product array parser)
├── cacheKey.ts            KEEP UNCHANGED
├── rateLimit.ts           KEEP UNCHANGED
├── secrets.ts             EXTEND — add CSE_API_KEY + CSE_ENGINE_ID defineSecret
├── urlNormalization.ts    KEEP UNCHANGED — reused for de-dupe in cseNormalizer.ts
└── [trigger + popular files untouched]
```

### Pattern 1: Gemini Intent Extraction (JSON mode, no tools)

**What:** Call Gemini 2.5 Flash with `generationConfig.response_mime_type = "application/json"` and `generationConfig.response_schema` to get structured intent + CSE queries. No `tools` field in the request body.

**Why this approach:** Using a `responseSchema` (confirmed working for Gemini 2.5 Flash without tools) produces reliably parseable JSON without markdown fences. The current `parseGeminiResponse.ts` strips fences defensively, but with JSON mode the model is constrained by the schema — the fence-stripping fallback can still remain as a belt-and-suspenders.

**Important:** There is a known incompatibility between `response_mime_type: "application/json"` AND function calling (tools) in the same request for Gemini 2.5 Flash. Since we are dropping tools entirely, this is NOT a problem.

**REST request body shape:**
```typescript
// geminiClient.ts — new callGeminiIntent function
const body = {
  systemInstruction: {
    parts: [{ text: systemPrompt }],
  },
  contents: [{ role: "user", parts: [{ text: userPrompt }] }],
  // NO tools field — this is the whole point of the pivot
  generationConfig: {
    response_mime_type: "application/json",
    response_schema: INTENT_SCHEMA,  // see below
  },
};
```

**Recommended IntentResult schema** (passed as `response_schema` in `generationConfig`):
```typescript
const INTENT_SCHEMA = {
  type: "OBJECT",
  properties: {
    recipient: { type: "STRING" },
    occasion: { type: "STRING" },
    interests: { type: "ARRAY", items: { type: "STRING" } },
    budget: {
      type: "OBJECT",
      properties: {
        amount: { type: "NUMBER" },
        currency: { type: "STRING" },
      },
    },
    giftCategories: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          name: { type: "STRING" },        // e.g. "Manual Coffee Grinder"
          reason: { type: "STRING" },      // e.g. "Useful for specialty coffee fans"
          cseQuery: { type: "STRING" },    // e.g. "rasnita cafea manuala"
        },
        required: ["name", "reason", "cseQuery"],
      },
    },
  },
  required: ["giftCategories"],
};
```

**Fan-out cap:** Generate max 3 `giftCategories` in the intent prompt. This limits CSE to 3 calls per user search. Instruction in the system prompt: "Generate between 1 and 3 gift categories." This is the critical cost control lever.

**Fallback:** If Gemini returns malformed JSON or `giftCategories` is empty/missing, fall through to a single CSE call using the original query string verbatim. Never throw to the client.

### Pattern 2: CSE HTTP Call (pure function)

```typescript
// cseClient.ts — new file
export interface CseItem {
  title: string;
  link: string;
  displayLink: string;
  snippet: string;
  pagemap?: {
    cse_image?: Array<{ src: string }>;
    cse_thumbnail?: Array<{ src: string; width?: string; height?: string }>;
    metatags?: Array<Record<string, string>>;
  };
}

export async function callCse(
  query: string,
  apiKey: string,
  cx: string,
  options: { gl?: string; lr?: string; hl?: string; num?: number } = {},
): Promise<CseItem[]> {
  const params = new URLSearchParams({
    key: apiKey,
    cx,
    q: query,
    num: String(options.num ?? 10),
    gl: options.gl ?? "ro",
    lr: options.lr ?? "lang_ro",
    hl: options.hl ?? "ro",
  });
  const url = `https://customsearch.googleapis.com/customsearch/v1?${params}`;
  const response = await fetch(url, {
    signal: AbortSignal.timeout(10000),  // 10s per CSE call
  });
  if (!response.ok) {
    throw new Error(`CSE HTTP ${response.status}: ${await response.text().catch(() => "")}`);
  }
  const json = await response.json() as { items?: CseItem[] };
  return json.items ?? [];
}
```

**Key params:**
- `key` — `CSE_API_KEY` secret value
- `cx` — `CSE_ENGINE_ID` secret value (the PSE engine ID)
- `num` — max 10 (hard API limit per request)
- `gl=ro` — country for geolocation (Romania)
- `lr=lang_ro` — language restriction to Romanian
- `hl=ro` — UI language hint

**No `siteSearch` param needed** — the PSE engine already restricts to the configured sites. `siteSearch` only accepts a single site and is not the right mechanism for a 40-site allowlist.

### Pattern 3: CSE Result Normalization

```typescript
// cseNormalizer.ts — new file
const DOMAIN_TO_RETAILER: Record<string, string> = {
  "emag.ro": "eMAG",
  "altex.ro": "Altex",
  "flanco.ro": "Flanco",
  "cel.ro": "CEL.ro",
  "pcgarage.ro": "PC Garage",
  "fashiondays.ro": "Fashion Days",
  "notino.ro": "Notino",
  "sephora.ro": "Sephora",
  "douglas.ro": "Douglas",
  "ikea.com": "IKEA",
  "jysk.ro": "JYSK",
  "mobexpert.ro": "Mobexpert",
  "carturesti.ro": "Cărturești",
  "libris.ro": "Libris",
  "elefant.ro": "Elefant",
  "noriel.ro": "Noriel",
  "decathlon.ro": "Decathlon",
  "hervis.ro": "Hervis",
  "floria.ro": "Floria",
  "etsy.com": "Etsy",
  "breslo.ro": "Breslo",
  "nespresso.com": "Nespresso",
  // ... full map for all 43 spec stores
};

function extractImageUrl(item: CseItem): string {
  // Priority: cse_image[0].src > cse_thumbnail[0].src > metatags[0]["og:image"] > ""
  const cseImage = item.pagemap?.cse_image?.[0]?.src;
  if (cseImage) return rewriteToHttps(cseImage);
  const thumb = item.pagemap?.cse_thumbnail?.[0]?.src;
  if (thumb) return rewriteToHttps(thumb);
  const ogImage = item.pagemap?.metatags?.[0]?.["og:image"];
  if (ogImage) return rewriteToHttps(ogImage);
  return "";
}

function extractRetailerName(displayLink: string): string {
  // displayLink is "www.emag.ro" or "emag.ro" — strip "www."
  const host = displayLink.replace(/^www\./, "");
  // Try exact match, then root domain match
  return DOMAIN_TO_RETAILER[host]
    ?? DOMAIN_TO_RETAILER[host.split(".").slice(-2).join(".")]
    ?? displayLink;  // fallback to raw domain
}

export function normalizeCseItems(items: CseItem[]): DiscoverProduct[] {
  return items
    .filter(item => item.link && item.title)
    .map(item => ({
      title: item.title,
      description: item.snippet?.slice(0, 200) ?? "",
      image_url: extractImageUrl(item),
      price: 0,          // CSE does not reliably return price (see below)
      currency: "RON",
      retailer_url: item.link,
      retailer_name: extractRetailerName(item.displayLink),
    }));
}
```

### Pattern 4: De-duplication by normalized URL

After merging all CSE results across categories, de-dupe using `normalizeUrl()` (already in `urlNormalization.ts`). Track seen productIds in a `Set<string>`:

```typescript
const seen = new Set<string>();
const merged: DiscoverProduct[] = [];
for (const product of allCseProducts) {
  const { productId } = normalizeUrl(product.retailer_url);
  if (!seen.has(productId)) {
    seen.add(productId);
    merged.push(product);
  }
}
```

### Backend Response Shape

The backend response from `discoverSearch` KEEPS the same outer shape:
```json
{ "products": [...], "cached_at": "ISO string" }
```

The spec's `giftIdeas[].products[]` nested structure is used INTERNALLY during processing (to associate a category/reason with each product group) but the Callable response flattens to `products[]` before returning. This means `DiscoverRepositoryImpl` requires ZERO changes.

**Optional:** Each product in the flat `products[]` array MAY include `category` and `reason` as optional extra fields. `DiscoverProduct.kt` already has `retailerName` as an optional param — `category` and `reason` could be added as optional fields with empty-string defaults. However, since the spec says "MAY surface as per-card metadata" and the current `DiscoverProductCard.kt` does not render them, the safest approach is to NOT add these fields in v2 (avoids touching Android). They can be added in a future plan if the UI needs them.

### Anti-Patterns to Avoid

- **Using `siteSearch` param for multi-site restriction:** It only accepts one site. Use PSE configuration instead.
- **Dropping `google_search` tool while keeping the old product-listing prompt:** The two changes MUST be paired — the prompt must be rewritten to ask for intent/queries, not products.
- **Caching failed/empty CSE results:** Maintain the existing "only cache non-empty results" logic.
- **Generating more than 3 CSE queries:** Each query = 1 API call = $0.005 against the paid quota. 3 per search is the recommended cap for cost control.
- **Using `response_mime_type: "application/json"` WITH a `tools` field:** Known incompatibility with Gemini 2.5 Flash. Since tools are dropped, this is not a risk, but must never be re-introduced accidentally.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| CSE HTTP call | Custom HTTP abstraction | Simple Node 22 `fetch` with URLSearchParams | CSE is a simple GET; no auth complexity |
| Image extraction fallback | Manual HTML fetching | pagemap.cse_image / cse_thumbnail / metatags og:image | CSE already provides 3 image sources per result |
| Price extraction | Scraping or heuristics | Accept price=0 (mark as unavailable) | CSE pagemap does NOT reliably carry price for Romanian retailers |
| Site restriction | Per-request `siteSearch` | PSE engine configuration in control panel | PSE is the correct mechanism for multi-site allowlists |
| URL de-duplication | Custom string comparison | `normalizeUrl()` in urlNormalization.ts | Already built, tested, handles tracking params + trailing slash + fragments |
| Gemini JSON parsing | Custom fence-stripping | Keep existing fence-stripping in parseGeminiResponse.ts + add JSON-mode constraint | Belt-and-suspenders — JSON mode reduces fences but doesn't eliminate all edge cases |

**Key insight on price:** Google Custom Search Engine structured data extraction does NOT support ecommerce product/price/offer schema.org types for Romanian retailers. The CSE pagemap may occasionally have price data for sites that embed custom PageMap XML, but this is unreliable and site-specific. The spec correctly marks price as "if available" — the normalizer should set `price = 0` when absent and the Android UI already handles `price = 0.0` gracefully (shows "0 RON" — planner may want to add a "price unavailable" display guard). This is an acceptable v2 tradeoff.

---

## File-Level Reuse-vs-Replace Table

| File | Action | Reason |
|------|--------|--------|
| `search.ts` | REPLACE internals | Drop `callGemini(prompt)` + `enrichWithOgImages()`; add `callGeminiIntent()` + CSE fan-out + `normalizeCseItems()` + de-dupe |
| `geminiClient.ts` | REPLACE | Drop `google_search` tool; add JSON-mode intent call. Rename export: `callGemini` → `callGeminiIntent`, return type changes from `string` to `IntentResult` (parsed) |
| `promptTemplate.ts` | REPLACE | Intent-extraction prompt (recipient/occasion/interests/budget/giftCategories with cseQuery) replaces product-listing prompt. New export: `buildIntentPrompt(query)` |
| `parseGeminiResponse.ts` | REPLACE | IntentResult parser replaces `DiscoverProduct[]` parser. Keep fence-stripping + try/catch + graceful-empty-on-failure pattern |
| `retailers.ts` | KEEP (minor update) | `selectSitesForQuery()` still used to populate Gemini system prompt context (occasion-aware retailer names help Gemini pick better CSE queries). CSE site-restriction moves to PSE config. Consider renaming function to `selectRetailersForContext()` for clarity |
| `enrichImages.ts` | DELETE | OG scraping eliminated; CSE provides images via pagemap |
| `cseClient.ts` | NEW | CSE HTTP GET wrapper |
| `cseNormalizer.ts` | NEW | CSE items → DiscoverProduct[] + domain→retailer label |
| `cacheKey.ts` | KEEP UNCHANGED | Same cache key normalization logic |
| `rateLimit.ts` | KEEP UNCHANGED | Same 20/hr per-uid rolling window |
| `secrets.ts` | EXTEND | Add `CSE_API_KEY` and `CSE_ENGINE_ID` via `defineSecret` |
| `urlNormalization.ts` | KEEP UNCHANGED | Reused for de-dupe in cseNormalizer |
| `getPopular.ts` | UNTOUCHED | Community-popular path, do not modify |
| `triggers.ts` | UNTOUCHED | popularItems triggers, do not modify |
| `functions/src/index.ts` | KEEP UNCHANGED | discoverSearch already exported; no new exports needed |
| All Android files | ZERO CHANGES | DiscoverRepositoryImpl maps `products[]` → DiscoverProduct, backend contract unchanged |

---

## Common Pitfalls

### Pitfall 1: CSE "closed to new customers" 403 on first deploy

**What goes wrong:** Plan deploys; `discoverSearch` calls CSE; gets 403 PERMISSION_DENIED; every search returns empty results.
**Why it happens:** The Firebase project's Google Cloud organization was created in 2025 or later, or the Custom Search API was never previously enabled on the account.
**How to avoid:** Gate task in Plan 17-07 verifies access BEFORE any code is committed. If 403, pivot to an alternative (Brave Search API or SerpAPI — see Open Questions).
**Warning signs:** HTTP 403 with body `{"error": {"code": 403, "message": "This project does not have the access to Custom Search JSON API"}}`.

### Pitfall 2: PSE returns irrelevant results because engine searches full web

**What goes wrong:** CSE queries return non-Romanian or off-topic results.
**Why it happens:** The PSE "Search the entire web" toggle was left ON, or sites were added to the engine's "Sites to exclude" instead of "Sites to search."
**How to avoid:** During the CSE setup checkpoint, verify the PSE control panel shows: "Search the entire web: OFF" and all 43 sites listed under "Sites to search." Test with one query and confirm all returned `displayLink` values belong to the configured stores.
**Warning signs:** Results from unrelated domains (e.g., Amazon, Google Shopping) appearing in the response.

### Pitfall 3: Gemini JSON mode with schema produces empty `giftCategories`

**What goes wrong:** Gemini returns `{"giftCategories": []}` for valid queries.
**Why it happens:** The system prompt is too restrictive, or the query is ambiguous and Gemini hedges.
**How to avoid:** Keep a fallback: if `giftCategories.length === 0`, issue one CSE call with the original user query verbatim. Log the zero-category case for tuning.
**Warning signs:** Search results always empty despite valid user queries; Cloud Logging shows `giftCategories: []`.

### Pitfall 4: CSE timeout bloating function latency

**What goes wrong:** 3 parallel CSE calls each take 5-8 seconds, causing the Function to time out.
**Why it happens:** CSE can be slow for niche queries with few indexed pages.
**How to avoid:** Set `AbortSignal.timeout(10000)` per CSE call (10s). Run all 3 in `Promise.allSettled()` (not `Promise.all()`) so one timeout doesn't abort the others. The total orchestration should complete in ≤ 35s (10s Gemini intent + 10s CSE + overhead) — well within the 90s Function timeout.
**Warning signs:** Function timeout errors in Cloud Logging; `discoverSearch` handler duration > 60s.

### Pitfall 5: `response_mime_type` + `tools` incompatibility accidentally reintroduced

**What goes wrong:** A code review adds back a tool or the grounding flag while JSON mode is active; API returns 400.
**Why it happens:** Future edit accidentally merges old geminiClient.ts pattern with new.
**How to avoid:** The new `geminiClient.ts` must not have a `tools` field. Add a comment: `// No tools — JSON mode is incompatible with function calling in Gemini 2.5 Flash.`
**Warning signs:** Gemini API returns HTTP 400 with `"Function calling with a response mime type: 'application/json' is unsupported"`.

### Pitfall 6: Image URLs are `http://` — Android blocks cleartext

**What goes wrong:** CSE `cse_image.src` returns `http://` URLs; Android's network security config blocks them.
**Why it happens:** Some Romanian retailers serve images over HTTP.
**How to avoid:** The `cseNormalizer.ts` must include the `rewriteToHttps()` helper (copy from the deleted `enrichImages.ts`) and apply it to all image URLs extracted from pagemap.
**Warning signs:** Coil `AsyncImage` shows placeholder/error drawable for most product cards.

### Pitfall 7: Function timeout too short after removing OG enrichment

**What goes wrong:** Function times out even though OG enrichment was removed.
**Why it happens:** The current `search.ts` has `timeoutSeconds: 90` which was set for OG enrichment. 90s is now generous for intent (≤15s) + 3x CSE (≤10s each). But if you accidentally set it lower, it may fail under load.
**How to avoid:** Keep `timeoutSeconds: 90` in the `onCall` options — no reason to reduce it. The Function will simply return faster in practice.

---

## Code Examples

### Gemini Intent Call (geminiClient.ts replacement)

```typescript
// Source: https://ai.google.dev/api/generate-content (GenerationConfig docs)
// Source: confirmed working pattern per Gemini API changelog May 2026

const INTENT_SCHEMA = {
  type: "OBJECT",
  properties: {
    recipient: { type: "STRING" },
    occasion: { type: "STRING" },
    interests: { type: "ARRAY", items: { type: "STRING" } },
    budget: {
      type: "OBJECT",
      properties: {
        amount: { type: "NUMBER" },
        currency: { type: "STRING" },
      },
    },
    giftCategories: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          name: { type: "STRING" },
          reason: { type: "STRING" },
          cseQuery: { type: "STRING" },
        },
        required: ["name", "reason", "cseQuery"],
      },
    },
  },
  required: ["giftCategories"],
};

export async function callGeminiIntent(
  prompt: BuiltPrompt,
  apiKey: string,
): Promise<IntentResult> {
  const url =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" +
    `?key=${encodeURIComponent(apiKey)}`;

  const body = {
    systemInstruction: { parts: [{ text: prompt.systemPrompt }] },
    contents: [{ role: "user", parts: [{ text: prompt.userPrompt }] }],
    // NO tools field — JSON mode is incompatible with function calling in Gemini 2.5 Flash
    generationConfig: {
      response_mime_type: "application/json",
      response_schema: INTENT_SCHEMA,
    },
  };

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(30000),  // 30s — intent-only is faster than grounded search
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => "<unreadable>");
    throw new Error(`Gemini HTTP ${response.status}: ${errText.slice(0, 500)}`);
  }

  const json = await response.json() as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };
  const text =
    json?.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";

  return parseIntentResponse(text, prompt.userPrompt);  // new parser
}
```

### Intent Prompt (promptTemplate.ts replacement)

```typescript
// Intent-extraction prompt — NOT product-listing prompt
export function buildIntentPrompt(query: string, contextSites: string[]): BuiltPrompt {
  const sitesList = contextSites.slice(0, 10).join(", ");  // trim for prompt length
  const systemPrompt =
    "You are a gift-idea assistant for a Romanian gift-registry app. " +
    "Analyze the user's query and extract their intent (recipient, occasion, interests, budget). " +
    "Then generate 1-3 gift categories and an optimized Romanian-language search query for each category. " +
    `Focus categories on products available at Romanian retailers such as: ${sitesList}. ` +
    "Return prices and budgets in RON. " +
    "cseQuery must be a short, specific product search query in Romanian (2-5 words), " +
    "optimized for finding real products. Example: 'rasnita cafea manuala', 'carte dezvoltare personala'. " +
    "Generate at most 3 gift categories. If fewer than 3 make sense for the query, return fewer.";

  return { systemPrompt, userPrompt: query };
}
```

### CSE Fan-Out Orchestration (search.ts internals)

```typescript
// Promise.allSettled so one CSE failure doesn't abort the others
const cseApiKey = CSE_API_KEY.value();
const cseEngineId = CSE_ENGINE_ID.value();

const settled = await Promise.allSettled(
  intent.giftCategories.map(cat =>
    callCse(cat.cseQuery, cseApiKey, cseEngineId)
      .then(items => ({ category: cat, items }))
  )
);

const allProducts: DiscoverProduct[] = [];
const seen = new Set<string>();

for (const result of settled) {
  if (result.status === "rejected") {
    console.warn("[discoverSearch] CSE call failed", result.reason);
    continue;
  }
  const { category, items } = result.value;
  for (const item of normalizeCseItems(items)) {
    const { productId } = normalizeUrl(item.retailer_url);
    if (!seen.has(productId)) {
      seen.add(productId);
      allProducts.push(item);
    }
  }
}
```

---

## CSE Response Structure — What to Extract

### Confirmed pagemap fields (from CSE API, multiple sources)

```json
{
  "items": [{
    "title": "Timemore Chestnut C2 Grinder - eMAG.ro",
    "link": "https://www.emag.ro/p/ABC123",
    "displayLink": "www.emag.ro",
    "snippet": "Rasnita manuala Timemore Chestnut C2, 30 RON...",
    "formattedUrl": "https://www.emag.ro/p/ABC123",
    "pagemap": {
      "cse_image": [{ "src": "https://img.emag.ro/product.jpg" }],
      "cse_thumbnail": [{ "src": "https://encrypted-tbn0...", "width": "200", "height": "200" }],
      "metatags": [{
        "og:image": "https://img.emag.ro/product.jpg",
        "og:title": "Rasnita manuala Timemore...",
        "og:description": "...",
        "og:price:amount": "179",          // UNRELIABLE — not all retailers emit this
        "og:price:currency": "RON"
      }]
    }
  }]
}
```

### Image extraction priority

1. `pagemap.cse_image[0].src` — Google's extracted primary image (most reliable)
2. `pagemap.cse_thumbnail[0].src` — Google's thumbnail (always present when indexed, lower resolution)
3. `pagemap.metatags[0]["og:image"]` — page's own og:image (reliable for modern Romanian retailers)
4. Fall back to `""` — Coil renders `discover_card_placeholder` drawable

### Price — DO NOT rely on pagemap

CSE's pagemap structured data extraction does NOT consistently surface price/offer data from Romanian ecommerce sites. The PSE docs confirm only Event, ClaimReview, and EducationalOrganization schema.org types are extracted — Product/Offer are excluded. Set `price = 0` in the normalizer and treat it as unavailable. The Android `DiscoverProductCard.kt` already handles `price = 0.0` (displays "0,00 RON"). The planner may want to add a guard: "if price == 0, show nothing" to avoid displaying "0,00 RON" to users.

### Retailer name from displayLink

`displayLink` is reliably populated as `"www.emag.ro"` or `"emag.ro"`. Strip the `www.` prefix and map to the human retailer name via a static `DOMAIN_TO_RETAILER` map in `cseNormalizer.ts`. Cover all 43 spec stores. For unrecognized domains (possible if PSE engine accidentally indexes outside the configured sites), fall back to the raw `displayLink` value.

---

## Cost Modeling (Confirmed)

| Scenario | Cost |
|----------|------|
| Free tier | 100 CSE queries/day free (per Google docs) |
| Paid tier | $5 per 1,000 queries ($0.005/query) |
| Daily cap | 10,000 queries/day |
| Per user search | 1 Gemini call + max 3 CSE calls = max 3 billable CSE queries |
| Free searches/day | ~33 searches/day (100/3) |
| Cost per search (paid) | ~$0.015 (3 × $0.005) |
| 1,000 searches/month (paid) | ~$15–20 |
| Cache effect | Same normalized query → 0 CSE calls (30-day TTL in discoverCache) |
| Gemini intent cost | ~$0.0003/call (negligible — no grounding fee) |

The 30-day discoverCache dramatically reduces real-world cost: repeat searches for "cadou de ziua de nastere" from many users hit the cache and cost $0.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Jest (already in project — see functions/__tests__/) |
| Config file | `functions/jest.config.js` or `package.json` jest field |
| Quick run command | `cd functions && npm test -- --testPathPattern discover` |
| Full suite command | `cd functions && npm test` |

### Phase Requirements → Test Map

| Requirement | Behavior | Test Type | Automated Command | File Exists? |
|-------------|----------|-----------|-------------------|-------------|
| Intent extraction | Gemini returns valid IntentResult with giftCategories[] | unit | `npm test -- parseIntentResponse` | No — Wave 0 gap |
| Intent fallback | Malformed JSON → fallback to single raw-query CSE call | unit | `npm test -- parseIntentResponse` | No — Wave 0 gap |
| CSE normalizer | CseItem[] → DiscoverProduct[] with correct field mapping | unit | `npm test -- cseNormalizer` | No — Wave 0 gap |
| Image extraction | Priority: cse_image > cse_thumbnail > og:image > "" | unit | `npm test -- cseNormalizer` | No — Wave 0 gap |
| Domain→retailer map | "www.emag.ro" → "eMAG", unknown → raw displayLink | unit | `npm test -- cseNormalizer` | No — Wave 0 gap |
| De-dupe by URL | Same URL via different CSE categories → one product | unit | `npm test -- cseNormalizer` | No — Wave 0 gap |
| http:// → https:// | cse_image with http:// rewritten to https:// | unit | `npm test -- cseNormalizer` | No — Wave 0 gap |
| Fan-out cap | Max 3 CSE queries regardless of giftCategories length | unit | `npm test -- search.handler` (mock) | No — Wave 0 gap |
| CSE call failure | One CSE call rejects → others still succeed; partial results returned | unit | `npm test -- search.handler` (mock) | No — Wave 0 gap |
| Empty intent fallback | giftCategories: [] → 1 CSE call with raw query | unit | `npm test -- search.handler` (mock) | No — Wave 0 gap |
| Existing: cacheKey | Cache key normalization unchanged | unit | `npm test -- cacheKeyNormalization` | EXISTS |
| Existing: rateLimit | Rate limiting unchanged | unit | `npm test -- rateLimit` | EXISTS |
| Existing: urlNorm | URL normalization unchanged | unit | `npm test -- urlNormalization` | EXISTS |
| CSE access gate | API key works, engine configured correctly | MANUAL | Human checkpoint in Plan 17-07 | N/A |
| UAT-6 re-validation | "cadou copil 2 ani" returns REAL products with correct titles | MANUAL | On-device re-UAT (was UAT-6 driver) | N/A |
| UAT-07 new | "Gift for coffee lover" → relevant coffee product cards | MANUAL | On-device UAT | N/A |
| UAT-08 new | "Wedding gift for friends" → relevant results from wedding stores | MANUAL | On-device UAT | N/A |
| UAT-11 new | "cadou Craciun bunica" → Romanian-language results | MANUAL | On-device UAT | N/A |

### Sampling Rate

- **Per task commit:** `cd functions && npm test -- --testPathPattern discover`
- **Per wave merge:** `cd functions && npm test`
- **Phase gate:** Full suite green before marking Phase 17 verified

### Wave 0 Gaps (new test files needed in Plan 17-07)

- `functions/src/__tests__/discover/parseIntentResponse.test.ts` — covers IntentResult parsing, malformed JSON fallback, missing giftCategories fallback
- `functions/src/__tests__/discover/cseNormalizer.test.ts` — covers image extraction priority, domain→retailer map, de-dupe, https rewrite, price=0 when absent
- `functions/src/__tests__/discover/cseOrchestration.test.ts` — covers fan-out cap, Promise.allSettled partial-failure, empty-intent fallback

**Existing test files to update (not replace):**
- `functions/src/__tests__/discover/promptTemplate.test.ts` — add cases for new `buildIntentPrompt()` shape
- `functions/src/__tests__/discover/retailers.test.ts` — verify `selectSitesForQuery()` still returns correct occasion lists (behavior unchanged)

---

## State of the Art

| Old Approach | Current Approach | Changed | Impact |
|--------------|------------------|---------|--------|
| Gemini `google_search` grounding | Gemini intent + Google CSE | UAT-6 2026-05-28 | Eliminates hallucinated product titles |
| OG scraping for image + title | CSE pagemap fields | 2026-05-28 re-scope | Eliminates 4s per-product scrape; faster pipeline |
| Single Gemini call (grounding handles both intent and retrieval) | Two calls: Gemini intent + CSE retrieval | 2026-05-28 re-scope | Cleaner separation of concerns; slower (one extra call) but more reliable |
| google_search tool active | No tools; JSON-mode `response_schema` | 2026-05-28 re-scope | Structured output; avoids known JSON+tools incompatibility in Gemini 2.5 Flash |

**Deprecated/outdated:**
- `enrichImages.ts`: OG scraping approach — delete entirely
- `google_search` tool in `geminiClient.ts`: drop the `tools: [{ google_search: {} }]` field
- Product-listing prompt in `promptTemplate.ts`: replace with intent-extraction prompt

---

## Open Questions

1. **CSE access for the `gift-registry-ro` project**
   - What we know: The Custom Search JSON API is closed to new customers as of 2025. New orgs get 403.
   - What's unclear: Whether `pop.v.victor@gmail.com` / the `gift-registry-ro` Google Cloud org qualifies as an "existing customer" (i.e., was ever previously active with this API).
   - Recommendation: **Gate task in Plan 17-07 verifies this before any code is written.** If the verification fails (403), pivot to one of:
     - **Brave Search API:** Free 2,000 queries/month; paid $3/1,000. Site-restriction via `result_filter=web&goggles_id=...` (limited, not as clean as PSE). Available to new users.
     - **SerpAPI:** ~$50/month; Google Search results via their proxy; site restriction via `as_sitesearch` (but same single-site limit). More expensive.
     - **Fallback: Gemini with revised prompt:** Accept the hallucination risk but add explicit "return only URLs you are confident exist" + stronger post-processing validation. Last resort.

2. **Price display when `price = 0`**
   - What we know: CSE does not reliably return price. The Android `DiscoverProductCard.kt` currently formats `price` via `NumberFormat.getCurrencyInstance` — at price=0.0 it shows "0,00 RON".
   - What's unclear: Whether "0,00 RON" is acceptable UX or should be hidden.
   - Recommendation: Add a guard in `DiscoverProductCard.kt`: `if (price > 0) Text(priceFormatted)`. This is a 2-line Android change and avoids confusing display. Flag for the planner to include.

3. **`retailers.ts` role in the new architecture**
   - What we know: The original `retailers.ts` embedded retailer site lists in the Gemini prompt (D-28). In v2, CSE handles site restriction via the PSE engine; Gemini no longer needs a site list in its prompt.
   - What's unclear: Whether `retailers.ts` / `selectSitesForQuery()` should be removed or kept for context in the intent prompt.
   - Recommendation: KEEP but narrow the role. Pass the selected retailer names (not domains) to the intent prompt as context ("focus on products from stores like eMAG, Altex, Flanco") so Gemini generates more targeted CSE query strings. Rename the export to `selectRetailersForContext()` for clarity. The PSE engine handles the actual site restriction — the retailers list in the prompt is just a hint.

4. **Fan-out product count**
   - What we know: 3 CSE calls × 10 results each = max 30 raw products → after de-dupe maybe 20-25 unique products for the flat list.
   - What's unclear: Whether 20-25 products is the right list length for the Discover screen.
   - Recommendation: Accept up to 30 (before de-dupe) and return whatever survives. The spec says "fixed list sizes" but doesn't mandate a specific search result count. More is better for the user since they scroll.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Node.js 22 | Functions runtime | Already deployed | 22 LTS | — |
| Firebase Functions 2nd gen | discoverSearch Callable | Already deployed | via BoM 34.11.0 | — |
| Gemini API | Intent extraction | Confirmed working (GEMINI_API_KEY already set in Secret Manager per Plan 17-06) | gemini-2.5-flash | — |
| Google Custom Search JSON API | Product search | UNVERIFIED — must gate in Plan 17-07 | unknown | Brave Search API |
| Programmable Search Engine (PSE) | CSE engine config | UNVERIFIED — must create during Plan 17-07 checkpoint | n/a | — |
| Secret Manager (CSE_API_KEY, CSE_ENGINE_ID) | New secrets | Available — same infrastructure as GEMINI_API_KEY | n/a | — |

**Missing dependencies with no fallback:**
- Programmable Search Engine (PSE engine) — must be created and configured as a human-action checkpoint before Plan 17-07 code can be tested end-to-end.

**Missing dependencies with fallback:**
- Google Custom Search JSON API access — if 403 PERMISSION_DENIED, fall back to Brave Search API (requires researching Brave Search API for site-restricted queries; deferred to the gate task).

---

## Recommended New Plan Breakdown (Plans 17-07+)

Based on the code audit, this work fits in two plans:

### Plan 17-07: Backend Search Re-Scope (TDD)

**Tasks:**
1. **Human checkpoint** (GATE): Verify CSE API access on `gift-registry-ro` project. Create PSE with all 43 spec stores. Set `CSE_API_KEY` + `CSE_ENGINE_ID` secrets. Test one CSE call manually.
2. **TDD RED** — write failing tests: `parseIntentResponse.test.ts`, `cseNormalizer.test.ts`, `cseOrchestration.test.ts` (mock callCse + callGeminiIntent)
3. **TDD GREEN + files**:
   - Replace `geminiClient.ts` → `callGeminiIntent()` with JSON mode
   - Replace `promptTemplate.ts` → `buildIntentPrompt()`
   - Replace `parseGeminiResponse.ts` → `parseIntentResponse()` (IntentResult parser)
   - New `cseClient.ts` → `callCse()`
   - New `cseNormalizer.ts` → `normalizeCseItems()` + `DOMAIN_TO_RETAILER` map + `extractImageUrl()` + `extractRetailerName()`
   - Extend `secrets.ts` → add `CSE_API_KEY`, `CSE_ENGINE_ID`
   - Replace `search.ts` internals → new orchestration (Gemini intent → CSE fan-out → normalize → de-dupe → cache → return)
   - Delete `enrichImages.ts`
   - Update `promptTemplate.test.ts` for new intent prompt shape
4. Deploy: `firebase deploy --only functions:discoverSearch`

**Duration estimate:** Similar to Plan 17-02 (~4-8 min coding, pending CSE gate)

### Plan 17-08: Android Price Guard + Deploy + UAT Re-Validation

**Tasks:**
1. Android: add `price > 0` guard in `DiscoverProductCard.kt` (2-line change, avoids "0,00 RON" display)
2. Build + install on device
3. Re-run UAT-6 scenarios: "cadou copil 2 ani", "gift for coffee lover", "wedding gift for friends", "cadou Craciun bunica"
4. Verify: product titles match actual retailer page content (not hallucinated); images load; store names correct; browser tap opens correct page
5. Mark Phase 17 verified if all UAT passes

---

## Sources

### Primary (HIGH confidence)
- [Google Custom Search JSON API Overview](https://developers.google.com/custom-search/v1/overview) — Pricing ($5/1K), free 100/day, closed to new customers statement, 10k/day cap
- [CSE cse.list Reference](https://developers.google.com/custom-search/v1/reference/rest/v1/cse/list) — All request parameters (key, cx, q, num max 10, start, gl, cr, lr, hl, siteSearch), confirmed 100-result total cap
- [Gemini API Structured Output Docs](https://ai.google.dev/gemini-api/docs/structured-output) — `generationConfig.response_mime_type` and `response_schema` fields; confirmed working for Gemini 2.5 Flash without tools
- [Gemini API GenerateContent Reference](https://ai.google.dev/api/generate-content) — GenerationConfig fields in snake_case: `response_mime_type`, `response_schema`
- [Google Custom Search JSON API REST Cookbook](https://github.com/google-gemini/cookbook/blob/main/quickstarts/rest/JSON_mode_REST.ipynb) — Confirmed `response_mime_type: "application/json"` syntax

### Secondary (MEDIUM confidence)
- [PSE Structured Data Documentation](https://developers.google.com/custom-search/docs/structured_data) — Confirmed CSE does NOT extract Product/Offer/price schema.org types
- [Google Developer Forums — 403 PERMISSION_DENIED on new orgs](https://discuss.google.dev/t/custom-search-json-api-returns-403-permission-denied-on-new-org-new-account-restriction/347093) — Confirmed "new customers cannot access" is enforced at org level
- [Raymond Camden CSE Blog](https://www.raymondcamden.com/2020/03/22/implementing-google-custom-search-engines-json-api-in-the-jamstack) — pagemap.cse_thumbnail structure (`src`, `width`, `height`)
- [Winbuzzer — Google Ends Free Web Search](https://winbuzzer.com/2026/01/23/google-ends-free-web-search-programmable-search-engine-xcxwbn/) — New PSEs limited to 50 domains max; "Search the entire web" removed for new engines

### Tertiary (LOW confidence — cross-check before relying)
- [Expertrec CSE Pricing Guide](https://blog.expertrec.com/google-custom-search-json-api-simplified/) — Pricing details + new customer status; consistent with Google official docs
- [GitHub Issue — CSE closed to new customers](https://github.com/diegosouzapw/OmniRoute/issues/1984) — Community confirmation of 403 behavior

---

## Metadata

**Confidence breakdown:**
- Gemini JSON-mode intent (no tools): HIGH — confirmed working, official docs cited
- CSE request shape + params: HIGH — official reference docs
- CSE pagemap image fields: MEDIUM — verified via multiple community sources + SDK source; no official schema doc
- CSE price reliability: HIGH — PSE docs explicitly exclude Product schema.org type from extraction
- CSE API access for this project: LOW until gate task runs — the critical unknown

**Research date:** 2026-05-28
**Valid until:** 2026-11-28 (CSE deprecation deadline January 2027 is a forcing function for migration planning regardless)

---

## RESEARCH COMPLETE

**Phase:** 17 - Discover Search v2 (search re-scope)
**Confidence:** HIGH for technical approach; MEDIUM for CSE access (gate task resolves)

### Key Findings

1. **CSE API availability is a CRITICAL GATE** — the API is closed to new customers; the plan MUST verify `gift-registry-ro` project access before writing any CSE code. The Firebase project's Google account was created well before the 2025 cutoff and is likely fine, but this must be confirmed with a test API call.

2. **Gemini JSON-mode works cleanly without tools** — `generationConfig.response_mime_type: "application/json"` + `response_schema` produces structured IntentResult. The known incompatibility between JSON mode and tools is NOT a risk since we are dropping tools entirely.

3. **Android requires ZERO changes** — `DiscoverRepositoryImpl.mapResponseToProducts()` already maps `products[]` to `DiscoverProduct` and the backend response contract (outer `{products, cached_at}`) is unchanged. The only optional Android change is a 2-line price guard (show nothing when price=0) to avoid "0,00 RON" display.

4. **CSE does not return price** — PSE structured data extraction excludes Product/Offer schema.org types. Price should be set to 0 in the normalizer and the Android card should guard against displaying it.

5. **Existing modules slot in cleanly** — `cacheKey.ts`, `rateLimit.ts`, `urlNormalization.ts`, `getPopular.ts`, `triggers.ts` are entirely unchanged. The Callable wiring in `search.ts` (auth gate, validation, cache check, rate-limit) is unchanged; only the Gemini+enrichImages internals are replaced.

6. **PSE setup is the right mechanism for 40+ stores** — not the per-request `siteSearch` param (which only accepts one site). All 43 spec stores fit within the new 50-domain cap for new PSEs.

### File Created
`/Users/victorpop/ai-projects/gift-registry/.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Gemini JSON-mode intent | HIGH | Official docs confirmed; working pattern for no-tools case |
| CSE request shape | HIGH | Official reference docs; params verified |
| CSE pagemap images | MEDIUM | Multiple community sources; no single official pagemap schema |
| CSE price availability | HIGH | Official PSE structured data docs explicitly exclude Product schema |
| CSE API access for this project | LOW until gate task | Cannot verify remotely; must test with actual API call |

### Open Questions

- Does the `gift-registry-ro` Google Cloud project have CSE API access? (Gate task answers this)
- Should price=0 be hidden in `DiscoverProductCard.kt`? (Small Android change; planner decides)
- Should `selectSitesForQuery()` in `retailers.ts` be renamed to reflect its new hint-only role?

### Ready for Planning

Research complete. The planner can now write Plan 17-07 (backend re-scope, TDD) and Plan 17-08 (Android price guard + UAT re-validation).
