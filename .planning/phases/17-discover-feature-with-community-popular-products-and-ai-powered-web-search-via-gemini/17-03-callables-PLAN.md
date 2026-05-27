---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 03
type: execute
wave: 3
depends_on:
  - "17-02"
files_modified:
  - functions/src/discover/getPopular.ts
  - functions/src/discover/search.ts
  - functions/src/discover/rateLimit.ts
  - functions/src/__tests__/discover/rateLimit.test.ts
  - functions/src/index.ts
autonomous: true
requirements:
  - D-10
  - D-11
  - D-12
  - D-13
  - D-15
  - D-20
  - D-21
  - D-23
  - D-25
  - D-26
  - D-31
  - D-47
  - D-48

must_haves:
  truths:
    - "discoverPopular Callable returns up to 20 products from popularItems ordered by registryCount desc, updatedAt desc"
    - "discoverPopular caches results in an in-memory L1 with 1-hour TTL inside the Function instance"
    - "discoverPopular and discoverSearch both reject unauthenticated and anonymous-provider callers"
    - "discoverSearch enforces 20-calls/hour per uid via a Firestore transaction on discoverRateLimits/{uid}"
    - "discoverSearch checks discoverCache before calling Gemini; cache miss writes back results before returning"
    - "discoverSearch returns { products, cached_at } per D-31"
    - "TTL fields (discoverCache.cachedAt and discoverRateLimits.lastWriteAt) are written as Timestamp.fromDate(new Date(Date.now() + TTL_MS)) — the DEADLINE, not creation time — so Firestore TTL semantics (delete when field_value < now) preserve docs for the full window"
  artifacts:
    - path: "functions/src/discover/getPopular.ts"
      provides: "discoverPopular onCall Callable + module-scope L1 cache"
      contains: "export const discoverPopular"
    - path: "functions/src/discover/search.ts"
      provides: "discoverSearch onCall Callable orchestrating rate-limit + cache + Gemini"
      contains: "export const discoverSearch"
    - path: "functions/src/discover/rateLimit.ts"
      provides: "checkAndIncrementRateLimit(db, uid): Promise<void> — throws HttpsError when limit hit"
      contains: "export async function checkAndIncrementRateLimit"
    - path: "functions/src/__tests__/discover/rateLimit.test.ts"
      provides: "Rate-limit unit tests (under-limit allow, at-limit reject, expired cleanup)"
      contains: "describe(\"checkAndIncrementRateLimit\""
    - path: "functions/src/index.ts"
      provides: "Exports discoverPopular + discoverSearch"
      contains: "export { discoverPopular"
  key_links:
    - from: "functions/src/discover/getPopular.ts"
      to: "popularItems collection"
      via: "Firestore query orderBy('registryCount', 'desc').orderBy('updatedAt', 'desc').limit(20)"
      pattern: "registryCount.*desc"
    - from: "functions/src/discover/search.ts"
      to: "discoverCache collection"
      via: "doc(normalizeCacheKey(query)).get() before Gemini call"
      pattern: "discoverCache"
    - from: "functions/src/discover/search.ts"
      to: "GEMINI_API_KEY secret"
      via: "onCall(secrets: [GEMINI_API_KEY], ...) + GEMINI_API_KEY.value()"
      pattern: "secrets:.*GEMINI_API_KEY"
    - from: "functions/src/discover/search.ts"
      to: "checkAndIncrementRateLimit"
      via: "Firestore transaction on discoverRateLimits/{uid}"
      pattern: "discoverRateLimits"
    - from: "functions/src/discover/search.ts"
      to: "discoverCache TTL deadline"
      via: "Timestamp.fromDate(new Date(Date.now() + CACHE_TTL_MS)) where CACHE_TTL_MS = 30 days"
      pattern: "Timestamp.fromDate\\(new Date\\(Date.now\\(\\) \\+ CACHE_TTL_MS"
    - from: "functions/src/discover/rateLimit.ts"
      to: "discoverRateLimits TTL deadline"
      via: "Timestamp.fromDate(new Date(Date.now() + RATE_LIMIT_TTL_MS)) where RATE_LIMIT_TTL_MS = 7 days"
      pattern: "Timestamp.fromDate\\(new Date\\(Date.now\\(\\) \\+ RATE_LIMIT_TTL_MS"
---

