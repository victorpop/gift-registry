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
  - functions/src/discover/cseClient.ts
  - functions/src/discover/cseNormalizer.ts
  - functions/src/discover/search.ts
  - functions/src/discover/enrichImages.ts
  - functions/src/__tests__/discover/parseIntentResponse.test.ts
  - functions/src/__tests__/discover/cseNormalizer.test.ts
  - functions/src/__tests__/discover/cseOrchestration.test.ts
  - functions/src/__tests__/discover/promptTemplate.test.ts
autonomous: false
requirements:
  # V2-SPEC locked decisions (2026-05-28) addressed by this plan:
  - V2-D1   # formal re-plan as re-scope (search path only)
  - V2-ARCH # architecture flow: query → Gemini intent (no grounding) → CSE fan-out → normalize → flat products
  - V2-RULE # product-suggestion rule: AI never invents products; CSE provides real products
  - V2-STORES # supported-store list (43 stores) drives PSE config + DOMAIN_TO_RETAILER map
  - V2-COST # fan-out cap (max 3 CSE queries) for cost control
  # Unchanged D-* the search path still honors (NOT re-claimed — these were delivered by 17-02/03, this plan must not regress them):
  - D-12    # auth gate: reject unauthenticated + anonymous-provider (search.ts Callable wrapper — unchanged)
  - D-13    # rate limit 20/hr per uid (rateLimit.ts — reused unchanged)
  - D-23    # query validation: non-empty, ≤200 chars (search.ts wrapper — unchanged)
  - D-24    # cache key normalization (cacheKey.ts — reused unchanged)
  - D-25    # discoverCache 30-day TTL (search.ts cache write — unchanged)
  - D-26    # flow: validate → rate-limit → cache check → (miss) pipeline → cache → return
  - D-31    # response shape { products, cached_at } (unchanged contract → zero Android changes)
user_setup:
  - service: google-custom-search
    why: "Real product results for Discover search (replaces hallucination-prone Gemini grounding). CSE JSON API is closed to NEW customers since 2025 — the gift-registry-ro project's access MUST be verified before any CSE code is written."
    env_vars:
      - name: CSE_API_KEY
        source: "Google Cloud Console → APIs & Services → Credentials → API key restricted to Custom Search JSON API (gift-registry-ro project)"
      - name: CSE_ENGINE_ID
        source: "programmablesearchengine.google.com/controlpanel → the created engine's 'cx' value"
    dashboard_config:
      - task: "Enable Custom Search JSON API on the gift-registry-ro project; if the enable button is greyed out / 'not available for new customers', STOP and pivot to Brave Search API fallback"
        location: "console.cloud.google.com/apis/library/customsearch.googleapis.com"
      - task: "Create a Programmable Search Engine with 'Search the entire web' OFF; add all 43 V2-SPEC supported stores under 'Sites to search'"
        location: "programmablesearchengine.google.com/controlpanel/create"

must_haves:
  truths:
    - "A user search returns REAL products whose titles match the linked retailer page (no hallucinated titles on real product IDs — the UAT-6 root cause is structurally eliminated)"
    - "Gemini is called WITHOUT the google_search tool and WITHOUT grounding (intent extraction only, JSON mode)"
    - "At most 3 Google CSE queries are issued per user search (cost cap)"
    - "Product results come exclusively from Google Custom Search JSON API, not from Gemini"
    - "Duplicate products (same URL reached via different CSE categories) appear once"
    - "Image URLs are https:// (Android cleartext-blocked http:// rewritten)"
    - "The discoverSearch Callable response shape is unchanged ({ products, cached_at }) so Android needs no changes"
    - "Rate limit (20/hr), App Check, anonymous rejection, query validation, and 30-day cache are still enforced"
  artifacts:
    - path: "functions/src/discover/cseClient.ts"
      provides: "Pure callCse() HTTP GET wrapper for the Custom Search JSON API"
      contains: "export async function callCse"
    - path: "functions/src/discover/cseNormalizer.ts"
      provides: "CseItem[] → DiscoverProduct[] normalizer + DOMAIN_TO_RETAILER map + image extraction + https rewrite + de-dupe"
      contains: "export function normalizeCseItems"
    - path: "functions/src/discover/geminiClient.ts"
      provides: "callGeminiIntent() JSON-mode intent extraction (no tools)"
      contains: "callGeminiIntent"
    - path: "functions/src/discover/promptTemplate.ts"
      provides: "buildIntentPrompt() intent-extraction prompt"
      contains: "buildIntentPrompt"
    - path: "functions/src/discover/parseGeminiResponse.ts"
      provides: "parseIntentResponse() IntentResult parser + DiscoverProduct type"
      contains: "parseIntentResponse"
    - path: "functions/src/discover/search.ts"
      provides: "Re-architected discoverSearch orchestration (Gemini intent → CSE fan-out → normalize → de-dupe → cache)"
      contains: "callGeminiIntent"
    - path: "functions/src/discover/secrets.ts"
      provides: "CSE_API_KEY + CSE_ENGINE_ID defineSecret declarations"
      contains: "CSE_API_KEY"
  key_links:
    - from: "functions/src/discover/search.ts"
      to: "functions/src/discover/cseClient.ts"
      via: "Promise.allSettled fan-out of callCse per giftCategory.cseQuery"
      pattern: "Promise.allSettled"
    - from: "functions/src/discover/search.ts"
      to: "functions/src/discover/geminiClient.ts"
      via: "callGeminiIntent(prompt, apiKey) replaces callGemini"
      pattern: "callGeminiIntent"
    - from: "functions/src/discover/cseNormalizer.ts"
      to: "functions/src/discover/urlNormalization.ts"
      via: "normalizeUrl() for de-dupe (reused, unchanged)"
      pattern: "normalizeUrl"
