/**
 * Phase 17 UAT-6 follow-up: enrich Gemini search results with REAL page
 * metadata (og:title, og:description, og:image) fetched directly from each
 * retailer URL.
 *
 * Why this is no longer just "enrich images":
 *   During UAT we discovered Gemini hallucinates product slugs onto real
 *   product IDs. Example: it returned a URL with slug
 *   "/jucarie-educativa-montessori-camion-de-ferma-.../pd/DKT00YMBM/" with
 *   title "Jucarie educativa Montessori, camion de ferma, sortare forme".
 *   Fetching that URL with the product ID resolves to an Esprit T-shirt —
 *   og:title is "Esprit, Tricou din bumbac organic cu imprimeu logo".
 *   Gemini invented a query-appropriate title for a product ID it knew
 *   existed but had not actually inspected.
 *
 *   The URL is real (resolves to a real product). The title is fabricated.
 *   The image_url Gemini sometimes returns is empty or a placeholder.
 *   We can only trust the URL + whatever the page itself declares.
 *
 * Strategy:
 *   - Fetch each retailer_url in parallel (4s timeout each).
 *   - Extract og:title, og:description, og:image from the response.
 *   - Override the Gemini-supplied title/description/image_url with the
 *     fetched values when present.
 *   - Drop items where the fetch failed entirely OR where neither og:title
 *     nor og:image could be extracted — better to surface 4 verifiable
 *     products than 12 fabrications.
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

interface FetchedMetadata {
  title: string;
  description: string;
  image: string;
}

async function fetchOgMetadata(url: string): Promise<FetchedMetadata | null> {
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
    if (!response.ok) return null;
    const html = await response.text();
    const root = parse(html);

    const pick = (selectors: string[]): string => {
      for (const sel of selectors) {
        const v = root.querySelector(sel)?.getAttribute("content");
        if (v && v.trim().length > 0) return v.trim();
      }
      return "";
    };

    const title = pick([
      'meta[property="og:title"]',
      'meta[name="og:title"]',
      'meta[property="twitter:title"]',
      'meta[name="twitter:title"]',
    ]);
    const description = pick([
      'meta[property="og:description"]',
      'meta[name="og:description"]',
      'meta[property="twitter:description"]',
      'meta[name="twitter:description"]',
      'meta[name="description"]',
    ]);
    const imageRaw = pick([
      'meta[property="og:image"]',
      'meta[name="og:image"]',
      'meta[property="twitter:image"]',
      'meta[name="twitter:image"]',
    ]);
    const image = imageRaw ? rewriteToHttps(imageRaw) : "";

    if (!title && !image) return null;
    return { title, description, image };
  } catch {
    return null;
  }
}

/**
 * Replace Gemini's potentially-fabricated title/description/image_url with
 * real OG metadata fetched from the retailer URL. Drops items where the
 * fetch failed entirely — Gemini may have hallucinated the URL itself, or
 * the URL may point to a product different from what Gemini described, so
 * we lean on the live page as the source of truth.
 *
 * Never throws. Returns the filtered+enriched array.
 */
export async function enrichWithOgImages(
  products: DiscoverProduct[],
): Promise<DiscoverProduct[]> {
  const enriched = await Promise.all(
    products.map(async (p) => {
      if (!p.retailer_url) return null;
      const meta = await fetchOgMetadata(p.retailer_url);
      if (!meta) return null;
      return {
        ...p,
        title: meta.title || p.title,
        description: meta.description || p.description,
        image_url: meta.image || "",
      };
    }),
  );
  return enriched.filter((p): p is DiscoverProduct => p !== null);
}
