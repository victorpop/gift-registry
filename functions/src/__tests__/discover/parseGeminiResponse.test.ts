/**
 * Phase 17-07: Compatibility tests for parseGeminiResponse.ts exports.
 *
 * The old `parseGeminiResponse` function (product-array parser) was replaced by
 * `parseIntentResponse` in Phase 17-07 (search v2 re-scope — Serper pivot).
 *
 * parseIntentResponse is tested in parseIntentResponse.test.ts.
 *
 * This file now verifies that the module still exports the required types and
 * functions that other modules depend on (DiscoverProduct, parseIntentResponse).
 */
import { parseIntentResponse } from "../../discover/parseGeminiResponse";
import type { DiscoverProduct } from "../../discover/parseGeminiResponse";

describe("parseGeminiResponse module exports (post search-v2 re-scope)", () => {
  it("exports parseIntentResponse as a function", () => {
    expect(typeof parseIntentResponse).toBe("function");
  });

  it("parseIntentResponse returns an object with giftCategories array", () => {
    const result = parseIntentResponse(
      JSON.stringify({ giftCategories: [{ name: "Test", reason: "r", searchQuery: "test query" }] }),
      "fallback query",
    );
    expect(result.giftCategories).toBeDefined();
    expect(Array.isArray(result.giftCategories)).toBe(true);
  });

  it("DiscoverProduct type is exported (compile-time check)", () => {
    // If DiscoverProduct is not exported, this will cause a TypeScript compile error.
    const p: DiscoverProduct = {
      title: "Test Product",
      description: "A description",
      image_url: "https://img.emag.ro/pic.jpg",
      price: 99,
      currency: "RON",
      retailer_url: "https://www.emag.ro/product/123",
      retailer_name: "eMAG",
    };
    expect(p.title).toBe("Test Product");
    expect(p.price).toBe(99);
    expect(p.currency).toBe("RON");
  });
});
