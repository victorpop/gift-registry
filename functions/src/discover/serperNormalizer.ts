/**
 * Phase 17-07: Serper /shopping result normalizer.
 *
 * Converts SerperShoppingItem[] → DiscoverProduct[] with:
 *   - parsePrice: Romanian + US number format (period/comma disambiguation)
 *   - normalizeSerperItems: field mapping per RESEARCH §3
 *   - applyAllowlistFilter: 43-store allowlist post-filter with graceful padding
 *   - dedupeByUrl: collapses same-URL products (normalizeUrl-based)
 *   - deriveRetailerName: source → DOMAIN_TO_RETAILER fallback → raw root domain
 *   - rewriteToHttps: defensive https rewrite (Serper imageUrl is already https,
 *     but belt-and-suspenders per Pitfall 6 in enrichImages context)
 *
 * Note: rewriteToHttps copied from enrichImages.ts before that file is deleted in Task 5.
 */

import type { DiscoverProduct } from "./parseGeminiResponse";
import type { SerperShoppingItem } from "./serperClient";
import { normalizeUrl } from "./urlNormalization";

// ---------------------------------------------------------------------------
// rewriteToHttps — copied from enrichImages.ts (Task 5 will delete that file)
// ---------------------------------------------------------------------------
function rewriteToHttps(raw: string): string {
  return raw.startsWith("http://") ? "https://" + raw.slice(7) : raw;
}

// ---------------------------------------------------------------------------
// extractRootDomain
// ---------------------------------------------------------------------------
/**
 * Extract the root domain from a URL for allowlist matching.
 *
 * "www.emag.ro"    → "emag.ro"
 * "shop.altex.ro"  → "altex.ro"
 * "ikea.com"       → "ikea.com"
 * invalid URL      → ""
 */
function extractRootDomain(url: string): string {
  try {
    const hostname = new URL(url).hostname;
    // Strip www. prefix
    const withoutWww = hostname.replace(/^www\./, "");
    // For subdomains like "shop.altex.ro" → take last two segments
    const parts = withoutWww.split(".");
    if (parts.length > 2) {
      return parts.slice(-2).join(".");
    }
    return withoutWww;
  } catch {
    return "";
  }
}

// ---------------------------------------------------------------------------
// ALLOWED_DOMAINS — 43-store allowlist (from 17-SEARCH-V2-SPEC.md + 17-RESEARCH §4)
// ---------------------------------------------------------------------------
export const ALLOWED_DOMAINS = new Set([
  // Electronics / general
  "emag.ro",
  "altex.ro",
  "mediagalaxy.ro",
  "flanco.ro",
  "cel.ro",
  "pcgarage.ro",
  "vexio.ro",
  // Fashion
  "fashiondays.ro",
  "aboutyou.ro",
  "answear.ro",
  "modivo.ro",
  "zalando.ro",
  "epantofi.ro",
  "otter.ro",
  // Beauty / health
  "notino.ro",
  "sephora.ro",
  "douglas.ro",
  "sabon.ro",
  "farmaciatei.ro",
  "bebetei.ro",
  // Home / furniture
  "ikea.com",
  "jysk.ro",
  "mobexpert.ro",
  "bonami.ro",
  "vivre.ro",
  // Books / media
  "carturesti.ro",
  "libris.ro",
  "elefant.ro",
  // Kids / toys
  "noriel.ro",
  "hobbyshop.ro",
  // Sports / outdoors
  "decathlon.ro",
  "sportguru.ro",
  "hervis.ro",
  "intersport.ro",
  // Baby / maternity
  "mothercare.ro",
  // Flowers / gifts
  "floria.ro",
  "magnolia.ro",
  "complice.ro",
  // Handmade / crafts
  "etsy.com",
  "breslo.ro",
  "kfea.ro",
  // Specialty
  "delicateseflorescu.ro",
  "nespresso.com",
]);

