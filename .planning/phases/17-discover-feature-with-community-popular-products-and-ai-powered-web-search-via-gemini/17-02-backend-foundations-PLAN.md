---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 02
type: execute
wave: 2
depends_on:
  - "17-01"
files_modified:
  - functions/src/discover/urlNormalization.ts
  - functions/src/discover/retailers.ts
  - functions/src/discover/promptTemplate.ts
  - functions/src/discover/geminiClient.ts
  - functions/src/discover/parseGeminiResponse.ts
  - functions/src/discover/secrets.ts
  - functions/src/__tests__/discover/urlNormalization.test.ts
  - functions/src/__tests__/discover/parseGeminiResponse.test.ts
  - functions/src/__tests__/discover/cacheKeyNormalization.test.ts
  - functions/src/__tests__/discover/retailers.test.ts
  - functions/src/__tests__/discover/promptTemplate.test.ts
  - firestore.rules
  - firestore.indexes.json
  - tests/rules/firestore.rules.test.ts
autonomous: true
requirements:
  - D-16
  - D-17
  - D-23
  - D-24
  - D-27
  - D-28
  - D-29
  - D-30
  - D-43
  - D-45
  - D-46
  - D-48

must_haves:
  truths:
    - "Pure utility functions urlNormalization, parseGeminiResponse, retailers selection, and promptTemplate all return the documented shapes for documented inputs (unit-tested)"
    - "Firestore rules deny all client access to discoverCache + discoverRateLimits, and authenticated-non-anonymous read on popularItems"
    - "Firestore composite index for popularItems ordered by (registryCount desc, updatedAt desc) is declared in firestore.indexes.json"
    - "Gemini API key is sourced via 2nd-gen defineSecret('GEMINI_API_KEY') — never appears in code or logs"
  artifacts:
    - path: "functions/src/discover/urlNormalization.ts"
      provides: "normalizeUrl(url): { productId, canonicalUrl } pure function"
      contains: "export function normalizeUrl"
    - path: "functions/src/discover/retailers.ts"
      provides: "RETAILERS verbatim catalog + selectSitesForQuery(query) heuristic"
      contains: "export const RETAILERS"
    - path: "functions/src/discover/promptTemplate.ts"
      provides: "buildPrompt(query, sites): { systemPrompt, userPrompt }"
      contains: "export function buildPrompt"
    - path: "functions/src/discover/geminiClient.ts"
      provides: "callGemini(systemPrompt, userPrompt, apiKey): Promise<string> wrapping the generateContent HTTP call"
      contains: "gemini-2.5-flash"
    - path: "functions/src/discover/parseGeminiResponse.ts"
      provides: "parseGeminiResponse(raw): DiscoverProduct[] defensive parser"
      contains: "export function parseGeminiResponse"
    - path: "functions/src/discover/secrets.ts"
      provides: "GEMINI_API_KEY defineSecret declaration shared across callables"
      contains: "defineSecret(\"GEMINI_API_KEY\")"
    - path: "firestore.rules"
      provides: "Rules for popularItems, discoverCache, discoverRateLimits"
      contains: "match /popularItems/{productId}"
    - path: "firestore.indexes.json"
      provides: "Composite index for popularItems ordered by (registryCount desc, updatedAt desc)"
      contains: "popularItems"
  key_links:
    - from: "functions/src/discover/urlNormalization.ts"
      to: "Node.js crypto + URL"
      via: "createHash('sha256').update(canonicalUrl).digest('hex')"
      pattern: "crypto.*createHash.*sha256"
    - from: "functions/src/discover/retailers.ts"
      to: "selectSitesForQuery(query) heuristic"
      via: "Romanian + English keyword match (birthday/wedding/housewarming/baby/christmas)"
      pattern: "ziua de na.ster\\|wedding\\|nunt.\\|warming\\|bebelu\\|cr.ciun"
    - from: "firestore.rules"
      to: "popularItems read gate"
      via: "auth != null AND sign_in_provider != 'anonymous'"
      pattern: "sign_in_provider"
---

<objective>
Establish the backend foundation layer for Phase 17: all pure utility modules under `functions/src/discover/`, the Gemini secret declaration, the Firestore composite index, and the security rules + tests for the three new collections (`popularItems`, `discoverCache`, `discoverRateLimits`). No Callables yet (plan 17-03). No triggers yet (plan 17-04). No Android yet (plan 17-05).

Purpose: De-risk plan 17-03 (Callables) and plan 17-04 (triggers) by getting the small, pure, easily-tested pieces locked down first with full unit-test coverage. Per CONTEXT.md D-16, D-17, D-24, D-27, D-28, D-29, D-30, D-43, D-45, D-48.

Output: 6 source files in `functions/src/discover/`, 5 test files in `functions/src/__tests__/discover/`, updated `firestore.rules` (3 new collection rules, no removals — plan 17-01 handles removal), updated `firestore.indexes.json` (1 new composite index), updated `tests/rules/firestore.rules.test.ts` (3 new describe blocks).
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md
@CLAUDE.md
</context>

<interfaces>
<!-- Reference implementations from the existing codebase to mirror. -->

