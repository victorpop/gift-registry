/**
 * Phase 17-07: Gemini 2.5 Flash HTTP wrapper for intent extraction.
 *
 * Replaces the old callGemini (which used google_search grounding) with
 * callGeminiIntent — JSON-mode structured output, no tools.
 *
 * Why no tools: using response_mime_type:"application/json" + response_schema
 * is INCOMPATIBLE with function calling (tools) in Gemini 2.5 Flash.
 * Since we dropped google_search grounding entirely (UAT-6 hallucination fix),
 * this is not a problem — we only need structured intent output here.
 *
 * Uses the Node 22 built-in fetch — no node-fetch dependency.
 *
 * // No tools — JSON mode is incompatible with function calling in Gemini 2.5 Flash.
 */

import type { BuiltPrompt } from "./promptTemplate";
import { parseIntentResponse } from "./parseGeminiResponse";

export interface IntentResult {
  recipient?: string;
  occasion?: string;
  interests?: string[];
  budget?: { amount?: number; currency?: string };
  giftCategories: Array<{
    name: string;
    reason: string;
    /** Provider-agnostic search query (renamed from cseQuery after CSE 403 → Serper pivot) */
    searchQuery: string;
  }>;
}

/**
 * Gemini responseSchema for intent extraction.
 *
 * Uses `searchQuery` (not `cseQuery`) to be provider-agnostic after the
 * CSE → Serper pivot (2026-05-28).
 */
const INTENT_SCHEMA = {
  type: "OBJECT",
  properties: {
    recipient: { type: "STRING" },
    occasion: { type: "STRING" },
    interests: { type: "ARRAY", items: { type: "STRING" } },
    budget: {
      type: "OBJECT",
      properties: {
        amount: { type: "NUMBER" },
        currency: { type: "STRING" },
      },
    },
    giftCategories: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          name: { type: "STRING" },
          reason: { type: "STRING" },
          searchQuery: { type: "STRING" },
        },
        required: ["name", "reason", "searchQuery"],
      },
    },
  },
  required: ["giftCategories"],
};

export const INTENT_SCHEMA_EXPORT = INTENT_SCHEMA;

export async function callGeminiIntent(
  prompt: BuiltPrompt,
  apiKey: string,
): Promise<IntentResult> {
  const url =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" +
    `?key=${encodeURIComponent(apiKey)}`;

  const body = {
    systemInstruction: {
      parts: [{ text: prompt.systemPrompt }],
    },
    contents: [{ role: "user", parts: [{ text: prompt.userPrompt }] }],
    // NO tools field — JSON mode is incompatible with function calling in Gemini 2.5 Flash.
    generationConfig: {
      response_mime_type: "application/json",
      response_schema: INTENT_SCHEMA,
    },
  };

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    // 30s — intent-only is faster than grounded search; reduces the old 60s timeout.
    signal: AbortSignal.timeout(30000),
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => "<unreadable>");
    throw new Error(`Gemini HTTP ${response.status}: ${errText.slice(0, 500)}`);
  }

  const json = (await response.json()) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };
  const text =
    json?.candidates?.[0]?.content?.parts?.map((p) => p.text ?? "").join("") ?? "";

  return parseIntentResponse(text, prompt.userPrompt);
}
