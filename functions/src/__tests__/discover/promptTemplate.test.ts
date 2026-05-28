/**
 * Phase 17-07 search-v2: unit tests for `buildIntentPrompt`.
 *
 * Replaces the old `buildPrompt` tests (product-listing prompt) with tests
 * for the intent-extraction prompt (`buildIntentPrompt`).
 *
 * Key changes from the old test:
 *   - Import `buildIntentPrompt` instead of `buildPrompt`
 *   - Assert intent-extraction semantics (giftCategories, searchQuery, RON)
 *   - Remove old assertions: "Return ONLY a strict JSON array", "Schema per item",
 *     "Return between 5 and 15 items", "Drop items missing"
 */
import { buildIntentPrompt } from "../../discover/promptTemplate";

describe("buildIntentPrompt", () => {
  const sites = ["emag.ro", "altex.ro", "carturesti.ro"];

  it("returns the user query verbatim in userPrompt", () => {
    const { userPrompt } = buildIntentPrompt("espresso machine", sites);
    expect(userPrompt).toBe("espresso machine");
  });

  it("preserves diacritics in userPrompt", () => {
    const { userPrompt } = buildIntentPrompt("cadou pentru bunică", sites);
    expect(userPrompt).toBe("cadou pentru bunică");
  });

  it("systemPrompt instructs 'at most 3 gift categories' or '1-3 gift categories'", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    const hasCapInstruction =
      systemPrompt.includes("3 gift categor") ||
      systemPrompt.includes("1-3 gift categor") ||
      systemPrompt.includes("1 and 3 gift categor") ||
      systemPrompt.includes("at most 3");
    expect(hasCapInstruction).toBe(true);
  });

  it("systemPrompt mentions Romanian retailers / relevant sites", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    // At least one of the passed sites should appear in systemPrompt
    const hasSiteRef =
      systemPrompt.includes("emag.ro") ||
      systemPrompt.includes("altex.ro") ||
      systemPrompt.includes("carturesti.ro") ||
      systemPrompt.includes("Romanian retailer");
    expect(hasSiteRef).toBe(true);
  });

  it("systemPrompt requires RON currency for prices and budgets", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    expect(systemPrompt).toContain("RON");
  });

  it("systemPrompt instructs searchQuery to be a short Romanian product search query", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    // Must describe the query field for each category as a product search query
    const hasQueryInstruction =
      systemPrompt.includes("search query") ||
      systemPrompt.includes("searchQuery") ||
      systemPrompt.includes("product search");
    expect(hasQueryInstruction).toBe(true);
  });

  it("systemPrompt mentions Romanian language for search queries", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    const hasRomanianRef =
      systemPrompt.includes("Romanian") ||
      systemPrompt.includes("română") ||
      systemPrompt.includes("romana");
    expect(hasRomanianRef).toBe(true);
  });

  it("does NOT contain the old product-listing directive 'Return ONLY a strict JSON array'", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    expect(systemPrompt).not.toContain("Return ONLY a strict JSON array");
  });

  it("does NOT contain the old schema instruction 'Schema per item'", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    expect(systemPrompt).not.toContain("Schema per item");
  });

  it("does NOT contain the old item-count range '5 and 15 items'", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    expect(systemPrompt).not.toContain("5 and 15 items");
  });

  it("includes all supplied sites in systemPrompt", () => {
    const { systemPrompt } = buildIntentPrompt("anything", sites);
    for (const site of sites) {
      expect(systemPrompt).toContain(site);
    }
  });
});