---

<objective>
Re-architect the `discoverSearch` backend so real products come from Google Custom Search JSON API (CSE) and Gemini only does intent extraction + query generation (no `google_search` grounding). This structurally eliminates the UAT-6 failure where Gemini hallucinated product titles onto real product IDs.

Purpose: The shipped search path lets Gemini name products via grounding, which fabricates titles. The fix: Gemini extracts intent and produces up-to-3 optimized Romanian search queries (JSON mode); CSE returns the actual products from a curated 43-store Programmable Search Engine.

Output: A re-architected `functions/src/discover/` search pipeline (Gemini intent → CSE fan-out → normalize → de-dupe → cache → return), with the Callable wrapper, cache, rate-limit, and response contract all UNCHANGED so Android requires zero changes. The community-popular path (`getPopular.ts`, `triggers.ts`) is NOT touched.
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
  price: number;       // CSE has no reliable price → set 0
  currency: string;    // "RON"
  retailer_url: string;
  retailer_name: string;
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

rewriteToHttps() helper to COPY from enrichImages.ts (lines 37-39) into cseNormalizer.ts before deleting enrichImages.ts:
```typescript
function rewriteToHttps(raw: string): string {
  return raw.startsWith("http://") ? "https://" + raw.slice(7) : raw;
}
```
</interfaces>
</context>

<tasks>

