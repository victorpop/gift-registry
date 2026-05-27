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

import { defineSecret } from "firebase-functions/params";

export const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
