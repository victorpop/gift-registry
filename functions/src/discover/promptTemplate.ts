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
    "Return prices in RON (Romanian lei). " +
    "Return ONLY a strict JSON array — no prose, no markdown code fences, no explanation. " +
    "Schema per item: {title, description, image_url, price, currency, retailer_url, retailer_name}. " +
    "Return between 5 and 15 items; if fewer than 5 confident matches exist, return what's available — never pad with low-quality matches. " +
    "Drop items missing title, price, or retailer_url.";
  const userPrompt = query;
  return { systemPrompt, userPrompt };
}