From functions/src/registry/fetchOgMetadata.ts (existing Callable pattern):
```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
// ...
export const fetchOgMetadata = onCall(
  { region: "europe-west3" },
  async (request): Promise<OgMetadataResponse> => fetchOgMetadataHandler(request)
);
```

From firestore.rules (existing helper pattern — extend with non-anonymous check):
```
function isSignedIn() {
  return request.auth != null;
}
```

From functions/package.json (existing tech stack):
```json
"engines": { "node": "22" },
"dependencies": {
  "firebase-admin": "^13.7.0",
  "firebase-functions": "^7.2.3",
  "node-html-parser": "^7.1.0"
}
```
Note: Native `fetch` is available in Node 22 — no `node-fetch` dependency needed.

Test framework: Jest + ts-jest (per functions/package.json devDependencies). Test files live at `functions/src/__tests__/*.test.ts`.
</interfaces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: urlNormalization + retailers + promptTemplate + parseGeminiResponse pure modules + unit tests</name>

  <behavior>
    - urlNormalization.normalizeUrl: lowercase host, strip listed tracking params, sort remaining, drop fragment, strip trailing slash, force https → identical productId for "same product with different utm/ref" inputs
    - urlNormalization.normalizeUrl: returns sha256 hex (64 chars) + canonicalUrl string
    - retailers.RETAILERS: exposes 6 categories matching the verbatim spec arrays
    - retailers.selectSitesForQuery: "ziua de naștere" → birthday + universal; "nuntă" → wedding + universal; "casă nouă" → housewarming + universal; "bebeluș" → baby_shower + universal; "crăciun" → christmas + universal; "cafetiera espresso" → universal only
    - promptTemplate.buildPrompt: embeds "prioritize results from: [comma-separated hosts]" with the chosen sites list, includes "Return ONLY a strict JSON array" instruction, accepts user query verbatim
    - parseGeminiResponse: valid JSON array → parsed items; markdown code fences (```json ... ``` or ``` ... ```) → stripped before parse; non-array → []; missing required fields (title/price/retailer_url) → item dropped; price as string "199.99" → coerced to 199.99 number; description > 200 chars → truncated; malformed JSON → [] + console.error called
    - cacheKeyNormalization (exposed via a helper alongside urlNormalization OR inline in search.ts; this plan locks it as `normalizeCacheKey(q): string` in a new helper file): lowercase + trim + collapse whitespace; Romanian diacritics preserved (ă, î, ș, ț, â)
  </behavior>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-16, D-17, D-24, D-28, D-29, D-30 verbatim — copy the prompt template and retailer arrays exactly)
    - functions/src/registry/fetchOgMetadata.ts (existing Callable code style — TypeScript strict, no semicolons-disabled, explicit return types)
    - functions/tsconfig.json (verify "strict": true and target ES2022 / Node 22)
    - functions/package.json (confirm Jest + ts-jest setup; native fetch available in Node 22)
    - functions/src/__tests__/fetchOgMetadata.test.ts (existing test file structure to mirror — Jest `describe`/`it` style, imports from "../registry/fetchOgMetadata")
  </read_first>

  <files>
    functions/src/discover/urlNormalization.ts,
    functions/src/discover/retailers.ts,
    functions/src/discover/promptTemplate.ts,
    functions/src/discover/parseGeminiResponse.ts,
    functions/src/discover/cacheKey.ts,
    functions/src/__tests__/discover/urlNormalization.test.ts,
    functions/src/__tests__/discover/retailers.test.ts,
    functions/src/__tests__/discover/promptTemplate.test.ts,
    functions/src/__tests__/discover/parseGeminiResponse.test.ts,
    functions/src/__tests__/discover/cacheKeyNormalization.test.ts
  </files>

  <action>
    Create five pure-function TypeScript modules and their Jest unit tests. All values are dictated verbatim by CONTEXT.md decisions D-16, D-24, D-28, D-29, D-30.

    **1. `functions/src/discover/urlNormalization.ts`** — implements D-16, D-17. Use Node's built-in `crypto` and `URL`:
    ```typescript
    import { createHash } from "crypto";

    /**
     * Phase 17 D-16: tracking params stripped before canonicalization. Order
     * matters only for the final reconstruction; the membership check is what
     * actually deduplicates.
     */
    const TRACKING_PARAMS = new Set([
      "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
      "fbclid", "gclid", "mc_cid", "mc_eid",
      // EMAG affiliate suffix params
      "ref", "affiliate_id", "cmpid",
    ]);

    export interface NormalizedUrl {
      productId: string;     // 64-char sha256 hex of canonicalUrl
      canonicalUrl: string;  // normalized form
    }

    /**
     * D-16 normalization pipeline:
     *  1. Parse URL.
     *  2. Lowercase host.
     *  3. Strip tracking params from TRACKING_PARAMS.
     *  4. Sort remaining params alphabetically by key.
     *  5. Drop fragment.
     *  6. Strip trailing slash from path (unless path is exactly "/").
     *  7. Reconstruct as `https://{host}{path}{?sortedQuery}` (force https).
     *
     * Returns the canonical URL string AND a sha256-hex productId.
     */
    export function normalizeUrl(url: string): NormalizedUrl {
      const parsed = new URL(url);
      const host = parsed.host.toLowerCase();

      // Strip tracking params, then collect remaining as [key, value] pairs.
      const remaining: Array<[string, string]> = [];
      for (const [key, value] of parsed.searchParams.entries()) {
        if (!TRACKING_PARAMS.has(key.toLowerCase())) {
          remaining.push([key, value]);
        }
      }
      remaining.sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0));

      let path = parsed.pathname;
      if (path !== "/" && path.endsWith("/")) {
        path = path.slice(0, -1);
      }

      const queryString = remaining.length
        ? "?" + remaining.map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join("&")
        : "";

      const canonicalUrl = `https://${host}${path}${queryString}`;
      const productId = createHash("sha256").update(canonicalUrl).digest("hex");
      return { productId, canonicalUrl };
    }
    ```

    **2. `functions/src/discover/retailers.ts`** — implements D-28. Copy the RETAILERS object **verbatim** from CONTEXT.md `<specifics>` section:
    ```typescript
    /**
     * Phase 17 D-28: Romanian retailer category lists, exact verbatim from spec.
     * Site-list selection is by heuristic occasion detection on user query.
     */
    export const RETAILERS = {
      universal: ["emag.ro", "altex.ro", "carrefour.ro", "vivre.eu", "elefant.ro", "flanco.ro"],
      birthday: ["mindblower.ro", "funfox.ro", "borealy.ro", "douglas.ro", "sephora.ro", "libris.ro", "carturesti.ro"],
      wedding: ["23h.ro", "crisiashop.ro", "wedday.ro", "happycards.ro", "magazinulmireselor.ro"],
      housewarming: ["jysk.ro", "mobexpert.ro", "ikea.com/ro", "dedeman.ro", "leroymerlin.ro", "vivre.eu", "insignis.ro", "kika.ro", "somproduct.ro"],
      baby_shower: ["bekid.ro", "babyneeds.ro", "bebelul.ro", "bebebliss.ro", "bebenou.ro", "chicco.ro", "erfi.ro", "babymatters.ro", "noriel.ro"],
      christmas: ["borealy.ro", "mindblower.ro", "funfox.ro", "gourmetgift.ro", "douglas.ro", "sephora.ro", "kaufland.ro", "lidl.ro"],
    } as const;

    export type OccasionCategory = keyof typeof RETAILERS;

    /**
     * D-28: Romanian + English keyword match → occasion category.
     * Default = universal only (returns RETAILERS.universal).
     * When a category matches, returns universal + category concatenated.
     * Keyword matching is case-insensitive simple substring (D-28 "keyword
     * sufficient for v1" per Claude's discretion in CONTEXT.md).
     */
    export function selectSitesForQuery(query: string): string[] {
      const q = query.toLowerCase();
      const matches = (...needles: string[]): boolean => needles.some(n => q.includes(n));

      if (matches("ziua de naștere", "ziua de nastere", "birthday")) {
        return [...RETAILERS.universal, ...RETAILERS.birthday];
      }
      if (matches("nuntă", "nunta", "wedding")) {
        return [...RETAILERS.universal, ...RETAILERS.wedding];
      }
      if (matches("casă nouă", "casa noua", "warming", "mutare", "housewarming")) {
        return [...RETAILERS.universal, ...RETAILERS.housewarming];
      }
      if (matches("bebeluș", "bebelus", "baby", "shower")) {
        return [...RETAILERS.universal, ...RETAILERS.baby_shower];
      }
      if (matches("crăciun", "craciun", "christmas")) {
        return [...RETAILERS.universal, ...RETAILERS.christmas];
      }
      return [...RETAILERS.universal];
    }
    ```

    **3. `functions/src/discover/promptTemplate.ts`** — implements D-29. Copy the prompt **verbatim** from CONTEXT.md D-29:
    ```typescript
    /**
     * Phase 17 D-29: Gemini prompt template — verbatim from CONTEXT.md.
     * No paraphrasing — schema and instructions are part of the contract with
     * the parsing layer (parseGeminiResponse.ts).
     */
    export interface BuiltPrompt {
      systemPrompt: string;
      userPrompt: string;
    }

    export function buildPrompt(query: string, sites: string[]): BuiltPrompt {
      const sitesList = sites.join(", ");
      const systemPrompt =
        "You are a product-discovery assistant for a Romanian gift-registry app. " +
        "Search the Romanian web for products matching the user's query. " +
        `Prioritize results from these Romanian retailers (highest priority first): ${sitesList}. ` +
        "Return prices in RON (Romanian lei). " +
        "Return ONLY a strict JSON array — no prose, no markdown code fences, no explanation. " +
        "Schema per item: {title, description, image_url, price, currency, retailer_url, retailer_name}. " +
        "Return between 5 and 15 items; if fewer than 5 confident matches exist, return what's available — never pad with low-quality matches. " +
        "Drop items missing title, price, or retailer_url.";
      const userPrompt = query;
      return { systemPrompt, userPrompt };
    }
    ```

    **4. `functions/src/discover/parseGeminiResponse.ts`** — implements D-30:
    ```typescript
    export interface DiscoverProduct {
      title: string;
      description: string;
      image_url: string;
      price: number;
      currency: string;
      retailer_url: string;
      retailer_name: string;
    }

    /**
     * Phase 17 D-30: defensive Gemini response parser.
     *  1. Strip markdown code fences (```json...``` or ```...```).
     *  2. JSON.parse inside try/catch — failure → [] + console.error.
     *  3. Verify array — else [].
     *  4. Per item: drop if missing title|price|retailer_url; coerce price to number;
     *     description truncate to 200; image_url default "".
     *  5. NEVER throw to client.
     */
    export function parseGeminiResponse(raw: string, query?: string): DiscoverProduct[] {
      // 1. Strip code fences.
      let cleaned = raw.trim();
      cleaned = cleaned.replace(/^\s*```(?:json)?\s*\n?/i, "");
      cleaned = cleaned.replace(/\n?\s*```\s*$/, "");

      // 2. Parse.
      let parsed: unknown;
      try {
        parsed = JSON.parse(cleaned);
      } catch (err) {
        console.error("Gemini parse failed", { rawResponse: raw, query });
        return [];
      }

      // 3. Verify array.
      if (!Array.isArray(parsed)) {
        console.error("Gemini parse: non-array root", { rawResponse: raw, query });
        return [];
      }

      // 4. Per-item validation.
      const out: DiscoverProduct[] = [];
      for (const item of parsed) {
        if (item == null || typeof item !== "object") continue;
        const rec = item as Record<string, unknown>;
        const title = typeof rec.title === "string" ? rec.title : undefined;
        const retailerUrl = typeof rec.retailer_url === "string" ? rec.retailer_url : undefined;
        let price: number | undefined;
        if (typeof rec.price === "number") price = rec.price;
        else if (typeof rec.price === "string") {
          const n = parseFloat(rec.price);
          if (!isNaN(n)) price = n;
        }
        if (!title || price === undefined || !retailerUrl) continue;
        const descriptionRaw = typeof rec.description === "string" ? rec.description : "";
        const description = descriptionRaw.length > 200 ? descriptionRaw.slice(0, 200) : descriptionRaw;
        out.push({
          title,
          description,
          image_url: typeof rec.image_url === "string" ? rec.image_url : "",
          price,
          currency: typeof rec.currency === "string" ? rec.currency : "RON",
          retailer_url: retailerUrl,
          retailer_name: typeof rec.retailer_name === "string" ? rec.retailer_name : "",
        });
      }
      return out;
    }
    ```

    **5. `functions/src/discover/cacheKey.ts`** — implements D-24:
    ```typescript
    /**
     * Phase 17 D-24: Firestore doc-ID-safe cache key for discoverSearch.
     * lowercase + trim + collapse whitespace; Romanian diacritics preserved.
     * URL-encodes path-segment-illegal characters so the result is a valid
     * Firestore document ID.
     */
    export function normalizeCacheKey(query: string): string {
      const normalized = query.toLowerCase().trim().replace(/\s+/g, " ");
      // Firestore doc IDs reject "/", and have a 1500-byte limit. Encode to be safe.
      return encodeURIComponent(normalized);
    }
    ```

    **6-10. Unit tests in `functions/src/__tests__/discover/`**:

    **`urlNormalization.test.ts`** (D-48 verbatim cases):
    ```typescript
    import { normalizeUrl } from "../../discover/urlNormalization";

    describe("normalizeUrl", () => {
      it("returns the same productId for same product with different utm params", () => {
        const a = normalizeUrl("https://emag.ro/products/x?utm_source=fb&utm_campaign=spring");
        const b = normalizeUrl("https://emag.ro/products/x?utm_source=google");
        expect(a.productId).toBe(b.productId);
      });
      it("is independent of query-param order", () => {
        const a = normalizeUrl("https://emag.ro/x?a=1&b=2");
        const b = normalizeUrl("https://emag.ro/x?b=2&a=1");
        expect(a.productId).toBe(b.productId);
      });
      it("treats trailing slash as identical", () => {
        const a = normalizeUrl("https://emag.ro/x/");
        const b = normalizeUrl("https://emag.ro/x");
        expect(a.productId).toBe(b.productId);
      });
      it("strips fragment", () => {
        const a = normalizeUrl("https://emag.ro/x#section");
        const b = normalizeUrl("https://emag.ro/x");
        expect(a.productId).toBe(b.productId);
      });
      it("lowercases host", () => {
        const a = normalizeUrl("https://EMAG.ro/x");
        const b = normalizeUrl("https://emag.ro/x");
        expect(a.productId).toBe(b.productId);
      });
      it("forces http to https", () => {
        const a = normalizeUrl("http://emag.ro/x");
        const b = normalizeUrl("https://emag.ro/x");
        expect(a.productId).toBe(b.productId);
      });
      it("strips EMAG affiliate suffix params (ref, affiliate_id, cmpid)", () => {
        const a = normalizeUrl("https://emag.ro/x?ref=aff123&affiliate_id=42&cmpid=xyz");
        const b = normalizeUrl("https://emag.ro/x");
        expect(a.productId).toBe(b.productId);
      });
      it("returns sha256 hex (64 chars) for productId", () => {
        const { productId } = normalizeUrl("https://emag.ro/x");
        expect(productId).toMatch(/^[a-f0-9]{64}$/);
      });
      it("preserves non-tracking query params", () => {
        const a = normalizeUrl("https://emag.ro/x?color=red");
        expect(a.canonicalUrl).toContain("color=red");
      });
    });
    ```

    **`retailers.test.ts`** — at minimum, assert RETAILERS contains the 6 categories with the exact spec arrays, and `selectSitesForQuery` returns the expected lists for: Romanian birthday phrase, Romanian wedding phrase, Romanian housewarming, Romanian baby_shower, Romanian christmas, English equivalents, and a neutral query ("cafetiera espresso") returning universal only. Use `expect(sites).toContain("emag.ro")` and `expect(sites).toContain("mindblower.ro")` for the birthday case, etc.

    **`promptTemplate.test.ts`** — assert: `systemPrompt` contains the verbatim "Return ONLY a strict JSON array" string, contains every site from the supplied sites array, contains "Schema per item:", and `userPrompt` equals the input query verbatim.

    **`parseGeminiResponse.test.ts`** (D-48 verbatim cases):
    ```typescript
    import { parseGeminiResponse } from "../../discover/parseGeminiResponse";

    describe("parseGeminiResponse", () => {
      it("parses a valid JSON array", () => {
        const raw = JSON.stringify([{
          title: "T1", description: "d", image_url: "u", price: 10, currency: "RON",
          retailer_url: "https://emag.ro/x", retailer_name: "eMAG",
        }]);
        expect(parseGeminiResponse(raw)).toHaveLength(1);
      });
      it("strips ```json``` fences before parsing", () => {
        const raw = "```json\n[" + JSON.stringify({
          title: "T", price: 5, retailer_url: "https://x", currency: "RON",
          description: "", image_url: "", retailer_name: "",
        }) + "]\n```";
        expect(parseGeminiResponse(raw)).toHaveLength(1);
      });
      it("strips plain ``` fences before parsing", () => {
        const raw = "```\n[" + JSON.stringify({
          title: "T", price: 5, retailer_url: "https://x", currency: "RON",
          description: "", image_url: "", retailer_name: "",
        }) + "]\n```";
        expect(parseGeminiResponse(raw)).toHaveLength(1);
      });
      it("returns [] on malformed JSON", () => {
        const spy = jest.spyOn(console, "error").mockImplementation(() => {});
        expect(parseGeminiResponse("not json")).toEqual([]);
        expect(spy).toHaveBeenCalled();
        spy.mockRestore();
      });
      it("drops items missing title/price/retailer_url", () => {
        const raw = JSON.stringify([
          { title: "ok", price: 1, retailer_url: "u" },
          { title: "no-price", retailer_url: "u" },          // dropped
          { price: 1, retailer_url: "u" },                    // dropped (no title)
          { title: "no-url", price: 1 },                      // dropped (no retailer_url)
        ]);
        expect(parseGeminiResponse(raw)).toHaveLength(1);
      });
      it("coerces price as string to number", () => {
        const raw = JSON.stringify([{ title: "x", price: "199.99", retailer_url: "u" }]);
        expect(parseGeminiResponse(raw)[0]?.price).toBe(199.99);
      });
      it("drops item when price string is not parseable", () => {
        const raw = JSON.stringify([{ title: "x", price: "free", retailer_url: "u" }]);
        expect(parseGeminiResponse(raw)).toHaveLength(0);
      });
      it("truncates description over 200 chars", () => {
        const longDesc = "a".repeat(250);
        const raw = JSON.stringify([{ title: "x", description: longDesc, price: 1, retailer_url: "u" }]);
        expect(parseGeminiResponse(raw)[0]?.description).toHaveLength(200);
      });
      it("defaults image_url to '' when missing", () => {
        const raw = JSON.stringify([{ title: "x", price: 1, retailer_url: "u" }]);
        expect(parseGeminiResponse(raw)[0]?.image_url).toBe("");
      });
      it("returns [] when root is not an array", () => {
        const spy = jest.spyOn(console, "error").mockImplementation(() => {});
        expect(parseGeminiResponse(JSON.stringify({ products: [] }))).toEqual([]);
        spy.mockRestore();
      });
    });
    ```

    **`cacheKeyNormalization.test.ts`** (D-48 verbatim cases):
    ```typescript
    import { normalizeCacheKey } from "../../discover/cacheKey";

    describe("normalizeCacheKey", () => {
      it("lowercases the input", () => {
        expect(normalizeCacheKey("Espresso")).toContain("espresso");
      });
      it("trims leading and trailing whitespace", () => {
        expect(normalizeCacheKey("  espresso  ")).toBe(normalizeCacheKey("espresso"));
      });
      it("collapses internal whitespace", () => {
        expect(normalizeCacheKey("espresso   machine")).toBe(normalizeCacheKey("espresso machine"));
      });
      it("preserves Romanian diacritics", () => {
        const out = normalizeCacheKey("cadou pentru bunică");
        // URL-encoded but the diacritics survive (after decoding)
        expect(decodeURIComponent(out)).toContain("bunică");
      });
    });
    ```

    All tests should pass: `cd functions && npm test -- --testPathPattern=discover`. Use Jest spies on `console.error` only where needed (the negative paths) to keep test output clean.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # Files exist with expected exports
      grep -q "export function normalizeUrl" functions/src/discover/urlNormalization.ts
      grep -q "export const RETAILERS" functions/src/discover/retailers.ts
      grep -q "export function selectSitesForQuery" functions/src/discover/retailers.ts
      grep -q "export function buildPrompt" functions/src/discover/promptTemplate.ts
      grep -q "export function parseGeminiResponse" functions/src/discover/parseGeminiResponse.ts
      grep -q "export function normalizeCacheKey" functions/src/discover/cacheKey.ts
      # RETAILERS verbatim sanity check: birthday includes mindblower.ro, housewarming includes ikea.com/ro
      grep -q "mindblower.ro" functions/src/discover/retailers.ts
      grep -q "ikea.com/ro" functions/src/discover/retailers.ts
      grep -q "magazinulmireselor.ro" functions/src/discover/retailers.ts
      # Prompt template verbatim sanity
      grep -q "Return ONLY a strict JSON array" functions/src/discover/promptTemplate.ts
      grep -q "Return between 5 and 15 items" functions/src/discover/promptTemplate.ts
      # Tests pass
      cd functions && npm test -- --testPathPattern=discover --silent 2>&1 | tail -30
      echo OK
      '
    </automated>
  </verify>

  <done>
    Five pure modules exist in `functions/src/discover/` with documented exports. Five Jest test files exist in `functions/src/__tests__/discover/` and all pass. Spec-verbatim strings preserved: RETAILERS arrays, prompt template "Return ONLY a strict JSON array", "Return between 5 and 15 items", schema field names. All 30+ test cases pass.
  </done>
</task>

<task type="auto">
  <name>Task 2: geminiClient + secret declaration + Firestore rules + indexes + rules tests</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-27 verbatim Gemini endpoint + model, D-43 verbatim rules, D-45 TTL documentation, Claude's discretion on composite index)
    - functions/src/discover/parseGeminiResponse.ts (created in Task 1 — geminiClient does not parse, but its output is fed into parseGeminiResponse)
    - functions/src/discover/promptTemplate.ts (created in Task 1 — geminiClient consumes BuiltPrompt)
    - firestore.rules (updated by plan 17-01 — config/{configId} removed; this task ADDS three new rules without touching the rest)
    - firestore.indexes.json (current — has 5 indexes; add one for popularItems)
    - tests/rules/firestore.rules.test.ts (updated by plan 17-01 — config/stores describe removed; this task ADDS three new describe blocks)
    - tests/rules/jest.config.js (Jest config — confirm test path matching for new describe blocks)
  </read_first>

  <files>
    functions/src/discover/secrets.ts,
    functions/src/discover/geminiClient.ts,
    firestore.rules,
    firestore.indexes.json,
    tests/rules/firestore.rules.test.ts
  </files>

  <action>
    Wire the Gemini HTTP client to the secret manager, lock down Firestore rules for the three new collections, add the composite index, and add rules tests. Per CONTEXT.md D-27, D-43, D-45.

    **1. `functions/src/discover/secrets.ts`** — define the secret once so plan 17-03 and any future Gemini callers share the declaration:
    ```typescript
    import { defineSecret } from "firebase-functions/params";

    /**
     * Phase 17 D-27: Gemini API key. 2nd-gen Functions secret (NOT
     * functions.config()). Declared once here; Callables that need Gemini
     * include it in their onCall `secrets: [GEMINI_API_KEY]` option.
     *
     * NEVER log this value. NEVER expose to Android.
     */
    export const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
    ```

    **2. `functions/src/discover/geminiClient.ts`** — thin HTTP wrapper around the Gemini generateContent endpoint with `google_search` tool enabled. D-27 endpoint + model verbatim:
    ```typescript
    import type { BuiltPrompt } from "./promptTemplate";

    /**
     * Phase 17 D-27: Gemini 2.5 Flash call with google_search grounding tool.
     * Returns the raw text the model produced (single text candidate). The
     * caller (search.ts) feeds this directly into parseGeminiResponse.
     *
     * Uses Node 22 built-in fetch — no node-fetch dependency.
     *
     * Throws on network errors and non-2xx HTTP responses; the caller wraps
     * those in HttpsError before they reach the client.
     */
    export async function callGemini(prompt: BuiltPrompt, apiKey: string): Promise<string> {
      const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(apiKey)}`;

      const body = {
        systemInstruction: {
          parts: [{ text: prompt.systemPrompt }],
        },
        contents: [{
          role: "user",
          parts: [{ text: prompt.userPrompt }],
        }],
        tools: [{ google_search: {} }],
      };

      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
        signal: AbortSignal.timeout(20000),  // 20 s — google_search grounding can be slow
      });

      if (!response.ok) {
        const errText = await response.text().catch(() => "<unreadable>");
        throw new Error(`Gemini HTTP ${response.status}: ${errText.slice(0, 500)}`);
      }

      const json = await response.json() as {
        candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
      };
      const text = json?.candidates?.[0]?.content?.parts?.map(p => p.text ?? "").join("") ?? "";
      return text;
    }
    ```

    Note: This function is hard to unit-test without mocking `global.fetch`. Per D-50 precedent (no UI tests, ViewModel/repository/unit-tests of pure functions suffice), we DO NOT add a test for `geminiClient.ts` — its behavior is exercised end-to-end via plan 17-06 smoke test.

    **3. `firestore.rules`** — append three new `match` blocks **inside** the existing `match /databases/{database}/documents { … }` block. Place them immediately after the `users/{userId}/notifications/{notificationId}` block (preserve all existing rules; plan 17-01 has already deleted `config/{configId}`):
    ```
        // Phase 17 D-43: popularItems — global community popularity counter docs.
        // Maintained by Cloud Functions triggers (Admin SDK bypasses rules).
        // Reads allowed for any authenticated NON-anonymous user.
        // Anonymous web-fallback guests (Phase 5) are excluded — Discover is
        // for registered users only per D-12.
        match /popularItems/{productId} {
          allow read: if request.auth != null
                      && request.auth.token.firebase.sign_in_provider != "anonymous";
          allow write: if false;
        }

        // Phase 17 D-43: discoverCache — server-only cache for Gemini search
        // results. No client read or write — Cloud Functions Admin SDK only.
        match /discoverCache/{normalizedQuery} {
          allow read, write: if false;
        }

        // Phase 17 D-43: discoverRateLimits — server-only per-user rate-limit
        // counters for discoverSearch. No client read or write — prevents
        // client-side tampering of the call timestamp array.
        match /discoverRateLimits/{uid} {
          allow read, write: if false;
        }
    ```

    **4. `firestore.indexes.json`** — append one new composite index to the `indexes` array (per CONTEXT.md "Claude's Discretion" note: composite index required for `popularItems orderBy registryCount desc, updatedAt desc`):
    ```json
        {
          "collectionGroup": "popularItems",
          "queryScope": "COLLECTION",
          "fields": [
            { "fieldPath": "registryCount", "order": "DESCENDING" },
            { "fieldPath": "updatedAt", "order": "DESCENDING" }
          ]
        }
    ```
    Add a leading comma after the last existing index entry. The final `fieldOverrides: []` array stays untouched.

    **5. `tests/rules/firestore.rules.test.ts`** — append three new `describe()` blocks at the end of the file (after the legacy registry tests, before `afterAll`). Use the existing test helpers (`testEnv.authenticatedContext`, `testEnv.unauthenticatedContext`, `withSecurityRulesDisabled` for seeding):

    ```typescript
    // ─────────────────────────────────────────────────────────────────────────
    // describe("popularItems rules — Phase 17 D-43")
    // ─────────────────────────────────────────────────────────────────────────
    describe("popularItems rules", () => {
      beforeEach(async () => {
        await testEnv.withSecurityRulesDisabled(async (ctx) => {
          await setDoc(doc(ctx.firestore(), "popularItems", "p1"), {
            canonicalUrl: "https://emag.ro/x",
            title: "Item",
            registryCount: 3,
            registryIds: ["r1", "r2", "r3"],
          });
        });
      });

      it("allows authenticated non-anonymous read", async () => {
        const db = testEnv.authenticatedContext("user1").firestore();
        await assertSucceeds(getDoc(doc(db, "popularItems", "p1")));
      });

      it("denies unauthenticated read", async () => {
        const db = testEnv.unauthenticatedContext().firestore();
        await assertFails(getDoc(doc(db, "popularItems", "p1")));
      });

      it("denies anonymous-provider read", async () => {
        const db = testEnv.authenticatedContext("anon-user", {
          firebase: { sign_in_provider: "anonymous" },
        }).firestore();
        await assertFails(getDoc(doc(db, "popularItems", "p1")));
      });

      it("denies all client writes (even authenticated)", async () => {
        const db = testEnv.authenticatedContext("user1").firestore();
        await assertFails(setDoc(doc(db, "popularItems", "p1"), { registryCount: 999 }));
      });
    });

    // ─────────────────────────────────────────────────────────────────────────
    // describe("discoverCache rules — Phase 17 D-43")
    // ─────────────────────────────────────────────────────────────────────────
    describe("discoverCache rules", () => {
      it("denies authenticated read", async () => {
        await testEnv.withSecurityRulesDisabled(async (ctx) => {
          await setDoc(doc(ctx.firestore(), "discoverCache", "q1"), { results: [] });
        });
        const db = testEnv.authenticatedContext("user1").firestore();
        await assertFails(getDoc(doc(db, "discoverCache", "q1")));
      });

      it("denies authenticated write", async () => {
        const db = testEnv.authenticatedContext("user1").firestore();
        await assertFails(setDoc(doc(db, "discoverCache", "q1"), { results: [] }));
      });
    });

    // ─────────────────────────────────────────────────────────────────────────
    // describe("discoverRateLimits rules — Phase 17 D-43")
    // ─────────────────────────────────────────────────────────────────────────
    describe("discoverRateLimits rules", () => {
      it("denies authenticated self read (prevents tampering enumeration)", async () => {
        await testEnv.withSecurityRulesDisabled(async (ctx) => {
          await setDoc(doc(ctx.firestore(), "discoverRateLimits", "u1"), { timestamps: [] });
        });
        const db = testEnv.authenticatedContext("u1").firestore();
        await assertFails(getDoc(doc(db, "discoverRateLimits", "u1")));
      });

      it("denies authenticated self write", async () => {
        const db = testEnv.authenticatedContext("u1").firestore();
        await assertFails(setDoc(doc(db, "discoverRateLimits", "u1"), { timestamps: [] }));
      });
    });
    ```

    Note: `testEnv.authenticatedContext("anon-user", { firebase: { sign_in_provider: "anonymous" } })` is the firebase-rules-unit-testing API for forging custom auth token claims; verify the exact API surface against the existing test file's imports before invocation.

    **TTL documentation** — D-45 says to "document the exact commands in the plan." Per CONTEXT.md these are configured at deploy time. Add a comment block at the bottom of `firestore.indexes.json`'s nearest documenting file (or create a docs entry inside the indexes file via JSON comments — JSON doesn't support comments, so this lives in the plan summary instead). For this plan's purpose, the TTL gcloud commands documented here are:
    ```
    gcloud firestore fields ttls update cachedAt --collection-group=discoverCache --enable-ttl
    gcloud firestore fields ttls update lastWriteAt --collection-group=discoverRateLimits --enable-ttl
    ```
    These are run from plan 17-06 (deploy plan). They do not need to be persisted in a source file; the plan-17-06 task action will include them verbatim.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      # Source files exist
      grep -q "defineSecret(\"GEMINI_API_KEY\")" functions/src/discover/secrets.ts
      grep -q "export async function callGemini" functions/src/discover/geminiClient.ts
      grep -q "gemini-2.5-flash:generateContent" functions/src/discover/geminiClient.ts
      grep -q "google_search" functions/src/discover/geminiClient.ts
      # Firestore rules — three new blocks present, config/{configId} absent (from plan 17-01)
      grep -q "match /popularItems/{productId}" firestore.rules
      grep -q "match /discoverCache/{normalizedQuery}" firestore.rules
      grep -q "match /discoverRateLimits/{uid}" firestore.rules
      grep -q "sign_in_provider != \"anonymous\"" firestore.rules
      ! grep -q "match /config/{configId}" firestore.rules
      # Firestore index added
      grep -q "popularItems" firestore.indexes.json
      grep -q "registryCount" firestore.indexes.json
      # Rules tests added
      grep -q "popularItems rules" tests/rules/firestore.rules.test.ts
      grep -q "discoverCache rules" tests/rules/firestore.rules.test.ts
      grep -q "discoverRateLimits rules" tests/rules/firestore.rules.test.ts
      # All rules tests still pass
      cd tests/rules && npm test --silent 2>&1 | tail -20
      # TypeScript compiles
      cd ../../functions && npm run build 2>&1 | tail -10
      echo OK
      '
    </automated>
  </verify>

  <done>
    `functions/src/discover/secrets.ts` declares GEMINI_API_KEY via defineSecret. `functions/src/discover/geminiClient.ts` exports `callGemini` calling `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` with `tools: [{ google_search: {} }]`. `firestore.rules` has three new `match` blocks for popularItems / discoverCache / discoverRateLimits and the popularItems block gates on non-anonymous auth. `firestore.indexes.json` has the new composite index. `tests/rules/firestore.rules.test.ts` has three new describe blocks and all tests pass. `npm run build` in functions/ succeeds.
  </done>
