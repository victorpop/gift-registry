import { onCall, HttpsError } from "firebase-functions/v2/https";
import { parse } from "node-html-parser";

interface OgMetadataResponse {
  title: string | null;
  imageUrl: string | null;
  price: string | null;
  siteName: string | null;
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
        return { title: null, imageUrl: null, price: null, siteName: null };
      }

      const html = await response.text();
      const root = parse(html);

      const og = (property: string): string | null =>
        root.querySelector(`meta[property="og:${property}"]`)?.getAttribute("content") ?? null;

      const metaName = (name: string): string | null =>
        root.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? null;

      return {
        title: og("title") ?? root.querySelector("title")?.text?.trim() ?? null,
        imageUrl: og("image"),
        price: og("price:amount") ?? og("product:price:amount") ?? metaName("product:price:amount") ?? null,
        siteName: og("site_name"),
      };
    } catch {
      // Return nulls on any failure — network timeout, parse error, etc.
      // Client falls back to manual entry (D-11)
      return { title: null, imageUrl: null, price: null, siteName: null };
    }
  }
);
