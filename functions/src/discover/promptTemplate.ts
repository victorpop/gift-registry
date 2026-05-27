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
    "Return between 5 and 15 items. Aim for at least 5 — broaden your search across additional Romanian retailers as needed to reach the floor. " +
    "For image_url: use the product's main image URL from its retailer page (the og:image meta tag or the primary product photo). If you cannot find a direct image URL, leave image_url as an empty string rather than guessing. " +
    "For retailer_name: use the retailer's public brand name (e.g., 'eMAG', 'Altex', 'Carrefour'), not the domain. " +
    "Drop items missing title, price, or retailer_url.";
  const userPrompt = query;
  return { systemPrompt, userPrompt };
}
