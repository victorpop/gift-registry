# Phase 17: Discover Feature - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-27
**Phase:** 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
**Areas discussed:** Backend transport + access policy, Popular-item identity + aggregation, Affiliate URL on Discover taps, Stores screen fate
**Mode:** discuss (interactive, single-question-per-turn)

---

## Pre-Discussion Context

User invoked `/gsd:plan-phase 17 <huge PRD spec>` after adding Phase 17 to roadmap. Orchestrator detected conflicts between spec and project-locked constraints, recommended discuss-phase first. User agreed.

### Carry-forward decisions surfaced (not voted on — pre-locked by project)

| Spec asked | Project locks | Resolution |
|---|---|---|
| "Backend: pick the best solution" | Firebase only (CLAUDE.md) | Cloud Functions 2nd gen, Node 22, TypeScript, europe-west3 |
| "discover_cache table (jsonb)" | No SQL/Postgres allowed | Firestore `discoverCache` collection |
| "Cache popular in memory 1hr" | Functions stateless / cold-start loses memory | In-memory L1 per-instance, no L2 |
| "Replace Stores nav entry" | `GiftMaisonBottomNav.kt` slot 2 = Stores | Rewire slot 2 with Discover icon + label |
| "Add to Retrofit service interface" | No Retrofit in project | Translate to Firebase Callable Functions |
| Spec item fields (description, currency) | Item schema lacks description + currency | Description = "" returned; currency = "RON" hardcoded |

### Todo cross-reference

5 todos matched weakly (generic keyword overlap on `phase`/`web`/`plan`). None semantically related to Discover. **Recommended skip** — user did not contest, all deferred unchanged.

---

## Area 1: Backend Transport + Access Policy

### Q1.1: Backend transport pattern

| Option | Description | Selected |
|---|---|---|
| Firebase Callable Functions | Translate spec's GET/POST to two Callables: `discoverPopular()` and `discoverSearch({query})`. Matches every other Function in the project. Auth context (uid, token) free; rate-limit keyed on `context.auth.uid`. No Retrofit, no JWT middleware. Saves ~200 LOC. | ✓ |
| REST onRequest endpoints | Two `onRequest` handlers at `/discover/popular` and `/discover/search`. Forces adding: Retrofit + Moshi/kotlinx-serialization converter, OkHttp interceptor for Firebase ID token, JWT verification middleware in Functions, CORS config. ~200 LOC of plumbing, no behavioral benefit. | |
| Mixed — Callable for Android, REST for future web | Ship Callable now (Android-only consumer); add REST wrappers only if web fallback ever needs Discover. Defers complexity. | |

**User's choice:** Firebase Callable Functions (Recommended)
**Notes:** Locked. Drives D-10 in CONTEXT.md.

### Q1.2: Who can call the Discover Callables?

| Option | Description | Selected |
|---|---|---|
| Signed-in users only — reject anonymous | Callable rejects if `context.auth` is null OR `context.auth.token.firebase.sign_in_provider == 'anonymous'`. Rate-limit keyed on `context.auth.uid`. Discover is registered-user only. | ✓ |
| All signed-in including anonymous | Allow `context.auth.uid` regardless of provider. Slight abuse risk (anon accounts free to mint), but rate-limit still caps. | |
| Open to all callers, IP rate-limit | No auth check. Rate-limit by IP. Enables Gemini API budget abuse. | |

**User's choice:** Signed-in users only — reject anonymous (Recommended)
**Notes:** Anonymous Auth (web giver path, Phase 15 territory) explicitly excluded from Discover. Drives D-12 in CONTEXT.md.

### Q1.3: Rate-limit counter storage

| Option | Description | Selected |
|---|---|---|
| Firestore doc per user with rolling window | `discoverRateLimits/{uid}` doc holds array of recent call timestamps (last N). Each call: filter to last 1hr, reject if count >= 20, else append + write. Atomic via transaction. Firestore TTL auto-cleans abandoned docs at 7 days. | ✓ |
| In-memory counter (per Function instance) | `Map<uid, timestamps[]>` in module scope. Faster but unreliable: each instance has its own copy. Resets on cold start. Easy to bypass. | |
| Firebase Realtime DB atomic counter | RTDB transactions atomic and fast. But project doesn't use RTDB anywhere else. Violates "Firestore only." | |

**User's choice:** Firestore doc per user with rolling window (Recommended)
**Notes:** Drives D-13 + D-14 in CONTEXT.md.

---

## Area 2: Popular-Item Identity + Aggregation

### Q2.1: Product identity rule

| Option | Description | Selected |
|---|---|---|
| Normalized originalUrl | Strip query params (utm_*, fbclid, EMAG aff suffix), lowercase host, strip trailing slash, drop fragment. SHA-256 the result → productId. Robust to mild URL variation, deterministic. EMAG canonical paths work well. | ✓ |
| Exact originalUrl match | No normalization. Misses obvious dupes (different utm params). Simplest but lower-quality rankings. | |
| Title-similarity clustering | Lowercase + Levenshtein/Jaccard. Catches manual re-entries but expensive at query time, fuzzy clusters. Unsuitable at scale. | |

**User's choice:** Normalized originalUrl (Recommended)
**Notes:** Normalization order specified in D-16 in CONTEXT.md.

### Q2.2: Popularity computation strategy

