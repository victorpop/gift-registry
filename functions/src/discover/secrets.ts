/**
 * Phase 17 D-27: Gemini API key — 2nd-gen Functions secret.
 *
 * Declared ONCE here so every Callable / trigger that talks to Gemini shares
 * the same `defineSecret` reference. 2nd-gen requires the secret to be
 * declared in the same module as the function (or imported into it) and
 * passed via `onCall({ secrets: [GEMINI_API_KEY], ... })` to be materialised
 * in the runtime.
 *
 * Operational notes (do not commit values):
 *   - Set the secret with: `firebase functions:secrets:set GEMINI_API_KEY`
 *   - List versions: `firebase functions:secrets:get GEMINI_API_KEY`
 *
 * NEVER log this value. NEVER expose to Android.
 */

/**
 * Serper.dev API key — replaces CSE_API_KEY after CSE 403 pivot (2026-05-28).
 *
 * Used by discoverSearch (Callable) for Serper /shopping fan-out.
 * Serper.dev is open to new customers (unlike Google CSE, which returned 403).
 *
 * Operational notes (do not commit values):
 *   - Set the secret with: `firebase functions:secrets:set SERPER_API_KEY`
 *   - List versions: `firebase functions:secrets:get SERPER_API_KEY`
 *
 * NEVER log this value. NEVER expose to Android.
 */

import { defineSecret } from "firebase-functions/params";

export const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

export const SERPER_API_KEY = defineSecret("SERPER_API_KEY");