<objective>
Implement the two Callable Cloud Functions that the Android Discover surface (plan 17-05) will invoke: `discoverPopular` (community popularity) and `discoverSearch` (Gemini search). Wire rate-limiting (`rateLimit.ts`), cache-read/write, Gemini orchestration, and the L1 in-memory cache. Register both as exports in `functions/src/index.ts`.

Purpose: Per CONTEXT.md D-10, D-11, D-12, D-13, D-15, D-20, D-21, D-23, D-25, D-26, D-31, D-47. Triggers (plan 17-04) and the Android client (plan 17-05) consume these Callables.

Output: Three new TypeScript source files in `functions/src/discover/`, one new test file, and an updated `functions/src/index.ts` with two new named exports.
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
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-02-backend-foundations-PLAN.md
@CLAUDE.md
</context>

<interfaces>
<!-- Modules created in plan 17-02 — consumed verbatim here. -->

From functions/src/discover/urlNormalization.ts:
```typescript
export interface NormalizedUrl { productId: string; canonicalUrl: string }
export function normalizeUrl(url: string): NormalizedUrl;
```

From functions/src/discover/retailers.ts:
```typescript
export function selectSitesForQuery(query: string): string[];
```

From functions/src/discover/promptTemplate.ts:
```typescript
export interface BuiltPrompt { systemPrompt: string; userPrompt: string }
export function buildPrompt(query: string, sites: string[]): BuiltPrompt;
```

From functions/src/discover/parseGeminiResponse.ts:
```typescript
export interface DiscoverProduct {
  title: string; description: string; image_url: string; price: number;
  currency: string; retailer_url: string; retailer_name: string;
}
export function parseGeminiResponse(raw: string, query?: string): DiscoverProduct[];
```

From functions/src/discover/cacheKey.ts:
```typescript
export function normalizeCacheKey(query: string): string;
```

From functions/src/discover/secrets.ts:
```typescript
export const GEMINI_API_KEY: SecretParam;  // defineSecret("GEMINI_API_KEY")
```

From functions/src/discover/geminiClient.ts:
```typescript
export async function callGemini(prompt: BuiltPrompt, apiKey: string): Promise<string>;
```

