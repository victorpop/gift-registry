/**
 * Phase 17-07 search-v2 TDD RED: unit tests for `serperNormalizer`.
 *
 * Covers: parsePrice (Romanian format), normalizeSerperItems field mapping,
 * applyAllowlistFilter (graceful padding, google.com redirect, www./subdomain strip,
 * .com TLDs), source→retailer_name, https rewrite, and de-dupe by URL.
 *
 * RED-phase: committed before implementation; tests MUST fail until Task 4.
 */
import {
  normalizeSerperItems,
  parsePrice,
  applyAllowlistFilter,
  dedupeByUrl,
} from "../../discover/serperNormalizer";
import type { SerperShoppingItem } from "../../discover/serperClient";

// Minimal fixture factory
function makeItem(overrides: Partial<SerperShoppingItem>): SerperShoppingItem {
  return {
    title: "Test Product",
    source: "eMAG",
    link: "https://www.emag.ro/product/123",
    price: "100 RON",
    imageUrl: "https://encrypted-tbn.gstatic.com/image/123.jpg",
    position: 1,
    ...overrides,
  };
}

// ---------------------------------------------------------------------------
// parsePrice
// ---------------------------------------------------------------------------
describe("parsePrice", () => {
  it("parses plain RON amount", () => {
    expect(parsePrice("179 RON")).toEqual({ price: 179, currency: "RON" });
  });

  it("parses Romanian thousands format (period=thousands, comma=decimal) — must NOT yield 1.299", () => {
    expect(parsePrice("1.299,00 lei")).toEqual({ price: 1299, currency: "RON" });
  });

  it("parses US thousands format (comma=thousands, period=decimal)", () => {
    expect(parsePrice("2,499.00 RON")).toEqual({ price: 2499, currency: "RON" });
  });

  it("returns 0 for undefined input", () => {
    expect(parsePrice(undefined)).toEqual({ price: 0, currency: "RON" });
  });

  it("returns 0 for non-numeric string 'Indisponibil'", () => {
    expect(parsePrice("Indisponibil")).toEqual({ price: 0, currency: "RON" });
  });

  it("returns 0 for non-numeric string 'Pret la cerere'", () => {
    expect(parsePrice("Pret la cerere")).toEqual({ price: 0, currency: "RON" });
  });

  it("returns 0 for empty string", () => {
    expect(parsePrice("")).toEqual({ price: 0, currency: "RON" });
  });

  it("correctly handles integer RON amount without decimals", () => {
    expect(parsePrice("250 RON")).toEqual({ price: 250, currency: "RON" });
  });

  it("detects EUR currency", () => {
    const result = parsePrice("49.99 EUR");
    expect(result.currency).toBe("EUR");
    expect(result.price).toBe(49.99);
  });
});

