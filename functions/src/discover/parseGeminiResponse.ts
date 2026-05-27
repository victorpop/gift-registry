/**
 * Phase 17 D-30: defensive Gemini response parser.
 *
 * Contract:
 *   1. Strip markdown code fences (```json...``` or ```...```).
 *   2. JSON.parse inside try/catch — failure → [] + console.error.
 *   3. Verify root is array — else [].
 *   4. Per item: drop if missing title|price|retailer_url; coerce price to
 *      number (parseFloat allowed); description truncated to 200 chars;
 *      image_url, currency, retailer_name default sensibly.
 *   5. NEVER throw — Gemini parse errors must surface as empty results, not
 *      HttpsError to the client.
 */

export interface DiscoverProduct {
  title: string;
  description: string;
  image_url: string;
  price: number;
  currency: string;
  retailer_url: string;
  retailer_name: string;
}

export function parseGeminiResponse(raw: string, query?: string): DiscoverProduct[] {
  // 1. Strip code fences. Permissive on opening + closing whitespace.
  let cleaned = raw.trim();
  cleaned = cleaned.replace(/^\s*```(?:json)?\s*\n?/i, "");
  cleaned = cleaned.replace(/\n?\s*```\s*$/, "");

  // 2. Parse.
  let parsed: unknown;
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    console.error("Gemini parse failed", { rawResponse: raw, query });
    return [];
  }

  // 3. Verify root is array.
  if (!Array.isArray(parsed)) {
    console.error("Gemini parse: non-array root", { rawResponse: raw, query });
    return [];
  }

  // 4. Per-item validation.
  const out: DiscoverProduct[] = [];
  for (const item of parsed) {
    if (item == null || typeof item !== "object") continue;
    const rec = item as Record<string, unknown>;

    const title = typeof rec.title === "string" ? rec.title : undefined;
    const retailerUrl = typeof rec.retailer_url === "string" ? rec.retailer_url : undefined;

    let price: number | undefined;
    if (typeof rec.price === "number" && Number.isFinite(rec.price)) {
      price = rec.price;
    } else if (typeof rec.price === "string") {
      const n = parseFloat(rec.price);
      if (!isNaN(n) && Number.isFinite(n)) price = n;
    }

    if (!title || price === undefined || !retailerUrl) continue;

    const descriptionRaw = typeof rec.description === "string" ? rec.description : "";
    const description =
      descriptionRaw.length > 200 ? descriptionRaw.slice(0, 200) : descriptionRaw;

    out.push({
      title,
      description,
      image_url: typeof rec.image_url === "string" ? rec.image_url : "",
      price,
      currency: typeof rec.currency === "string" ? rec.currency : "RON",
      retailer_url: retailerUrl,
      retailer_name: typeof rec.retailer_name === "string" ? rec.retailer_name : "",
    });
  }
  return out;
}
