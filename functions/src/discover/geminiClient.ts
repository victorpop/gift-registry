/**
 * Phase 17 D-27: Gemini 2.5 Flash HTTP wrapper with `google_search` grounding.
 *
 * Returns the raw text the model produced (single text candidate, parts
 * concatenated). The caller (search.ts in Plan 17-03) feeds this directly
 * into parseGeminiResponse.
 *
 * Uses the Node 22 built-in fetch — no node-fetch dependency added.
 *
 * Throws on network errors and non-2xx HTTP responses; the caller wraps
 * those in HttpsError before they reach the client. Not unit-tested
 * directly (per D-50 precedent — pure-function tests suffice, end-to-end
 * verification arrives via Plan 17-06 deploy smoke test).
 */

import type { BuiltPrompt } from "./promptTemplate";

export async function callGemini(prompt: BuiltPrompt, apiKey: string): Promise<string> {
  const url =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" +
    `?key=${encodeURIComponent(apiKey)}`;

  const body = {
    systemInstruction: {
      parts: [{ text: prompt.systemPrompt }],
    },
    contents: [
      {
        role: "user",
        parts: [{ text: prompt.userPrompt }],
      },
    ],
    tools: [{ google_search: {} }],
  };

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
    // 60s — google_search grounding with broad retailer fallback can issue
    // multiple search rounds before composing the response, especially when
    // the prompt asks the model to expand beyond the prioritized retailer
    // list. Outer Cloud Function timeout is 90s, leaving headroom for OG
    // enrichment.
    signal: AbortSignal.timeout(60000),
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
  return text;
}
