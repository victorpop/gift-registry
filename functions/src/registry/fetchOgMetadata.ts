import { onCall, HttpsError } from "firebase-functions/v2/https";
import { parse, HTMLElement } from "node-html-parser";

interface OgMetadataResponse {
  title: string | null;
  imageUrl: string | null;
  /** Pre-formatted display string, e.g. "459,00 RON". Kept as the primary field for UI auto-fill. */
  price: string | null;
  /** Numeric price as a string (not a number — preserves original locale decimal), e.g. "459.00". */
  priceAmount: string | null;
  /** ISO 4217 currency code, e.g. "RON", "EUR", "USD". Null if no currency could be determined. */
  priceCurrency: string | null;
  siteName: string | null;
}

interface PriceCandidate {
  amount: string;
  currency: string | null;
}

/**
 * Maps common currency symbols and localized words (Romanian "lei", etc.) to their
 * ISO 4217 code. Used both when parsing structured metadata and when extracting
 * currency from raw price strings like "459,00 lei" or "€ 19.99".
 */
const CURRENCY_ALIASES: Record<string, string> = {
  // symbols
  "€": "EUR",
  "$": "USD",
  "£": "GBP",
  "¥": "JPY",
  "₽": "RUB",
  "₺": "TRY",
  "₴": "UAH",
  "₹": "INR",
  // Romanian
  "lei": "RON",
  "ron": "RON",
  // common English / European spellings
  "eur": "EUR",
  "euro": "EUR",
  "euros": "EUR",
  "usd": "USD",
  "us$": "USD",
  "gbp": "GBP",
  "chf": "CHF",
  "pln": "PLN",
  "huf": "HUF",
  "czk": "CZK",
  "bgn": "BGN",
};

/**
 * Normalizes a free-form currency token ("Lei", "€", "EUR") to an ISO 4217 code,
 * or returns null if unrecognized.
 */
function normalizeCurrency(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const trimmed = raw.trim();
  if (trimmed === "") return null;
  // If it's already a 3-letter uppercase code, trust it.
  if (/^[A-Z]{3}$/.test(trimmed)) return trimmed;
  return CURRENCY_ALIASES[trimmed.toLowerCase()] ?? null;
}

/**
 * Extracts a numeric amount from a raw price string, leaving the original
 * decimal separator intact so we don't lose locale information on the way to
 * display. Accepts forms like "459", "459.00", "459,00", "1.299,99", "$19.99".
 */
function extractAmount(raw: string): string | null {
  // Grab the first run that looks like a number, optionally with thousand separators
  // and a decimal comma or dot. Examples matched: 459, 459.00, 459,00, 1.299,99, 1,299.99.
  const match = raw.match(/\d[\d.,]*/);
  return match ? match[0] : null;
}

/**
 * Given a raw price string that may or may not already include currency
 * information (e.g. "459,00 lei", "€19.99", "1299"), return a PriceCandidate
 * with the numeric part and a normalized currency when one can be inferred
 * from the same string. Callers layer additional currency sources on top.
 */
function parsePriceString(raw: string): PriceCandidate | null {
  const amount = extractAmount(raw);
  if (amount == null) return null;

  // Pull out anything that's *not* digits/separators/whitespace and try to
  // normalize it as a currency token. This catches "lei", "€", "RON", "EUR", etc.
  const remainder = raw.replace(amount, "").replace(/[\s\u00A0]+/g, " ").trim();
  let currency: string | null = null;
  if (remainder) {
    // Try the whole remainder first, then each whitespace-separated piece.
    currency = normalizeCurrency(remainder);
    if (!currency) {
      for (const token of remainder.split(/\s+/)) {
        currency = normalizeCurrency(token);
        if (currency) break;
      }
    }
    // Symbols often sit flush against the number with no space — try the
    // first character of the remainder as a last resort.
    if (!currency && remainder.length > 0) {
      currency = normalizeCurrency(remainder[0]);
    }
  }
  return { amount, currency };
}

/**
 * Walks a parsed JSON-LD blob looking for a schema.org price and its currency.
 * Handles:
 * - a Product with `offers` as an object or array
 * - a bare Offer at the root
 * - `@graph` arrays (common on WordPress / WooCommerce)
 * - `priceSpecification.price` for AggregateOffer
 * Returns the first candidate it encounters, or null if none is found.
 */
function findPriceInJsonLd(node: unknown): PriceCandidate | null {
  if (node == null) return null;
  if (Array.isArray(node)) {
    for (const item of node) {
      const found = findPriceInJsonLd(item);
      if (found != null) return found;
    }
    return null;
  }
  if (typeof node !== "object") return null;
  const obj = node as Record<string, unknown>;

  // Direct price field (Offer, AggregateOffer, PriceSpecification).
  const direct = obj.price ?? obj.lowPrice;
  if (direct != null) {
    let amount: string | null = null;
    if (typeof direct === "string" && direct.trim() !== "") amount = direct.trim();
    else if (typeof direct === "number") amount = String(direct);
    if (amount != null) {
      const currency = normalizeCurrency(
        (obj.priceCurrency ?? obj.currency) as string | undefined
      );
      return { amount, currency };
    }
  }

  // Recurse into likely containers.
  for (const key of ["offers", "priceSpecification", "@graph", "mainEntity"]) {
    if (key in obj) {
      const found = findPriceInJsonLd(obj[key]);
      if (found != null) return found;
    }
  }
  return null;
}

