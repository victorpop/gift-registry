/**
 * Phase 17 D-29: Gemini prompt template — verbatim from CONTEXT.md.
 *
 * No paraphrasing — schema and instructions are part of the contract with
 * `parseGeminiResponse.ts`. Any phrasing change here must be mirrored in the
 * defensive parser and re-verified against the live model.
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
    "If a prioritized retailer has insufficient matches, expand the search to other reputable Romanian retailers — do not return a short list just because the top sites are sparse. " +
    "Return prices in RON (Romanian lei). " +
    "Return ONLY a strict JSON array — no prose, no markdown code fences, no explanation. " +
    "Schema per item: {title, description, image_url, price, currency, retailer_url, retailer_name}. " +
    "Return between 5 and 15 items. The 5-item floor is a hard requirement, not a suggestion — if the prioritized retailers do not have enough exact matches, broaden the search to ANY reputable Romanian retailer (or international retailer shipping to Romania) and include closely-related products in the same category. Returning fewer than 5 items is only acceptable when the query is so specific that fewer than 5 real products exist in Romania. " +
    "For image_url: leave it as an empty string — the server will fetch og:image from each retailer URL separately. Do not guess image URLs. " +
    "For retailer_name: use the retailer's public brand name (e.g., 'eMAG', 'Altex', 'Carrefour'), not the domain. " +
    "Drop items missing title, price, or retailer_url.";
  const userPrompt = query;
  return { systemPrompt, userPrompt };
}