| Option | Description | Selected |
|---|---|---|
| Firestore trigger maintains popularItems counter | `onItemCreate`/`onItemDelete` triggers update `popularItems/{productId}` doc with `registryIds: Set<string>`, `registryCount: N`. `discoverPopular()` reads top-20 ordered by `registryCount desc`, in-memory cache 1hr. O(1) read; writes only on item create/delete. Realtime-accurate. | ✓ |
| On-demand collection-group scan | Each cache miss runs `collectionGroup('items').get()` across all registries, computes ranking in-Function. Expensive on miss, doesn't scale. | |
| Scheduled rebuild (daily Cloud Scheduler) | Nightly full rebuild. Cheaper than triggers but staleness up to 24hr. Acceptable for all-time popularity but slower to surface new trends. | |

**User's choice:** Firestore trigger maintains popularItems counter (Recommended)
**Notes:** Drives D-18 through D-21 in CONTEXT.md.

### Q2.3: Backfill of existing items

| Option | Description | Selected |
|---|---|---|
| One-shot backfill script | `functions/scripts/backfillPopularItems.ts`: scan all existing items via collection group, normalize URLs, batch-write `popularItems`. Run once via CLI after deploying triggers. Same pattern as Phase 7 `seedStores.ts`. | ✓ |
| Self-heal on first call | `discoverPopular()` detects empty collection, kicks off internal scan + write before returning. Slow first call (could timeout), brittle. | |
| No backfill — start fresh | Discover popular empty until enough new items accumulate. Hurts launch UX. | |

**User's choice:** One-shot backfill script (Recommended)
**Notes:** Drives D-22 in CONTEXT.md. Deferred section notes orchestration safety (run backfill BEFORE deploying triggers to avoid double-count).

### Q2.4: More questions about Area 2?

| Option | Description | Selected |
|---|---|---|
| Next area | Have what's needed: identity + counter + backfill. Counter doc field shape is Claude's discretion. | ✓ |
| More questions | Tiebreaker rules, Gemini result overlap with popular, etc. | |

**User's choice:** Next area (Recommended)

---

## Area 3: Affiliate URL on Discover Taps

### Q3.1: How is retailer URL opened from a Discover card?

| Option | Description | Selected |
|---|---|---|
| Transform on backend, return affiliateUrl | Both Callables run results through TS port of `AffiliateUrlTransformer` before returning. Response adds `affiliateUrl`. Android opens the affiliate URL. Preserves revenue. | |
| Transform on Android before launching Intent | Reuse existing Kotlin `AffiliateUrlTransformer` in `DiscoverRepository` before `startActivity`. For popular items already have `affiliateUrl` in Firestore; for Gemini new URLs, Android-side transform. Mixed model. | |
| Open raw URL per spec (lose affiliate revenue) | Strictly honor spec wording. No transformation. Discover taps generate zero affiliate income. | ✓ |

**User's choice:** Open raw URL per spec (lose affiliate revenue)
**Notes:** Unusual choice given the project's revenue model is affiliate-only. User was shown the trade-off explicitly ("lose affiliate revenue") and confirmed. Drives D-32 in CONTEXT.md. Deferred section logs a future revisit path (~80 LOC, 1 plan) if business changes its mind.

---

## Area 4: Stores Screen Fate

### Q4.1: What happens to Phase 7's Stores capability beyond the nav slot?

| Option | Description | Selected |
|---|---|---|
| Decommission everything | Delete StoreListScreen, StoreBrowserScreen, ViewModels, repos, modules, nav keys, `config/stores` doc, drawables, strings, FAB sheet "Browse stores" row, seed script. Clean removal. | ✓ |
| Keep Stores via FAB sheet only | Remove nav slot but keep "Browse stores" row in Add-action sheet (Phase 9 D-09). Stores reachable from FAB. Dual discovery surfaces with overlapping intent — UX confusion risk. | |
| Leave as dead code (cleanup later) | Keep all Phase 7 code, only remove nav wiring. Tech debt. | |

**User's choice:** Decommission everything (Recommended)
**Notes:** Drives D-03 through D-09 in CONTEXT.md. Combined with user's "no add to my registry from Discover" out-of-scope rule, this is a net product capability reduction — owners who used Stores → tap product → Add to list lose that flow. User aware (it was in their original spec under "out of scope").

---

## Wrap-Up

| Option | Description | Selected |
|---|---|---|
| I'm ready for context | Write 17-CONTEXT.md + 17-DISCUSSION-LOG.md, then re-run `/gsd:plan-phase 17`. | ✓ |
| Explore more gray areas | Gemini hallucinated-URL validation, image hotlink fallback, empty popular state, FCM interaction, etc. | |

**User's choice:** I'm ready for context (Recommended)
**Notes:** Items listed in "Explore more" option captured in CONTEXT.md `<deferred>` section as future revisits.

---

## Claude's Discretion (Carried into Plan)

- Exact shimmer/skeleton component (Material3 placeholder modifier vs custom)
- `R.drawable.discover_card_placeholder` exact visual
- Snackbar timing/positioning for "no browser" error
- Denormalized field shape in `popularItems` doc beyond required
- Firestore composite index declarations
- Occasion-keyword matching (simple string contains, no regex)
- Whether to cache failed/empty Gemini responses (no — only successful non-empty)
- Locale handling for Gemini prompt (pass query verbatim, Gemini multilingual-native)

## Deferred Ideas Captured

- Analytics events
- "Add to my registry" from Discover cards
- Pagination / infinite scroll
- Per-user personalization
- Time-windowed popularity (last 7d, last month)
- Price-refresh on cached search results
- Affiliate URL transformation on Discover taps (logged with revisit path)
- Web fallback variant of Discover
- Hallucinated-URL HEAD-check validation
- Image hotlink-protection server-side proxying
- Cloud Logging alert on Gemini 429 / project-quota exhaustion
- Phase 7 Stores rollback (no path planned)
