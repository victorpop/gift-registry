---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 07
type: tdd
wave: 6
depends_on: []
files_modified:
  - functions/src/discover/secrets.ts
  - functions/src/discover/geminiClient.ts
  - functions/src/discover/promptTemplate.ts
  - functions/src/discover/parseGeminiResponse.ts
  - functions/src/discover/retailers.ts
  - functions/src/discover/serperClient.ts
  - functions/src/discover/serperNormalizer.ts
  - functions/src/discover/search.ts
  - functions/src/discover/enrichImages.ts
  - functions/src/__tests__/discover/parseIntentResponse.test.ts
  - functions/src/__tests__/discover/serperNormalizer.test.ts
  - functions/src/__tests__/discover/serperOrchestration.test.ts
  - functions/src/__tests__/discover/promptTemplate.test.ts
autonomous: false
requirements:
  # V2-SPEC locked decisions (2026-05-28) addressed by this plan:
  - V2-D1   # formal re-plan as re-scope (search path only)
  - V2-ARCH # architecture flow: query → Gemini intent (no grounding) → Serper /shopping fan-out → normalize → allowlist post-filter → flat products
  - V2-RULE # product-suggestion rule: AI never invents products; Serper provides real products
  - V2-STORES # supported-store list (43 stores) drives the ALLOWLIST hostname post-filter + DOMAIN_TO_RETAILER fallback map
  - V2-COST # fan-out cap (max 3 Serper queries) for cost control
  # Unchanged D-* the search path still honors (NOT re-claimed — these were delivered by 17-02/03, this plan must not regress them):
  - D-12    # auth gate: reject unauthenticated + anonymous-provider (search.ts Callable wrapper — unchanged)
  - D-13    # rate limit 20/hr per uid (rateLimit.ts — reused unchanged)
  - D-23    # query validation: non-empty, ≤200 chars (search.ts wrapper — unchanged)
  - D-24    # cache key normalization (cacheKey.ts — reused unchanged)
  - D-25    # discoverCache 30-day TTL (search.ts cache write — unchanged)
  - D-26    # flow: validate → rate-limit → cache check → (miss) pipeline → cache → return
  - D-31    # response shape { products, cached_at } (unchanged contract → zero Android changes)
user_setup:
  - service: serper-dev
    why: "Real product results for Discover search (replaces the CSE plan after Google CSE returned HTTP 403 — closed to new customers). Serper.dev /shopping is open to new signups, requires no credit card, and returns price + image directly. ONE API key, no engine/cx/PSE to configure."
    env_vars:
      - name: SERPER_API_KEY
        source: "serper.dev → sign up (no credit card) → API Keys section of the dashboard → copy the key"
    dashboard_config:
      - task: "Sign up at serper.dev and copy the API key. No engine/PSE/cx to create — site restriction is a hostname post-filter in code, not an external config."
        location: "https://serper.dev (signup → API Keys)"

must_haves:
  truths:
    - "A user search returns REAL products whose titles match the linked retailer page (no hallucinated titles on real product IDs — the UAT-6 root cause is structurally eliminated)"
    - "Product results come exclusively from Serper.dev Google Shopping (/shopping), not from Gemini"
    - "Gemini is called WITHOUT any google_search / grounding tool (intent extraction only, JSON mode)"
    - "At most 3 Serper /shopping queries are issued per user search (cost cap)"
    - "Results are restricted to the 43-store allowlist via a hostname post-filter, with graceful padding (out-of-allowlist results appended by rank) when fewer than 3 in-allowlist matches are found"
    - "A real price is shown when Serper provides one (parsed from the Romanian price string), and 0 when Serper provides none — price is never fabricated"
    - "Duplicate products (same URL reached via different categories) appear once"
    - "Image URLs are https:// (Serper imageUrl is already https; rewrite/validate defensively)"
    - "The discoverSearch Callable response shape is unchanged ({ products, cached_at }) so Android needs no changes"
    - "Rate limit (20/hr), App Check, anonymous rejection, query validation, and 30-day cache are still enforced"
  artifacts:
    - path: "functions/src/discover/serperClient.ts"
      provides: "Pure callSerper() HTTP POST wrapper for the Serper.dev /shopping endpoint (X-API-KEY header)"
      contains: "export async function callSerper"
    - path: "functions/src/discover/serperNormalizer.ts"
      provides: "SerperShoppingItem[] → DiscoverProduct[] normalizer + parsePrice (Romanian format) + applyAllowlistFilter (graceful padding) + ALLOWLIST + DOMAIN_TO_RETAILER fallback + https rewrite + de-dupe"
      contains: "export function normalizeSerperItems"
    - path: "functions/src/discover/geminiClient.ts"
      provides: "callGeminiIntent() JSON-mode intent extraction (no tools)"
      contains: "callGeminiIntent"
    - path: "functions/src/discover/promptTemplate.ts"
      provides: "buildIntentPrompt() intent-extraction prompt"
      contains: "buildIntentPrompt"
    - path: "functions/src/discover/parseGeminiResponse.ts"
      provides: "parseIntentResponse() IntentResult parser (searchQuery field) + DiscoverProduct type"
      contains: "parseIntentResponse"
    - path: "functions/src/discover/search.ts"
      provides: "Re-architected discoverSearch orchestration (Gemini intent → Serper fan-out → normalize → allowlist filter → de-dupe → cache)"
      contains: "callGeminiIntent"
    - path: "functions/src/discover/secrets.ts"
      provides: "SERPER_API_KEY defineSecret declaration"
      contains: "SERPER_API_KEY"
  key_links:
    - from: "functions/src/discover/search.ts"
      to: "functions/src/discover/serperClient.ts"
      via: "Promise.allSettled fan-out of callSerper per giftCategory.searchQuery"
      pattern: "Promise.allSettled"
    - from: "functions/src/discover/search.ts"
      to: "functions/src/discover/geminiClient.ts"
      via: "callGeminiIntent(prompt, apiKey) replaces callGemini"
      pattern: "callGeminiIntent"
    - from: "functions/src/discover/serperNormalizer.ts"
      to: "functions/src/discover/urlNormalization.ts"
      via: "normalizeUrl() for de-dupe (reused, unchanged)"
      pattern: "normalizeUrl"