/**
 * Combines a raw amount and an optional currency into a human-facing display
 * string. We intentionally keep the original amount format (which may use a
 * comma decimal separator) rather than reformatting via Intl — the source page
 * already reflects the locale the gift-giver is used to seeing.
 */
function formatPriceForDisplay(amount: string, currency: string | null): string {
  return currency ? `${amount} ${currency}` : amount;
}

/**
 * Resolves a PriceCandidate by consulting, in order of reliability:
 * 1. OG tags (og:price:amount + og:price:currency).
 * 2. Product-scoped OG (og:product:price:amount + og:product:price:currency).
 * 3. <meta name="product:price:amount"> + <meta name="product:price:currency">.
 * 4. Microdata <... itemprop="price"> + <... itemprop="priceCurrency">.
 * 5. JSON-LD walk.
 * If a source provides amount but no currency, falls through to the next source
 * only for the currency portion is NOT attempted — instead we try to parse
 * currency out of the amount string itself (handles "459,00 lei" shapes).
 */
function resolvePrice(root: HTMLElement): PriceCandidate | null {
  const og = (property: string): string | null =>
    root.querySelector(`meta[property="og:${property}"]`)?.getAttribute("content") ?? null;
  const metaName = (name: string): string | null =>
    root.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? null;

  // Each tuple: [raw amount, explicit currency tag value or null].
  const sources: Array<[string | null, string | null]> = [
    [og("price:amount"), og("price:currency")],
    [og("product:price:amount"), og("product:price:currency")],
    [metaName("product:price:amount"), metaName("product:price:currency")],
    [
      root.querySelector('[itemprop="price"]')?.getAttribute("content") ??
        root.querySelector('[itemprop="price"]')?.text?.trim() ??
        null,
      root.querySelector('[itemprop="priceCurrency"]')?.getAttribute("content") ??
        root.querySelector('[itemprop="priceCurrency"]')?.text?.trim() ??
        null,
    ],
  ];

  for (const [rawAmount, rawCurrency] of sources) {
    if (!rawAmount) continue;
    const parsed = parsePriceString(rawAmount);
    if (!parsed) continue;
    const currency = normalizeCurrency(rawCurrency) ?? parsed.currency;
    return { amount: parsed.amount, currency };
  }

  // JSON-LD fallback.
  const scripts = root.querySelectorAll('script[type="application/ld+json"]');
  for (const script of scripts) {
    const raw = script.text?.trim();
    if (!raw) continue;
    let parsed: unknown;
    try {
      parsed = JSON.parse(raw);
    } catch {
      continue; // some sites embed invalid JSON-LD; skip it
    }
    const found = findPriceInJsonLd(parsed);
    if (found) {
      // JSON-LD amounts are usually already clean numerics, but run them
      // through parsePriceString anyway for consistency (handles "19.99 USD"
      // shapes that a few sites smuggle into the price field).
      const normalized = parsePriceString(found.amount);
      if (normalized) {
        return {
          amount: normalized.amount,
          currency: found.currency ?? normalized.currency,
        };
      }
    }
  }

  return null;
}

export const fetchOgMetadata = onCall(
  { region: "europe-west3" },
  async (request): Promise<OgMetadataResponse> => {
    const url: string | undefined = request.data?.url;
    if (!url || typeof url !== "string") {
      throw new HttpsError("invalid-argument", "url is required and must be a string");
    }

    // Basic URL validation
    try {
      new URL(url);
    } catch {
      throw new HttpsError("invalid-argument", "url must be a valid URL");
    }

    const empty: OgMetadataResponse = {
      title: null,
      imageUrl: null,
      price: null,
      priceAmount: null,
      priceCurrency: null,
      siteName: null,
    };

    try {
      const response = await fetch(url, {
        headers: {
          "User-Agent": "Mozilla/5.0 (compatible; GiftRegistryBot/1.0)",
          "Accept": "text/html",
        },
        signal: AbortSignal.timeout(5000),
      });

      if (!response.ok) {
        // Return nulls on HTTP error — client falls back to manual entry (D-11)
        return empty;
      }

      const html = await response.text();
      const root = parse(html);

      const og = (property: string): string | null =>
        root.querySelector(`meta[property="og:${property}"]`)?.getAttribute("content") ?? null;

      const priceCandidate = resolvePrice(root);

      return {
        title: og("title") ?? root.querySelector("title")?.text?.trim() ?? null,
        imageUrl: og("image"),
        price: priceCandidate
          ? formatPriceForDisplay(priceCandidate.amount, priceCandidate.currency)
          : null,
        priceAmount: priceCandidate?.amount ?? null,
        priceCurrency: priceCandidate?.currency ?? null,
        siteName: og("site_name"),
      };
    } catch {
      // Return nulls on any failure — network timeout, parse error, etc.
      // Client falls back to manual entry (D-11)
      return empty;
    }
  }
);
