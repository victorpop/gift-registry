/**
 * Phase 17-07 search-v2 TDD RED: unit tests for `parseIntentResponse`.
 *
 * Tests the IntentResult parser with `searchQuery` field (renamed from `cseQuery`
 * after CSE 403 → Serper pivot on 2026-05-28).
 *
 * RED-phase: committed before implementation; tests MUST fail until Task 3.
 */
import { parseIntentResponse } from "../../discover/parseGeminiResponse";

describe("parseIntentResponse", () => {
  const fallback = "cadou copil 2 ani";

  describe("valid JSON", () => {
    it("returns parsed IntentResult for valid JSON object with giftCategories", () => {
      const input = JSON.stringify({
        recipient: "child",
        occasion: "birthday",
        interests: ["toys", "books"],
        budget: { amount: 150, currency: "RON" },
        giftCategories: [
          { name: "Jucarii educative", reason: "Potrivite varstei", searchQuery: "jucarii educative 2 ani" },
          { name: "Carti ilustrate", reason: "Stimuleaza lectura", searchQuery: "carti ilustrate copii" },
        ],
      });
      const result = parseIntentResponse(input, fallback);
      expect(result.giftCategories).toHaveLength(2);
      expect(result.giftCategories[0].name).toBe("Jucarii educative");
      expect(result.giftCategories[0].searchQuery).toBe("jucarii educative 2 ani");
      expect(result.giftCategories[1].searchQuery).toBe("carti ilustrate copii");
      expect(result.recipient).toBe("child");
      expect(result.occasion).toBe("birthday");
    });

    it("preserves all giftCategory fields: name, reason, searchQuery", () => {
      const input = JSON.stringify({
        giftCategories: [
          { name: "Rasnita cafea", reason: "Pasionat de cafea", searchQuery: "rasnita cafea manuala" },
        ],
      });
      const result = parseIntentResponse(input, fallback);
      expect(result.giftCategories[0]).toMatchObject({
        name: "Rasnita cafea",
        reason: "Pasionat de cafea",
        searchQuery: "rasnita cafea manuala",
      });
    });

    it("does NOT use cseQuery field — only searchQuery", () => {
      const input = JSON.stringify({
        giftCategories: [
          { name: "Test", reason: "reason", searchQuery: "correct query" },
        ],
      });
      const result = parseIntentResponse(input, fallback);
      // searchQuery must be present
      expect(result.giftCategories[0].searchQuery).toBe("correct query");
      // cseQuery must not appear on the IntentResult interface
      expect((result.giftCategories[0] as Record<string, unknown>).cseQuery).toBeUndefined();
    });
  });

  describe("JSON fence stripping", () => {
    it("strips ```json fences and parses successfully", () => {
      const inner = JSON.stringify({
        giftCategories: [{ name: "Carte", reason: "educational", searchQuery: "carte copii" }],
      });
      const raw = "```json\n" + inner + "\n```";
      const result = parseIntentResponse(raw, fallback);
      expect(result.giftCategories).toHaveLength(1);
      expect(result.giftCategories[0].searchQuery).toBe("carte copii");
    });

    it("strips plain ``` fences and parses successfully", () => {
      const inner = JSON.stringify({
        giftCategories: [{ name: "Puzzle", reason: "logic", searchQuery: "puzzle 2 ani" }],
      });
      const raw = "```\n" + inner + "\n```";
      const result = parseIntentResponse(raw, fallback);
      expect(result.giftCategories).toHaveLength(1);
    });
  });

  describe("malformed JSON fallback", () => {
    it("returns fallback IntentResult for malformed JSON and logs console.error", () => {
      const spy = jest.spyOn(console, "error").mockImplementation(() => {});
      const result = parseIntentResponse("not valid json {{{{", fallback);
      expect(result.giftCategories).toHaveLength(1);
      expect(result.giftCategories[0].searchQuery).toBe(fallback);
      expect(result.giftCategories[0].name).toBe("");
      expect(result.giftCategories[0].reason).toBe("");
      expect(spy).toHaveBeenCalled();
      spy.mockRestore();
    });

    it("never throws on malformed JSON", () => {
      expect(() => parseIntentResponse("{broken:", fallback)).not.toThrow();
    });
  });

  describe("missing or empty giftCategories fallback", () => {
    it("returns single-category fallback when giftCategories is missing", () => {
      const input = JSON.stringify({ recipient: "adult", occasion: "birthday" });
      const result = parseIntentResponse(input, fallback);
      expect(result.giftCategories).toHaveLength(1);
      expect(result.giftCategories[0].searchQuery).toBe(fallback);
    });

    it("returns single-category fallback when giftCategories is empty array", () => {
      const input = JSON.stringify({ giftCategories: [] });
      const result = parseIntentResponse(input, fallback);
      expect(result.giftCategories).toHaveLength(1);
      expect(result.giftCategories[0].searchQuery).toBe(fallback);
    });
  });

  describe("fan-out cap enforcement", () => {
    it("trims giftCategories to exactly 3 when more than 3 are returned", () => {
      const input = JSON.stringify({
        giftCategories: [
          { name: "A", reason: "r1", searchQuery: "query1" },
          { name: "B", reason: "r2", searchQuery: "query2" },
          { name: "C", reason: "r3", searchQuery: "query3" },
          { name: "D", reason: "r4", searchQuery: "query4" },
          { name: "E", reason: "r5", searchQuery: "query5" },
        ],
      });
      const result = parseIntentResponse(input, fallback);
      expect(result.giftCategories).toHaveLength(3);
      expect(result.giftCategories[0].searchQuery).toBe("query1");
      expect(result.giftCategories[2].searchQuery).toBe("query3");
    });
  });
});
