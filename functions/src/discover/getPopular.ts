/**
 * Phase 17 D-12/D-15/D-20/D-21: `discoverPopular` Callable.
 *
 * Returns up to 20 community-popular products from the `popularItems`
 * collection, ordered by `registryCount desc, updatedAt desc`. Backed by a
 * module-scope L1 in-memory cache (1-hour TTL) that lives for the lifetime
 * of the Function instance — D-15 explicitly states this Callable has NO
 * Firestore L2 cache and no per-call rate limit because `popularItems` IS
 * already the authoritative aggregated store.
 *
 * Access policy (D-12):
 *   - Unauthenticated → `HttpsError("unauthenticated", "Sign in required")`.
 *   - Anonymous-provider Auth → `HttpsError("permission-denied", "Registered
 *     account required")`. Discover is for signed-in registered users only;
 *     the anonymous guest path (web giver flow) is explicitly excluded.
 *
 * Response shape (D-20 → spec):
 *   { products: [{ id, title, description, image_url, price, currency,
 *                  retailer_url }] }
 */
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

interface PopularProduct {
  id: string;
  title: string;
  description: string;
  image_url: string;
  price: number;
  currency: string;
  retailer_url: string;
}

interface PopularResponse {
  products: PopularProduct[];
}

// D-21: module-scope L1 cache, lives for the lifetime of the Function instance.
interface CacheEntry {
  data: PopularResponse;
  expiresAt: number;
}
let l1Cache: CacheEntry | null = null;
const L1_TTL_MS = 60 * 60 * 1000;
const TOP_N = 20;
const REGION = "europe-west3";

async function loadFromFirestore(): Promise<PopularResponse> {
  const db = admin.firestore();
  const snap = await db
    .collection("popularItems")
    .orderBy("registryCount", "desc")
    .orderBy("updatedAt", "desc")
    .limit(TOP_N)
    .get();

  const products: PopularProduct[] = snap.docs.map((d) => {
    const data = d.data() as Record<string, unknown>;
    const priceRaw = data.price;
    let price = 0;
    if (typeof priceRaw === "number") {
      price = priceRaw;
    } else if (typeof priceRaw === "string") {
      const n = parseFloat(priceRaw);
      if (!isNaN(n)) price = n;
    }
    return {
      id: d.id,
      title: typeof data.title === "string" ? data.title : "",
      description: "",
      image_url: typeof data.imageUrl === "string" ? data.imageUrl : "",
      price,
      currency: "RON",
      retailer_url: typeof data.canonicalUrl === "string" ? data.canonicalUrl : "",
    };
  });
  return { products };
}

export async function discoverPopularHandler(
  request: CallableRequest<unknown>,
): Promise<PopularResponse> {
  // D-12: auth gate
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  if (request.auth.token.firebase?.sign_in_provider === "anonymous") {
    throw new HttpsError("permission-denied", "Registered account required");
  }

  // D-21: L1 cache
  const now = Date.now();
  if (l1Cache && now < l1Cache.expiresAt) {
    return l1Cache.data;
  }
  const fresh = await loadFromFirestore();
  l1Cache = { data: fresh, expiresAt: now + L1_TTL_MS };
  return fresh;
}

// App Check enforcement: Phase 14 / Phase 16 precedent (e.g. acceptInvite.ts)
// enables `enforceAppCheck: true` on every new Callable. Plan 17-06 flip-the-
// switch task assumes this is on at deploy time.
export const discoverPopular = onCall(
  { region: REGION, enforceAppCheck: true },
  discoverPopularHandler,
);
