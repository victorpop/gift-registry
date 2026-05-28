/**
 * Phase 17-07: Gemini intent-extraction prompt template.
 *
 * Replaces the old buildPrompt (product-listing prompt) with buildIntentPrompt
 * which asks Gemini to extract intent + generate up-to-3 Romanian search queries.
 *
 * The intent prompt is provider-agnostic: the `searchQuery` field per category
 * is what Serper /shopping receives (previously called `cseQuery` — renamed after
 * the CSE 403 → Serper pivot on 2026-05-28).
 *
 * The BuiltPrompt interface is kept for backward compatibility with callers.
 */

export interface BuiltPrompt {
  systemPrompt: string;
  userPrompt: string;
}

/**
 * Build the Gemini intent-extraction prompt for a Romanian gift-registry search.
 *
 * @param query - The verbatim user query (preserved as userPrompt)
 * @param contextSites - Retailer site names / domains to bias Gemini's categories
 */
export function buildIntentPrompt(query: string, contextSites: string[]): BuiltPrompt {
  const sitesList = contextSites.slice(0, 10).join(", ");

  const systemPrompt =
    "You are a gift-idea assistant for a Romanian gift-registry app. " +
    "Analyze the user's query and extract their intent (recipient, occasion, interests, budget). " +
    "Then generate 1-3 gift categories and an optimized Romanian-language product search query for each category. " +
    `Focus categories on products available at Romanian retailers such as: ${sitesList}. ` +
    "Return prices and budgets in RON. " +
    "For each gift category, searchQuery must be a short, specific product search query in Romanian (2-5 words), " +
    "optimized for finding real products. Example: 'rasnita cafea manuala', 'carte dezvoltare personala'. " +
    "Generate at most 3 gift categories. If fewer than 3 make sense for the query, return fewer.";

  return { systemPrompt, userPrompt: query };
}