---

<objective>
Re-architect the `discoverSearch` backend so real products come from the Serper.dev Google Shopping API (`/shopping`) and Gemini only does intent extraction + query generation (no `google_search` grounding). This structurally eliminates the UAT-6 failure where Gemini hallucinated product titles onto real product IDs.

Purpose: The shipped search path lets Gemini name products via grounding, which fabricates titles. The fix: Gemini extracts intent and produces up-to-3 optimized Romanian search queries (JSON mode); Serper `/shopping` returns the actual products (with price + image), and a hostname post-filter restricts results to the curated 43-store allowlist (gracefully padding when too few match).

Context for this revision: the original plan targeted Google Custom Search JSON API (CSE). The CSE access gate (Task 1) FAILED — `gift-registry-ro` got HTTP 403 because CSE is closed to new customers. The user pivoted the search provider to **Serper.dev** using its **`/shopping`** endpoint with a **hostname post-filter**. The Gemini-intent half, the Callable wrapper, cache, rate-limit, de-dupe, the `DiscoverProduct` contract, and deploy are ALL UNCHANGED. Only the access gate, the search-provider client, the result normalizer (incl. price parsing), and the test/secret names change. The authoritative source for this revision is 17-RESEARCH.md → "## Serper.dev Pivot (2026-05-28 — CSE 403 fallback)".

Output: A re-architected `functions/src/discover/` search pipeline (Gemini intent → Serper `/shopping` fan-out → normalize → allowlist filter → de-dupe → cache → return), with the Callable wrapper, cache, rate-limit, and response contract all UNCHANGED so Android requires zero changes. The community-popular path (`getPopular.ts`, `triggers.ts`) is NOT touched.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-VALIDATION.md

<interfaces>
<!-- Contracts the executor needs. Extracted from the live codebase. Use these directly — no exploration needed. -->