From functions/src/registry/fetchOgMetadata.ts (existing Callable pattern):
```typescript
export const fetchOgMetadata = onCall(
  { region: "europe-west3" },
  async (request): Promise<OgMetadataResponse> => fetchOgMetadataHandler(request)
);
```
</interfaces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: rateLimit.ts + unit tests (transactional 20/hr per uid)</name>

  <behavior>
    - First call for a new uid: writes timestamps=[now], lastWriteAt = Timestamp.fromDate(new Date(now + 7 days)) (TTL deadline, NOT creation time), succeeds
    - Under-limit call (e.g. 5 prior timestamps within last hour): appends now, refreshes lastWriteAt deadline, succeeds
    - At-limit call (20 prior timestamps within last hour): throws HttpsError("resource-exhausted", "Rate limit exceeded"); does NOT append
    - Expired timestamps (> 3600000 ms old): filtered out before counting; new call after cleanup succeeds even if pre-filter count was at 20
    - Atomicity: read + filter + check + write happen in a single Firestore transaction to prevent races between concurrent calls
    - D-45 TTL semantics: `lastWriteAt` is written as the DEADLINE (now + 7 days), not as serverTimestamp() — Firestore TTL deletes docs when field_value < now, so writing now would mark the doc eligible immediately
  </behavior>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-13 verbatim — 20/1 hr, timestamps array, atomic transaction; D-14 + D-45 — 7-day TTL on lastWriteAt)
    - Firestore TTL semantics reference: https://firebase.google.com/docs/firestore/ttl — "Documents whose [TTL field] value is in the past are considered expired and eligible for deletion." THIS MEANS the writer must store `now + TTL_DURATION`, NOT serverTimestamp().
    - functions/src/reservation/createReservation.ts (existing Firestore transaction pattern — db.runTransaction((tx) => …))
    - functions/src/__tests__/discover/urlNormalization.test.ts (existing test file pattern for the new functions/__tests__/discover folder)
    - functions/src/__tests__/createReservation.test.ts (existing transaction test pattern — uses firebase-functions-test + jest.mock for Firestore)
  </read_first>

  <files>
    functions/src/discover/rateLimit.ts,
    functions/src/__tests__/discover/rateLimit.test.ts
  </files>

  <action>
    Create the rate-limiter helper and its unit test. The helper takes a Firestore instance + uid and either succeeds (writing the updated timestamps array) or throws `HttpsError("resource-exhausted", …)`.

    **1. `functions/src/discover/rateLimit.ts`**:
    ```typescript
    import { HttpsError } from "firebase-functions/v2/https";
    import type { Firestore } from "firebase-admin/firestore";
    import { Timestamp } from "firebase-admin/firestore";

    const WINDOW_MS = 60 * 60 * 1000;                   // 1 hour rolling window
    const MAX_CALLS = 20;                               // D-13 verbatim limit
    const RATE_LIMIT_TTL_MS = 7 * 24 * 60 * 60 * 1000;  // D-14: 7-day TTL on abandoned counters

    /**
     * Phase 17 D-13: per-uid rolling-window rate limit for discoverSearch.
     *
     * Stored at discoverRateLimits/{uid} as:
     *   { timestamps: number[], lastWriteAt: Timestamp }
     *
     * `timestamps` holds epoch-ms call markers from within the last hour.
     * `lastWriteAt` is the Firestore TTL DEADLINE — stored as `now + 7 days`
     * because Firestore TTL semantics delete a doc when the field's value is
     * less than current time. Writing `serverTimestamp()` would make the doc
     * eligible for deletion immediately (next TTL sweep, ~24h). We must store
     * the deadline so the doc survives until 7 days after the last write.
     *
     * Implementation note: read + filter + check + write happen inside
     * `db.runTransaction` to prevent two concurrent calls from both passing
     * the check at length=19 and bringing the array to 21.
     */
    export async function checkAndIncrementRateLimit(db: Firestore, uid: string): Promise<void> {
      const ref = db.collection("discoverRateLimits").doc(uid);
      await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const now = Date.now();
        const cutoff = now - WINDOW_MS;

        const prior = snap.exists ? (snap.data()?.timestamps as unknown) : null;
        const priorList: number[] = Array.isArray(prior)
          ? (prior.filter((t) => typeof t === "number" && t >= cutoff) as number[])
          : [];

        if (priorList.length >= MAX_CALLS) {
          throw new HttpsError("resource-exhausted", "Rate limit exceeded");
        }

        const nextList = [...priorList, now];
        // D-14 + D-45 TTL semantics: lastWriteAt must be the DEADLINE, not now.
        // Firestore TTL deletes when field_value < current_time, so we store
        // `now + 7 days` to ensure the counter survives the full 7-day window.
        tx.set(ref, {
          timestamps: nextList,
          lastWriteAt: Timestamp.fromDate(new Date(Date.now() + RATE_LIMIT_TTL_MS)),
        });
      });
    }
    ```

    **2. `functions/src/__tests__/discover/rateLimit.test.ts`** — use a Firestore mock or firebase-functions-test pattern. Mirror the existing `createReservation.test.ts` mocking approach. Key cases (D-48 verbatim):
    ```typescript
    import { checkAndIncrementRateLimit } from "../../discover/rateLimit";
    import { HttpsError } from "firebase-functions/v2/https";

    // Build a fake Firestore that supports runTransaction with in-memory state.
    function makeFakeDb(initial?: { timestamps?: number[] }) {
      let stored: { timestamps?: number[]; lastWriteAt?: unknown } | null =
        initial ? { ...initial } : null;
      const ref = {
        // tx.get(ref) returns snap with exists + data()
        _path: "discoverRateLimits/u1",
      };
      const db = {
        collection: (_: string) => ({ doc: (__: string) => ref }),
        runTransaction: async (cb: (tx: any) => Promise<void>) => {
          const tx = {
            get: async (_: any) => ({
              exists: stored !== null,
              data: () => stored ?? undefined,
            }),
            set: (_: any, data: any) => { stored = data; },
          };
          await cb(tx);
        },
      };
      return { db: db as any, getStored: () => stored };
    }

    describe("checkAndIncrementRateLimit", () => {
      it("allows the first call for a new uid", async () => {
        const { db, getStored } = makeFakeDb();
        await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
        expect(getStored()?.timestamps).toHaveLength(1);
      });

      it("allows a call when 5 prior timestamps are within the window", async () => {
        const now = Date.now();
        const { db, getStored } = makeFakeDb({
          timestamps: [now - 1000, now - 2000, now - 3000, now - 4000, now - 5000],
        });
        await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
        expect(getStored()?.timestamps).toHaveLength(6);
      });

      it("rejects the call at exactly 20 prior timestamps", async () => {
        const now = Date.now();
        const ts = Array.from({ length: 20 }, (_, i) => now - i * 1000);
        const { db, getStored } = makeFakeDb({ timestamps: ts });
        await expect(checkAndIncrementRateLimit(db, "u1")).rejects.toBeInstanceOf(HttpsError);
        // Stored array unchanged (transaction did not write)
        expect(getStored()?.timestamps).toHaveLength(20);
      });

      it("filters out timestamps older than 1 hour before counting", async () => {
        const now = Date.now();
        // 20 stale entries (older than 1 hour) — should all be filtered out
        const staleTs = Array.from({ length: 20 }, (_, i) => now - 3700000 - i * 1000);
        const { db, getStored } = makeFakeDb({ timestamps: staleTs });
        await expect(checkAndIncrementRateLimit(db, "u1")).resolves.toBeUndefined();
        // After cleanup, only the new timestamp remains
        expect(getStored()?.timestamps).toHaveLength(1);
      });

      it("throws HttpsError with code 'resource-exhausted'", async () => {
        const now = Date.now();
        const ts = Array.from({ length: 20 }, (_, i) => now - i * 1000);
        const { db } = makeFakeDb({ timestamps: ts });
        try {
          await checkAndIncrementRateLimit(db, "u1");
          fail("Expected HttpsError");
        } catch (err) {
          expect(err).toBeInstanceOf(HttpsError);
          expect((err as HttpsError).code).toBe("resource-exhausted");
        }
      });
    });
    ```
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "export async function checkAndIncrementRateLimit" functions/src/discover/rateLimit.ts
      grep -q "resource-exhausted" functions/src/discover/rateLimit.ts
      grep -q "runTransaction" functions/src/discover/rateLimit.ts
      grep -q "60 \\* 60 \\* 1000\\|3600000" functions/src/discover/rateLimit.ts
      # D-45 TTL semantics: lastWriteAt must be the deadline (now + 7d), not serverTimestamp()
      grep -q "Timestamp.fromDate(new Date(Date.now()" functions/src/discover/rateLimit.ts
      grep -q "RATE_LIMIT_TTL_MS\\|7 \\* 24 \\* 60 \\* 60 \\* 1000" functions/src/discover/rateLimit.ts
      ! grep -q "lastWriteAt: FieldValue.serverTimestamp" functions/src/discover/rateLimit.ts
      cd functions && npm test -- --testPathPattern=discover/rateLimit --silent 2>&1 | tail -20
      npm run build 2>&1 | tail -5
      echo OK
      '
    </automated>
  </verify>

  <done>
    `rateLimit.ts` exports `checkAndIncrementRateLimit(db, uid)` using `db.runTransaction`. Window is 60 * 60 * 1000 ms; limit is 20. `lastWriteAt` is written as `Timestamp.fromDate(new Date(Date.now() + RATE_LIMIT_TTL_MS))` (the 7-day TTL deadline per D-45 semantics) — NOT `FieldValue.serverTimestamp()`. All 5 unit test cases pass. `npm run build` succeeds.
  </done>