</task>

</tasks>

<verification>
After both tasks:

1. `cd functions && npm run build` succeeds (no TypeScript errors).
2. `cd functions && npm test -- --testPathPattern=discover` runs the 5 new test files and all pass.
3. `cd tests/rules && npm test` runs all rule tests including the 3 new describe blocks; all pass.
4. `grep -c "match /popularItems\|match /discoverCache\|match /discoverRateLimits" firestore.rules` returns 3.
5. `grep -A1 "popularItems" firestore.indexes.json | grep -c "registryCount\|updatedAt"` returns at least 2 (both fields declared in the index).
</verification>

<success_criteria>
- Six new TypeScript source files under `functions/src/discover/`: urlNormalization, retailers, promptTemplate, parseGeminiResponse, cacheKey, secrets, geminiClient.
- Five Jest test files under `functions/src/__tests__/discover/`: urlNormalization, retailers, promptTemplate, parseGeminiResponse, cacheKeyNormalization — all passing.
- firestore.rules gains 3 new match blocks (popularItems, discoverCache, discoverRateLimits) with D-43 verbatim policies.
- firestore.indexes.json gains 1 composite index for popularItems.
- tests/rules/firestore.rules.test.ts gains 3 new describe blocks covering the new rules.
- functions/ TypeScript builds cleanly.
- All 5 D-48 backend unit test categories (parseGeminiResponse, urlNormalization, cacheKeyNormalization, plus retailers and promptTemplate) pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-02-SUMMARY.md` documenting:
- The exact TRACKING_PARAMS list used in urlNormalization (D-16 verbatim).
- The exact RETAILERS arrays committed (D-28 verbatim — confirm match to spec).
- The TTL gcloud commands intended for plan 17-06 deploy (D-45):
  - `gcloud firestore fields ttls update cachedAt --collection-group=discoverCache --enable-ttl`
  - `gcloud firestore fields ttls update lastWriteAt --collection-group=discoverRateLimits --enable-ttl`
- A note that geminiClient.ts is NOT unit-tested per D-50 precedent (network-mocking deferred to e2e smoke).
- The composite index path and Firebase console deploy reminder (deploy via `firebase deploy --only firestore:indexes` in plan 17-06).
</output>
