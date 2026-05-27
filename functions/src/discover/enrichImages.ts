/**
 * Phase 17 UAT-6 follow-up: enrich Gemini search results with og:image
 * fetched directly from each product page.
 *
 * Why: Gemini with `google_search` grounding has access to search snippets
 * only — NOT the full page DOM — so it cannot reliably produce og:image
 * URLs. Asking the model for image_url either yields an empty string or a
 * hallucinated/expired URL. Reliable images require a server-side fetch
 * of each retailer URL and direct og:image parsing.
 *
 * Strategy:
 *   - Only enrich items where image_url is empty or missing.
 *   - One parallel HTTP fetch per URL, 4s timeout each.
 *   - Failures swallowed: empty image_url remains empty, client falls
 *     back to discover_card_placeholder.
 *   - http:// image URLs rewritten to https:// (Android cleartext block).
 */
import { parse } from "node-html-parser";
import type { DiscoverProduct } from "./parseGeminiResponse";

const FETCH_TIMEOUT_MS = 4000;
const USER_AGENT =
  "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 GiftRegistryBot/1.0";

function rewriteToHttps(raw: string): string {
  return raw.startsWith("http://") ? "https://" + raw.slice(7) : raw;
}

async function fetchOgImage(url: string): Promise<string> {
  try {
    const response = await fetch(url, {
      method: "GET",
      headers: {
        "User-Agent": USER_AGENT,
        Accept: "text/html,application/xhtml+xml",
      },
      redirect: "follow",
      signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
    });
    if (!response.ok) return "";
    const html = await response.text();
    const root = parse(html);
    const candidates = [
      root
        .querySelector('meta[property="og:image"]')
        ?.getAttribute("content"),
      root
        .querySelector('meta[name="og:image"]')
        ?.getAttribute("content"),
      root
        .querySelector('meta[property="twitter:image"]')
        ?.getAttribute("content"),
      root
        .querySelector('meta[name="twitter:image"]')
        ?.getAttribute("content"),
    ];
    for (const c of candidates) {
      if (c && c.trim().length > 0) return rewriteToHttps(c.trim());
    }
    return "";
  } catch {
    return "";
  }
}

/**
 * Enrich a list of products with og:image URLs fetched from each
 * retailer_url, in parallel. Items that already have an image_url are
 * left untouched. Returns the same array shape; never throws.
 */
export async function enrichWithOgImages(
  products: DiscoverProduct[],
): Promise<DiscoverProduct[]> {
  const enriched = await Promise.all(
    products.map(async (p) => {
      if (p.image_url && p.image_url.length > 0) return p;
      if (!p.retailer_url) return p;
      const ogImage = await fetchOgImage(p.retailer_url);
      return ogImage ? { ...p, image_url: ogImage } : p;
    }),
  );
  return enriched;
}