</task>

<task type="auto">
  <name>Task 2: discoverPopular Callable (with L1 in-memory cache) + discoverSearch Callable (rate-limit + cache + Gemini orchestration) + index.ts exports</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-10, D-11, D-12, D-15, D-20, D-21, D-23, D-25, D-26, D-31, D-47 verbatim; D-45 — 30-day TTL on discoverCache.cachedAt)
    - Firestore TTL semantics reference: https://firebase.google.com/docs/firestore/ttl — TTL deletes when field_value < now, so cachedAt must be the DEADLINE (now + 30 days), not creation time.
    - functions/src/discover/rateLimit.ts (Task 1 — consumed by search.ts; pattern: Timestamp.fromDate(new Date(Date.now() + RATE_LIMIT_TTL_MS)) for lastWriteAt deadline)
    - functions/src/discover/secrets.ts (plan 17-02 — GEMINI_API_KEY)
    - functions/src/discover/cacheKey.ts (plan 17-02)
    - functions/src/discover/parseGeminiResponse.ts (plan 17-02)
    - functions/src/discover/promptTemplate.ts (plan 17-02)
    - functions/src/discover/retailers.ts (plan 17-02)
    - functions/src/discover/geminiClient.ts (plan 17-02)
    - functions/src/registry/fetchOgMetadata.ts (existing Callable pattern — onCall + region + handler split for testability)
    - functions/src/reservation/hydrateActiveReservation.ts (Callable that reads Firestore + returns shaped data — structural twin of discoverPopular per CONTEXT.md code_context note)
    - functions/src/index.ts (current — list of named exports to append to)
  </read_first>

  <files>
    functions/src/discover/getPopular.ts,
    functions/src/discover/search.ts,
    functions/src/index.ts
  </files>

  <action>
    **1. `functions/src/discover/getPopular.ts`** — implements D-12, D-15, D-20, D-21:
    ```typescript
    import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
    import * as admin from "firebase-admin";

    interface PopularProduct {
      id: string;
      title: string;
      description: string;
      image_url: string;
      price: number;
      currency: string;
      retailer_url: string;
    }

    interface PopularResponse {
      products: PopularProduct[];
    }

    // D-21: module-scope L1 cache, lives for the lifetime of the Function instance.
    // 1 hour TTL.
    interface CacheEntry { data: PopularResponse; expiresAt: number }
    let l1Cache: CacheEntry | null = null;
    const L1_TTL_MS = 60 * 60 * 1000;
    const TOP_N = 20;

    /**
     * Phase 17 D-20: query popularItems ordered by (registryCount desc, updatedAt desc),
     * limit 20, map to spec response shape.
     */
    async function loadFromFirestore(): Promise<PopularResponse> {
      const db = admin.firestore();
      const snap = await db
        .collection("popularItems")
        .orderBy("registryCount", "desc")
        .orderBy("updatedAt", "desc")
        .limit(TOP_N)
        .get();

      const products: PopularProduct[] = snap.docs.map((d) => {
        const data = d.data() as Record<string, unknown>;
        const priceRaw = data.price;
        let price = 0;
        if (typeof priceRaw === "number") price = priceRaw;
        else if (typeof priceRaw === "string") {
          const n = parseFloat(priceRaw);
          if (!isNaN(n)) price = n;
        }
        return {
          id: d.id,
          title: typeof data.title === "string" ? data.title : "",
          description: "",
          image_url: typeof data.imageUrl === "string" ? data.imageUrl : "",
          price,
          currency: "RON",
          retailer_url: typeof data.canonicalUrl === "string" ? data.canonicalUrl : "",
        };
      });
      return { products };
    }

    export async function discoverPopularHandler(
      request: CallableRequest<unknown>
    ): Promise<PopularResponse> {
      // D-12: auth gate
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "Sign in required");
      }
      if (request.auth.token.firebase?.sign_in_provider === "anonymous") {
        throw new HttpsError("permission-denied", "Registered account required");
      }

      // D-21: L1 cache
      const now = Date.now();
      if (l1Cache && now < l1Cache.expiresAt) {
        return l1Cache.data;
      }
      const fresh = await loadFromFirestore();
      l1Cache = { data: fresh, expiresAt: now + L1_TTL_MS };
      return fresh;
    }

    export const discoverPopular = onCall(
      { region: "europe-west3", enforceAppCheck: true },
      discoverPopularHandler
    );
    ```

    Note on `enforceAppCheck`: Phase 16 D-XX precedent established App Check enforcement on new Callables. Confirm the pattern in `acceptInvite.ts` (Phase 16 reference) before committing. If unsure, omit `enforceAppCheck` — but plan 17-06 deploy task explicitly enables it.

    **2. `functions/src/discover/search.ts`** — implements D-12, D-13, D-23, D-25, D-26, D-31. Orchestrates rate-limit → cache check → Gemini call → cache write:
    ```typescript
    import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
    import * as admin from "firebase-admin";
    import { Timestamp } from "firebase-admin/firestore";
    import { GEMINI_API_KEY } from "./secrets";
    import { normalizeCacheKey } from "./cacheKey";
    import { selectSitesForQuery } from "./retailers";
    import { buildPrompt } from "./promptTemplate";
    import { callGemini } from "./geminiClient";
    import { parseGeminiResponse, DiscoverProduct } from "./parseGeminiResponse";
    import { checkAndIncrementRateLimit } from "./rateLimit";

    // D-45 TTL semantics: cachedAt is stored as the DEADLINE (now + 30 days), NOT creation time.
    // Firestore TTL deletes a doc when field_value < current_time; storing `now`
    // would mark the doc eligible for deletion immediately on the next TTL sweep.
    const CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000;  // 30 days

    interface SearchRequest { query?: unknown }
    interface SearchResponse {
      products: DiscoverProduct[];
      cached_at: string;
    }

    export async function discoverSearchHandler(
      request: CallableRequest<SearchRequest>
    ): Promise<SearchResponse> {
      // D-12: auth gate
      if (!request.auth) {
        throw new HttpsError("unauthenticated", "Sign in required");
      }
      if (request.auth.token.firebase?.sign_in_provider === "anonymous") {
        throw new HttpsError("permission-denied", "Registered account required");
      }

      // D-23: query validation
      const raw = request.data?.query;
      if (typeof raw !== "string" || raw.trim().length === 0) {
        throw new HttpsError("invalid-argument", "Query required");
      }
      const query = raw;
      if (query.length > 200) {
        throw new HttpsError("invalid-argument", "Query too long (max 200 chars)");
      }

      const db = admin.firestore();

      // D-13: rate limit (throws resource-exhausted on cap)
      await checkAndIncrementRateLimit(db, request.auth.uid);

      // D-24, D-25, D-26: cache lookup
      const cacheKey = normalizeCacheKey(query);
      const cacheRef = db.collection("discoverCache").doc(cacheKey);
      const cacheSnap = await cacheRef.get();
      if (cacheSnap.exists) {
        const data = cacheSnap.data()!;
        // D-31 client-facing cached_at = creation time. Since cachedAt is stored
        // as the DEADLINE (now + 30 d) per D-45 TTL semantics, recover the
        // creation time by subtracting CACHE_TTL_MS from the deadline.
        const cachedAtDeadline = data.cachedAt as Timestamp | undefined;
        const cachedAtCreation = cachedAtDeadline
          ? new Date(cachedAtDeadline.toDate().getTime() - CACHE_TTL_MS)
          : new Date();
        return {
          products: (data.results as DiscoverProduct[]) ?? [],
          cached_at: cachedAtCreation.toISOString(),
        };
      }

      // D-26, D-27, D-28, D-29: cache miss → Gemini
      const sites = selectSitesForQuery(query);
      const prompt = buildPrompt(query, sites);
      const apiKey = GEMINI_API_KEY.value();
      let rawResponse: string;
      try {
        rawResponse = await callGemini(prompt, apiKey);
      } catch (err) {
        console.error("[discoverSearch] Gemini call failed", err);
        // D-31 + Claude discretion ("do not cache failures") — return empty, no cache write
        return { products: [], cached_at: new Date().toISOString() };
      }

      const products = parseGeminiResponse(rawResponse, query);

      // Per Claude's Discretion ("only cache successful non-empty results") — skip empty results write
      const now = new Date();
      if (products.length > 0) {
        // D-45 TTL semantics: cachedAt must be the DEADLINE (now + 30 days), not creation time.
        // Firestore TTL deletes when field_value < current_time — writing
        // serverTimestamp() would make every cache doc eligible immediately.
        await cacheRef.set({
          query,
          normalizedQuery: cacheKey,
          results: products,
          cachedAt: Timestamp.fromDate(new Date(Date.now() + CACHE_TTL_MS)),
        });
      }

      return { products, cached_at: now.toISOString() };
    }

    export const discoverSearch = onCall(
      {
        region: "europe-west3",
        enforceAppCheck: true,
        secrets: [GEMINI_API_KEY],
      },
      discoverSearchHandler
    );
    ```

    **3. `functions/src/index.ts`** — add two new named exports. Append after the existing `export { declineInvite } from "./registry/declineInvite";` line:
    ```typescript
    export { discoverPopular } from "./discover/getPopular";
    export { discoverSearch } from "./discover/search";
    ```
    Do not touch any other line. The dev-only emulator block at the bottom stays untouched.
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "export const discoverPopular" functions/src/discover/getPopular.ts
      grep -q "export const discoverSearch" functions/src/discover/search.ts
      grep -q "orderBy(\"registryCount\", \"desc\")" functions/src/discover/getPopular.ts
      grep -q "orderBy(\"updatedAt\", \"desc\")" functions/src/discover/getPopular.ts
      grep -q "limit(20)\|limit(TOP_N)" functions/src/discover/getPopular.ts
      grep -q "sign_in_provider" functions/src/discover/getPopular.ts
      grep -q "sign_in_provider" functions/src/discover/search.ts
      grep -q "Query too long (max 200 chars)" functions/src/discover/search.ts
      grep -q "checkAndIncrementRateLimit" functions/src/discover/search.ts
      grep -q "secrets:.*GEMINI_API_KEY" functions/src/discover/search.ts
      grep -q "discoverCache" functions/src/discover/search.ts
      # D-45 TTL semantics: cachedAt must be the deadline (now + 30d), not serverTimestamp()
      grep -q "Timestamp.fromDate(new Date(Date.now()" functions/src/discover/search.ts
      grep -q "CACHE_TTL_MS\\|30 \\* 24 \\* 60 \\* 60 \\* 1000" functions/src/discover/search.ts
      ! grep -q "cachedAt: FieldValue.serverTimestamp" functions/src/discover/search.ts
      grep -q "europe-west3" functions/src/discover/getPopular.ts
      grep -q "europe-west3" functions/src/discover/search.ts
      grep -q "export { discoverPopular } from \"./discover/getPopular\"" functions/src/index.ts
      grep -q "export { discoverSearch } from \"./discover/search\"" functions/src/index.ts
      cd functions && npm run build 2>&1 | tail -10
      npm test -- --testPathPattern=discover --silent 2>&1 | tail -10
      echo OK
      '
    </automated>
  </verify>

  <done>
    `discoverPopular` Callable queries popularItems ordered by (registryCount desc, updatedAt desc) with limit(20), uses module-scope L1 cache with 1-hour TTL, rejects unauthenticated + anonymous. `discoverSearch` Callable validates query (1–200 chars), enforces rate limit via transactional checkAndIncrementRateLimit, reads discoverCache before Gemini, calls Gemini with `[GEMINI_API_KEY]` secret, defensively parses response, writes back to cache on non-empty result. `cachedAt` is written as `Timestamp.fromDate(new Date(Date.now() + CACHE_TTL_MS))` (the 30-day TTL deadline per D-45 semantics) — NOT `FieldValue.serverTimestamp()`. Both exported from `functions/src/index.ts`. `npm run build` clean, all discover tests pass.
  </done>
