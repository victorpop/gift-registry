# Phase 17 Search Re-Scope — "Discover Search v2" (intent → Google CSE)

> Captured 2026-05-28 mid-UAT. Supersedes the original Gemini-`google_search`-grounding
> search path (plans 17-02/17-03 `discoverSearch` + the search half of 17-05 UI).
> The **community-popular** path (17-02/03 `discoverPopular`, 17-04 triggers/backfill) is
> DONE and stays untouched. This doc is the planning input for the re-plan.

## Why we are pivoting

UAT-6 exposed a root-cause flaw in the shipped search: Gemini with `google_search`
grounding **hallucinates product titles onto real product IDs**. Concrete example
(query "cadou copil 2 ani"): Gemini returned a Montessori-toy title on eMAG URL
`/pd/DKT00YMBM/`, but that ID resolves to an Esprit T-shirt. The URL is real, the
product description is fiction. OG-as-source-of-truth (commit eaf4cb2) mitigated but
did not cure it — Gemini still fabricates which URLs are relevant.

**Decision:** stop letting Gemini name products. Gemini does intent + query generation
+ explanations only. Real products come from Google Custom Search JSON API. This
structurally eliminates hallucinated products and avoids scraping.

## Decisions locked (2026-05-28)

1. **Process:** formal re-plan as a Phase 17 re-scope (not inline).
2. **Scope:** search path only. "From the Community" popular section stays exactly as-is.
3. **UI shape:** FLAT product list (not nested category→reason→products). Gemini's
   category + reason are used to drive CSE queries and MAY surface as per-card metadata,
   but the screen renders one flat list of product cards (keeps current DiscoverScreen
   structure, swaps the backend).
4. **Google CSE setup:** handled as a human-action checkpoint during execution (same
   pattern as the Gemini key + ADC login in 17-06).

## Goal

Users search for gift ideas in natural language describing recipient / occasion /
interests / personality / budget. Examples:
- "Gift for my boyfriend who likes gaming under 300 lei"
- "Wedding gift for friends"
- "Gift for coffee lover"
- "Birthday gift for 8 year old girl"

App returns: gift categories, real product suggestions, links to retailer product
pages, and short explanations of why each gift matches.

## Architecture (required flow)

1. User enters a natural-language query.
2. Backend sends the query to Gemini API (no grounding).
3. Gemini extracts structured intent: recipient type, occasion, interests, budget,
   gift categories, and optimized product search queries.
4. Backend runs Google Custom Search JSON API queries (one per gift category /
   optimized query) against the supported-store list.
5. Backend normalizes CSE results (title, price if available, store, url, imageUrl
   from `pagemap`/`cse_image`).
6. Android renders a flat list of gift-suggestion cards.

## Product suggestion rules

The AI MUST NOT invent products. Gemini only generates: gift categories, optimized
search queries, and short explanations. Google CSE provides the actual products.
CSE queries should prioritize real product pages.

## Supported stores (CSE included sites)

eMAG, Altex, Media Galaxy, Flanco, CEL.ro, PC Garage, Vexio, Fashion Days,
About You Romania, Answear Romania, Modivo, Zalando Romania, EPantofi, Otter,
Notino Romania, Sephora Romania, Douglas Romania, Sabon Romania, Farmacia Tei,
Bebe Tei, IKEA Romania, JYSK Romania, Mobexpert, Bonami Romania, Vivre, Cărturești,
Libris, Elefant, Noriel, Hobby Shop, Decathlon Romania, SportGuru, Hervis Romania,
Intersport Romania, Mothercare Romania, Floria, Magnolia, Complice, Etsy, Breslo,
Kfea, Delicatese Florescu, Nespresso Romania.

## Desired API response shape (example)

```json
{
  "originalQuery": "gift for coffee lover under 200 lei",
  "interpretedIntent": {
    "recipient": "friend",
    "occasion": "birthday",
    "interests": ["coffee"],
    "budget": { "amount": 200, "currency": "RON" }
  },
  "giftIdeas": [
    {
      "category": "Manual Coffee Grinder",
      "reason": "Useful for someone interested in specialty coffee brewing.",
      "products": [
        {
          "title": "Timemore Chestnut C2 Grinder",
          "price": "179 RON",
          "store": "eMAG",
          "url": "https://…",
          "imageUrl": "https://…"
        }
      ]
    }
  ]
}
```

Note: backend response keeps the `giftIdeas[].products[]` structure (carries category +
reason), but the Android client FLATTENS it into one product list for display per
decision 3. Category/reason may be shown as card metadata.

## Technical constraints

- Easy to implement; scalable later; minimal complexity; minimal API cost; NO scraping.
- AI provider: Gemini API. Product search: Google Custom Search JSON API.
- Avoid: overengineering, microservices, complicated recommendation engines,
  scraping retailer sites, AI-hallucinated products.
- Focus: MVP simplicity, production realism, maintainability, low cost, good UX.

## Estimated API cost (recorded for planning)

- **Google CSE** (cost driver): 100 queries/day free; $5 per 1,000 beyond, cap 10k/day.
  ~3 CSE queries per user search (one per gift category) → free ≈ 30 searches/day;
  paid ≈ $0.015/search; ~1,000 searches/mo ≈ $15–20. `discoverCache` (30d TTL) makes
  repeat queries free.
- **Gemini 2.5 Flash** (intent only, no grounding): ~$0.0003/search — negligible, and
  cheaper than the current grounded implementation.

## What gets reused vs replaced

- Reuse: `discoverCache` (L2 cache + TTL), `rateLimit.ts`, the Callable + App Check
  wiring, `DiscoverScreen`/`DiscoverProductCard`/ViewModel scaffolding, `DiscoverProduct`
  domain model (now includes `retailerName`).
- Replace: `discoverSearch` internals (intent-extraction prompt → CSE fan-out →
  normalize), `geminiClient` (drop `google_search` tool, add JSON-mode intent call),
  `enrichImages.ts` (CSE returns images via pagemap — OG scrape no longer needed),
  `promptTemplate.ts` (now an intent-extraction prompt, not a product-listing prompt),
  `retailers.ts` (becomes the CSE site list / or moves to CSE config).
- New: Google CSE client + secrets (CSE_API_KEY, CSE_ENGINE_ID), CSE result normalizer.
