/**
 * Phase 17 D-16/D-17/D-48: unit tests for `normalizeUrl` — verbatim spec cases.
 * RED-phase: this file is committed before the implementation; tests MUST fail.
 */
import { normalizeUrl } from "../../discover/urlNormalization";

describe("normalizeUrl", () => {
  it("returns the same productId for same product with different utm params", () => {
    const a = normalizeUrl("https://emag.ro/products/x?utm_source=fb&utm_campaign=spring");
    const b = normalizeUrl("https://emag.ro/products/x?utm_source=google");
    expect(a.productId).toBe(b.productId);
  });

  it("is independent of query-param order", () => {
    const a = normalizeUrl("https://emag.ro/x?a=1&b=2");
    const b = normalizeUrl("https://emag.ro/x?b=2&a=1");
    expect(a.productId).toBe(b.productId);
  });

  it("treats trailing slash as identical", () => {
    const a = normalizeUrl("https://emag.ro/x/");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("preserves trailing slash on root path", () => {
    const a = normalizeUrl("https://emag.ro/");
    expect(a.canonicalUrl).toBe("https://emag.ro/");
  });

  it("strips fragment", () => {
    const a = normalizeUrl("https://emag.ro/x#section");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("lowercases host", () => {
    const a = normalizeUrl("https://EMAG.ro/x");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("forces http to https", () => {
    const a = normalizeUrl("http://emag.ro/x");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("strips EMAG affiliate suffix params (ref, affiliate_id, cmpid)", () => {
    const a = normalizeUrl("https://emag.ro/x?ref=aff123&affiliate_id=42&cmpid=xyz");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("strips all utm_* tracking params", () => {
    const a = normalizeUrl(
      "https://emag.ro/x?utm_source=a&utm_medium=b&utm_campaign=c&utm_term=d&utm_content=e",
    );
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("strips fbclid, gclid, mc_cid, mc_eid", () => {
    const a = normalizeUrl("https://emag.ro/x?fbclid=1&gclid=2&mc_cid=3&mc_eid=4");
    const b = normalizeUrl("https://emag.ro/x");
    expect(a.productId).toBe(b.productId);
  });

  it("returns sha256 hex (64 chars) for productId", () => {
    const { productId } = normalizeUrl("https://emag.ro/x");
    expect(productId).toMatch(/^[a-f0-9]{64}$/);
  });

  it("preserves non-tracking query params", () => {
    const a = normalizeUrl("https://emag.ro/x?color=red");
    expect(a.canonicalUrl).toContain("color=red");
  });

  it("emits canonicalUrl starting with https://", () => {
    const a = normalizeUrl("http://emag.ro/x");
    expect(a.canonicalUrl.startsWith("https://")).toBe(true);
  });
});
