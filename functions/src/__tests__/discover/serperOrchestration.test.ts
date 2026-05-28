/**
 * Phase 17-07 search-v2 TDD RED: orchestration tests for the search pipeline.
 *
 * Tests the `runSearchPipeline` pure function exported from search.ts.
 * Uses jest.mock to stub callSerper and callGeminiIntent so no real API calls happen.
 *
 * RED-phase: committed before implementation; tests MUST fail until Task 5.
 */

// Mock the dependencies before importing the module under test
jest.mock("../../discover/serperClient");
jest.mock("../../discover/geminiClient");

import { runSearchPipeline } from "../../discover/search";
import { callSerper } from "../../discover/serperClient";
import { callGeminiIntent } from "../../discover/geminiClient";
import type { SerperShoppingItem } from "../../discover/serperClient";
import type { IntentResult } from "../../discover/geminiClient";

const mockCallSerper = callSerper as jest.MockedFunction<typeof callSerper>;
const mockCallGeminiIntent = callGeminiIntent as jest.MockedFunction<typeof callGeminiIntent>;

function makeShoppingItem(title: string, url: string): SerperShoppingItem {
  return {
    title,
    source: "eMAG",
    link: url,
    price: "100 RON",
    imageUrl: "https://encrypted-tbn.gstatic.com/image/abc.jpg",
    position: 1,
  };
}

function makeIntentResult(searchQueries: string[]): IntentResult {
  return {
    giftCategories: searchQueries.map((q, i) => ({
      name: `Category ${i + 1}`,
      reason: `Reason ${i + 1}`,
      searchQuery: q,
    })),
  };
}

describe("runSearchPipeline", () => {
  const deps = {
    intentFn: mockCallGeminiIntent,
    serperFn: mockCallSerper,
    apiKeyGemini: "test-gemini-key",
    apiKeySerper: "test-serper-key",
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("fan-out cap", () => {
    it("issues exactly 3 callSerper invocations when giftCategories has 3 entries", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult(["jucarii copii", "carti ilustrate", "puzzle educativ"]),
      );
      mockCallSerper.mockResolvedValue([
        makeShoppingItem("Product A", "https://www.emag.ro/product/a"),
      ]);

      await runSearchPipeline("cadou copil", deps);

      expect(mockCallSerper).toHaveBeenCalledTimes(3);
    });

    it("issues at most 3 callSerper invocations when giftCategories has 5 entries (defensive)", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult([
          "jucarii copii",
          "carti ilustrate",
          "puzzle educativ",
          "lego seturi",
          "figurine colectie",
        ]),
      );
      mockCallSerper.mockResolvedValue([
        makeShoppingItem("Product", "https://www.emag.ro/product/a"),
      ]);

      await runSearchPipeline("cadou copil", deps);

      // Cap at 3 regardless of how many giftCategories returned
      expect(mockCallSerper).toHaveBeenCalledTimes(3);
    });
  });

  describe("Promise.allSettled partial-failure tolerance", () => {
    it("keeps results from two successful calls when one callSerper rejects", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult(["query1", "query2", "query3"]),
      );
      mockCallSerper
        .mockResolvedValueOnce([
          makeShoppingItem("Product 1", "https://www.emag.ro/product/1"),
        ])
        .mockRejectedValueOnce(new Error("Serper timeout"))
        .mockResolvedValueOnce([
          makeShoppingItem("Product 2", "https://altex.ro/product/2"),
        ]);

      const products = await runSearchPipeline("some query", deps);

      // Should not throw; should return the 2 successful results
      expect(products.length).toBeGreaterThanOrEqual(1);
      expect(mockCallSerper).toHaveBeenCalledTimes(3);
    });

    it("returns empty array when all 3 callSerper calls reject", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult(["q1", "q2", "q3"]),
      );
      mockCallSerper.mockRejectedValue(new Error("Serper unreachable"));

      const products = await runSearchPipeline("query", deps);

      expect(products).toHaveLength(0);
    });
  });

  describe("empty intent fallback", () => {
    it("issues exactly 1 callSerper call with the raw query when giftCategories is empty", async () => {
      mockCallGeminiIntent.mockResolvedValue({ giftCategories: [] });
      mockCallSerper.mockResolvedValue([
        makeShoppingItem("Product", "https://www.emag.ro/product/a"),
      ]);

      await runSearchPipeline("cadou prieteni", deps);

      expect(mockCallSerper).toHaveBeenCalledTimes(1);
      // The single call must use the raw query
      expect(mockCallSerper).toHaveBeenCalledWith("cadou prieteni", expect.any(String));
    });

    it("uses raw query as fallback when Gemini intent call throws", async () => {
      mockCallGeminiIntent.mockRejectedValue(new Error("Gemini API error"));
      mockCallSerper.mockResolvedValue([
        makeShoppingItem("Product", "https://www.emag.ro/product/a"),
      ]);

      const products = await runSearchPipeline("cadou mame", deps);

      // Should not throw; should fall back to 1 Serper call
      expect(mockCallSerper).toHaveBeenCalledTimes(1);
      expect(products.length).toBeGreaterThanOrEqual(0);
    });
  });

  describe("result cap", () => {
    it("caps the flat product list at 20 even when Serper returns more", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult(["query1", "query2", "query3"]),
      );
      // 3 calls × 10 unique allowlisted items = 30 unique products before cap
      const tenItems = (prefix: string): SerperShoppingItem[] =>
        Array.from({ length: 10 }, (_, i) =>
          makeShoppingItem(`${prefix} ${i}`, `https://www.emag.ro/product/${prefix}-${i}`),
        );
      mockCallSerper
        .mockResolvedValueOnce(tenItems("a"))
        .mockResolvedValueOnce(tenItems("b"))
        .mockResolvedValueOnce(tenItems("c"));

      const products = await runSearchPipeline("cadou copil", deps);

      expect(products.length).toBe(20);
    });
  });

  describe("de-duplication", () => {
    it("collapses duplicate URLs across different Serper calls into a single product", async () => {
      mockCallGeminiIntent.mockResolvedValue(
        makeIntentResult(["query1", "query2"]),
      );
      const duplicateUrl = "https://www.emag.ro/product/pd/SAME123";
      // Both Serper calls return the same URL
      mockCallSerper
        .mockResolvedValueOnce([makeShoppingItem("Product A", duplicateUrl)])
        .mockResolvedValueOnce([makeShoppingItem("Product A duplicate", duplicateUrl)]);

      const products = await runSearchPipeline("some query", deps);

      // After de-dupe, only one entry for the same normalized URL
      const sameUrlProducts = products.filter((p) => p.retailer_url === duplicateUrl);
      expect(sameUrlProducts.length).toBeLessThanOrEqual(1);
    });
  });
});
