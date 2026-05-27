/**
 * Phase 17 D-24/D-48: unit tests for `normalizeCacheKey`.
 * RED-phase: committed before implementation; tests MUST fail.
 */
import { normalizeCacheKey } from "../../discover/cacheKey";

describe("normalizeCacheKey", () => {
  it("lowercases the input", () => {
    expect(normalizeCacheKey("Espresso")).toContain("espresso");
  });

  it("trims leading and trailing whitespace", () => {
    expect(normalizeCacheKey("  espresso  ")).toBe(normalizeCacheKey("espresso"));
  });

  it("collapses internal whitespace", () => {
    expect(normalizeCacheKey("espresso   machine")).toBe(normalizeCacheKey("espresso machine"));
  });

  it("collapses tabs and newlines to a single space", () => {
    expect(normalizeCacheKey("espresso\t\nmachine")).toBe(normalizeCacheKey("espresso machine"));
  });

  it("preserves Romanian diacritics (after URL decode)", () => {
    const out = normalizeCacheKey("cadou pentru bunică");
    expect(decodeURIComponent(out)).toContain("bunică");
  });

  it("URL-encodes slashes (Firestore doc-ID safety)", () => {
    const out = normalizeCacheKey("cadou/bun");
    expect(out).not.toContain("/");
  });

  it("returns a deterministic key (same input → same output)", () => {
    expect(normalizeCacheKey("CADOU")).toBe(normalizeCacheKey("cadou"));
  });
});