<task type="checkpoint:human-action" gate="blocking">
  <name>Task 1: CSE-ACCESS GATE — verify Custom Search JSON API + create PSE + set secrets</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (sections "CRITICAL: Google Custom Search JSON API Availability" + "Gate task for the plan" + "Open Questions" Brave fallback)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md (the 43-store "Supported stores" list — these go into the PSE)
    - functions/src/discover/secrets.ts (existing GEMINI_API_KEY defineSecret pattern to mirror)
  </read_first>
  <action>
    HUMAN-ACTION GATE — this is the FIRST task and it BLOCKS every CSE code task below (Tasks 4-5). No CSE implementation is reachable until this gate passes. The human verifies Custom Search JSON API access on the gift-registry-ro project, creates the 43-store Programmable Search Engine, and Claude then sets the two secrets from the values the human produces. Detailed steps are in <how-to-verify>. Blocking outcomes: "gate passed" (curl 200 + both secrets set) → proceed to Task 2; "blocked — pivot to Brave" (API enable unavailable / curl 403) → STOP and re-plan onto the Brave Search API fallback (17-RESEARCH.md "Open Questions") before writing ANY CSE code. Claude runs the two `firebase functions:secrets:set` commands once the human supplies the key + cx.
  </action>
  <what-built>
    Nothing automated yet — this gate MUST pass before ANY CSE code is written. The Custom Search JSON API is closed to NEW customers (403 PERMISSION_DENIED). The gift-registry-ro project was created before the 2025 cutoff and is LIKELY an existing customer, but this is UNVERIFIED and is the single critical unknown for the entire re-scope.
  </what-built>
  <how-to-verify>
    1. Open https://console.cloud.google.com/apis/library/customsearch.googleapis.com on the `gift-registry-ro` project. Click ENABLE.
       - If the enable button is greyed out with "not available for new customers" → the account is BLOCKED. STOP. Do not proceed to CSE code. Surface the Brave Search API fallback (free 2,000/mo) per 17-RESEARCH.md "Open Questions" and re-plan. Type "blocked — pivot to Brave" to record this outcome.
    2. Create a Programmable Search Engine at https://programmablesearchengine.google.com/controlpanel/create:
       - "Search the entire web": OFF.
       - Under "Sites to search", add all 43 stores from 17-SEARCH-V2-SPEC.md (eMAG, Altex, Media Galaxy, Flanco, CEL.ro, PC Garage, Vexio, Fashion Days, About You Romania, Answear Romania, Modivo, Zalando Romania, EPantofi, Otter, Notino Romania, Sephora Romania, Douglas Romania, Sabon Romania, Farmacia Tei, Bebe Tei, IKEA Romania, JYSK Romania, Mobexpert, Bonami Romania, Vivre, Cărturești, Libris, Elefant, Noriel, Hobby Shop, Decathlon Romania, SportGuru, Hervis Romania, Intersport Romania, Mothercare Romania, Floria, Magnolia, Complice, Etsy, Breslo, Kfea, Delicatese Florescu, Nespresso Romania). 43 ≤ 50-domain cap — all fit in one engine.
       - Record the engine's `cx` value.
    3. Generate an API key (Google Cloud Console → Credentials) restricted to the Custom Search JSON API. Record the key.
    4. Make ONE live test call (replace KEY and CX):
       `curl "https://customsearch.googleapis.com/customsearch/v1?key=KEY&cx=CX&q=cadou+copil&gl=ro&lr=lang_ro&hl=ro&num=3"`
       - HTTP 200 with an `items` array whose `displayLink` values are all from the configured stores → access CONFIRMED.
       - HTTP 403 `{"error":{"code":403,"message":"...does not have access to Custom Search JSON API"}}` → BLOCKED, STOP, pivot to Brave (see step 1).
    5. Set the two secrets (Claude runs these once the values exist):
       `firebase functions:secrets:set CSE_API_KEY`
       `firebase functions:secrets:set CSE_ENGINE_ID`  (paste the cx value)
  </how-to-verify>
  <acceptance_criteria>
    - `firebase functions:secrets:get CSE_API_KEY` lists at least one version (secret exists)
    - `firebase functions:secrets:get CSE_ENGINE_ID` lists at least one version (secret exists)
    - The test curl returned HTTP 200 with at least one item whose displayLink is one of the 43 configured stores
    - PSE control panel shows "Search the entire web: OFF"
  </acceptance_criteria>
  <resume-signal>Type "gate passed" once the curl returned 200 and both secrets are set, OR "blocked — pivot to Brave" if the API enable was unavailable / curl returned 403.</resume-signal>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wave 0 RED — write/update the search-v2 test suite (must fail)</name>
  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-VALIDATION.md (the Per-Task Verification Map + Wave 0 Requirements — these define exactly which test files and behaviors are required)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Patterns 1-4 + the INTENT_SCHEMA + the IntentResult shape + CseItem shape + image-extraction priority + DOMAIN_TO_RETAILER)
    - functions/src/__tests__/discover/parseGeminiResponse.test.ts (existing test style/conventions to mirror — Jest, describe/it, jest.spyOn for console.error)
    - functions/src/__tests__/discover/promptTemplate.test.ts (existing buildPrompt tests to REWRITE for buildIntentPrompt)
    - functions/src/__tests__/discover/retailers.test.ts (existing selectSitesForQuery tests — behavior is UNCHANGED, leave passing or adapt only the export name if renamed)
  </read_first>
  <behavior>
    parseIntentResponse.test.ts (NEW) — tests `parseIntentResponse(rawText, fallbackQuery): IntentResult`:
    - valid JSON object with giftCategories[] → returns parsed IntentResult; giftCategories preserved with name/reason/cseQuery
    - JSON wrapped in ```json fences → fences stripped, parses (belt-and-suspenders kept from old parser)
    - malformed JSON → returns fallback IntentResult: { giftCategories: [{ name: "", reason: "", cseQuery: <fallbackQuery> }] } and logs console.error (never throws)
    - object with missing/empty giftCategories ([]) → returns the same single-category fallback using fallbackQuery as cseQuery
    - giftCategories with more than 3 entries → trimmed to exactly 3 (fan-out cap enforced at parse OR documented to be enforced in orchestration — pick one and test it)
    cseNormalizer.test.ts (NEW) — tests `normalizeCseItems(items: CseItem[]): DiscoverProduct[]`:
    - CseItem with title + link → DiscoverProduct with title, retailer_url=link, price=0, currency="RON"
    - image priority: cse_image[0].src present → used; absent but cse_thumbnail[0].src present → thumbnail used; both absent but metatags[0]["og:image"] present → og:image used; all absent → image_url=""
    - http:// image rewritten to https://
    - displayLink "www.emag.ro" → retailer_name "eMAG"; unknown domain "foo.ro" → retailer_name falls back to raw displayLink
    - item missing link OR missing title → dropped (filter)
    - de-dupe helper: two products with same normalized URL (one with utm param) collapse to one when run through the de-dupe path
    - price is ALWAYS 0 (never fabricated) — assert no item has price > 0 even when pagemap has og:price:amount
    cseOrchestration.test.ts (NEW) — tests the search-miss pipeline with callCse + callGeminiIntent MOCKED:
    - giftCategories of length 3 → exactly 3 callCse invocations (fan-out cap)
    - giftCategories of length 5 (defensive) → still ≤ 3 callCse invocations
    - one callCse rejects, other two resolve → Promise.allSettled keeps the two successes; partial results returned, no throw
    - empty giftCategories from intent → exactly 1 callCse with the raw query
    promptTemplate.test.ts (REWRITE) — tests `buildIntentPrompt(query, contextSites): BuiltPrompt`:
    - userPrompt contains the verbatim query (diacritics preserved)
    - systemPrompt instructs "at most 3 gift categories" / "1-3 gift categories"
    - systemPrompt mentions Romanian retailers / RON
    - systemPrompt instructs cseQuery to be a short Romanian product search query
    - REMOVE old assertions tied to the product-listing prompt ("Return ONLY a strict JSON array", "Schema per item", 5-15 items)
  </behavior>
  <action>
    Create three NEW test files and rewrite one existing test file. All assertions reference the contracts in 17-RESEARCH.md verbatim.

    NEW `functions/src/__tests__/discover/parseIntentResponse.test.ts` — import `{ parseIntentResponse }` from `"../../discover/parseGeminiResponse"`. The IntentResult shape (from RESEARCH INTENT_SCHEMA): `{ recipient?, occasion?, interests?: string[], budget?: { amount, currency }, giftCategories: Array<{ name: string; reason: string; cseQuery: string }> }`. Cover every behavior listed above. Use `jest.spyOn(console, "error").mockImplementation(() => {})` for the malformed-JSON cases, matching parseGeminiResponse.test.ts.

    NEW `functions/src/__tests__/discover/cseNormalizer.test.ts` — import `{ normalizeCseItems }` (and the de-dupe export if separate) from `"../../discover/cseNormalizer"`. CseItem fixtures per RESEARCH Pattern 2: `{ title, link, displayLink, snippet, pagemap?: { cse_image?: [{src}], cse_thumbnail?: [{src,width,height}], metatags?: [Record<string,string>] } }`. Assert the image-extraction priority chain `cse_image > cse_thumbnail > og:image > ""`, the DOMAIN_TO_RETAILER mapping (www.emag.ro→eMAG, unknown→raw), price always 0, https rewrite, and drop-on-missing-link/title.

    NEW `functions/src/__tests__/discover/cseOrchestration.test.ts` — use `jest.mock("../../discover/cseClient")` and `jest.mock("../../discover/geminiClient")` to stub `callCse` and `callGeminiIntent`. Import the testable orchestration function from search.ts (in Task 5 you will export a pure `runSearchPipeline(query, intentFn, cseFn, ...)` or export the cache-miss core so it is unit-testable without Firestore — define that seam now and assert against it). Assert: fan-out cap = 3, Promise.allSettled partial-failure tolerance, empty-intent → 1 raw-query CSE call.

    REWRITE `functions/src/__tests__/discover/promptTemplate.test.ts` — replace the buildPrompt import with `{ buildIntentPrompt }` and rewrite assertions per the behavior block. Keep the diacritics-preservation test.

    Run the suite — it MUST fail (RED) because the implementation does not exist yet. Commit RED:
    `cd functions && git add src/__tests__/discover/ && git commit -m "test(17-07): add failing search-v2 test suite (RED)"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern "parseIntentResponse|cseNormalizer|cseOrchestration|promptTemplate" 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/__tests__/discover/parseIntentResponse.test.ts` exists and contains `parseIntentResponse`
    - `functions/src/__tests__/discover/cseNormalizer.test.ts` exists and contains `normalizeCseItems`
    - `functions/src/__tests__/discover/cseOrchestration.test.ts` exists and contains `allSettled` OR a fan-out-cap assertion of `3`
    - `functions/src/__tests__/discover/promptTemplate.test.ts` contains `buildIntentPrompt` and does NOT contain `Return ONLY a strict JSON array`
    - The test run FAILS (RED) — non-zero exit, failures reported for the new specs (implementation absent)
  </acceptance_criteria>
  <done>Three new test files + one rewritten test file committed; the suite fails because the search-v2 implementation does not yet exist.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: GREEN part 1 — secrets + Gemini intent (JSON mode, no tools) + intent prompt + intent parser</name>
  <read_first>
    - functions/src/discover/secrets.ts (extend — add CSE_API_KEY + CSE_ENGINE_ID alongside GEMINI_API_KEY)
    - functions/src/discover/geminiClient.ts (REPLACE — current callGemini uses tools:[{google_search:{}}]; drop it)
    - functions/src/discover/promptTemplate.ts (REPLACE — current buildPrompt is product-listing; becomes buildIntentPrompt)
    - functions/src/discover/parseGeminiResponse.ts (REPLACE parser; KEEP exporting the DiscoverProduct interface unchanged — search.ts and cseNormalizer.ts both import it)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Code Examples: "Gemini Intent Call" + "Intent Prompt" + INTENT_SCHEMA verbatim)
    - functions/src/__tests__/discover/parseIntentResponse.test.ts + promptTemplate.test.ts (the RED tests this task must turn GREEN)
  </read_first>
  <behavior>
    - callGeminiIntent(prompt, apiKey) posts to gemini-2.5-flash:generateContent with NO `tools` field and `generationConfig.response_mime_type="application/json"` + `response_schema=INTENT_SCHEMA`, returns a parsed IntentResult (via parseIntentResponse)
    - buildIntentPrompt(query, contextSites) returns a system prompt that asks for 1-3 gift categories each with name/reason/cseQuery, Romanian queries, RON budgets; userPrompt is the verbatim query
    - parseIntentResponse(rawText, fallbackQuery) returns a valid IntentResult, falling back to a single category using fallbackQuery on malformed/empty JSON, never throwing; caps giftCategories to 3
  </behavior>
  <action>
    EXTEND `functions/src/discover/secrets.ts`: add
    `export const CSE_API_KEY = defineSecret("CSE_API_KEY");`
    `export const CSE_ENGINE_ID = defineSecret("CSE_ENGINE_ID");`
    Keep the existing `GEMINI_API_KEY` export. Mirror the existing JSDoc "do not log / never expose to Android" note.

    REPLACE `functions/src/discover/geminiClient.ts`: export `callGeminiIntent(prompt: BuiltPrompt, apiKey: string): Promise<IntentResult>`. Build the request body EXACTLY per RESEARCH "Gemini Intent Call": `{ systemInstruction: { parts: [{ text: prompt.systemPrompt }] }, contents: [{ role: "user", parts: [{ text: prompt.userPrompt }] }], generationConfig: { response_mime_type: "application/json", response_schema: INTENT_SCHEMA } }`. NO `tools` field. Add the comment `// No tools — JSON mode is incompatible with function calling in Gemini 2.5 Flash.` Use `AbortSignal.timeout(30000)`. Extract `json.candidates[0].content.parts[].text` joined, then `return parseIntentResponse(text, prompt.userPrompt)`. Define and export the `INTENT_SCHEMA` const and the `IntentResult` interface (from RESEARCH §INTENT_SCHEMA): `interface IntentResult { recipient?: string; occasion?: string; interests?: string[]; budget?: { amount?: number; currency?: string }; giftCategories: Array<{ name: string; reason: string; cseQuery: string }> }`.

    REPLACE `functions/src/discover/promptTemplate.ts`: keep the `BuiltPrompt` interface; replace `buildPrompt` with `buildIntentPrompt(query: string, contextSites: string[]): BuiltPrompt` per RESEARCH "Intent Prompt" code example (system prompt: gift-idea assistant for a Romanian gift-registry app; extract recipient/occasion/interests/budget; generate 1-3 gift categories each with an optimized Romanian cseQuery 2-5 words; focus on the contextSites; RON budgets; "Generate at most 3 gift categories"). userPrompt = query verbatim.

    REPLACE the parser in `functions/src/discover/parseGeminiResponse.ts`: KEEP the `export interface DiscoverProduct { ... }` block byte-for-byte (search.ts + cseNormalizer.ts import it). Replace `parseGeminiResponse` with `export function parseIntentResponse(raw: string, fallbackQuery: string): IntentResult`. Keep the fence-stripping (`^\s*```(?:json)?\s*\n?` and `\n?\s*```\s*$`) and try/catch → on failure log `console.error("Intent parse failed", { rawResponse: raw, query: fallbackQuery })` and return the fallback `{ giftCategories: [{ name: "", reason: "", cseQuery: fallbackQuery }] }`. If parsed object lacks a non-empty `giftCategories` array, return the same fallback. Cap `giftCategories` to the first 3. Import the `IntentResult` type from geminiClient (or define it in a shared spot and re-export — Claude's discretion, keep one source of truth).

    Run the two relevant test files — they must now pass. Commit GREEN part 1:
    `cd functions && git add src/discover/secrets.ts src/discover/geminiClient.ts src/discover/promptTemplate.ts src/discover/parseGeminiResponse.ts && git commit -m "feat(17-07): Gemini intent extraction (JSON mode, no grounding) + CSE secrets"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern "parseIntentResponse|promptTemplate" 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/geminiClient.ts` contains `callGeminiIntent` and does NOT contain `google_search`
    - `functions/src/discover/geminiClient.ts` contains `response_mime_type` and `response_schema` and the no-tools comment
    - `functions/src/discover/promptTemplate.ts` contains `buildIntentPrompt`
    - `functions/src/discover/parseGeminiResponse.ts` contains `parseIntentResponse` AND still contains `export interface DiscoverProduct`
    - `functions/src/discover/secrets.ts` contains `CSE_API_KEY` and `CSE_ENGINE_ID`
    - `npm test -- --testPathPattern "parseIntentResponse|promptTemplate"` exits 0
  </acceptance_criteria>
  <done>Gemini calls intent-only in JSON mode with no grounding; intent prompt + parser + CSE secrets exist; parseIntentResponse and promptTemplate tests pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 4: GREEN part 2 — CSE client + CSE normalizer (image priority, domain map, https rewrite, price=0)</name>
  <read_first>
    - functions/src/discover/enrichImages.ts (COPY the rewriteToHttps() helper from lines 37-39 into cseNormalizer BEFORE this file is deleted in Task 5; do NOT copy the OG-scraping logic)
    - functions/src/discover/urlNormalization.ts (reused for de-dupe — note normalizeUrl throws on invalid URLs, so wrap in try/catch)
    - functions/src/discover/parseGeminiResponse.ts (import the DiscoverProduct type the normalizer must produce)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (Pattern 2 callCse + Pattern 3 cseNormalizer + Pattern 4 de-dupe + "CSE Response Structure" + the full 43-store DOMAIN_TO_RETAILER list)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-SEARCH-V2-SPEC.md (Supported stores — the 43 stores whose domains populate DOMAIN_TO_RETAILER)
    - functions/src/__tests__/discover/cseNormalizer.test.ts (the RED tests this task must turn GREEN)
  </read_first>
  <behavior>
    - callCse(query, apiKey, cx, options?) issues a GET to customsearch.googleapis.com with params key, cx, q, num=10, gl=ro, lr=lang_ro, hl=ro; 10s timeout; returns CseItem[] (json.items ?? []); throws on non-2xx
    - normalizeCseItems(items) maps CseItem[]→DiscoverProduct[]: drops items missing link|title; price always 0; currency "RON"; image via cse_image>cse_thumbnail>og:image>"" with https rewrite; retailer_name via DOMAIN_TO_RETAILER (www. stripped) with raw-displayLink fallback
    - a de-dupe-by-normalized-URL path collapses duplicate products to one
  </behavior>
  <action>
    NEW `functions/src/discover/cseClient.ts` per RESEARCH Pattern 2 verbatim: export `interface CseItem { title: string; link: string; displayLink: string; snippet: string; pagemap?: { cse_image?: Array<{ src: string }>; cse_thumbnail?: Array<{ src: string; width?: string; height?: string }>; metatags?: Array<Record<string, string>>; } }` and `export async function callCse(query, apiKey, cx, options = {})`. Build `URLSearchParams` with `key, cx, q, num=String(options.num ?? 10), gl=options.gl ?? "ro", lr=options.lr ?? "lang_ro", hl=options.hl ?? "ro"`. URL = `https://customsearch.googleapis.com/customsearch/v1?${params}`. `fetch(url, { signal: AbortSignal.timeout(10000) })`. On `!response.ok` throw `new Error("CSE HTTP " + response.status + ": " + (await response.text().catch(()=> "")))`. Return `(json.items ?? [])`.

    NEW `functions/src/discover/cseNormalizer.ts` per RESEARCH Pattern 3 + 4:
    - Copy `rewriteToHttps()` from enrichImages.ts.
    - `const DOMAIN_TO_RETAILER: Record<string,string>` covering ALL 43 spec stores' domains (emag.ro→eMAG, altex.ro→Altex, mediagalaxy.ro→Media Galaxy, flanco.ro→Flanco, cel.ro→CEL.ro, pcgarage.ro→PC Garage, vexio.ro→Vexio, fashiondays.ro→Fashion Days, aboutyou.ro→About You, answear.ro→Answear, modivo.ro→Modivo, zalando.ro→Zalando, epantofi.ro→EPantofi, otter.ro→Otter, notino.ro→Notino, sephora.ro→Sephora, douglas.ro→Douglas, sabon.ro→Sabon, farmaciatei.ro→Farmacia Tei, bebetei.ro→Bebe Tei, ikea.com→IKEA, jysk.ro→JYSK, mobexpert.ro→Mobexpert, bonami.ro→Bonami, vivre.eu→Vivre, carturesti.ro→Cărturești, libris.ro→Libris, elefant.ro→Elefant, noriel.ro→Noriel, hobbyshop.ro→Hobby Shop, decathlon.ro→Decathlon, sportguru.ro→SportGuru, hervis.ro→Hervis, intersport.ro→Intersport, mothercare.ro→Mothercare, floria.ro→Floria, magnolia.ro→Magnolia, complice.ro→Complice, etsy.com→Etsy, breslo.ro→Breslo, kfea.ro→Kfea, delicateseflorescu.ro→Delicatese Florescu, nespresso.com→Nespresso). Use your judgment on the exact domain spelling — the test only pins www.emag.ro→eMAG and unknown→raw, so cover those plus the common stores.
    - `extractImageUrl(item)`: cse_image[0].src → cse_thumbnail[0].src → metatags[0]["og:image"] → "", each passed through rewriteToHttps when present.
    - `extractRetailerName(displayLink)`: strip leading "www.", exact-match DOMAIN_TO_RETAILER, then root-domain (last two labels) match, else return raw displayLink.
    - `export function normalizeCseItems(items: CseItem[]): DiscoverProduct[]` — filter to items with link && title; map to `{ title, description: snippet?.slice(0,200) ?? "", image_url: extractImageUrl(item), price: 0, currency: "RON", retailer_url: item.link, retailer_name: extractRetailerName(item.displayLink) }`.
    - `export function dedupeByUrl(products: DiscoverProduct[]): DiscoverProduct[]` — iterate, `const seen = new Set<string>()`; for each product try `normalizeUrl(product.retailer_url).productId` inside try/catch (on throw, fall back to the raw retailer_url as the key); push only if unseen. (search.ts in Task 5 may inline this — but exporting it makes the cseNormalizer.test de-dupe case directly testable.)

    Run cseNormalizer tests — must pass. Commit GREEN part 2:
    `cd functions && git add src/discover/cseClient.ts src/discover/cseNormalizer.ts && git commit -m "feat(17-07): CSE client + normalizer (image priority, domain map, https, de-dupe)"`
  </action>
  <verify>
    <automated>cd functions && npm test -- --testPathPattern cseNormalizer 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/cseClient.ts` contains `export async function callCse(` and `customsearch.googleapis.com`
    - `functions/src/discover/cseNormalizer.ts` contains `export function normalizeCseItems(`
    - `functions/src/discover/cseNormalizer.ts` contains `cse_image` AND `cse_thumbnail` AND `og:image` (image priority chain)
    - `functions/src/discover/cseNormalizer.ts` contains `rewriteToHttps` and `"emag.ro"` and `price: 0`
    - `npm test -- --testPathPattern cseNormalizer` exits 0
  </acceptance_criteria>
  <done>CSE HTTP client + normalizer exist; image priority, domain→retailer map, https rewrite, price=0, and de-dupe all pass their unit tests.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 5: GREEN part 3 — re-architect search.ts orchestration, delete enrichImages.ts, deploy</name>
  <read_first>
    - functions/src/discover/search.ts (REPLACE only the cache-miss internals between cache lookup and cache write; preserve auth gate, validation, rate-limit, cache lookup, cache write, and onCall options exactly)
    - functions/src/discover/cseClient.ts + cseNormalizer.ts + geminiClient.ts + promptTemplate.ts + parseGeminiResponse.ts (the modules built in Tasks 3-4 — wire them here)
    - functions/src/discover/retailers.ts (selectSitesForQuery still used to feed contextSites into buildIntentPrompt — narrowed to a hint role; keep the export name OR rename to selectRetailersForContext and update retailers.test.ts accordingly)
    - functions/src/discover/enrichImages.ts (to be DELETED — confirm rewriteToHttps was copied into cseNormalizer in Task 4 first)
    - functions/src/registry/fetchOgMetadata.ts (DO NOT remove node-html-parser from package.json — this file still imports it)
    - functions/src/__tests__/discover/cseOrchestration.test.ts (the RED orchestration tests this task must turn GREEN)
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-RESEARCH.md (CSE Fan-Out Orchestration code example + the full orchestration flow Step 1-5 + Pitfall 4 timeout + Pitfall 5 no-tools)
  </read_first>
  <behavior>
    - On cache miss: call selectSitesForQuery(query) → buildIntentPrompt(query, sites) → callGeminiIntent(prompt, GEMINI_API_KEY.value()) → IntentResult
    - Fan-out: take the first 3 giftCategories; if 0, synthesize one category with cseQuery = the raw query. For each, callCse(cseQuery, CSE_API_KEY.value(), CSE_ENGINE_ID.value()) inside Promise.allSettled
    - For each fulfilled CSE result: normalizeCseItems, then de-dupe across all categories by normalized URL into a single flat products[] array
    - Cache + return UNCHANGED: only cache when products.length > 0; cachedAt = deadline; response { products, cached_at }
    - Never throw to client: Gemini failure → fallback to a single CSE call on the raw query; all CSE calls failing → return empty products, no cache write
  </behavior>
  <action>
    REPLACE the cache-miss block in `functions/src/discover/search.ts`. Remove imports of `selectSitesForQuery`+`buildPrompt`+`callGemini`+`parseGeminiResponse`+`enrichWithOgImages`; add imports for `buildIntentPrompt`, `callGeminiIntent`, `callCse`, `normalizeCseItems` (+ `dedupeByUrl` if exported), `CSE_API_KEY`, `CSE_ENGINE_ID`, and keep `DiscoverProduct` from parseGeminiResponse, `normalizeUrl` from urlNormalization, `selectSitesForQuery` from retailers.

    New cache-miss core (extract into an exported pure-ish function `runSearchPipeline(query, deps)` so cseOrchestration.test can drive it with mocked callCse/callGeminiIntent — deps = { intentFn, cseFn, apiKeyGemini, apiKeyCse, cx }):
    1. `const sites = selectSitesForQuery(query);`
    2. `const prompt = buildIntentPrompt(query, sites);`
    3. `let intent; try { intent = await intentFn(prompt, apiKeyGemini); } catch (e) { console.error("[discoverSearch] intent failed", e); intent = { giftCategories: [{ name: "", reason: "", cseQuery: query }] }; }`
    4. `let categories = (intent.giftCategories ?? []).slice(0, 3); if (categories.length === 0) categories = [{ name: "", reason: "", cseQuery: query }];`
    5. `const settled = await Promise.allSettled(categories.map(c => cseFn(c.cseQuery, apiKeyCse, cx).then(items => normalizeCseItems(items))));`
    6. de-dupe: flatten fulfilled results, run through the de-dupe-by-normalizeUrl path (use exported dedupeByUrl or inline the Set<productId> loop with try/catch around normalizeUrl); log rejected count via console.warn.
    7. return the flat `DiscoverProduct[]`.

    In `discoverSearchHandler`, wire `runSearchPipeline(query, { intentFn: callGeminiIntent, cseFn: callCse, apiKeyGemini: GEMINI_API_KEY.value(), apiKeyCse: CSE_API_KEY.value(), cx: CSE_ENGINE_ID.value() })` for the products. Keep the existing cache-write (`products.length > 0` guard, deadline cachedAt) and the `{ products, cached_at }` return.

    Update the `onCall` options: add `CSE_API_KEY, CSE_ENGINE_ID` to the `secrets` array (keep `GEMINI_API_KEY`). Keep `enforceAppCheck: true`, `region: "europe-west3"`, `timeoutSeconds: 90` (Pitfall 7 — do not reduce). Add the comment `// No google_search / grounding — products come from CSE (UAT-6 hallucination fix).`

    DELETE `functions/src/discover/enrichImages.ts` (`git rm`). Confirm `node-html-parser` stays in package.json (fetchOgMetadata.ts still imports it) — do NOT touch package.json.

    Build to confirm TypeScript compiles, then run the full discover suite. Commit GREEN part 3:
    `cd functions && npm run build && git add -A src/discover/ && git commit -m "feat(17-07): re-architect discoverSearch (Gemini intent → CSE fan-out → normalize → de-dupe); delete enrichImages"`

    Then deploy (the gate already set the secrets):
    `firebase deploy --only functions:discoverSearch`
  </action>
  <verify>
    <automated>cd functions && npm run build 2>&1 | tail -10 && npm test -- --testPathPattern discover 2>&1 | tail -25</automated>
  </verify>
  <acceptance_criteria>
    - `functions/src/discover/search.ts` contains `callGeminiIntent` and `callCse` and `Promise.allSettled` and `.slice(0, 3)`
    - `functions/src/discover/search.ts` does NOT contain `enrichWithOgImages` or `callGemini(` (old grounded call) or `google_search`
    - `functions/src/discover/search.ts` onCall `secrets` array contains `CSE_API_KEY` and `CSE_ENGINE_ID` and `GEMINI_API_KEY`
    - `functions/src/discover/enrichImages.ts` does NOT exist (`git rm`-ed)
    - `node-html-parser` still present in `functions/package.json` dependencies (fetchOgMetadata still needs it)
    - `cd functions && npm run build` exits 0 (TypeScript compiles)
    - `cd functions && npm test -- --testPathPattern discover` exits 0 (full discover suite green, including the existing cacheKey/rateLimit/urlNormalization/triggers tests — proving the community-popular path was not regressed)
    - `firebase deploy --only functions:discoverSearch` completes without error
  </acceptance_criteria>
  <done>discoverSearch orchestration is Gemini-intent → CSE fan-out → normalize → de-dupe; enrichImages.ts deleted; full discover test suite green; function deployed to europe-west3.</done>
</task>

</tasks>

<verification>
- `cd functions && npm test -- --testPathPattern discover` is fully green (new search-v2 specs + the unchanged cacheKey/rateLimit/urlNormalization/triggers specs).
- `cd functions && npm run build` compiles with zero TypeScript errors.
- `grep -rn "google_search" functions/src/discover/` returns NOTHING (grounding fully removed).
- `grep -rn "enrichWithOgImages\|enrichImages" functions/src/discover/` returns NOTHING (file deleted, no dangling imports).
- The CSE-access gate (Task 1) passed (curl 200, secrets set) — or the plan was halted and re-routed to Brave before any CSE code.
- `discoverSearch` is deployed; a manual live search returns real products (validated on-device in Plan 17-08).
</verification>

<success_criteria>
- Gemini is called intent-only (JSON mode, response_schema, no tools); products come exclusively from Google CSE.
- Max 3 CSE queries per search; Promise.allSettled tolerates partial CSE failure; empty intent falls back to one raw-query CSE call.
- CSE results normalized to the unchanged DiscoverProduct shape (https images, price=0, domain→retailer label), de-duped by normalized URL.
- The Callable wrapper (auth gate, App Check, anonymous rejection, query validation, rate-limit, 30-day cache) and the `{ products, cached_at }` response contract are unchanged → zero Android changes required.
- The community-popular path (getPopular.ts, triggers.ts) is untouched and its tests still pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-07-SUMMARY.md` documenting: CSE gate outcome (passed / pivoted to Brave), the final orchestration flow, files replaced/created/deleted, the DiscoverProduct contract confirmation (unchanged), and any deviations from the research param shapes.
</output>