// ---------------------------------------------------------------------------
// DOMAIN_TO_RETAILER — fallback when item.source is empty
// ---------------------------------------------------------------------------
const DOMAIN_TO_RETAILER: Record<string, string> = {
  "emag.ro": "eMAG",
  "altex.ro": "Altex",
  "mediagalaxy.ro": "Media Galaxy",
  "flanco.ro": "Flanco",
  "cel.ro": "CEL.ro",
  "pcgarage.ro": "PC Garage",
  "vexio.ro": "Vexio",
  "fashiondays.ro": "Fashion Days",
  "aboutyou.ro": "About You",
  "answear.ro": "Answear",
  "modivo.ro": "Modivo",
  "zalando.ro": "Zalando",
  "epantofi.ro": "EPantofi",
  "otter.ro": "Otter",
  "notino.ro": "Notino",
  "sephora.ro": "Sephora",
  "douglas.ro": "Douglas",
  "sabon.ro": "Sabon",
  "farmaciatei.ro": "Farmacia Tei",
  "bebetei.ro": "Bebe Tei",
  "ikea.com": "IKEA",
  "jysk.ro": "JYSK",
  "mobexpert.ro": "Mobexpert",
  "bonami.ro": "Bonami",
  "vivre.ro": "Vivre",
  "carturesti.ro": "Cărturești",
  "libris.ro": "Libris",
  "elefant.ro": "Elefant",
  "noriel.ro": "Noriel",
  "hobbyshop.ro": "Hobby Shop",
  "decathlon.ro": "Decathlon",
  "sportguru.ro": "SportGuru",
  "hervis.ro": "Hervis",
  "intersport.ro": "Intersport",
  "mothercare.ro": "Mothercare",
  "floria.ro": "Floria",
  "magnolia.ro": "Magnolia",
  "complice.ro": "Complice",
  "etsy.com": "Etsy",
  "breslo.ro": "Breslo",
  "kfea.ro": "Kfea",
  "delicateseflorescu.ro": "Delicatese Florescu",
  "nespresso.com": "Nespresso",
};

// ---------------------------------------------------------------------------
// isAllowedDomain
// ---------------------------------------------------------------------------
function isAllowedDomain(url: string): boolean {
  return ALLOWED_DOMAINS.has(extractRootDomain(url));
}

// ---------------------------------------------------------------------------
// deriveRetailerName — source first, hostname fallback
// ---------------------------------------------------------------------------
function deriveRetailerName(item: SerperShoppingItem): string {
  // source is the merchant label from Google Shopping — use it directly when present
  if (item.source && item.source.trim().length > 0) {
    return item.source.trim();
  }
  // Fallback: derive from link hostname
  const root = extractRootDomain(item.link);
  return DOMAIN_TO_RETAILER[root] ?? root;
}

// ---------------------------------------------------------------------------
// parsePrice — handles Romanian + US number formats
// ---------------------------------------------------------------------------
/**
 * Parse a Serper /shopping price string into a numeric value and currency code.
 *
 * Romanian format: "1.299,00 lei" → { price: 1299, currency: "RON" }
 *   (period = thousands separator; comma = decimal separator)
 * US/standard:     "2,499.00 RON" → { price: 2499, currency: "RON" }
 *   (comma = thousands separator; period = decimal separator)
 * Non-numeric:     "Indisponibil" → { price: 0, currency: "RON" }
 * Absent:          undefined      → { price: 0, currency: "RON" }
 *
 * Critical: naive parseFloat("1.299,00") → 1.299 (wrong!). This function
 * detects Romanian formatting and corrects it. Unit-tested in serperNormalizer.test.ts.
 */