The DiscoverProduct type the normalizer must produce (currently exported from parseGeminiResponse.ts; KEEP this exact shape — Android's DiscoverRepositoryImpl maps these fields):
```typescript
export interface DiscoverProduct {
  title: string;
  description: string;
  image_url: string;   // MUST be https:// (Android blocks cleartext)
  price: number;       // parsed from Serper item.price; 0 when Serper has none
  currency: string;    // "RON"
  retailer_url: string;
  retailer_name: string;
}
```

The Serper `/shopping` response item shape (from 17-RESEARCH §3, transitive-bullshit/serper.ts typed SDK gist):
```typescript
export interface SerperShoppingItem {
  title: string;          // product title
  source: string;         // merchant label, e.g. "eMAG", "Altex"
  link: string;           // product URL — may be a google.com/shopping redirect (Pitfall S-2)
  price: string;          // formatted price, e.g. "179 RON" or "1.299,00 lei"
  imageUrl: string;       // product image URL (https encrypted-tbn CDN)
  delivery?: string | Record<string, string>;
  rating?: number;        // 0–5 float
  ratingCount?: number;   // review count
  offers?: string;        // "10+" sellers
  productId?: string;     // Google product ID
  position?: number;      // rank in results
}
```

normalizeUrl() — REUSE for de-dupe (functions/src/discover/urlNormalization.ts, UNCHANGED):
```typescript
export interface NormalizedUrl { productId: string; canonicalUrl: string; }
export function normalizeUrl(url: string): NormalizedUrl;  // throws on invalid URL — wrap in try/catch when de-duping
```

The search.ts Callable wrapper that MUST stay intact (only the cache-miss internals between cache lookup and cache write change):
- auth gate D-12: `if (!request.auth) throw HttpsError("unauthenticated", ...)`; `if (request.auth.token.firebase?.sign_in_provider === "anonymous") throw HttpsError("permission-denied", ...)`
- query validation D-23: non-empty string, ≤200 chars
- `await checkAndIncrementRateLimit(db, request.auth.uid)` (rateLimit.ts — UNCHANGED)
- cache lookup: `db.collection("discoverCache").doc(normalizeCacheKey(query))` (cacheKey.ts — UNCHANGED). On hit return `{ products: data.results, cached_at: <creation time = deadline - CACHE_TTL_MS> }`
- cache write: only when `products.length > 0`; `cachedAt = Timestamp.fromDate(new Date(Date.now() + CACHE_TTL_MS))` (deadline semantics — DO NOT change to serverTimestamp)
- `onCall({ region: "europe-west3", enforceAppCheck: true, secrets: [...], timeoutSeconds: 90 }, discoverSearchHandler)`

rewriteToHttps() helper to COPY from enrichImages.ts (lines 37-39) into serperNormalizer.ts before deleting enrichImages.ts:
```typescript
function rewriteToHttps(raw: string): string {
  return raw.startsWith("http://") ? "https://" + raw.slice(7) : raw;
}
```

Current secrets.ts only exports GEMINI_API_KEY — no CSE secrets were ever added. Just ADD SERPER_API_KEY alongside it.
</interfaces>
</context>

<tasks>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 1: SERPER API-KEY GATE — sign up at serper.dev + set one secret + prove access</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (section "## Serper.dev Pivot" → "### 6. Access Gate (replaces CSE gate)" — the exact signup + secret + test-curl steps)
    - functions/src/discover/secrets.ts (existing GEMINI_API_KEY defineSecret pattern to mirror — currently the ONLY secret exported)
  </read_first>
  <action>
    HUMAN-ACTION GATE — this is the FIRST task and it BLOCKS every Serper code task below (Tasks 4-5). No Serper implementation is reachable until this gate passes. The human signs up at serper.dev (no credit card), copies the API key; Claude then sets the ONE secret from the value the human supplies and runs the proof-of-access curl. Detailed steps are in <how-to-verify>.

    Blocking outcomes: "gate passed" (curl returned HTTP 200 with a `shopping` array AND the secret is set) → proceed to Task 2. If Serper signup somehow fails (e.g. the dashboard rejects the email, or the curl returns 401 "Invalid API key"), SURFACE the failure and stop — re-check the key was copied correctly and the secret was set; there is no further provider pivot planned (Serper is the chosen fallback after CSE 403). Claude runs the single `firebase functions:secrets:set SERPER_API_KEY` command once the human supplies the key, then runs the test curl with that same key.
  </action>
  <what-built>
    Nothing automated yet — this gate MUST pass before ANY Serper code is written. Serper.dev is open to new customers (unlike Google CSE, which returned 403 for this project). The gate is trivially simple compared to the abandoned CSE gate: there is NO Programmable Search Engine, NO cx/engine ID, NO Google Cloud console, NO 43-store engine config. Site restriction is a hostname post-filter implemented in code (Task 4), not an external config. The entire gate is: sign up → copy key → set one secret → prove with one curl.
  </what-built>
  <how-to-verify>
    1. Go to https://serper.dev and sign up with an email address. NO credit card required (2,500 free credits on signup).
    2. In the dashboard, open the "API Keys" section and copy the API key.
    3. Claude sets the secret (run once the human pastes the key when prompted):
       `firebase functions:secrets:set SERPER_API_KEY`
    4. Verify the secret exists:
       `firebase functions:secrets:get SERPER_API_KEY`  (must list at least one version)
    5. Run the proof-of-access curl (replace YOUR_KEY_HERE with the key from step 2 — POST, X-API-KEY header, JSON body):
       ```bash
       curl -s -X POST "https://google.serper.dev/shopping" \
         -H "X-API-KEY: YOUR_KEY_HERE" \
         -H "Content-Type: application/json" \
         -d '{"q":"cadou copil","gl":"ro","hl":"ro","location":"Romania"}' \
         | head -c 2000
       ```
       - HTTP 200 with a JSON body containing a `"shopping": [...]` array of product objects (each with `title`, `source`, `link`, `price`, `imageUrl`) → access CONFIRMED.
       - HTTP 401 `{"message":"Invalid API key."}` or `{"message":"Unauthorized"}` → the key is wrong / not activated. Re-copy from the dashboard and retry; do NOT proceed to code until the curl returns 200.
  </how-to-verify>
  <acceptance_criteria>
    - `firebase functions:secrets:get SERPER_API_KEY` lists at least one version (secret exists)
    - The proof-of-access curl returned HTTP 200 with a non-empty `shopping` array (product objects with title/source/link/price/imageUrl)
    - No engine/cx/PSE was created (Serper needs none — confirms the simplified gate)
  </acceptance_criteria>
  <resume-signal>Type "gate passed" once the curl returned 200 with a shopping array and SERPER_API_KEY is set. If Serper signup/curl fails after re-checking the key, surface the exact error message instead.</resume-signal>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wave 0 RED — write/update the search-v2 test suite (must fail)</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-VALIDATION.md (the Per-Task Verification Map + Wave 0 Requirements — these define exactly which test files and behaviors are required)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (the Serper Pivot section: §3 SerperShoppingItem + field mapping + parsePrice rules + §4 applyAllowlistFilter + extractRootDomain + ALLOWED_DOMAINS + §10 the explicit parsePrice/allowlist test surface; AND the unchanged INTENT_SCHEMA / IntentResult / buildIntentPrompt patterns — note the `cseQuery` → `searchQuery` rename)
    - functions/src/__tests__/discover/parseGeminiResponse.test.ts (existing test style/conventions to mirror — Jest, describe/it, jest.spyOn for console.error)
    - functions/src/__tests__/discover/promptTemplate.test.ts (existing buildPrompt tests to REWRITE for buildIntentPrompt)
    - functions/src/__tests__/discover/retailers.test.ts (existing selectSitesForQuery tests — behavior is UNCHANGED, leave passing or adapt only the export name if renamed)
  </read_first>
  <behavior>
    parseIntentResponse.test.ts (NEW) — tests `parseIntentResponse(rawText, fallbackQuery): IntentResult`:
    - valid JSON object with giftCategories[] → returns parsed IntentResult; giftCategories preserved with name/reason/searchQuery (NOTE: field is `searchQuery`, renamed from `cseQuery`)
    - JSON wrapped in ```json fences → fences stripped, parses (belt-and-suspenders kept from old parser)
    - malformed JSON → returns fallback IntentResult: { giftCategories: [{ name: "", reason: "", searchQuery: <fallbackQuery> }] } and logs console.error (never throws)
    - object with missing/empty giftCategories ([]) → returns the same single-category fallback using fallbackQuery as searchQuery
    - giftCategories with more than 3 entries → trimmed to exactly 3 (fan-out cap enforced at parse OR documented to be enforced in orchestration — pick one and test it)
    serperNormalizer.test.ts (NEW) — tests the Serper normalizer + price parser + allowlist filter:
    - `parsePrice(raw)` (returns { price: number, currency: string }):
      - "179 RON" → { price: 179, currency: "RON" }
      - "1.299,00 lei" → { price: 1299, currency: "RON" }  (Romanian format: dots are thousands, comma is decimal — must NOT yield 1.299)
      - "2,499.00 RON" → { price: 2499, currency: "RON" }  (US/standard format)
      - undefined / "" → { price: 0, currency: "RON" }
      - "Indisponibil" / "Pret la cerere" / non-numeric → { price: 0, currency: "RON" }
    - `normalizeSerperItems(items: SerperShoppingItem[]): DiscoverProduct[]`:
      - SerperShoppingItem with title + link → DiscoverProduct with title, retailer_url=link, price via parsePrice, currency "RON"
      - retailer_name from item.source when present (e.g. "eMAG" → "eMAG"); empty source → derived from link hostname via DOMAIN_TO_RETAILER else raw root domain
      - image_url from item.imageUrl, https-validated/rewritten (already https for Serper, assert it stays https)
      - item missing link OR missing title → dropped (filter)
    - `applyAllowlistFilter(products): DiscoverProduct[]` (graceful padding):
      - all in-allowlist → returns in-allowlist items in original rank order
      - fewer than 3 in-allowlist → pads with out-of-allowlist items by original rank until the floor is met
      - root-domain match strips `www.` (www.emag.ro → emag.ro matches the allowlist)
      - subdomain like shop.altex.ro → altex.ro matches the allowlist
      - a `https://www.google.com/shopping/product/...` redirect link → treated as out-of-allowlist (lands in the padding set, not the in-allowlist set)
      - IKEA with `.com` TLD (ikea.com) and Etsy (etsy.com) match correctly
    - `dedupeByUrl(products)` (de-dupe): two products with same normalized URL (one with a utm param) collapse to one when run through the de-dupe path
    promptTemplate.test.ts (REWRITE) — tests `buildIntentPrompt(query, contextSites): BuiltPrompt`:
    - userPrompt contains the verbatim query (diacritics preserved)
    - systemPrompt instructs "at most 3 gift categories" / "1-3 gift categories"
    - systemPrompt mentions Romanian retailers / RON
    - systemPrompt instructs searchQuery to be a short Romanian product search query
    - REMOVE old assertions tied to the product-listing prompt ("Return ONLY a strict JSON array", "Schema per item", 5-15 items)
    serperOrchestration.test.ts (NEW) — tests the search-miss pipeline with callSerper + callGeminiIntent MOCKED:
    - giftCategories of length 3 → exactly 3 callSerper invocations (fan-out cap)
    - giftCategories of length 5 (defensive) → still ≤ 3 callSerper invocations
    - one callSerper rejects, other two resolve → Promise.allSettled keeps the two successes; partial results returned, no throw
    - empty giftCategories from intent → exactly 1 callSerper with the raw query
  </behavior>
  <action>
    Create three NEW test files and rewrite one existing test file. All assertions reference the contracts in 17-RESEARCH.md (Serper Pivot section) verbatim.

    NEW `functions/src/__tests__/discover/parseIntentResponse.test.ts` — import `{ parseIntentResponse }` from `"../../discover/parseGeminiResponse"`. The IntentResult shape (from RESEARCH INTENT_SCHEMA, with the field renamed): `{ recipient?, occasion?, interests?: string[], budget?: { amount, currency }, giftCategories: Array<{ name: string; reason: string; searchQuery: string }> }`. Cover every behavior listed above. Use `jest.spyOn(console, "error").mockImplementation(() => {})` for the malformed-JSON cases, matching parseGeminiResponse.test.ts. Use `searchQuery` (NOT `cseQuery`) in every fixture and assertion.

    NEW `functions/src/__tests__/discover/serperNormalizer.test.ts` — import `{ normalizeSerperItems, parsePrice, applyAllowlistFilter }` (and `dedupeByUrl` if separately exported) from `"../../discover/serperNormalizer"`. SerperShoppingItem fixtures per RESEARCH §3: `{ title, source, link, price, imageUrl, position? }`. Add the explicit `describe("parsePrice", ...)` block from RESEARCH §10 (the five cases above). Add the `describe("applyAllowlistFilter", ...)` block from RESEARCH §10 (in-allowlist-first, padding when < 3, google.com/shopping redirect → padding set, IKEA .com, www. strip, shop.altex.ro subdomain). Assert normalizeSerperItems maps source→retailer_name, imageUrl→image_url (https), price via parsePrice, currency RON, retailer_url=link, and drops items missing link|title. Assert the de-dupe path collapses same-normalized-URL products.

    NEW `functions/src/__tests__/discover/serperOrchestration.test.ts` — use `jest.mock("../../discover/serperClient")` and `jest.mock("../../discover/geminiClient")` to stub `callSerper` and `callGeminiIntent`. Import the testable orchestration function from search.ts (in Task 5 you will export a pure `runSearchPipeline(query, deps)` so it is unit-testable without Firestore — define that seam now and assert against it). Assert: fan-out cap = 3, Promise.allSettled partial-failure tolerance, empty-intent → 1 raw-query Serper call.

    REWRITE `functions/src/__tests__/discover/promptTemplate.test.ts` — replace the buildPrompt import with `{ buildIntentPrompt }` and rewrite assertions per the behavior block. Keep the diacritics-preservation test. Use `searchQuery` terminology where the prompt instructs the query field.

    Run the suite — it MUST fail (RED) because the implementation does not exist yet. Commit RED:
    `cd functions && git add src/__tests__/discover/ && git commit -m "test(17-07): add failing search-v2 test suite — Serper /shopping + parsePrice + allowlist (RED)"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern "parseIntentResponse|serperNormalizer|serperOrchestration|promptTemplate" 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/__tests__/discover/parseIntentResponse.test.ts` exists, contains `parseIntentResponse` and `searchQuery`, and does NOT contain `cseQuery`
    - `functions/src/__tests__/discover/serperNormalizer.test.ts` exists and contains `normalizeSerperItems`, a `parsePrice` describe block (with "1.299,00 lei"), and an `applyAllowlistFilter` describe block
    - `functions/src/__tests__/discover/serperOrchestration.test.ts` exists and contains `allSettled` OR a fan-out-cap assertion of `3`, mocking `callSerper`
    - `functions/src/__tests__/discover/promptTemplate.test.ts` contains `buildIntentPrompt` and does NOT contain `Return ONLY a strict JSON array`
    - The test run FAILS (RED) — non-zero exit, failures reported for the new specs (implementation absent)
  </acceptance_criteria>
  <done>Three new test files + one rewritten test file committed; the suite fails because the search-v2 implementation does not yet exist.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: GREEN part 1 — secret + Gemini intent (JSON mode, no tools) + intent prompt + intent parser (cseQuery → searchQuery)</name>
  <read_first>
    - functions/src/discover/secrets.ts (extend — add SERPER_API_KEY alongside GEMINI_API_KEY; currently GEMINI_API_KEY is the only export)
    - functions/src/discover/geminiClient.ts (REPLACE — current callGemini uses tools:[{google_search:{}}] + a 60s timeout; drop the tool)
    - functions/src/discover/promptTemplate.ts (REPLACE — current buildPrompt is product-listing; becomes buildIntentPrompt)
    - functions/src/discover/parseGeminiResponse.ts (REPLACE parser; KEEP exporting the DiscoverProduct interface unchanged — search.ts and serperNormalizer.ts both import it)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Code Examples: "Gemini Intent Call" + "Intent Prompt" + INTENT_SCHEMA verbatim; AND Serper §8 "Gemini schema field rename: cseQuery → searchQuery in 3 files")
    - functions/src/__tests__/discover/parseIntentResponse.test.ts + promptTemplate.test.ts (the RED tests this task must turn GREEN)
  </read_first>
  <behavior>
    - callGeminiIntent(prompt, apiKey) posts to gemini-2.5-flash:generateContent with NO `tools` field and `generationConfig.response_mime_type="application/json"` + `response_schema=INTENT_SCHEMA`, returns a parsed IntentResult (via parseIntentResponse)
    - buildIntentPrompt(query, contextSites) returns a system prompt that asks for 1-3 gift categories each with name/reason/searchQuery, Romanian queries, RON budgets; userPrompt is the verbatim query
    - parseIntentResponse(rawText, fallbackQuery) returns a valid IntentResult, falling back to a single category using fallbackQuery (as `searchQuery`) on malformed/empty JSON, never throwing; caps giftCategories to 3
  </behavior>
  <action>
    EXTEND `functions/src/discover/secrets.ts`: add (mirror the GEMINI_API_KEY JSDoc "do not log / never expose to Android" note and the RESEARCH §6 secrets snippet verbatim)
    `export const SERPER_API_KEY = defineSecret("SERPER_API_KEY");`
    Keep the existing `GEMINI_API_KEY` export. Do NOT add any CSE_API_KEY / CSE_ENGINE_ID (those were never added and must not be).

    REPLACE `functions/src/discover/geminiClient.ts`: export `callGeminiIntent(prompt: BuiltPrompt, apiKey: string): Promise<IntentResult>`. Build the request body EXACTLY per RESEARCH "Gemini Intent Call": `{ systemInstruction: { parts: [{ text: prompt.systemPrompt }] }, contents: [{ role: "user", parts: [{ text: prompt.userPrompt }] }], generationConfig: { response_mime_type: "application/json", response_schema: INTENT_SCHEMA } }`. NO `tools` field. Add the comment `// No tools — JSON mode is incompatible with function calling in Gemini 2.5 Flash.` Use `AbortSignal.timeout(30000)` (intent-only is faster than grounded search; reduces the old 60s). Extract `json.candidates[0].content.parts[].text` joined, then `return parseIntentResponse(text, prompt.userPrompt)`. Define and export the `INTENT_SCHEMA` const and the `IntentResult` interface — using `searchQuery` (NOT `cseQuery`) as the per-category query field: `interface IntentResult { recipient?: string; occasion?: string; interests?: string[]; budget?: { amount?: number; currency?: string }; giftCategories: Array<{ name: string; reason: string; searchQuery: string }> }`. The INTENT_SCHEMA's giftCategories items properties are `{ name, reason, searchQuery }` with `required: ["name", "reason", "searchQuery"]`.

    REPLACE `functions/src/discover/promptTemplate.ts`: keep the `BuiltPrompt` interface; replace `buildPrompt` with `buildIntentPrompt(query: string, contextSites: string[]): BuiltPrompt` per RESEARCH "Intent Prompt" code example (system prompt: gift-idea assistant for a Romanian gift-registry app; extract recipient/occasion/interests/budget; generate 1-3 gift categories each with an optimized Romanian product search query 2-5 words; focus on the contextSites; RON budgets; "Generate at most 3 gift categories"). The instruction text describes the query field generically (a "short Romanian product search query") — provider-agnostic, no "cse" wording. userPrompt = query verbatim.

    REPLACE the parser in `functions/src/discover/parseGeminiResponse.ts`: KEEP the `export interface DiscoverProduct { ... }` block byte-for-byte (search.ts + serperNormalizer.ts import it). Replace `parseGeminiResponse` with `export function parseIntentResponse(raw: string, fallbackQuery: string): IntentResult`. Keep the fence-stripping (`^\s*```(?:json)?\s*\n?` and `\n?\s*```\s*$`) and try/catch → on failure log `console.error("Intent parse failed", { rawResponse: raw, query: fallbackQuery })` and return the fallback `{ giftCategories: [{ name: "", reason: "", searchQuery: fallbackQuery }] }`. If parsed object lacks a non-empty `giftCategories` array, return the same fallback. Cap `giftCategories` to the first 3. Import the `IntentResult` type from geminiClient (or define it in a shared spot and re-export — Claude's discretion, keep one source of truth). Use `searchQuery` everywhere; there must be NO `cseQuery` left in this file.

    Run the two relevant test files — they must now pass. Commit GREEN part 1:
    `cd functions && git add src/discover/secrets.ts src/discover/geminiClient.ts src/discover/promptTemplate.ts src/discover/parseGeminiResponse.ts && git commit -m "feat(17-07): Gemini intent extraction (JSON mode, no grounding) + SERPER_API_KEY secret + cseQuery→searchQuery rename"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern "parseIntentResponse|promptTemplate" 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/geminiClient.ts` contains `callGeminiIntent` and does NOT contain `google_search`
    - `functions/src/discover/geminiClient.ts` contains `response_mime_type` and `response_schema` and the no-tools comment, and uses `searchQuery` in INTENT_SCHEMA (NOT `cseQuery`)
    - `functions/src/discover/promptTemplate.ts` contains `buildIntentPrompt`
    - `functions/src/discover/parseGeminiResponse.ts` contains `parseIntentResponse` AND still contains `export interface DiscoverProduct`, with NO `cseQuery`
    - `functions/src/discover/secrets.ts` contains `SERPER_API_KEY` and does NOT contain `CSE_API_KEY` or `CSE_ENGINE_ID`
    - `npm test -- --testPathPattern "parseIntentResponse|promptTemplate"` exits 0
  </acceptance_criteria>
  <done>Gemini calls intent-only in JSON mode with no grounding; intent prompt + parser + SERPER_API_KEY secret exist; the `searchQuery` rename is applied; parseIntentResponse and promptTemplate tests pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 4: GREEN part 2 — Serper client (/shopping POST) + Serper normalizer (parsePrice, allowlist post-filter, source→retailer, https)</name>
  <read_first>
    - functions/src/discover/enrichImages.ts (COPY the rewriteToHttps() helper from lines 37-39 into serperNormalizer BEFORE this file is deleted in Task 5; do NOT copy the OG-scraping logic)
    - functions/src/discover/urlNormalization.ts (reused for de-dupe — note normalizeUrl throws on invalid URLs, so wrap in try/catch; returns { productId, canonicalUrl })
    - functions/src/discover/parseGeminiResponse.ts (import the DiscoverProduct type the normalizer must produce)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Serper Pivot: §1 request shape + §3 SerperShoppingItem + field mapping + the parsePrice() function verbatim + §4 applyAllowlistFilter + extractRootDomain + ALLOWED_DOMAINS + deriveRetailerName + Pitfalls S-1/S-2/S-3/S-4/S-6)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md (Supported stores — the 43 stores whose root domains populate ALLOWLIST + DOMAIN_TO_RETAILER)
    - functions/src/__tests__/discover/serperNormalizer.test.ts (the RED tests this task must turn GREEN)
  </read_first>
  <behavior>
    - callSerper(query, apiKey, options?) POSTs to https://google.serper.dev/shopping with X-API-KEY header + JSON body {q, gl:"ro", hl:"ro", location:"Romania", num:10}; 10s timeout; returns SerperShoppingItem[] (json.shopping ?? []); throws on non-2xx
    - parsePrice(raw) parses Romanian ("1.299,00 lei" → 1299) and US ("2,499.00" → 2499) formats; undefined/non-numeric → 0; returns { price, currency } with currency RON unless EUR detected
    - normalizeSerperItems(items) maps SerperShoppingItem[]→DiscoverProduct[]: drops items missing link|title; price via parsePrice; currency RON; image_url from imageUrl (https); retailer_url=link; retailer_name from source else DOMAIN_TO_RETAILER else raw root domain
    - applyAllowlistFilter(products) returns in-allowlist first by rank; pads with out-of-allowlist by rank when < 3 in-allowlist; google.com redirect → out-of-allowlist
    - a de-dupe-by-normalized-URL path collapses duplicate products to one
  </behavior>
  <action>
    NEW `functions/src/discover/serperClient.ts` per RESEARCH §1 + §3 verbatim:
    - `export interface SerperShoppingItem { title: string; source: string; link: string; price: string; imageUrl: string; delivery?: string | Record<string, string>; rating?: number; ratingCount?: number; offers?: string; productId?: string; position?: number; }`
    - `export async function callSerper(query: string, apiKey: string, options: { gl?: string; hl?: string; location?: string; num?: number } = {}): Promise<SerperShoppingItem[]>`:
      - `const body = { q: query, gl: options.gl ?? "ro", hl: options.hl ?? "ro", location: options.location ?? "Romania", num: options.num ?? 10 };`  // num:10 — keeps cost at 1 credit/call; >10 costs 2 credits (Pitfall S-6)
      - `const response = await fetch("https://google.serper.dev/shopping", { method: "POST", headers: { "X-API-KEY": apiKey, "Content-Type": "application/json" }, body: JSON.stringify(body), signal: AbortSignal.timeout(10000) });`
      - On `!response.ok` throw `new Error("Serper HTTP " + response.status + ": " + (await response.text().catch(() => "")))`.
      - `const json = await response.json() as { shopping?: SerperShoppingItem[] }; return json.shopping ?? [];`

    NEW `functions/src/discover/serperNormalizer.ts` per RESEARCH §3 + §4:
    - Copy `rewriteToHttps()` from enrichImages.ts (used as a belt-and-suspenders on imageUrl).
    - `export function parsePrice(priceStr: string | undefined): { price: number; currency: string }` — VERBATIM from RESEARCH §3: detect currency (lei|RON → RON, EUR → EUR, default RON); strip currency labels; Romanian-format detection (`/\d\.\d{3}[,\s]/.test(cleaned) || /,\d{2}$/.test(cleaned)` → strip dots, replace comma with dot) else US-format (strip commas); `parseFloat`; NaN → 0. (Note: the test asserts the field name `price`, matching the RESEARCH return shape `{ price, currency }`.)
    - `const DOMAIN_TO_RETAILER: Record<string,string>` covering the 43 spec stores' root domains as a fallback when `source` is empty (emag.ro→eMAG, altex.ro→Altex, mediagalaxy.ro→Media Galaxy, flanco.ro→Flanco, cel.ro→CEL.ro, pcgarage.ro→PC Garage, vexio.ro→Vexio, fashiondays.ro→Fashion Days, aboutyou.ro→About You, answear.ro→Answear, modivo.ro→Modivo, zalando.ro→Zalando, epantofi.ro→EPantofi, otter.ro→Otter, notino.ro→Notino, sephora.ro→Sephora, douglas.ro→Douglas, sabon.ro→Sabon, farmaciatei.ro→Farmacia Tei, bebetei.ro→Bebe Tei, ikea.com→IKEA, jysk.ro→JYSK, mobexpert.ro→Mobexpert, bonami.ro→Bonami, vivre.ro→Vivre, carturesti.ro→Cărturești, libris.ro→Libris, elefant.ro→Elefant, noriel.ro→Noriel, hobbyshop.ro→Hobby Shop, decathlon.ro→Decathlon, sportguru.ro→SportGuru, hervis.ro→Hervis, intersport.ro→Intersport, mothercare.ro→Mothercare, floria.ro→Floria, magnolia.ro→Magnolia, complice.ro→Complice, etsy.com→Etsy, breslo.ro→Breslo, kfea.ro→Kfea, delicateseflorescu.ro→Delicatese Florescu, nespresso.com→Nespresso). Since Serper supplies `source` for most results, this map is a fallback — the tests mainly pin the allowlist domains, so cover those.
    - `const ALLOWLIST: Set<string>` (or `ALLOWED_DOMAINS`) of the 43 root domains — VERBATIM from RESEARCH §4 `ALLOWED_DOMAINS` (the exact list there).
    - `function extractRootDomain(url: string): string` — VERBATIM from RESEARCH §4: `new URL(url).hostname`, strip leading `www.`, if > 2 labels take the last two joined by "."; try/catch → return "".
    - `function isAllowedDomain(url: string): boolean` → `ALLOWLIST.has(extractRootDomain(url))`.
    - `function deriveRetailerName(item: SerperShoppingItem): string` — VERBATIM from RESEARCH §4: `item.source` if non-empty (trimmed), else `DOMAIN_TO_RETAILER[extractRootDomain(item.link)] ?? extractRootDomain(item.link)`.
    - `export function normalizeSerperItems(items: SerperShoppingItem[]): DiscoverProduct[]` — filter to items with link && title; for each: call `parsePrice(item.price)` ONCE and destructure, then map to `{ title: item.title, description: "", image_url: item.imageUrl ? rewriteToHttps(item.imageUrl) : "", price, currency, retailer_url: item.link, retailer_name: deriveRetailerName(item) }`.
    - `export function applyAllowlistFilter(products: DiscoverProduct[]): DiscoverProduct[]` — VERBATIM logic from RESEARCH §4 `applyAllowlistFilter`, but operating on the normalized DiscoverProduct[] using `retailer_url` for the hostname check: `const MIN_IN_ALLOWLIST = 3; const inAllowlist = products.filter(p => isAllowedDomain(p.retailer_url)); if (inAllowlist.length >= MIN_IN_ALLOWLIST) return inAllowlist; const out = products.filter(p => !isAllowedDomain(p.retailer_url)); return [...inAllowlist, ...out];` (preserve original rank order; padding appends out-of-allowlist after the in-allowlist set).
    - `export function dedupeByUrl(products: DiscoverProduct[]): DiscoverProduct[]` — iterate, `const seen = new Set<string>()`; for each product try `normalizeUrl(product.retailer_url).productId` inside try/catch (on throw, fall back to the raw retailer_url as the key); push only if unseen.

    Run serperNormalizer tests — must pass. Commit GREEN part 2:
    `cd functions && git add src/discover/serperClient.ts src/discover/serperNormalizer.ts && git commit -m "feat(17-07): Serper /shopping client + normalizer (parsePrice RO format, allowlist post-filter, source→retailer, https, de-dupe)"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern serperNormalizer 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/serperClient.ts` contains `export async function callSerper(` and `google.serper.dev/shopping` and `"X-API-KEY"`
    - `functions/src/discover/serperNormalizer.ts` contains `export function normalizeSerperItems(`, `export function parsePrice(`, and `export function applyAllowlistFilter(`
    - `functions/src/discover/serperNormalizer.ts` contains `rewriteToHttps` and `"emag.ro"` and a Romanian-format branch (e.g. `replace(",", ".")`)
    - `npm test -- --testPathPattern serperNormalizer` exits 0 (includes parsePrice "1.299,00 lei"→1299 and the allowlist padding/redirect cases)
  </acceptance_criteria>
  <done>Serper HTTP client + normalizer exist; price parsing (Romanian format), source→retailer, https, allowlist post-filter with graceful padding, and de-dupe all pass their unit tests.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 5: GREEN part 3 — re-architect search.ts orchestration, delete enrichImages.ts, deploy</name>
  <read_first>
    - functions/src/discover/search.ts (REPLACE only the cache-miss internals between cache lookup and cache write; preserve auth gate, validation, rate-limit, cache lookup, cache write, and onCall options exactly. Current imports: GEMINI_API_KEY (secrets), normalizeCacheKey (cacheKey), selectSitesForQuery (retailers), buildPrompt (promptTemplate), callGemini (geminiClient), parseGeminiResponse + DiscoverProduct (parseGeminiResponse), checkAndIncrementRateLimit (rateLimit), enrichWithOgImages (enrichImages))
    - functions/src/discover/serperClient.ts + serperNormalizer.ts + geminiClient.ts + promptTemplate.ts + parseGeminiResponse.ts (the modules built in Tasks 3-4 — wire them here)
    - functions/src/discover/retailers.ts (selectSitesForQuery still used to feed contextSites into buildIntentPrompt — narrowed to a hint role; keep the export name `selectSitesForQuery` to avoid touching retailers.test.ts)
    - functions/src/discover/enrichImages.ts (to be DELETED — confirm rewriteToHttps was copied into serperNormalizer in Task 4 first)
    - functions/src/registry/fetchOgMetadata.ts (DO NOT remove node-html-parser from package.json — this file still imports it: `import { parse, HTMLElement } from "node-html-parser"` at line 2)
    - functions/src/__tests__/discover/serperOrchestration.test.ts (the RED orchestration tests this task must turn GREEN)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Serper §9 Updated Orchestration Flow + §7 Pitfall S-4 gl+location + S-6 num:10 + S-7 timeout)
  </read_first>
  <behavior>
    - On cache miss: call selectSitesForQuery(query) → buildIntentPrompt(query, sites) → callGeminiIntent(prompt, GEMINI_API_KEY.value()) → IntentResult
    - Fan-out: take the first 3 giftCategories; if 0, synthesize one category with searchQuery = the raw query. For each, callSerper(searchQuery, SERPER_API_KEY.value()) inside Promise.allSettled
    - For each fulfilled Serper result: normalizeSerperItems → applyAllowlistFilter (per batch), then flatten + de-dupe across all categories by normalized URL into a single flat products[] array
    - Cache + return UNCHANGED: only cache when products.length > 0; cachedAt = deadline; response { products, cached_at }
    - Never throw to client: Gemini failure → fallback to a single Serper call on the raw query; all Serper calls failing → return empty products, no cache write
  </behavior>
  <action>
    REPLACE the cache-miss block in `functions/src/discover/search.ts`. REMOVE the imports of `buildPrompt` + `callGemini` + `parseGeminiResponse` (the old product-array parser) + `enrichWithOgImages`. ADD imports for `buildIntentPrompt`, `callGeminiIntent`, `callSerper`, `normalizeSerperItems`, `applyAllowlistFilter` (+ `dedupeByUrl` if exported), and `SERPER_API_KEY`. KEEP these existing imports unchanged: `selectSitesForQuery` from retailers, `DiscoverProduct` from parseGeminiResponse, `normalizeUrl` from urlNormalization, `GEMINI_API_KEY` from secrets, `normalizeCacheKey` from cacheKey, `checkAndIncrementRateLimit` from rateLimit, and the Firestore/onCall/HttpsError imports.

    New cache-miss core (extract into an exported pure-ish function `runSearchPipeline(query, deps)` so serperOrchestration.test can drive it with mocked callSerper/callGeminiIntent — deps = { intentFn, serperFn, apiKeyGemini, apiKeySerper }):
    1. `const sites = selectSitesForQuery(query);`
    2. `const prompt = buildIntentPrompt(query, sites);`
    3. `let intent; try { intent = await intentFn(prompt, apiKeyGemini); } catch (e) { console.error("[discoverSearch] intent failed", e); intent = { giftCategories: [{ name: "", reason: "", searchQuery: query }] }; }`
    4. `let categories = (intent.giftCategories ?? []).slice(0, 3); if (categories.length === 0) categories = [{ name: "", reason: "", searchQuery: query }];`
    5. `const settled = await Promise.allSettled(categories.map(c => serperFn(c.searchQuery, apiKeySerper).then(items => applyAllowlistFilter(normalizeSerperItems(items)))));`
    6. de-dupe: flatten fulfilled results, run through the de-dupe-by-normalizeUrl path (use exported dedupeByUrl or inline the Set<productId> loop with try/catch around normalizeUrl); log rejected count via console.warn.
    7. return the flat `DiscoverProduct[]`.

    In `discoverSearchHandler`, wire `runSearchPipeline(query, { intentFn: callGeminiIntent, serperFn: callSerper, apiKeyGemini: GEMINI_API_KEY.value(), apiKeySerper: SERPER_API_KEY.value() })` for the products. Keep the existing cache-write (`products.length > 0` guard, deadline cachedAt) and the `{ products, cached_at }` return.

    Update the `onCall` options: set the `secrets` array to `[GEMINI_API_KEY, SERPER_API_KEY]`. Keep `enforceAppCheck: true`, `region: "europe-west3"`, `timeoutSeconds: 90` (Pitfall S-7 — do not reduce; ~2s Serper × 3 parallel + ~15s intent is well within 90s). Add the comment `// No google_search / grounding — products come from Serper /shopping (UAT-6 hallucination fix).`

    DELETE `functions/src/discover/enrichImages.ts` (`git rm functions/src/discover/enrichImages.ts`). Confirm `node-html-parser` STAYS in package.json (fetchOgMetadata.ts still imports it) — do NOT touch package.json.

    Build to confirm TypeScript compiles, then run the full discover suite. Commit GREEN part 3:
    `cd functions && npm run build && git rm src/discover/enrichImages.ts && git add -A src/discover/ && git commit -m "feat(17-07): re-architect discoverSearch (Gemini intent → Serper /shopping fan-out → normalize → allowlist filter → de-dupe); delete enrichImages"`

    Then deploy (the gate already set the secret):
    `firebase deploy --only functions:discoverSearch`
  </action>
  <verify>
    <automated>cd functions && npm run build 2>&1 | tail -10 && npm test -- --testPathPattern discover 2>&1 | tail -25</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/search.ts` contains `callGeminiIntent` and `callSerper` and `Promise.allSettled` and `.slice(0, 3)` and `applyAllowlistFilter`
    - `functions/src/discover/search.ts` does NOT contain `enrichWithOgImages` or `callCse` or `google_search`
    - `functions/src/discover/search.ts` onCall `secrets` array contains `SERPER_API_KEY` and `GEMINI_API_KEY`
    - `functions/src/discover/enrichImages.ts` does NOT exist (`git rm`-ed)
    - `node-html-parser` still present in `functions/package.json` dependencies (fetchOgMetadata still needs it)
    - `cd functions && npm run build` exits 0 (TypeScript compiles)
    - `cd functions && npm test -- --testPathPattern discover` exits 0 (full discover suite green, including the existing cacheKey/rateLimit/urlNormalization/triggers tests — proving the community-popular path was not regressed)
    - `firebase deploy --only functions:discoverSearch` completes without error
  </acceptance_criteria>
  <done>discoverSearch orchestration is Gemini-intent → Serper /shopping fan-out → normalize → allowlist filter → de-dupe; enrichImages.ts deleted; full discover test suite green; function deployed to europe-west3.</done>
</task>

</tasks>

<verification>
- `cd functions && npm test -- --testPathPattern discover` is fully green (new search-v2 specs + the unchanged cacheKey/rateLimit/urlNormalization/triggers specs).
- `cd functions && npm run build` compiles with zero TypeScript errors.
- `grep -rn "google_search" functions/src/discover/` returns NOTHING (grounding fully removed).
- `grep -rn "enrichWithOgImages\|enrichImages" functions/src/discover/` returns NOTHING (file deleted, no dangling imports).
- `grep -rni "cseQuery\|callCse\|CSE_API_KEY\|CSE_ENGINE_ID" functions/src/discover/` returns NOTHING (CSE fully removed; field renamed to searchQuery).
- The Serper API-key gate (Task 1) passed (curl 200 + SERPER_API_KEY set) before any Serper code was written.
- `discoverSearch` is deployed; a manual live search returns real products with real prices (validated on-device in Plan 17-08).
</verification>

<success_criteria>
- Gemini is called intent-only (JSON mode, response_schema, no tools); products come exclusively from Serper /shopping.
- Max 3 Serper queries per search; Promise.allSettled tolerates partial Serper failure; empty intent falls back to one raw-query Serper call.
- Serper results normalized to the unchanged DiscoverProduct shape (https images, real price via parsePrice or 0 when absent, source→retailer label), restricted to the 43-store allowlist with graceful padding, de-duped by normalized URL.
- The Callable wrapper (auth gate, App Check, anonymous rejection, query validation, rate-limit, 30-day cache) and the `{ products, cached_at }` response contract are unchanged → zero Android changes required.
- The community-popular path (getPopular.ts, triggers.ts) is untouched and its tests still pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-07-SUMMARY.md` documenting: Serper gate outcome (passed / failed), the final orchestration flow, files replaced/created/deleted, the DiscoverProduct contract confirmation (unchanged — price now populated by Serper instead of always 0), the `cseQuery`→`searchQuery` rename, and any deviations from the research param shapes (esp. the rate at which `link` was a google.com/shopping redirect, per Pitfall S-2).
</output>
</content>