</task>

</tasks>

<verification>
1. `cd functions && npm run build` succeeds.
2. `cd functions && npm test -- --testPathPattern=discover` runs all discover tests (rateLimit + the 5 from plan 17-02) and all pass.
3. `grep -E "discoverPopular|discoverSearch" functions/src/index.ts` shows two new exports.
4. `firebase deploy --only functions --dry-run` (or `firebase functions:list` in plan 17-06 after real deploy) will recognize both Callables.
</verification>

<success_criteria>
- `functions/src/discover/getPopular.ts` exports `discoverPopular` onCall + `discoverPopularHandler` (testable inner function).
- `functions/src/discover/search.ts` exports `discoverSearch` onCall + `discoverSearchHandler`.
- `functions/src/discover/rateLimit.ts` exports `checkAndIncrementRateLimit` with full unit-test coverage (5 cases).
- Both Callables use `region: "europe-west3"`, reject unauthenticated + anonymous auth, and (per Phase 16 precedent) enforce App Check.
- `discoverSearch` integrates: validation (D-23) → rate-limit (D-13) → cache lookup (D-25, D-26) → Gemini call (D-27, D-28, D-29) → defensive parse (D-30) → cache write (only on non-empty) → response shape (D-31).
- `discoverPopular` L1 cache: 1-hour TTL module-scope variable (D-21).
- Both Callables registered as named exports in `functions/src/index.ts`.
- `npm run build` succeeds; all discover tests pass.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-03-SUMMARY.md` documenting:
- Whether `enforceAppCheck: true` was retained on both Callables (Phase 16 precedent) or omitted pending plan 17-06 confirmation.
- The exact rate-limit constants used (WINDOW_MS=3600000, MAX_CALLS=20) — confirm matches D-13.
- A note that plan 17-06 must set the GEMINI_API_KEY secret via `firebase functions:secrets:set GEMINI_API_KEY` before deploy (or via `gcloud secrets create`).
- Any deviation from D-31 response shape (e.g., if `cached_at` lossy-conversion edge case was hit).
</output>