export function parsePrice(priceStr: string | undefined): { price: number; currency: string } {
  if (!priceStr || priceStr.trim().length === 0) {
    return { price: 0, currency: "RON" };
  }

  // Detect currency — "lei" is Romanian for RON
  const currency = /lei/i.test(priceStr)
    ? "RON"
    : /RON/i.test(priceStr)
    ? "RON"
    : /EUR/i.test(priceStr)
    ? "EUR"
    : "RON";

  // Remove currency labels and any surrounding whitespace
  const cleaned = priceStr
    .replace(/lei|RON|EUR/gi, "")
    .trim();

  if (cleaned.length === 0) return { price: 0, currency };

  // Romanian-format detection:
  //   "1.299,00"  → dot-then-3-digits-then-comma  OR  comma-then-2-digits at end
  // US-format:
  //   "2,499.00"  → comma-then-digits-then-period
  let normalized: string;
  if (/\d\.\d{3}[,\s]/.test(cleaned) || /,\d{2}$/.test(cleaned)) {
    // Romanian format: dots are thousands separators, comma is decimal
    normalized = cleaned.replace(/\./g, "").replace(",", ".");
  } else {
    // Assume US/standard format: strip thousands commas
    normalized = cleaned.replace(/,/g, "");
  }

  const price = parseFloat(normalized);
  return { price: isNaN(price) ? 0 : price, currency };
}

// ---------------------------------------------------------------------------
// normalizeSerperItems — SerperShoppingItem[] → DiscoverProduct[]
// ---------------------------------------------------------------------------
/**
 * Map Serper /shopping items to the DiscoverProduct shape.
 *
 * Drops items missing link or title (required fields).
 * Maps imageUrl → image_url with https rewrite (defensive).
 * Maps source → retailer_name via deriveRetailerName.
 * Sets description = "" (Serper shopping carries no snippet).
 */
export function normalizeSerperItems(items: SerperShoppingItem[]): DiscoverProduct[] {
  return items
    .filter((item) => item.link && item.link.trim().length > 0 && item.title && item.title.trim().length > 0)
    .map((item) => {
      const { price, currency } = parsePrice(item.price);
      return {
        title: item.title,
        description: "",
        image_url: item.imageUrl ? rewriteToHttps(item.imageUrl) : "",
        price,
        currency,
        retailer_url: item.link,
        retailer_name: deriveRetailerName(item),
      };
    });
}

// ---------------------------------------------------------------------------
// applyAllowlistFilter — in-allowlist first, graceful padding
// ---------------------------------------------------------------------------
const MIN_IN_ALLOWLIST = 3; // minimum before padding with out-of-allowlist items

/**
 * Post-filter products to the 43-store allowlist with graceful padding.
 *
 * - If >= MIN_IN_ALLOWLIST products are in-allowlist: return only those (original rank order).
 * - If < MIN_IN_ALLOWLIST products are in-allowlist: prepend in-allowlist results then append
 *   out-of-allowlist results (rank order) to pad. No hard minimum enforced — callers may get
 *   fewer than MIN_IN_ALLOWLIST total if all results are out-of-allowlist.
 *
 * Note: google.com/shopping redirect links → extractRootDomain → "google.com" → NOT in allowlist.
 * These land in the out-of-allowlist padding set (still work via browser redirect on Android).
 */
export function applyAllowlistFilter(products: DiscoverProduct[]): DiscoverProduct[] {
  const inAllowlist = products.filter((p) => isAllowedDomain(p.retailer_url));
  if (inAllowlist.length >= MIN_IN_ALLOWLIST) {
    return inAllowlist;
  }
  const outOfAllowlist = products.filter((p) => !isAllowedDomain(p.retailer_url));
  return [...inAllowlist, ...outOfAllowlist];
}

// ---------------------------------------------------------------------------
// dedupeByUrl — collapse duplicate products by normalized URL
// ---------------------------------------------------------------------------
/**
 * Remove duplicate products where two or more share the same normalized URL.
 *
 * Uses normalizeUrl() from urlNormalization.ts (strips tracking params + SHA-256).
 * Falls back to raw retailer_url as key if normalizeUrl throws (e.g. invalid URL).
 */
export function dedupeByUrl(products: DiscoverProduct[]): DiscoverProduct[] {
  const seen = new Set<string>();
  const result: DiscoverProduct[] = [];
  for (const product of products) {
    let key: string;
    try {
      key = normalizeUrl(product.retailer_url).productId;
    } catch {
      key = product.retailer_url;
    }
    if (!seen.has(key)) {
      seen.add(key);
      result.push(product);
    }
  }
  return result;
}