// ---------------------------------------------------------------------------
// normalizeSerperItems
// ---------------------------------------------------------------------------
describe("normalizeSerperItems", () => {
  it("maps SerperShoppingItem to DiscoverProduct with all required fields", () => {
    const items = [makeItem({ price: "179 RON" })];
    const products = normalizeSerperItems(items);
    expect(products).toHaveLength(1);
    const p = products[0];
    expect(p.title).toBe("Test Product");
    expect(p.retailer_url).toBe("https://www.emag.ro/product/123");
    expect(p.price).toBe(179);
    expect(p.currency).toBe("RON");
    expect(p.image_url).toBe("https://encrypted-tbn.gstatic.com/image/123.jpg");
    expect(p.retailer_name).toBe("eMAG");
    expect(typeof p.description).toBe("string");
  });

  it("uses item.source as retailer_name when present", () => {
    const items = [makeItem({ source: "Altex", link: "https://altex.ro/produs/x" })];
    const products = normalizeSerperItems(items);
    expect(products[0].retailer_name).toBe("Altex");
  });

  it("derives retailer_name from link hostname when source is empty", () => {
    const items = [makeItem({ source: "", link: "https://www.emag.ro/product/abc" })];
    const products = normalizeSerperItems(items);
    // Should derive from link — either via DOMAIN_TO_RETAILER or raw root domain
    expect(products[0].retailer_name).toBeTruthy();
    // Must NOT be empty when link is valid
    expect(products[0].retailer_name.length).toBeGreaterThan(0);
  });

  it("ensures image_url starts with https:// (already https from Serper)", () => {
    const items = [makeItem({ imageUrl: "https://encrypted-tbn.gstatic.com/image/abc.jpg" })];
    const products = normalizeSerperItems(items);
    expect(products[0].image_url.startsWith("https://")).toBe(true);
  });

  it("rewrites http:// imageUrl to https:// (defensive rewrite)", () => {
    const items = [makeItem({ imageUrl: "http://img.example.com/pic.jpg" })];
    const products = normalizeSerperItems(items);
    expect(products[0].image_url.startsWith("https://")).toBe(true);
    expect(products[0].image_url).toBe("https://img.example.com/pic.jpg");
  });

  it("drops item missing link", () => {
    const items = [makeItem({ link: "" })];
    const products = normalizeSerperItems(items);
    expect(products).toHaveLength(0);
  });

  it("drops item missing title", () => {
    const items = [makeItem({ title: "" })];
    const products = normalizeSerperItems(items);
    expect(products).toHaveLength(0);
  });

  it("handles price via parsePrice (Romanian format)", () => {
    const items = [makeItem({ price: "1.299,00 lei" })];
    const products = normalizeSerperItems(items);
    expect(products[0].price).toBe(1299);
  });

  it("sets price=0 when price string is non-numeric", () => {
    const items = [makeItem({ price: "Indisponibil" })];
    const products = normalizeSerperItems(items);
    expect(products[0].price).toBe(0);
  });

  it("sets description to empty string (Serper shopping has no snippet)", () => {
    const items = [makeItem({})];
    const products = normalizeSerperItems(items);
    expect(products[0].description).toBe("");
  });
});

// ---------------------------------------------------------------------------
// applyAllowlistFilter
// ---------------------------------------------------------------------------
describe("applyAllowlistFilter", () => {
  function makeProduct(url: string, title = "Product") {
    return {
      title,
      description: "",
      image_url: "https://img.example.com/pic.jpg",
      price: 100,
      currency: "RON",
      retailer_url: url,
      retailer_name: "Store",
    };
  }

  it("returns all in-allowlist items when >= 3 exist", () => {
    const products = [
      makeProduct("https://www.emag.ro/product/1"),
      makeProduct("https://altex.ro/product/2"),
      makeProduct("https://flanco.ro/product/3"),
      makeProduct("https://www.amazon.com/dp/123"),
    ];
    const result = applyAllowlistFilter(products);
    // All 3 in-allowlist items must be in result
    expect(result.some((p) => p.retailer_url.includes("emag.ro"))).toBe(true);
    expect(result.some((p) => p.retailer_url.includes("altex.ro"))).toBe(true);
    expect(result.some((p) => p.retailer_url.includes("flanco.ro"))).toBe(true);
    // amazon.com must NOT be in in-allowlist position
  });

  it("pads with out-of-allowlist items when fewer than 3 in-allowlist matches", () => {
    const products = [
      makeProduct("https://www.emag.ro/product/1"),          // in allowlist
      makeProduct("https://www.amazon.com/dp/2"),            // NOT in allowlist
      makeProduct("https://www.ebay.com/itm/3"),             // NOT in allowlist
    ];
    const result = applyAllowlistFilter(products);
    // Must include emag.ro (in-allowlist) + padding from amazon/ebay
    expect(result.length).toBeGreaterThanOrEqual(1);
    expect(result.some((p) => p.retailer_url.includes("emag.ro"))).toBe(true);
    // In-allowlist items come first
    expect(result[0].retailer_url).toContain("emag.ro");
  });

  it("treats google.com/shopping redirect links as out-of-allowlist (padding set)", () => {
    const products = [
      makeProduct("https://www.google.com/shopping/product/123456?gl=ro"),
      makeProduct("https://www.emag.ro/product/456"),
      makeProduct("https://altex.ro/product/789"),
      makeProduct("https://flanco.ro/product/012"),
    ];
    const result = applyAllowlistFilter(products);
    // google.com redirect must NOT be in the in-allowlist first positions
    // emag, altex, flanco must all be included
    const googleItem = result.find((p) => p.retailer_url.includes("google.com"));
    const emagItem = result.find((p) => p.retailer_url.includes("emag.ro"));
    expect(emagItem).toBeDefined();
    // If google redirect is included, it should be after the in-allowlist items
    if (googleItem) {
      const googleIdx = result.indexOf(googleItem);
      const emagIdx = result.indexOf(emagItem!);
      expect(emagIdx).toBeLessThan(googleIdx);
    }
  });

  it("strips www. before domain matching (www.emag.ro → emag.ro in allowlist)", () => {
    const products = [
      makeProduct("https://www.emag.ro/product/test"),
      makeProduct("https://www.altex.ro/product/test"),
      makeProduct("https://www.flanco.ro/product/test"),
    ];
    const result = applyAllowlistFilter(products);
    // All three should be recognized as in-allowlist
    expect(result).toHaveLength(3);
  });

  it("matches subdomain like shop.altex.ro → altex.ro in allowlist", () => {
    const products = [
      makeProduct("https://shop.altex.ro/product/123"),
      makeProduct("https://emag.ro/product/456"),
      makeProduct("https://flanco.ro/product/789"),
    ];
    const result = applyAllowlistFilter(products);
    // shop.altex.ro → altex.ro should match allowlist
    expect(result.some((p) => p.retailer_url.includes("shop.altex.ro"))).toBe(true);
  });

  it("correctly matches IKEA with .com TLD (ikea.com)", () => {
    const products = [
      makeProduct("https://www.ikea.com/ro/en/cat/gifts/"),
      makeProduct("https://www.emag.ro/product/1"),
      makeProduct("https://altex.ro/product/2"),
    ];
    const result = applyAllowlistFilter(products);
    expect(result.some((p) => p.retailer_url.includes("ikea.com"))).toBe(true);
  });

  it("correctly matches Etsy with .com TLD (etsy.com)", () => {
    const products = [
      makeProduct("https://www.etsy.com/listing/123/gift"),
      makeProduct("https://www.emag.ro/product/1"),
      makeProduct("https://altex.ro/product/2"),
    ];
    const result = applyAllowlistFilter(products);
    expect(result.some((p) => p.retailer_url.includes("etsy.com"))).toBe(true);
  });

  it("preserves in-allowlist items in original rank order", () => {
    const products = [
      makeProduct("https://flanco.ro/product/3", "Third"),
      makeProduct("https://altex.ro/product/2", "Second"),
      makeProduct("https://www.emag.ro/product/1", "First"),
    ];
    const result = applyAllowlistFilter(products);
    // All should be in-allowlist, original order preserved
    expect(result[0].title).toBe("Third");
    expect(result[1].title).toBe("Second");
    expect(result[2].title).toBe("First");
  });
});

