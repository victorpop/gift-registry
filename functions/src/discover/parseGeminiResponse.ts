/**
 * Phase 17-07: Intent parser + DiscoverProduct type.
 *
 * The DiscoverProduct interface is KEPT byte-for-byte — search.ts and
 * serperNormalizer.ts import it. Android's DiscoverRepositoryImpl maps these
 * fields; do NOT change the shape.
 *
 * parseGeminiResponse is REPLACED by parseIntentResponse — it now parses
 * an IntentResult from Gemini JSON-mode output instead of a product array.
 * The `searchQuery` field name is used throughout (renamed from `cseQuery`
 * after the CSE 403 → Serper pivot on 2026-05-28).
 *
 * Contract:
 *   1. Strip markdown code fences (```json...``` or ```...```) — belt-and-suspenders
 *      even in JSON mode, which may still produce fences in edge cases.
 *   2. JSON.parse inside try/catch — on failure log console.error + return fallback.
 *   3. If parsed object lacks a non-empty giftCategories array → return fallback.
 *   4. Cap giftCategories to first 3 (fan-out cap enforced here).
 *   5. NEVER throw — intent parse errors must return a graceful fallback, not throw.
 *
 * Fallback IntentResult: { giftCategories: [{ name: "", reason: "", searchQuery: fallbackQuery }] }
 */

import type { IntentResult } from "./geminiClient";

/** Backward-compat re-export so callers don't need to update import paths. */
export type { IntentResult };

/**
 * The canonical DiscoverProduct type.
 *
 * KEEP this interface byte-for-byte — Android's DiscoverRepositoryImpl maps
 * these exact field names. Any change requires a coordinated Android update.
 */
export interface DiscoverProduct {
  title: string;
  description: string;
  /** Must be https:// — Android blocks cleartext (network security config). */
  image_url: string;
  /** Parsed from Serper item.price; 0 when Serper has none. Never fabricated. */
  price: number;
  currency: string;
  retailer_url: string;
  retailer_name: string;
}

const MAX_CATEGORIES = 3;

function makeFallbackIntent(fallbackQuery: string): IntentResult {
  return {
    giftCategories: [{ name: "", reason: "", searchQuery: fallbackQuery }],
  };
}

/**
 * Parse Gemini JSON-mode text into an IntentResult.
 *
 * @param raw - Raw text from Gemini candidates[0].content.parts[].text
 * @param fallbackQuery - The original user query used as searchQuery on parse failure
 */
export function parseIntentResponse(raw: string, fallbackQuery: string): IntentResult {
  // 1. Strip code fences. Permissive on opening + closing whitespace.
  let cleaned = raw.trim();
  cleaned = cleaned.replace(/^\s*```(?:json)?\s*\n?/i, "");
  cleaned = cleaned.replace(/\n?\s*```\s*$/, "");

  // 2. Parse.
  let parsed: unknown;
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    console.error("Intent parse failed", { rawResponse: raw, query: fallbackQuery });
    return makeFallbackIntent(fallbackQuery);
  }

  // 3. Verify root is an object with a non-empty giftCategories array.
  if (
    parsed == null ||
    typeof parsed !== "object" ||
    Array.isArray(parsed)
  ) {
    console.error("Intent parse: non-object root", { rawResponse: raw, query: fallbackQuery });
    return makeFallbackIntent(fallbackQuery);
  }

  const rec = parsed as Record<string, unknown>;
  const categories = rec.giftCategories;

  if (!Array.isArray(categories) || categories.length === 0) {
    return makeFallbackIntent(fallbackQuery);
  }

  // 4. Cap to MAX_CATEGORIES.
  const capped = categories.slice(0, MAX_CATEGORIES) as Array<Record<string, unknown>>;

  const giftCategories = capped.map((cat) => ({
    name: typeof cat.name === "string" ? cat.name : "",
    reason: typeof cat.reason === "string" ? cat.reason : "",
    searchQuery: typeof cat.searchQuery === "string" ? cat.searchQuery : fallbackQuery,
  }));

  const result: IntentResult = {
    giftCategories,
  };

  if (typeof rec.recipient === "string") result.recipient = rec.recipient;
  if (typeof rec.occasion === "string") result.occasion = rec.occasion;
  if (Array.isArray(rec.interests)) {
    result.interests = rec.interests.filter((i): i is string => typeof i === "string");
  }
  if (rec.budget != null && typeof rec.budget === "object" && !Array.isArray(rec.budget)) {
    const b = rec.budget as Record<string, unknown>;
    result.budget = {
      amount: typeof b.amount === "number" ? b.amount : undefined,
      currency: typeof b.currency === "string" ? b.currency : undefined,
    };
  }

  return result;
}