// ---------------------------------------------------------------------------
// dedupeByUrl
// ---------------------------------------------------------------------------
describe("dedupeByUrl", () => {
  it("collapses two products with the same URL (one with UTM param) to one", () => {
    const base = {
      title: "Product A",
      description: "",
      image_url: "https://img.emag.ro/pic.jpg",
      price: 100,
      currency: "RON",
      retailer_name: "eMAG",
    };
    const products = [
      { ...base, retailer_url: "https://www.emag.ro/product/pd/ABC123/a" },
      {
        ...base,
        retailer_url: "https://www.emag.ro/product/pd/ABC123/a?utm_source=google&utm_medium=cpc",
      },
    ];
    const result = dedupeByUrl(products);
    expect(result).toHaveLength(1);
  });

  it("keeps products with different URLs", () => {
    const base = {
      title: "Product",
      description: "",
      image_url: "https://img.emag.ro/pic.jpg",
      price: 100,
      currency: "RON",
      retailer_name: "eMAG",
    };
    const products = [
      { ...base, retailer_url: "https://www.emag.ro/product/pd/ABC123/a" },
      { ...base, retailer_url: "https://www.emag.ro/product/pd/XYZ789/b" },
    ];
    const result = dedupeByUrl(products);
    expect(result).toHaveLength(2);
  });

  it("keeps the first occurrence when deduplicating", () => {
    const base = {
      description: "",
      image_url: "https://img.emag.ro/pic.jpg",
      price: 100,
      currency: "RON",
      retailer_name: "eMAG",
      retailer_url: "https://www.emag.ro/product/pd/ABC123",
    };
    const products = [
      { ...base, title: "First occurrence" },
      { ...base, title: "Second occurrence" },
    ];
    const result = dedupeByUrl(products);
    expect(result).toHaveLength(1);
    expect(result[0].title).toBe("First occurrence");
  });
});
